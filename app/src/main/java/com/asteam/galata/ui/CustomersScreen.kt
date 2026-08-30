package com.asteam.galata.ui

import android.app.AlertDialog
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.view.View
import android.widget.LinearLayout
import com.asteam.galata.Customer
import com.asteam.galata.MainActivity

/**
 * مدیریت مشتری‌ها: افزودن، جستجو، ویرایش، حذف/بایگانی و ورود به صورت‌حساب.
 * مشتری دارای سابقه مالی هرگز از دیتابیس حذف فیزیکی نمی‌شود و فقط بایگانی می‌شود.
 */
class CustomersScreen : GalataScreen {
    override fun build(host: MainActivity): View {
        val page = ScreenUi.page(host, "مشتری‌ها", "مدیریت مشتری و حساب‌های باز", CafeIconView.Icon.CUSTOMER)
        val content = ScreenUi.content(host)
        content.addView(ScreenUi.primaryButton(host, "+ افزودن مشتری") { editCustomer(host, null) })
        val search = ScreenUi.input(host, "🔎 جستجو با نام یا شماره تماس")
        content.addView(search)
        val list = LinearLayout(host).apply { orientation = LinearLayout.VERTICAL }
        content.addView(list)

        fun render(filter: String = "") {
            list.removeAllViews()
            val customers = host.db.customers(query = filter)
            if (customers.isEmpty()) list.addView(ScreenUi.empty(host, "مشتری پیدا نشد"))
            customers.forEach { customer ->
                val balanceText = when {
                    customer.balance > 0 -> "بدهی: ${ScreenUi.money(customer.balance)}"
                    customer.balance < 0 -> "بستانکار: ${ScreenUi.money(-customer.balance)}"
                    else -> "حساب تسویه است"
                }
                list.addView(CafeTheme.card(
                    host,
                    customer.name,
                    listOf(customer.phone.ifBlank { "بدون شماره تماس" }, balanceText, customer.note).filter { it.isNotBlank() }.joinToString("\n"),
                    CafeIconView.Icon.CUSTOMER
                ).apply {
                    setOnClickListener { host.showLedger(customer) }
                    setOnLongClickListener { showActions(host, customer); true }
                })
            }
        }

        render()
        search.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = render(s?.toString().orEmpty())
            override fun afterTextChanged(s: Editable?) = Unit
        })
        content.addView(CafeTheme.card(host, "راهنما", "لمس مشتری: صورت‌حساب • نگه‌داشتن انگشت: ویرایش، اصلاح حساب یا حذف/بایگانی", CafeIconView.Icon.ABOUT))
        page.addView(ScreenUi.scroll(host, content), LinearLayout.LayoutParams(-1, 0, 1f))
        return page
    }

    private fun editCustomer(host: MainActivity, customer: Customer?) {
        val name = ScreenUi.input(host, "نام و نام خانوادگی").apply { setText(customer?.name.orEmpty()) }
        val phone = ScreenUi.input(host, "شماره تماس").apply {
            inputType = InputType.TYPE_CLASS_PHONE
            setText(customer?.phone.orEmpty())
        }
        val note = ScreenUi.input(host, "یادداشت مشتری").apply { setText(customer?.note.orEmpty()) }
        AlertDialog.Builder(host)
            .setTitle(if (customer == null) "مشتری جدید" else "ویرایش مشتری")
            .setView(ScreenUi.form(host, name, phone, note))
            .setNegativeButton("انصراف", null)
            .setPositiveButton("ذخیره") { _, _ ->
                if (name.text.isBlank()) ScreenUi.toast(host, "نام مشتری الزامی است")
                else {
                    if (customer == null) host.db.addCustomer(name.text.toString(), ScreenUi.fa(phone.text.toString()), note.text.toString())
                    else host.db.updateCustomer(customer.id, name.text.toString(), ScreenUi.fa(phone.text.toString()), note.text.toString())
                    host.navigate(MainActivity.Route.CUSTOMERS, addToHistory = false)
                }
            }.show()
    }

    private fun showActions(host: MainActivity, customer: Customer) {
        AlertDialog.Builder(host).setTitle(customer.name)
            .setItems(arrayOf("ویرایش", "اصلاح مانده حساب", "حذف / بایگانی")) { _, which ->
                when (which) {
                    0 -> editCustomer(host, customer)
                    1 -> adjustBalance(host, customer)
                    2 -> ScreenUi.confirm(host, "حذف مشتری", "اگر مشتری سابقه مالی داشته باشد حذف نمی‌شود و فقط بایگانی خواهد شد.", "ادامه") {
                        val result = host.db.removeCustomer(customer.id)
                        ScreenUi.toast(host, if (result == "ARCHIVED") "مشتری به دلیل سابقه مالی بایگانی شد" else "مشتری حذف شد")
                        host.navigate(MainActivity.Route.CUSTOMERS, addToHistory = false)
                    }
                }
            }.show()
    }

    /** اصلاح حساب همواره یک رکورد Ledger جدید ایجاد می‌کند؛ مانده قبلی بازنویسی نمی‌شود. */
    private fun adjustBalance(host: MainActivity, customer: Customer) {
        val amount = ScreenUi.input(host, "مبلغ اصلاح", true)
        val note = ScreenUi.input(host, "علت اصلاح")
        AlertDialog.Builder(host).setTitle("اصلاح حساب ${customer.name}")
            .setSingleChoiceItems(arrayOf("افزایش بدهی", "کاهش بدهی / افزایش بستانکاری"), 0, null)
            .setView(ScreenUi.form(host, amount, note))
            .setNegativeButton("انصراف", null)
            .setPositiveButton("ثبت") { dialog, _ ->
                val value = ScreenUi.num(amount.text.toString())
                val selected = (dialog as AlertDialog).listView.checkedItemPosition
                if (value <= 0) ScreenUi.toast(host, "مبلغ معتبر وارد کنید")
                else {
                    host.db.adjustBalance(customer.id, if (selected == 1) -value else value, note.text.toString())
                    host.navigate(MainActivity.Route.CUSTOMERS, addToHistory = false)
                }
            }.show()
    }
}
