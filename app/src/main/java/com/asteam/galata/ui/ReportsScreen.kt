package com.asteam.galata.ui

import android.view.View
import android.widget.LinearLayout
import com.asteam.galata.MainActivity

/** گزارش مالی که فروش، دریافتی، هزینه و سود را عمداً از هم جدا نشان می‌دهد. */
class ReportsScreen : GalataScreen {
    override fun build(host: MainActivity): View {
        val d = host.db.dashboard()
        val today = host.db.report(startOfToday(), startOfToday() + 86_400_000L)
        val month = host.db.currentMonthReport()
        val page = ScreenUi.page(host, "گزارش مالی", "فروش با دریافتی یکی نیست", CafeIconView.Icon.REPORT)
        val content = ScreenUi.content(host)
        content.addView(CafeTheme.sectionTitle(host, "امروز"))
        content.addView(ScreenUi.stat(host, "فروش امروز", ScreenUi.money(today.sales), CafeIconView.Icon.COFFEE))
        content.addView(ScreenUi.stat(host, "دریافتی امروز", ScreenUi.money(today.receipts), CafeIconView.Icon.WALLET))
        content.addView(ScreenUi.stat(host, "هزینه امروز", ScreenUi.money(today.expenses), CafeIconView.Icon.EXPENSE))
        content.addView(ScreenUi.stat(host, "سود ناخالص امروز", ScreenUi.money(today.grossProfit), CafeIconView.Icon.REPORT))
        content.addView(ScreenUi.stat(host, "سود خالص امروز", ScreenUi.money(today.netProfit), CafeIconView.Icon.REPORT))
        content.addView(CafeTheme.sectionTitle(host, "ماه شمسی جاری"))
        content.addView(ScreenUi.stat(host, "فروش ماه", ScreenUi.money(month.sales), CafeIconView.Icon.COFFEE))
        content.addView(ScreenUi.stat(host, "دریافتی ماه", ScreenUi.money(month.receipts), CafeIconView.Icon.WALLET))
        content.addView(ScreenUi.stat(host, "هزینه ماه", ScreenUi.money(month.expenses), CafeIconView.Icon.EXPENSE))
        content.addView(ScreenUi.stat(host, "سود خالص ماه", ScreenUi.money(month.netProfit), CafeIconView.Icon.REPORT))
        content.addView(CafeTheme.sectionTitle(host, "مانده حساب‌ها"))
        content.addView(ScreenUi.stat(host, "مطالبات از مشتریان", ScreenUi.money(d.receivable), CafeIconView.Icon.INVOICE))
        content.addView(ScreenUi.stat(host, "بستانکاری مشتریان", ScreenUi.money(d.customerCredit), CafeIconView.Icon.WALLET))
        content.addView(CafeTheme.card(host, "تعریف اعداد", "فروش = جمع فاکتورهای ثبت‌شده\nدریافتی = پولی که واقعاً دریافت شده\nسود ناخالص = فروش منهای بهای خرید اقلام\nسود خالص = سود ناخالص منهای هزینه‌های ثبت‌شده", CafeIconView.Icon.ABOUT))
        page.addView(ScreenUi.scroll(host, content), LinearLayout.LayoutParams(-1,0,1f))
        return page
    }

    private fun startOfToday(): Long = java.util.Calendar.getInstance().apply {
        set(java.util.Calendar.HOUR_OF_DAY,0); set(java.util.Calendar.MINUTE,0); set(java.util.Calendar.SECOND,0); set(java.util.Calendar.MILLISECOND,0)
    }.timeInMillis
}
