package com.asteam.galata.ui

import android.app.AlertDialog
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.LinearLayout
import com.asteam.galata.Customer
import com.asteam.galata.MainActivity

/** تسویه مشتری: تسویه کامل یا پرداخت جزئی بدون دستکاری مستقیم مانده. */
class PaymentsScreen : GalataScreen {
    override fun build(host: MainActivity): View {
        val d = host.db.dashboard()
        val page = ScreenUi.page(host, "تسویه مشتری", "ثبت پول واقعاً دریافت‌شده", CafeIconView.Icon.WALLET)
        val content = ScreenUi.content(host)
        content.addView(ScreenUi.stat(host, "دریافتی امروز", ScreenUi.money(d.receiptsToday), CafeIconView.Icon.WALLET))
        content.addView(ScreenUi.stat(host, "مطالبات کل", ScreenUi.money(d.receivable), CafeIconView.Icon.INVOICE))
        val debtors = host.db.customers().filter { it.balance > 0 }.sortedBy { it.name }
        val search = ScreenUi.input(host, "🔎 جستجو در مشتری‌های بدهکار")
        content.addView(search)
        val list = LinearLayout(host).apply { orientation = LinearLayout.VERTICAL }
        content.addView(list)

        fun render(filter: String = "") {
            list.removeAllViews()
            val visible = debtors.filter { it.name.contains(filter, true) || it.phone.contains(filter) }
            if (visible.isEmpty()) list.addView(ScreenUi.empty(host, if (debtors.isEmpty()) "همه حساب‌ها تسویه هستند" else "مشتری پیدا نشد"))
            visible.forEach { customer ->
                list.addView(CafeTheme.card(host, customer.name, "مانده بدهی: ${ScreenUi.money(customer.balance)}", CafeIconView.Icon.WALLET).apply {
                    setOnClickListener { chooseSettlement(host, customer) }
                })
            }
        }
        render()
        search.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = render(s?.toString().orEmpty())
            override fun afterTextChanged(s: Editable?) = Unit
        })
        page.addView(ScreenUi.scroll(host, content), LinearLayout.LayoutParams(-1,0,1f))
        return page
    }

    private fun chooseSettlement(host: MainActivity, customer: Customer) {
        AlertDialog.Builder(host).setTitle(customer.name)
            .setMessage("مانده بدهی: ${ScreenUi.money(host.db.balance(customer.id))}")
            .setItems(arrayOf("✅ تسویه کل بدهی", "تسویه قسمتی از بدهی", "مشاهده صورت‌حساب")) { _, which ->
                when (which) {
                    0 -> {
                        val balance = host.db.balance(customer.id)
                        if (balance > 0 && host.db.addPayment(customer.id, balance, "تسویه کل بدهی") > 0) {
                            ScreenUi.toast(host, "حساب ${customer.name} کامل تسویه شد")
                            host.navigate(MainActivity.Route.PAYMENTS, addToHistory = false)
                        }
                    }
                    1 -> partialPaymentDialog(host, customer)
                    2 -> host.showLedger(customer)
                }
            }.setNegativeButton("انصراف", null).show()
    }

    private fun partialPaymentDialog(host: MainActivity, customer: Customer) {
        val amount = ScreenUi.input(host, "مبلغ پرداختی", true)
        val note = ScreenUi.input(host, "توضیح پرداخت")
        AlertDialog.Builder(host).setTitle("تسویه قسمتی از بدهی")
            .setMessage("مانده فعلی: ${ScreenUi.money(host.db.balance(customer.id))}")
            .setView(ScreenUi.form(host, amount, note))
            .setNegativeButton("انصراف", null)
            .setPositiveButton("ثبت دریافت") { _, _ ->
                val value = ScreenUi.num(amount.text.toString())
                val balance = host.db.balance(customer.id)
                if (value <= 0 || value > balance) ScreenUi.toast(host, "مبلغ باید بیشتر از صفر و حداکثر برابر مانده بدهی باشد")
                else if (host.db.addPayment(customer.id, value, note.text.toString().ifBlank { "تسویه قسمتی" }) > 0) {
                    ScreenUi.toast(host, "دریافت ثبت شد؛ مانده جدید ${ScreenUi.money(balance - value)}")
                    host.navigate(MainActivity.Route.PAYMENTS, addToHistory = false)
                }
            }.show()
    }
}
