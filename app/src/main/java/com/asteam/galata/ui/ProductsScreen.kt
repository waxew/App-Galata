package com.asteam.galata.ui

import android.app.AlertDialog
import android.view.View
import android.widget.LinearLayout
import com.asteam.galata.MainActivity

/** صفحه مستقل منوی کافه / محصولات و خدمات. */
class ProductsScreen : GalataScreen {
    override fun build(host: MainActivity): View {
        val page = ScreenUi.page(host, "منوی کافه", "قهوه، چای، نوشیدنی و خدمات", CafeIconView.Icon.PRODUCT)
        val content = ScreenUi.content(host)
        content.addView(ScreenUi.primaryButton(host, "+ افزودن محصول یا خدمت") { addProduct(host) })
        val products = host.db.products()
        if (products.isEmpty()) content.addView(ScreenUi.empty(host, "محصولات کافه را ثبت کنید"))
        products.forEach { p ->
            val icon = if (p.name.contains("چای")) CafeIconView.Icon.TEA else CafeIconView.Icon.COFFEE
            content.addView(CafeTheme.card(host, p.name, "فروش: ${ScreenUi.money(p.sellPrice)}\nخرید: ${ScreenUi.money(p.buyPrice)} • واحد: ${p.unit}", icon))
        }
        page.addView(ScreenUi.scroll(host, content), LinearLayout.LayoutParams(-1, 0, 1f))
        return page
    }

    private fun addProduct(host: MainActivity) {
        val name = ScreenUi.input(host, "نام محصول؛ مثال: لاته")
        val buy = ScreenUi.input(host, "قیمت خرید", true)
        val sell = ScreenUi.input(host, "قیمت فروش", true)
        val unit = ScreenUi.input(host, "واحد؛ مثال: فنجان")
        AlertDialog.Builder(host).setTitle("محصول جدید کافه").setView(ScreenUi.form(host, name, buy, sell, unit))
            .setNegativeButton("انصراف", null)
            .setPositiveButton("ذخیره") { _, _ ->
                val price = ScreenUi.num(sell.text.toString())
                if (name.text.isBlank() || price <= 0) ScreenUi.toast(host, "نام و قیمت فروش معتبر وارد کنید")
                else {
                    host.db.addProduct(name.text.toString(), ScreenUi.num(buy.text.toString()), price, unit.text.toString().ifBlank { "فنجان" })
                    host.navigate(MainActivity.Route.PRODUCTS)
                }
            }.show()
    }
}
