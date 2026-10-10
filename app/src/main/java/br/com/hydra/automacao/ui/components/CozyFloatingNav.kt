package br.com.hydra.automacao.ui.components

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import br.com.hydra.automacao.R
import br.com.hydra.automacao.ui.theme.HydraTheme

/**
 * Menu de navegação flutuante moderno ("Floating Action Dock").
 * Ícones profissionais, feedback tátil suave e indicadores sutis de atividade.
 */
class CozyFloatingNav(
    context: Context,
    private val onTabSelected: (NavTab) -> Unit
) : FrameLayout(context) {

    enum class NavTab(val title: String, val iconRes: Int) {
        HOME("Início", R.drawable.fig_nav_home),
        TEST("Histórico", R.drawable.fig_nav_history),
        PRODUCTION("Teste", R.drawable.fig_nav_test),
        PERMISSIONS("Permissões", R.drawable.fig_nav_permissions),
        SETTINGS("Perfil", R.drawable.fig_nav_profile)
    }

    private var currentTab: NavTab = NavTab.HOME
    private var homeAppearance = false
    private val tabViews = HashMap<NavTab, TabViewHolder>()
    private val container: LinearLayout

    private class TabViewHolder(
        val root: LinearLayout,
        val icon: ImageView,
        val label: TextView,
        val spacer: View,
        val pulseDot: CozyPulseDot
    )

    init {
        val marginH = HydraTheme.dp(context, 14)
        val marginB = HydraTheme.dp(context, 12)
        val padV = HydraTheme.dp(context, 6)
        val padH = HydraTheme.dp(context, 6)

        layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT).apply {
            setMargins(marginH, 0, marginH, marginB)
            gravity = Gravity.BOTTOM
        }

        // Fundo em pílula flutuante com borda sutil e sombra confortável
        background = HydraTheme.rounded(
            context,
            bgColor = HydraTheme.surface,
            radiusDp = 24,
            strokeColor = HydraTheme.borderSubtle,
            strokeWidthDp = 1
        )
        elevation = HydraTheme.dp(context, 6).toFloat()

        container = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            clipChildren = false
            clipToPadding = false
            setPadding(padH, padV, padH, padV)
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
        }

        NavTab.entries.forEach { tab ->
            val tabView = createTabView(tab)
            container.addView(tabView.root, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
            tabViews[tab] = tabView
        }

        addView(container)
        updateSelection(NavTab.HOME, animate = false)
    }

    private fun createTabView(tab: NavTab): TabViewHolder {
        val root = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            clipChildren = false
            clipToPadding = false
            val padV = HydraTheme.dp(context, 7)
            val padH = HydraTheme.dp(context, 4)
            setPadding(padH, padV, padH, padV)
            isClickable = true
            isFocusable = true
        }

        val iconContainer = FrameLayout(context).apply {
            // Frame de ícone selecionado no Figma: 24 x 24dp.
            layoutParams = LinearLayout.LayoutParams(HydraTheme.dp(context, 24), HydraTheme.dp(context, 24))
        }

        val icon = ImageView(context).apply {
            setImageResource(tab.iconRes)
            setColorFilter(HydraTheme.textMuted)
            layoutParams = FrameLayout.LayoutParams(HydraTheme.dp(context, 24), HydraTheme.dp(context, 24), Gravity.CENTER)
        }
        iconContainer.addView(icon)

        // Ponto de pulso de atividade sobreposto (ex.: teste ou produção em andamento)
        val pulse = CozyPulseDot(context, dotColor = HydraTheme.primary).apply {
            val dotSize = HydraTheme.dp(context, 8)
            layoutParams = FrameLayout.LayoutParams(dotSize, dotSize, Gravity.TOP or Gravity.END)
            visibility = View.GONE
        }
        iconContainer.addView(pulse)

        root.addView(iconContainer)

        val spacer = View(context).apply {
            layoutParams = LinearLayout.LayoutParams(1, HydraTheme.dp(context, 3))
        }
        root.addView(spacer)

        val label = TextView(context).apply {
            text = tab.title
            textSize = 11f
            typeface = HydraTheme.fontMedium
            setTextColor(HydraTheme.textMuted)
            gravity = Gravity.CENTER
            includeFontPadding = false
            minHeight = HydraTheme.dp(context, 18)
            letterSpacing = 0.005f
            setSingleLine(true)
            setHorizontallyScrolling(true)
            // O pai do rótulo tem 43dp (modo home) ou ~56dp de conteúdo, e limita a
            // medida de um filho WRAP_CONTENT a esse valor — cortando o texto. Por
            // isso medimos o texto sem limite e travamos a caixa no tamanho exato:
            // largura explícita ignora a restrição do pai e o final nunca corta.
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                HydraTheme.dp(context, 18)
            )
        }
        label.measure(
            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
            View.MeasureSpec.makeMeasureSpec(HydraTheme.dp(context, 18), View.MeasureSpec.EXACTLY)
        )
        label.layoutParams = LinearLayout.LayoutParams(
            label.measuredWidth + HydraTheme.dp(context, 2),
            HydraTheme.dp(context, 18)
        )
        root.addView(label)

        root.setOnClickListener {
            // Micro animação tátil de toque
            root.animate().scaleX(0.92f).scaleY(0.92f).setDuration(70).withEndAction {
                root.animate().scaleX(1f).scaleY(1f).setDuration(120).start()
            }.start()

            selectTab(tab)
        }

        return TabViewHolder(root, icon, label, spacer, pulse)
    }

    fun selectTab(tab: NavTab) {
        if (currentTab != tab) {
            currentTab = tab
            updateSelection(tab, animate = true)
            onTabSelected(tab)
        }
    }

    fun setTabBadge(tab: NavTab, active: Boolean) {
        tabViews[tab]?.pulseDot?.visibility = if (active) View.VISIBLE else View.GONE
    }

    /** Aplica a barra preta, sem o cartao flutuante, do layout da tela Inicio. */
    fun setHomeAppearance(enabled: Boolean) {
        if (homeAppearance == enabled) return
        homeAppearance = enabled
        val pad = HydraTheme.dp(context, if (enabled) 24 else 6)
        container.setPadding(pad, if (enabled) 0 else HydraTheme.dp(context, 6), pad, if (enabled) 0 else HydraTheme.dp(context, 6))
        container.gravity = Gravity.CENTER_VERTICAL
        if (enabled) {
            background = homeNavBackground()
            elevation = 0f
            (layoutParams as LayoutParams).apply {
                height = HydraTheme.dp(context, 98)
                setMargins(0, 0, 0, 0)
            }
            (container.layoutParams as LayoutParams).apply {
                height = HydraTheme.dp(context, 49)
                gravity = Gravity.TOP
                topMargin = HydraTheme.dp(context, 15)
            }
        } else {
            background = HydraTheme.rounded(context, HydraTheme.surface, 24, HydraTheme.borderSubtle, 1)
            elevation = HydraTheme.dp(context, 6).toFloat()
            (layoutParams as LayoutParams).apply {
                height = LayoutParams.WRAP_CONTENT
                setMargins(HydraTheme.dp(context, 14), 0, HydraTheme.dp(context, 14), HydraTheme.dp(context, 12))
            }
            (container.layoutParams as LayoutParams).apply {
                height = LayoutParams.WRAP_CONTENT
                gravity = Gravity.NO_GRAVITY
                topMargin = 0
            }
        }
        NavTab.entries.forEachIndexed { index, tab ->
            val root = tabViews[tab]?.root ?: return@forEachIndexed
            root.setPadding(if (enabled) 0 else HydraTheme.dp(context, 4), if (enabled) 0 else HydraTheme.dp(context, 7), if (enabled) 0 else HydraTheme.dp(context, 4), if (enabled) 0 else HydraTheme.dp(context, 7))
            root.layoutParams = LinearLayout.LayoutParams(
                if (enabled) HydraTheme.dp(context, 43) else 0,
                if (enabled) HydraTheme.dp(context, 44) else LinearLayout.LayoutParams.WRAP_CONTENT,
                if (enabled) 0f else 1f
            ).apply { if (enabled && index < NavTab.entries.size - 1) marginEnd = HydraTheme.dp(context, 28) }
            // ícone 24dp + espaço 2dp + rótulo, como o frame selecionado no Figma.
            (root.getChildAt(1).layoutParams as LinearLayout.LayoutParams).height = HydraTheme.dp(context, if (enabled && tab == currentTab) 2 else if (enabled) 4 else 3)
        }
        if (enabled) applyHomeItemSpacing()
        requestLayout()
        updateSelection(currentTab, animate = false)
    }

    /**
     * O layout do Figma usa 24dp laterais, itens de 43dp e ocupa toda a largura
     * de 375dp. Em aparelhos mais largos, recalculamos apenas o vão entre itens.
     */
    private fun applyHomeItemSpacing() {
        container.post {
            val itemWidth = HydraTheme.dp(context, 43)
            val usableWidth = container.width - HydraTheme.dp(context, 48)
            val gap = ((usableWidth - itemWidth * NavTab.entries.size) / (NavTab.entries.size - 1)).coerceAtLeast(0)
            NavTab.entries.forEachIndexed { index, tab ->
                val root = tabViews[tab]?.root ?: return@forEachIndexed
                root.layoutParams = (root.layoutParams as LinearLayout.LayoutParams).apply {
                    marginEnd = if (index < NavTab.entries.size - 1) gap else 0
                }
            }
            container.requestLayout()
        }
    }

    private fun homeNavBackground(): GradientDrawable = GradientDrawable().apply {
        setColor(Color.rgb(18, 18, 18))
        val radius = HydraTheme.dp(context, 24).toFloat()
        cornerRadii = floatArrayOf(radius, radius, radius, radius, 0f, 0f, 0f, 0f)
    }

    private fun updateSelection(selectedTab: NavTab, animate: Boolean) {
        NavTab.entries.forEach { tab ->
            val holder = tabViews[tab] ?: return@forEach
            val isSelected = tab == selectedTab

            if (isSelected) {
                holder.root.background = if (homeAppearance) null else HydraTheme.rounded(
                    context, bgColor = HydraTheme.primaryLight, radiusDp = 18
                )
                holder.icon.setColorFilter(if (homeAppearance) android.graphics.Color.WHITE else HydraTheme.primary)
                holder.label.setTextColor(if (homeAppearance) android.graphics.Color.WHITE else HydraTheme.primary)
                holder.label.typeface = HydraTheme.fontMedium
            } else {
                holder.root.background = null
                holder.icon.setColorFilter(if (homeAppearance) android.graphics.Color.rgb(168, 172, 175) else HydraTheme.textMuted)
                holder.label.setTextColor(if (homeAppearance) android.graphics.Color.rgb(168, 172, 175) else HydraTheme.textMuted)
                holder.label.typeface = if (homeAppearance) HydraTheme.fontMedium else HydraTheme.fontRegular
            }
            if (homeAppearance) {
                holder.root.layoutParams = (holder.root.layoutParams as LinearLayout.LayoutParams).apply {
                    height = HydraTheme.dp(context, if (isSelected) 44 else 46)
                }
                (holder.spacer.layoutParams as LinearLayout.LayoutParams).height = HydraTheme.dp(context, if (isSelected) 2 else 4)
            }
        }
    }
}
