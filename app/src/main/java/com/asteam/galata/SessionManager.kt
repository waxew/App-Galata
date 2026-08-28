package com.asteam.galata

import android.content.Context

/**
 * مدیریت نشست ورود مالک گالاتا.
 *
 * اگر کاربر هنگام ورود گزینه «ورود من را به خاطر بسپار» را فعال کند،
 * زمان پایان نشست به‌صورت محلی ذخیره می‌شود. این نشست حداکثر ۲۰ ساعت معتبر است.
 * بعد از پایان این بازه، برنامه دوباره صفحه ورود و رمز عبور را نمایش می‌دهد.
 *
 * نکته امنیتی: رمز عبور در SharedPreferences ذخیره نمی‌شود؛ فقط زمان اعتبار نشست نگه‌داری می‌شود.
 */
class SessionManager(context: Context) {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    /** بررسی می‌کند نشست «به خاطر سپرده‌شده» هنوز معتبر است یا نه. */
    fun isRememberedSessionValid(now: Long = System.currentTimeMillis()): Boolean {
        val expiry = prefs.getLong(KEY_EXPIRY, 0L)
        return expiry > now
    }

    /** بعد از ورود موفق، در صورت انتخاب کاربر، نشست را برای ۲۰ ساعت معتبر می‌کند. */
    fun saveRememberedSession(remember: Boolean, now: Long = System.currentTimeMillis()) {
        if (!remember) {
            clearRememberedSession()
            return
        }
        prefs.edit().putLong(KEY_EXPIRY, now + REMEMBER_DURATION_MS).apply()
    }

    /** نشست به خاطر سپرده‌شده را پاک می‌کند؛ برای خروج دستی یا منقضی‌شدن نشست. */
    fun clearRememberedSession() {
        prefs.edit().remove(KEY_EXPIRY).apply()
    }

    companion object {
        private const val PREFS_NAME = "galata_session"
        private const val KEY_EXPIRY = "remembered_login_expiry"
        private const val REMEMBER_DURATION_MS = 20L * 60L * 60L * 1000L
    }
}
