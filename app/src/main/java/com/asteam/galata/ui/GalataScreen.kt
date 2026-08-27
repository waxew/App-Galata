package com.asteam.galata.ui

import android.view.View
import com.asteam.galata.MainActivity

/**
 * قرارداد مشترک تمام صفحه‌های گالاتا.
 * هر صفحه در فایل جداگانه خودش ساخته می‌شود تا نگهداری، توسعه و کامنت‌گذاری ساده باشد.
 */
interface GalataScreen {
    /** ساخت View کامل صفحه و تحویل آن به MainActivity. */
    fun build(host: MainActivity): View
}
