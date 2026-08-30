package com.asteam.galata.ui

import android.text.InputType
import android.view.Gravity
import android.view.View
import android.widget.CheckBox
import android.widget.LinearLayout
import android.widget.TextView
import com.asteam.galata.MainActivity

/** ثبت‌نام یک‌باره مالک و ورود محلی؛ رمز خام هیچ‌گاه ذخیره نمی‌شود. */
class AuthScreen : GalataScreen {
    override fun build(host: MainActivity): View {
        val root = LinearLayout(host).apply {
            orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER_HORIZONTAL; layoutDirection = View.LAYOUT_DIRECTION_RTL
            setBackgroundColor(CafeTheme.cream); setPadding(CafeTheme.dp(host,20),CafeTheme.dp(host,32),CafeTheme.dp(host,20),CafeTheme.dp(host,24))
        }
        root.addView(GalataLogoView(host), LinearLayout.LayoutParams(CafeTheme.dp(host,210),CafeTheme.dp(host,210)))
        root.addView(TextView(host).apply {
            text = if (host.db.hasOwner()) "ورود به گالاتا" else "راه‌اندازی اولیه گالاتا"
            textSize = 21f; gravity = Gravity.CENTER; setTextColor(CafeTheme.espresso); setPadding(0,8,0,14)
        })
        val user = ScreenUi.input(host,"نام کاربری")
        val pass = ScreenUi.input(host,"رمز عبور").apply { inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD }
        val remember = CheckBox(host).apply {
            text = "ورود من را ۲۰ ساعت به خاطر بسپار"; textSize = 14f; setTextColor(CafeTheme.ink); gravity = Gravity.END; layoutDirection = View.LAYOUT_DIRECTION_RTL
            setPadding(CafeTheme.dp(host,10),CafeTheme.dp(host,6),CafeTheme.dp(host,10),CafeTheme.dp(host,6))
        }
        root.addView(user); root.addView(pass); root.addView(remember, LinearLayout.LayoutParams(-1,-2))
        if (!host.db.hasOwner()) {
            val name = ScreenUi.input(host,"نام صاحب برنامه")
            root.addView(name)
            root.addView(ScreenUi.primaryButton(host,"ساخت حساب و ورود") {
                when {
                    user.text.isBlank() -> ScreenUi.toast(host,"نام کاربری الزامی است")
                    pass.text.length < 4 -> ScreenUi.toast(host,"رمز عبور باید حداقل ۴ کاراکتر باشد")
                    host.db.registerOwner(user.text.toString(),pass.text.toString(),name.text.toString()) -> {
                        host.markLoggedIn(remember.isChecked); host.navigate(MainActivity.Route.HOME, addToHistory = false)
                    }
                    else -> ScreenUi.toast(host,"ساخت حساب انجام نشد")
                }
            })
        } else {
            root.addView(ScreenUi.primaryButton(host,"ورود") {
                if (host.db.login(user.text.toString(),pass.text.toString())) {
                    host.markLoggedIn(remember.isChecked); host.navigate(MainActivity.Route.HOME, addToHistory = false)
                } else ScreenUi.toast(host,"نام کاربری یا رمز عبور نادرست است")
            })
        }
        root.addView(TextView(host).apply {
            text = "تمام اطلاعات کسب‌وکار روی همین دستگاه ذخیره می‌شود. از بخش تنظیمات به‌صورت دوره‌ای فایل پشتیبان بگیرید."
            textSize = 11.5f; gravity = Gravity.CENTER; setTextColor(CafeTheme.mocha); setPadding(0,16,0,0)
        })
        return root
    }
}
