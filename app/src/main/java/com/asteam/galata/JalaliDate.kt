package com.asteam.galata

import java.util.Calendar

/**
 * موتور تاریخ جلالی مستقل گالاتا.
 * تاریخ داخل دیتابیس به صورت timestamp ذخیره می‌شود و فقط در رابط کاربری به شمسی تبدیل می‌شود؛
 * بنابراین مرتب‌سازی، AlarmManager و Migration به تقویم نمایشی وابسته نمی‌شوند.
 */
object JalaliDate {
    /** یک تاریخ شمسی ساده و immutable. */
    data class Date(val year: Int, val month: Int, val day: Int)

    /** تبدیل تاریخ میلادی به جلالی با الگوریتم عددروز. */
    fun fromGregorian(gy: Int, gm: Int, gd: Int): Date {
        val gdm = intArrayOf(0, 31, 59, 90, 120, 151, 181, 212, 243, 273, 304, 334)
        val gy2 = if (gm > 2) gy + 1 else gy
        var days = 355666 + 365 * gy + (gy2 + 3) / 4 - (gy2 + 99) / 100 + (gy2 + 399) / 400 + gd + gdm[gm - 1]
        var jy = -1595 + 33 * (days / 12053)
        days %= 12053
        jy += 4 * (days / 1461)
        days %= 1461
        if (days > 365) {
            jy += (days - 1) / 365
            days = (days - 1) % 365
        }
        val jm: Int
        val jd: Int
        if (days < 186) {
            jm = 1 + days / 31
            jd = 1 + days % 31
        } else {
            jm = 7 + (days - 186) / 30
            jd = 1 + (days - 186) % 30
        }
        return Date(jy, jm, jd)
    }

    /** تبدیل تاریخ جلالی به میلادی. خروجی به ترتیب سال، ماه و روز است. */
    fun toGregorian(jyInput: Int, jm: Int, jd: Int): IntArray {
        var jy = jyInput + 1595
        var days = -355668 + 365 * jy + (jy / 33) * 8 + ((jy % 33 + 3) / 4) + jd
        days += if (jm < 7) (jm - 1) * 31 else (jm - 7) * 30 + 186
        var gy = 400 * (days / 146097)
        days %= 146097
        if (days > 36524) {
            gy += 100 * (--days / 36524)
            days %= 36524
            if (days >= 365) days++
        }
        gy += 4 * (days / 1461)
        days %= 1461
        if (days > 365) {
            gy += (days - 1) / 365
            days = (days - 1) % 365
        }
        var gd = days + 1
        val leap = (gy % 4 == 0 && gy % 100 != 0) || gy % 400 == 0
        val monthDays = intArrayOf(31, if (leap) 29 else 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31)
        var gm = 1
        while (gm <= 12 && gd > monthDays[gm - 1]) {
            gd -= monthDays[gm - 1]
            gm++
        }
        return intArrayOf(gy, gm, gd)
    }

    /** تاریخ شمسی امروز. */
    fun today(now: Long = System.currentTimeMillis()): Date {
        val c = Calendar.getInstance().apply { timeInMillis = now }
        return fromGregorian(c.get(Calendar.YEAR), c.get(Calendar.MONTH) + 1, c.get(Calendar.DAY_OF_MONTH))
    }

    /** تبدیل تاریخ/ساعت شمسی انتخاب‌شده به timestamp محلی اندروید. */
    fun toMillis(year: Int, month: Int, day: Int, hour: Int = 9, minute: Int = 0): Long {
        val g = toGregorian(year, month, day)
        return Calendar.getInstance().apply {
            set(Calendar.YEAR, g[0])
            set(Calendar.MONTH, g[1] - 1)
            set(Calendar.DAY_OF_MONTH, g[2])
            set(Calendar.HOUR_OF_DAY, hour.coerceIn(0, 23))
            set(Calendar.MINUTE, minute.coerceIn(0, 59))
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }

    /** تعداد روزهای ماه شمسی؛ اسفند با اختلاف طول سال محاسبه می‌شود. */
    fun daysInMonth(year: Int, month: Int): Int = when {
        month in 1..6 -> 31
        month in 7..11 -> 30
        month == 12 -> if (isLeap(year)) 30 else 29
        else -> 0
    }

    /** سال کبیسه جلالی با اختلاف دو نوروز متوالی. */
    fun isLeap(year: Int): Boolean {
        val a = toMillis(year, 1, 1, 0, 0)
        val b = toMillis(year + 1, 1, 1, 0, 0)
        return (b - a) / 86_400_000L == 366L
    }

    /** اندیس روز هفته با قرارداد شنبه=۰ ... جمعه=۶. */
    fun weekDayIndex(year: Int, month: Int, day: Int): Int {
        val g = toGregorian(year, month, day)
        val javaDay = Calendar.getInstance().apply {
            set(g[0], g[1] - 1, g[2], 12, 0, 0)
        }.get(Calendar.DAY_OF_WEEK)
        // Calendar.SATURDAY=7 -> 0 و SUNDAY=1 -> 1؛ دقیقاً مطابق ترتیب شنبه تا جمعه.
        return javaDay % 7
    }

    /** کلید پایدار یک روز برای جدول یادداشت‌های تقویم. */
    fun dayKey(year: Int, month: Int, day: Int): String = "%04d-%02d-%02d".format(year, month, day)

    /** نام فارسی ماه برای هدر تقویم. */
    fun monthName(month: Int): String = arrayOf(
        "فروردین", "اردیبهشت", "خرداد", "تیر", "مرداد", "شهریور",
        "مهر", "آبان", "آذر", "دی", "بهمن", "اسفند"
    ).getOrElse(month - 1) { "" }
}
