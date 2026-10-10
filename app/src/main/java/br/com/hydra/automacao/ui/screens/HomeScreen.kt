package br.com.hydra.automacao.ui.screens

import android.content.Context
import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ObjectAnimator
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.View
import android.view.animation.DecelerateInterpolator
import android.widget.FrameLayout
import android.widget.HorizontalScrollView
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import br.com.hydra.automacao.R
import br.com.hydra.automacao.ui.components.buildIdentityHeader
import br.com.hydra.automacao.ui.theme.HydraTheme
import java.util.Calendar

/** Tela inicial com a composição visual exportada do Figma. */
class HomeScreen(
    private val context: Context,
    private val isAccessibilityReady: () -> Boolean,
    private val isShopeeInstalled: () -> Boolean,
    private val onNavigateToTest: () -> Unit,
    private val onNavigateToProduction: () -> Unit,
    private val onNavigateToPermissions: () -> Unit
) {
    private val pagerDots = mutableListOf<View>()

    fun buildView(): View {
        val content = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            clipChildren = false
            addView(buildIdentityHeader(context)); addView(space(26))
            addView(label(greeting(), 28f, BLACK, HydraTheme.fontMedium)); addView(space(8))
            addView(label("Automatize sua operação na Shopee com segurança.", 14f, MUTED)); addView(space(38))
            addView(slider()); addView(space(12)); addView(pager()); addView(space(18))
            addView(label("VISÃO GERAL", 11f, MUTED, HydraTheme.fontMedium).apply { letterSpacing = .05f }); addView(space(10))
            addView(salesCard()); addView(space(20)); addView(dailyCard()); addView(space(16))
            addView(metricRow()); addView(space(16)); addView(lastResult()); addView(space(20))
        }
        return FrameLayout(context).apply {
            setBackgroundColor(Color.WHITE)
            clipChildren = false
            // Nó "Decor / Meia-lua superior": 255x261, x=192, y=-131, #DADADA a 27%.
            addView(View(context).apply { background = oval(Color.argb(69, 218, 218, 218)) }, FrameLayout.LayoutParams(dp(255), dp(261)).apply {
                leftMargin = dp(192); topMargin = -dp(141)
            })
            addView(content, FrameLayout.LayoutParams(-1, -2))
        }
    }

    /** Faixa sem fim: um banner completo e uma prévia fixa do próximo. */
    private fun slider() = HorizontalScrollView(context).apply {
        isHorizontalScrollBarEnabled = false; overScrollMode = View.OVER_SCROLL_NEVER; clipToPadding = false
        val strip = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            // Mantém o slide ativo 8dp afastado da borda, sem reduzir a janela do carrossel.
            setPadding(dp(8), 0, 0, 0)
        }
        repeat(4) { index ->
            val first = index % 2 == 0
            val card = hero(
                if (first) "Sua rotina de\nafiliado Shopee" else "Publique com\nconsistência",
                if (first) "Automatize vídeos. Venda mais." else "Automação para vender todos os dias.",
                if (first) R.drawable.hero_shopee_routine else R.drawable.hero_shopee_consistency
            )
            strip.addView(card, LinearLayout.LayoutParams(dp(302), dp(118)))
            if (index < 3) strip.addView(View(context), LinearLayout.LayoutParams(dp(8), 1))
        }
        addView(strip, FrameLayout.LayoutParams(-2, dp(118)))
        var offset = 0
        var step = 0
        var page = 0
        var animator: ObjectAnimator? = null
        val pulse = object : Runnable {
            override fun run() {
                val target = offset + step
                page = 1 - page
                updatePager(page)
                animator?.cancel()
                animator = ObjectAnimator.ofInt(this@apply, "scrollX", target).apply {
                    duration = 820
                    interpolator = DecelerateInterpolator()
                    if (target >= step * 2) {
                        addListener(object : AnimatorListenerAdapter() {
                            override fun onAnimationEnd(animation: Animator) {
                                // O terceiro card repete o primeiro; reposicionamento é invisível.
                                scrollTo(0, 0)
                            }
                        })
                        offset = 0
                    } else {
                        offset = target
                    }
                    start()
                }
                postDelayed(this, 4_500)
            }
        }
        addOnAttachStateChangeListener(object : View.OnAttachStateChangeListener {
            override fun onViewAttachedToWindow(view: View) {
                post {
                    // Sai 16dp à esquerda e preserva um respiro de 8dp à direita.
                    val expandedWidth = width + dp(24)
                    layoutParams = (layoutParams as LinearLayout.LayoutParams).apply { width = expandedWidth }
                    translationX = -dp(16).toFloat()
                    // Dimensões do Figma: card 302dp e espaço de 8dp entre os slides.
                    // Há quatro cards em sequência (1, 2, 1, 2), portanto nunca há fim visível.
                    step = dp(310)
                    requestLayout()
                    postDelayed(pulse, 3_000)
                }
            }
            override fun onViewDetachedFromWindow(view: View) { removeCallbacks(pulse); animator?.cancel() }
        })
    }.also { it.layoutParams = LinearLayout.LayoutParams(-1, dp(118)) }

    private fun hero(title: String, subtitle: String, imageRes: Int) = FrameLayout(context).apply {
        background = rounded(BLACK, 16); clipToOutline = true; isClickable = true; setOnClickListener { onNavigateToProduction() }
        addView(ImageView(context).apply { setImageResource(imageRes); scaleType = ImageView.ScaleType.CENTER_CROP }, FrameLayout.LayoutParams(-1, -1))
        addView(View(context).apply { background = heroOverlay() }, FrameLayout.LayoutParams(-1, -1))
        addView(LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL; setPadding(dp(12), dp(22), 0, 0)
            addView(label(title, 20f, Color.WHITE, HydraTheme.fontMedium)); addView(space(4)); addView(label(subtitle, 12f, Color.WHITE))
        }, FrameLayout.LayoutParams(-1, -1))
    }

    private fun pager() = LinearLayout(context).apply {
        gravity = Gravity.CENTER
        repeat(2) { index ->
            val item = dot(if (index == 0) 17 else 8, if (index == 0) BLACK else Color.rgb(217, 217, 217))
            pagerDots += item; addView(item)
        }
    }

    private fun updatePager(active: Int) {
        pagerDots.forEachIndexed { index, dot ->
            val selected = index == active
            dot.background = if (selected) rounded(BLACK, 80) else circle(Color.rgb(217, 217, 217))
            dot.layoutParams = (dot.layoutParams as LinearLayout.LayoutParams).apply { width = dp(if (selected) 17 else 8) }
            dot.animate().scaleX(if (selected) 1f else .85f).scaleY(if (selected) 1f else .85f).setDuration(220).start()
        }
    }

    private fun salesCard() = LinearLayout(context).apply {
        orientation = LinearLayout.VERTICAL; setPadding(dp(18), dp(16), dp(18), dp(16)); background = darkGradient(16)
        val heading = LinearLayout(context).apply { gravity = Gravity.CENTER_VERTICAL }
        heading.addView(salesIcon(), LinearLayout.LayoutParams(dp(16), dp(17)))
        heading.addView(label("Suas vendas na Shopee", 13f, Color.WHITE, HydraTheme.fontMedium).apply { setPadding(dp(8), 0, 0, 0) }, LinearLayout.LayoutParams(0, -2, 1f))
        heading.addView(label("Ver detalhes  ›", 10f, Color.WHITE)); addView(heading); addView(space(13))
        addView(LinearLayout(context).apply {
            gravity = Gravity.CENTER_VERTICAL
            addView(salesMetric("Cliques", "12,5 mil"), LinearLayout.LayoutParams(0, -2, 1f))
            addView(salesMetric("Conversões", "R$ 802,40"), LinearLayout.LayoutParams(0, -2, 1.15f))
            addView(salesMetric("Pedidos", "153"), LinearLayout.LayoutParams(0, -2, .72f))
        })
    }.also { it.layoutParams = LinearLayout.LayoutParams(-1, dp(112)) }

    private fun salesIcon() = FrameLayout(context).apply {
        addView(ImageView(context).apply { setImageResource(R.drawable.ic_sales_bar_short) }, FrameLayout.LayoutParams(dp(3), dp(6)).apply { leftMargin = dp(2); topMargin = dp(10) })
        addView(ImageView(context).apply { setImageResource(R.drawable.ic_sales_bar_medium) }, FrameLayout.LayoutParams(dp(3), dp(9)).apply { leftMargin = dp(7); topMargin = dp(6) })
        addView(ImageView(context).apply { setImageResource(R.drawable.ic_sales_bar_tall) }, FrameLayout.LayoutParams(dp(3), dp(13)).apply { leftMargin = dp(12); topMargin = dp(2) })
    }

    private fun salesMetric(name: String, value: String) = LinearLayout(context).apply { orientation = LinearLayout.VERTICAL; addView(label(name, 10f, Color.WHITE)); addView(space(2)); addView(label(value, 15f, Color.WHITE, HydraTheme.fontMedium)) }

    private fun dailyCard() = FrameLayout(context).apply {
        background = rounded(Color.WHITE, 18, Color.rgb(232, 232, 232))
        addView(LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL; setPadding(dp(18), dp(17), dp(18), dp(14))
            addView(label("VÍDEOS PUBLICADOS HOJE", 11f, MUTED, HydraTheme.fontMedium))
            addView(LinearLayout(context).apply { gravity = Gravity.BOTTOM; addView(label("3", 32f, BLACK, Typeface.create("sans-serif", Typeface.BOLD))); addView(label("de 10", 13f, MUTED).apply { setPadding(dp(6), 0, 0, dp(5)) }) })
            addView(space(12)); addView(progress()); addView(space(9))
            addView(LinearLayout(context).apply { gravity = Gravity.CENTER_VERTICAL; addView(label("30% do limite diário", 12f, MUTED), LinearLayout.LayoutParams(0, -2, 1f)); addView(label("Reseta em 23h 26min", 11f, MUTED)) })
        }, FrameLayout.LayoutParams(-1, -1))
        addView(upgradeButton(), FrameLayout.LayoutParams(dp(114), dp(22), Gravity.TOP or Gravity.END).apply { topMargin = dp(16); marginEnd = dp(7) })
    }.also { it.layoutParams = LinearLayout.LayoutParams(-1, dp(142)) }

    private fun upgradeButton() = LinearLayout(context).apply {
        background = greenGradient(12)
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER
        // Coroa + barra em uma coluna, para que a barra acompanhe a coroa.
        val crown = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            addView(ImageView(context).apply { setImageResource(R.drawable.ic_upgrade_crown_vector) }, LinearLayout.LayoutParams(dp(16), dp(11)))
            // No Figma esta camada é uma barra sólida 7x1 ao lado de uma coroa 9x6 — parte da coroa, escala junto.
            addView(View(context).apply { setBackgroundColor(Color.rgb(185, 220, 194)) }, LinearLayout.LayoutParams(dp(12), dp(2)).apply { topMargin = dp(2) })
        }
        addView(crown, LinearLayout.LayoutParams(-2, -2))
        addView(label("FAZER UPGRADE", 10.5f, Color.WHITE, HydraTheme.fontMedium), LinearLayout.LayoutParams(-2, -2).apply { marginStart = dp(6) })
    }

    private fun metricRow() = LinearLayout(context).apply {
        orientation = LinearLayout.HORIZONTAL
        addView(metric("TOTAL PUBLICADOS", "128", "Desde o início"), LinearLayout.LayoutParams(0, dp(104), 1f).apply { marginEnd = dp(8) })
        addView(metric("TAXA DE SUCESSO", "96%", "Últimos 30 dias"), LinearLayout.LayoutParams(0, dp(104), 1f).apply { marginStart = dp(8) })
    }
    private fun metric(title: String, value: String, sub: String) = LinearLayout(context).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(16), dp(16), dp(12), dp(10)); background = rounded(Color.WHITE, 18, Color.rgb(232,232,232)); addView(label(title,10f,MUTED,HydraTheme.fontMedium)); addView(space(8)); addView(label(value,28f,BLACK,Typeface.create("sans-serif",Typeface.BOLD))); addView(space(4)); addView(label(sub,11f,MUTED)) }
    // Card / Último resultado (Figma 4054:402): ponto de sucesso, preview 90x60 com
    // play centralizado e coluna de textos (título, horário e ação alinhada à direita).
    private fun lastResult() = LinearLayout(context).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(dp(18), dp(17), dp(18), dp(14))
        background = rounded(Color.WHITE, 18, Color.rgb(232,232,232))
        addView(LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            addView(View(context).apply { background = circle(Color.rgb(49,111,81)) }, LinearLayout.LayoutParams(dp(8), dp(8)))
            addView(label("ÚLTIMO RESULTADO", 11f, MUTED, HydraTheme.fontMedium).apply { includeFontPadding = false; gravity = Gravity.CENTER_VERTICAL }, LinearLayout.LayoutParams(-2, dp(14)).apply { marginStart = dp(8) })
        }, LinearLayout.LayoutParams(-2, dp(14)))
        addView(space(11))
        addView(LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            addView(FrameLayout(context).apply {
                background = rounded(BLACK, 12); clipToOutline = true
                addView(ImageView(context).apply { setImageResource(R.drawable.img_preview_ultimo_resultado); scaleType = ImageView.ScaleType.CENTER_CROP }, FrameLayout.LayoutParams(-1, -1))
                addView(ImageView(context).apply { setImageResource(R.drawable.ic_play_preview) }, FrameLayout.LayoutParams(dp(24), dp(24), Gravity.CENTER))
            }, LinearLayout.LayoutParams(dp(90), dp(60)))
            addView(LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                addView(label("Publicado com sucesso", 16f, BLACK, HydraTheme.fontMedium).apply { includeFontPadding = false; gravity = Gravity.CENTER_VERTICAL }, LinearLayout.LayoutParams(-2, dp(20)))
                addView(space(5))
                addView(label("Hoje, 21:42", 12f, MUTED).apply { includeFontPadding = false; gravity = Gravity.CENTER_VERTICAL }, LinearLayout.LayoutParams(-2, dp(16)))
                addView(label("Ver detalhes  →", 13f, BLACK, HydraTheme.fontMedium).apply { includeFontPadding = false; gravity = Gravity.CENTER_VERTICAL }, LinearLayout.LayoutParams(-2, dp(18)).apply { gravity = Gravity.END })
            }, LinearLayout.LayoutParams(0, -2, 1f).apply { marginStart = dp(16); topMargin = dp(3) })
        }, LinearLayout.LayoutParams(-1, -2))
    }.also { it.layoutParams=LinearLayout.LayoutParams(-1,dp(118)) }
    private fun progress() = FrameLayout(context).apply { background=rounded(Color.rgb(238,238,238),4); addView(View(context).apply { background=greenGradient(4) },FrameLayout.LayoutParams(dp(92),dp(8))) }.also { it.layoutParams=LinearLayout.LayoutParams(-1,dp(8)) }
    private fun dot(width: Int, color: Int) = View(context).apply { background=if(width>8) rounded(color,80) else circle(color); layoutParams=LinearLayout.LayoutParams(dp(width),dp(8)).apply { marginEnd=dp(4) } }
    private fun label(value:String,size:Float,color:Int,face:Typeface=HydraTheme.fontRegular)=TextView(context).apply{text=value;textSize=size;typeface=face;setTextColor(color)}
    private fun space(height:Int)=View(context).apply{layoutParams=LinearLayout.LayoutParams(1,dp(height))}
    private fun dp(value:Int)=HydraTheme.dp(context,value)
    private fun rounded(color:Int,radius:Int,stroke:Int?=null)=HydraTheme.rounded(context,color,radius,stroke)
    private fun circle(color:Int,stroke:Int?=null)=rounded(color,100,stroke)
    private fun oval(color: Int) = GradientDrawable().apply {
        shape = GradientDrawable.OVAL
        setColor(color)
    }
    // Gradiente diagonal compartilhado (Card / Suas vendas, Card / Último resultado,
    // Badge / Plano Elite). Implementação centralizada em HydraTheme.darkGradient.
    private fun darkGradient(radius: Int) = HydraTheme.darkGradient(context, radius)
    private fun greenGradient(radius:Int)=GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT,intArrayOf(Color.rgb(41,92,64),Color.rgb(71,133,97),Color.rgb(97,158,120),Color.rgb(64,120,87),Color.rgb(31,74,51))).apply{cornerRadius=dp(radius).toFloat()}
    private fun heroOverlay()=GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT,intArrayOf(0xD91B1B1B.toInt(),0x9C1B1B1B.toInt(),0x151B1B1B)).apply{cornerRadius=dp(16).toFloat()}
    private fun greeting()=when(Calendar.getInstance().get(Calendar.HOUR_OF_DAY)){in 5..11->"Bom dia.";in 12..17->"Boa tarde.";else->"Boa noite."}
    private companion object { val BLACK=Color.rgb(18,18,18); val MUTED=Color.rgb(107,107,112) }
}
