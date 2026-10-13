package br.com.hydra.automacao.ui.components

import android.content.Context
import android.graphics.Color
import android.view.Gravity
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import br.com.hydra.automacao.ui.theme.HydraTheme

/**
 * Botão aconchegante com feedback tátil de clique e spinner animado de loading integrado.
 */
class CozyButton(
    context: Context,
    private val text: String,
    private val style: Style = Style.PRIMARY,
    private val iconRes: Int? = null,
    private val onClick: () -> Unit
) : FrameLayout(context) {

    enum class Style { PRIMARY, SECONDARY, OUTLINE }

    private val contentLayout: LinearLayout
    private val labelView: TextView
    private val iconView: ImageView?
    private val spinnerView: CozyLoadingSpinner
    private var isLoadingState = false

    init {
        val padH = HydraTheme.dp(context, 16)
        val padV = HydraTheme.dp(context, 13)

        // Configuração de estilo visual e ripple
        when (style) {
            Style.PRIMARY -> {
                background = HydraTheme.ripple(
                    context,
                    normalColor = HydraTheme.primary,
                    pressedColor = HydraTheme.primaryPressed,
                    radiusDp = 14
                )
            }
            Style.SECONDARY -> {
                background = HydraTheme.ripple(
                    context,
                    normalColor = HydraTheme.primaryLight,
                    pressedColor = Color.rgb(215, 230, 220),
                    radiusDp = 14
                )
            }
            Style.OUTLINE -> {
                background = HydraTheme.rounded(
                    context,
                    bgColor = Color.TRANSPARENT,
                    radiusDp = 14,
                    strokeColor = HydraTheme.borderMedium,
                    strokeWidthDp = 1
                )
            }
        }

        // Layout de conteúdo (ícone + label)
        contentLayout = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
            setPadding(padH, padV, padH, padV)
        }

        val textColor = when (style) {
            Style.PRIMARY -> Color.WHITE
            Style.SECONDARY -> HydraTheme.primary
            Style.OUTLINE -> HydraTheme.textPrimary
        }

        if (iconRes != null) {
            iconView = ImageView(context).apply {
                setImageResource(iconRes)
                setColorFilter(textColor)
                layoutParams = LinearLayout.LayoutParams(HydraTheme.dp(context, 18), HydraTheme.dp(context, 18)).also {
                    it.marginEnd = HydraTheme.dp(context, 8)
                }
            }
            contentLayout.addView(iconView)
        } else {
            iconView = null
        }

        labelView = TextView(context).apply {
            text = this@CozyButton.text
            textSize = 14.5f
            typeface = HydraTheme.fontMedium
            setTextColor(textColor)
            gravity = Gravity.CENTER
        }
        contentLayout.addView(labelView)

        // Spinner inline à direita do texto (não sobreposto): aparece ao lado
        // do label durante o loading, sem cobrir o texto.
        val spinnerColor = if (style == Style.PRIMARY) Color.WHITE else HydraTheme.primary
        spinnerView = CozyLoadingSpinner(context, strokeColor = spinnerColor, strokeWidthDp = 2.4f).apply {
            val size = HydraTheme.dp(context, 18)
            layoutParams = LinearLayout.LayoutParams(size, size).also {
                it.marginStart = HydraTheme.dp(context, 8)
            }
            visibility = GONE
        }
        contentLayout.addView(spinnerView)
        addView(contentLayout)

        setOnClickListener {
            if (!isLoadingState) {
                onClick()
            }
        }
    }

    fun setLoading(loading: Boolean, loadingText: String? = null) {
        isLoadingState = loading
        isEnabled = !loading
        if (loading) {
            if (loadingText != null) labelView.text = loadingText
            spinnerView.visibility = VISIBLE
            iconView?.visibility = GONE
        } else {
            labelView.text = text
            spinnerView.visibility = GONE
            iconView?.visibility = VISIBLE
        }
    }
}
