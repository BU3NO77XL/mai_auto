package br.com.hydra.automacao.ui.screens

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.text.Editable
import android.text.TextWatcher
import android.view.Gravity
import android.view.View
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import br.com.hydra.automacao.R
import br.com.hydra.automacao.ui.components.buildIdentityHeader
import br.com.hydra.automacao.ui.theme.HydraTheme

/**
 * Tela de Histórico de vídeos (aba "Histórico"), fiel ao Figma "historico/".
 * Cabeçalho de identidade (chrome da Home), busca, chips de filtro e lista de cards.
 */
class HistoricoScreen(
    private val context: Context
) {

    private data class Video(
        val title: String,
        val time: String,
        val statusLabel: String,
        val statusColor: Int,
        val group: String
    )

    private val videos = listOf(
        Video("Look esportivo para Shopee", "Hoje, 16:34", "Publicado com sucesso", SUCCESS, "Concluídos"),
        Video("Rotina de skincare noturna", "Hoje, 14:02", "Publicado com sucesso", SUCCESS, "Concluídos"),
        Video("Unboxing do fone bluetooth", "Ontem, 19:48", "Falha na publicação", ERROR, "Com erro"),
        Video("Dicas de frete grátis", "Ontem, 11:20", "Em andamento", RUNNING, "Em andamento"),
        Video("Review do tênis de corrida", "12 mai, 09:15", "Publicado com sucesso", SUCCESS, "Concluídos")
    )

    private val filters = listOf("Todos", "Concluídos", "Com erro", "Em andamento")
    private var activeFilter = "Todos"
    private var searchQuery = ""
    private var listContainer: LinearLayout? = null
    private val chipViews = mutableMapOf<String, TextView>()

    fun buildView(): View {
        val content = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            clipChildren = false
            addView(buildIdentityHeader(context)); addView(space(26))
            addView(label("Histórico de vídeos", 28f, BLACK, HydraTheme.fontMedium)); addView(space(8))
            addView(label("Acompanhe e revise suas publicações.", 14f, MUTED)); addView(space(24))
            addView(searchField()); addView(space(20))
            addView(filterRow()); addView(space(20))
            val list = LinearLayout(context).apply { orientation = LinearLayout.VERTICAL; clipChildren = false }
            listContainer = list
            addView(list)
        }
        renderList()
        return FrameLayout(context).apply {
            setBackgroundColor(Color.WHITE); clipChildren = false
            // Nó "Decor / Meia-lua superior" (mesmo chrome da Home).
            addView(View(context).apply { background = oval(Color.argb(69, 217, 217, 217)) }, FrameLayout.LayoutParams(dp(255), dp(261)).apply {
                leftMargin = dp(192); topMargin = -dp(141)
            })
            addView(content, FrameLayout.LayoutParams(-1, -2))
        }
    }

    /** Campo de busca: ícone de lupa + input transparente sobre pílula branca com borda. */
    private fun searchField(): View = LinearLayout(context).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        background = HydraTheme.rounded(context, Color.WHITE, 14, Color.rgb(232, 232, 232), 1)
        val padH = dp(14); setPadding(padH, 0, padH, 0)
        addView(ImageView(context).apply { setImageResource(R.drawable.ic_search) }, LinearLayout.LayoutParams(dp(18), dp(18)).apply { marginEnd = dp(8) })
        addView(EditText(context).apply {
            hint = "Buscar produto por legenda"
            textSize = 14f
            typeface = HydraTheme.fontRegular
            setTextColor(BLACK); setHintTextColor(MUTED)
            background = null; isSingleLine = true
            setPadding(0, 0, 0, 0)
            addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, st: Int, c: Int, a: Int) {}
                override fun onTextChanged(s: CharSequence?, st: Int, before: Int, count: Int) {}
                override fun afterTextChanged(s: Editable?) { searchQuery = s?.toString()?.trim().orEmpty(); renderList() }
            })
        }, LinearLayout.LayoutParams(0, -2, 1f))
    }.also { it.layoutParams = LinearLayout.LayoutParams(-1, dp(48)) }

    /** Chips de filtro: ativo em pílula escura, inativos em texto suave. */
    private fun filterRow(): View = LinearLayout(context).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        filters.forEachIndexed { index, name ->
            val chip = TextView(context).apply {
                text = name
                textSize = 13f
                typeface = HydraTheme.fontMedium
                gravity = Gravity.CENTER
                val padH = dp(14); val padV = dp(8)
                setPadding(padH, padV, padH, padV)
                isClickable = true; isFocusable = true
                setOnClickListener { activeFilter = name; syncChips(); renderList() }
            }
            chipViews[name] = chip
            addView(chip, LinearLayout.LayoutParams(-2, -2).apply { if (index > 0) marginStart = dp(4) })
        }
    }.also { syncChips(); it.layoutParams = LinearLayout.LayoutParams(-1, -2) }

    private fun syncChips() {
        chipViews.forEach { (name, chip) ->
            val active = name == activeFilter
            chip.background = if (active) HydraTheme.rounded(context, BLACK, 16) else null
            chip.setTextColor(if (active) Color.WHITE else MUTED)
        }
    }

    private fun renderList() {
        val list = listContainer ?: return
        list.removeAllViews()
        val filtered = videos.filter { video ->
            val matchesFilter = activeFilter == "Todos" || video.group == activeFilter
            val matchesSearch = searchQuery.isEmpty() || video.title.contains(searchQuery, ignoreCase = true)
            matchesFilter && matchesSearch
        }
        if (filtered.isEmpty()) {
            list.addView(label("Nenhum vídeo encontrado.", 13f, MUTED).apply { setPadding(0, dp(8), 0, dp(8)) })
            return
        }
        filtered.forEachIndexed { index, video ->
            list.addView(videoCard(video))
            if (index < filtered.size - 1) list.addView(space(12))
        }
    }

    private fun videoCard(video: Video): View = LinearLayout(context).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        background = HydraTheme.rounded(context, Color.WHITE, 18, Color.rgb(232, 232, 232), 1)
        val padV = dp(16); setPadding(dp(12), padV, dp(12), padV)
        addView(ImageView(context).apply {
            setImageResource(R.drawable.img_historico_thumb)
            scaleType = ImageView.ScaleType.CENTER_CROP
            background = HydraTheme.rounded(context, BLACK, 12); clipToOutline = true
        }, LinearLayout.LayoutParams(dp(88), dp(60)).apply { marginEnd = dp(16) })
        addView(LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            addView(label(video.title, 15f, BLACK, HydraTheme.fontMedium))
            addView(space(4))
            addView(label(video.time, 12f, MUTED))
            addView(space(4))
            addView(label(video.statusLabel, 12f, video.statusColor, HydraTheme.fontMedium))
        }, LinearLayout.LayoutParams(0, -2, 1f))
    }.also { it.layoutParams = LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(12) } }

    private fun label(value: String, size: Float, color: Int, face: Typeface = HydraTheme.fontRegular) =
        TextView(context).apply { text = value; textSize = size; typeface = face; setTextColor(color) }

    private fun space(height: Int) = View(context).apply { layoutParams = LinearLayout.LayoutParams(1, dp(height)) }
    private fun dp(value: Int) = HydraTheme.dp(context, value)
    private fun oval(color: Int) = GradientDrawable().apply {
        shape = GradientDrawable.OVAL; setColor(color)
    }

    private companion object {
        val BLACK = Color.rgb(18, 18, 18)
        val MUTED = Color.rgb(107, 107, 112)
        val SUCCESS = Color.rgb(49, 111, 81)      // #316F51
        val ERROR = Color.rgb(192, 57, 43)        // #C0392B
        val RUNNING = Color.rgb(230, 126, 34)     // #E67E22
    }
}
