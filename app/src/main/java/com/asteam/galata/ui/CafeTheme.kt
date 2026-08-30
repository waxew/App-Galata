package com.asteam.galata.ui

import android.content.Context
import android.graphics.*
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import kotlin.math.min

/** تم گرافیکی گرم گالاتا؛ تمام رنگ‌ها و آیکون‌های برداری از این فایل کنترل می‌شوند. */
object CafeTheme {
    val espresso = Color.rgb(67, 40, 24)
    val mocha = Color.rgb(98, 60, 37)
    val caramel = Color.rgb(196, 133, 76)
    val cream = Color.rgb(250, 242, 230)
    val foam = Color.rgb(255, 250, 243)
    val leaf = Color.rgb(79, 111, 67)
    val danger = Color.rgb(177, 75, 58)
    val ink = Color.rgb(49, 37, 29)
    val blue = Color.rgb(77, 115, 166)

    fun dp(context: Context, value: Int): Int = (value * context.resources.displayMetrics.density).toInt()
    fun rounded(color: Int, radius: Float, stroke: Int? = null, strokeWidth: Int = 1): GradientDrawable = GradientDrawable().apply {
        shape = GradientDrawable.RECTANGLE; setColor(color); cornerRadius = radius; if (stroke != null) setStroke(strokeWidth, stroke)
    }
    fun sectionTitle(context: Context, text: String): TextView = TextView(context).apply {
        this.text = text; textSize = 18f; setTextColor(ink); gravity = Gravity.END
        setPadding(dp(context, 18), dp(context, 16), dp(context, 18), dp(context, 8)); typeface = Typeface.DEFAULT_BOLD
    }
    fun card(context: Context, title: String, subtitle: String, iconType: CafeIconView.Icon): LinearLayout = LinearLayout(context).apply {
        orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL; layoutDirection = View.LAYOUT_DIRECTION_RTL
        setPadding(dp(context, 14), dp(context, 12), dp(context, 14), dp(context, 12))
        background = rounded(foam, dp(context, 18).toFloat(), Color.rgb(232, 216, 198), dp(context, 1)); elevation = dp(context, 3).toFloat()
        layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
            setMargins(dp(context, 14), dp(context, 6), dp(context, 14), dp(context, 6))
        }
        addView(CafeIconView(context).apply { icon = iconType }, LinearLayout.LayoutParams(dp(context, 46), dp(context, 46)).apply { marginStart = dp(context, 12) })
        addView(LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL; gravity = Gravity.END
            addView(TextView(context).apply { text = title; textSize = 16f; setTextColor(ink); gravity = Gravity.END; typeface = Typeface.DEFAULT_BOLD })
            addView(TextView(context).apply { text = subtitle; textSize = 13f; setTextColor(Color.rgb(111, 91, 76)); gravity = Gravity.END })
        }, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
    }
}

/** لوگوی Galata به صورت Canvas و بدون فایل تصویری خارجی. */
class GalataLogoView(context: Context) : View(context) {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val w = width.toFloat(); val h = height.toFloat(); val s = min(w, h)
        paint.color = CafeTheme.espresso; canvas.drawCircle(w / 2f, h / 2f, s * .46f, paint)
        paint.style = Paint.Style.STROKE; paint.strokeWidth = s * .035f; paint.color = CafeTheme.caramel; canvas.drawCircle(w / 2f, h / 2f, s * .43f, paint); paint.style = Paint.Style.FILL
        paint.color = CafeTheme.cream; val cup = RectF(w * .25f, h * .30f, w * .72f, h * .59f); canvas.drawRoundRect(cup, s * .08f, s * .08f, paint)
        paint.style = Paint.Style.STROKE; paint.strokeWidth = s * .045f; canvas.drawArc(RectF(w * .62f, h * .34f, w * .83f, h * .55f), -80f, 180f, false, paint); paint.style = Paint.Style.FILL
        paint.color = CafeTheme.caramel; paint.strokeWidth = s * .025f; paint.style = Paint.Style.STROKE; paint.strokeCap = Paint.Cap.ROUND
        canvas.drawPath(Path().apply { moveTo(w*.42f,h*.28f); cubicTo(w*.35f,h*.20f,w*.49f,h*.17f,w*.43f,h*.10f) }, paint)
        canvas.drawPath(Path().apply { moveTo(w*.55f,h*.28f); cubicTo(w*.48f,h*.21f,w*.62f,h*.17f,w*.56f,h*.09f) }, paint)
        paint.style = Paint.Style.FILL; paint.color = CafeTheme.foam; paint.textAlign = Paint.Align.CENTER; paint.typeface = Typeface.create(Typeface.SERIF, Typeface.BOLD_ITALIC); paint.textSize = s * .16f
        canvas.drawText("Galata", w/2f, h*.80f, paint)
    }
}

/** مجموعه آیکون‌های برداری سبک برای Drawer و کارت‌ها. */
class CafeIconView(context: Context) : View(context) {
    enum class Icon { COFFEE, TEA, CUSTOMER, PRODUCT, INVOICE, WALLET, BELL, HOME, EXPENSE, SETTINGS, SHARE, CALENDAR, REPORT, ABOUT, CONTACT, EXIT }
    var icon: Icon = Icon.COFFEE
    private val p = Paint(Paint.ANTI_ALIAS_FLAG)

    override fun onDraw(c: Canvas) {
        super.onDraw(c)
        val w = width.toFloat(); val h = height.toFloat(); val s = min(w,h)
        p.color = CafeTheme.cream; c.drawCircle(w/2f,h/2f,s*.48f,p)
        p.color = CafeTheme.espresso; p.style = Paint.Style.STROKE; p.strokeWidth = s*.075f; p.strokeCap = Paint.Cap.ROUND
        when(icon) {
            Icon.COFFEE, Icon.TEA -> { c.drawRoundRect(RectF(w*.22f,h*.36f,w*.65f,h*.68f),s*.08f,s*.08f,p); c.drawArc(RectF(w*.58f,h*.41f,w*.83f,h*.66f),-80f,180f,false,p); c.drawLine(w*.42f,h*.29f,w*.46f,h*.17f,p) }
            Icon.CUSTOMER -> { c.drawCircle(w*.50f,h*.35f,s*.13f,p); c.drawArc(RectF(w*.27f,h*.48f,w*.73f,h*.82f),200f,140f,false,p) }
            Icon.PRODUCT -> { c.drawRect(w*.27f,h*.30f,w*.73f,h*.72f,p); c.drawLine(w*.27f,h*.42f,w*.73f,h*.42f,p); c.drawLine(w*.50f,h*.30f,w*.50f,h*.72f,p) }
            Icon.INVOICE, Icon.REPORT -> { c.drawRoundRect(RectF(w*.30f,h*.20f,w*.70f,h*.78f),s*.04f,s*.04f,p); c.drawLine(w*.38f,h*.38f,w*.62f,h*.38f,p); c.drawLine(w*.38f,h*.50f,w*.62f,h*.50f,p); c.drawLine(w*.38f,h*.62f,w*.57f,h*.62f,p) }
            Icon.WALLET -> { c.drawRoundRect(RectF(w*.20f,h*.32f,w*.78f,h*.70f),s*.06f,s*.06f,p); c.drawRoundRect(RectF(w*.54f,h*.42f,w*.82f,h*.60f),s*.04f,s*.04f,p) }
            Icon.BELL -> { c.drawArc(RectF(w*.30f,h*.25f,w*.70f,h*.66f),190f,160f,false,p); c.drawLine(w*.30f,h*.58f,w*.70f,h*.58f,p); c.drawCircle(w*.50f,h*.73f,s*.05f,p) }
            Icon.HOME -> { c.drawPath(Path().apply { moveTo(w*.20f,h*.48f); lineTo(w*.50f,h*.22f); lineTo(w*.80f,h*.48f); moveTo(w*.30f,h*.43f); lineTo(w*.30f,h*.75f); lineTo(w*.70f,h*.75f); lineTo(w*.70f,h*.43f) },p) }
            Icon.EXPENSE -> { c.drawCircle(w*.50f,h*.50f,s*.25f,p); c.drawLine(w*.35f,h*.50f,w*.65f,h*.50f,p) }
            Icon.SETTINGS -> { c.drawCircle(w*.50f,h*.50f,s*.20f,p); c.drawCircle(w*.50f,h*.50f,s*.06f,p); c.drawLine(w*.50f,h*.18f,w*.50f,h*.28f,p); c.drawLine(w*.50f,h*.72f,w*.50f,h*.82f,p); c.drawLine(w*.18f,h*.50f,w*.28f,h*.50f,p); c.drawLine(w*.72f,h*.50f,w*.82f,h*.50f,p) }
            Icon.SHARE -> { c.drawCircle(w*.30f,h*.50f,s*.07f,p); c.drawCircle(w*.68f,h*.28f,s*.07f,p); c.drawCircle(w*.68f,h*.72f,s*.07f,p); c.drawLine(w*.36f,h*.46f,w*.62f,h*.32f,p); c.drawLine(w*.36f,h*.54f,w*.62f,h*.68f,p) }
            Icon.CALENDAR -> { c.drawRect(w*.24f,h*.28f,w*.76f,h*.74f,p); c.drawLine(w*.24f,h*.42f,w*.76f,h*.42f,p); c.drawLine(w*.37f,h*.20f,w*.37f,h*.34f,p); c.drawLine(w*.63f,h*.20f,w*.63f,h*.34f,p) }
            Icon.ABOUT -> { c.drawCircle(w*.50f,h*.50f,s*.27f,p); c.drawCircle(w*.50f,h*.36f,s*.02f,p); c.drawLine(w*.50f,h*.47f,w*.50f,h*.65f,p) }
            Icon.CONTACT -> { c.drawRoundRect(RectF(w*.24f,h*.30f,w*.76f,h*.70f),s*.04f,s*.04f,p); c.drawLine(w*.24f,h*.33f,w*.50f,h*.52f,p); c.drawLine(w*.76f,h*.33f,w*.50f,h*.52f,p) }
            Icon.EXIT -> { c.drawRect(w*.25f,h*.25f,w*.58f,h*.75f,p); c.drawLine(w*.43f,h*.50f,w*.80f,h*.50f,p); c.drawLine(w*.70f,h*.40f,w*.80f,h*.50f,p); c.drawLine(w*.70f,h*.60f,w*.80f,h*.50f,p) }
        }
        p.style = Paint.Style.FILL
    }
}
