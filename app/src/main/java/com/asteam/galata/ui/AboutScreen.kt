package com.asteam.galata.ui

import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import com.asteam.galata.MainActivity

/** صفحه درباره نرم‌افزار؛ فقط توضیح برنامه و نسخه، بدون اطلاعات فنی پکیج. */
class AboutScreen : GalataScreen {
    override fun build(host: MainActivity): View {
        val page = ScreenUi.page(host, "درباره نرم‌افزار", "معرفی گالاتا", CafeIconView.Icon.COFFEE)
        val content = ScreenUi.content(host)
        content.addView(GalataLogoView(host), LinearLayout.LayoutParams(-1, CafeTheme.dp(host, 190)))
        content.addView(CafeTheme.card(host, "گالاتا", "نرم‌افزار آفلاین مدیریت کافه برای فروش، مشتری‌ها، فاکتور، تسویه، هزینه و یادآورها.\n\nنسخه ۰.۲.۰", CafeIconView.Icon.COFFEE))
        content.addView(TextView(host).apply {
            text = "تمامی حقوق مربوط به این برنامه انحصاری میباشد"
            gravity = Gravity.CENTER
            setTextColor(CafeTheme.mocha)
            setPadding(16, 20, 16, 12)
        })
        page.addView(ScreenUi.scroll(host, content), LinearLayout.LayoutParams(-1,0,1f))
        return page
    }
}
