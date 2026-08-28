package com.asteam.galata.ui

import android.graphics.Color
import android.graphics.Typeface
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import com.asteam.galata.MainActivity
import java.util.Calendar

/**
 * صفحه Home بر اساس ساختار فریم Activity / Home در Miro.
 * ترتیب اصلی صفحه: تقویم هفتگی، Galata، Money و Reminder.
 */
class HomeScreen : GalataScreen {
    override fun build(host: MainActivity): View {
        val d = host.db.dashboard()
        val page = ScreenUi.page(host, "گالاتا", host.db.ownerName(), CafeIconView.Icon.COFFEE)
        val content = ScreenUi.content(host)

        // ---------- Calendar ----------
        content.addView(sectionLabel(host, "تقویم", Color.rgb(255, 246, 182), CafeTheme.ink))
        content.addView(weeklyCalendar(host))
        content.addView(TextView(host).apply {
            text = "مناسبت‌ها و یادداشت‌های این ماه در بخش تقویم نمایش داده می‌شوند"
            textSize = 12f
            gravity = Gravity.CENTER
            setTextColor(CafeTheme.mocha)
            setPadding(12, 8, 12, 14)
        })

        // ---------- Galata / فروش ----------
        content.addView(sectionLabel(host, "Galata", Color.rgb(254, 159, 77), Color.WHITE))
        content.addView(miroBlock(host, Color.rgb(254, 159, 77), "فروش", "مجموع فروش کل امروز\n${ScreenUi.money(d.salesToday)}") {
            host.navigate(MainActivity.Route.SALE)
        })

        // ---------- Money ----------
        content.addView(sectionLabel(host, "مالی", Color.rgb(254, 159, 77), Color.WHITE))
        val monthSales = currentMonthSales(host)
        val moneyRow = LinearLayout(host).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutDirection = View.LAYOUT_DIRECTION_RTL
            setPadding(CafeTheme.dp(host, 10), 0, CafeTheme.dp(host, 10), 0)
        }
        moneyRow.addView(financeTile(host, "درآمد این ماه", ScreenUi.money(monthSales), Color.rgb(189, 10, 10), null), LinearLayout.LayoutParams(0, -2, 1f))
        moneyRow.addView(financeTile(host, "تسویه مشتری", "مشاهده و ثبت دریافت", Color.rgb(101, 157, 242)) {
            host.navigate(MainActivity.Route.PAYMENTS)
        }, LinearLayout.LayoutParams(0, -2, 1f))
        content.addView(moneyRow)
        content.addView(financeTile(host, "جمع کل پول بیرون", ScreenUi.money(d.expenses), Color.rgb(189, 10, 10)) {
            host.navigate(MainActivity.Route.EXPENSES)
        })

        // ---------- Reminder ----------
        content.addView(sectionLabel(host, "یاد آور", Color.rgb(254, 159, 77), Color.WHITE))
        content.addView(reminderRow(host))

        page.addView(ScreenUi.scroll(host, content), LinearLayout.LayoutParams(-1, 0, 1f))
        return page
    }

    /** نوار هفت روزه مطابق درخواست «نمایش تقویم بصورت هفتگی». */
    private fun weeklyCalendar(host: MainActivity): View {
        val row = LinearLayout(host).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutDirection = View.LAYOUT_DIRECTION_RTL
            setPadding(CafeTheme.dp(host, 8), CafeTheme.dp(host, 10), CafeTheme.dp(host, 8), CafeTheme.dp(host, 10))
            background = CafeTheme.rounded(Color.rgb(255, 246, 182), CafeTheme.dp(host, 14).toFloat())
        }

        val dayNames = arrayOf("ش", "ی", "د", "س", "چ", "پ", "ج")
        val today = Calendar.getInstance()
        val start = today.clone() as Calendar
        val javaDay = today.get(Calendar.DAY_OF_WEEK)
        // در این تبدیل شنبه ابتدای هفته است.
        val daysFromSaturday = (javaDay + 1) % 7
        start.add(Calendar.DAY_OF_MONTH, -daysFromSaturday)

        repeat(7) { index ->
            val c = start.clone() as Calendar
            c.add(Calendar.DAY_OF_MONTH, index)
            val selected = c.get(Calendar.YEAR) == today.get(Calendar.YEAR) && c.get(Calendar.DAY_OF_YEAR) == today.get(Calendar.DAY_OF_YEAR)
            row.addView(LinearLayout(host).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER
                setPadding(2, 7, 2, 7)
                background = CafeTheme.rounded(
                    if (selected) Color.rgb(254, 159, 77) else Color.TRANSPARENT,
                    CafeTheme.dp(host, 12).toFloat()
                )
                addView(TextView(host).apply {
                    text = dayNames[index]
                    textSize = 12f
                    gravity = Gravity.CENTER
                    setTextColor(if (selected) Color.WHITE else CafeTheme.ink)
                    typeface = Typeface.DEFAULT_BOLD
                })
                addView(TextView(host).apply {
                    text = ScreenUi.fa(c.get(Calendar.DAY_OF_MONTH).toString())
                    textSize = 15f
                    gravity = Gravity.CENTER
                    setTextColor(if (selected) Color.WHITE else CafeTheme.ink)
                })
            }, LinearLayout.LayoutParams(0, CafeTheme.dp(host, 62), 1f))
        }
        return row
    }

    private fun sectionLabel(host: MainActivity, text: String, color: Int, textColor: Int): TextView = TextView(host).apply {
        this.text = text
        textSize = 14f
        typeface = Typeface.DEFAULT_BOLD
        gravity = Gravity.CENTER
        setTextColor(textColor)
        background = CafeTheme.rounded(color, CafeTheme.dp(host, 10).toFloat())
        setPadding(12, 9, 12, 9)
        layoutParams = LinearLayout.LayoutParams(CafeTheme.dp(host, 110), -2).apply {
            gravity = Gravity.END
            setMargins(12, 14, 12, 5)
        }
    }

    private fun miroBlock(host: MainActivity, color: Int, title: String, subtitle: String, click: () -> Unit): View = LinearLayout(host).apply {
        orientation = LinearLayout.VERTICAL
        gravity = Gravity.END
        setPadding(CafeTheme.dp(host, 16), CafeTheme.dp(host, 14), CafeTheme.dp(host, 16), CafeTheme.dp(host, 14))
        background = CafeTheme.rounded(color, CafeTheme.dp(host, 12).toFloat())
        layoutParams = LinearLayout.LayoutParams(-1, -2).apply { setMargins(12, 5, 12, 7) }
        addView(TextView(host).apply {
            text = title; textSize = 18f; typeface = Typeface.DEFAULT_BOLD; gravity = Gravity.END; setTextColor(Color.WHITE)
        })
        addView(TextView(host).apply {
            text = subtitle; textSize = 13f; gravity = Gravity.END; setTextColor(Color.WHITE); setPadding(0, 8, 0, 0)
        })
        setOnClickListener { click() }
    }

    private fun financeTile(host: MainActivity, title: String, value: String, color: Int, click: (() -> Unit)? = null): View = LinearLayout(host).apply {
        orientation = LinearLayout.VERTICAL
        gravity = Gravity.CENTER
        setPadding(8, 12, 8, 12)
        background = CafeTheme.rounded(color, CafeTheme.dp(host, 10).toFloat())
        layoutParams = LinearLayout.LayoutParams(-1, -2).apply { setMargins(6, 6, 6, 6) }
        addView(TextView(host).apply { text = title; textSize = 13f; typeface = Typeface.DEFAULT_BOLD; gravity = Gravity.CENTER; setTextColor(Color.WHITE) })
        addView(TextView(host).apply { text = value; textSize = 12f; gravity = Gravity.CENTER; setTextColor(Color.WHITE); setPadding(0, 6, 0, 0) })
        if (click != null) setOnClickListener { click() }
    }

    /** سه ستون یادآور مطابق Miro: اقساط، چک، یادداشت. */
    private fun reminderRow(host: MainActivity): View {
        val reminders = host.db.reminders()
        val installments = reminders.count { it.title.contains("قسط") || it.title.contains("اقساط") }
        val checks = reminders.count { it.title.contains("چک") }
        val notes = reminders.size - installments - checks

        return LinearLayout(host).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutDirection = View.LAYOUT_DIRECTION_RTL
            setPadding(CafeTheme.dp(host, 8), 4, CafeTheme.dp(host, 8), 8)
            addView(reminderTile(host, "یاد آور اقساط", "${ScreenUi.fa(installments.toString())} مورد"), LinearLayout.LayoutParams(0, -2, 1f))
            addView(reminderTile(host, "یاد آور چک", "${ScreenUi.fa(checks.toString())} مورد"), LinearLayout.LayoutParams(0, -2, 1f))
            addView(reminderTile(host, "یاد آور یادداشت", "${ScreenUi.fa(notes.toString())} مورد"), LinearLayout.LayoutParams(0, -2, 1f))
        }
    }

    private fun reminderTile(host: MainActivity, title: String, value: String): View = LinearLayout(host).apply {
        orientation = LinearLayout.VERTICAL
        gravity = Gravity.CENTER
        setPadding(5, 10, 5, 10)
        background = CafeTheme.rounded(Color.rgb(254, 159, 77), CafeTheme.dp(host, 10).toFloat())
        layoutParams = LinearLayout.LayoutParams(-1, -2).apply { setMargins(4, 4, 4, 4) }
        addView(TextView(host).apply { text = title; textSize = 11f; typeface = Typeface.DEFAULT_BOLD; gravity = Gravity.CENTER; setTextColor(Color.WHITE) })
        addView(TextView(host).apply { text = value; textSize = 12f; gravity = Gravity.CENTER; setTextColor(CafeTheme.ink); setPadding(0, 7, 0, 0) })
        setOnClickListener { host.navigate(MainActivity.Route.REMINDERS) }
    }

    private fun currentMonthSales(host: MainActivity): Long {
        val now = Calendar.getInstance()
        return host.db.invoices().filter { invoice ->
            val c = Calendar.getInstance().apply { timeInMillis = invoice.createdAt }
            c.get(Calendar.YEAR) == now.get(Calendar.YEAR) && c.get(Calendar.MONTH) == now.get(Calendar.MONTH)
        }.sumOf { it.total }
    }
}
