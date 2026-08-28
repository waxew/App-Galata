package com.asteam.galata.ui

import android.app.AlertDialog
import android.text.InputType
import android.view.View
import android.widget.LinearLayout
import com.asteam.galata.MainActivity

/** صفحه مستقل مشتری‌ها؛ نمایش مانده و ورود به گردش حساب. */
class CustomersScreen : GalataScreen {
    override fun build(host: MainActivity): View {
        val page = ScreenUi.page(host, "مشتری‌ها", "باشگاه مشتریان و حساب‌های باز", CafeIconView.Icon.CUSTOMER)
        val content = ScreenUi.content(host)
        content.addView(ScreenUi.primaryButton(host, "+ افزودن مشتری") { addCustomer(host) })
        val customers = host.db.customers()
        if (customers.isEmpty()) content.addView(ScreenUi.empty(host, "اولین مشتری کافه را اضافه کنید"))
        customers.forEach { c ->
            val status = if (c.balance > 0) "مانده حساب: ${ScreenUi.money(c.balance)}" else "حساب تسویه است"
            content.addView(CafeTheme.card(host, c.name, "${c.phone}\n$status", CafeIconView.Icon.CUSTOMER).apply {
                setOnClickListener { host.showLedger(c) }
            })
        }
        page.addView(ScreenUi.scroll(host, content), LinearLayout.LayoutParams(-1, 0, 1f))
        return page
    }

    private fun addCustomer(host: MainActivity) {
        val name = ScreenUi.input(host, "نام و نام خانوادگی")
        val phone = ScreenUi.input(host, "شماره تماس").apply { inputType = InputType.TYPE_CLASS_PHONE }
        AlertDialog.Builder(host).setTitle("مشتری جدید").setView(ScreenUi.form(host, name, phone))
            .setNegativeButton("انصراف", null)
            .setPositiveButton("ذخیره") { _, _ ->
                if (name.text.isBlank()) ScreenUi.toast(host, "نام مشتری الزامی است")
                else { host.db.addCustomer(name.text.toString(), ScreenUi.fa(phone.text.toString())); host.navigate(MainActivity.Route.CUSTOMERS) }
            }.show()
    }
}
