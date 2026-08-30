package com.asteam.galata.ui

import android.app.AlertDialog
import android.view.View
import android.widget.LinearLayout
import com.asteam.galata.Expense
import com.asteam.galata.MainActivity

/** هزینه‌های کسب‌وکار با ثبت، ویرایش و حذف؛ در سود خالص گزارش مالی استفاده می‌شوند. */
class ExpensesScreen : GalataScreen {
    override fun build(host: MainActivity): View {
        val page = ScreenUi.page(host, "هزینه‌ها", "مخارج کسب‌وکار", CafeIconView.Icon.EXPENSE)
        val content = ScreenUi.content(host)
        val total = host.db.expenses().sumOf { it.amount }
        content.addView(ScreenUi.stat(host, "جمع کل هزینه‌های ثبت‌شده", ScreenUi.money(total), CafeIconView.Icon.EXPENSE))
        content.addView(ScreenUi.primaryButton(host, "+ ثبت هزینه") { editExpense(host, null) })
        val expenses = host.db.expenses()
        if (expenses.isEmpty()) content.addView(ScreenUi.empty(host, "هزینه‌ای ثبت نشده"))
        expenses.forEach { expense ->
            content.addView(CafeTheme.card(
                host,
                expense.title,
                "${ScreenUi.money(expense.amount)}\n${ScreenUi.date(expense.createdAt)}${if (expense.note.isBlank()) "" else "\n${expense.note}"}",
                CafeIconView.Icon.EXPENSE
            ).apply { setOnLongClickListener { actions(host, expense); true } })
        }
        content.addView(CafeTheme.card(host, "راهنما", "برای ویرایش یا حذف هزینه، انگشت را روی کارت نگه دارید.", CafeIconView.Icon.ABOUT))
        page.addView(ScreenUi.scroll(host, content), LinearLayout.LayoutParams(-1, 0, 1f))
        return page
    }

    private fun editExpense(host: MainActivity, expense: Expense?) {
        val title = ScreenUi.input(host, "عنوان هزینه").apply { setText(expense?.title.orEmpty()) }
        val amount = ScreenUi.input(host, "مبلغ", true).apply { if (expense != null) setText(expense.amount.toString()) }
        val note = ScreenUi.input(host, "توضیح / دسته‌بندی").apply { setText(expense?.note.orEmpty()) }
        AlertDialog.Builder(host).setTitle(if (expense == null) "هزینه جدید" else "ویرایش هزینه")
            .setView(ScreenUi.form(host, title, amount, note))
            .setNegativeButton("انصراف", null)
            .setPositiveButton("ذخیره") { _, _ ->
                val value = ScreenUi.num(amount.text.toString())
                if (title.text.isBlank() || value <= 0) ScreenUi.toast(host, "عنوان و مبلغ معتبر وارد کنید")
                else {
                    if (expense == null) host.db.addExpense(title.text.toString(), value, note.text.toString())
                    else host.db.updateExpense(expense.id, title.text.toString(), value, note.text.toString())
                    host.navigate(MainActivity.Route.EXPENSES, addToHistory = false)
                }
            }.show()
    }

    private fun actions(host: MainActivity, expense: Expense) {
        AlertDialog.Builder(host).setTitle(expense.title).setItems(arrayOf("ویرایش", "حذف")) { _, which ->
            if (which == 0) editExpense(host, expense)
            else ScreenUi.confirm(host, "حذف هزینه", "این هزینه از گزارش‌های مالی حذف می‌شود.", "حذف") {
                host.db.deleteExpense(expense.id)
                host.navigate(MainActivity.Route.EXPENSES, addToHistory = false)
            }
        }.show()
    }
}
