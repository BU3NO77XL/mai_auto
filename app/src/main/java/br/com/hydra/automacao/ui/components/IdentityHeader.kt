package br.com.hydra.automacao.ui.components

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import br.com.hydra.automacao.ui.theme.HydraTheme

/**
 * Cabeçalho de identidade compartilhado pelas abas com chrome da Home
 * (Início, Histórico e Teste): avatar, PAINEL LUNA, saudação, badge
 * PLANO ELITE e sino de notificação. Extraído da HomeScreen para
 * reaproveitamento sem duplicação de código.
 */
fun buildIdentityHeader(context: Context): View = LinearLayout(context).apply {
    orientation = LinearLayout.HORIZONTAL
    gravity = Gravity.CENTER_VERTICAL
    addView(TextView(context).apply {
        text = "L"
        textSize = 19f
        typeface = Typeface.create("sans-serif", Typeface.BOLD)
        setTextColor(Color.WHITE)
        gravity = Gravity.CENTER
        background = HydraTheme.rounded(context, Color.rgb(27, 43, 36), 100)
    }, LinearLayout.LayoutParams(HydraTheme.dp(context, 44), HydraTheme.dp(context, 44)))

    val identity = LinearLayout(context).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(HydraTheme.dp(context, 10), 0, 0, 0)
    }
    identity.addView(TextView(context).apply {
        text = "PAINEL LUNA"
        textSize = 10f
        typeface = HydraTheme.fontMedium
        setTextColor(Color.rgb(18, 18, 18))
        letterSpacing = .04f
    })
    identity.addView(space(context, 2))
    identity.addView(TextView(context).apply {
        text = "Olá, Alexandre!"
        textSize = 16f
        typeface = HydraTheme.fontMedium
        setTextColor(Color.rgb(27, 43, 36))
    })
    identity.addView(space(context, 4))
    identity.addView(TextView(context).apply {
        text = "PLANO ELITE"
        textSize = 10f
        typeface = HydraTheme.fontMedium
        setTextColor(Color.rgb(244, 247, 245))
        gravity = Gravity.CENTER
        background = HydraTheme.darkGradient(context, 9)
    }, LinearLayout.LayoutParams(HydraTheme.dp(context, 88), HydraTheme.dp(context, 18)))
    addView(identity, LinearLayout.LayoutParams(0, -2, 1f))
    addView(HydraTheme.notificationButton(context), LinearLayout.LayoutParams(HydraTheme.dp(context, 44), HydraTheme.dp(context, 44)))
}

private fun space(context: Context, heightDp: Int): View = View(context).apply {
    layoutParams = LinearLayout.LayoutParams(1, HydraTheme.dp(context, heightDp))
}
