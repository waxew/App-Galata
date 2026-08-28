package com.asteam.galata.ui

import android.app.AlertDialog
import android.view.View
import android.widget.LinearLayout
import com.asteam.galata.MainActivity

/** صفحه مستقل یادآورها برای چک، قرار، قسط و یادداشت روزانه. */
class RemindersScreen : GalataScreen {
    override fun build(host: MainActivity): View {
        val page = ScreenUi.page(host, "یادآورها", "قرارها، چک‌ها و پیگیری‌های کافه", CafeIconView.Icon.BELL)
        val content = ScreenUi.content(host)
        content.addView(ScreenUi.primaryButton(host, "+ یادآور جدید") { addReminder(host) })
        val reminders = host.db.reminders()
        if (reminders.isEmpty()) content.addView(ScreenUi.empty(host, "یادآوری ثبت نشده"))
        reminders.forEach { r ->
            content.addView(CafeTheme.card(host, r.title, "${r.description}\n${ScreenUi.date(r.dueAt)}", CafeIconView.Icon.BELL))
        }
        page.addView(ScreenUi.scroll(host, content), LinearLayout.LayoutParams(-1,0,1f))
        return page
    }

    private fun addReminder(host: MainActivity) {
        val title = ScreenUi.input(host, "عنوان")
        val description = ScreenUi.input(host, "توضیح")
        val days = ScreenUi.input(host, "چند روز دیگر؟", true).apply { setText("۱") }
        AlertDialog.Builder(host).setTitle("یادآور جدید").setView(ScreenUi.form(host, title, description, days))
            .setNegativeButton("انصراف", null)
            .setPositiveButton("ثبت") { _, _ ->
                if (title.text.isBlank()) ScreenUi.toast(host, "عنوان الزامی است")
                else {
                    val due = System.currentTimeMillis() + ScreenUi.num(days.text.toString()).coerceAtLeast(0) * 24L * 60 * 60 * 1000
                    host.db.addReminder(title.text.toString(), description.text.toString(), due)
                    host.navigate(MainActivity.Route.REMINDERS)
                }
            }.show()
    }
}
