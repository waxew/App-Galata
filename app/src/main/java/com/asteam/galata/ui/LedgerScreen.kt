package com.asteam.galata.ui

import android.app.AlertDialog
import android.view.View
import android.widget.LinearLayout
import com.asteam.galata.Customer
import com.asteam.galata.LedgerRow
import com.asteam.galata.MainActivity

/** صورت‌حساب کامل مشتری با گردش بدهکار/بستانکار و مانده پس از هر رویداد. */
class LedgerScreen(private val customer: Customer) : GalataScreen {
    override fun build(host: MainActivity): View {
        val current = host.db.balance(customer.id)
        val subtitle = when {
            current > 0 -> "بدهی فعلی: ${ScreenUi.money(current)}"
            current < 0 -> "بستانکاری فعلی: ${ScreenUi.money(-current)}"
            else -> "حساب تسویه است"
        }
        val page = ScreenUi.page(host, "صورت‌حساب ${customer.name}", subtitle, CafeIconView.Icon.INVOICE)
        val content = ScreenUi.content(host)
        if (current > 0) content.addView(ScreenUi.primaryButton(host, "ثبت دریافت از مشتری") { payment(host, current) })
        if (current < 0) content.addView(ScreenUi.primaryButton(host, "ثبت بازپرداخت به مشتری") { refund(host, -current) })
        val rows = host.db.ledger(customer.id)
        if (rows.isEmpty()) content.addView(ScreenUi.empty(host, "گردش حسابی وجود ندارد"))
        rows.forEach { row -> content.addView(rowCard(host, row)) }
        page.addView(ScreenUi.scroll(host, content), LinearLayout.LayoutParams(-1,0,1f))
        return page
    }

    private fun rowCard(host: MainActivity, row: LedgerRow): View {
        val title = when (row.type) {
            "DEBT" -> "فروش / بدهکار"
            "PAYMENT" -> "دریافت از مشتری"
            "CANCEL_DEBT" -> "لغو بدهی فاکتور"
            "REFUND" -> "بازپرداخت به مشتری"
            "ADJUST_DEBT" -> "اصلاح افزایشی بدهی"
            "ADJUST_CREDIT" -> "اصلاح کاهشی بدهی"
            else -> row.type
        }
        val signed = if (row.signedAmount >= 0) "+${ScreenUi.money(row.signedAmount)}" else "−${ScreenUi.money(-row.signedAmount)}"
        val balance = if (row.balanceAfter >= 0) "مانده بدهی: ${ScreenUi.money(row.balanceAfter)}" else "بستانکاری: ${ScreenUi.money(-row.balanceAfter)}"
        return CafeTheme.card(host, title, "$signed\n${row.note}\n$balance\n${ScreenUi.date(row.createdAt)}", if (row.signedAmount >= 0) CafeIconView.Icon.INVOICE else CafeIconView.Icon.WALLET)
    }

    private fun payment(host: MainActivity, max: Long) {
        val amount = ScreenUi.input(host, "مبلغ دریافت", true).apply { setText(max.toString()) }
        val note = ScreenUi.input(host, "توضیح").apply { setText("دریافت از مشتری") }
        AlertDialog.Builder(host).setTitle("ثبت دریافت").setView(ScreenUi.form(host, amount, note)).setNegativeButton("انصراف", null)
            .setPositiveButton("ثبت") { _, _ ->
                val value = ScreenUi.num(amount.text.toString())
                if (value <= 0 || value > max) ScreenUi.toast(host, "مبلغ معتبر نیست")
                else if (host.db.addPayment(customer.id, value, note.text.toString()) > 0) host.showLedger(host.db.customer(customer.id) ?: customer)
            }.show()
    }

    private fun refund(host: MainActivity, max: Long) {
        val amount = ScreenUi.input(host, "مبلغ بازپرداخت", true).apply { setText(max.toString()) }
        val note = ScreenUi.input(host, "توضیح").apply { setText("بازپرداخت به مشتری") }
        AlertDialog.Builder(host).setTitle("بازپرداخت").setView(ScreenUi.form(host, amount, note)).setNegativeButton("انصراف", null)
            .setPositiveButton("ثبت") { _, _ ->
                val value = ScreenUi.num(amount.text.toString())
                if (value <= 0 || value > max) ScreenUi.toast(host, "مبلغ معتبر نیست")
                else if (host.db.addRefund(customer.id, value, note.text.toString()) > 0) host.showLedger(host.db.customer(customer.id) ?: customer)
            }.show()
    }
}
