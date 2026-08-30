package com.asteam.galata.ui

import android.graphics.Color
import android.graphics.Typeface
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import com.asteam.galata.JalaliDate
import com.asteam.galata.MainActivity
import java.util.Calendar

/** داشبورد اصلی گالاتا مطابق فلو Miro؛ تقویم، فروش/مالی و یادآور در یک صفحه. */
class HomeScreen : GalataScreen {
    override fun build(host: MainActivity): View {
        val d = host.db.dashboard()
        val month = host.db.currentMonthReport()
        val page = ScreenUi.page(host, "گالاتا", host.db.ownerName(), CafeIconView.Icon.COFFEE)
        val content = ScreenUi.content(host)

        content.addView(sectionLabel(host, "تقویم", Color.rgb(255,246,182), CafeTheme.ink))
        content.addView(weeklyCalendar(host).apply { setOnClickListener { host.navigate(MainActivity.Route.CALENDAR) } })
        content.addView(TextView(host).apply {
            text = "برای مشاهده ماه کامل و ثبت یادداشت روی روز، تقویم را لمس کنید"
            textSize = 12f; gravity = Gravity.CENTER; setTextColor(CafeTheme.mocha); setPadding(12,8,12,14)
            setOnClickListener { host.navigate(MainActivity.Route.CALENDAR) }
        })

        content.addView(sectionLabel(host, "فروش", Color.rgb(254,159,77), Color.WHITE))
        content.addView(miroBlock(host, Color.rgb(254,159,77), "ثبت فروش", "فروش امروز: ${ScreenUi.money(d.salesToday)}") { host.navigate(MainActivity.Route.SALE) })

        content.addView(sectionLabel(host, "مالی", Color.rgb(254,159,77), Color.WHITE))
        val row1 = LinearLayout(host).apply { orientation = LinearLayout.HORIZONTAL; layoutDirection = View.LAYOUT_DIRECTION_RTL; setPadding(10,0,10,0) }
        row1.addView(financeTile(host, "فروش امروز", ScreenUi.money(d.salesToday), CafeTheme.blue) { host.navigate(MainActivity.Route.INVOICES) }, LinearLayout.LayoutParams(0,-2,1f))
        row1.addView(financeTile(host, "دریافتی امروز", ScreenUi.money(d.receiptsToday), CafeTheme.leaf) { host.navigate(MainActivity.Route.PAYMENTS) }, LinearLayout.LayoutParams(0,-2,1f))
        content.addView(row1)
        val row2 = LinearLayout(host).apply { orientation = LinearLayout.HORIZONTAL; layoutDirection = View.LAYOUT_DIRECTION_RTL; setPadding(10,0,10,0) }
        row2.addView(financeTile(host, "مطالبات کل", ScreenUi.money(d.receivable), CafeTheme.danger) { host.navigate(MainActivity.Route.PAYMENTS) }, LinearLayout.LayoutParams(0,-2,1f))
        row2.addView(financeTile(host, "هزینه امروز", ScreenUi.money(d.expensesToday), CafeTheme.mocha) { host.navigate(MainActivity.Route.EXPENSES) }, LinearLayout.LayoutParams(0,-2,1f))
        content.addView(row2)
        content.addView(financeTile(host, "سود خالص امروز", ScreenUi.money(d.netProfitToday), if (d.netProfitToday >= 0) CafeTheme.leaf else CafeTheme.danger) { host.navigate(MainActivity.Route.REPORTS) })
        content.addView(CafeTheme.card(host, "ماه جاری", "فروش: ${ScreenUi.money(month.sales)}\nدریافتی: ${ScreenUi.money(month.receipts)}\nسود خالص: ${ScreenUi.money(month.netProfit)}", CafeIconView.Icon.REPORT).apply { setOnClickListener { host.navigate(MainActivity.Route.REPORTS) } })

        content.addView(sectionLabel(host, "یادآور", Color.rgb(254,159,77), Color.WHITE))
        content.addView(reminderRow(host))
        val nextReminders = host.db.reminders(includeDone = false).take(3)
        if (nextReminders.isEmpty()) content.addView(ScreenUi.empty(host, "یادآور بازی وجود ندارد"))
        nextReminders.forEach { reminder ->
            content.addView(CafeTheme.card(host, reminder.title, "${ScreenUi.reminderKind(reminder.kind)} • ${ScreenUi.date(reminder.dueAt)}${if (reminder.amount > 0) "\n${ScreenUi.money(reminder.amount)}" else ""}", CafeIconView.Icon.BELL).apply {
                setOnClickListener { host.navigate(MainActivity.Route.REMINDERS) }
            })
        }
        page.addView(ScreenUi.scroll(host, content), LinearLayout.LayoutParams(-1,0,1f))
        return page
    }

    /** نوار هفتگی بر اساس تاریخ جلالی واقعی؛ شنبه ابتدای هفته است. */
    private fun weeklyCalendar(host: MainActivity): View {
        val row = LinearLayout(host).apply {
            orientation = LinearLayout.HORIZONTAL; layoutDirection = View.LAYOUT_DIRECTION_RTL
            setPadding(CafeTheme.dp(host,8), CafeTheme.dp(host,10), CafeTheme.dp(host,8), CafeTheme.dp(host,10))
            background = CafeTheme.rounded(Color.rgb(255,246,182), CafeTheme.dp(host,14).toFloat())
        }
        val dayNames = arrayOf("ش", "ی", "د", "س", "چ", "پ", "ج")
        val now = Calendar.getInstance()
        val daysFromSaturday = (now.get(Calendar.DAY_OF_WEEK) + 1) % 7
        val start = now.clone() as Calendar
        start.add(Calendar.DAY_OF_MONTH, -daysFromSaturday)
        repeat(7) { index ->
            val c = start.clone() as Calendar; c.add(Calendar.DAY_OF_MONTH, index)
            val j = JalaliDate.fromGregorian(c.get(Calendar.YEAR), c.get(Calendar.MONTH)+1, c.get(Calendar.DAY_OF_MONTH))
            val selected = c.get(Calendar.YEAR) == now.get(Calendar.YEAR) && c.get(Calendar.DAY_OF_YEAR) == now.get(Calendar.DAY_OF_YEAR)
            row.addView(LinearLayout(host).apply {
                orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER; setPadding(2,7,2,7)
                background = CafeTheme.rounded(if (selected) Color.rgb(254,159,77) else Color.TRANSPARENT, CafeTheme.dp(host,12).toFloat())
                addView(TextView(host).apply { text = dayNames[index]; textSize = 12f; gravity = Gravity.CENTER; setTextColor(if (selected) Color.WHITE else CafeTheme.ink); typeface = Typeface.DEFAULT_BOLD })
                addView(TextView(host).apply { text = ScreenUi.fa(j.day.toString()); textSize = 15f; gravity = Gravity.CENTER; setTextColor(if (selected) Color.WHITE else CafeTheme.ink) })
            }, LinearLayout.LayoutParams(0,CafeTheme.dp(host,62),1f))
        }
        return row
    }

    private fun sectionLabel(host: MainActivity, text: String, color: Int, textColor: Int): TextView = TextView(host).apply {
        this.text = text; textSize = 14f; typeface = Typeface.DEFAULT_BOLD; gravity = Gravity.CENTER; setTextColor(textColor)
        background = CafeTheme.rounded(color, CafeTheme.dp(host,10).toFloat()); setPadding(12,9,12,9)
        layoutParams = LinearLayout.LayoutParams(CafeTheme.dp(host,110),-2).apply { gravity = Gravity.END; setMargins(12,14,12,5) }
    }

    private fun miroBlock(host: MainActivity, color: Int, title: String, subtitle: String, click: () -> Unit): View = LinearLayout(host).apply {
        orientation = LinearLayout.VERTICAL; gravity = Gravity.END; setPadding(CafeTheme.dp(host,16),CafeTheme.dp(host,14),CafeTheme.dp(host,16),CafeTheme.dp(host,14))
        background = CafeTheme.rounded(color, CafeTheme.dp(host,12).toFloat()); layoutParams = LinearLayout.LayoutParams(-1,-2).apply { setMargins(12,5,12,7) }
        addView(TextView(host).apply { text = title; textSize = 18f; typeface = Typeface.DEFAULT_BOLD; gravity = Gravity.END; setTextColor(Color.WHITE) })
        addView(TextView(host).apply { text = subtitle; textSize = 13f; gravity = Gravity.END; setTextColor(Color.WHITE); setPadding(0,8,0,0) })
        setOnClickListener { click() }
    }

    private fun financeTile(host: MainActivity, title: String, value: String, color: Int, click: (() -> Unit)? = null): View = LinearLayout(host).apply {
        orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER; setPadding(8,12,8,12); background = CafeTheme.rounded(color, CafeTheme.dp(host,10).toFloat())
        layoutParams = LinearLayout.LayoutParams(-1,-2).apply { setMargins(6,6,6,6) }
        addView(TextView(host).apply { text = title; textSize = 13f; typeface = Typeface.DEFAULT_BOLD; gravity = Gravity.CENTER; setTextColor(Color.WHITE) })
        addView(TextView(host).apply { text = value; textSize = 12f; gravity = Gravity.CENTER; setTextColor(Color.WHITE); setPadding(0,6,0,0) })
        if (click != null) setOnClickListener { click() }
    }

    private fun reminderRow(host: MainActivity): View {
        val reminders = host.db.reminders(includeDone = false)
        val installments = reminders.count { it.kind == "INSTALLMENT" }
        val checks = reminders.count { it.kind == "CHECK" }
        val others = reminders.size - installments - checks
        return LinearLayout(host).apply {
            orientation = LinearLayout.HORIZONTAL; layoutDirection = View.LAYOUT_DIRECTION_RTL; setPadding(8,4,8,8)
            addView(reminderTile(host,"اقساط","${ScreenUi.fa(installments.toString())} مورد"), LinearLayout.LayoutParams(0,-2,1f))
            addView(reminderTile(host,"چک‌ها","${ScreenUi.fa(checks.toString())} مورد"), LinearLayout.LayoutParams(0,-2,1f))
            addView(reminderTile(host,"سایر","${ScreenUi.fa(others.toString())} مورد"), LinearLayout.LayoutParams(0,-2,1f))
        }
    }
    private fun reminderTile(host: MainActivity, title: String, value: String): View = LinearLayout(host).apply {
        orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER; setPadding(5,10,5,10); background = CafeTheme.rounded(Color.rgb(254,159,77), CafeTheme.dp(host,10).toFloat())
        layoutParams = LinearLayout.LayoutParams(-1,-2).apply { setMargins(4,4,4,4) }
        addView(TextView(host).apply { text = title; textSize = 11f; typeface = Typeface.DEFAULT_BOLD; gravity = Gravity.CENTER; setTextColor(Color.WHITE) })
        addView(TextView(host).apply { text = value; textSize = 12f; gravity = Gravity.CENTER; setTextColor(CafeTheme.ink); setPadding(0,7,0,0) })
        setOnClickListener { host.navigate(MainActivity.Route.REMINDERS) }
    }
}
