package com.asteam.galata

import android.app.Activity
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import com.asteam.galata.ui.*

/**
 * اکتیویتی اصلی گالاتا.
 *
 * مسئولیت این فایل فقط سه مورد است:
 * ۱) پوسته اصلی برنامه و Drawer استاندارد سمت راست در محیط RTL.
 * ۲) ناوبری بین صفحه‌های مستقل.
 * ۳) کنترل نشست ورود ۲۰ ساعته.
 */
class MainActivity : Activity() {
    lateinit var db: GalataDb
        private set

    private lateinit var sessionManager: SessionManager
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var contentFrame: FrameLayout
    private lateinit var drawerPanel: LinearLayout
    private lateinit var drawerOwnerName: TextView

    private var loggedIn = false
    private var currentRoute = Route.AUTH
    private var detailBackRoute: Route? = null

    enum class Route {
        AUTH, HOME, SALE, PAYMENTS, CUSTOMERS, PRODUCTS, INVOICES, REMINDERS, EXPENSES, ABOUT, CONTACT
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        db = GalataDb(this)
        sessionManager = SessionManager(this)

        // تمام برنامه فارسی و RTL است؛ در RTL، START همان سمت راست صفحه است.
        window.decorView.layoutDirection = View.LAYOUT_DIRECTION_RTL
        buildShell()

        // اگر مالک قبلاً گزینه «به خاطر سپردن ورود» را فعال کرده باشد و ۲۰ ساعت تمام نشده باشد،
        // صفحه رمز عبور رد می‌شود و کاربر مستقیم وارد خانه می‌شود.
        if (db.hasOwner() && sessionManager.isRememberedSessionValid()) {
            loggedIn = true
            refreshDrawerHeader()
            navigate(Route.HOME)
        } else {
            sessionManager.clearRememberedSession()
            navigate(Route.AUTH)
        }
    }

    /** پوسته اصلی شامل محتوای صفحه و Drawer استاندارد از سمت راست. */
    private fun buildShell() {
        drawerLayout = DrawerLayout(this).apply {
            layoutDirection = View.LAYOUT_DIRECTION_RTL
            setBackgroundColor(CafeTheme.cream)
            // جلوگیری از رفتار مبهم Drawer با لمس‌ها؛ وضعیت قفل فقط با ورود کنترل می‌شود.
            setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED, GravityCompat.START)
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
     * Drawer واقعی گالاتا.
     * چون کل UI در حالت RTL است از GravityCompat.START استفاده می‌کنیم؛ START در RTL سمت راست است.
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
        ).apply {
            gravity = GravityCompat.START
        }

        // پروفایل/برند بالای Drawer مطابق ساختار پروژه.
        addView(GalataLogoView(this@MainActivity).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                CafeTheme.dp(this@MainActivity, 135)
            )
        })

        drawerOwnerName = TextView(this@MainActivity).apply {
            text = db.ownerName()
            textSize = 17f
            gravity = Gravity.CENTER
            setTextColor(CafeTheme.foam)
            setPadding(8, 4, 8, 14)
        }
        addView(drawerOwnerName)
        addView(drawerDivider())

        // ترتیب گزینه‌ها مطابق Activity / Setting در Miro نگه داشته شده است.
        menuItem("خانه", CafeIconView.Icon.HOME) { navigate(Route.HOME) }
        menuItem("صورت حساب", CafeIconView.Icon.INVOICE) { navigate(Route.INVOICES) }
        menuItem("لیست مشتری ها", CafeIconView.Icon.CUSTOMER) { navigate(Route.CUSTOMERS) }
        menuItem("لیست کالا / خدمات", CafeIconView.Icon.PRODUCT) { navigate(Route.PRODUCTS) }
        menuItem("یاد آور", CafeIconView.Icon.BELL) { navigate(Route.REMINDERS) }

        addView(drawerDivider())
        menuItem("درباره نرم افزار", CafeIconView.Icon.COFFEE) { navigate(Route.ABOUT) }
        menuItem("تماس با ما", CafeIconView.Icon.TEA) { navigate(Route.CONTACT) }
        menuItem("خروج", CafeIconView.Icon.HOME) { logout() }
    }

    private fun drawerDivider(): View = View(this).apply {
        setBackgroundColor(CafeTheme.caramel)
        layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            CafeTheme.dp(this@MainActivity, 1)
        ).apply {
            setMargins(0, CafeTheme.dp(this@MainActivity, 8), 0, CafeTheme.dp(this@MainActivity, 8))
        }
    }

    /** یک ردیف قابل لمس در Drawer با آیکون گرافیکی کدنویسی‌شده. */
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
            setOnClickListener {
                action()
                closeDrawer()
            }
        })
    }

    /** ناوبری مرکزی؛ هر Route به فایل مستقل همان صفحه متصل است. */
    fun navigate(route: Route) {
        currentRoute = route
        detailBackRoute = null
        closeDrawer()

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

    /** باز کردن منوی همبرگری. Drawer دیگر هنگام هر لمس حذف و دوباره ساخته نمی‌شود. */
    fun openDrawer() {
        if (!loggedIn) return
        refreshDrawerHeader()
        drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED, GravityCompat.START)
        drawerLayout.openDrawer(GravityCompat.START)
    }

    private fun closeDrawer() {
        if (::drawerLayout.isInitialized && drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START)
        }
    }

    private fun refreshDrawerHeader() {
        if (::drawerOwnerName.isInitialized) drawerOwnerName.text = db.ownerName()
    }

    /** بعد از ورود موفق فراخوانی می‌شود و انتخاب «به خاطر سپردن» را ذخیره می‌کند. */
    fun markLoggedIn(rememberFor20Hours: Boolean = false) {
        loggedIn = true
        sessionManager.saveRememberedSession(rememberFor20Hours)
        drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED, GravityCompat.START)
        refreshDrawerHeader()
    }

    /** خروج دستی همیشه نشست ۲۰ ساعته را هم پاک می‌کند. */
    fun logout() {
        sessionManager.clearRememberedSession()
        loggedIn = false
        drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED, GravityCompat.START)
        navigate(Route.AUTH)
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        when {
            drawerLayout.isDrawerOpen(GravityCompat.START) -> closeDrawer()
            detailBackRoute != null -> navigate(detailBackRoute!!)
            loggedIn && currentRoute != Route.HOME -> navigate(Route.HOME)
            else -> super.onBackPressed()
        }
    }
}
