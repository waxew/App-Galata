package com.asteam.galata.ui

import android.app.AlertDialog
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.LinearLayout
import com.asteam.galata.JalaliDate
import com.asteam.galata.MainActivity
import com.asteam.galata.Reminder
import com.asteam.galata.ReminderScheduler
import java.util.Calendar

/**
 * یادآورهای کامل گالاتا: چک، قسط، قرار و عمومی با تاریخ/ساعت شمسی، مبلغ، جستجو، ویرایش، حذف و وضعیت انجام.
 */
class RemindersScreen : GalataScreen {
    override fun build(host: MainActivity): View {
        val page = ScreenUi.page(host, "یادآورها", "چک، قسط، قرار و کارهای مهم", CafeIconView.Icon.BELL)
        val content = ScreenUi.content(host)
        content.addView(ScreenUi.primaryButton(host, "+ یادآور جدید") { editReminder(host, null, null) })
        val search = ScreenUi.input(host, "🔎 جستجو در یادآورها")
        content.addView(search)
        val list = LinearLayout(host).apply { orientation = LinearLayout.VERTICAL }
        content.addView(list)

        fun render(filter: String = "") {
            list.removeAllViews()
            val reminders = host.db.reminders(includeDone = true, query = filter)
            if (reminders.isEmpty()) list.addView(ScreenUi.empty(host, "یادآوری پیدا نشد"))
            reminders.forEach { reminder ->
                val overdue = !reminder.done && reminder.dueAt < System.currentTimeMillis()
                val status = when {
                    reminder.done -> "انجام‌شده"
                    overdue -> "سررسید گذشته"
                    else -> "باز"
                }
                val amount = if (reminder.amount > 0L) "\nمبلغ: ${ScreenUi.money(reminder.amount)}" else ""
                list.addView(CafeTheme.card(
                    host,
                    "${ScreenUi.reminderKind(reminder.kind)} — ${reminder.title}",
                    "${ScreenUi.date(reminder.dueAt)}$amount\n$status${if (reminder.description.isBlank()) "" else "\n${reminder.description}"}",
                    CafeIconView.Icon.BELL
                ).apply {
                    alpha = if (reminder.done) .55f else 1f
                    setOnClickListener { editReminder(host, reminder, null) }
                    setOnLongClickListener { actions(host, reminder); true }
                })
            }
        }
        render()
        search.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = render(s?.toString().orEmpty())
            override fun afterTextChanged(s: Editable?) = Unit
        })
        content.addView(CafeTheme.card(host, "راهنما", "لمس: ویرایش • نگه‌داشتن انگشت: انجام‌شده/باز یا حذف", CafeIconView.Icon.ABOUT))
        page.addView(ScreenUi.scroll(host, content), LinearLayout.LayoutParams(-1,0,1f))
        return page
    }

    private fun actions(host: MainActivity, reminder: Reminder) {
        AlertDialog.Builder(host).setTitle(reminder.title)
            .setItems(arrayOf(if (reminder.done) "باز کردن مجدد" else "علامت‌گذاری انجام‌شده", "ویرایش", "حذف")) { _, which ->
                when (which) {
                    0 -> {
                        val newDone = !reminder.done
                        host.db.setReminderDone(reminder.id, newDone)
                        if (newDone) ReminderScheduler.cancel(host, reminder.id)
                        else host.db.reminder(reminder.id)?.let { ReminderScheduler.schedule(host, it) }
                        host.navigate(MainActivity.Route.REMINDERS, addToHistory = false)
                    }
                    1 -> editReminder(host, reminder, null)
                    2 -> ScreenUi.confirm(host, "حذف یادآور", "این یادآور و اعلان زمان‌بندی‌شده آن حذف می‌شود.", "حذف") {
                        ReminderScheduler.cancel(host, reminder.id)
                        host.db.deleteReminder(reminder.id)
                        host.navigate(MainActivity.Route.REMINDERS, addToHistory = false)
                    }
                }
            }.show()
    }

    companion object {
        /** Editor مشترک تا CalendarScreen هم بتواند برای روز انتخاب‌شده یادآور بسازد. */
        fun editReminder(host: MainActivity, reminder: Reminder?, presetDate: JalaliDate.Date?) {
            val currentJalali: JalaliDate.Date
            val currentCalendar = Calendar.getInstance()
            if (reminder != null) {
                currentCalendar.timeInMillis = reminder.dueAt
                currentJalali = JalaliDate.fromGregorian(currentCalendar.get(Calendar.YEAR), currentCalendar.get(Calendar.MONTH) + 1, currentCalendar.get(Calendar.DAY_OF_MONTH))
            } else {
                currentJalali = presetDate ?: JalaliDate.today()
                currentCalendar.set(Calendar.HOUR_OF_DAY, 9)
                currentCalendar.set(Calendar.MINUTE, 0)
            }

            val title = ScreenUi.input(host, "عنوان؛ مثال: چک مشتری").apply { setText(reminder?.title.orEmpty()) }
            val description = ScreenUi.input(host, "توضیح").apply { setText(reminder?.description.orEmpty()) }
            val amount = ScreenUi.input(host, "مبلغ - اختیاری", true).apply { if ((reminder?.amount ?: 0L) > 0L) setText(reminder!!.amount.toString()) }
            val year = ScreenUi.input(host, "سال شمسی", true).apply { setText(currentJalali.year.toString()) }
            val month = ScreenUi.input(host, "ماه شمسی", true).apply { setText(currentJalali.month.toString()) }
            val day = ScreenUi.input(host, "روز شمسی", true).apply { setText(currentJalali.day.toString()) }
            val hour = ScreenUi.input(host, "ساعت ۰ تا ۲۳", true).apply { setText(currentCalendar.get(Calendar.HOUR_OF_DAY).toString()) }
            val minute = ScreenUi.input(host, "دقیقه", true).apply { setText(currentCalendar.get(Calendar.MINUTE).toString()) }
            val kinds = arrayOf("GENERAL", "CHECK", "INSTALLMENT", "APPOINTMENT")
            val labels = arrayOf("عمومی", "چک", "قسط", "قرار / مراجعه")
            var selectedKind = kinds.indexOf(reminder?.kind ?: "GENERAL").coerceAtLeast(0)

            AlertDialog.Builder(host)
                .setTitle(if (reminder == null) "یادآور جدید" else "ویرایش یادآور")
                .setSingleChoiceItems(labels, selectedKind) { _, which -> selectedKind = which }
                .setView(ScreenUi.form(host, title, description, amount, year, month, day, hour, minute))
                .setNegativeButton("انصراف", null)
                .setPositiveButton("ذخیره") { _, _ ->
                    val y = ScreenUi.num(year.text.toString()).toInt()
                    val m = ScreenUi.num(month.text.toString()).toInt()
                    val d = ScreenUi.num(day.text.toString()).toInt()
                    val h = ScreenUi.num(hour.text.toString()).toInt()
                    val min = ScreenUi.num(minute.text.toString()).toInt()
                    val valid = y in 1300..1600 && m in 1..12 && d in 1..JalaliDate.daysInMonth(y, m) && h in 0..23 && min in 0..59
                    if (title.text.isBlank() || !valid) {
                        ScreenUi.toast(host, "عنوان و تاریخ/ساعت شمسی معتبر وارد کنید")
                    } else {
                        val due = JalaliDate.toMillis(y, m, d, h, min)
                        val money = ScreenUi.num(amount.text.toString())
                        val id = if (reminder == null) {
                            host.db.addReminder(title.text.toString(), description.text.toString(), due, kinds[selectedKind], money)
                        } else {
                            ReminderScheduler.cancel(host, reminder.id)
                            host.db.updateReminder(reminder.id, title.text.toString(), description.text.toString(), due, kinds[selectedKind], money)
                            reminder.id
                        }
                        host.db.reminder(id)?.let { ReminderScheduler.schedule(host, it) }
                        if (host.currentFocus != null) host.currentFocus?.clearFocus()
                        host.navigate(MainActivity.Route.REMINDERS, addToHistory = false)
                    }
                }.show()
        }
    }
}
