package com.asteam.galata.ui

import android.graphics.Typeface
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import com.asteam.galata.BuildConfig
import com.asteam.galata.MainActivity

/** صفحه درباره نرم‌افزار؛ بدون نمایش package name و اطلاعات فنی غیرضروری. */
class AboutScreen : GalataScreen {
    override fun build(host: MainActivity): View {
        val page = ScreenUi.page(host, "درباره نرم‌افزار", "معرفی گالاتا", CafeIconView.Icon.ABOUT)
        val content = ScreenUi.content(host)
        content.addView(GalataLogoView(host), LinearLayout.LayoutParams(-1, CafeTheme.dp(host, 185)))
        content.addView(CafeTheme.card(host, "گالاتا", "نرم‌افزار آفلاین مدیریت و حسابداری کسب‌وکار برای فروش، فاکتور، حساب مشتری، تسویه، هزینه، تقویم شمسی و یادآورها.\n\nنسخه ${ScreenUi.fa(BuildConfig.VERSION_NAME)}", CafeIconView.Icon.COFFEE))
        content.addView(CafeTheme.card(host, "راه‌های ارتباطی با ما:", "AS.Developers.Support@Gmail.Com", CafeIconView.Icon.CONTACT))
        content.addView(View(host).apply { setBackgroundColor(CafeTheme.caramel); layoutParams = LinearLayout.LayoutParams(-1, 1).apply { setMargins(30, 28, 30, 16) } })
        content.addView(TextView(host).apply {
            text = "Develop by AS Team Group"; gravity = Gravity.CENTER; textSize = 15f; typeface = Typeface.DEFAULT_BOLD; setTextColor(CafeTheme.espresso); setPadding(8, 8, 8, 18)
        })
        page.addView(ScreenUi.scroll(host, content), LinearLayout.LayoutParams(-1, 0, 1f))
        return page
    }
}
