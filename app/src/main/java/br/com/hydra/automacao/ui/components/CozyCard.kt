package br.com.hydra.automacao.ui.components

import android.content.Context
import android.graphics.Color
import android.view.Gravity
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import br.com.hydra.automacao.ui.theme.HydraTheme

/**
 * Card acolhedor, limpo e minimalista com cantos arredondados e borda sutil.
 */
class CozyCard(context: Context) : LinearLayout(context) {

    init {
        orientation = VERTICAL
        val pad = HydraTheme.dp(context, 16)
        setPadding(pad, pad, pad, pad)
        background = HydraTheme.cardBackground(context)
        elevation = HydraTheme.dp(context, 2).toFloat()
        layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
    }

    /**
     * Adiciona um cabeçalho editorial de seção com tag superior opcional.
     */
    fun addHeader(
        tag: String? = null,
        title: String,
        subtitle: String? = null,
        iconRes: Int? = null,
        statusView: View? = null
    ) {
        if (!tag.isNullOrBlank()) {
            val tagView = TextView(context).apply {
                text = tag.uppercase()
                textSize = 10.5f
                typeface = HydraTheme.fontMedium
                setTextColor(HydraTheme.primary)
                letterSpacing = 0.12f
            }
            addView(tagView)
            addSpacer(6)
        }

        val row = LinearLayout(context).apply {
            orientation = HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }

        if (iconRes != null) {
            val icon = ImageView(context).apply {
                setImageResource(iconRes)
                setColorFilter(HydraTheme.primary)
                layoutParams = LayoutParams(HydraTheme.dp(context, 20), HydraTheme.dp(context, 20)).also {
                    it.marginEnd = HydraTheme.dp(context, 10)
                }
            }
            row.addView(icon)
        }

        val titleView = TextView(context).apply {
            text = title
            textSize = 18f
            typeface = HydraTheme.fontMedium
            setTextColor(HydraTheme.textPrimary)
            layoutParams = LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f)
        }
        row.addView(titleView)

        if (statusView != null) {
            row.addView(statusView)
        }

        addView(row)

        if (!subtitle.isNullOrBlank()) {
            addSpacer(6)
            val subView = TextView(context).apply {
                text = subtitle
                textSize = 13.5f
                typeface = HydraTheme.fontRegular
                setTextColor(HydraTheme.textSecondary)
                setLineSpacing(HydraTheme.dp(context, 2).toFloat(), 1f)
            }
            addView(subView)
        }

        addSpacer(14)
    }

    fun addSpacer(heightDp: Int) {
        val spacer = View(context).apply {
            layoutParams = LayoutParams(1, HydraTheme.dp(context, heightDp))
        }
        addView(spacer)
    }
}
