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
        /** Títulos curtos e acolhedores, sorteados a cada exibição. */
        private val TITLES = listOf(
            "Tem novidade pra você ✨",
            "Cheguei com melhorias 💚",
            "Olha o que preparei pra você 🎁",
            "Atualização fresquinha 🌱",
            "Passei pra deixar tudo melhor 🌟",
        )

        /** Corpo em 1ª pessoa, sorteado junto. Mantém o passo a passo da permissão. */
        private val BODIES = listOf(
            "Sou eu mesma trazendo melhorias para a sua experiência. " +
                "Atualizar é rapidinho: toque abaixo e pronto! 💚",
            "Caprichei nessa versão para você ✨ Toque abaixo e " +
                "em segundos já está tudo novinho.",
            "Preparei tudo com carinho para a sua experiência ficar ainda melhor. " +
                "É só tocar abaixo, rapidinho! 🎁",
            "Vim deixar tudo redondinho para você 🌱 Um toque abaixo " +
                "e já está pronta!",
            "Quero ver você aproveitando o melhor de mim 🌟 " +
                "Atualiza aqui embaixo, é rapidinho!",
        )

        private const val PERMISSION_HINT =
            "Se o celular pedir permissão para \"instalar apps de outras fontes\", " +
                "não se preocupe, sou eu mesma. Toque em Configurações, " +
                "ative a chavinha e volte aqui para concluir. 💚"
    }

    init {
        val pad = HydraTheme.dp(context, 24)
        val card = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            background = HydraTheme.rounded(context, Color.WHITE, radiusDp = 20)
            setPadding(pad, pad, pad, pad)
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
            // Sufixo -(versão): diferencia builds da mesma versionName
            // (ex: -0.2.0 (9) vs -0.2.0 (10)).
            text = "${TITLES[index]} -${remote.version} (${remote.build})"
            textSize = 18f
            typeface = HydraTheme.fontMedium
            setTextColor(HydraTheme.textPrimary)
            gravity = Gravity.CENTER
        })

        card.addView(TextView(context).apply {
            text = BODIES[index] + "\n\n" + PERMISSION_HINT
            textSize = 14f
            typeface = HydraTheme.fontRegular
            setTextColor(HydraTheme.textSecondary)
            gravity = Gravity.CENTER
            setPadding(0, HydraTheme.dp(context, 8), 0, HydraTheme.dp(context, 20))
        })

        updateButton = CozyButton(context, text = "Atualizar agora", onClick = {
            updateButton.setLoading(true, "Baixando…")
            onUpdate()
        })
        card.addView(updateButton, LinearLayout.LayoutParams(-1, -2))

        card.addView(TextView(context).apply {
            text = "Depois"
            textSize = 14f
            typeface = HydraTheme.fontMedium
            setTextColor(HydraTheme.textMuted)
            gravity = Gravity.CENTER
            setPadding(0, HydraTheme.dp(context, 14), 0, 0)
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
}
