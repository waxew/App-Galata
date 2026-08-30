package com.asteam.galata.ui

import android.app.AlertDialog
import android.content.Intent
import android.net.Uri
import android.view.View
import android.widget.CheckBox
import android.widget.LinearLayout
import com.asteam.galata.BuildConfig
import com.asteam.galata.MainActivity
import com.asteam.galata.ReminderScheduler
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/** تنظیمات گالاتا: پروفایل، امنیت، اعلان، Backup/Restore و بررسی نسخه. */
class SettingsScreen : GalataScreen {
    override fun build(host: MainActivity): View {
        val page = ScreenUi.page(host, "تنظیمات", "حساب، امنیت و پشتیبان‌گیری", CafeIconView.Icon.SETTINGS)
        val content = ScreenUi.content(host)
        val profile = host.db.ownerProfile()
        content.addView(CafeTheme.card(host, "پروفایل مالک", "${host.db.ownerName()}\n${profile?.description.orEmpty().ifBlank { "برای ویرایش پروفایل لمس کنید" }}", CafeIconView.Icon.CUSTOMER).apply {
            setOnClickListener { editProfile(host) }
            setOnLongClickListener { host.openOwnerPhotoPicker(); true }
        })
        content.addView(ScreenUi.action(host, "عکس پروفایل", "برای انتخاب یا تغییر تصویر لمس کنید", CafeIconView.Icon.CUSTOMER) { host.openOwnerPhotoPicker() })
        content.addView(ScreenUi.action(host, "تغییر رمز عبور", "رمز محلی مالک برنامه", CafeIconView.Icon.SETTINGS) { changePassword(host) })

        val notifications = CheckBox(host).apply {
            text = "اعلان یادآورها فعال باشد"
            isChecked = ReminderScheduler.isEnabled(host)
            textSize = 15f
            setTextColor(CafeTheme.ink)
            setPadding(CafeTheme.dp(host, 18), CafeTheme.dp(host, 10), CafeTheme.dp(host, 18), CafeTheme.dp(host, 10))
            setOnCheckedChangeListener { _, enabled ->
                ReminderScheduler.setEnabled(host, enabled)
                if (enabled) {
                    host.requestNotificationPermission()
                    ReminderScheduler.rescheduleAll(host, host.db)
                }
            }
        }
        content.addView(notifications)

        content.addView(ScreenUi.action(host, "پشتیبان‌گیری", "ذخیره یک فایل کامل از دیتابیس روی حافظه دلخواه", CafeIconView.Icon.SHARE) { host.requestBackupExport() })
        content.addView(ScreenUi.action(host, "بازیابی اطلاعات", "فایل پشتیبان معتبر گالاتا را انتخاب کنید", CafeIconView.Icon.CALENDAR) {
            ScreenUi.confirm(host, "بازیابی اطلاعات", "اطلاعات فعلی با محتوای فایل پشتیبان جایگزین می‌شود. قبل از ادامه بهتر است یک Backup جدید بگیرید.", "انتخاب فایل") { host.requestBackupRestore() }
        })
        content.addView(ScreenUi.action(host, "بررسی نسخه جدید", "نسخه نصب‌شده: ${ScreenUi.fa(BuildConfig.VERSION_NAME)}", CafeIconView.Icon.ABOUT) { checkUpdate(host) })
        content.addView(ScreenUi.action(host, "اشتراک‌گذاری برنامه", "ارسال لینک گالاتا برای دیگران", CafeIconView.Icon.SHARE) { host.shareApp() })

        page.addView(ScreenUi.scroll(host, content), LinearLayout.LayoutParams(-1, 0, 1f))
        return page
    }

    private fun editProfile(host: MainActivity) {
        val profile = host.db.ownerProfile() ?: return
        val name = ScreenUi.input(host, "نام صاحب برنامه").apply { setText(profile.name) }
        val description = ScreenUi.input(host, "توضیح کوتاه").apply { setText(profile.description) }
        AlertDialog.Builder(host).setTitle("ویرایش پروفایل").setView(ScreenUi.form(host, name, description))
            .setNegativeButton("انصراف", null)
            .setPositiveButton("ذخیره") { _, _ ->
                host.db.updateOwnerProfile(name.text.toString(), description.text.toString())
                host.refreshDrawerHeader(); host.navigate(MainActivity.Route.SETTINGS, addToHistory = false)
            }.show()
    }

    private fun changePassword(host: MainActivity) {
        val current = ScreenUi.input(host, "رمز فعلی")
        val replacement = ScreenUi.input(host, "رمز جدید - حداقل ۴ کاراکتر")
        AlertDialog.Builder(host).setTitle("تغییر رمز عبور").setView(ScreenUi.form(host, current, replacement))
            .setNegativeButton("انصراف", null)
            .setPositiveButton("تغییر رمز") { _, _ ->
                if (host.db.changePassword(current.text.toString(), replacement.text.toString())) ScreenUi.toast(host, "رمز عبور تغییر کرد")
                else ScreenUi.toast(host, "رمز فعلی نادرست است یا رمز جدید کوتاه است")
            }.show()
    }

    /** بررسی نسخه فقط در این عملیات اینترنت می‌خواهد؛ شکست شبکه هیچ اثری روی داده‌های آفلاین ندارد. */
    private fun checkUpdate(host: MainActivity) {
        ScreenUi.toast(host, "در حال بررسی نسخه...")
        Thread {
            try {
                val connection = (URL("https://raw.githubusercontent.com/waxew/App-Galata/main/version.json").openConnection() as HttpURLConnection).apply {
                    connectTimeout = 6000; readTimeout = 6000; requestMethod = "GET"
                }
                val text = connection.inputStream.bufferedReader().use { it.readText() }
                val json = JSONObject(text)
                val latestCode = json.optInt("versionCode", BuildConfig.VERSION_CODE)
                val latestName = json.optString("versionName", BuildConfig.VERSION_NAME)
                val download = json.optString("downloadUrl", "https://github.com/waxew/App-Galata")
                host.runOnUiThread {
                    if (latestCode > BuildConfig.VERSION_CODE) {
                        AlertDialog.Builder(host).setTitle("نسخه جدید $latestName").setMessage("نسخه جدید گالاتا برای دریافت آماده است.")
                            .setNegativeButton("بعداً", null).setPositiveButton("مشاهده") { _, _ -> host.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(download))) }.show()
                    } else ScreenUi.infoDialog(host, "بررسی نسخه", "آخرین نسخه گالاتا روی دستگاه نصب است: ${BuildConfig.VERSION_NAME}")
                }
            } catch (e: Exception) {
                host.runOnUiThread { ScreenUi.toast(host, "بررسی نسخه انجام نشد؛ اتصال اینترنت را بررسی کنید") }
            }
        }.start()
    }
}
