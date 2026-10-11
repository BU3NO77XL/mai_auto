package br.com.hydra.automacao.ui.screens

import android.app.Activity
import android.app.KeyguardManager
import android.app.NotificationManager
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.DashPathEffect
import android.graphics.LinearGradient
import android.graphics.Shader
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import android.text.TextUtils
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import br.com.hydra.automacao.R
import br.com.hydra.automacao.ui.components.buildIdentityHeader
import br.com.hydra.automacao.ui.theme.HydraTheme

/**
 * Tela de Permissoes (aba Permissoes), fiel ao Figma "permiss-es/".
 * Chrome da Home (fundo branco, meia-lua, cabecalho de identidade), card hero
 * TRANSPARENCIA & PRIVACIDADE e checklist com timeline vertical a esquerda.
 */
class PermissionsScreen(
    private val activity: Activity,
    private val isShopeeInstalled: () -> Boolean,
    private val isAccessibilityReady: () -> Boolean,
    private val onToast: (String) -> Unit
) {
    private val BLACK = Color.rgb(18, 18, 18)
    private val MUTED = Color.rgb(107, 107, 112)
    private val BORDER = Color.rgb(236, 238, 236)
    private val GREEN = Color.rgb(49, 111, 81)
    private val RED_DOT = Color.rgb(194, 72, 61)
    private val RED_TEXT = Color.rgb(180, 35, 24)
    private val TRACK = Color.rgb(211, 216, 212)
    private val GRANTED_BG = Color.rgb(247, 250, 248)
    private val PENDING_BG = Color.rgb(255, 247, 245)

    private fun dp(v: Int): Int = HydraTheme.dp(activity, v)
    private fun dp(v: Float): Float = HydraTheme.dpF(activity, v)

    private fun space(h: Int): View = View(activity).apply {
        layoutParams = LinearLayout.LayoutParams(1, dp(h))
    }

    private fun label(text: String, sizeSp: Float, color: Int, tf: Typeface = HydraTheme.fontRegular): TextView =
        TextView(activity).apply {
            this.text = text
            textSize = sizeSp
            typeface = tf
            setTextColor(color)
            includeFontPadding = false
        }

    private fun gradientEyebrow(): TextView = label(
        "TRANSPARÊNCIA & PRIVACIDADE", 11f, BLACK, HydraTheme.fontMedium
    ).apply {
        // Gradiente linear do node 4060:733: verde escuro nas extremidades,
        // clareando no centro do título.
        post {
            paint.shader = LinearGradient(
                0f, 0f, width.toFloat().coerceAtLeast(1f), 0f,
                intArrayOf(
                    Color.rgb(15, 46, 28),
                    Color.rgb(41, 97, 59),
                    Color.rgb(97, 158, 112),
                    Color.rgb(46, 107, 64),
                    Color.rgb(10, 38, 20)
                ),
                floatArrayOf(0f, .28f, .5f, .7f, 1f),
                Shader.TileMode.CLAMP
            )
            invalidate()
        }
    }

    private fun oval(color: Int): GradientDrawable = GradientDrawable().apply {
        shape = GradientDrawable.OVAL
        setColor(color)
    }

    private fun dashedTrack(color: Int): View = object : View(activity) {
        private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = color
            style = Paint.Style.STROKE
            strokeWidth = dp(1.5f)
            pathEffect = DashPathEffect(floatArrayOf(dp(4f), dp(6f)), 0f)
        }

        override fun onDraw(canvas: Canvas) {
            val x = width / 2f
            canvas.drawLine(x, 0f, x, height.toFloat(), paint)
        }
    }

    fun buildView(): View {
        val items = buildItems()

        val content = LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
            clipChildren = false
            // A margem lateral de 16dp ja pertence ao ScrollView da Activity.
            // Reaplica-la aqui reduzia o hero de 343dp para 311dp.
            addView(buildIdentityHeader(activity)); addView(space(25))
            addView(label("Preparar aparelho", 28f, BLACK, HydraTheme.fontMedium).apply { letterSpacing = -0.01f }); addView(space(6))
            addView(label("Configurações essenciais para automatizar com segurança.", 14f, MUTED)); addView(space(16))
            addView(heroCard()); addView(space(24))
            addView(sectionHeader("CHECKLIST DE PREPARAÇÃO")); addView(space(10))
            items.forEachIndexed { index, item ->
                val prevOk = if (index == 0) false else items[index - 1].isOk
                addView(timelineRow(item, isFirst = index == 0, isLast = index == items.lastIndex, prevOk = prevOk))
            }
            addView(space(24))
        }

        return FrameLayout(activity).apply {
            setBackgroundColor(Color.WHITE); clipChildren = false
            addView(View(activity).apply { background = oval(Color.argb(69, 217, 217, 217)) }, FrameLayout.LayoutParams(dp(255), dp(261)).apply {
                // Mantem a mesma ancora do chrome compartilhado por Inicio,
                // Teste e Historico.
                leftMargin = dp(192); topMargin = -dp(141)
            })
            addView(content, FrameLayout.LayoutParams(-1, -2))
        }
    }

    /** Card hero TRANSPARENCIA & PRIVACIDADE (343x132, raio 18, textura do Figma). */
    private fun heroCard(): View = FrameLayout(activity).apply {
        clipChildren = false
        background = HydraTheme.rounded(activity, Color.WHITE, 18, BORDER, 1)
        addView(ImageView(activity).apply {
            setImageResource(R.drawable.img_perm_privacy_bg)
            scaleType = ImageView.ScaleType.CENTER_CROP
            background = HydraTheme.rounded(activity, Color.WHITE, 18)
            clipToOutline = true
        }, FrameLayout.LayoutParams(-1, dp(132)))
        addView(LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(18), dp(20), dp(18), 0)
            // Título desativado temporariamente conforme o ajuste visual solicitado.
            // addView(gradientEyebrow().apply {
            //     gravity = Gravity.CENTER_VERTICAL
            // }, LinearLayout.LayoutParams(-2, dp(14)))
            addView(space(6))
            val titleRow = LinearLayout(activity).apply {
                orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL
            }
            titleRow.addView(FrameLayout(activity).apply {
                addView(ImageView(activity).apply { setImageResource(R.drawable.fig_perm_shield) },
                    FrameLayout.LayoutParams(dp(14), dp(17)).apply { leftMargin = dp(3); topMargin = dp(2) })
                addView(ImageView(activity).apply { setImageResource(R.drawable.fig_perm_shield_check) },
                    FrameLayout.LayoutParams(dp(7), dp(5)).apply { leftMargin = dp(6); topMargin = dp(7) })
            }, LinearLayout.LayoutParams(dp(20), dp(20)))
            titleRow.addView(label("Como a Mai utiliza os acessos", 15f, BLACK, HydraTheme.fontMedium).apply {
                gravity = Gravity.CENTER_VERTICAL
            }, LinearLayout.LayoutParams(-2, dp(20)))
            addView(titleRow, LinearLayout.LayoutParams(-1, dp(20)))
            addView(space(10))
            addView(label("A Mai só interage com a tela\nquando você inicia uma tarefa.\nSeus dados permanecem protegidos.", 13f, MUTED).apply {
                setPadding(dp(4), 0, 0, 0)
            },
                LinearLayout.LayoutParams(-2, LinearLayout.LayoutParams.WRAP_CONTENT))
        }, FrameLayout.LayoutParams(-1, -2))
        addView(View(activity).apply { background = HydraTheme.rounded(activity, Color.TRANSPARENT, 18, BORDER, 1) },
            FrameLayout.LayoutParams(-1, dp(132)))
    }.also { it.layoutParams = LinearLayout.LayoutParams(-1, dp(132)) }

    private data class PermItem(
        val title: String,
        val description: String,
        val isOk: Boolean,
        val pendingAction: String,
        val onAction: () -> Unit
    )

    private fun buildItems(): List<PermItem> {
        val pm = activity.packageManager
        val pkg = activity.packageName
        val shopeeOk = isShopeeInstalled()
        val accOk = isAccessibilityReady()
        val notifOk = if (Build.VERSION.SDK_INT >= 33) {
            activity.checkSelfPermission("android.permission.POST_NOTIFICATIONS") == PackageManager.PERMISSION_GRANTED
        } else {
            activity.getSystemService(NotificationManager::class.java).areNotificationsEnabled()
        }
        val videosOk = if (shopeeOk) {
            val perm = if (Build.VERSION.SDK_INT >= 33) "android.permission.READ_MEDIA_VIDEO"
            else "android.permission.READ_EXTERNAL_STORAGE"
            pm.checkPermission(perm, "com.shopee.br") == PackageManager.PERMISSION_GRANTED
        } else false
        val batteryOk = activity.getSystemService(PowerManager::class.java).isIgnoringBatteryOptimizations(pkg)
        val dndOk = activity.getSystemService(NotificationManager::class.java).isNotificationPolicyAccessGranted
        val lockOk = !activity.getSystemService(KeyguardManager::class.java).isDeviceSecure
        return listOf(
            PermItem("Shopee Brasil", "Aplicativo instalado e visível no aparelho.", shopeeOk,
                "Instalar Shopee", { openShopeeStore() }),
            PermItem("Serviço de acessibilidade", "Permite navegar e ler botões da Shopee.", accOk,
                "Ativar nas Configurações", { open("android.settings.ACCESSIBILITY_SETTINGS") }),
            PermItem("Notificações do sistema", "Acompanha o andamento em segundo plano.", notifOk,
                "Autorizar notificações", { openNotifSettings(pkg) }),
            PermItem("Permissão de vídeos na Shopee", "Permite anexar mídias da galeria.", videosOk,
                "Permissões do app Shopee", { open("android.settings.APPLICATION_DETAILS_SETTINGS", Uri.parse("package:com.shopee.br")) }),
            PermItem("Otimização de bateria", "Evita pausas durante fluxos longos.", batteryOk,
                "Desativar otimização", { openBatterySettings(pkg) }),
            PermItem("Acesso a Não Perturbe", "Ajuda na estabilidade das interrupções.", dndOk,
                "Configurar modo", { open("android.settings.NOTIFICATION_POLICY_ACCESS_SETTINGS") }),
            PermItem("Bloqueio de tela", "Evita que PIN ou biometria interrompam o fluxo.", lockOk,
                "Ajustar segurança", { open("android.settings.SECURITY_SETTINGS") })
        )
    }

    /** Linha da timeline: trilho com dot + card (303dp, raio 16). */
    private fun timelineRow(item: PermItem, isFirst: Boolean, isLast: Boolean, prevOk: Boolean): View {
        val cardH = if (item.isOk) 92 else 110
        // O trilho ja incorpora o vao para o proximo card; nao some outro
        // spacer apos a linha ou o erro vertical se acumula no checklist.
        val gapAfter = when (item.title) {
            "Serviço de acessibilidade", "Notificações do sistema" -> 16
            else -> 12
        }
        val railH = cardH + if (isLast) 0 else gapAfter
        val dotTop = 26
        val dotCenter = dotTop + 6

        val row = LinearLayout(activity).apply {
            orientation = LinearLayout.HORIZONTAL
            clipChildren = false
        }
        val rail = FrameLayout(activity).apply {
            clipChildren = false
            layoutParams = LinearLayout.LayoutParams(dp(30), dp(railH))
        }
        if (!isFirst) {
            rail.addView(dashedTrack(if (prevOk) GREEN else TRACK),
                FrameLayout.LayoutParams(dp(2), dp(dotCenter)).apply {
                    leftMargin = dp(14); gravity = Gravity.TOP or Gravity.START
                })
        }
        if (!isLast) {
            rail.addView(dashedTrack(if (item.isOk) GREEN else TRACK),
                FrameLayout.LayoutParams(dp(2), dp(railH - dotCenter)).apply {
                    leftMargin = dp(14); topMargin = dp(dotCenter); gravity = Gravity.TOP or Gravity.START
                })
        }
        rail.addView(View(activity).apply {
            background = HydraTheme.rounded(activity, if (item.isOk) GREEN else RED_DOT, 100)
        }, FrameLayout.LayoutParams(dp(12), dp(12)).apply {
            leftMargin = dp(9); topMargin = dp(dotTop); gravity = Gravity.TOP or Gravity.START
        })
        row.addView(rail)

        val card = LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
            background = HydraTheme.rounded(activity, if (item.isOk) GRANTED_BG else PENDING_BG, 16, BORDER, 1)
            setPadding(dp(16), dp(12), dp(16), 0)
            clipChildren = false
            layoutParams = LinearLayout.LayoutParams(0, dp(cardH), 1f).apply { leftMargin = dp(10) }
        }
        card.addView(label(item.title, 15f, BLACK, HydraTheme.fontMedium).apply {
            maxLines = 1; ellipsize = TextUtils.TruncateAt.END
        })
        card.addView(space(6))
        card.addView(label(item.description, 13f, MUTED).apply {
            maxLines = 1; ellipsize = TextUtils.TruncateAt.END
        })
        card.addView(space(if (item.isOk) 8 else 10))
        val status = label(if (item.isOk) "Concedido" else "Pendente \u00B7 ${item.pendingAction} \u2192",
            12f, if (item.isOk) GREEN else RED_TEXT, HydraTheme.fontMedium).apply {
            maxLines = 1; ellipsize = TextUtils.TruncateAt.END
        }
        if (!item.isOk) {
            status.setOnClickListener { safeAction(item.onAction) }
            card.setOnClickListener { safeAction(item.onAction) }
        }
        card.addView(status)
        row.addView(card)

        return LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
            addView(row)
        }
    }

    private fun sectionHeader(title: String): View = TextView(activity).apply {
        text = title
        textSize = 11f
        typeface = HydraTheme.fontMedium
        setTextColor(MUTED)
        letterSpacing = 0.08f
        includeFontPadding = false
        gravity = Gravity.CENTER_VERTICAL
        layoutParams = LinearLayout.LayoutParams(-2, dp(14))
    }

    private fun safeAction(block: () -> Unit) {
        try { block() } catch (e: Exception) { onToast("Nao foi possivel abrir a configuracao") }
    }

    private fun open(action: String, data: Uri? = null) {
        val intent = Intent(action).apply { if (data != null) this.data = data }
        try { activity.startActivity(intent) }
        catch (e: Exception) { onToast("Nao foi possivel abrir a configuracao") }
    }

    private fun openShopeeStore() {
        try {
            activity.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=com.shopee.br")))
        } catch (e: Exception) {
            try { activity.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=com.shopee.br"))) }
            catch (e2: Exception) { onToast("Instale a Shopee Brasil antes de prosseguir") }
        }
    }

    private fun openNotifSettings(pkg: String) {
        try {
            if (Build.VERSION.SDK_INT >= 26) {
                activity.startActivity(Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                    putExtra(Settings.EXTRA_APP_PACKAGE, pkg)
                })
            } else open("android.settings.APPLICATION_DETAILS_SETTINGS", Uri.parse("package:$pkg"))
        } catch (e: Exception) { onToast("Nao foi possivel abrir a configuracao") }
    }

    private fun openBatterySettings(pkg: String) {
        try {
            activity.startActivity(Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, Uri.parse("package:$pkg")))
        } catch (e: Exception) {
            open("android.settings.IGNORE_BATTERY_OPTIMIZATION_SETTINGS")
        }
    }
}
