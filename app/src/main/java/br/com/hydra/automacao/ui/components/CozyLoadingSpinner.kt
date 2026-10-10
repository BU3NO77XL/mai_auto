package br.com.hydra.automacao.ui.components

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.view.View
import android.view.animation.LinearInterpolator
import br.com.hydra.automacao.ui.theme.HydraTheme

/**
 * Spinner de loading minimalista, suave e aconchegante.
 * Rotação orgânica contínua com traço arredondado.
 */
class CozyLoadingSpinner(
    context: Context,
    private val strokeColor: Int = HydraTheme.primary,
    private val strokeWidthDp: Float = 2.8f
) : View(context) {

    private val trackPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        color = strokeColor and 0x00FFFFFF or 0x22000000 // 13% de opacidade para a trilha
    }

    private val spinnerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        color = strokeColor
    }

    private val arcBounds = RectF()
    private var currentAngle = 0f
    private var sweepAngle = 80f
    private var animator: ValueAnimator? = null

    init {
        val strokePx = HydraTheme.dpF(context, strokeWidthDp)
        trackPaint.strokeWidth = strokePx
        spinnerPaint.strokeWidth = strokePx
        startAnimation()
    }

    private fun startAnimation() {
        animator = ValueAnimator.ofFloat(0f, 360f).apply {
            duration = 1000L
            repeatCount = ValueAnimator.INFINITE
            interpolator = LinearInterpolator()
            addUpdateListener { anim ->
                currentAngle = anim.animatedValue as Float
                invalidate()
            }
            start()
        }
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        val stroke = spinnerPaint.strokeWidth
        val padding = stroke / 2f + HydraTheme.dpF(context, 2f)
        arcBounds.set(padding, padding, w - padding, h - padding)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        // Trilha sutil de fundo
        canvas.drawOval(arcBounds, trackPaint)
        // Arco animado em rotação
        canvas.drawArc(arcBounds, currentAngle, sweepAngle, false, spinnerPaint)
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        if (animator?.isStarted != true) {
            animator?.start()
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        animator?.cancel()
    }
}
