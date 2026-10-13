package br.com.hydra.automacao.ui.screens

import android.content.Context
import android.graphics.Color
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import br.com.hydra.automacao.R
import br.com.hydra.automacao.ui.theme.HydraTheme

/**
 * Onboarding — estilo do Figma "onboarding-8", textos em português.
 * Mosaico de fotos + faixa branca com título, subtítulo, dots e botões.
 */
class OnboardingScreen(
    private val context: Context,
    private val onStart: () -> Unit,
) {

    private val TEXT = Color.rgb(18, 18, 18)
    private val SUB = Color.rgb(140, 142, 152)
    private val DOT_OFF = Color.rgb(167, 167, 167)

    fun buildView(): View = LinearLayout(context).apply {
        orientation = LinearLayout.VERTICAL
        setBackgroundColor(Color.WHITE)

        addView(mosaic(), LinearLayout.LayoutParams(-1, dp(480)))
        addView(bottomSheet(), LinearLayout.LayoutParams(-1, 0, 1f))
    }

    private fun mosaic() = FrameLayout(context).apply {
        setBackgroundColor(Color.WHITE)
        clipChildren = false
        // Foto única de destaque (img_onboarding_hero) com cantos arredondados,
        // centralizada com margens laterais — substitui o mosaico de 6 fotos.
        addView(ImageView(context).apply {
            setImageResource(R.drawable.img_onboarding_hero)
            scaleType = ImageView.ScaleType.CENTER_CROP
            background = HydraTheme.rounded(context, Color.rgb(240, 240, 240), radiusDp = 30)
            clipToOutline = true
        }, FrameLayout.LayoutParams(-1, dp(430), Gravity.CENTER).apply {
            marginStart = dp(16)
            marginEnd = dp(16)
        })
    }

    @Deprecated("Mosaico antigo de 6 fotos — mantido para referência até remover os assets.")
    private fun mosaicLegacy() = FrameLayout(context).apply {
        setBackgroundColor(Color.WHITE)
        clipChildren = false
        addView(LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_HORIZONTAL
            addView(LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                addView(mosaicImage(R.drawable.image_1_371, 85, 200))
                addView(View(context).apply {
                    layoutParams = LinearLayout.LayoutParams(1, dp(12))
                })
                addView(mosaicImage(R.drawable.image_1_372, 85, 200))
            }, LinearLayout.LayoutParams(dp(85), -2).apply { marginStart = dp(-50) })
            addView(View(context).apply {
                layoutParams = LinearLayout.LayoutParams(dp(10), 1)
            })
            addView(LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                addView(mosaicImage(R.drawable.image_1_367, 180, 250))
                addView(View(context).apply {
                    layoutParams = LinearLayout.LayoutParams(1, dp(12))
                })
                addView(mosaicImage(R.drawable.image_1_368, 180, 200))
            }, LinearLayout.LayoutParams(dp(180), -2))
            addView(View(context).apply {
                layoutParams = LinearLayout.LayoutParams(dp(10), 1)
            })
            addView(LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                addView(mosaicImage(R.drawable.image_1_369, 85, 200))
                addView(View(context).apply {
                    layoutParams = LinearLayout.LayoutParams(1, dp(12))
                })
                addView(mosaicImage(R.drawable.image_1_370, 85, 200))
            }, LinearLayout.LayoutParams(dp(85), -2).apply { marginEnd = dp(-50) })
        }, FrameLayout.LayoutParams(-1, -2, Gravity.CENTER))
    }

    private fun bottomSheet() = LinearLayout(context).apply {
        orientation = LinearLayout.VERTICAL
        gravity = Gravity.CENTER_HORIZONTAL
        setBackgroundColor(Color.WHITE)
        setPadding(dp(24), dp(20), dp(24), dp(24))

        addView(TextView(context).apply {
            text = "As novidades em produtos, em um só lugar"
            textSize = 22f
            typeface = HydraTheme.fontRegular
            setTextColor(TEXT)
            gravity = Gravity.CENTER
        }, LinearLayout.LayoutParams(-1, -2))
        addView(View(context).apply {
            layoutParams = LinearLayout.LayoutParams(1, dp(15))
        })
        addView(TextView(context).apply {
            text = "Olá! Confira os últimos estilos e acessórios incríveis a caminho do seu e-commerce de moda"
            textSize = 14f
            typeface = HydraTheme.fontRegular
            setTextColor(SUB)
            gravity = Gravity.CENTER
        }, LinearLayout.LayoutParams(-1, -2))
        addView(View(context).apply {
            layoutParams = LinearLayout.LayoutParams(1, dp(20))
        })
        addView(dots())
        addView(View(context).apply {
            layoutParams = LinearLayout.LayoutParams(1, dp(20))
        })
        addView(TextView(context).apply {
            text = "Começar"
            textSize = 16f
            typeface = HydraTheme.fontRegular
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            setPadding(0, dp(15), 0, dp(15))
            background = HydraTheme.rounded(context, TEXT, radiusDp = 30)
            setOnClickListener { onStart() }
        }, LinearLayout.LayoutParams(-1, dp(56)))
        addView(View(context).apply {
            layoutParams = LinearLayout.LayoutParams(1, dp(8))
        })
        addView(TextView(context).apply {
            text = "Já tenho conta"
            textSize = 16f
            typeface = HydraTheme.fontMedium
            setTextColor(TEXT)
            gravity = Gravity.CENTER
            setPadding(0, dp(15), 0, dp(15))
            setOnClickListener { onStart() }
        }, LinearLayout.LayoutParams(-1, dp(56)))
    }

    private fun mosaicImage(res: Int, w: Int, h: Int) = ImageView(context).apply {
        setImageResource(res)
        scaleType = ImageView.ScaleType.CENTER_CROP
        background = HydraTheme.rounded(context, Color.rgb(240, 240, 240), radiusDp = 30)
        clipToOutline = true
        layoutParams = LinearLayout.LayoutParams(dp(w), dp(h))
    }

    private fun dots() = LinearLayout(context).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER
        repeat(2) {
            addView(View(context).apply {
                background = HydraTheme.rounded(context, DOT_OFF, radiusDp = 56)
                alpha = 0.4f
            }, LinearLayout.LayoutParams(dp(6), dp(6)).apply { marginEnd = dp(6) })
        }
        addView(View(context).apply {
            background = HydraTheme.rounded(context, TEXT, radiusDp = 56)
        }, LinearLayout.LayoutParams(dp(6), dp(6)))
    }

    private fun dp(value: Int) = HydraTheme.dp(context, value)
}
