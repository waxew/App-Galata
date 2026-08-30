package com.asteam.galata.ui

import android.app.AlertDialog
import android.graphics.Color
import android.graphics.Typeface
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import com.asteam.galata.CalendarNote
import com.asteam.galata.JalaliDate
import com.asteam.galata.MainActivity
import java.util.Calendar

/** تقویم ماهانه شمسی با یادداشت روزانه و اتصال مستقیم به یادآورها. */
class CalendarScreen : GalataScreen {
    override fun build(host: MainActivity): View {
        val today = JalaliDate.today()
        var year = today.year
        var month = today.month
        val page = ScreenUi.page(host, "تقویم شمسی", "یادداشت و سررسید روی روز", CafeIconView.Icon.CALENDAR)
        val content = ScreenUi.content(host)
        val header = LinearLayout(host).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER; layoutDirection = View.LAYOUT_DIRECTION_RTL }
        val previous = Button(host).apply { text = "ماه قبل"; isAllCaps = false }
        val title = TextView(host).apply { gravity = Gravity.CENTER; textSize = 18f; typeface = Typeface.DEFAULT_BOLD; setTextColor(CafeTheme.espresso) }
        val next = Button(host).apply { text = "ماه بعد"; isAllCaps = false }
        header.addView(previous, LinearLayout.LayoutParams(0, CafeTheme.dp(host,48), 1f))
        header.addView(title, LinearLayout.LayoutParams(0, CafeTheme.dp(host,48), 1.4f))
        header.addView(next, LinearLayout.LayoutParams(0, CafeTheme.dp(host,48), 1f))
        content.addView(header)

        val week = LinearLayout(host).apply { orientation = LinearLayout.HORIZONTAL; layoutDirection = View.LAYOUT_DIRECTION_RTL }
        arrayOf("ش", "ی", "د", "س", "چ", "پ", "ج").forEach { name ->
            week.addView(TextView(host).apply { text = name; gravity = Gravity.CENTER; typeface = Typeface.DEFAULT_BOLD; setTextColor(CafeTheme.mocha) }, LinearLayout.LayoutParams(0, CafeTheme.dp(host,38), 1f))
        }
        content.addView(week)
        val grid = LinearLayout(host).apply { orientation = LinearLayout.VERTICAL }
        content.addView(grid)
        val selectedInfo = LinearLayout(host).apply { orientation = LinearLayout.VERTICAL }
        content.addView(selectedInfo)

        fun reminderKeys(): Set<String> = host.db.reminders(includeDone = false).map { reminder ->
            val c = Calendar.getInstance().apply { timeInMillis = reminder.dueAt }
            val j = JalaliDate.fromGregorian(c.get(Calendar.YEAR), c.get(Calendar.MONTH) + 1, c.get(Calendar.DAY_OF_MONTH))
            JalaliDate.dayKey(j.year, j.month, j.day)
        }.toSet()

        fun render() {
            title.text = "${JalaliDate.monthName(month)} ${ScreenUi.fa(year.toString())}"
            grid.removeAllViews()
            val noteDays = host.db.calendarNoteDays("%04d-%02d".format(year, month))
            val reminderDays = reminderKeys()
            val firstOffset = JalaliDate.weekDayIndex(year, month, 1)
            val count = JalaliDate.daysInMonth(year, month)
            var day = 1
            repeat(6) { rowIndex ->
                val row = LinearLayout(host).apply { orientation = LinearLayout.HORIZONTAL; layoutDirection = View.LAYOUT_DIRECTION_RTL }
                repeat(7) { column ->
                    val position = rowIndex * 7 + column
                    if (position < firstOffset || day > count) {
                        row.addView(View(host), LinearLayout.LayoutParams(0, CafeTheme.dp(host,64), 1f))
                    } else {
                        val currentDay = day++
                        val key = JalaliDate.dayKey(year, month, currentDay)
                        val isToday = year == today.year && month == today.month && currentDay == today.day
                        val marker = buildString {
                            if (noteDays.contains(key)) append(" •")
                            if (reminderDays.contains(key)) append(" ●")
                        }
                        row.addView(TextView(host).apply {
                            text = ScreenUi.fa(currentDay.toString()) + marker
                            gravity = Gravity.CENTER; textSize = 14f
                            setTextColor(if (isToday) Color.WHITE else CafeTheme.ink)
                            background = CafeTheme.rounded(if (isToday) CafeTheme.caramel else CafeTheme.foam, CafeTheme.dp(host,10).toFloat(), Color.rgb(225,211,197), 1)
                            setOnClickListener { showDay(host, JalaliDate.Date(year, month, currentDay), selectedInfo) { render() } }
                        }, LinearLayout.LayoutParams(0, CafeTheme.dp(host,64), 1f).apply { setMargins(2,2,2,2) })
                    }
                }
                grid.addView(row)
            }
        }

        previous.setOnClickListener {
            if (month == 1) { month = 12; year-- } else month--
            render()
        }
        next.setOnClickListener {
            if (month == 12) { month = 1; year++ } else month++
            render()
        }
        render()
        content.addView(CafeTheme.card(host, "نشانه‌ها", "• یادداشت روزانه   ● یادآور باز", CafeIconView.Icon.CALENDAR))
        page.addView(ScreenUi.scroll(host, content), LinearLayout.LayoutParams(-1,0,1f))
        return page
    }

    private fun showDay(host: MainActivity, date: JalaliDate.Date, selectedInfo: LinearLayout, refresh: () -> Unit) {
        val key = JalaliDate.dayKey(date.year, date.month, date.day)
        val notes = host.db.calendarNotes(key)
        val reminders = host.db.reminders(includeDone = false).filter { reminder ->
            val c = Calendar.getInstance().apply { timeInMillis = reminder.dueAt }
            val j = JalaliDate.fromGregorian(c.get(Calendar.YEAR), c.get(Calendar.MONTH)+1, c.get(Calendar.DAY_OF_MONTH))
            j == date
        }
        selectedInfo.removeAllViews()
        selectedInfo.addView(CafeTheme.sectionTitle(host, "${ScreenUi.fa(date.day.toString())} ${JalaliDate.monthName(date.month)} ${ScreenUi.fa(date.year.toString())}"))
        if (notes.isEmpty() && reminders.isEmpty()) selectedInfo.addView(ScreenUi.empty(host, "برای این روز موردی ثبت نشده"))
        notes.forEach { note ->
            selectedInfo.addView(CafeTheme.card(host, "یادداشت: ${note.title}", note.text, CafeIconView.Icon.CALENDAR).apply {
                setOnLongClickListener { manageNote(host, note, refresh); true }
            })
        }
        reminders.forEach { reminder ->
            selectedInfo.addView(CafeTheme.card(host, "${ScreenUi.reminderKind(reminder.kind)}: ${reminder.title}", "${ScreenUi.date(reminder.dueAt)}${if (reminder.amount > 0) "\n${ScreenUi.money(reminder.amount)}" else ""}", CafeIconView.Icon.BELL).apply {
                setOnClickListener { RemindersScreen.editReminder(host, reminder, null) }
            })
        }
        selectedInfo.addView(ScreenUi.primaryButton(host, "+ یادداشت برای این روز") { addNote(host, date, refresh) })
        selectedInfo.addView(ScreenUi.primaryButton(host, "+ یادآور برای این روز") { RemindersScreen.editReminder(host, null, date) })
    }

    private fun addNote(host: MainActivity, date: JalaliDate.Date, refresh: () -> Unit) {
        val title = ScreenUi.input(host, "عنوان یادداشت")
        val text = ScreenUi.input(host, "متن یادداشت")
        AlertDialog.Builder(host).setTitle("یادداشت ${ScreenUi.fa(date.day.toString())} ${JalaliDate.monthName(date.month)}")
            .setView(ScreenUi.form(host, title, text)).setNegativeButton("انصراف", null)
            .setPositiveButton("ذخیره") { _, _ ->
                if (title.text.isBlank()) ScreenUi.toast(host, "عنوان یادداشت الزامی است")
                else { host.db.addCalendarNote(JalaliDate.dayKey(date.year,date.month,date.day), title.text.toString(), text.text.toString()); refresh() }
            }.show()
    }

    private fun manageNote(host: MainActivity, note: CalendarNote, refresh: () -> Unit) {
        AlertDialog.Builder(host).setTitle(note.title).setMessage(note.text)
            .setNegativeButton("بستن", null)
            .setPositiveButton("حذف یادداشت") { _, _ -> host.db.deleteCalendarNote(note.id); refresh() }
            .show()
    }
}
