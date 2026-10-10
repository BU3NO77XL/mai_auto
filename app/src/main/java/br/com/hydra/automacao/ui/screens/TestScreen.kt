package br.com.hydra.automacao.ui.screens

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.text.TextUtils
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import br.com.hydra.automacao.automation.AutotestService
import br.com.hydra.automacao.automation.TestResult
import br.com.hydra.automacao.automation.TestResultStore
import br.com.hydra.automacao.ui.components.buildIdentityHeader
import br.com.hydra.automacao.ui.theme.HydraTheme

/**
 * Tela de Autoteste (aba "Teste"), fiel ao Figma "teste/".
 * Chrome da Home (cabeçalho de identidade + meia-lua), card da Etapa 01 com os
 * dois botões, seção DIAGNÓSTICO com o último resultado e card de calibração.
 */
class TestScreen(
    private val context: Context,
    private val onStartTest: () -> Unit,
    private val onRefresh: () -> Unit
) {

    fun buildView(): View {
        val isRunning = AutotestService.isRunning
        val result = TestResultStore(context).load()

        val content = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            clipChildren = false
            addView(buildIdentityHeader(context)); addView(space(25))
            addView(label("Autoteste", 28f, BLACK, HydraTheme.fontMedium).apply { letterSpacing = -0.01f }); addView(space(6))
            addView(label("Validação segura do fluxo Shopee.", 14f, MUTED)); addView(space(35))
            addView(stepCard(isRunning)); addView(space(15))
            addView(sectionHeader("DIAGNÓSTICO")); addView(space(12))
            addView(diagnosticCard(result)); addView(space(24))
        }

        return FrameLayout(context).apply {
            setBackgroundColor(Color.WHITE); clipChildren = false
            // Nó "Decor / Meia-lua superior" (mesmo chrome da Home).
            addView(View(context).apply { background = oval(Color.argb(69, 217, 217, 217)) }, FrameLayout.LayoutParams(dp(255), dp(261)).apply {
                leftMargin = dp(192); topMargin = -dp(141)
            })
            addView(content, FrameLayout.LayoutParams(-1, -2))
        }
    }

    /** Card "Etapa 01" (343x207): textos, botão primário escuro e botão de contorno. */
    private fun stepCard(isRunning: Boolean): View = LinearLayout(context).apply {
        orientation = LinearLayout.VERTICAL
        background = HydraTheme.rounded(context, Color.WHITE, 18, BORDER, 1)
        val padH = dp(16); setPadding(padH, dp(18), padH, dp(9))
        addView(label("ETAPA 01", 11f, BLACK, HydraTheme.fontMedium).apply {
            includeFontPadding = false; gravity = Gravity.CENTER_VERTICAL
        }, LinearLayout.LayoutParams(-2, dp(14)))
        addView(space(6))
        addView(label("Shopee → Galeria → Retorno", 16f, BLACK, HydraTheme.fontMedium).apply {
            includeFontPadding = false; gravity = Gravity.CENTER_VERTICAL
        }, LinearLayout.LayoutParams(-2, dp(20)))
        addView(space(4))
        addView(label("Abre a Shopee, acessa a galeria e retorna sem publicar.", 13f, MUTED).apply {
            includeFontPadding = false; gravity = Gravity.CENTER_VERTICAL
            maxLines = 1; ellipsize = TextUtils.TruncateAt.END
        }, LinearLayout.LayoutParams(-2, dp(16)))
        addView(space(34))
        // Primário: 311x44, fundo #121212, rádio 10 (medido no PNG exportado do Figma).
        addView(TextView(context).apply {
            text = if (isRunning) "Autoteste em execução…" else "Iniciar autoteste"
            textSize = 14f
            typeface = HydraTheme.fontMedium
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            background = HydraTheme.ripple(context, BLACK, Color.argb(72, 255, 255, 255), 10)
            if (isRunning) { alpha = 0.6f } else { setOnClickListener { onStartTest() } }
        }, LinearLayout.LayoutParams(-1, dp(44)))
        addView(space(6))
        // Secundário: 311x36, branco com borda, rádio 10 (Figma).
        addView(TextView(context).apply {
            text = "Atualizar Resultado"
            textSize = 13f
            typeface = HydraTheme.fontMedium
            setTextColor(BLACK)
            gravity = Gravity.CENTER
            background = HydraTheme.rounded(context, Color.WHITE, 10, BORDER, 1)
            setOnClickListener { onRefresh() }
        }, LinearLayout.LayoutParams(-1, dp(36)))
    }.also { it.layoutParams = LinearLayout.LayoutParams(-1, -2) }

    /** Rótulo de seção "DIAGNÓSTICO" (11sp semibold, tracking amplo). */
    private fun sectionHeader(title: String): View = TextView(context).apply {
        text = title
        textSize = 11f
        typeface = HydraTheme.fontMedium
        setTextColor(MUTED)
        letterSpacing = 0.08f
        includeFontPadding = false
        gravity = Gravity.CENTER_VERTICAL
        layoutParams = LinearLayout.LayoutParams(-2, dp(14))
    }

    /**
     * Card DIAGNÓSTICO (343x104): estado vazio do Figma ou o último resultado,
     * com badge de status (Sucesso / Falha / Em progresso).
     */
    private fun diagnosticCard(result: TestResult?): View = LinearLayout(context).apply {
        orientation = LinearLayout.VERTICAL
        background = HydraTheme.rounded(context, Color.WHITE, 18, BORDER, 1)
        val padH = dp(16); setPadding(padH, dp(22), padH, dp(34))

        val titleRow = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        val title = result?.step ?: "Nenhum teste executado"
        titleRow.addView(label(title, 15f, BLACK, HydraTheme.fontMedium).apply {
            includeFontPadding = false
            gravity = Gravity.CENTER_VERTICAL
        }, LinearLayout.LayoutParams(0, dp(20), 1f))

        if (result != null) {
            val isSuccess = result.success == true
            val isFailure = result.success == false
            val badgeText = when {
                isSuccess -> "Sucesso"
                isFailure -> "Falha"
                else -> "Em progresso"
            }
            val badgeColor = when {
                isSuccess -> SUCCESS
                isFailure -> Color.rgb(192, 57, 43)
                else -> Color.rgb(230, 126, 34)
            }
            titleRow.addView(TextView(context).apply {
                text = badgeText
                textSize = 12f
                typeface = HydraTheme.fontMedium
                setTextColor(badgeColor)
                background = HydraTheme.pill(context, Color.argb(26, badgeColor shr 16 and 0xFF, badgeColor shr 8 and 0xFF, badgeColor and 0xFF))
                includeFontPadding = false
                val padB = dp(10)
                setPadding(padB, dp(4), padB, dp(4))
            }, LinearLayout.LayoutParams(-2, -2).apply { marginStart = dp(8) })
        }
        addView(titleRow, LinearLayout.LayoutParams(-1, -2))
        addView(space(12))
        val message = result?.message ?: "Inicie o teste para verificar a navegação."
        addView(label(message, 13f, MUTED).apply {
            includeFontPadding = false
            gravity = Gravity.CENTER_VERTICAL
        }, LinearLayout.LayoutParams(-2, -2))
    }.also { it.layoutParams = LinearLayout.LayoutParams(-1, -2) }

    private fun label(value: String, size: Float, color: Int, face: Typeface = HydraTheme.fontRegular) =
        TextView(context).apply { text = value; textSize = size; typeface = face; setTextColor(color) }

    private fun space(heightDp: Int): View = View(context).apply {
        layoutParams = LinearLayout.LayoutParams(1, dp(heightDp))
    }

    private fun dp(value: Int) = HydraTheme.dp(context, value)

    private fun oval(color: Int) = GradientDrawable().apply {
        shape = GradientDrawable.OVAL
        setColor(color)
    }

    private companion object {
        val BLACK = Color.rgb(18, 18, 18)
        val MUTED = Color.rgb(107, 107, 112)
        val BORDER = Color.rgb(232, 232, 232)
        val SUCCESS = Color.rgb(49, 111, 81)
    }
}
