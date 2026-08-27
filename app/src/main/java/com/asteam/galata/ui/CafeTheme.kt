package com.asteam.galata.ui

import android.content.Context
import android.graphics.*
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import kotlin.math.min

/**
 * تم گرافیکی مشترک گالاتا.
 * تمام رنگ‌ها، کارت‌ها، آیکون‌ها و لوگوی کافه به‌صورت کدنویسی تولید می‌شوند.
 */
object CafeTheme {
    // پالت گرم کافه‌ای.
    val espresso = Color.rgb(67, 40, 24)
    val mocha = Color.rgb(98, 60, 37)
    val caramel = Color.rgb(196, 133, 76)
    val cream = Color.rgb(250, 242, 230)
    val foam = Color.rgb(255, 250, 243)
    val leaf = Color.rgb(79, 111, 67)
    val danger = Color.rgb(177, 75, 58)
    val ink = Color.rgb(49, 37, 29)

    fun dp(context: Context, value: Int): Int = (value * context.resources.displayMetrics.density).toInt()

    fun rounded(color: Int, radius: Float, stroke: Int? = null, strokeWidth: Int = 1): GradientDrawable = GradientDrawable().apply {
        shape = GradientDrawable.RECTANGLE
        setColor(color)
        cornerRadius = radius
        if (stroke != null) setStroke(strokeWidth, stroke)
    }

    fun sectionTitle(context: Context, text: String): TextView = TextView(context).apply {
        this.text = text
        textSize = 18f
        setTextColor(ink)
        gravity = Gravity.END
        setPadding(dp(context, 18), dp(context, 16), dp(context, 18), dp(context, 8))
        typeface = Typeface.DEFAULT_BOLD
    }

    fun card(context: Context, title: String, subtitle: String, iconType: CafeIconView.Icon): LinearLayout = LinearLayout(context).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        layoutDirection = View.LAYOUT_DIRECTION_RTL
        setPadding(dp(context, 14), dp(context, 12), dp(context, 14), dp(context, 12))
        background = rounded(foam, dp(context, 18).toFloat(), Color.rgb(232, 216, 198), dp(context, 1))
        elevation = dp(context, 3).toFloat()
        layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
            setMargins(dp(context, 14), dp(context, 6), dp(context, 14), dp(context, 6))
        }

        addView(CafeIconView(context).apply {
            icon = iconType
            layoutParams = LinearLayout.LayoutParams(dp(context, 46), dp(context, 46)).apply { marginStart = dp(context, 12) }
        })

        addView(LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.END
            addView(TextView(context).apply {
                text = title
                textSize = 16f
                setTextColor(ink)
                gravity = Gravity.END
                typeface = Typeface.DEFAULT_BOLD
            })
            addView(TextView(context).apply {
                text = subtitle
                textSize = 13f
                setTextColor(Color.rgb(111, 91, 76))
                gravity = Gravity.END
            })
        }, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
    }
}

/**
 * لوگوی Galata به‌صورت Canvas و بدون وابستگی به عکس ثابت.
 * فنجان، بخار، دانه قهوه و نام Galata مستقیماً رسم می‌شوند.
 */
class GalataLogoView(context: Context) : View(context) {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val w = width.toFloat()
        val h = height.toFloat()
        val s = min(w, h)

        // زمینه دایره‌ای برند.
        paint.color = CafeTheme.espresso
        canvas.drawCircle(w / 2f, h / 2f, s * 0.46f, paint)
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = s * 0.035f
        paint.color = CafeTheme.caramel
        canvas.drawCircle(w / 2f, h / 2f, s * 0.43f, paint)
        paint.style = Paint.Style.FILL

        // فنجان قهوه.
        paint.color = CafeTheme.cream
        val cup = RectF(w * .25f, h * .30f, w * .72f, h * .59f)
        canvas.drawRoundRect(cup, s * .08f, s * .08f, paint)
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = s * .045f
        canvas.drawArc(RectF(w * .62f, h * .34f, w * .83f, h * .55f), -80f, 180f, false, paint)
        paint.style = Paint.Style.FILL

        // بخار.
        paint.color = CafeTheme.caramel
        paint.strokeWidth = s * .025f
        paint.style = Paint.Style.STROKE
        paint.strokeCap = Paint.Cap.ROUND
        val steam1 = Path().apply { moveTo(w*.42f,h*.28f); cubicTo(w*.35f,h*.20f,w*.49f,h*.17f,w*.43f,h*.10f) }
        val steam2 = Path().apply { moveTo(w*.55f,h*.28f); cubicTo(w*.48f,h*.21f,w*.62f,h*.17f,w*.56f,h*.09f) }
        canvas.drawPath(steam1, paint); canvas.drawPath(steam2, paint)
        paint.style = Paint.Style.FILL

        // نام برند.
        paint.color = CafeTheme.foam
        paint.textAlign = Paint.Align.CENTER
        paint.typeface = Typeface.create(Typeface.SERIF, Typeface.BOLD_ITALIC)
        paint.textSize = s * .16f
        canvas.drawText("Galata", w/2f, h*.80f, paint)
    }
}

/** آیکون‌های اختصاصی رابط که با Canvas رسم می‌شوند. */
class CafeIconView(context: Context) : View(context) {
    enum class Icon { COFFEE, TEA, CUSTOMER, PRODUCT, INVOICE, WALLET, BELL, HOME, EXPENSE }
    var icon: Icon = Icon.COFFEE
    private val p = Paint(Paint.ANTI_ALIAS_FLAG)

    override fun onDraw(c: Canvas) {
        super.onDraw(c)
        val w = width.toFloat(); val h = height.toFloat(); val s = min(w,h)
        p.color = CafeTheme.cream
        c.drawCircle(w/2f,h/2f,s*.48f,p)
        p.color = CafeTheme.espresso
        p.style = Paint.Style.STROKE
        p.strokeWidth = s*.08f
        p.strokeCap = Paint.Cap.ROUND
        when(icon) {
            Icon.COFFEE -> { c.drawRoundRect(RectF(w*.22f,h*.36f,w*.65f,h*.68f),s*.08f,s*.08f,p); c.drawArc(RectF(w*.58f,h*.41f,w*.83f,h*.66f),-80f,180f,false,p); c.drawLine(w*.38f,h*.29f,w*.42f,h*.17f,p); c.drawLine(w*.53f,h*.29f,w*.56f,h*.15f,p) }
            Icon.TEA -> { c.drawRoundRect(RectF(w*.22f,h*.38f,w*.66f,h*.68f),s*.05f,s*.05f,p); c.drawArc(RectF(w*.59f,h*.42f,w*.83f,h*.66f),-80f,180f,false,p); c.drawLine(w*.45f,h*.38f,w*.52f,h*.22f,p) }
            Icon.CUSTOMER -> { c.drawCircle(w*.50f,h*.35f,s*.13f,p); c.drawArc(RectF(w*.27f,h*.48f,w*.73f,h*.82f),200f,140f,false,p) }
            Icon.PRODUCT -> { c.drawRect(w*.27f,h*.30f,w*.73f,h*.72f,p); c.drawLine(w*.27f,h*.42f,w*.73f,h*.42f,p); c.drawLine(w*.50f,h*.30f,w*.50f,h*.72f,p) }
            Icon.INVOICE -> { c.drawRoundRect(RectF(w*.30f,h*.20f,w*.70f,h*.78f),s*.04f,s*.04f,p); c.drawLine(w*.38f,h*.38f,w*.62f,h*.38f,p); c.drawLine(w*.38f,h*.50f,w*.62f,h*.50f,p); c.drawLine(w*.38f,h*.62f,w*.57f,h*.62f,p) }
            Icon.WALLET -> { c.drawRoundRect(RectF(w*.20f,h*.32f,w*.78f,h*.70f),s*.06f,s*.06f,p); c.drawRoundRect(RectF(w*.54f,h*.42f,w*.82f,h*.60f),s*.04f,s*.04f,p) }
            Icon.BELL -> { c.drawArc(RectF(w*.30f,h*.25f,w*.70f,h*.66f),190f,160f,false,p); c.drawLine(w*.30f,h*.58f,w*.70f,h*.58f,p); c.drawCircle(w*.50f,h*.73f,s*.05f,p) }
            Icon.HOME -> { val path=Path().apply{moveTo(w*.20f,h*.48f);lineTo(w*.50f,h*.22f);lineTo(w*.80f,h*.48f);moveTo(w*.30f,h*.43f);lineTo(w*.30f,h*.75f);lineTo(w*.70f,h*.75f);lineTo(w*.70f,h*.43f)}; c.drawPath(path,p) }
            Icon.EXPENSE -> { c.drawCircle(w*.50f,h*.50f,s*.25f,p); c.drawLine(w*.50f,h*.32f,w*.50f,h*.68f,p); c.drawLine(w*.39f,h*.43f,w*.61f,h*.43f,p); c.drawLine(w*.39f,h*.57f,w*.61f,h*.57f,p) }
        }
        p.style = Paint.Style.FILL
    }
}
