package com.asteam.galata.ui

import android.graphics.Typeface
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.Space
import android.widget.TextView
import com.asteam.galata.MainActivity

/** صفحه تماس با ما مطابق ساختار مشترک پروژه‌های Android App. */
class ContactScreen : GalataScreen {
    override fun build(host: MainActivity): View {
        val page = ScreenUi.page(host, "تماس با ما", "ارتباط با تیم توسعه", CafeIconView.Icon.TEA)
        val content = ScreenUi.content(host)
        content.addView(CafeTheme.card(host, "پشتیبانی گالاتا", "as.team.support@gmail.com", CafeIconView.Icon.TEA))
        content.addView(Space(host), LinearLayout.LayoutParams(-1, 0, 1f))
        content.addView(View(host).apply {
            setBackgroundColor(CafeTheme.caramel)
            layoutParams = LinearLayout.LayoutParams(-1, 1).apply { setMargins(24, 24, 24, 16) }
        })
        content.addView(TextView(host).apply {
            text = "گروه توسعه فناوری و نرم افزاری as Team"
            gravity = Gravity.CENTER
            textSize = 15f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(CafeTheme.espresso)
        })
        content.addView(TextView(host).apply {
            text = "as.team.support@gmail.com"
            gravity = Gravity.CENTER
            textSize = 13f
            setTextColor(CafeTheme.mocha)
            setPadding(0, 6, 0, 20)
        })
        page.addView(content, LinearLayout.LayoutParams(-1,0,1f))
        return page
    }
}
