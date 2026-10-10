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
 * Barra de progresso sutil e orgânica com cantos em pílula.
 * Suporta modo indeterminado pulsante e determinado.
 */
class CozyProgressBar(
    context: Context,
    private val barColor: Int = HydraTheme.primary,
    private val trackColor: Int = HydraTheme.surfaceSubtle
) : View(context) {

    private val trackPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = trackColor
    }

    private val progressPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = barColor
    }

    private val boundsRect = RectF()
    private val progressRect = RectF()
    private var isIndeterminate = true
    private var indeterminateOffset = 0f
    private var animator: ValueAnimator? = null

    init {
        startIndeterminateAnim()
    }

    fun setIndeterminate(indeterminate: Boolean) {
        if (isIndeterminate == indeterminate) return
        isIndeterminate = indeterminate
        if (indeterminate) {
            startIndeterminateAnim()
        } else {
            animator?.cancel()
            invalidate()
        }
    }

    private fun startIndeterminateAnim() {
        animator?.cancel()
        animator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 1400L
            repeatCount = ValueAnimator.INFINITE
            interpolator = LinearInterpolator()
            addUpdateListener { anim ->
                indeterminateOffset = anim.animatedValue as Float
                invalidate()
            }
            start()
        }
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        boundsRect.set(0f, 0f, w.toFloat(), h.toFloat())
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val radius = height / 2f
        // Trilha de fundo
        canvas.drawRoundRect(boundsRect, radius, radius, trackPaint)

        if (isIndeterminate) {
            val totalWidth = width.toFloat()
            val chunkWidth = totalWidth * 0.35f
            // Deslocamento cíclico suave de -chunkWidth até totalWidth
            val left = -chunkWidth + (totalWidth + chunkWidth) * indeterminateOffset
            val right = left + chunkWidth
            progressRect.set(left.coerceAtLeast(0f), 0f, right.coerceAtMost(totalWidth), height.toFloat())
            canvas.drawRoundRect(progressRect, radius, radius, progressPaint)
        }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        if (isIndeterminate && animator?.isStarted != true) {
            animator?.start()
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        animator?.cancel()
    }
}
