package com.asteam.galata

import android.app.Activity
import android.app.AlertDialog
import android.graphics.Color
import android.os.Bundle
import android.text.InputType
import android.view.Gravity
import android.view.View
import android.widget.*
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * رابط اولیه گالاتا برای تست منطق اصلی روی گوشی.
 * همه بخش‌ها فعلاً برنامه‌نویسی شده‌اند تا MVP سریع بالا بیاید؛ ظاهر نهایی بعداً دقیق‌تر می‌شود.
 */
class MainActivity : Activity() {
    private lateinit var db: GalataDb
    private lateinit var root: LinearLayout
    private var loggedIn = false
    private var onHome = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        db = GalataDb(this)
        root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutDirection = View.LAYOUT_DIRECTION_RTL
            setBackgroundColor(Color.rgb(247, 248, 250))
        }
        window.decorView.layoutDirection = View.LAYOUT_DIRECTION_RTL
        setContentView(root)
        showAuth()
    }

    /** برگشت از هر بخش به خانه انجام می‌شود و از خانه امکان خروج وجود دارد. */
    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        if (loggedIn && !onHome) showHome() else super.onBackPressed()
    }

    private fun showAuth() {
        loggedIn = false
        onHome = false
        root.removeAllViews()
        header("گالاتا", if (db.hasOwner()) "ورود مالک" else "ثبت‌نام اولیه")
        val user = input("نام کاربری")
        val pass = input("رمز عبور").apply { inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD }
        root.addView(user); root.addView(pass)
        if (!db.hasOwner()) {
            val name = input("نام صاحب کسب‌وکار")
            root.addView(name)
            root.addView(button("ساخت حساب") {
                if (user.text.isBlank() || pass.text.isBlank()) toast("نام کاربری و رمز عبور الزامی است")
                else if (db.registerOwner(user.text.toString(), pass.text.toString(), name.text.toString())) {
                    loggedIn = true; showHome()
                }
            })
        } else {
            root.addView(button("ورود") {
                if (db.login(user.text.toString(), pass.text.toString())) { loggedIn = true; showHome() }
                else toast("نام کاربری یا رمز عبور نادرست است")
            })
        }
    }

    private fun showHome() {
        loggedIn = true
        onHome = true
        val d = db.dashboard()
        reset("گالاتا", "${db.ownerName()} — مدیریت آفلاین کسب‌وکار")
        val a = horizontal(); a.addView(stat("فروش امروز", money(d.salesToday))); a.addView(stat("دریافتی امروز", money(d.receiptsToday))); root.addView(a)
        val b = horizontal(); b.addView(stat("مطالبات", money(d.receivable))); b.addView(stat("هزینه‌ها", money(d.expenses))); root.addView(b)
        root.addView(title("دسترسی سریع"))
        root.addView(button("فروش") { showSale() })
        root.addView(button("تسویه مشتری") { showPayments() })
        root.addView(button("فاکتورها (${fa(d.invoiceCount.toString())})") { showInvoices() })
        root.addView(button("یادآورها (${fa(d.reminderCount.toString())})") { showReminders() })
    }

    /** منوی اصلی فعلاً به‌صورت پنجره است و در نسخه ظاهری بعدی Drawer واقعی می‌شود. */
    private fun openMenu() {
        val items = arrayOf("خانه", "صورت‌حساب", "لیست مشتری‌ها", "لیست کالا / خدمات", "یادآور", "هزینه‌ها", "درباره نرم‌افزار", "تماس با ما", "خروج")
        AlertDialog.Builder(this).setTitle("منوی گالاتا").setItems(items) { _, i ->
            when (i) {
                0 -> showHome(); 1 -> showInvoices(); 2 -> showCustomers(); 3 -> showProducts(); 4 -> showReminders(); 5 -> showExpenses()
                6 -> info("درباره نرم‌افزار", "گالاتا نرم‌افزار آفلاین مدیریت فروش، مشتری، فاکتور و یادآوری است.\nنسخه ۰.۱.۰")
                7 -> info("تماس با ما", "گروه توسعه فناوری و نرم افزاری as Team\nAS.Support.info@Gmail.com")
                8 -> showAuth()
            }
        }.show()
    }

    private fun showCustomers() {
        onHome = false
        reset("مشتری‌ها", "مدیریت مشتری و مانده حساب")
        root.addView(button("+ افزودن مشتری") { customerDialog() })
        db.customers().forEach { c ->
            val v = card("${c.name}\n${c.phone}\nمانده: ${money(c.balance)}")
            v.setOnClickListener { showLedger(c) }
            root.addView(v)
        }
    }

    private fun customerDialog() {
        val name = input("نام و نام خانوادگی")
        val phone = input("شماره تماس").apply { inputType = InputType.TYPE_CLASS_PHONE }
        AlertDialog.Builder(this).setTitle("مشتری جدید").setView(form(name, phone)).setNegativeButton("انصراف", null).setPositiveButton("ذخیره") { _, _ ->
            if (name.text.isBlank()) toast("نام الزامی است") else { db.addCustomer(name.text.toString(), fa(phone.text.toString())); showCustomers() }
        }.show()
    }

    private fun showLedger(c: Customer) {
        onHome = false
        reset("صورت‌حساب ${c.name}", "مانده فعلی: ${money(db.balance(c.id))}")
        db.ledger(c.id).forEach { r ->
            root.addView(card("${if (r.type == "DEBT") "بدهکار" else "پرداخت"} — ${money(r.amount)}\n${r.note}\n${date(r.createdAt)}"))
        }
    }

    private fun showProducts() {
        onHome = false
        reset("کالا / خدمات", "قیمت خرید و فروش")
        root.addView(button("+ افزودن کالا یا خدمت") { productDialog() })
        db.products().forEach { p -> root.addView(card("${p.name}\nفروش: ${money(p.sellPrice)} — خرید: ${money(p.buyPrice)}\nواحد: ${p.unit}")) }
    }

    private fun productDialog() {
        val name = input("نام")
        val buy = input("قیمت خرید").apply { inputType = InputType.TYPE_CLASS_NUMBER }
        val sell = input("قیمت فروش").apply { inputType = InputType.TYPE_CLASS_NUMBER }
        val unit = input("واحد")
        AlertDialog.Builder(this).setTitle("کالا / خدمت جدید").setView(form(name, buy, sell, unit)).setNegativeButton("انصراف", null).setPositiveButton("ذخیره") { _, _ ->
            val s = num(sell.text.toString())
            if (name.text.isBlank() || s <= 0) toast("نام و قیمت فروش معتبر وارد کنید")
            else { db.addProduct(name.text.toString(), num(buy.text.toString()), s, unit.text.toString().ifBlank { "عدد" }); showProducts() }
        }.show()
    }

    private fun showSale() {
        onHome = false
        reset("فروش", "انتخاب مشتری و چند قلم کالا / خدمت")
        val customers = db.customers(); val products = db.products()
        if (customers.isEmpty() || products.isEmpty()) { root.addView(card("ابتدا مشتری و کالا ثبت کنید.")); return }
        val customerSpinner = Spinner(this).apply { adapter = ArrayAdapter(this@MainActivity, android.R.layout.simple_spinner_dropdown_item, customers.map { it.name }) }
        val productSpinner = Spinner(this).apply { adapter = ArrayAdapter(this@MainActivity, android.R.layout.simple_spinner_dropdown_item, products.map { "${it.name} — ${money(it.sellPrice)}" }) }
        val qty = input("تعداد").apply { inputType = InputType.TYPE_CLASS_NUMBER; setText("۱") }
        root.addView(label("مشتری")); root.addView(customerSpinner); root.addView(label("کالا / خدمت")); root.addView(productSpinner); root.addView(qty)
        val cart = mutableListOf<SaleLine>()
        val preview = card("")
        fun refresh() { preview.text = if (cart.isEmpty()) "فاکتور خالی است" else cart.joinToString("\n") { "${it.product.name} × ${fa(it.qty.toString())} = ${money(it.total)}" } + "\n\nجمع: ${money(cart.sumOf { it.total })}" }
        refresh()
        root.addView(button("افزودن به فاکتور") {
            val q = num(qty.text.toString()).toInt()
            if (q <= 0) toast("تعداد معتبر نیست") else { cart += SaleLine(products[productSpinner.selectedItemPosition], q); refresh() }
        })
        root.addView(preview)
        root.addView(button("ثبت سفارش") {
            if (cart.isEmpty()) toast("فاکتور خالی است") else {
                val id = db.saveInvoice(customers[customerSpinner.selectedItemPosition], cart)
                toast("فاکتور ${fa(id.toString())} ثبت شد")
                showInvoices()
            }
        })
    }

    private fun showInvoices() {
        onHome = false
        reset("فاکتورها", "آرشیو فروش")
        db.invoices().forEach { inv ->
            val names = db.invoiceItems(inv.id).joinToString(" ٫ ") { it.name }
            val v = card("فاکتور ${fa(inv.id.toString())} — ${inv.customerName}\n$names\n${money(inv.total)}\n${date(inv.createdAt)}")
            v.setOnClickListener { showInvoiceDetail(inv) }
            root.addView(v)
        }
    }

    private fun showInvoiceDetail(inv: Invoice) {
        val text = db.invoiceItems(inv.id).joinToString("\n") { "${it.name} × ${fa(it.qty.toString())} — ${money(it.total)}" }
        info("فاکتور ${fa(inv.id.toString())}", "مشتری: ${inv.customerName}\n$text\n\nجمع کل: ${money(inv.total)}")
    }

    private fun showPayments() {
        onHome = false
        reset("تسویه مشتری", "پرداخت کامل یا جزئی")
        db.customers().filter { it.balance > 0 }.forEach { c ->
            val v = card("${c.name}\nبدهی: ${money(c.balance)}\nبرای ثبت پرداخت لمس کنید")
            v.setOnClickListener { paymentDialog(c) }
            root.addView(v)
        }
    }

    private fun paymentDialog(c: Customer) {
        val amount = input("مبلغ پرداخت").apply { inputType = InputType.TYPE_CLASS_NUMBER; setText(fa(c.balance.toString())) }
        val note = input("توضیح")
        AlertDialog.Builder(this).setTitle("تسویه ${c.name}").setView(form(amount, note)).setNegativeButton("انصراف", null).setPositiveButton("ثبت") { _, _ ->
            val a = num(amount.text.toString()); val bal = db.balance(c.id)
            if (a <= 0 || a > bal) toast("مبلغ باید کمتر یا مساوی مانده بدهی باشد") else { db.addPayment(c.id, a, note.text.toString()); showPayments() }
        }.show()
    }

    private fun showReminders() {
        onHome = false
        reset("یادآورها", "چک، قسط، قرار و یادداشت")
        root.addView(button("+ یادآور جدید") { reminderDialog() })
        db.reminders().forEach { r -> root.addView(card("${r.title}\n${r.description}\n${date(r.dueAt)}")) }
    }

    private fun reminderDialog() {
        val title = input("عنوان")
        val desc = input("توضیح")
        val days = input("چند روز دیگر؟").apply { inputType = InputType.TYPE_CLASS_NUMBER; setText("۱") }
        AlertDialog.Builder(this).setTitle("یادآور جدید").setView(form(title, desc, days)).setNegativeButton("انصراف", null).setPositiveButton("ثبت") { _, _ ->
            if (title.text.isBlank()) toast("عنوان الزامی است") else {
                val due = System.currentTimeMillis() + num(days.text.toString()).coerceAtLeast(0) * 24 * 60 * 60 * 1000
                db.addReminder(title.text.toString(), desc.text.toString(), due); showReminders()
            }
        }.show()
    }

    private fun showExpenses() {
        onHome = false
        reset("هزینه‌ها", "ثبت پول خروجی کسب‌وکار")
        root.addView(button("+ ثبت هزینه") {
            val title = input("عنوان هزینه")
            val amount = input("مبلغ").apply { inputType = InputType.TYPE_CLASS_NUMBER }
            AlertDialog.Builder(this).setTitle("هزینه جدید").setView(form(title, amount)).setNegativeButton("انصراف", null).setPositiveButton("ثبت") { _, _ ->
                val a = num(amount.text.toString())
                if (title.text.isBlank() || a <= 0) toast("عنوان و مبلغ معتبر وارد کنید") else { db.addExpense(title.text.toString(), a); showHome() }
            }.show()
        })
    }

    // ---------- ابزارهای رابط ----------
    private fun reset(t: String, s: String) { root.removeAllViews(); header(t, s); if (loggedIn && !onHome) root.addView(button("خانه") { showHome() }) }
    private fun header(t: String, s: String) {
        val h = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL; setPadding(dp(12), dp(12), dp(12), dp(12)); setBackgroundColor(Color.rgb(124, 32, 48)) }
        if (loggedIn) h.addView(Button(this).apply { text = "☰"; isAllCaps = false; setOnClickListener { openMenu() } })
        h.addView(LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL; layoutParams = LinearLayout.LayoutParams(0, -2, 1f)
            addView(TextView(this@MainActivity).apply { text = t; textSize = 24f; setTextColor(Color.WHITE); gravity = Gravity.END })
            addView(TextView(this@MainActivity).apply { text = s; textSize = 12f; setTextColor(Color.LTGRAY); gravity = Gravity.END })
        })
        root.addView(h)
    }
    private fun button(v: String, action: () -> Unit) = Button(this).apply { text = v; isAllCaps = false; textSize = 16f; setOnClickListener { action() }; layoutParams = LinearLayout.LayoutParams(-1, dp(52)).apply { setMargins(dp(14), dp(5), dp(14), dp(5)) } }
    private fun card(v: String) = TextView(this).apply { text = v; textSize = 16f; gravity = Gravity.END; setPadding(dp(16), dp(14), dp(16), dp(14)); setBackgroundColor(Color.WHITE); layoutParams = LinearLayout.LayoutParams(-1, -2).apply { setMargins(dp(14), dp(4), dp(14), dp(4)) } }
    private fun title(v: String) = TextView(this).apply { text = v; textSize = 18f; gravity = Gravity.END; setPadding(dp(16), dp(16), dp(16), dp(6)); setTextColor(Color.rgb(124, 32, 48)) }
    private fun label(v: String) = TextView(this).apply { text = v; gravity = Gravity.END; setPadding(dp(16), dp(8), dp(16), 0) }
    private fun input(h: String) = EditText(this).apply { hint = h; gravity = Gravity.END; textDirection = View.TEXT_DIRECTION_RTL; layoutParams = LinearLayout.LayoutParams(-1, -2).apply { setMargins(dp(14), dp(4), dp(14), dp(4)) } }
    private fun form(vararg views: View) = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; layoutDirection = View.LAYOUT_DIRECTION_RTL; setPadding(dp(14), dp(6), dp(14), 0); views.forEach { addView(it) } }
    private fun horizontal() = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; layoutDirection = View.LAYOUT_DIRECTION_RTL; setPadding(dp(8), dp(4), dp(8), 0) }
    private fun stat(l: String, v: String) = TextView(this).apply { text = "$l\n$v"; textSize = 15f; gravity = Gravity.CENTER; setBackgroundColor(Color.WHITE); setPadding(dp(6), dp(12), dp(6), dp(12)); layoutParams = LinearLayout.LayoutParams(0, dp(82), 1f).apply { setMargins(dp(4), dp(4), dp(4), dp(4)) } }
    private fun info(t: String, m: String) = AlertDialog.Builder(this).setTitle(t).setMessage(m).setPositiveButton("بستن", null).show()
    private fun money(v: Long) = fa(NumberFormat.getIntegerInstance(Locale.US).format(v).replace(",", "٫")) + " تومان"
    private fun date(v: Long) = fa(SimpleDateFormat("yyyy/MM/dd - HH:mm", Locale.US).format(Date(v)))
    private fun num(v: String): Long = v.replace('۰','0').replace('۱','1').replace('۲','2').replace('۳','3').replace('۴','4').replace('۵','5').replace('۶','6').replace('۷','7').replace('۸','8').replace('۹','9').replace("٫", "").replace(",", "").replace(" ", "").toLongOrNull() ?: 0L
    private fun fa(v: String) = v.replace('0','۰').replace('1','۱').replace('2','۲').replace('3','۳').replace('4','۴').replace('5','۵').replace('6','۶').replace('7','۷').replace('8','۸').replace('9','۹')
    private fun toast(v: String) = Toast.makeText(this, v, Toast.LENGTH_SHORT).show()
    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()
}
