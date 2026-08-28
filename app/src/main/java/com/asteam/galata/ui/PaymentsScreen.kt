package com.asteam.galata.ui

import android.app.AlertDialog
import android.view.View
import android.widget.LinearLayout
import com.asteam.galata.Customer
import com.asteam.galata.MainActivity

/** صفحه مستقل تسویه مشتری؛ پرداخت کامل یا جزئی. */
class PaymentsScreen : GalataScreen {
    override fun build(host: MainActivity): View {
        val page = ScreenUi.page(host, "تسویه مشتری", "دریافت و کاهش مانده بدهی", CafeIconView.Icon.WALLET)
        val content = ScreenUi.content(host)
        val debtors = host.db.customers().filter { it.balance > 0 }
        if (debtors.isEmpty()) content.addView(ScreenUi.empty(host, "همه حساب‌ها تسویه هستند"))
        debtors.forEach { c ->
            content.addView(CafeTheme.card(host, c.name, "بدهی: ${ScreenUi.money(c.balance)}\nبرای ثبت دریافت لمس کنید", CafeIconView.Icon.WALLET).apply {
                setOnClickListener { paymentDialog(host, c) }
            })
        }
        page.addView(ScreenUi.scroll(host, content), LinearLayout.LayoutParams(-1,0,1f))
        return page
    }

    private fun paymentDialog(host: MainActivity, customer: Customer) {
        val amount = ScreenUi.input(host, "مبلغ پرداخت", true).apply { setText(ScreenUi.fa(customer.balance.toString())) }
        val note = ScreenUi.input(host, "توضیح پرداخت")
        AlertDialog.Builder(host).setTitle("تسویه ${customer.name}").setView(ScreenUi.form(host, amount, note))
            .setNegativeButton("انصراف", null)
            .setPositiveButton("ثبت دریافت") { _, _ ->
                val value = ScreenUi.num(amount.text.toString())
                val balance = host.db.balance(customer.id)
                if (value <= 0 || value > balance) ScreenUi.toast(host, "مبلغ باید از صفر بیشتر و حداکثر برابر مانده باشد")
                else { host.db.addPayment(customer.id, value, note.text.toString()); host.navigate(MainActivity.Route.PAYMENTS) }
            }.show()
    }
}
