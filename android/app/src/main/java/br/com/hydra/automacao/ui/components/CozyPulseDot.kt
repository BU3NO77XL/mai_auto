package br.com.hydra.automacao.ui.components

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import br.com.hydra.automacao.ui.theme.HydraTheme

/**
 * Ponto indicador que respira com halo pulsante ("Breathing dot").
 * Traz sensação orgânica, viva e acolhedora de monitoramento em tempo real.
 */
class CozyPulseDot(
    context: Context,
    var dotColor: Int = HydraTheme.success,
    private val isPulsing: Boolean = true
) : View(context) {

    private val corePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = dotColor
    }

    private val pulsePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = dotColor
    }

    private var pulseFraction = 0f
    private var animator: ValueAnimator? = null

    init {
        if (isPulsing) {
            startPulse()
        }
    }

    fun setColor(newColor: Int) {
        dotColor = newColor
        corePaint.color = newColor
        pulsePaint.color = newColor
        invalidate()
    }

    private fun startPulse() {
        animator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 1800L
            repeatCount = ValueAnimator.INFINITE
            interpolator = AccelerateDecelerateInterpolator()
            addUpdateListener { anim ->
                pulseFraction = anim.animatedValue as Float
                invalidate()
            }
            start()
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val cx = width / 2f
        val cy = height / 2f
        val maxRadius = minOf(cx, cy)
        val coreRadius = maxRadius * 0.42f

        if (isPulsing) {
            // Halo expandindo de coreRadius até maxRadius com fade de opacidade
            val haloRadius = coreRadius + (maxRadius - coreRadius) * pulseFraction
            val alpha = ((1f - pulseFraction) * 0.38f * 255).toInt().coerceIn(0, 255)
            pulsePaint.alpha = alpha
            canvas.drawCircle(cx, cy, haloRadius, pulsePaint)
        }

        // Ponto central sólido
        corePaint.color = dotColor
        canvas.drawCircle(cx, cy, coreRadius, corePaint)
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        if (isPulsing && animator?.isStarted != true) {
            animator?.start()
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        animator?.cancel()
    }
}
