package br.com.hydra.automacao.ui.screens

import android.content.Context
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import br.com.hydra.automacao.R
import br.com.hydra.automacao.automation.ProductionFlowService
import br.com.hydra.automacao.automation.TestResultStore
import br.com.hydra.automacao.ui.components.CozyButton
import br.com.hydra.automacao.ui.components.CozyCard
import br.com.hydra.automacao.ui.components.CozyInput
import br.com.hydra.automacao.ui.components.CozyLoadingSpinner
import br.com.hydra.automacao.ui.components.CozyProgressBar
import br.com.hydra.automacao.ui.components.CozySegmentedControl
import br.com.hydra.automacao.ui.theme.HydraTheme
import java.text.DateFormat
import java.util.Date

/**
 * Tela de Produção minimalista e intuitiva para execução do fluxo completo.
 */
class ProductionScreen(
    private val context: Context,
    private val onStartProduction: (caption: String, products: List<String>, isDraft: Boolean, useAiLabel: Boolean) -> Unit,
    private val onRefresh: () -> Unit
) {

    private var selectedDraft = false
    private var selectedAiLabel = false

    fun buildView(): View {
        val root = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
        }

        val isRunning = ProductionFlowService.isRunning
        val result = TestResultStore(context).load()

        // ── Título da Página ──
        val titleView = TextView(context).apply {
            text = "Produção"
            textSize = 28f
            typeface = HydraTheme.fontMedium
            setTextColor(HydraTheme.textPrimary)
            letterSpacing = -0.01f
        }
        root.addView(titleView)

        val subtitleView = TextView(context).apply {
            text = "Execução autônoma de vídeo, legenda, produtos e publicação."
            textSize = 14f
            typeface = HydraTheme.fontRegular
            setTextColor(HydraTheme.textSecondary)
        }
        root.addView(spacer(6))
        root.addView(subtitleView)
        root.addView(spacer(22))

        // ── Card de Formulário ──
        val formCard = CozyCard(context).apply {
            addHeader(
                tag = "Configuração do Envio",
                title = "Parâmetros do Vídeo",
                subtitle = "Preencha a legenda e a lista de links dos produtos a vincular.",
                iconRes = R.drawable.ic_nav_production
            )

            // Campo de Legenda
            val captionInput = CozyInput(
                context = context,
                labelText = "Texto da Legenda",
                hintText = "Ex.: Produto incrível com cupom de desconto na bio! ✨",
                isMultiline = true,
                minLinesCount = 3,
                maxLinesCount = 5
            )
            addView(captionInput)
            addSpacer(16)

            // Campo de Links
            val productsInput = CozyInput(
                context = context,
                labelText = "Links de Afiliado da Shopee (um por linha)",
                hintText = "https://shopee.com.br/product/...\nhttps://s.shopee.com.br/...",
                isMultiline = true,
                minLinesCount = 3,
                maxLinesCount = 8
            )
            addView(productsInput)
            addSpacer(18)

            // Seletor de Modo
            val modeLabel = TextView(context).apply {
                text = "Modo de Finalização"
                textSize = 13f
                typeface = HydraTheme.fontMedium
                setTextColor(HydraTheme.textPrimary)
            }
            addView(modeLabel)
            addSpacer(8)

            val modeSelector = CozySegmentedControl(
                context = context,
                options = listOf("Publicar agora", "Salvar em rascunhos"),
                defaultSelectedIndex = if (selectedDraft) 1 else 0
            ) { index ->
                selectedDraft = index == 1
            }
            addView(modeSelector)
            addSpacer(18)

            // Seletor de Rótulo IA
            val aiLabel = TextView(context).apply {
                text = "Rótulo de Conteúdo IA"
                textSize = 13f
                typeface = HydraTheme.fontMedium
                setTextColor(HydraTheme.textPrimary)
            }
            addView(aiLabel)
            addSpacer(4)

            val aiDesc = TextView(context).apply {
                text = "Indica se o vídeo contém elementos visuais ou áudio criados por IA."
                textSize = 12.5f
                typeface = HydraTheme.fontRegular
                setTextColor(HydraTheme.textMuted)
            }
            addView(aiDesc)
            addSpacer(8)

            val aiSelector = CozySegmentedControl(
                context = context,
                options = listOf("Desligado", "Ativado (IA)"),
                defaultSelectedIndex = if (selectedAiLabel) 1 else 0
            ) { index ->
                selectedAiLabel = index == 1
            }
            addView(aiSelector)
            addSpacer(22)

            // Botão ou Indicador de Execução
            if (isRunning) {
                val runningRow = LinearLayout(context).apply {
                    orientation = LinearLayout.HORIZONTAL
                    gravity = Gravity.CENTER_VERTICAL
                    val pad = HydraTheme.dp(context, 12)
                    setPadding(pad, pad, pad, pad)
                    background = HydraTheme.rounded(context, HydraTheme.primaryLight, radiusDp = 12)
                }

                val spinner = CozyLoadingSpinner(context, strokeColor = HydraTheme.primary).apply {
                    val size = HydraTheme.dp(context, 20)
                    layoutParams = LinearLayout.LayoutParams(size, size).also {
                        it.marginEnd = HydraTheme.dp(context, 12)
                    }
                }
                runningRow.addView(spinner)

                val runningText = TextView(context).apply {
                    text = "Fluxo de produção em andamento na Shopee…"
                    textSize = 13.5f
                    typeface = HydraTheme.fontMedium
                    setTextColor(HydraTheme.primary)
                }
                runningRow.addView(runningText)
                addView(runningRow)
                addSpacer(12)

                val progressBar = CozyProgressBar(context).apply {
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        HydraTheme.dp(context, 4)
                    )
                }
                addView(progressBar)
                addSpacer(14)

                addView(CozyButton(
                    context,
                    text = "Atualizar andamento",
                    style = CozyButton.Style.SECONDARY,
                    iconRes = R.drawable.ic_refresh,
                    onClick = onRefresh
                ))
            } else {
                addView(CozyButton(
                    context,
                    text = "Executar fluxo completo",
                    style = CozyButton.Style.PRIMARY,
                    iconRes = R.drawable.ic_nav_production,
                    onClick = {
                        val caption = captionInput.getText()
                        val links = productsInput.getText().lines()
                            .map { it.trim() }
                            .filter { it.startsWith("http") }
                        onStartProduction(caption, links, selectedDraft, selectedAiLabel)
                    }
                ))
            }
        }
        root.addView(formCard)
        root.addView(spacer(18))

        // ── Card de Resultado ──
        root.addView(sectionHeader("STATUS DA PRODUÇÃO"))

        val resultCard = CozyCard(context).apply {
            if (result == null) {
                addHeader(
                    title = "Nenhuma produção registrada",
                    subtitle = "Configure os campos acima e execute quando o vídeo estiver pronto."
                )
            } else {
                val isSuccess = result.success == true
                val isFailure = result.success == false

                val badgeColor = when {
                    isSuccess -> HydraTheme.success
                    isFailure -> HydraTheme.error
                    else -> HydraTheme.warning
                }

                val badgeBg = when {
                    isSuccess -> HydraTheme.successBg
                    isFailure -> HydraTheme.errorBg
                    else -> HydraTheme.warningBg
                }

                val badgeText = when {
                    isSuccess -> "Concluído"
                    isFailure -> "Falhou"
                    else -> "Em progresso"
                }

                val badgeView = TextView(context).apply {
                    text = badgeText
                    textSize = 12f
                    typeface = HydraTheme.fontMedium
                    setTextColor(badgeColor)
                    background = HydraTheme.pill(context, badgeBg)
                    val padH = HydraTheme.dp(context, 10)
                    val padV = HydraTheme.dp(context, 4)
                    setPadding(padH, padV, padH, padV)
                }

                addHeader(
                    tag = "Último Retorno",
                    title = result.step,
                    statusView = badgeView
                )

                val messageContainer = LinearLayout(context).apply {
                    orientation = LinearLayout.VERTICAL
                    val pad = HydraTheme.dp(context, 12)
                    setPadding(pad, pad, pad, pad)
                    background = HydraTheme.rounded(context, HydraTheme.surfaceSubtle, radiusDp = 12)
                }

                val messageView = TextView(context).apply {
                    text = result.message
                    textSize = 13.5f
                    typeface = HydraTheme.fontRegular
                    setTextColor(HydraTheme.textPrimary)
                    setLineSpacing(HydraTheme.dp(context, 2).toFloat(), 1.1f)
                }
                messageContainer.addView(messageView)
                addView(messageContainer)

                addSpacer(12)

                val dateView = TextView(context).apply {
                    val formatted = DateFormat.getDateTimeInstance().format(Date(result.timestamp))
                    text = "Registrado em $formatted"
                    textSize = 12f
                    typeface = HydraTheme.fontRegular
                    setTextColor(HydraTheme.textMuted)
                }
                addView(dateView)
            }
        }
        root.addView(resultCard)
        root.addView(spacer(24))

        return root
    }

    private fun sectionHeader(title: String): View = TextView(context).apply {
        text = title
        textSize = 11f
        typeface = HydraTheme.fontMedium
        setTextColor(HydraTheme.textMuted)
        letterSpacing = 0.12f
        val padV = HydraTheme.dp(context, 8)
        setPadding(0, padV, 0, padV)
    }

    private fun spacer(heightDp: Int): View = View(context).apply {
        layoutParams = LinearLayout.LayoutParams(1, HydraTheme.dp(context, heightDp))
    }
}
