package br.com.hydra.automacao.update

import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.Gravity
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import br.com.hydra.automacao.ui.components.CozyButton
import br.com.hydra.automacao.ui.theme.HydraTheme

/**
 * Diálogo "Nova versão disponível" no padrão Cozy do app.
 * Mostra versão, botão Atualizar (baixa + instala) e botão Depois.
 */
class UpdateDialog(
    context: Context,
    private val remote: AppUpdateChecker.RemoteVersion,
    private val onUpdate: () -> Unit,
) : Dialog(context) {

    private lateinit var updateButton: CozyButton

    companion object {
        /** Títulos curtos, sorteados a cada exibição. */
        private val TITLES = listOf(
            "Tem novidade pra você ✨",
            "Cheguei com melhorias 💚",
            "Olha o que preparei pra você 🎁",
        )

        /** Corpo em 1ª pessoa, curto. Dica de permissão embutida numa frase. */
        private val BODIES = listOf(
            "Sou eu mesma com novidades pra você. É rapidinho! 💚",
            "Caprichei nessa versão ✨ Atualiza aqui embaixo!",
            "Deixei tudo melhor pra você 🎁 Um toque e pronto!",
        )

        private const val PERMISSION_HINT =
            "Se pedir permissão de outras fontes, sou eu mesma — " +
                "ative e volte aqui. 💚"
    }

    init {
        val padH = HydraTheme.dp(context, 20)
        val padV = HydraTheme.dp(context, 20)
        val card = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            background = HydraTheme.rounded(context, Color.WHITE, radiusDp = 24)
            setPadding(padH, padV, padH, padV)
        }

        // Título + corpo sorteados (mesmo índice): nunca repete a mesma
        // combinação em sequência e mantém 1ª pessoa feminina.
        val pick = (TITLES.indices).random()
        val prefs = context.getSharedPreferences("mai_update", Context.MODE_PRIVATE)
        val lastPick = prefs.getInt("last_title_pick", -1)
        val index = if (TITLES.size > 1) {
            var candidate = pick
            var guard = 0
            while (candidate == lastPick && guard < 5) {
                candidate = (TITLES.indices).random()
                guard++
            }
            candidate
        } else pick
        prefs.edit().putInt("last_title_pick", index).apply()

        card.addView(TextView(context).apply {
            // Só versionName (padrão dos grandes apps): build number é
            // metadado interno e confunde o usuário leigo.
            text = "${TITLES[index]} · v${remote.version}"
            textSize = 17f
            typeface = HydraTheme.fontMedium
            setTextColor(HydraTheme.textPrimary)
            gravity = Gravity.CENTER
        })

        card.addView(TextView(context).apply {
            text = BODIES[index] + "\n" + PERMISSION_HINT
            textSize = 13.5f
            typeface = HydraTheme.fontRegular
            setTextColor(HydraTheme.textSecondary)
            gravity = Gravity.CENTER
            setLineSpacing(HydraTheme.dp(context, 2).toFloat(), 1f)
            setPadding(0, HydraTheme.dp(context, 10), 0, HydraTheme.dp(context, 18))
        })

        updateButton = CozyButton(context, text = "Atualizar agora", onClick = {
            updateButton.setLoading(true, "Baixando…")
            onUpdate()
        })
        card.addView(updateButton, LinearLayout.LayoutParams(-1, dp56()))

        card.addView(TextView(context).apply {
            text = "Depois"
            textSize = 13.5f
            typeface = HydraTheme.fontMedium
            setTextColor(HydraTheme.textMuted)
            gravity = Gravity.CENTER
            setPadding(0, HydraTheme.dp(context, 12), 0, HydraTheme.dp(context, 2))
            setOnClickListener { dismiss() }
        })

        setContentView(card, ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT,
        ))
        window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        window?.setLayout(
            (context.resources.displayMetrics.widthPixels * 0.88).toInt(),
            ViewGroup.LayoutParams.WRAP_CONTENT,
        )
        setCanceledOnTouchOutside(true)
    }

    private fun dp56(): Int = HydraTheme.dp(context, 56)
}
