package com.asteam.galata.ui

import android.text.InputType
import android.view.Gravity
import android.view.View
import android.widget.CheckBox
import android.widget.LinearLayout
import android.widget.TextView
import com.asteam.galata.MainActivity

/**
 * صفحه ورود و ثبت‌نام مالک کافه.
 * کاربر می‌تواند انتخاب کند ورود موفق او حداکثر ۲۰ ساعت روی همین دستگاه معتبر بماند.
 */
class AuthScreen : GalataScreen {
    override fun build(host: MainActivity): View {
        val root = LinearLayout(host).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            layoutDirection = View.LAYOUT_DIRECTION_RTL
            setBackgroundColor(CafeTheme.cream)
            setPadding(CafeTheme.dp(host, 20), CafeTheme.dp(host, 32), CafeTheme.dp(host, 20), CafeTheme.dp(host, 24))
        }

        root.addView(GalataLogoView(host), LinearLayout.LayoutParams(CafeTheme.dp(host, 210), CafeTheme.dp(host, 210)))
        root.addView(TextView(host).apply {
            text = if (host.db.hasOwner()) "ورود به گالاتا" else "راه‌اندازی اولیه گالاتا"
            textSize = 21f
            gravity = Gravity.CENTER
            setTextColor(CafeTheme.espresso)
            setPadding(0, 8, 0, 14)
        })

        val user = ScreenUi.input(host, "نام کاربری")
        val pass = ScreenUi.input(host, "رمز عبور").apply {
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
        }
        val remember = CheckBox(host).apply {
            text = "ورود من را ۲۰ ساعت به خاطر بسپار"
            textSize = 14f
            setTextColor(CafeTheme.ink)
            gravity = Gravity.END
            layoutDirection = View.LAYOUT_DIRECTION_RTL
            setPadding(CafeTheme.dp(host, 10), CafeTheme.dp(host, 6), CafeTheme.dp(host, 10), CafeTheme.dp(host, 6))
        }

        root.addView(user)
        root.addView(pass)
        root.addView(remember, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT))

        if (!host.db.hasOwner()) {
            val name = ScreenUi.input(host, "نام صاحب کافه")
            root.addView(name)
            root.addView(ScreenUi.primaryButton(host, "ساخت حساب و ورود") {
                if (user.text.isBlank() || pass.text.isBlank()) {
                    ScreenUi.toast(host, "نام کاربری و رمز عبور الزامی است")
                } else if (host.db.registerOwner(user.text.toString(), pass.text.toString(), name.text.toString())) {
                    host.markLoggedIn(remember.isChecked)
                    host.navigate(MainActivity.Route.HOME)
                }
            })
        } else {
            root.addView(ScreenUi.primaryButton(host, "ورود") {
                if (host.db.login(user.text.toString(), pass.text.toString())) {
                    host.markLoggedIn(remember.isChecked)
                    host.navigate(MainActivity.Route.HOME)
                } else {
                    ScreenUi.toast(host, "نام کاربری یا رمز عبور نادرست است")
                }
            })
        }

        root.addView(TextView(host).apply {
            text = "اگر گزینه بالا فعال باشد، پس از بستن و باز کردن برنامه تا ۲۰ ساعت دوباره رمز خواسته نمی‌شود."
            textSize = 11.5f
            gravity = Gravity.CENTER
            setTextColor(CafeTheme.mocha)
            setPadding(0, 16, 0, 0)
        })
        return root
    }
}
