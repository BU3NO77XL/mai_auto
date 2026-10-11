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

    init {
        val pad = HydraTheme.dp(context, 24)
        val card = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            background = HydraTheme.rounded(context, Color.WHITE, radiusDp = 20)
            setPadding(pad, pad, pad, pad)
        }

        card.addView(TextView(context).apply {
            text = "Nova versão disponível"
            textSize = 18f
            typeface = HydraTheme.fontMedium
            setTextColor(HydraTheme.textPrimary)
            gravity = Gravity.CENTER
        })

        card.addView(TextView(context).apply {
            text = "Mai v${remote.version} já está pronta para download."
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
