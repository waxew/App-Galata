package com.asteam.galata.ui

import android.app.AlertDialog
import android.graphics.Color
import android.graphics.Typeface
import android.text.Editable
import android.text.TextWatcher
import android.view.Gravity
import android.view.View
import android.widget.EditText
import android.widget.LinearLayout
import com.asteam.galata.Customer
import com.asteam.galata.MainActivity

/**
 * صفحه Activity_Payment بر اساس Miro.
 * ابتدا لیست مشتری‌های بدهکار و مانده بدهی نمایش داده می‌شود؛ سپس کاربر تسویه کل یا قسمتی را انتخاب می‌کند.
 */
class PaymentsScreen : GalataScreen {
    override fun build(host: MainActivity): View {
        val page = ScreenUi.page(host, "تسویه مشتری", "Payment", CafeIconView.Icon.WALLET)
        val content = ScreenUi.content(host)
        val debtors = host.db.customers().filter { it.balance > 0 }.sortedBy { it.name }

        val search = ScreenUi.input(host, "🔎 جستجو در لیست مشتری ها")
        content.addView(search)
        val list = LinearLayout(host).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(CafeTheme.dp(host, 8), 0, CafeTheme.dp(host, 8), 0)
        }
        content.addView(list)

        fun render(filter: String = "") {
            list.removeAllViews()
            val visible = debtors.filter { it.name.contains(filter, ignoreCase = true) }
            if (visible.isEmpty()) {
                list.addView(ScreenUi.empty(host, if (debtors.isEmpty()) "همه حساب‌ها تسویه هستند" else "مشتری پیدا نشد"))
            }
            visible.forEach { customer ->
                list.addView(CafeTheme.card(
                    host,
                    customer.name,
                    "کل / مانده بدهی مشتری: ${ScreenUi.money(customer.balance)}",
                    CafeIconView.Icon.WALLET
                ).apply {
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

        page.addView(ScreenUi.scroll(host, content), LinearLayout.LayoutParams(-1, 0, 1f))
        return page
    }

    /** انتخاب بین «تسویه کل بدهی» و «تسویه قسمتی از بدهی» مطابق فریم Miro. */
    private fun chooseSettlement(host: MainActivity, customer: Customer) {
        val options = arrayOf("✅ تسویه کل بدهی", "تسویه قسمتی از بدهی")
        AlertDialog.Builder(host)
            .setTitle(customer.name)
            .setMessage("مانده بدهی: ${ScreenUi.money(host.db.balance(customer.id))}")
            .setItems(options) { _, which ->
                if (which == 0) {
                    val balance = host.db.balance(customer.id)
                    if (balance > 0) {
                        host.db.addPayment(customer.id, balance, "تسویه کل بدهی")
                        ScreenUi.toast(host, "حساب ${customer.name} کامل تسویه شد")
                        host.navigate(MainActivity.Route.PAYMENTS)
                    }
                } else {
                    partialPaymentDialog(host, customer)
                }
            }
            .setNegativeButton("انصراف", null)
            .show()
    }

    /** تسویه قسمتی: فقط عددی بین صفر و مانده بدهی پذیرفته می‌شود. */
    private fun partialPaymentDialog(host: MainActivity, customer: Customer) {
        val amount = ScreenUi.input(host, "مبلغ تسویه قسمتی", true)
        val note = ScreenUi.input(host, "توضیح پرداخت")
        val form = ScreenUi.form(host, amount, note).apply {
            setBackgroundColor(Color.WHITE)
        }

        AlertDialog.Builder(host)
            .setTitle("تسویه قسمتی از بدهی")
            .setMessage("مانده فعلی: ${ScreenUi.money(host.db.balance(customer.id))}")
            .setView(form)
            .setNegativeButton("انصراف", null)
            .setPositiveButton("ثبت دریافت") { _, _ ->
                val value = ScreenUi.num(amount.text.toString())
                val balance = host.db.balance(customer.id)
                if (value <= 0 || value > balance) {
                    ScreenUi.toast(host, "مبلغ باید بیشتر از صفر و حداکثر برابر مانده بدهی باشد")
                } else {
                    host.db.addPayment(customer.id, value, note.text.toString().ifBlank { "تسویه قسمتی" })
                    ScreenUi.toast(host, "دریافت ثبت شد")
                    host.navigate(MainActivity.Route.PAYMENTS)
                }
            }
            .show()
    }
}
