package com.asteam.galata.ui

import android.graphics.Color
import android.graphics.Typeface
import android.text.Editable
import android.text.TextWatcher
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import com.asteam.galata.Customer
import com.asteam.galata.MainActivity
import com.asteam.galata.Product
import com.asteam.galata.SaleLine

/**
 * Activity_sell واقعی: انتخاب مشتری، جستجوی کالا، سبد با تعداد، پیش‌نمایش، دریافتی لحظه‌ای و ثبت تراکنشی فاکتور.
 * فروش روزانه از کل فاکتور و دریافتی روزانه فقط از پول دریافت‌شده محاسبه می‌شود.
 */
class SaleScreen : GalataScreen {
    override fun build(host: MainActivity): View {
        val page = ScreenUi.page(host, "فروش", "ثبت سفارش و فاکتور", CafeIconView.Icon.COFFEE)
        val content = ScreenUi.content(host)
        val allCustomers = host.db.customers().sortedBy { it.name }
        val allProducts = host.db.products().sortedBy { it.name }

        if (allCustomers.isEmpty() || allProducts.isEmpty()) {
            content.addView(ScreenUi.empty(host, "برای ثبت فروش، حداقل یک مشتری و یک کالا/خدمت فعال لازم است"))
            content.addView(ScreenUi.primaryButton(host, "مدیریت مشتری‌ها") { host.navigate(MainActivity.Route.CUSTOMERS) })
            content.addView(ScreenUi.primaryButton(host, "مدیریت کالا و خدمات") { host.navigate(MainActivity.Route.PRODUCTS) })
            page.addView(ScreenUi.scroll(host, content), LinearLayout.LayoutParams(-1, 0, 1f))
            return page
        }

        var selectedCustomer: Customer = allCustomers.first()
        val cart = mutableListOf<SaleLine>()
        val selectedCustomerText = TextView(host).apply {
            textSize = 14f; typeface = Typeface.DEFAULT_BOLD; gravity = Gravity.END; setTextColor(CafeTheme.espresso)
            setPadding(CafeTheme.dp(host, 14), CafeTheme.dp(host, 10), CafeTheme.dp(host, 14), CafeTheme.dp(host, 10))
        }
        content.addView(selectedCustomerText)

        val columns = LinearLayout(host).apply {
            orientation = LinearLayout.HORIZONTAL; layoutDirection = View.LAYOUT_DIRECTION_RTL
            setPadding(CafeTheme.dp(host, 8), 0, CafeTheme.dp(host, 8), 0)
        }
        val productsColumn = listColumn(host, "🔎  لیست محصولات")
        val customersColumn = listColumn(host, "🔎  لیست مشتری‌ها")
        columns.addView(productsColumn.root, LinearLayout.LayoutParams(0, CafeTheme.dp(host, 330), 1f).apply { setMargins(4,0,4,0) })
        columns.addView(customersColumn.root, LinearLayout.LayoutParams(0, CafeTheme.dp(host, 330), 1f).apply { setMargins(4,0,4,0) })
        content.addView(columns)

        val preview = TextView(host).apply {
            gravity = Gravity.END; textSize = 13f; setTextColor(CafeTheme.ink)
            setPadding(CafeTheme.dp(host,14), CafeTheme.dp(host,12), CafeTheme.dp(host,14), CafeTheme.dp(host,12))
            background = CafeTheme.rounded(Color.rgb(198,220,255), CafeTheme.dp(host,10).toFloat())
            layoutParams = LinearLayout.LayoutParams(-1,-2).apply { setMargins(12,10,12,6) }
        }
        val cartRows = LinearLayout(host).apply { orientation = LinearLayout.VERTICAL }
        val received = ScreenUi.input(host, "مبلغ دریافتی همین لحظه؛ در صورت نسیه صفر", true).apply { setText("0") }
        val note = ScreenUi.input(host, "توضیح فاکتور - اختیاری")

        fun refresh() {
            val total = cart.sumOf { it.total }
            selectedCustomerText.text = "مشتری انتخاب‌شده: ${selectedCustomer.name} • مانده فعلی: ${ScreenUi.money(selectedCustomer.balance.coerceAtLeast(0L))}"
            preview.text = if (cart.isEmpty()) {
                "نام مشتری: ${selectedCustomer.name}\nاقلام: —\nجمع کل: ۰ تومان"
            } else {
                "نام مشتری: ${selectedCustomer.name}\nاقلام: ${cart.joinToString(" ٫ ") { "${it.product.name} × ${ScreenUi.fa(it.qty.toString())}" }}\nجمع کل: ${ScreenUi.money(total)}"
            }
            cartRows.removeAllViews()
            cart.forEachIndexed { index, line ->
                cartRows.addView(LinearLayout(host).apply {
                    orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL; layoutDirection = View.LAYOUT_DIRECTION_RTL
                    setPadding(12,5,12,5)
                    addView(TextView(host).apply {
                        text = "${line.product.name} × ${ScreenUi.fa(line.qty.toString())} — ${ScreenUi.money(line.total)}"
                        gravity = Gravity.END; setTextColor(CafeTheme.ink); textSize = 12.5f
                    }, LinearLayout.LayoutParams(0,-2,1f))
                    addView(Button(host).apply { text = "+"; isAllCaps = false; setOnClickListener { cart[index] = SaleLine(line.product, line.qty + 1); refresh() } }, LinearLayout.LayoutParams(CafeTheme.dp(host,48), CafeTheme.dp(host,42)))
                    addView(Button(host).apply { text = "−"; isAllCaps = false; setOnClickListener {
                        if (line.qty <= 1) cart.removeAt(index) else cart[index] = SaleLine(line.product, line.qty - 1)
                        refresh()
                    } }, LinearLayout.LayoutParams(CafeTheme.dp(host,48), CafeTheme.dp(host,42)))
                })
            }
        }

        fun renderProducts(filter: String = "") {
            productsColumn.items.removeAllViews()
            allProducts.filter { it.name.contains(filter, true) }.forEach { product ->
                val stock = if (product.trackStock) "موجودی ${ScreenUi.fa(product.stockQty.toString())}" else "نامحدود"
                productsColumn.items.addView(listRow(host, "＋ ${product.name}", "${ScreenUi.money(product.sellPrice)} • $stock") {
                    val idx = cart.indexOfFirst { it.product.id == product.id }
                    val qty = if (idx >= 0) cart[idx].qty + 1 else 1
                    if (product.trackStock && qty > product.stockQty) ScreenUi.toast(host, "موجودی ${product.name} کافی نیست")
                    else {
                        if (idx >= 0) cart[idx] = SaleLine(product, qty) else cart += SaleLine(product, 1)
                        refresh()
                    }
                })
            }
        }

        fun renderCustomers(filter: String = "") {
            customersColumn.items.removeAllViews()
            allCustomers.filter { it.name.contains(filter, true) || it.phone.contains(filter) }.forEach { customer ->
                customersColumn.items.addView(listRow(host, customer.name, "مانده: ${ScreenUi.money(customer.balance.coerceAtLeast(0L))}") {
                    selectedCustomer = customer; refresh()
                })
            }
        }

        renderProducts(); renderCustomers(); refresh()
        productsColumn.search.addTextChangedListener(SimpleWatcher { renderProducts(it) })
        customersColumn.search.addTextChangedListener(SimpleWatcher { renderCustomers(it) })
        content.addView(preview)
        content.addView(cartRows)
        content.addView(received)
        content.addView(note)
        content.addView(TextView(host).apply {
            text = "کل مبلغ فاکتور = فروش؛ فقط مبلغی که واقعاً دریافت شده = دریافتی. باقی‌مانده به بدهی مشتری اضافه می‌شود."
            textSize = 11.5f; gravity = Gravity.END; setTextColor(CafeTheme.danger); setPadding(18,6,18,4)
        })
        content.addView(ScreenUi.primaryButton(host, "ثبت سفارش") {
            val total = cart.sumOf { it.total }
            val paid = ScreenUi.num(received.text.toString())
            when {
                cart.isEmpty() -> ScreenUi.toast(host, "حداقل یک محصول به سبد اضافه کنید")
                paid > total -> ScreenUi.toast(host, "دریافتی نمی‌تواند از مبلغ فاکتور بیشتر باشد")
                else -> try {
                    val id = host.db.saveInvoice(selectedCustomer, cart.toList(), paid, note.text.toString())
                    ScreenUi.toast(host, "فاکتور ${ScreenUi.fa(id.toString())} ثبت شد")
                    host.navigate(MainActivity.Route.INVOICES)
                } catch (e: Exception) {
                    ScreenUi.toast(host, e.message ?: "ثبت فاکتور انجام نشد")
                }
            }
        })
        page.addView(ScreenUi.scroll(host, content), LinearLayout.LayoutParams(-1,0,1f))
        return page
    }

    private data class ColumnViews(val root: LinearLayout, val search: EditText, val items: LinearLayout)
    private fun listColumn(host: MainActivity, title: String): ColumnViews {
        val root = LinearLayout(host).apply {
            orientation = LinearLayout.VERTICAL; background = CafeTheme.rounded(Color.rgb(198,220,255), CafeTheme.dp(host,10).toFloat()); setPadding(6,6,6,6)
        }
        root.addView(TextView(host).apply { text = title; textSize = 12.5f; typeface = Typeface.DEFAULT_BOLD; gravity = Gravity.CENTER; setTextColor(CafeTheme.ink); setPadding(4,5,4,5) })
        val search = EditText(host).apply { hint = "جستجو"; textSize = 12f; gravity = Gravity.END; setSingleLine(true); background = CafeTheme.rounded(Color.WHITE, CafeTheme.dp(host,9).toFloat()); setPadding(10,5,10,5) }
        root.addView(search, LinearLayout.LayoutParams(-1,CafeTheme.dp(host,42)))
        val items = LinearLayout(host).apply { orientation = LinearLayout.VERTICAL }
        root.addView(ScrollView(host).apply { addView(items) }, LinearLayout.LayoutParams(-1,0,1f))
        return ColumnViews(root, search, items)
    }
    private fun listRow(host: MainActivity, title: String, subtitle: String, click: () -> Unit): View = LinearLayout(host).apply {
        orientation = LinearLayout.VERTICAL; gravity = Gravity.END; setPadding(8,8,8,8); background = CafeTheme.rounded(Color.WHITE, CafeTheme.dp(host,8).toFloat())
        layoutParams = LinearLayout.LayoutParams(-1,-2).apply { setMargins(2,3,2,3) }
        addView(TextView(host).apply { text = title; textSize = 12.5f; typeface = Typeface.DEFAULT_BOLD; gravity = Gravity.END; setTextColor(CafeTheme.ink) })
        addView(TextView(host).apply { text = subtitle; textSize = 10.5f; gravity = Gravity.END; setTextColor(CafeTheme.mocha) })
        setOnClickListener { click() }
    }
    private class SimpleWatcher(private val onChanged: (String) -> Unit) : TextWatcher {
        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = onChanged(s?.toString().orEmpty())
        override fun afterTextChanged(s: Editable?) = Unit
    }
}
