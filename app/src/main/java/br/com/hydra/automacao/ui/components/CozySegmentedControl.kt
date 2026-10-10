package br.com.hydra.automacao.ui.components

import android.content.Context
import android.graphics.Color
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.TextView
import br.com.hydra.automacao.ui.theme.HydraTheme

/**
 * Seletor de opções moderno em cápsula ("Segmented Pill").
 */
class CozySegmentedControl(
    context: Context,
    private val options: List<String>,
    private val defaultSelectedIndex: Int = 0,
    private val onSelectionChanged: (Int) -> Unit
) : LinearLayout(context) {

    private var selectedIndex = defaultSelectedIndex
    private val itemViews = ArrayList<TextView>()

    init {
        orientation = HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        val pad = HydraTheme.dp(context, 4)
        setPadding(pad, pad, pad, pad)
        background = HydraTheme.pill(context, HydraTheme.surfaceSubtle, HydraTheme.borderSubtle, 1)

        options.forEachIndexed { index, title ->
            val textView = TextView(context).apply {
                text = title
                textSize = 13.5f
                typeface = HydraTheme.fontMedium
                gravity = Gravity.CENTER
                val padH = HydraTheme.dp(context, 16)
                val padV = HydraTheme.dp(context, 8)
                setPadding(padH, padV, padH, padV)
                setOnClickListener { select(index) }
            }
            itemViews.add(textView)
            addView(textView, LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f))
        }

        updateStates()
    }

    fun select(index: Int) {
        if (selectedIndex != index && index in options.indices) {
            selectedIndex = index
            updateStates()
            onSelectionChanged(index)
        }
    }

    fun getSelectedIndex(): Int = selectedIndex

    private fun updateStates() {
        itemViews.forEachIndexed { index, view ->
            val isSelected = index == selectedIndex
            if (isSelected) {
                view.setTextColor(Color.WHITE)
                view.background = HydraTheme.pill(context, HydraTheme.primary)
                view.elevation = HydraTheme.dp(context, 1).toFloat()
            } else {
                view.setTextColor(HydraTheme.textSecondary)
                view.background = null
                view.elevation = 0f
            }
        }
    }
}
