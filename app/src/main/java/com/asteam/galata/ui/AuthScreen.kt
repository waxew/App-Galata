package com.asteam.galata.ui

import android.app.AlertDialog
import android.text.InputType
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import com.asteam.galata.MainActivity

/** صفحه ورود/ثبت‌نام مالک کافه؛ کاملاً مستقل از MainActivity. */
class AuthScreen : GalataScreen {
    override fun build(host: MainActivity): View {
        val root = LinearLayout(host).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            layoutDirection = View.LAYOUT_DIRECTION_RTL
            setBackgroundColor(CafeTheme.cream)
            setPadding(CafeTheme.dp(host, 20), CafeTheme.dp(host, 32), CafeTheme.dp(host, 20), CafeTheme.dp(host, 24))
        }
        root.addView(GalataLogoView(host), LinearLayout.LayoutParams(CafeTheme.dp(host, 220), CafeTheme.dp(host, 220)))
        root.addView(TextView(host).apply {
            text = if (host.db.hasOwner()) "ورود به مدیریت کافه" else "راه‌اندازی اولیه گالاتا"
            textSize = 21f
            gravity = Gravity.CENTER
            setTextColor(CafeTheme.espresso)
            setPadding(0, 8, 0, 14)
        })

        val user = ScreenUi.input(host, "نام کاربری")
        val pass = ScreenUi.input(host, "رمز عبور").apply { inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD }
        root.addView(user); root.addView(pass)

        if (!host.db.hasOwner()) {
            val name = ScreenUi.input(host, "نام صاحب کافه")
            root.addView(name)
            root.addView(ScreenUi.primaryButton(host, "ساخت حساب و ورود") {
                if (user.text.isBlank() || pass.text.isBlank()) ScreenUi.toast(host, "نام کاربری و رمز عبور الزامی است")
                else if (host.db.registerOwner(user.text.toString(), pass.text.toString(), name.text.toString())) {
                    host.markLoggedIn(); host.navigate(MainActivity.Route.HOME)
                }
            })
        } else {
            root.addView(ScreenUi.primaryButton(host, "ورود") {
                if (host.db.login(user.text.toString(), pass.text.toString())) {
                    host.markLoggedIn(); host.navigate(MainActivity.Route.HOME)
                } else ScreenUi.toast(host, "نام کاربری یا رمز عبور نادرست است")
            })
        }
        root.addView(TextView(host).apply {
            text = "مدیریت فروش، مشتری، فاکتور و حساب کافه — کاملاً آفلاین"
            textSize = 12f; gravity = Gravity.CENTER; setTextColor(CafeTheme.mocha); setPadding(0, 18, 0, 0)
        })
        return root
    }
}
