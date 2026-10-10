package br.com.hydra.automacao.ui.screens

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.ScrollView
import br.com.hydra.automacao.R
import br.com.hydra.automacao.ui.theme.HydraTheme

/** ApresentaÃ§Ã£o estÃ¡tica da tela Perfil â€” fiel ao Figma Ecarty Content 1:2737 + topbar 1:2891. */
class ProfileScreen(private val context: Context) {

    fun buildView(): View = LinearLayout(context).apply {
        orientation = LinearLayout.VERTICAL
        setBackgroundColor(Color.WHITE)

        addView(topBar())
        val scroll = ScrollView(context).apply {
            isVerticalScrollBarEnabled = false
            overScrollMode = View.OVER_SCROLL_NEVER
            clipToPadding = false
        }
        val content = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.WHITE)
            setPadding(dp(16), dp(0), dp(16), dp(0))
            addView(profileIdentity())
            addView(space(24))
            addView(section("Configurações da conta", listOf(
                "Informações pessoais" to ProfileIcon.PERSON,
                "Seus pedidos" to ProfileIcon.ORDER,
                "Lista de desejos" to ProfileIcon.HEART,
                "Senha e segurança" to ProfileIcon.LOCK,
                "Preferências de notificações" to ProfileIcon.BELL
            )))
            addView(space(24))
            addView(section("Outros", listOf(
                "Termos e condições" to ProfileIcon.CHAT,
                "Central de ajuda" to ProfileIcon.HELP,
                "Configurações" to ProfileIcon.GEAR,
                "Sair" to ProfileIcon.LOGOUT
            )))
            addView(space(24))
        }
        scroll.addView(content, FrameLayout.LayoutParams(-1, -2))
        addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))
    }

    /** Figma topbar 1:2891 â€” 343x44: botÃ£o voltar 44dp #EFEFEF + tÃ­tulo 18sp centralizado. */
    private fun topBar() = FrameLayout(context).apply {
        setBackgroundColor(Color.WHITE)
        setPadding(dp(16), dp(8), dp(16), dp(8))
        layoutParams = LinearLayout.LayoutParams(-1, dp(60))

        val back = FrameLayout(context).apply {
            background = HydraTheme.pill(context, BACK_BG)
            addView(ImageView(context).apply {
                setImageResource(R.drawable.ic_back_west)
                scaleType = ImageView.ScaleType.FIT_CENTER
            }, FrameLayout.LayoutParams(dp(24), dp(24), Gravity.CENTER))
        }
        addView(back, FrameLayout.LayoutParams(dp(44), dp(44), Gravity.START or Gravity.CENTER_VERTICAL))

        addView(text("Perfil", 18f, TEXT).apply { gravity = Gravity.CENTER },
            FrameLayout.LayoutParams(-1, -1, Gravity.CENTER))
    }

    private fun profileIdentity() = LinearLayout(context).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL

        val avatar = ImageView(context).apply {
            setImageResource(R.drawable.profile_avatar)
            scaleType = ImageView.ScaleType.CENTER_CROP
            clipToOutline = true
            outlineProvider = object : android.view.ViewOutlineProvider() {
                override fun getOutline(view: View, outline: android.graphics.Outline) {
                    outline.setOval(0, 0, view.width, view.height)
                }
            }
        }
        addView(avatar, LinearLayout.LayoutParams(dp(60), dp(60)))

        val identity = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_VERTICAL
            addView(text("Alex Robert", 16f, TEXT))
            addView(space(4))
            addView(text("example@gmail.com", 14f, SECONDARY))
        }
        addView(identity, LinearLayout.LayoutParams(0, dp(60), 1f).apply {
            marginStart = dp(12)
        })
    }

    private fun section(title: String, rows: List<Pair<String, ProfileIcon>>) =
        LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            addView(text(title, 16f, TEXT), LinearLayout.LayoutParams(-1, dp(24)))
            addView(space(16))

            val card = LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                background = HydraTheme.rounded(context, Color.WHITE, radiusDp = 12)
                // Figma Detail information 1:2747 / 1:2825 â€” padding vertical 12, horizontal 0.
                setPadding(dp(0), dp(12), dp(0), dp(12))
                rows.forEachIndexed { index, (label, icon) ->
                    addView(row(label, icon))
                    // Figma: gap 16 + Line #EFEFEF 1px + gap 16 entre linhas.
                    if (index < rows.lastIndex) {
                        addView(space(16))
                        addView(divider())
                        addView(space(16))
                    }
                }
            }
            addView(card, LinearLayout.LayoutParams(-1, -2))
        }

    private fun divider() = View(context).apply {
        setBackgroundColor(DIVIDER)
        layoutParams = LinearLayout.LayoutParams(-1, dp(1))
    }

    private fun row(label: String, icon: ProfileIcon) = LinearLayout(context).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL

        // Assets 1:1 do Figma (SVG 40x40 exportado via bridge, convertido p/ VectorDrawable).
        // Pill #F3F3F3 + glyph #8C8E98 stroke 1.5 jÃ¡ inclusos no drawable â€” sem desenho manual.
        val iconRes = when (icon) {
            ProfileIcon.PERSON -> R.drawable.ic_profile_person
            ProfileIcon.ORDER -> R.drawable.ic_profile_order
            ProfileIcon.HEART -> R.drawable.ic_profile_heart
            ProfileIcon.LOCK -> R.drawable.ic_profile_lock
            ProfileIcon.BELL -> R.drawable.ic_profile_bell
            ProfileIcon.CHAT -> R.drawable.ic_profile_chat
            ProfileIcon.HELP -> R.drawable.ic_profile_help
            ProfileIcon.GEAR -> R.drawable.ic_profile_settings
            ProfileIcon.LOGOUT -> R.drawable.ic_profile_logout
        }
        val iconView = ImageView(context).apply {
            setImageResource(iconRes)
            scaleType = ImageView.ScaleType.FIT_CENTER
            adjustViewBounds = false
        }
        addView(iconView, LinearLayout.LayoutParams(dp(40), dp(40)))

        addView(text(label, 14f, TEXT), LinearLayout.LayoutParams(0, -2, 1f).apply {
            marginStart = dp(12)
        })
        addView(Chevron(context), LinearLayout.LayoutParams(dp(24), dp(24)).apply {
            marginStart = dp(8)
        })
    }

    private fun text(value: String, size: Float, color: Int) = TextView(context).apply {
        text = value
        textSize = size
        typeface = HydraTheme.fontRegular
        setTextColor(color)
        includeFontPadding = false
    }

    private fun space(heightDp: Int) = View(context).apply {
        layoutParams = LinearLayout.LayoutParams(1, dp(heightDp))
    }

    private fun dp(value: Int) = HydraTheme.dp(context, value)

    private enum class ProfileIcon { PERSON, ORDER, HEART, LOCK, BELL, CHAT, HELP, GEAR, LOGOUT }

    private class Chevron(context: Context) : View(context) {
        private val density = resources.displayMetrics.density
        // Figma Arrow 1:2758 â€” #121212 stroke 1.5.
        private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = TEXT
            style = Paint.Style.STROKE
            strokeWidth = 1.5f * density
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
        }

        override fun onDraw(canvas: Canvas) {
            super.onDraw(canvas)
            val centerY = height / 2f
            val centerX = width / 2f
            val path = Path().apply {
                moveTo(centerX - 2f * density, centerY - 4f * density)
                lineTo(centerX + 2f * density, centerY)
                lineTo(centerX - 2f * density, centerY + 4f * density)
            }
            canvas.drawPath(path, paint)
        }
    }

    private companion object {
        val TEXT = Color.rgb(18, 18, 18)
        val SECONDARY = Color.rgb(140, 144, 150)
        val ICON_BACKGROUND = Color.rgb(243, 243, 243)
        val ICON_STROKE = Color.rgb(140, 142, 152)
        val DIVIDER = Color.rgb(239, 239, 239)
        val BACK_BG = Color.rgb(239, 239, 239)
    }
}
