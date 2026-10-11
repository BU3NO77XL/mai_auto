package br.com.hydra.automacao.ui.components

import android.content.Context
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import br.com.hydra.automacao.ui.theme.HydraTheme

/**
 * Campo de texto com label superior, feedback de foco e contador opcional.
 */
class CozyInput(
    context: Context,
    val labelText: String? = null,
    val hintText: String = "",
    val initialValue: String = "",
    val isMultiline: Boolean = false,
    val minLinesCount: Int = 1,
    val maxLinesCount: Int = 1,
    val isMono: Boolean = false,
    val onTextChanged: ((String) -> Unit)? = null
) : LinearLayout(context) {

    val editText: EditText
    private val counterView: TextView?

    init {
        orientation = VERTICAL
        layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)

        if (!labelText.isNullOrBlank()) {
            val labelRow = LinearLayout(context).apply {
                orientation = HORIZONTAL
                layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
            }
            val title = TextView(context).apply {
                text = labelText
                textSize = 13f
                typeface = HydraTheme.fontMedium
                setTextColor(HydraTheme.textPrimary)
                layoutParams = LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f)
            }
            labelRow.addView(title)

            if (isMultiline) {
                counterView = TextView(context).apply {
                    textSize = 12f
                    typeface = HydraTheme.fontRegular
                    setTextColor(HydraTheme.textMuted)
                }
                labelRow.addView(counterView)
            } else {
                counterView = null
            }

            addView(labelRow)
            val spacer = View(context).apply {
                layoutParams = LayoutParams(1, HydraTheme.dp(context, 6))
            }
            addView(spacer)
        } else {
            counterView = null
        }

        editText = EditText(context).apply {
            setText(initialValue)
            hint = hintText
            textSize = 14.5f
            setTextColor(HydraTheme.textPrimary)
            setHintTextColor(HydraTheme.textMuted)
            typeface = if (isMono) HydraTheme.fontMono else HydraTheme.fontRegular

            if (isMultiline) {
                isSingleLine = false
                minLines = minLinesCount
                maxLines = maxLinesCount
                setHorizontallyScrolling(false)
            } else {
                isSingleLine = true
            }

            val padH = HydraTheme.dp(context, 14)
            val padV = HydraTheme.dp(context, 12)
            setPadding(padH, padV, padH, padV)

            // Background inicial inativo
            background = HydraTheme.rounded(
                context,
                bgColor = HydraTheme.surfaceSubtle,
                radiusDp = 12,
                strokeColor = HydraTheme.borderMedium,
                strokeWidthDp = 1
            )

            setOnFocusChangeListener { _, hasFocus ->
                background = HydraTheme.rounded(
                    context,
                    bgColor = if (hasFocus) HydraTheme.surface else HydraTheme.surfaceSubtle,
                    radiusDp = 12,
                    strokeColor = if (hasFocus) HydraTheme.borderFocused else HydraTheme.borderMedium,
                    strokeWidthDp = if (hasFocus) 1 else 1
                )
            }

            addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                    val str = s?.toString().orEmpty()
                    updateCounter(str)
                    onTextChanged?.invoke(str)
                }
                override fun afterTextChanged(s: Editable?) {}
            })
        }

        updateCounter(initialValue)
        addView(editText)
    }

    private fun updateCounter(text: String) {
        if (counterView != null) {
            val lines = text.lines().count { it.isNotBlank() }
            counterView.text = if (lines <= 1) "${text.length} caracteres" else "$lines linhas · ${text.length} carac."
        }
    }

    fun getText(): String = editText.text.toString().trim()
}
