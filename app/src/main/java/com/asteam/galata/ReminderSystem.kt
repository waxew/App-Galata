package com.asteam.galata

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build

/**
 * زمان‌بندی محلی یادآورها؛ هیچ سرور یا اینترنتی برای اعلان چک، قسط و قرار لازم نیست.
 */
object ReminderScheduler {
    private const val CHANNEL_ID = "galata_reminders"

    /** ساخت کانال اعلان از Android 8 به بعد. */
    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(
                NotificationChannel(CHANNEL_ID, "یادآورهای گالاتا", NotificationManager.IMPORTANCE_HIGH).apply {
                    description = "چک، قسط، قرار و یادآورهای ثبت‌شده در گالاتا"
                }
            )
        }
    }

    /** آیا اعلان‌ها در تنظیمات خود گالاتا فعال‌اند؟ */
    fun isEnabled(context: Context): Boolean = context.getSharedPreferences("galata_settings", Context.MODE_PRIVATE)
        .getBoolean("notifications", true)

    /** فعال یا غیرفعال کردن اعلان‌های محلی. */
    fun setEnabled(context: Context, enabled: Boolean) {
        context.getSharedPreferences("galata_settings", Context.MODE_PRIVATE).edit().putBoolean("notifications", enabled).apply()
    }

    /** زمان‌بندی یک یادآور باز؛ برای زمان گذشته Alarm ساخته نمی‌شود. */
    fun schedule(context: Context, reminder: Reminder) {
        if (!isEnabled(context) || reminder.done || reminder.dueAt <= System.currentTimeMillis()) return
        ensureChannel(context)
        val alarm = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, ReminderReceiver::class.java).apply {
            putExtra("id", reminder.id)
            putExtra("title", reminder.title)
            putExtra("description", reminder.description)
            putExtra("amount", reminder.amount)
            putExtra("kind", reminder.kind)
        }
        val pending = PendingIntent.getBroadcast(
            context,
            reminder.id.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        // setAndAllowWhileIdle مجوز Exact Alarm نمی‌خواهد و برای یادآورهای عمومی مناسب است.
        alarm.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, reminder.dueAt, pending)
    }

    /** لغو Alarm یک یادآور حذف/انجام‌شده. */
    fun cancel(context: Context, reminderId: Long) {
        val alarm = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val pending = PendingIntent.getBroadcast(
            context,
            reminderId.toInt(),
            Intent(context, ReminderReceiver::class.java),
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (pending != null) alarm.cancel(pending)
    }

    /** بازسازی همه Alarmها بعد از Boot یا Restore دیتابیس. */
    fun rescheduleAll(context: Context, db: GalataDb) {
        if (!isEnabled(context)) return
        db.reminders(includeDone = false).forEach { schedule(context, it) }
    }

    /** شناسه کانال برای Receiver. */
    internal fun channelId(): String = CHANNEL_ID
}

/** Receiver اعلان سررسید. */
class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (!ReminderScheduler.isEnabled(context)) return
        if (Build.VERSION.SDK_INT >= 33 && context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) return
        ReminderScheduler.ensureChannel(context)
        val id = intent.getLongExtra("id", System.currentTimeMillis())
        val title = intent.getStringExtra("title") ?: "یادآور گالاتا"
        val description = intent.getStringExtra("description").orEmpty()
        val amount = intent.getLongExtra("amount", 0L)
        val body = buildString {
            if (description.isNotBlank()) append(description)
            if (amount > 0L) {
                if (isNotEmpty()) append(" — ")
                append(com.asteam.galata.ui.ScreenUi.money(amount))
            }
        }.ifBlank { "زمان یادآور فرا رسیده است" }
        val openIntent = PendingIntent.getActivity(
            context,
            id.toInt(),
            Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            android.app.Notification.Builder(context, ReminderScheduler.channelId())
        } else {
            @Suppress("DEPRECATION") android.app.Notification.Builder(context)
        }.setSmallIcon(com.asteam.galata.R.drawable.ic_galata)
            .setContentTitle(title)
            .setContentText(body)
            .setAutoCancel(true)
            .setContentIntent(openIntent)
            .build()
        (context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).notify(id.toInt(), notification)
    }
}

/** Receiver روشن‌شدن گوشی؛ Alarmهای سیستم بعد از Reboot پاک می‌شوند و باید بازسازی شوند. */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            val db = GalataDb(context)
            ReminderScheduler.rescheduleAll(context, db)
            db.close()
        }
    }
}
