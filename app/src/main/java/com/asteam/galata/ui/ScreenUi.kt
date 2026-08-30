package com.asteam.galata.ui

import android.app.AlertDialog
import android.graphics.Color
import android.graphics.Typeface
import android.text.InputType
import android.view.Gravity
import android.view.View
import android.widget.*
import com.asteam.galata.JalaliDate
import com.asteam.galata.MainActivity
import java.text.NumberFormat
import java.util.Calendar
import java.util.Locale

/** ابزارهای مشترک رابط؛ فرمت پول/تاریخ و اجزای تکراری فقط در این فایل تعریف می‌شوند. */
object ScreenUi {
    fun page(host: MainActivity, title: String, subtitle: String, icon: CafeIconView.Icon, showMenu: Boolean = true): LinearLayout {
        val root = LinearLayout(host).apply {
            orientation = LinearLayout.VERTICAL
            layoutDirection = View.LAYOUT_DIRECTION_RTL
            setBackgroundColor(CafeTheme.cream)
        }
        root.addView(topBar(host, title, subtitle, icon, showMenu))
        return root
    }

    private fun topBar(host: MainActivity, title: String, subtitle: String, icon: CafeIconView.Icon, showMenu: Boolean): View = LinearLayout(host).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        setPadding(CafeTheme.dp(host, 12), CafeTheme.dp(host, 12), CafeTheme.dp(host, 12), CafeTheme.dp(host, 12))
        background = CafeTheme.rounded(CafeTheme.mocha, 0f)
        if (showMenu) addView(TextView(host).apply {
            text = "☰"; textSize = 29f; gravity = Gravity.CENTER; setTextColor(CafeTheme.foam); setOnClickListener { host.openDrawer() }
        }, LinearLayout.LayoutParams(CafeTheme.dp(host, 52), CafeTheme.dp(host, 52)))
        addView(LinearLayout(host).apply {
            orientation = LinearLayout.VERTICAL; gravity = Gravity.END
            addView(TextView(host).apply { text = title; textSize = 20f; setTextColor(CafeTheme.foam); gravity = Gravity.END; typeface = Typeface.DEFAULT_BOLD })
            addView(TextView(host).apply { text = subtitle; textSize = 12f; setTextColor(Color.rgb(231, 211, 191)); gravity = Gravity.END })
        }, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        addView(CafeIconView(host).apply { this.icon = icon }, LinearLayout.LayoutParams(CafeTheme.dp(host, 46), CafeTheme.dp(host, 46)))
    }

    fun scroll(host: MainActivity, content: LinearLayout): ScrollView = ScrollView(host).apply { isFillViewport = true; addView(content) }
    fun content(host: MainActivity): LinearLayout = LinearLayout(host).apply {
        orientation = LinearLayout.VERTICAL; layoutDirection = View.LAYOUT_DIRECTION_RTL
        setPadding(0, CafeTheme.dp(host, 10), 0, CafeTheme.dp(host, 28))
    }

    fun action(host: MainActivity, text: String, subtitle: String, icon: CafeIconView.Icon, click: () -> Unit): View =
        CafeTheme.card(host, text, subtitle, icon).apply { setOnClickListener { click() } }

    fun primaryButton(host: MainActivity, text: String, click: () -> Unit): Button = Button(host).apply {
        this.text = text; isAllCaps = false; textSize = 15f; setTextColor(Color.WHITE)
        background = CafeTheme.rounded(CafeTheme.leaf, CafeTheme.dp(host, 16).toFloat())
        setOnClickListener { click() }
        layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, CafeTheme.dp(host, 54)).apply {
            setMargins(CafeTheme.dp(host, 14), CafeTheme.dp(host, 8), CafeTheme.dp(host, 14), CafeTheme.dp(host, 8))
        }
    }

    fun dangerButton(host: MainActivity, text: String, click: () -> Unit): Button = primaryButton(host, text, click).apply {
        background = CafeTheme.rounded(CafeTheme.danger, CafeTheme.dp(host, 16).toFloat())
    }

    fun input(host: MainActivity, hint: String, numeric: Boolean = false): EditText = EditText(host).apply {
        this.hint = hint; gravity = Gravity.END; textDirection = View.TEXT_DIRECTION_RTL
        setTextColor(CafeTheme.ink); setHintTextColor(Color.rgb(135, 112, 94))
        setPadding(CafeTheme.dp(host, 14), CafeTheme.dp(host, 10), CafeTheme.dp(host, 14), CafeTheme.dp(host, 10))
        background = CafeTheme.rounded(CafeTheme.foam, CafeTheme.dp(host, 14).toFloat(), Color.rgb(222, 202, 183), 1)
        if (numeric) inputType = InputType.TYPE_CLASS_NUMBER
        layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
            setMargins(CafeTheme.dp(host, 14), CafeTheme.dp(host, 6), CafeTheme.dp(host, 14), CafeTheme.dp(host, 6))
        }
    }

    fun form(host: MainActivity, vararg views: View): LinearLayout = LinearLayout(host).apply {
        orientation = LinearLayout.VERTICAL; layoutDirection = View.LAYOUT_DIRECTION_RTL
        setPadding(CafeTheme.dp(host, 8), CafeTheme.dp(host, 8), CafeTheme.dp(host, 8), 0); views.forEach { addView(it) }
    }

    fun stat(host: MainActivity, label: String, value: String, icon: CafeIconView.Icon): View = CafeTheme.card(host, label, value, icon)
    fun empty(host: MainActivity, message: String): View = CafeTheme.card(host, "هنوز موردی ثبت نشده", message, CafeIconView.Icon.COFFEE)
    fun infoDialog(host: MainActivity, title: String, text: String) { AlertDialog.Builder(host).setTitle(title).setMessage(text).setPositiveButton("باشه", null).show() }
    fun confirm(host: MainActivity, title: String, message: String, positive: String = "تأیید", action: () -> Unit) {
        AlertDialog.Builder(host).setTitle(title).setMessage(message).setNegativeButton("انصراف", null).setPositiveButton(positive) { _, _ -> action() }.show()
    }
    fun toast(host: MainActivity, message: String) = Toast.makeText(host, message, Toast.LENGTH_SHORT).show()

    /** عدد تومان با ارقام فارسی و جداساز سه‌رقمی «٫». */
    fun money(value: Long): String = fa(NumberFormat.getIntegerInstance(Locale.US).format(value).replace(",", "٫")) + " تومان"

    /** تاریخ و ساعت به صورت شمسی. */
    fun date(value: Long): String {
        val c = Calendar.getInstance().apply { timeInMillis = value }
        val j = JalaliDate.fromGregorian(c.get(Calendar.YEAR), c.get(Calendar.MONTH) + 1, c.get(Calendar.DAY_OF_MONTH))
        return "${fa(j.year.toString())}/${fa(j.month.toString().padStart(2, '0'))}/${fa(j.day.toString().padStart(2, '0'))} - ${fa(c.get(Calendar.HOUR_OF_DAY).toString().padStart(2, '0'))}:${fa(c.get(Calendar.MINUTE).toString().padStart(2, '0'))}"
    }

    fun dateOnly(value: Long): String = date(value).substringBefore(" - ")

    /** تبدیل ارقام فارسی/جداساز پول به Long. */
    fun num(value: String): Long = value.replace('۰','0').replace('۱','1').replace('۲','2').replace('۳','3').replace('۴','4').replace('۵','5').replace('۶','6').replace('۷','7').replace('۸','8').replace('۹','9').replace("٫", "").replace(",", "").replace(" ", "").toLongOrNull() ?: 0L
    fun fa(value: String): String = value.replace('0','۰').replace('1','۱').replace('2','۲').replace('3','۳').replace('4','۴').replace('5','۵').replace('6','۶').replace('7','۷').replace('8','۸').replace('9','۹')

    fun reminderKind(kind: String): String = when (kind) {
        "CHECK" -> "چک"
        "INSTALLMENT" -> "قسط"
        "APPOINTMENT" -> "قرار / مراجعه"
        else -> "عمومی"
    }
}
