package com.asteam.galata.ui

import android.app.AlertDialog
import android.view.View
import android.widget.LinearLayout
import com.asteam.galata.MainActivity

/** صفحه مستقل هزینه‌های کافه. */
class ExpensesScreen : GalataScreen {
    override fun build(host: MainActivity): View {
        val dashboard = host.db.dashboard()
        val page = ScreenUi.page(host, "هزینه‌ها", "ثبت هزینه مواد اولیه و مخارج کافه", CafeIconView.Icon.EXPENSE)
        val content = ScreenUi.content(host)
        content.addView(ScreenUi.stat(host, "جمع هزینه‌های ثبت‌شده", ScreenUi.money(dashboard.expenses), CafeIconView.Icon.EXPENSE))
        content.addView(ScreenUi.primaryButton(host, "+ ثبت هزینه") { addExpense(host) })
        content.addView(CafeTheme.card(host, "نمونه دسته‌بندی", "قهوه و مواد اولیه • شیر • چای • بسته‌بندی • هزینه جاری", CafeIconView.Icon.COFFEE))
        page.addView(ScreenUi.scroll(host, content), LinearLayout.LayoutParams(-1,0,1f))
        return page
    }

    private fun addExpense(host: MainActivity) {
        val title = ScreenUi.input(host, "عنوان هزینه")
        val amount = ScreenUi.input(host, "مبلغ", true)
        AlertDialog.Builder(host).setTitle("هزینه جدید").setView(ScreenUi.form(host, title, amount))
            .setNegativeButton("انصراف", null)
            .setPositiveButton("ثبت") { _, _ ->
                val value = ScreenUi.num(amount.text.toString())
                if (title.text.isBlank() || value <= 0) ScreenUi.toast(host, "عنوان و مبلغ معتبر وارد کنید")
                else { host.db.addExpense(title.text.toString(), value); host.navigate(MainActivity.Route.EXPENSES) }
            }.show()
    }
}
