package com.asteam.galata.ui

import android.view.View
import android.widget.LinearLayout
import com.asteam.galata.MainActivity

/** آرشیو فاکتورها؛ هر فاکتور با اقلام و مبلغ در کارت گرافیکی نمایش داده می‌شود. */
class InvoicesScreen : GalataScreen {
    override fun build(host: MainActivity): View {
        val page = ScreenUi.page(host, "فاکتورها", "آرشیو فروش کافه", CafeIconView.Icon.INVOICE)
        val content = ScreenUi.content(host)
        val invoices = host.db.invoices()
        if (invoices.isEmpty()) content.addView(ScreenUi.empty(host, "هنوز فروشی ثبت نشده"))
        invoices.forEach { inv ->
            val names = host.db.invoiceItems(inv.id).joinToString(" • ") { it.name }
            content.addView(CafeTheme.card(
                host,
                "فاکتور ${ScreenUi.fa(inv.id.toString())} — ${inv.customerName}",
                "$names\n${ScreenUi.money(inv.total)}\n${ScreenUi.date(inv.createdAt)}",
                CafeIconView.Icon.INVOICE
            ).apply {
                setOnClickListener {
                    val lines = host.db.invoiceItems(inv.id).joinToString("\n") { "${it.name} × ${ScreenUi.fa(it.qty.toString())} — ${ScreenUi.money(it.total)}" }
                    ScreenUi.infoDialog(host, "فاکتور ${ScreenUi.fa(inv.id.toString())}", "مشتری: ${inv.customerName}\n\n$lines\n\nجمع کل: ${ScreenUi.money(inv.total)}")
                }
            })
        }
        page.addView(ScreenUi.scroll(host, content), LinearLayout.LayoutParams(-1,0,1f))
        return page
    }
}
