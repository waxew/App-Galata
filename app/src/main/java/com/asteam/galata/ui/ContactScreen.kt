package com.asteam.galata.ui

import android.content.Intent
import android.net.Uri
import android.view.View
import android.widget.LinearLayout
import com.asteam.galata.MainActivity

/** صفحه تماس با تیم توسعه؛ لمس کارت ایمیل، برنامه ایمیل دستگاه را باز می‌کند. */
class ContactScreen : GalataScreen {
    override fun build(host: MainActivity): View {
        val page = ScreenUi.page(host, "تماس با ما", "پشتیبانی گالاتا", CafeIconView.Icon.CONTACT)
        val content = ScreenUi.content(host)
        content.addView(CafeTheme.card(host, "پشتیبانی گالاتا", "AS.Developers.Support@Gmail.Com", CafeIconView.Icon.CONTACT).apply {
            setOnClickListener {
                try { host.startActivity(Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:AS.Developers.Support@Gmail.Com"))) }
                catch (_: Exception) { ScreenUi.toast(host, "برنامه ایمیل روی دستگاه پیدا نشد") }
            }
        })
        content.addView(CafeTheme.card(host, "تیم توسعه", "Develop by AS Team Group", CafeIconView.Icon.ABOUT))
        page.addView(ScreenUi.scroll(host, content), LinearLayout.LayoutParams(-1, 0, 1f))
        return page
    }
}
