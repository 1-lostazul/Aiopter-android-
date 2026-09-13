package io.aiopter.companion.overlay

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.*
import android.graphics.drawable.GradientDrawable
import android.view.View
import android.view.animation.LinearInterpolator
import kotlin.math.min

class RotorBubbleView(context: Context) : View(context) {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private var angle = 0f
    private val animator = ValueAnimator.ofFloat(0f, 360f).apply {
        duration = 3800; repeatCount = ValueAnimator.INFINITE; interpolator = LinearInterpolator()
        addUpdateListener { angle = it.animatedValue as Float; invalidate() }
    }
    init {
        contentDescription = "Open AIopter floating assistant"
        elevation = 16f
        background = GradientDrawable().apply { shape = GradientDrawable.OVAL; setColor(Color.rgb(10, 20, 36)); setStroke(2, Color.rgb(64, 174, 255)) }
        animator.start()
    }
    override fun onDetachedFromWindow() { animator.cancel(); super.onDetachedFromWindow() }
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val r = min(width, height) * .38f
        canvas.save(); canvas.rotate(angle, width / 2f, height / 2f)
        repeat(3) { i ->
            canvas.save(); canvas.rotate(i * 120f, width / 2f, height / 2f)
            paint.shader = LinearGradient(width / 2f, height / 2f - r, width / 2f, height / 2f, Color.rgb(76, 225, 255), Color.rgb(42, 104, 255), Shader.TileMode.CLAMP)
            val rect = RectF(width / 2f - r * .16f, height / 2f - r, width / 2f + r * .16f, height / 2f - r * .24f)
            canvas.drawRoundRect(rect, r * .18f, r * .18f, paint); canvas.restore()
        }
        canvas.restore(); paint.shader = null; paint.color = Color.WHITE; canvas.drawCircle(width / 2f, height / 2f, r * .13f, paint)
    }
}
