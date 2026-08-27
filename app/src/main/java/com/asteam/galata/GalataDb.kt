package com.asteam.galata

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import java.security.MessageDigest

/**
 * هسته دیتابیس آفلاین گالاتا.
 * همه مبالغ به‌صورت عدد صحیح و بر حسب تومان ذخیره می‌شوند.
 */
class GalataDb(context: Context) : SQLiteOpenHelper(context, DB_NAME, null, DB_VERSION) {

    override fun onCreate(db: SQLiteDatabase) {
        // مالک محلی برنامه.
        db.execSQL("CREATE TABLE owner(id INTEGER PRIMARY KEY CHECK(id=1),username TEXT NOT NULL UNIQUE,password_hash TEXT NOT NULL,name TEXT NOT NULL DEFAULT '')")
        // مشتری‌ها.
        db.execSQL("CREATE TABLE customers(id INTEGER PRIMARY KEY AUTOINCREMENT,name TEXT NOT NULL,phone TEXT NOT NULL DEFAULT '')")
        // کالا و خدمات.
        db.execSQL("CREATE TABLE products(id INTEGER PRIMARY KEY AUTOINCREMENT,name TEXT NOT NULL,buy_price INTEGER NOT NULL DEFAULT 0,sell_price INTEGER NOT NULL DEFAULT 0,unit TEXT NOT NULL DEFAULT 'عدد')")
        // فاکتور و اقلام آن.
        db.execSQL("CREATE TABLE invoices(id INTEGER PRIMARY KEY AUTOINCREMENT,customer_id INTEGER NOT NULL,customer_name TEXT NOT NULL,total INTEGER NOT NULL,created_at INTEGER NOT NULL)")
        db.execSQL("CREATE TABLE invoice_items(id INTEGER PRIMARY KEY AUTOINCREMENT,invoice_id INTEGER NOT NULL,product_id INTEGER NOT NULL,name_snapshot TEXT NOT NULL,qty INTEGER NOT NULL,unit_price INTEGER NOT NULL,line_total INTEGER NOT NULL)")
        // دفتر کل مشتری؛ بدهکار مثبت و پرداخت کاهنده است.
        db.execSQL("CREATE TABLE ledger(id INTEGER PRIMARY KEY AUTOINCREMENT,customer_id INTEGER NOT NULL,type TEXT NOT NULL,amount INTEGER NOT NULL,note TEXT NOT NULL DEFAULT '',created_at INTEGER NOT NULL)")
        // یادآورها.
        db.execSQL("CREATE TABLE reminders(id INTEGER PRIMARY KEY AUTOINCREMENT,title TEXT NOT NULL,description TEXT NOT NULL DEFAULT '',due_at INTEGER NOT NULL)")
        // هزینه‌ها.
        db.execSQL("CREATE TABLE expenses(id INTEGER PRIMARY KEY AUTOINCREMENT,title TEXT NOT NULL,amount INTEGER NOT NULL,created_at INTEGER NOT NULL)")
        seed(db)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        // در نسخه‌های بعدی Migration افزایشی اضافه می‌شود؛ حذف داده ممنوع است.
    }

    /** داده نمونه برای تست سریع APK. */
    private fun seed(db: SQLiteDatabase) {
        val ali = addCustomer(db, "علیرضا محمدی", "۰۹۱۲۱۲۳۴۵۶۷")
        addCustomer(db, "سارا احمدی", "۰۹۳۵۱۲۳۴۵۶۷")
        addProduct(db, "اسپرسو", 70_000, 120_000, "عدد")
        addProduct(db, "آب معدنی", 8_000, 15_000, "عدد")
        addProduct(db, "خدمات نصب", 0, 350_000, "خدمت")
        addLedger(db, ali, "DEBT", 1_000_000, "مانده نمونه")
        db.insert("reminders", null, ContentValues().apply {
            put("title", "چک مشتری")
            put("description", "پیگیری چک")
            put("due_at", System.currentTimeMillis() + 5L * 24 * 60 * 60 * 1000)
        })
    }

    // ---------- مالک ----------
    fun hasOwner(): Boolean = readableDatabase.rawQuery("SELECT COUNT(*) FROM owner", null).use { c -> c.moveToFirst(); c.getInt(0) > 0 }

    fun registerOwner(username: String, password: String, name: String): Boolean {
        if (hasOwner()) return false
        return writableDatabase.insert("owner", null, ContentValues().apply {
            put("id", 1); put("username", username.trim()); put("password_hash", hash(password)); put("name", name.trim())
        }) != -1L
    }

    fun login(username: String, password: String): Boolean = readableDatabase.rawQuery(
        "SELECT COUNT(*) FROM owner WHERE username=? AND password_hash=?",
        arrayOf(username.trim(), hash(password))
    ).use { c -> c.moveToFirst(); c.getInt(0) == 1 }

    fun ownerName(): String = readableDatabase.rawQuery("SELECT name FROM owner LIMIT 1", null).use { c -> if (c.moveToFirst()) c.getString(0) else "مالک گالاتا" }

    // ---------- مشتری ----------
    private fun addCustomer(db: SQLiteDatabase, name: String, phone: String): Long = db.insert("customers", null, ContentValues().apply {
        put("name", name.trim()); put("phone", phone.trim())
    })

    fun addCustomer(name: String, phone: String): Long = addCustomer(writableDatabase, name, phone)

    fun customers(): List<Customer> {
        val out = mutableListOf<Customer>()
        readableDatabase.rawQuery("SELECT id,name,phone FROM customers ORDER BY id DESC", null).use { c ->
            while (c.moveToNext()) out += Customer(c.getLong(0), c.getString(1), c.getString(2), balance(c.getLong(0)))
        }
        return out
    }

    fun balance(customerId: Long): Long = readableDatabase.rawQuery(
        "SELECT COALESCE(SUM(CASE WHEN type='DEBT' THEN amount ELSE -amount END),0) FROM ledger WHERE customer_id=?",
        arrayOf(customerId.toString())
    ).use { c -> c.moveToFirst(); c.getLong(0) }

    // ---------- کالا ----------
    private fun addProduct(db: SQLiteDatabase, name: String, buy: Long, sell: Long, unit: String): Long = db.insert("products", null, ContentValues().apply {
        put("name", name.trim()); put("buy_price", buy); put("sell_price", sell); put("unit", unit.trim())
    })

    fun addProduct(name: String, buy: Long, sell: Long, unit: String): Long = addProduct(writableDatabase, name, buy, sell, unit)

    fun products(): List<Product> {
        val out = mutableListOf<Product>()
        readableDatabase.rawQuery("SELECT id,name,buy_price,sell_price,unit FROM products ORDER BY id DESC", null).use { c ->
            while (c.moveToNext()) out += Product(c.getLong(0), c.getString(1), c.getLong(2), c.getLong(3), c.getString(4))
        }
        return out
    }

    // ---------- فروش ----------
    fun saveInvoice(customer: Customer, lines: List<SaleLine>): Long {
        val db = writableDatabase
        db.beginTransaction()
        return try {
            val total = lines.sumOf { it.total }
            val now = System.currentTimeMillis()
            val invoiceId = db.insertOrThrow("invoices", null, ContentValues().apply {
                put("customer_id", customer.id); put("customer_name", customer.name); put("total", total); put("created_at", now)
            })
            lines.forEach { line ->
                db.insertOrThrow("invoice_items", null, ContentValues().apply {
                    put("invoice_id", invoiceId); put("product_id", line.product.id); put("name_snapshot", line.product.name)
                    put("qty", line.qty); put("unit_price", line.product.sellPrice); put("line_total", line.total)
                })
            }
            addLedger(db, customer.id, "DEBT", total, "فاکتور شماره $invoiceId")
            db.setTransactionSuccessful()
            invoiceId
        } finally { db.endTransaction() }
    }

    fun invoices(): List<Invoice> {
        val out = mutableListOf<Invoice>()
        readableDatabase.rawQuery("SELECT id,customer_id,customer_name,total,created_at FROM invoices ORDER BY id DESC", null).use { c ->
            while (c.moveToNext()) out += Invoice(c.getLong(0), c.getLong(1), c.getString(2), c.getLong(3), c.getLong(4))
        }
        return out
    }

    fun invoiceItems(invoiceId: Long): List<InvoiceItem> {
        val out = mutableListOf<InvoiceItem>()
        readableDatabase.rawQuery("SELECT name_snapshot,qty,unit_price,line_total FROM invoice_items WHERE invoice_id=? ORDER BY id", arrayOf(invoiceId.toString())).use { c ->
            while (c.moveToNext()) out += InvoiceItem(c.getString(0), c.getInt(1), c.getLong(2), c.getLong(3))
        }
        return out
    }

    // ---------- تسویه و صورت‌حساب ----------
    private fun addLedger(db: SQLiteDatabase, customerId: Long, type: String, amount: Long, note: String): Long = db.insert("ledger", null, ContentValues().apply {
        put("customer_id", customerId); put("type", type); put("amount", amount); put("note", note); put("created_at", System.currentTimeMillis())
    })

    fun addPayment(customerId: Long, amount: Long, note: String): Long = addLedger(writableDatabase, customerId, "PAYMENT", amount, note.ifBlank { "دریافت از مشتری" })

    fun ledger(customerId: Long): List<LedgerRow> {
        val out = mutableListOf<LedgerRow>()
        readableDatabase.rawQuery("SELECT type,amount,note,created_at FROM ledger WHERE customer_id=? ORDER BY id DESC", arrayOf(customerId.toString())).use { c ->
            while (c.moveToNext()) out += LedgerRow(c.getString(0), c.getLong(1), c.getString(2), c.getLong(3))
        }
        return out
    }

    // ---------- هزینه و یادآور ----------
    fun addExpense(title: String, amount: Long): Long = writableDatabase.insert("expenses", null, ContentValues().apply {
        put("title", title.trim()); put("amount", amount); put("created_at", System.currentTimeMillis())
    })

    fun addReminder(title: String, description: String, dueAt: Long): Long = writableDatabase.insert("reminders", null, ContentValues().apply {
        put("title", title.trim()); put("description", description.trim()); put("due_at", dueAt)
    })

    fun reminders(): List<Reminder> {
        val out = mutableListOf<Reminder>()
        readableDatabase.rawQuery("SELECT id,title,description,due_at FROM reminders ORDER BY due_at", null).use { c ->
            while (c.moveToNext()) out += Reminder(c.getLong(0), c.getString(1), c.getString(2), c.getLong(3))
        }
        return out
    }

    // ---------- داشبورد ----------
    fun dashboard(): Dashboard {
        val today = java.util.Calendar.getInstance().apply {
            set(java.util.Calendar.HOUR_OF_DAY, 0); set(java.util.Calendar.MINUTE, 0); set(java.util.Calendar.SECOND, 0); set(java.util.Calendar.MILLISECOND, 0)
        }.timeInMillis
        val sales = scalar("SELECT COALESCE(SUM(total),0) FROM invoices WHERE created_at>=?", arrayOf(today.toString()))
        val receipts = scalar("SELECT COALESCE(SUM(amount),0) FROM ledger WHERE type='PAYMENT' AND created_at>=?", arrayOf(today.toString()))
        val receivable = customers().sumOf { if (it.balance > 0) it.balance else 0L }
        val expenses = scalar("SELECT COALESCE(SUM(amount),0) FROM expenses", null)
        return Dashboard(sales, receipts, receivable, expenses, invoices().size, reminders().size)
    }

    private fun scalar(sql: String, args: Array<String>?): Long = readableDatabase.rawQuery(sql, args).use { c -> c.moveToFirst(); c.getLong(0) }

    private fun hash(value: String): String = MessageDigest.getInstance("SHA-256").digest(value.toByteArray()).joinToString("") { "%02x".format(it) }

    companion object {
        private const val DB_NAME = "galata.db"
        private const val DB_VERSION = 1
    }
}

data class Customer(val id: Long, val name: String, val phone: String, val balance: Long)
data class Product(val id: Long, val name: String, val buyPrice: Long, val sellPrice: Long, val unit: String)
data class SaleLine(val product: Product, val qty: Int) { val total: Long get() = product.sellPrice * qty }
data class Invoice(val id: Long, val customerId: Long, val customerName: String, val total: Long, val createdAt: Long)
data class InvoiceItem(val name: String, val qty: Int, val unitPrice: Long, val total: Long)
data class LedgerRow(val type: String, val amount: Long, val note: String, val createdAt: Long)
data class Reminder(val id: Long, val title: String, val description: String, val dueAt: Long)
data class Dashboard(val salesToday: Long, val receiptsToday: Long, val receivable: Long, val expenses: Long, val invoiceCount: Int, val reminderCount: Int)
