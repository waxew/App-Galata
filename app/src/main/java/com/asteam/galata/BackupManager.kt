package com.asteam.galata

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.net.Uri
import java.io.File

/**
 * Backup و Restore آفلاین دیتابیس گالاتا.
 * فایل قبل از جایگزینی با PRAGMA quick_check اعتبارسنجی می‌شود تا دیتابیس خراب وارد برنامه نشود.
 */
object BackupManager {
    /** خروجی‌گرفتن از فایل دیتابیس در Uri انتخاب‌شده توسط Storage Access Framework. */
    fun export(context: Context, db: GalataDb, target: Uri): Result<Unit> = runCatching {
        db.checkpoint()
        val source = db.databaseFile()
        require(source.exists()) { "فایل دیتابیس پیدا نشد" }
        context.contentResolver.openOutputStream(target, "w")!!.use { output ->
            source.inputStream().use { input -> input.copyTo(output) }
        }
    }

    /** بازیابی امن: ابتدا فایل در Temp کپی و بررسی می‌شود و سپس جای دیتابیس فعلی قرار می‌گیرد. */
    fun restore(context: Context, db: GalataDb, sourceUri: Uri): Result<Unit> = runCatching {
        val target = db.databaseFile()
        val temp = File(context.cacheDir, "galata-restore-${System.currentTimeMillis()}.db")
        context.contentResolver.openInputStream(sourceUri)!!.use { input ->
            temp.outputStream().use { output -> input.copyTo(output) }
        }
        require(temp.length() > 0L) { "فایل پشتیبان خالی است" }
        val testDb = SQLiteDatabase.openDatabase(temp.absolutePath, null, SQLiteDatabase.OPEN_READONLY)
        val ok = testDb.rawQuery("PRAGMA quick_check", null).use { c -> c.moveToFirst() && c.getString(0).equals("ok", true) }
        testDb.close()
        require(ok) { "فایل پشتیبان معتبر نیست" }
        db.close()
        val old = File(target.parentFile, "${target.name}.before-restore")
        if (target.exists()) target.copyTo(old, overwrite = true)
        temp.copyTo(target, overwrite = true)
        temp.delete()
        // بازشدن مجدد توسط SQLiteOpenHelper، Migrationهای لازم را به صورت خودکار اعمال می‌کند.
        db.writableDatabase
    }
}
