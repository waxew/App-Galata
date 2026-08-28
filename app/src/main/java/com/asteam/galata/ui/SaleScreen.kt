package com.asteam.galata.ui

import android.graphics.Color
import android.graphics.Typeface
import android.text.Editable
import android.text.TextWatcher
import android.view.Gravity
import android.view.View
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import com.asteam.galata.Customer
import com.asteam.galata.MainActivity
import com.asteam.galata.Product
import com.asteam.galata.SaleLine

/**
 * صفحه Activity_sell مطابق فلو و چیدمان Miro.
 * سمت راست لیست محصولات و سمت چپ لیست مشتری‌ها قرار می‌گیرد؛ هر دو جست‌وجو دارند.
 */
class SaleScreen : GalataScreen {
    override fun build(host: MainActivity): View {
        val page = ScreenUi.page(host, "فروش", "ثبت سفارش کافه", CafeIconView.Icon.COFFEE)
        val content = ScreenUi.content(host)
        val allCustomers = host.db.customers().sortedBy { it.name }
        val allProducts = host.db.products().sortedBy { it.name }

        if (allCustomers.isEmpty() || allProducts.isEmpty()) {
            content.addView(ScreenUi.empty(host, "برای ثبت سفارش ابتدا مشتری و محصول ثبت کنید"))
            page.addView(ScreenUi.scroll(host, content), LinearLayout.LayoutParams(-1, 0, 1f))
            return page
        }

        var selectedCustomer: Customer = allCustomers.first()
        val cart = mutableListOf<SaleLine>()

        // وضعیت مشتری انتخاب‌شده بالای فرم.
        val selectedCustomerText = TextView(host).apply {
            text = "مشتری انتخاب‌شده: ${selectedCustomer.name}"
            textSize = 14f
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.END
            setTextColor(CafeTheme.espresso)
            setPadding(CafeTheme.dp(host, 14), CafeTheme.dp(host, 10), CafeTheme.dp(host, 14), CafeTheme.dp(host, 10))
        }
        content.addView(selectedCustomerText)

        // دو ستون اصلی مطابق فریم Activity_sell در Miro.
        val columns = LinearLayout(host).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutDirection = View.LAYOUT_DIRECTION_RTL
            setPadding(CafeTheme.dp(host, 8), 0, CafeTheme.dp(host, 8), 0)
        }

        val productsColumn = listColumn(host, "🔎  لیست محصولات")
        val customersColumn = listColumn(host, "🔎  لیست مشتری ها")
        columns.addView(productsColumn.root, LinearLayout.LayoutParams(0, CafeTheme.dp(host, 330), 1f).apply { setMargins(4, 0, 4, 0) })
        columns.addView(customersColumn.root, LinearLayout.LayoutParams(0, CafeTheme.dp(host, 330), 1f).apply { setMargins(4, 0, 4, 0) })
        content.addView(columns)

        val preview = TextView(host).apply {
            gravity = Gravity.END
            textSize = 13f
            setTextColor(CafeTheme.ink)
            setPadding(CafeTheme.dp(host, 14), CafeTheme.dp(host, 12), CafeTheme.dp(host, 14), CafeTheme.dp(host, 12))
            background = CafeTheme.rounded(Color.rgb(198, 220, 255), CafeTheme.dp(host, 10).toFloat())
            layoutParams = LinearLayout.LayoutParams(-1, -2).apply { setMargins(12, 10, 12, 6) }
        }

        fun refreshPreview() {
            val total = cart.sumOf { it.total }
            preview.text = if (cart.isEmpty()) {
                "نام مشتری: ${selectedCustomer.name}\nخدمات دریافتی: —\nفی کل: ۰ تومان"
            } else {
                "نام مشتری: ${selectedCustomer.name}\n" +
                    "خدمات دریافتی: ${cart.joinToString(" ٫ ") { it.product.name }}\n" +
                    "فی کل: ${ScreenUi.money(total)}"
            }
        }
        refreshPreview()

        fun renderProducts(filter: String = "") {
            productsColumn.items.removeAllViews()
            allProducts.filter { it.name.contains(filter, ignoreCase = true) }.forEach { product ->
                productsColumn.items.addView(listRow(host, "＋  ${product.name}", ScreenUi.money(product.sellPrice)) {
                    val existingIndex = cart.indexOfFirst { it.product.id == product.id }
                    if (existingIndex >= 0) {
                        val old = cart[existingIndex]
                        cart[existingIndex] = SaleLine(old.product, old.qty + 1)
                    } else {
                        cart += SaleLine(product, 1)
                    }
                    refreshPreview()
                })
            }
        }

        fun renderCustomers(filter: String = "") {
            customersColumn.items.removeAllViews()
            allCustomers.filter { it.name.contains(filter, ignoreCase = true) }.forEach { customer ->
                customersColumn.items.addView(listRow(host, customer.name, "مانده: ${ScreenUi.money(customer.balance)}") {
                    selectedCustomer = customer
                    selectedCustomerText.text = "مشتری انتخاب‌شده: ${customer.name}"
                    refreshPreview()
                })
            }
        }

        renderProducts()
        renderCustomers()
        productsColumn.search.addTextChangedListener(SimpleWatcher { renderProducts(it) })
        customersColumn.search.addTextChangedListener(SimpleWatcher { renderCustomers(it) })

        content.addView(preview)
        content.addView(TextView(host).apply {
            text = "مبلغ کل این فاکتور پس از ثبت به بدهی مشتری اضافه می‌شود."
            textSize = 11.5f
            gravity = Gravity.END
            setTextColor(Color.rgb(189, 10, 10))
            setPadding(16, 2, 16, 4)
        })
        content.addView(ScreenUi.primaryButton(host, "ثبت سفارش") {
            if (cart.isEmpty()) {
                ScreenUi.toast(host, "حداقل یک محصول به سفارش اضافه کنید")
            } else {
                val id = host.db.saveInvoice(selectedCustomer, cart)
                ScreenUi.toast(host, "فاکتور ${ScreenUi.fa(id.toString())} ثبت شد")
                host.navigate(MainActivity.Route.INVOICES)
            }
        })

        page.addView(ScreenUi.scroll(host, content), LinearLayout.LayoutParams(-1, 0, 1f))
        return page
    }

    private data class ColumnViews(val root: LinearLayout, val search: EditText, val items: LinearLayout)

    /** ساخت یک ستون جست‌وجوشونده برای مشتری یا محصول. */
    private fun listColumn(host: MainActivity, title: String): ColumnViews {
        val root = LinearLayout(host).apply {
            orientation = LinearLayout.VERTICAL
            background = CafeTheme.rounded(Color.rgb(198, 220, 255), CafeTheme.dp(host, 10).toFloat())
            setPadding(6, 6, 6, 6)
        }
        root.addView(TextView(host).apply {
            text = title
            textSize = 12.5f
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
            setTextColor(CafeTheme.ink)
            setPadding(4, 5, 4, 5)
        })
        val search = EditText(host).apply {
            hint = "جستجو"
            textSize = 12f
            gravity = Gravity.END
            setSingleLine(true)
            background = CafeTheme.rounded(Color.WHITE, CafeTheme.dp(host, 9).toFloat())
            setPadding(10, 5, 10, 5)
        }
        root.addView(search, LinearLayout.LayoutParams(-1, CafeTheme.dp(host, 42)))
        val items = LinearLayout(host).apply { orientation = LinearLayout.VERTICAL }
        root.addView(ScrollView(host).apply { addView(items) }, LinearLayout.LayoutParams(-1, 0, 1f))
        return ColumnViews(root, search, items)
    }

    private fun listRow(host: MainActivity, title: String, subtitle: String, click: () -> Unit): View = LinearLayout(host).apply {
        orientation = LinearLayout.VERTICAL
        gravity = Gravity.END
        setPadding(8, 8, 8, 8)
        background = CafeTheme.rounded(Color.WHITE, CafeTheme.dp(host, 8).toFloat())
        layoutParams = LinearLayout.LayoutParams(-1, -2).apply { setMargins(2, 3, 2, 3) }
        addView(TextView(host).apply { text = title; textSize = 12.5f; typeface = Typeface.DEFAULT_BOLD; gravity = Gravity.END; setTextColor(CafeTheme.ink) })
        addView(TextView(host).apply { text = subtitle; textSize = 10.5f; gravity = Gravity.END; setTextColor(CafeTheme.mocha) })
        setOnClickListener { click() }
    }

    /** TextWatcher کوچک برای جلوگیری از تکرار کد فیلتر دو لیست. */
    private class SimpleWatcher(private val onChanged: (String) -> Unit) : TextWatcher {
        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = onChanged(s?.toString().orEmpty())
        override fun afterTextChanged(s: Editable?) = Unit
    }
}
