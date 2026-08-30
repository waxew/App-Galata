package com.asteam.galata.ui

import android.app.AlertDialog
import android.content.Intent
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.LinearLayout
import com.asteam.galata.Invoice
import com.asteam.galata.MainActivity

/** آرشیو فاکتور با جستجو، جزئیات، اشتراک و لغو بدون پاک‌کردن تاریخچه. */
class InvoicesScreen : GalataScreen {
    override fun build(host: MainActivity): View {
        val page = ScreenUi.page(host, "فاکتورها", "آرشیو کامل فروش", CafeIconView.Icon.INVOICE)
        val content = ScreenUi.content(host)
        val search = ScreenUi.input(host, "🔎 جستجو با شماره فاکتور یا نام مشتری")
        content.addView(search)
        val list = LinearLayout(host).apply { orientation = LinearLayout.VERTICAL }
        content.addView(list)

        fun render(filter: String = "") {
            list.removeAllViews()
            val invoices = host.db.invoices(filter)
            if (invoices.isEmpty()) list.addView(ScreenUi.empty(host, "فاکتوری پیدا نشد"))
            invoices.forEach { inv ->
                val names = host.db.invoiceItems(inv.id).joinToString(" ٫ ") { it.name }
                val status = if (inv.status == "CANCELLED") "لغو شده" else "فعال"
                list.addView(CafeTheme.card(
                    host,
                    "فاکتور ${ScreenUi.fa(inv.id.toString())} — ${inv.customerName}",
                    "$names\nجمع: ${ScreenUi.money(inv.total)} • دریافتی هنگام فروش: ${ScreenUi.money(inv.receivedAtSale)}\n${ScreenUi.date(inv.createdAt)} • $status",
                    CafeIconView.Icon.INVOICE
                ).apply {
                    alpha = if (inv.status == "CANCELLED") .58f else 1f
                    setOnClickListener { showInvoice(host, inv) }
                    setOnLongClickListener { actions(host, inv); true }
                })
            }
        }
        render()
        search.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = render(s?.toString().orEmpty())
            override fun afterTextChanged(s: Editable?) = Unit
        })
        content.addView(CafeTheme.card(host, "راهنما", "لمس: جزئیات فاکتور • نگه‌داشتن انگشت: اشتراک یا لغو فاکتور", CafeIconView.Icon.ABOUT))
        page.addView(ScreenUi.scroll(host, content), LinearLayout.LayoutParams(-1,0,1f))
        return page
    }

    private fun invoiceText(host: MainActivity, inv: Invoice): String {
        val lines = host.db.invoiceItems(inv.id).joinToString("\n") { "${it.name} × ${ScreenUi.fa(it.qty.toString())} — ${ScreenUi.money(it.total)}" }
        return "فاکتور ${ScreenUi.fa(inv.id.toString())}\nمشتری: ${inv.customerName}\nتاریخ: ${ScreenUi.date(inv.createdAt)}\n\n$lines\n\nجمع کل: ${ScreenUi.money(inv.total)}\nدریافتی هنگام فروش: ${ScreenUi.money(inv.receivedAtSale)}${if (inv.note.isBlank()) "" else "\nتوضیح: ${inv.note}"}\nوضعیت: ${if (inv.status == "CANCELLED") "لغو شده" else "فعال"}"
    }

    private fun showInvoice(host: MainActivity, inv: Invoice) = ScreenUi.infoDialog(host, "جزئیات فاکتور", invoiceText(host, inv))

    private fun actions(host: MainActivity, inv: Invoice) {
        val items = if (inv.status == "ACTIVE") arrayOf("اشتراک فاکتور", "لغو فاکتور") else arrayOf("اشتراک فاکتور")
        AlertDialog.Builder(host).setTitle("فاکتور ${ScreenUi.fa(inv.id.toString())}").setItems(items) { _, which ->
            if (which == 0) {
                host.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply { type = "text/plain"; putExtra(Intent.EXTRA_TEXT, invoiceText(host, inv)) }, "ارسال فاکتور"))
            } else {
                ScreenUi.confirm(host, "لغو فاکتور", "فاکتور پاک نمی‌شود؛ بدهی ایجادشده معکوس و موجودی کالا برگردانده می‌شود. اگر قبلاً وجه دریافت شده باشد مشتری ممکن است بستانکار شود.", "لغو فاکتور") {
                    if (host.db.cancelInvoice(inv.id)) { ScreenUi.toast(host, "فاکتور لغو شد"); host.navigate(MainActivity.Route.INVOICES, addToHistory = false) }
                }
            }
        }.show()
    }
}
