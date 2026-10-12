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
            // Ritmo vertical consistente: respiro 20 após o topBar, 24 entre
            // seções (padrão das demais telas: título→conteúdo 24, blocos 20).
            setPadding(dp(16), dp(20), dp(16), dp(0))
            addView(profileIdentity())
            addView(space(24))
            addView(devicesSection())
            addView(space(24))
            addView(publishSection())
            addView(space(24))
            addView(telegramSection())
            addView(space(24))
            addView(versionFooter())
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

        addView(text("Configurações", 18f, TEXT).apply { gravity = Gravity.CENTER },
            FrameLayout.LayoutParams(-1, -1, Gravity.CENTER))
    }

    /** Topo mantido: avatar + nome + cargo (visual, sem edição por enquanto). */
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
            addView(text("Afiliado Shopee", 14f, SECONDARY))
        }
        addView(identity, LinearLayout.LayoutParams(0, dp(60), 1f).apply {
            marginStart = dp(12)
        })
    }

    // ── Seção: Dispositivos & permissões (visual) ──
    // Padrão profissional: aparelho atual com identificação + resumo das
    // permissões (mesmas da tela Permissões) + atalho "Revisar permissões".
    private fun devicesSection() =
        LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            addView(text("Dispositivos & permissões", 16f, TEXT), LinearLayout.LayoutParams(-1, dp(24)))
            addView(space(16))

            val card = LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                background = HydraTheme.rounded(context, Color.WHITE, radiusDp = 12)
                setPadding(dp(0), dp(12), dp(0), dp(12))
                addView(deviceRow())
                addView(space(16))
                addView(divider())
                addView(space(16))
                addView(row("Revisar permissões", ProfileIcon.GEAR))
            }
            addView(card, LinearLayout.LayoutParams(-1, -2))
        }

    private fun deviceRow() = LinearLayout(context).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL

        val iconView = ImageView(context).apply {
            setImageResource(R.drawable.ic_profile_bell)
            scaleType = ImageView.ScaleType.FIT_CENTER
            adjustViewBounds = false
        }
        addView(iconView, LinearLayout.LayoutParams(dp(40), dp(40)))

        val info = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            addView(text("Este aparelho", 14f, TEXT))
            addView(space(2))
            addView(text("Android · sincronizado agora", 12f, SECONDARY))
            addView(space(2))
            addView(text("6 de 7 permissões concedidas", 12f, SECONDARY))
        }
        addView(info, LinearLayout.LayoutParams(0, -2, 1f).apply {
            marginStart = dp(12)
        })
        addView(Chevron(context), LinearLayout.LayoutParams(dp(24), dp(24)).apply {
            marginStart = dp(8)
        })
    }

    // ── Seção: Publicação (visual) ──
    // Padrão profissional: seletor persistente com as 2 opções explicadas.
    private fun publishSection() =
        LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            addView(text("Publicação", 16f, TEXT), LinearLayout.LayoutParams(-1, dp(24)))
            addView(space(4))
            addView(text("Escolha o destino padrão dos seus vídeos.", 13f, SECONDARY),
                LinearLayout.LayoutParams(-1, -2))
            addView(space(16))

            val card = LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                background = HydraTheme.rounded(context, Color.WHITE, radiusDp = 12)
                setPadding(dp(0), dp(12), dp(0), dp(12))
                addView(publishOption(
                    selected = true,
                    title = "Publicar na Shopee",
                    desc = "O vídeo vai ao ar assim que publicado.",
                ))
                addView(space(16))
                addView(divider())
                addView(space(16))
                addView(publishOption(
                    selected = false,
                    title = "Salvar como rascunho",
                    desc = "Fica na caixa de rascunhos da Shopee para você revisar e postar manualmente.",
                ))
            }
            addView(card, LinearLayout.LayoutParams(-1, -2))
        }

    private fun publishOption(selected: Boolean, title: String, desc: String) =
        LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL

            addView(View(context).apply {
                background = if (selected) selectedDot() else unselectedDot()
            }, LinearLayout.LayoutParams(dp(20), dp(20)))

            val info = LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                addView(text(title, 14f, TEXT))
                addView(space(2))
                addView(text(desc, 12f, SECONDARY))
            }
            addView(info, LinearLayout.LayoutParams(0, -2, 1f).apply {
                marginStart = dp(12)
            })
        }

    private fun selectedDot() = android.graphics.drawable.GradientDrawable().apply {
        shape = android.graphics.drawable.GradientDrawable.OVAL
        setColor(Color.WHITE)
        setStroke(dp(6), Color.rgb(49, 111, 81))
    }

    private fun unselectedDot() = android.graphics.drawable.GradientDrawable().apply {
        shape = android.graphics.drawable.GradientDrawable.OVAL
        setColor(Color.WHITE)
        setStroke(dp(2), Color.rgb(200, 200, 200))
    }

    // ── Seção: Telegram (visual) ──
    // Conta vinculada = entrada de vídeos (backend futuro). Botões Abrir e
    // Desvincular + explicação, padrão de apps com integração externa.
    private fun telegramSection() =
        LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            addView(text("Telegram", 16f, TEXT), LinearLayout.LayoutParams(-1, dp(24)))
            addView(space(16))

            val card = LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                background = HydraTheme.rounded(context, Color.WHITE, radiusDp = 12)
                setPadding(dp(16), dp(16), dp(16), dp(16))

                addView(LinearLayout(context).apply {
                    orientation = LinearLayout.HORIZONTAL
                    gravity = Gravity.CENTER_VERTICAL
                    addView(ImageView(context).apply {
                        setImageResource(R.drawable.ic_profile_chat)
                        scaleType = ImageView.ScaleType.FIT_CENTER
                    }, LinearLayout.LayoutParams(dp(40), dp(40)))
                    val info = LinearLayout(context).apply {
                        orientation = LinearLayout.VERTICAL
                        addView(text("@seu_usuario", 14f, TEXT))
                        addView(space(2))
                        addView(text("Conta vinculada", 12f, Color.rgb(49, 111, 81)))
                    }
                    addView(info, LinearLayout.LayoutParams(0, -2, 1f).apply {
                        marginStart = dp(12)
                    })
                })
                addView(space(12))
                addView(text(
                    "É pelo Telegram que você encaminha seus criativos. " +
                        "Você pode desvincular a conta atual a qualquer momento e vincular outra se quiser.",
                    12f, SECONDARY,
                ))
                addView(space(16))
                addView(LinearLayout(context).apply {
                    orientation = LinearLayout.HORIZONTAL
                    addView(actionButton("Abrir o Telegram", primary = true),
                        LinearLayout.LayoutParams(0, -2, 1f))
                    addView(View(context).apply {
                        layoutParams = LinearLayout.LayoutParams(dp(12), 1)
                    })
                    addView(actionButton("Desvincular", primary = false),
                        LinearLayout.LayoutParams(0, -2, 1f))
                })
            }
            addView(card, LinearLayout.LayoutParams(-1, -2))
        }

    private fun actionButton(label: String, primary: Boolean) = TextView(context).apply {
        text = label
        textSize = 14f
        typeface = HydraTheme.fontRegular
        gravity = Gravity.CENTER
        setPadding(dp(8), dp(12), dp(8), dp(12))
        background = if (primary)
            HydraTheme.rounded(context, Color.rgb(49, 111, 81), radiusDp = 12)
        else
            HydraTheme.rounded(context, Color.WHITE, radiusDp = 12,
                strokeColor = Color.rgb(232, 232, 232), strokeWidthDp = 1)
        setTextColor(if (primary) Color.WHITE else TEXT)
    }

    // ── Rodapé: versão (visual) ──
    // Padrão profissional: versão instalada + status de atualização.
    private fun versionFooter() =
        LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            addView(text("Mai v0.2.0", 13f, SECONDARY).apply { gravity = Gravity.CENTER })
            addView(space(4))
            addView(text("Você está na versão mais recente ✓", 12f, Color.rgb(49, 111, 81)).apply {
                gravity = Gravity.CENTER
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
