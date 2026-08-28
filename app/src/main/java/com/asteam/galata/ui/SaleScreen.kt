package com.asteam.galata.ui

import android.view.Gravity
import android.view.View
import android.widget.ArrayAdapter
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.TextView
import com.asteam.galata.MainActivity
import com.asteam.galata.SaleLine

/** صفحه فروش چندقلمی کافه؛ مشتری، محصول، تعداد و پیش‌نمایش فاکتور. */
class SaleScreen : GalataScreen {
    override fun build(host: MainActivity): View {
        val page = ScreenUi.page(host, "فروش کافه", "ثبت سفارش قهوه، چای و محصولات", CafeIconView.Icon.COFFEE)
        val content = ScreenUi.content(host)
        val customers = host.db.customers()
        val products = host.db.products()
        if (customers.isEmpty() || products.isEmpty()) {
            content.addView(ScreenUi.empty(host, "برای ثبت فروش ابتدا مشتری و محصول ثبت کنید"))
            page.addView(ScreenUi.scroll(host, content), LinearLayout.LayoutParams(-1, 0, 1f))
            return page
        }

        val customerSpinner = Spinner(host).apply {
            adapter = ArrayAdapter(host, android.R.layout.simple_spinner_dropdown_item, customers.map { it.name })
            layoutDirection = View.LAYOUT_DIRECTION_RTL
        }
        val productSpinner = Spinner(host).apply {
            adapter = ArrayAdapter(host, android.R.layout.simple_spinner_dropdown_item, products.map { "${it.name} — ${ScreenUi.money(it.sellPrice)}" })
            layoutDirection = View.LAYOUT_DIRECTION_RTL
        }
        val qty = ScreenUi.input(host, "تعداد", true).apply { setText("۱") }
        val cart = mutableListOf<SaleLine>()
        val preview = TextView(host).apply {
            gravity = Gravity.END
            textSize = 15f
            setTextColor(CafeTheme.ink)
            setPadding(CafeTheme.dp(host, 16), CafeTheme.dp(host, 14), CafeTheme.dp(host, 16), CafeTheme.dp(host, 14))
            background = CafeTheme.rounded(CafeTheme.foam, CafeTheme.dp(host, 16).toFloat())
            layoutParams = LinearLayout.LayoutParams(-1, -2).apply { setMargins(CafeTheme.dp(host,14),8,CafeTheme.dp(host,14),8) }
        }
        fun refresh() {
            preview.text = if (cart.isEmpty()) "هنوز آیتمی به سفارش اضافه نشده" else
                cart.joinToString("\n") { "☕ ${it.product.name} × ${ScreenUi.fa(it.qty.toString())} = ${ScreenUi.money(it.total)}" } +
                    "\n\nجمع سفارش: ${ScreenUi.money(cart.sumOf { it.total })}"
        }
        refresh()

        content.addView(CafeTheme.sectionTitle(host, "مشتری")); content.addView(customerSpinner)
        content.addView(CafeTheme.sectionTitle(host, "انتخاب از منوی کافه")); content.addView(productSpinner); content.addView(qty)
        content.addView(ScreenUi.primaryButton(host, "افزودن به سفارش") {
            val q = ScreenUi.num(qty.text.toString()).toInt()
            if (q <= 0) ScreenUi.toast(host, "تعداد معتبر وارد کنید")
            else { cart += SaleLine(products[productSpinner.selectedItemPosition], q); refresh() }
        })
        content.addView(preview)
        content.addView(ScreenUi.primaryButton(host, "ثبت نهایی فروش") {
            if (cart.isEmpty()) ScreenUi.toast(host, "سفارش خالی است")
            else {
                val id = host.db.saveInvoice(customers[customerSpinner.selectedItemPosition], cart)
                ScreenUi.toast(host, "فاکتور ${ScreenUi.fa(id.toString())} ثبت شد")
                host.navigate(MainActivity.Route.INVOICES)
            }
        })
        page.addView(ScreenUi.scroll(host, content), LinearLayout.LayoutParams(-1, 0, 1f))
        return page
    }
}
