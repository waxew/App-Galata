package com.asteam.galata

import android.app.Activity
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.drawerlayout.widget.DrawerLayout
import com.asteam.galata.ui.*

/**
 * اکتیویتی اصلی نسخه 0.2 گالاتا.
 * این فایل فقط پوسته برنامه، Drawer واقعی و ناوبری بین صفحه‌های مستقل را مدیریت می‌کند.
 * ظاهر و منطق هر صفحه در فایل جداگانه همان صفحه قرار دارد.
 */
class MainActivity : Activity() {
    lateinit var db: GalataDb
        private set

    private lateinit var drawerLayout: DrawerLayout
    private lateinit var contentFrame: FrameLayout
    private lateinit var drawerPanel: LinearLayout
    private var loggedIn = false
    private var currentRoute = Route.AUTH
    private var detailBackRoute: Route? = null

    enum class Route {
        AUTH, HOME, SALE, PAYMENTS, CUSTOMERS, PRODUCTS, INVOICES, REMINDERS, EXPENSES, ABOUT, CONTACT
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        db = GalataDb(this)
        window.decorView.layoutDirection = View.LAYOUT_DIRECTION_RTL
        buildShell()
        navigate(Route.AUTH)
    }

    /** پوسته اصلی شامل محتوای صفحه و Drawer استاندارد سمت راست. */
    private fun buildShell() {
        drawerLayout = DrawerLayout(this).apply {
            layoutDirection = View.LAYOUT_DIRECTION_RTL
            setBackgroundColor(CafeTheme.cream)
        }
        contentFrame = FrameLayout(this).apply {
            layoutParams = DrawerLayout.LayoutParams(
                DrawerLayout.LayoutParams.MATCH_PARENT,
                DrawerLayout.LayoutParams.MATCH_PARENT
            )
        }
        drawerPanel = buildDrawer()
        drawerLayout.addView(contentFrame)
        drawerLayout.addView(drawerPanel)
        setContentView(drawerLayout)
    }

    /**
     * Drawer کافه‌ای واقعی.
     * برخلاف نسخه 0.1 این منو Popup نیست و از سمت راست صفحه باز می‌شود.
     */
    private fun buildDrawer(): LinearLayout = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        gravity = Gravity.TOP
        layoutDirection = View.LAYOUT_DIRECTION_RTL
        setPadding(
            CafeTheme.dp(this@MainActivity, 16),
            CafeTheme.dp(this@MainActivity, 24),
            CafeTheme.dp(this@MainActivity, 16),
            CafeTheme.dp(this@MainActivity, 16)
        )
        background = CafeTheme.rounded(CafeTheme.espresso, 0f)
        layoutParams = DrawerLayout.LayoutParams(
            CafeTheme.dp(this@MainActivity, 310),
            DrawerLayout.LayoutParams.MATCH_PARENT
        ).apply { gravity = Gravity.END }

        // هدر پروفایل و برند کافه.
        addView(GalataLogoView(this@MainActivity).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                CafeTheme.dp(this@MainActivity, 150)
            )
        })
        addView(TextView(this@MainActivity).apply {
            text = db.ownerName()
            textSize = 17f
            gravity = Gravity.CENTER
            setTextColor(CafeTheme.foam)
            setPadding(8, 4, 8, 14)
        })
        addView(drawerDivider())

        menuItem("خانه", CafeIconView.Icon.HOME) { navigate(Route.HOME) }
        menuItem("فروش کافه", CafeIconView.Icon.COFFEE) { navigate(Route.SALE) }
        menuItem("تسویه مشتری", CafeIconView.Icon.WALLET) { navigate(Route.PAYMENTS) }
        menuItem("مشتری‌ها", CafeIconView.Icon.CUSTOMER) { navigate(Route.CUSTOMERS) }
        menuItem("منوی کافه / محصولات", CafeIconView.Icon.PRODUCT) { navigate(Route.PRODUCTS) }
        menuItem("فاکتورها", CafeIconView.Icon.INVOICE) { navigate(Route.INVOICES) }
        menuItem("یادآورها", CafeIconView.Icon.BELL) { navigate(Route.REMINDERS) }
        menuItem("هزینه‌ها", CafeIconView.Icon.EXPENSE) { navigate(Route.EXPENSES) }

        addView(drawerDivider())
        menuItem("درباره نرم‌افزار", CafeIconView.Icon.COFFEE) { navigate(Route.ABOUT) }
        menuItem("تماس با ما", CafeIconView.Icon.TEA) { navigate(Route.CONTACT) }
        menuItem("خروج", CafeIconView.Icon.HOME) { logout() }
    }

    private fun drawerDivider(): View = View(this).apply {
        setBackgroundColor(CafeTheme.caramel)
        layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            1
        ).apply {
            setMargins(0, CafeTheme.dp(this@MainActivity, 8), 0, CafeTheme.dp(this@MainActivity, 8))
        }
    }

    /** افزودن یک ردیف استاندارد به Drawer با آیکون کدنویسی‌شده. */
    private fun LinearLayout.menuItem(
        title: String,
        iconType: CafeIconView.Icon,
        action: () -> Unit
    ) {
        addView(LinearLayout(this@MainActivity).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutDirection = View.LAYOUT_DIRECTION_RTL
            setPadding(
                CafeTheme.dp(context, 8),
                CafeTheme.dp(context, 7),
                CafeTheme.dp(context, 8),
                CafeTheme.dp(context, 7)
            )
            addView(CafeIconView(context).apply {
                icon = iconType
                layoutParams = LinearLayout.LayoutParams(
                    CafeTheme.dp(context, 38),
                    CafeTheme.dp(context, 38)
                ).apply { marginStart = CafeTheme.dp(context, 10) }
            })
            addView(TextView(context).apply {
                text = title
                textSize = 15f
                setTextColor(CafeTheme.foam)
                gravity = Gravity.END
            }, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
            setOnClickListener { action() }
        })
    }

    /** ناوبری مرکزی؛ هر Route دقیقاً به یک فایل صفحه مستقل متصل است. */
    fun navigate(route: Route) {
        currentRoute = route
        detailBackRoute = null
        if (::drawerLayout.isInitialized && loggedIn) drawerLayout.closeDrawer(Gravity.END)

        val screen: GalataScreen = when (route) {
            Route.AUTH -> AuthScreen()
            Route.HOME -> HomeScreen()
            Route.SALE -> SaleScreen()
            Route.PAYMENTS -> PaymentsScreen()
            Route.CUSTOMERS -> CustomersScreen()
            Route.PRODUCTS -> ProductsScreen()
            Route.INVOICES -> InvoicesScreen()
            Route.REMINDERS -> RemindersScreen()
            Route.EXPENSES -> ExpensesScreen()
            Route.ABOUT -> AboutScreen()
            Route.CONTACT -> ContactScreen()
        }

        contentFrame.removeAllViews()
        contentFrame.addView(screen.build(this))
    }

    /** ورود به صورت‌حساب مشتری با برگشت صحیح به فهرست مشتری‌ها. */
    fun showLedger(customer: Customer) {
        detailBackRoute = Route.CUSTOMERS
        contentFrame.removeAllViews()
        contentFrame.addView(LedgerScreen(customer).build(this))
    }

    /** بازکردن Drawer و تازه‌سازی نام صاحب کافه در هدر. */
    fun openDrawer() {
        if (!loggedIn) return
        drawerLayout.removeView(drawerPanel)
        drawerPanel = buildDrawer()
        drawerLayout.addView(drawerPanel)
        drawerLayout.openDrawer(Gravity.END)
    }

    fun markLoggedIn() {
        loggedIn = true
    }

    fun logout() {
        loggedIn = false
        navigate(Route.AUTH)
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        when {
            drawerLayout.isDrawerOpen(Gravity.END) -> drawerLayout.closeDrawer(Gravity.END)
            detailBackRoute != null -> navigate(detailBackRoute!!)
            loggedIn && currentRoute != Route.HOME -> navigate(Route.HOME)
            else -> super.onBackPressed()
        }
    }
}
