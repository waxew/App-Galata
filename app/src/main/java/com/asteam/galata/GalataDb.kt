package com.asteam.galata

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.util.Base64
import java.io.File
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

/**
 * هسته دیتابیس کاملاً آفلاین گالاتا.
 * همه مبالغ Long و بر حسب تومان هستند و هیچ Float/Double برای محاسبات پولی استفاده نمی‌شود.
 * Migrationها افزایشی‌اند؛ هیچ onUpgradeای جدول کاربر را Drop نمی‌کند.
 */
class GalataDb(context: Context) : SQLiteOpenHelper(context.applicationContext, DB_NAME, null, DB_VERSION) {
    private val appContext = context.applicationContext

    override fun onCreate(db: SQLiteDatabase) {
        // حساب مالک برنامه؛ Salt جداگانه برای مشتق‌سازی امن رمز ذخیره می‌شود.
        db.execSQL("CREATE TABLE owner(id INTEGER PRIMARY KEY CHECK(id=1),username TEXT NOT NULL UNIQUE,password_hash TEXT NOT NULL,password_salt TEXT NOT NULL DEFAULT '',name TEXT NOT NULL DEFAULT '',description TEXT NOT NULL DEFAULT '',photo_uri TEXT NOT NULL DEFAULT '')")
        // مشتری‌ها؛ archived سابقه مالی را نگه می‌دارد و فقط از لیست فعال پنهان می‌کند.
        db.execSQL("CREATE TABLE customers(id INTEGER PRIMARY KEY AUTOINCREMENT,name TEXT NOT NULL,phone TEXT NOT NULL DEFAULT '',note TEXT NOT NULL DEFAULT '',archived INTEGER NOT NULL DEFAULT 0)")
        // کالا/خدمت؛ track_stock=0 یعنی موجودی نامحدود/خدمت.
        db.execSQL("CREATE TABLE products(id INTEGER PRIMARY KEY AUTOINCREMENT,name TEXT NOT NULL,buy_price INTEGER NOT NULL DEFAULT 0,sell_price INTEGER NOT NULL DEFAULT 0,unit TEXT NOT NULL DEFAULT 'عدد',stock_qty INTEGER NOT NULL DEFAULT 0,track_stock INTEGER NOT NULL DEFAULT 0,archived INTEGER NOT NULL DEFAULT 0)")
        // سربرگ فاکتور؛ received_at_sale فقط پولی است که همان لحظه دریافت شده است.
        db.execSQL("CREATE TABLE invoices(id INTEGER PRIMARY KEY AUTOINCREMENT,customer_id INTEGER NOT NULL,customer_name TEXT NOT NULL,total INTEGER NOT NULL,created_at INTEGER NOT NULL,status TEXT NOT NULL DEFAULT 'ACTIVE',received_at_sale INTEGER NOT NULL DEFAULT 0,note TEXT NOT NULL DEFAULT '')")
        // اقلام فاکتور Snapshot قیمت خرید/فروش و نام را حفظ می‌کنند تا تغییر قیمت آینده تاریخچه را عوض نکند.
        db.execSQL("CREATE TABLE invoice_items(id INTEGER PRIMARY KEY AUTOINCREMENT,invoice_id INTEGER NOT NULL,product_id INTEGER NOT NULL,name_snapshot TEXT NOT NULL,qty INTEGER NOT NULL,unit_price INTEGER NOT NULL,buy_price INTEGER NOT NULL DEFAULT 0,line_total INTEGER NOT NULL)")
        // دفتر گردش مشتری؛ مانده از رویدادها محاسبه می‌شود و یک عدد قابل‌دستکاری در Customers نیست.
        db.execSQL("CREATE TABLE ledger(id INTEGER PRIMARY KEY AUTOINCREMENT,customer_id INTEGER NOT NULL,type TEXT NOT NULL,amount INTEGER NOT NULL,note TEXT NOT NULL DEFAULT '',created_at INTEGER NOT NULL)")
        // یادآورهای نوع‌دار شامل چک، قسط، قرار و عمومی.
        db.execSQL("CREATE TABLE reminders(id INTEGER PRIMARY KEY AUTOINCREMENT,title TEXT NOT NULL,description TEXT NOT NULL DEFAULT '',due_at INTEGER NOT NULL,kind TEXT NOT NULL DEFAULT 'GENERAL',amount INTEGER NOT NULL DEFAULT 0,done INTEGER NOT NULL DEFAULT 0)")
        // یادداشت‌های روزانه روی تقویم شمسی با day_key مثل 1405-06-07.
        db.execSQL("CREATE TABLE calendar_notes(id INTEGER PRIMARY KEY AUTOINCREMENT,day_key TEXT NOT NULL,title TEXT NOT NULL,text TEXT NOT NULL DEFAULT '',created_at INTEGER NOT NULL)")
        // هزینه‌های کسب‌وکار.
        db.execSQL("CREATE TABLE expenses(id INTEGER PRIMARY KEY AUTOINCREMENT,title TEXT NOT NULL,amount INTEGER NOT NULL,created_at INTEGER NOT NULL,note TEXT NOT NULL DEFAULT '')")
        createIndexes(db)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        // Migration نسخه 0.x به 1.0؛ داده قبلی حفظ می‌شود و فقط ستون‌های جدید اضافه می‌شوند.
        if (oldVersion < 2) {
            db.execSQL("ALTER TABLE owner ADD COLUMN password_salt TEXT NOT NULL DEFAULT ''")
            db.execSQL("ALTER TABLE owner ADD COLUMN description TEXT NOT NULL DEFAULT ''")
            db.execSQL("ALTER TABLE owner ADD COLUMN photo_uri TEXT NOT NULL DEFAULT ''")
            db.execSQL("ALTER TABLE customers ADD COLUMN note TEXT NOT NULL DEFAULT ''")
            db.execSQL("ALTER TABLE customers ADD COLUMN archived INTEGER NOT NULL DEFAULT 0")
            db.execSQL("ALTER TABLE products ADD COLUMN stock_qty INTEGER NOT NULL DEFAULT 0")
            db.execSQL("ALTER TABLE products ADD COLUMN track_stock INTEGER NOT NULL DEFAULT 0")
            db.execSQL("ALTER TABLE products ADD COLUMN archived INTEGER NOT NULL DEFAULT 0")
            db.execSQL("ALTER TABLE invoices ADD COLUMN status TEXT NOT NULL DEFAULT 'ACTIVE'")
            db.execSQL("ALTER TABLE invoices ADD COLUMN received_at_sale INTEGER NOT NULL DEFAULT 0")
            db.execSQL("ALTER TABLE invoices ADD COLUMN note TEXT NOT NULL DEFAULT ''")
            db.execSQL("ALTER TABLE invoice_items ADD COLUMN buy_price INTEGER NOT NULL DEFAULT 0")
            db.execSQL("UPDATE invoice_items SET buy_price=COALESCE((SELECT buy_price FROM products WHERE products.id=invoice_items.product_id),0)")
            db.execSQL("ALTER TABLE reminders ADD COLUMN kind TEXT NOT NULL DEFAULT 'GENERAL'")
            db.execSQL("ALTER TABLE reminders ADD COLUMN amount INTEGER NOT NULL DEFAULT 0")
            db.execSQL("ALTER TABLE reminders ADD COLUMN done INTEGER NOT NULL DEFAULT 0")
            db.execSQL("ALTER TABLE expenses ADD COLUMN note TEXT NOT NULL DEFAULT ''")
            db.execSQL("CREATE TABLE IF NOT EXISTS calendar_notes(id INTEGER PRIMARY KEY AUTOINCREMENT,day_key TEXT NOT NULL,title TEXT NOT NULL,text TEXT NOT NULL DEFAULT '',created_at INTEGER NOT NULL)")
            createIndexes(db)
        }
    }

    /** ایندکس‌های پرتکرار برای سرعت جستجو، تاریخ و گردش حساب. */
    private fun createIndexes(db: SQLiteDatabase) {
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_invoices_created ON invoices(created_at)")
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_ledger_customer ON ledger(customer_id,created_at)")
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_reminders_due ON reminders(done,due_at)")
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_calendar_day ON calendar_notes(day_key)")
    }

    // ---------- مالک و امنیت ----------

    fun hasOwner(): Boolean = scalar("SELECT COUNT(*) FROM owner", null) > 0L

    fun registerOwner(username: String, password: String, name: String): Boolean {
        if (hasOwner() || username.isBlank() || password.length < 4) return false
        val salt = newSalt()
        return writableDatabase.insert("owner", null, ContentValues().apply {
            put("id", 1)
            put("username", username.trim())
            put("password_hash", deriveKey(password, salt))
            put("password_salt", salt)
            put("name", name.trim())
        }) != -1L
    }

    /** ورود سازگار با نسخه قدیمی؛ در اولین ورود موفق SHA-256 قدیمی خودکار به PBKDF2+Salt مهاجرت می‌کند. */
    fun login(username: String, password: String): Boolean {
        readableDatabase.rawQuery("SELECT password_hash,password_salt FROM owner WHERE username=?", arrayOf(username.trim())).use { c ->
            if (!c.moveToFirst()) return false
            val stored = c.getString(0)
            val salt = c.getString(1).orEmpty()
            if (salt.isBlank()) {
                val ok = secureEquals(stored, legacyHash(password))
                if (ok) upgradeLegacyPassword(password)
                return ok
            }
            return secureEquals(stored, deriveKey(password, salt))
        }
    }

    fun changePassword(current: String, replacement: String): Boolean {
        val profile = ownerProfile() ?: return false
        if (!login(profile.username, current) || replacement.length < 4) return false
        val salt = newSalt()
        return writableDatabase.update("owner", ContentValues().apply {
            put("password_hash", deriveKey(replacement, salt))
            put("password_salt", salt)
        }, "id=1", null) == 1
    }

    private fun upgradeLegacyPassword(password: String) {
        val salt = newSalt()
        writableDatabase.update("owner", ContentValues().apply {
            put("password_hash", deriveKey(password, salt))
            put("password_salt", salt)
        }, "id=1", null)
    }

    fun ownerProfile(): OwnerProfile? = readableDatabase.rawQuery(
        "SELECT username,name,description,photo_uri FROM owner WHERE id=1", null
    ).use { c -> if (c.moveToFirst()) OwnerProfile(c.getString(0), c.getString(1), c.getString(2), c.getString(3)) else null }

    fun ownerName(): String = ownerProfile()?.let { it.name.ifBlank { it.username } } ?: "مالک گالاتا"

    fun updateOwnerProfile(name: String, description: String): Boolean = writableDatabase.update(
        "owner", ContentValues().apply { put("name", name.trim()); put("description", description.trim()) }, "id=1", null
    ) == 1

    fun setOwnerPhoto(uri: String): Boolean = writableDatabase.update(
        "owner", ContentValues().apply { put("photo_uri", uri) }, "id=1", null
    ) == 1

    // ---------- مشتری ----------

    fun addCustomer(name: String, phone: String, note: String = ""): Long {
        if (name.isBlank()) return -1L
        return writableDatabase.insert("customers", null, ContentValues().apply {
            put("name", name.trim()); put("phone", phone.trim()); put("note", note.trim())
        })
    }

    fun updateCustomer(id: Long, name: String, phone: String, note: String): Boolean {
        if (name.isBlank()) return false
        return writableDatabase.update("customers", ContentValues().apply {
            put("name", name.trim()); put("phone", phone.trim()); put("note", note.trim())
        }, "id=?", arrayOf(id.toString())) == 1
    }

    fun customers(includeArchived: Boolean = false, query: String = ""): List<Customer> {
        val out = mutableListOf<Customer>()
        val where = buildString {
            if (!includeArchived) append("archived=0")
            if (query.isNotBlank()) {
                if (isNotEmpty()) append(" AND ")
                append("(name LIKE ? OR phone LIKE ?)")
            }
        }
        val args = if (query.isNotBlank()) arrayOf("%${query.trim()}%", "%${query.trim()}%") else null
        val sql = "SELECT id,name,phone,note,archived FROM customers" + (if (where.isNotEmpty()) " WHERE $where" else "") + " ORDER BY name COLLATE NOCASE"
        readableDatabase.rawQuery(sql, args).use { c ->
            while (c.moveToNext()) {
                val id = c.getLong(0)
                out += Customer(id, c.getString(1), c.getString(2), balance(id), c.getString(3), c.getInt(4) == 1)
            }
        }
        return out
    }

    fun customer(id: Long): Customer? = readableDatabase.rawQuery(
        "SELECT id,name,phone,note,archived FROM customers WHERE id=?", arrayOf(id.toString())
    ).use { c -> if (c.moveToFirst()) Customer(c.getLong(0), c.getString(1), c.getString(2), balance(c.getLong(0)), c.getString(3), c.getInt(4) == 1) else null }

    /** حذف بدون سابقه واقعی است؛ مشتری دارای فاکتور/گردش فقط Archive می‌شود. */
    fun removeCustomer(id: Long): String {
        val hasHistory = scalar("SELECT COUNT(*) FROM invoices WHERE customer_id=?", arrayOf(id.toString())) > 0L ||
            scalar("SELECT COUNT(*) FROM ledger WHERE customer_id=?", arrayOf(id.toString())) > 0L
        return if (hasHistory) {
            writableDatabase.update("customers", ContentValues().apply { put("archived", 1) }, "id=?", arrayOf(id.toString()))
            "ARCHIVED"
        } else {
            writableDatabase.delete("customers", "id=?", arrayOf(id.toString()))
            "DELETED"
        }
    }

    /** مانده مثبت=مشتری بدهکار، منفی=مشتری بستانکار. */
    fun balance(customerId: Long): Long = scalar(
        "SELECT COALESCE(SUM(CASE WHEN type IN ('DEBT','ADJUST_DEBT','REFUND') THEN amount WHEN type IN ('PAYMENT','CANCEL_DEBT','ADJUST_CREDIT') THEN -amount ELSE 0 END),0) FROM ledger WHERE customer_id=?",
        arrayOf(customerId.toString())
    )

    fun adjustBalance(customerId: Long, delta: Long, note: String): Long {
        if (delta == 0L) return -1L
        return addLedger(writableDatabase, customerId, if (delta > 0) "ADJUST_DEBT" else "ADJUST_CREDIT", kotlin.math.abs(delta), note.ifBlank { "اصلاح دستی حساب" })
    }

    // ---------- کالا و خدمات ----------

    fun addProduct(name: String, buy: Long, sell: Long, unit: String, stockQty: Long = 0L, trackStock: Boolean = false): Long {
        if (name.isBlank() || sell < 0L || buy < 0L) return -1L
        return writableDatabase.insert("products", null, ContentValues().apply {
            put("name", name.trim()); put("buy_price", buy); put("sell_price", sell); put("unit", unit.trim().ifBlank { "عدد" })
            put("stock_qty", stockQty.coerceAtLeast(0)); put("track_stock", if (trackStock) 1 else 0)
        })
    }

    fun updateProduct(id: Long, name: String, buy: Long, sell: Long, unit: String, stockQty: Long, trackStock: Boolean): Boolean {
        if (name.isBlank() || buy < 0 || sell < 0) return false
        return writableDatabase.update("products", ContentValues().apply {
            put("name", name.trim()); put("buy_price", buy); put("sell_price", sell); put("unit", unit.trim().ifBlank { "عدد" })
            put("stock_qty", stockQty.coerceAtLeast(0)); put("track_stock", if (trackStock) 1 else 0)
        }, "id=?", arrayOf(id.toString())) == 1
    }

    fun products(includeArchived: Boolean = false, query: String = ""): List<Product> {
        val out = mutableListOf<Product>()
        val where = mutableListOf<String>()
        val args = mutableListOf<String>()
        if (!includeArchived) where += "archived=0"
        if (query.isNotBlank()) { where += "name LIKE ?"; args += "%${query.trim()}%" }
        val sql = "SELECT id,name,buy_price,sell_price,unit,stock_qty,track_stock,archived FROM products" +
            (if (where.isNotEmpty()) " WHERE ${where.joinToString(" AND ")}" else "") + " ORDER BY name COLLATE NOCASE"
        readableDatabase.rawQuery(sql, args.toTypedArray().takeIf { it.isNotEmpty() }).use { c ->
            while (c.moveToNext()) out += Product(c.getLong(0), c.getString(1), c.getLong(2), c.getLong(3), c.getString(4), c.getLong(5), c.getInt(6) == 1, c.getInt(7) == 1)
        }
        return out
    }

    fun product(id: Long): Product? = readableDatabase.rawQuery(
        "SELECT id,name,buy_price,sell_price,unit,stock_qty,track_stock,archived FROM products WHERE id=?", arrayOf(id.toString())
    ).use { c -> if (c.moveToFirst()) Product(c.getLong(0), c.getString(1), c.getLong(2), c.getLong(3), c.getString(4), c.getLong(5), c.getInt(6) == 1, c.getInt(7) == 1) else null }

    fun removeProduct(id: Long): String {
        val hasHistory = scalar("SELECT COUNT(*) FROM invoice_items WHERE product_id=?", arrayOf(id.toString())) > 0L
        return if (hasHistory) {
            writableDatabase.update("products", ContentValues().apply { put("archived", 1) }, "id=?", arrayOf(id.toString()))
            "ARCHIVED"
        } else {
            writableDatabase.delete("products", "id=?", arrayOf(id.toString()))
            "DELETED"
        }
    }

    // ---------- فروش، فاکتور و موجودی ----------

    private data class PreparedLine(val productId: Long, val name: String, val qty: Int, val buy: Long, val sell: Long, val total: Long, val track: Boolean)

    fun saveInvoice(customer: Customer, lines: List<SaleLine>, receivedNow: Long = 0L, note: String = ""): Long {
        require(lines.isNotEmpty()) { "فاکتور بدون قلم قابل ثبت نیست" }
        val db = writableDatabase
        db.beginTransaction()
        return try {
            val prepared = lines.map { line ->
                require(line.qty > 0) { "تعداد باید بیشتر از صفر باشد" }
                db.rawQuery("SELECT name,buy_price,sell_price,stock_qty,track_stock,archived FROM products WHERE id=?", arrayOf(line.product.id.toString())).use { c ->
                    require(c.moveToFirst() && c.getInt(5) == 0) { "محصول حذف یا بایگانی شده است" }
                    val track = c.getInt(4) == 1
                    val stock = c.getLong(3)
                    require(!track || stock >= line.qty) { "موجودی ${c.getString(0)} کافی نیست" }
                    PreparedLine(line.product.id, c.getString(0), line.qty, c.getLong(1), c.getLong(2), c.getLong(2) * line.qty, track)
                }
            }
            val total = prepared.sumOf { it.total }
            require(receivedNow in 0..total) { "دریافتی اولیه نمی‌تواند از جمع فاکتور بیشتر باشد" }
            val now = System.currentTimeMillis()
            val invoiceId = db.insertOrThrow("invoices", null, ContentValues().apply {
                put("customer_id", customer.id); put("customer_name", customer.name); put("total", total); put("created_at", now)
                put("status", "ACTIVE"); put("received_at_sale", receivedNow); put("note", note.trim())
            })
            prepared.forEach { line ->
                db.insertOrThrow("invoice_items", null, ContentValues().apply {
                    put("invoice_id", invoiceId); put("product_id", line.productId); put("name_snapshot", line.name); put("qty", line.qty)
                    put("unit_price", line.sell); put("buy_price", line.buy); put("line_total", line.total)
                })
                if (line.track) db.execSQL("UPDATE products SET stock_qty=stock_qty-? WHERE id=?", arrayOf(line.qty, line.productId))
            }
            addLedger(db, customer.id, "DEBT", total, "فاکتور شماره $invoiceId", now)
            if (receivedNow > 0L) addLedger(db, customer.id, "PAYMENT", receivedNow, "دریافت هنگام فروش - فاکتور $invoiceId", now)
            db.setTransactionSuccessful()
            invoiceId
        } finally {
            db.endTransaction()
        }
    }

    fun invoices(query: String = ""): List<Invoice> {
        val out = mutableListOf<Invoice>()
        val where = if (query.isBlank()) "" else " WHERE customer_name LIKE ? OR CAST(id AS TEXT) LIKE ?"
        val args = if (query.isBlank()) null else arrayOf("%${query.trim()}%", "%${query.trim()}%")
        readableDatabase.rawQuery("SELECT id,customer_id,customer_name,total,created_at,status,received_at_sale,note FROM invoices$where ORDER BY id DESC", args).use { c ->
            while (c.moveToNext()) out += Invoice(c.getLong(0), c.getLong(1), c.getString(2), c.getLong(3), c.getLong(4), c.getString(5), c.getLong(6), c.getString(7))
        }
        return out
    }

    fun invoice(id: Long): Invoice? = readableDatabase.rawQuery(
        "SELECT id,customer_id,customer_name,total,created_at,status,received_at_sale,note FROM invoices WHERE id=?", arrayOf(id.toString())
    ).use { c -> if (c.moveToFirst()) Invoice(c.getLong(0), c.getLong(1), c.getString(2), c.getLong(3), c.getLong(4), c.getString(5), c.getLong(6), c.getString(7)) else null }

    fun invoiceItems(invoiceId: Long): List<InvoiceItem> {
        val out = mutableListOf<InvoiceItem>()
        readableDatabase.rawQuery("SELECT name_snapshot,qty,unit_price,line_total,buy_price,product_id FROM invoice_items WHERE invoice_id=? ORDER BY id", arrayOf(invoiceId.toString())).use { c ->
            while (c.moveToNext()) out += InvoiceItem(c.getString(0), c.getInt(1), c.getLong(2), c.getLong(3), c.getLong(4), c.getLong(5))
        }
        return out
    }

    /** لغو فاکتور به جای Delete؛ بدهی معکوس و موجودی برگردانده می‌شود و تاریخچه حفظ می‌شود. */
    fun cancelInvoice(invoiceId: Long): Boolean {
        val db = writableDatabase
        db.beginTransaction()
        return try {
            val inv = db.rawQuery("SELECT customer_id,total,status FROM invoices WHERE id=?", arrayOf(invoiceId.toString())).use { c ->
                if (!c.moveToFirst()) null else Triple(c.getLong(0), c.getLong(1), c.getString(2))
            } ?: return false
            if (inv.third != "ACTIVE") return false
            db.update("invoices", ContentValues().apply { put("status", "CANCELLED") }, "id=?", arrayOf(invoiceId.toString()))
            db.rawQuery("SELECT product_id,qty FROM invoice_items WHERE invoice_id=?", arrayOf(invoiceId.toString())).use { c ->
                while (c.moveToNext()) db.execSQL("UPDATE products SET stock_qty=stock_qty+? WHERE id=? AND track_stock=1", arrayOf(c.getInt(1), c.getLong(0)))
            }
            addLedger(db, inv.first, "CANCEL_DEBT", inv.second, "لغو فاکتور $invoiceId")
            db.setTransactionSuccessful()
            true
        } finally {
            db.endTransaction()
        }
    }

    // ---------- تسویه و دفتر گردش ----------

    private fun addLedger(db: SQLiteDatabase, customerId: Long, type: String, amount: Long, note: String, createdAt: Long = System.currentTimeMillis()): Long =
        db.insert("ledger", null, ContentValues().apply {
            put("customer_id", customerId); put("type", type); put("amount", amount.coerceAtLeast(0)); put("note", note.trim()); put("created_at", createdAt)
        })

    fun addPayment(customerId: Long, amount: Long, note: String): Long {
        val current = balance(customerId)
        if (amount <= 0L || current <= 0L || amount > current) return -1L
        return addLedger(writableDatabase, customerId, "PAYMENT", amount, note.ifBlank { "دریافت از مشتری" })
    }

    /** بازپرداخت وجه به مشتری بستانکار؛ پس از لغو فروش پرداخت‌شده کاربرد دارد. */
    fun addRefund(customerId: Long, amount: Long, note: String): Long {
        val current = balance(customerId)
        if (amount <= 0L || current >= 0L || amount > -current) return -1L
        return addLedger(writableDatabase, customerId, "REFUND", amount, note.ifBlank { "بازپرداخت به مشتری" })
    }

    fun ledger(customerId: Long): List<LedgerRow> {
        val chronological = mutableListOf<LedgerRow>()
        var running = 0L
        readableDatabase.rawQuery("SELECT type,amount,note,created_at FROM ledger WHERE customer_id=? ORDER BY id ASC", arrayOf(customerId.toString())).use { c ->
            while (c.moveToNext()) {
                val type = c.getString(0)
                val amount = c.getLong(1)
                val signed = signed(type, amount)
                running += signed
                chronological += LedgerRow(type, amount, c.getString(2), c.getLong(3), signed, running)
            }
        }
        return chronological.asReversed()
    }

    private fun signed(type: String, amount: Long): Long = when (type) {
        "DEBT", "ADJUST_DEBT", "REFUND" -> amount
        "PAYMENT", "CANCEL_DEBT", "ADJUST_CREDIT" -> -amount
        else -> 0L
    }

    // ---------- یادآور و تقویم ----------

    fun addReminder(title: String, description: String, dueAt: Long): Long = addReminder(title, description, dueAt, "GENERAL", 0L)

    fun addReminder(title: String, description: String, dueAt: Long, kind: String, amount: Long): Long {
        if (title.isBlank()) return -1L
        return writableDatabase.insert("reminders", null, ContentValues().apply {
            put("title", title.trim()); put("description", description.trim()); put("due_at", dueAt)
            put("kind", kind); put("amount", amount.coerceAtLeast(0)); put("done", 0)
        })
    }

    fun updateReminder(id: Long, title: String, description: String, dueAt: Long, kind: String, amount: Long): Boolean =
        writableDatabase.update("reminders", ContentValues().apply {
            put("title", title.trim()); put("description", description.trim()); put("due_at", dueAt); put("kind", kind); put("amount", amount.coerceAtLeast(0))
        }, "id=?", arrayOf(id.toString())) == 1

    fun setReminderDone(id: Long, done: Boolean): Boolean = writableDatabase.update(
        "reminders", ContentValues().apply { put("done", if (done) 1 else 0) }, "id=?", arrayOf(id.toString())
    ) == 1

    fun deleteReminder(id: Long): Boolean = writableDatabase.delete("reminders", "id=?", arrayOf(id.toString())) == 1

    fun reminder(id: Long): Reminder? = readableDatabase.rawQuery(
        "SELECT id,title,description,due_at,kind,amount,done FROM reminders WHERE id=?", arrayOf(id.toString())
    ).use { c -> if (c.moveToFirst()) Reminder(c.getLong(0), c.getString(1), c.getString(2), c.getLong(3), c.getString(4), c.getLong(5), c.getInt(6) == 1) else null }

    fun reminders(includeDone: Boolean = true, query: String = ""): List<Reminder> {
        val where = mutableListOf<String>()
        val args = mutableListOf<String>()
        if (!includeDone) where += "done=0"
        if (query.isNotBlank()) { where += "(title LIKE ? OR description LIKE ?)"; args += "%${query.trim()}%"; args += "%${query.trim()}%" }
        val sql = "SELECT id,title,description,due_at,kind,amount,done FROM reminders" +
            (if (where.isNotEmpty()) " WHERE ${where.joinToString(" AND ")}" else "") + " ORDER BY done ASC,due_at ASC"
        val out = mutableListOf<Reminder>()
        readableDatabase.rawQuery(sql, args.toTypedArray().takeIf { it.isNotEmpty() }).use { c ->
            while (c.moveToNext()) out += Reminder(c.getLong(0), c.getString(1), c.getString(2), c.getLong(3), c.getString(4), c.getLong(5), c.getInt(6) == 1)
        }
        return out
    }

    fun addCalendarNote(dayKey: String, title: String, text: String): Long {
        if (title.isBlank()) return -1L
        return writableDatabase.insert("calendar_notes", null, ContentValues().apply {
            put("day_key", dayKey); put("title", title.trim()); put("text", text.trim()); put("created_at", System.currentTimeMillis())
        })
    }

    fun calendarNotes(dayKey: String): List<CalendarNote> {
        val out = mutableListOf<CalendarNote>()
        readableDatabase.rawQuery("SELECT id,day_key,title,text,created_at FROM calendar_notes WHERE day_key=? ORDER BY id DESC", arrayOf(dayKey)).use { c ->
            while (c.moveToNext()) out += CalendarNote(c.getLong(0), c.getString(1), c.getString(2), c.getString(3), c.getLong(4))
        }
        return out
    }

    fun calendarNoteDays(monthPrefix: String): Set<String> {
        val out = mutableSetOf<String>()
        readableDatabase.rawQuery("SELECT DISTINCT day_key FROM calendar_notes WHERE day_key LIKE ?", arrayOf("$monthPrefix%" )).use { c -> while (c.moveToNext()) out += c.getString(0) }
        return out
    }

    fun deleteCalendarNote(id: Long): Boolean = writableDatabase.delete("calendar_notes", "id=?", arrayOf(id.toString())) == 1

    // ---------- هزینه ----------

    fun addExpense(title: String, amount: Long, note: String = ""): Long {
        if (title.isBlank() || amount <= 0) return -1L
        return writableDatabase.insert("expenses", null, ContentValues().apply {
            put("title", title.trim()); put("amount", amount); put("note", note.trim()); put("created_at", System.currentTimeMillis())
        })
    }

    fun expenses(): List<Expense> {
        val out = mutableListOf<Expense>()
        readableDatabase.rawQuery("SELECT id,title,amount,created_at,note FROM expenses ORDER BY id DESC", null).use { c ->
            while (c.moveToNext()) out += Expense(c.getLong(0), c.getString(1), c.getLong(2), c.getLong(3), c.getString(4))
        }
        return out
    }

    fun updateExpense(id: Long, title: String, amount: Long, note: String): Boolean = amount > 0L && title.isNotBlank() &&
        writableDatabase.update("expenses", ContentValues().apply { put("title", title.trim()); put("amount", amount); put("note", note.trim()) }, "id=?", arrayOf(id.toString())) == 1

    fun deleteExpense(id: Long): Boolean = writableDatabase.delete("expenses", "id=?", arrayOf(id.toString())) == 1

    // ---------- داشبورد و گزارش ----------

    fun dashboard(): Dashboard {
        val start = startOfToday()
        val end = start + 86_400_000L
        val today = report(start, end)
        val totalExpenses = scalar("SELECT COALESCE(SUM(amount),0) FROM expenses", null)
        val receivable = customers(includeArchived = true).sumOf { it.balance.coerceAtLeast(0L) }
        val creditor = customers(includeArchived = true).sumOf { (-it.balance).coerceAtLeast(0L) }
        return Dashboard(today.sales, today.receipts, receivable, totalExpenses, invoices().size, reminders(includeDone = false).size, today.expenses, today.grossProfit, today.netProfit, creditor)
    }

    fun report(startInclusive: Long, endExclusive: Long): FinancialReport {
        val args = arrayOf(startInclusive.toString(), endExclusive.toString())
        val sales = scalar("SELECT COALESCE(SUM(total),0) FROM invoices WHERE status='ACTIVE' AND created_at>=? AND created_at<?", args)
        val receipts = scalar("SELECT COALESCE(SUM(amount),0) FROM ledger WHERE type='PAYMENT' AND created_at>=? AND created_at<?", args)
        val expense = scalar("SELECT COALESCE(SUM(amount),0) FROM expenses WHERE created_at>=? AND created_at<?", args)
        val gross = scalar("SELECT COALESCE(SUM(ii.line_total-(ii.buy_price*ii.qty)),0) FROM invoice_items ii JOIN invoices i ON i.id=ii.invoice_id WHERE i.status='ACTIVE' AND i.created_at>=? AND i.created_at<?", args)
        return FinancialReport(sales, receipts, expense, gross, gross - expense)
    }

    fun currentMonthReport(): FinancialReport {
        val now = JalaliDate.today()
        val start = JalaliDate.toMillis(now.year, now.month, 1, 0, 0)
        val next = if (now.month == 12) JalaliDate.toMillis(now.year + 1, 1, 1, 0, 0) else JalaliDate.toMillis(now.year, now.month + 1, 1, 0, 0)
        return report(start, next)
    }

    private fun startOfToday(): Long = java.util.Calendar.getInstance().apply {
        set(java.util.Calendar.HOUR_OF_DAY, 0); set(java.util.Calendar.MINUTE, 0); set(java.util.Calendar.SECOND, 0); set(java.util.Calendar.MILLISECOND, 0)
    }.timeInMillis

    // ---------- Backup ----------

    fun databaseFile(): File = appContext.getDatabasePath(DB_NAME)

    fun checkpoint() {
        writableDatabase.rawQuery("PRAGMA wal_checkpoint(FULL)", null).use { it.moveToFirst() }
    }

    // ---------- ابزارهای داخلی ----------

    private fun scalar(sql: String, args: Array<String>?): Long = readableDatabase.rawQuery(sql, args).use { c -> if (c.moveToFirst()) c.getLong(0) else 0L }

    private fun newSalt(): String = ByteArray(16).also { SecureRandom().nextBytes(it) }.let { Base64.encodeToString(it, Base64.NO_WRAP) }

    private fun deriveKey(password: String, saltText: String): String {
        val salt = Base64.decode(saltText, Base64.NO_WRAP)
        val spec = PBEKeySpec(password.toCharArray(), salt, 120_000, 256)
        val algorithm = try { SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256") } catch (_: Exception) { SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1") }
        return Base64.encodeToString(algorithm.generateSecret(spec).encoded, Base64.NO_WRAP)
    }

    private fun legacyHash(value: String): String = MessageDigest.getInstance("SHA-256").digest(value.toByteArray()).joinToString("") { "%02x".format(it) }

    private fun secureEquals(a: String, b: String): Boolean = MessageDigest.isEqual(a.toByteArray(), b.toByteArray())

    companion object {
        private const val DB_NAME = "galata.db"
        private const val DB_VERSION = 2
    }
}

/** مدل مالک برنامه. */
data class OwnerProfile(val username: String, val name: String, val description: String, val photoUri: String)
/** مدل مشتری؛ balance مثبت بدهکار و منفی بستانکار است. */
data class Customer(val id: Long, val name: String, val phone: String, val balance: Long, val note: String = "", val archived: Boolean = false)
/** مدل کالا یا خدمت. */
data class Product(val id: Long, val name: String, val buyPrice: Long, val sellPrice: Long, val unit: String, val stockQty: Long = 0L, val trackStock: Boolean = false, val archived: Boolean = false)
/** یک ردیف سبد فروش. */
data class SaleLine(val product: Product, val qty: Int) { val total: Long get() = product.sellPrice * qty }
/** سربرگ فاکتور. */
data class Invoice(val id: Long, val customerId: Long, val customerName: String, val total: Long, val createdAt: Long, val status: String = "ACTIVE", val receivedAtSale: Long = 0L, val note: String = "")
/** قلم فاکتور با Snapshot قیمت‌ها. */
data class InvoiceItem(val name: String, val qty: Int, val unitPrice: Long, val total: Long, val buyPrice: Long = 0L, val productId: Long = 0L)
/** یک رویداد دفتر گردش به همراه اثر علامت‌دار و مانده پس از رویداد. */
data class LedgerRow(val type: String, val amount: Long, val note: String, val createdAt: Long, val signedAmount: Long = 0L, val balanceAfter: Long = 0L)
/** یادآور محلی. */
data class Reminder(val id: Long, val title: String, val description: String, val dueAt: Long, val kind: String = "GENERAL", val amount: Long = 0L, val done: Boolean = false)
/** یادداشت روی یک روز تقویم شمسی. */
data class CalendarNote(val id: Long, val dayKey: String, val title: String, val text: String, val createdAt: Long)
/** هزینه کسب‌وکار. */
data class Expense(val id: Long, val title: String, val amount: Long, val createdAt: Long, val note: String)
/** کارت‌های خلاصه Home؛ فیلدهای قدیمی حفظ شده‌اند تا Migration سورس هم سازگار باشد. */
data class Dashboard(val salesToday: Long, val receiptsToday: Long, val receivable: Long, val expenses: Long, val invoiceCount: Int, val reminderCount: Int, val expensesToday: Long = 0L, val grossProfitToday: Long = 0L, val netProfitToday: Long = 0L, val customerCredit: Long = 0L)
/** گزارش مالی یک بازه. */
data class FinancialReport(val sales: Long, val receipts: Long, val expenses: Long, val grossProfit: Long, val netProfit: Long)
