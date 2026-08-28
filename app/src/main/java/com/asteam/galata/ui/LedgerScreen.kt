package com.asteam.galata.ui

import android.view.View
import android.widget.LinearLayout
import com.asteam.galata.Customer
import com.asteam.galata.MainActivity

/** صورت‌حساب یک مشتری؛ فایل جدا چون این بخش بعداً به دفتر کل کامل تبدیل می‌شود. */
class LedgerScreen(private val customer: Customer) : GalataScreen {
    override fun build(host: MainActivity): View {
        val page = ScreenUi.page(host, "صورت‌حساب ${customer.name}", "مانده: ${ScreenUi.money(host.db.balance(customer.id))}", CafeIconView.Icon.INVOICE)
        val content = ScreenUi.content(host)
        val rows = host.db.ledger(customer.id)
        if (rows.isEmpty()) content.addView(ScreenUi.empty(host, "گردش حسابی وجود ندارد"))
        rows.forEach { row ->
            val debt = row.type == "DEBT"
            content.addView(CafeTheme.card(
                host,
                if (debt) "بدهکار" else "پرداخت / دریافتی",
                "${ScreenUi.money(row.amount)}\n${row.note}\n${ScreenUi.date(row.createdAt)}",
                if (debt) CafeIconView.Icon.INVOICE else CafeIconView.Icon.WALLET
            ))
        }
        page.addView(ScreenUi.scroll(host, content), LinearLayout.LayoutParams(-1,0,1f))
        return page
    }
}
