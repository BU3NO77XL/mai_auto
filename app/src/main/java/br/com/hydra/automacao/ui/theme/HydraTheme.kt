package br.com.hydra.automacao.ui.theme

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorFilter
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.RippleDrawable
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import br.com.hydra.automacao.R

/**
 * Design tokens e utilitários visuais do Hydra.
 * Paleta calorosa, minimalista e limpa ("Cozy Sage & Linen").
 */
object HydraTheme {
    // ── Cores de Superfície & Fundo ──
    val background = Color.rgb(248, 249, 246)     // Linho suave / off-white acolhedor
    val surface = Color.rgb(255, 255, 255)        // Branco puro dos cartões
    val surfaceSubtle = Color.rgb(243, 246, 243)  // Fundo sutil de inputs e chips
    val surfaceMuted = Color.rgb(238, 241, 238)

    // ── Bordas & Divisores ──
    val borderSubtle = Color.rgb(228, 234, 229)   // Borda leve
    val borderMedium = Color.rgb(212, 221, 214)   // Borda de input inativo
    val borderFocused = Color.rgb(42, 95, 72)     // Borda verde sálvia no foco

    // ── Textos & Tipografia ──
    val textPrimary = Color.rgb(20, 29, 24)       // Grafite orgânico profundo
    val textSecondary = Color.rgb(79, 96, 87)     // Verde-ardósia médio
    val textMuted = Color.rgb(126, 145, 135)      // Legendas e hints suaves

    // ── Cores Primárias (Verde Sálvia Sofisticado) ──
    val primary = Color.rgb(35, 83, 62)           // Verde floresta / sálvia principal
    val primaryLight = Color.rgb(234, 242, 237)   // Pílula / fundo de destaque sutil
    val primaryHover = Color.rgb(47, 107, 81)
    val primaryPressed = Color.rgb(25, 62, 46)

    // ── Cores Semânticas / Status Aconchegantes ──
    val success = Color.rgb(44, 122, 84)          // Verde êxito
    val successBg = Color.rgb(232, 244, 237)      // Fundo sucesso
    val successText = Color.rgb(27, 89, 58)

    val warning = Color.rgb(186, 125, 43)         // Âmbar suave
    val warningBg = Color.rgb(254, 247, 236)      // Fundo alerta suave
    val warningText = Color.rgb(138, 90, 26)

    val error = Color.rgb(196, 76, 58)            // Coral / terracota moderno
    val errorBg = Color.rgb(253, 239, 237)        // Fundo erro suave
    val errorText = Color.rgb(156, 52, 36)

    // ── Tipografia ──
    val fontRegular: Typeface = Typeface.create("sans-serif", Typeface.NORMAL)
    val fontMedium: Typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
    val fontLight: Typeface = Typeface.create("sans-serif-light", Typeface.NORMAL)
    val fontMono: Typeface = Typeface.create("monospace", Typeface.NORMAL)

    // ── Utilitários de Medida e Drawables ──

    fun dp(context: Context, value: Int): Int =
        (value * context.resources.displayMetrics.density).toInt()

    fun dpF(context: Context, value: Float): Float =
        value * context.resources.displayMetrics.density

    fun rounded(
        context: Context,
        bgColor: Int,
        radiusDp: Int = 16,
        strokeColor: Int? = null,
        strokeWidthDp: Int = 1
    ): GradientDrawable = GradientDrawable().apply {
        setColor(bgColor)
        cornerRadius = dp(context, radiusDp).toFloat()
        if (strokeColor != null) {
            setStroke(dp(context, strokeWidthDp), strokeColor)
        }
    }

    fun pill(
        context: Context,
        bgColor: Int,
        strokeColor: Int? = null,
        strokeWidthDp: Int = 1
    ): GradientDrawable = rounded(context, bgColor, 999, strokeColor, strokeWidthDp)

    fun cardBackground(context: Context): GradientDrawable =
        rounded(context, surface, radiusDp = 20, strokeColor = borderSubtle, strokeWidthDp = 1)

    /**
     * Gradiente "escuro" metálico em 45°, conforme o Figma (Card / Suas vendas,
     * Card / Último resultado, Badge / Plano Elite). A faixa clara corre pela
     * anti-diagonal ("/") do retângulo; escuro nos cantos superior-esquerdo e
     * inferior-direito. Recalcula o shader nos bounds, então serve qualquer tamanho.
     */
    fun darkGradient(context: Context, radiusDp: Int): Drawable = object : Drawable() {
        private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        private val rect = RectF()
        private val colors = intArrayOf(
            Color.rgb(5, 5, 5), Color.rgb(64, 69, 66), Color.rgb(107, 112, 110),
            Color.rgb(56, 61, 59), Color.rgb(3, 3, 3)
        )
        private val positions = floatArrayOf(0f, .35f, .5f, .7f, 1f)
        private fun updateShader(b: Rect) {
            val w = b.width().toFloat(); val h = b.height().toFloat()
            if (w <= 0f || h <= 0f) return
            val k = 2f * w * h / (w * w + h * h)
            paint.shader = LinearGradient(
                b.left.toFloat(), b.top.toFloat(),
                b.left + k * h, b.top + k * w,
                colors, positions, Shader.TileMode.CLAMP
            )
        }
        override fun onBoundsChange(bounds: Rect) { updateShader(bounds); invalidateSelf() }
        override fun draw(canvas: Canvas) {
            if (paint.shader == null) updateShader(bounds)
            rect.set(bounds)
            canvas.drawRoundRect(rect, dp(context, radiusDp).toFloat(), dp(context, radiusDp).toFloat(), paint)
        }
        override fun setAlpha(alpha: Int) { paint.alpha = alpha }
        override fun setColorFilter(colorFilter: ColorFilter?) { paint.colorFilter = colorFilter }
        @Suppress("DEPRECATION") override fun getOpacity(): Int = PixelFormat.TRANSLUCENT
    }

    /**
     * Botão de notificação do cabeçalho: círculo branco de 44dp, sino e badge vermelho.
     * Componente extraído da Home para ser reaproveitado (Histórico) sem duplicar —
     * reutiliza o drawable [R.drawable.ic_notification_bell] já existente no projeto.
     */
    fun notificationButton(context: Context): View = FrameLayout(context).apply {
        background = rounded(context, Color.WHITE, 100)
        addView(ImageView(context).apply { setImageResource(R.drawable.ic_notification_bell) },
            FrameLayout.LayoutParams(dp(context, 18), dp(context, 20)).apply {
                leftMargin = dp(context, 13); topMargin = dp(context, 12)
            })
        addView(View(context).apply { background = rounded(context, Color.rgb(255, 59, 48), 100) },
            FrameLayout.LayoutParams(dp(context, 10), dp(context, 10)).apply {
                leftMargin = dp(context, 24); topMargin = dp(context, 9)
            })
    }

    fun ripple(
        context: Context,
        normalColor: Int,
        pressedColor: Int,
        radiusDp: Int = 16
    ): RippleDrawable {
        val content = rounded(context, normalColor, radiusDp)
        val mask = rounded(context, Color.WHITE, radiusDp)
        val colorStateList = ColorStateList(
            arrayOf(intArrayOf(android.R.attr.state_pressed), intArrayOf()),
            intArrayOf(pressedColor, pressedColor)
        )
        return RippleDrawable(colorStateList, content, mask)
    }

    fun createDivider(context: Context): View = View(context).apply {
        setBackgroundColor(borderSubtle)
        layoutParams = android.widget.LinearLayout.LayoutParams(-1, dp(context, 1))
    }
}
