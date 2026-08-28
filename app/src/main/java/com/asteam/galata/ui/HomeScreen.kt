package com.asteam.galata.ui

import android.view.View
import android.widget.LinearLayout
import com.asteam.galata.MainActivity

/** داشبورد اصلی کافه با کارت‌های مالی و میانبرهای گرافیکی. */
class HomeScreen : GalataScreen {
    override fun build(host: MainActivity): View {
        val d = host.db.dashboard()
        val page = ScreenUi.page(host, "گالاتا", "${host.db.ownerName()} — مدیریت کافه", CafeIconView.Icon.COFFEE)
        val content = ScreenUi.content(host)

        content.addView(GalataLogoView(host).apply {
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, CafeTheme.dp(host, 180))
        })
        content.addView(CafeTheme.sectionTitle(host, "وضعیت امروز کافه"))
        content.addView(ScreenUi.stat(host, "فروش امروز", ScreenUi.money(d.salesToday), CafeIconView.Icon.COFFEE))
        content.addView(ScreenUi.stat(host, "دریافتی امروز", ScreenUi.money(d.receiptsToday), CafeIconView.Icon.WALLET))
        content.addView(ScreenUi.stat(host, "مطالبات مشتریان", ScreenUi.money(d.receivable), CafeIconView.Icon.CUSTOMER))
        content.addView(ScreenUi.stat(host, "هزینه‌ها", ScreenUi.money(d.expenses), CafeIconView.Icon.EXPENSE))

        content.addView(CafeTheme.sectionTitle(host, "دسترسی سریع"))
        content.addView(ScreenUi.action(host, "ثبت فروش", "قهوه، چای، نوشیدنی و خدمات", CafeIconView.Icon.COFFEE) { host.navigate(MainActivity.Route.SALE) })
        content.addView(ScreenUi.action(host, "تسویه مشتری", "پرداخت کامل یا جزئی", CafeIconView.Icon.WALLET) { host.navigate(MainActivity.Route.PAYMENTS) })
        content.addView(ScreenUi.action(host, "فاکتورها", "${ScreenUi.fa(d.invoiceCount.toString())} فاکتور ثبت شده", CafeIconView.Icon.INVOICE) { host.navigate(MainActivity.Route.INVOICES) })
        content.addView(ScreenUi.action(host, "یادآورها", "${ScreenUi.fa(d.reminderCount.toString())} یادآور", CafeIconView.Icon.BELL) { host.navigate(MainActivity.Route.REMINDERS) })

        page.addView(ScreenUi.scroll(host, content), LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f))
        return page
    }
}
