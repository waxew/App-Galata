package com.asteam.galata.ui

import android.app.AlertDialog
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.CheckBox
import android.widget.LinearLayout
import com.asteam.galata.MainActivity
import com.asteam.galata.Product

/** مدیریت کالا و خدمات با CRUD، جستجو و موجودی اختیاری. */
class ProductsScreen : GalataScreen {
    override fun build(host: MainActivity): View {
        val page = ScreenUi.page(host, "کالا و خدمات", "قیمت خرید، فروش و موجودی", CafeIconView.Icon.PRODUCT)
        val content = ScreenUi.content(host)
        content.addView(ScreenUi.primaryButton(host, "+ افزودن محصول یا خدمت") { editProduct(host, null) })
        val search = ScreenUi.input(host, "🔎 جستجو در کالا و خدمات")
        content.addView(search)
        val list = LinearLayout(host).apply { orientation = LinearLayout.VERTICAL }
        content.addView(list)

        fun render(filter: String = "") {
            list.removeAllViews()
            val products = host.db.products(query = filter)
            if (products.isEmpty()) list.addView(ScreenUi.empty(host, "محصول یا خدمتی پیدا نشد"))
            products.forEach { product ->
                val stock = if (product.trackStock) "موجودی: ${ScreenUi.fa(product.stockQty.toString())} ${product.unit}" else "موجودی: نامحدود / خدمت"
                list.addView(CafeTheme.card(
                    host,
                    product.name,
                    "فروش: ${ScreenUi.money(product.sellPrice)}\nخرید: ${ScreenUi.money(product.buyPrice)}\n$stock",
                    CafeIconView.Icon.PRODUCT
                ).apply { setOnLongClickListener { showActions(host, product); true } })
            }
        }
        render()
        search.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = render(s?.toString().orEmpty())
            override fun afterTextChanged(s: Editable?) = Unit
        })
        content.addView(CafeTheme.card(host, "راهنما", "برای ویرایش یا حذف یک مورد، انگشت را روی کارت آن نگه دارید.", CafeIconView.Icon.ABOUT))
        page.addView(ScreenUi.scroll(host, content), LinearLayout.LayoutParams(-1, 0, 1f))
        return page
    }

    private fun editProduct(host: MainActivity, product: Product?) {
        val name = ScreenUi.input(host, "نام محصول / خدمت").apply { setText(product?.name.orEmpty()) }
        val buy = ScreenUi.input(host, "قیمت خرید", true).apply { if (product != null) setText(product.buyPrice.toString()) }
        val sell = ScreenUi.input(host, "قیمت فروش", true).apply { if (product != null) setText(product.sellPrice.toString()) }
        val unit = ScreenUi.input(host, "واحد؛ مثال: عدد، فنجان، خدمت").apply { setText(product?.unit ?: "عدد") }
        val stock = ScreenUi.input(host, "موجودی فعلی", true).apply { if (product != null) setText(product.stockQty.toString()) else setText("0") }
        val track = CheckBox(host).apply {
            text = "موجودی این مورد کنترل شود"
            isChecked = product?.trackStock ?: false
            setTextColor(CafeTheme.ink)
        }
        AlertDialog.Builder(host)
            .setTitle(if (product == null) "محصول / خدمت جدید" else "ویرایش محصول / خدمت")
            .setView(ScreenUi.form(host, name, buy, sell, unit, stock, track))
            .setNegativeButton("انصراف", null)
            .setPositiveButton("ذخیره") { _, _ ->
                val buyValue = ScreenUi.num(buy.text.toString())
                val sellValue = ScreenUi.num(sell.text.toString())
                val stockValue = ScreenUi.num(stock.text.toString())
                if (name.text.isBlank() || sellValue < 0) ScreenUi.toast(host, "نام و قیمت معتبر وارد کنید")
                else {
                    if (product == null) host.db.addProduct(name.text.toString(), buyValue, sellValue, unit.text.toString(), stockValue, track.isChecked)
                    else host.db.updateProduct(product.id, name.text.toString(), buyValue, sellValue, unit.text.toString(), stockValue, track.isChecked)
                    host.navigate(MainActivity.Route.PRODUCTS, addToHistory = false)
                }
            }.show()
    }

    private fun showActions(host: MainActivity, product: Product) {
        AlertDialog.Builder(host).setTitle(product.name)
            .setItems(arrayOf("ویرایش", "حذف / بایگانی")) { _, which ->
                if (which == 0) editProduct(host, product)
                else ScreenUi.confirm(host, "حذف ${product.name}", "اگر این مورد داخل فاکتور قبلی استفاده شده باشد فقط بایگانی می‌شود تا تاریخچه فاکتورها سالم بماند.", "ادامه") {
                    val result = host.db.removeProduct(product.id)
                    ScreenUi.toast(host, if (result == "ARCHIVED") "به دلیل سابقه فروش بایگانی شد" else "حذف شد")
                    host.navigate(MainActivity.Route.PRODUCTS, addToHistory = false)
                }
            }.show()
    }
}
