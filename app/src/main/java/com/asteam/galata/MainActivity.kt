package com.asteam.galata

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import com.asteam.galata.ui.*
import java.util.ArrayDeque

/**
 * اکتیویتی اصلی گالاتا.
 * پوسته، Drawer، Back Stack، نشست ورود و Activity Resultهای فایل/عکس فقط در این فایل مدیریت می‌شوند؛
 * منطق مالی و داده داخل GalataDb و منطق هر صفحه داخل Screen مستقل خودش قرار دارد.
 */
class MainActivity : Activity() {
    lateinit var db: GalataDb
        private set

    private lateinit var sessionManager: SessionManager
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var contentFrame: FrameLayout
    private lateinit var drawerPanel: LinearLayout
    private lateinit var drawerOwnerName: TextView
    private lateinit var drawerProfileImage: ImageView

    private var loggedIn = false
    private var currentRoute = Route.AUTH
    private var detailBackRoute: Route? = null
    private val backStack = ArrayDeque<Route>()

    enum class Route {
        AUTH, HOME, SETTINGS, SALE, PAYMENTS, CUSTOMERS, PRODUCTS, INVOICES,
        CALENDAR, REMINDERS, EXPENSES, REPORTS, ABOUT, CONTACT
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        db = GalataDb(this)
        sessionManager = SessionManager(this)
        window.decorView.layoutDirection = View.LAYOUT_DIRECTION_RTL
        ReminderScheduler.ensureChannel(this)
        buildShell()

        if (db.hasOwner() && sessionManager.isRememberedSessionValid()) {
            loggedIn = true
            drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED, GravityCompat.START)
            refreshDrawerHeader()
            navigate(Route.HOME, addToHistory = false)
        } else {
            sessionManager.clearRememberedSession()
            navigate(Route.AUTH, addToHistory = false)
        }
    }

    /** پوسته ثابت شامل محتوای صفحه و Drawer سمت راست در RTL. */
    private fun buildShell() {
        drawerLayout = DrawerLayout(this).apply {
            layoutDirection = View.LAYOUT_DIRECTION_RTL
            setBackgroundColor(CafeTheme.cream)
            setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED, GravityCompat.START)
        }
        contentFrame = FrameLayout(this).apply {
            layoutParams = DrawerLayout.LayoutParams(DrawerLayout.LayoutParams.MATCH_PARENT, DrawerLayout.LayoutParams.MATCH_PARENT)
        }
        drawerPanel = buildDrawer()
        drawerLayout.addView(contentFrame)
        drawerLayout.addView(drawerPanel)
        setContentView(drawerLayout)
    }

    /** Drawer مشترک گالاتا؛ تنظیمات و اشتراک‌گذاری دو گزینه ثابت بالایی هستند. */
    private fun buildDrawer(): LinearLayout = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        gravity = Gravity.TOP
        layoutDirection = View.LAYOUT_DIRECTION_RTL
        setPadding(CafeTheme.dp(this@MainActivity, 16), CafeTheme.dp(this@MainActivity, 24), CafeTheme.dp(this@MainActivity, 16), CafeTheme.dp(this@MainActivity, 16))
        background = CafeTheme.rounded(CafeTheme.espresso, 0f)
        layoutParams = DrawerLayout.LayoutParams(CafeTheme.dp(this@MainActivity, 310), DrawerLayout.LayoutParams.MATCH_PARENT).apply {
            gravity = GravityCompat.START
        }

        drawerProfileImage = ImageView(this@MainActivity).apply {
            scaleType = ImageView.ScaleType.CENTER_CROP
            background = GradientDrawable().apply { shape = GradientDrawable.OVAL; setColor(CafeTheme.cream) }
            clipToOutline = true
            setPadding(5, 5, 5, 5)
            setOnClickListener { openOwnerPhotoPicker() }
        }
        addView(drawerProfileImage, LinearLayout.LayoutParams(CafeTheme.dp(this@MainActivity, 112), CafeTheme.dp(this@MainActivity, 112)).apply {
            gravity = Gravity.CENTER_HORIZONTAL
            bottomMargin = CafeTheme.dp(this@MainActivity, 8)
        })

        drawerOwnerName = TextView(this@MainActivity).apply {
            textSize = 17f
            gravity = Gravity.CENTER
            setTextColor(CafeTheme.foam)
            setPadding(8, 4, 8, 10)
        }
        addView(drawerOwnerName)
        addView(drawerDivider())

        menuItem("تنظیمات", CafeIconView.Icon.SETTINGS) { navigate(Route.SETTINGS) }
        menuItem("اشتراک‌گذاری با دوستان", CafeIconView.Icon.SHARE) { shareApp() }
        addView(drawerDivider())

        menuItem("خانه", CafeIconView.Icon.HOME) { navigate(Route.HOME) }
        menuItem("فروش", CafeIconView.Icon.COFFEE) { navigate(Route.SALE) }
        menuItem("تسویه مشتری", CafeIconView.Icon.WALLET) { navigate(Route.PAYMENTS) }
        menuItem("فاکتورها", CafeIconView.Icon.INVOICE) { navigate(Route.INVOICES) }
        menuItem("لیست مشتری‌ها", CafeIconView.Icon.CUSTOMER) { navigate(Route.CUSTOMERS) }
        menuItem("لیست کالا / خدمات", CafeIconView.Icon.PRODUCT) { navigate(Route.PRODUCTS) }
        menuItem("تقویم شمسی", CafeIconView.Icon.CALENDAR) { navigate(Route.CALENDAR) }
        menuItem("یادآورها", CafeIconView.Icon.BELL) { navigate(Route.REMINDERS) }
        menuItem("هزینه‌ها", CafeIconView.Icon.EXPENSE) { navigate(Route.EXPENSES) }
        menuItem("گزارش مالی", CafeIconView.Icon.REPORT) { navigate(Route.REPORTS) }

        addView(drawerDivider())
        menuItem("درباره نرم‌افزار", CafeIconView.Icon.ABOUT) { navigate(Route.ABOUT) }
        menuItem("تماس با ما", CafeIconView.Icon.CONTACT) { navigate(Route.CONTACT) }
        menuItem("خروج", CafeIconView.Icon.EXIT) { logout() }
    }

    private fun drawerDivider(): View = View(this).apply {
        setBackgroundColor(CafeTheme.caramel)
        layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, CafeTheme.dp(this@MainActivity, 1)).apply {
            setMargins(0, CafeTheme.dp(this@MainActivity, 7), 0, CafeTheme.dp(this@MainActivity, 7))
        }
    }

    private fun LinearLayout.menuItem(title: String, iconType: CafeIconView.Icon, action: () -> Unit) {
        addView(LinearLayout(this@MainActivity).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutDirection = View.LAYOUT_DIRECTION_RTL
            setPadding(CafeTheme.dp(context, 8), CafeTheme.dp(context, 5), CafeTheme.dp(context, 8), CafeTheme.dp(context, 5))
            addView(CafeIconView(context).apply { icon = iconType }, LinearLayout.LayoutParams(CafeTheme.dp(context, 34), CafeTheme.dp(context, 34)).apply { marginStart = CafeTheme.dp(context, 10) })
            addView(TextView(context).apply {
                text = title; textSize = 14.5f; setTextColor(CafeTheme.foam); gravity = Gravity.END
            }, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
            setOnClickListener { action(); closeDrawer() }
        })
    }

    /** ناوبری مرکزی با Back Stack؛ refresh همان Route، تاریخچه تکراری ایجاد نمی‌کند. */
    fun navigate(route: Route, addToHistory: Boolean = true) {
        if (route == Route.AUTH) backStack.clear()
        if (addToHistory && loggedIn && currentRoute != Route.AUTH && currentRoute != route) backStack.addLast(currentRoute)
        currentRoute = route
        detailBackRoute = null
        closeDrawer()

        val screen: GalataScreen = when (route) {
            Route.AUTH -> AuthScreen()
            Route.HOME -> HomeScreen()
            Route.SETTINGS -> SettingsScreen()
            Route.SALE -> SaleScreen()
            Route.PAYMENTS -> PaymentsScreen()
            Route.CUSTOMERS -> CustomersScreen()
            Route.PRODUCTS -> ProductsScreen()
            Route.INVOICES -> InvoicesScreen()
            Route.CALENDAR -> CalendarScreen()
            Route.REMINDERS -> RemindersScreen()
            Route.EXPENSES -> ExpensesScreen()
            Route.REPORTS -> ReportsScreen()
            Route.ABOUT -> AboutScreen()
            Route.CONTACT -> ContactScreen()
        }
        contentFrame.removeAllViews()
        contentFrame.addView(screen.build(this))
    }

    /** نمایش صورت‌حساب مشتری به عنوان صفحه جزئیات با مقصد برگشت مشخص. */
    fun showLedger(customer: Customer) {
        detailBackRoute = currentRoute
        contentFrame.removeAllViews()
        contentFrame.addView(LedgerScreen(customer).build(this))
    }

    fun openDrawer() {
        if (!loggedIn) return
        refreshDrawerHeader()
        drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED, GravityCompat.START)
        drawerLayout.openDrawer(GravityCompat.START)
    }

    private fun closeDrawer() {
        if (::drawerLayout.isInitialized && drawerLayout.isDrawerOpen(GravityCompat.START)) drawerLayout.closeDrawer(GravityCompat.START)
    }

    fun refreshDrawerHeader() {
        if (!::drawerOwnerName.isInitialized) return
        val profile = db.ownerProfile()
        drawerOwnerName.text = "👤  ${db.ownerName()}"
        val uri = profile?.photoUri.orEmpty()
        if (uri.isNotBlank()) {
            try { drawerProfileImage.setImageURI(Uri.parse(uri)) } catch (_: Exception) { drawerProfileImage.setImageResource(R.drawable.ic_galata) }
        } else drawerProfileImage.setImageResource(R.drawable.ic_galata)
    }

    fun markLoggedIn(rememberFor20Hours: Boolean = false) {
        loggedIn = true
        sessionManager.saveRememberedSession(rememberFor20Hours)
        drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED, GravityCompat.START)
        refreshDrawerHeader()
    }

    fun logout() {
        sessionManager.clearRememberedSession()
        loggedIn = false
        backStack.clear()
        drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED, GravityCompat.START)
        navigate(Route.AUTH, addToHistory = false)
    }

    /** انتخاب عکس پروفایل از حافظه با دسترسی پایدار Uri. */
    fun openOwnerPhotoPicker() {
        startActivityForResult(Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "image/*"
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION)
        }, REQ_PROFILE_PHOTO)
    }

    /** ساخت فایل Backup توسط Storage Access Framework. */
    fun requestBackupExport() {
        val today = JalaliDate.today()
        startActivityForResult(Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "application/octet-stream"
            putExtra(Intent.EXTRA_TITLE, "Galata-${today.year}-${today.month}-${today.day}.backup.db")
        }, REQ_BACKUP_EXPORT)
    }

    /** انتخاب فایل Backup برای Restore. */
    fun requestBackupRestore() {
        startActivityForResult(Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "*/*"
        }, REQ_BACKUP_RESTORE)
    }

    /** درخواست مجوز اعلان فقط در Android 13+ و فقط هنگام فعال‌کردن قابلیت. */
    fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= 33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), REQ_NOTIFICATION)
        }
    }

    /** اشتراک لینک پروژه/دریافت نسخه برنامه. */
    fun shareApp() {
        startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, "گالاتا؛ مدیریت و حسابداری آفلاین کسب‌وکار\nhttps://github.com/waxew/App-Galata")
        }, "اشتراک‌گذاری گالاتا"))
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode != RESULT_OK) return
        val uri = data?.data ?: return
        when (requestCode) {
            REQ_PROFILE_PHOTO -> {
                try { contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION) } catch (_: Exception) { }
                db.setOwnerPhoto(uri.toString())
                refreshDrawerHeader()
                if (currentRoute == Route.SETTINGS) navigate(Route.SETTINGS, addToHistory = false)
            }
            REQ_BACKUP_EXPORT -> {
                BackupManager.export(this, db, uri)
                    .onSuccess { ScreenUi.toast(this, "نسخه پشتیبان با موفقیت ذخیره شد") }
                    .onFailure { ScreenUi.toast(this, "خطا در پشتیبان‌گیری: ${it.message}") }
            }
            REQ_BACKUP_RESTORE -> {
                BackupManager.restore(this, db, uri)
                    .onSuccess {
                        ReminderScheduler.rescheduleAll(this, db)
                        ScreenUi.toast(this, "اطلاعات با موفقیت بازیابی شد")
                        navigate(Route.HOME, addToHistory = false)
                    }
                    .onFailure { ScreenUi.toast(this, "فایل بازیابی نشد: ${it.message}") }
            }
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        when {
            drawerLayout.isDrawerOpen(GravityCompat.START) -> closeDrawer()
            detailBackRoute != null -> {
                val target = detailBackRoute!!
                detailBackRoute = null
                navigate(target, addToHistory = false)
            }
            loggedIn && backStack.isNotEmpty() -> navigate(backStack.removeLast(), addToHistory = false)
            loggedIn && currentRoute != Route.HOME -> navigate(Route.HOME, addToHistory = false)
            else -> super.onBackPressed()
        }
    }

    companion object {
        private const val REQ_PROFILE_PHOTO = 4101
        private const val REQ_BACKUP_EXPORT = 4102
        private const val REQ_BACKUP_RESTORE = 4103
        private const val REQ_NOTIFICATION = 4104
    }
}
