package br.com.hydra.automacao.ui.screens

import android.content.Context
import android.graphics.Color
import android.text.InputType
import android.view.Gravity
import android.view.View
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import br.com.hydra.automacao.R
import br.com.hydra.automacao.ui.components.CozyButton
import br.com.hydra.automacao.ui.theme.HydraTheme

/**
 * Tela de Login — fiel ao Figma "Sign in" 1:474, login simulado por enquanto.
 * Título 22sp + subtítulo, 2 inputs pill (rótulo 16sp + campo 52dp borda
 * #F3F3F3), "Forgot Password?", botão Sign In 56dp radius 30, divisória
 * "Or using other method", 2 botões sociais brancos 56dp borda #F3F3F3.
 */
class LoginScreen(
    private val context: Context,
    private val onLogin: (email: String) -> Unit,
) {

    private val TEXT = Color.rgb(18, 18, 18)
    private val HINT = Color.rgb(167, 174, 193)
    private val FIELD_BORDER = Color.rgb(243, 243, 243)
    private val BTN_DISABLED = Color.rgb(202, 202, 202)

    private lateinit var emailInput: EditText
    private lateinit var passwordInput: EditText
    private lateinit var signInButton: CozyButton
    private lateinit var emailError: TextView
    private var passwordVisible = false

    fun buildView(): View = ScrollView(context).apply {
        isVerticalScrollBarEnabled = false
        setBackgroundColor(Color.WHITE)
        addView(LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.WHITE)
            setPadding(dp(16), dp(48), dp(16), dp(24))

            // Marca Mai — profissionaliza o topo sem quebrar o Figma
            // (o Figma não tem logo; adicionamos selo discreto acima do título).
            addView(LinearLayout(context).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                addView(View(context).apply {
                    background = HydraTheme.rounded(context, Color.rgb(35, 83, 62), radiusDp = 12)
                }, LinearLayout.LayoutParams(dp(36), dp(36)))
                addView(TextView(context).apply {
                    text = "Mai"
                    textSize = 20f
                    typeface = HydraTheme.fontMedium
                    setTextColor(TEXT)
                    gravity = Gravity.CENTER_VERTICAL
                }, LinearLayout.LayoutParams(-2, dp(36)).apply { marginStart = dp(10) })
            })
            addView(space(28))

            addView(label("Login Account", 22f, TEXT))
            addView(space(4))
            addView(label("Please login with registered account", 14f, HINT))
            addView(space(28))

            addView(label("Email or Phone Number", 16f, TEXT))
            addView(space(10))
            emailInput = field("Enter your email or phone number",
                InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS)
            addView(emailInput, LinearLayout.LayoutParams(-1, dp(52)))
            emailError = TextView(context).apply {
                textSize = 12f
                typeface = HydraTheme.fontRegular
                setTextColor(Color.rgb(196, 76, 58))
                visibility = View.GONE
                setPadding(dp(14), dp(6), dp(14), 0)
            }
            addView(emailError, LinearLayout.LayoutParams(-1, -2))
            addView(space(16))

            addView(label("Password", 16f, TEXT))
            addView(space(10))
            addView(passwordRow())
            addView(space(15))

            addView(TextView(context).apply {
                text = "Forgot Password?"
                textSize = 14f
                typeface = HydraTheme.fontRegular
                setTextColor(HINT)
                gravity = Gravity.END
            }, LinearLayout.LayoutParams(-1, -2))
            addView(space(30))

            signInButton = CozyButton(context, text = "Sign In", onClick = {
                tryLogin()
            }).apply {
                background = HydraTheme.rounded(context, BTN_DISABLED, radiusDp = 30)
            }
            addView(signInButton, LinearLayout.LayoutParams(-1, dp(56)))
            addView(space(28))

            // Divisória elegante com traços laterais (padrão profissional).
            addView(LinearLayout(context).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                addView(View(context).apply {
                    setBackgroundColor(FIELD_BORDER)
                }, LinearLayout.LayoutParams(0, dp(1), 1f))
                addView(label("Or using other method", 13f, HINT).apply {
                    gravity = Gravity.CENTER
                    setPadding(dp(12), 0, dp(12), 0)
                }, LinearLayout.LayoutParams(-2, -2))
                addView(View(context).apply {
                    setBackgroundColor(FIELD_BORDER)
                }, LinearLayout.LayoutParams(0, dp(1), 1f))
            }, LinearLayout.LayoutParams(-1, -2))
            addView(space(16))

            addView(socialButton("Sign In with Google", R.drawable.ic_google))
            addView(space(15))
            addView(socialButton("Sign In with Facebook", R.drawable.ic_facebook))

            emailInput.addTextChangedListener(SimpleWatcher { refreshButton() })
            passwordInput.addTextChangedListener(SimpleWatcher { refreshButton() })
            refreshButton()
        })
    }

    private fun field(hint: String, inputType: Int): EditText =
        EditText(context).apply {
            this.hint = hint
            setHintTextColor(HINT)
            setTextColor(TEXT)
            textSize = 14.5f
            typeface = HydraTheme.fontRegular
            this.inputType = InputType.TYPE_CLASS_TEXT or inputType
            isSingleLine = true
            setPadding(dp(14), dp(12), dp(14), dp(12))
            background = HydraTheme.rounded(
                context, bgColor = Color.WHITE, radiusDp = 50,
                strokeColor = FIELD_BORDER, strokeWidthDp = 1,
            )
        }

    private fun passwordRow() = LinearLayout(context).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        background = HydraTheme.rounded(
            context, bgColor = Color.WHITE, radiusDp = 50,
            strokeColor = FIELD_BORDER, strokeWidthDp = 1,
        )
        setPadding(dp(14), 0, dp(8), 0)

        passwordInput = EditText(context).apply {
            hint = "Enter your password"
            setHintTextColor(HINT)
            setTextColor(TEXT)
            textSize = 14.5f
            typeface = HydraTheme.fontRegular
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
            isSingleLine = true
            background = null
            setPadding(0, dp(12), 0, dp(12))
        }
        addView(passwordInput, LinearLayout.LayoutParams(0, -2, 1f))

        val eye = ImageView(context).apply {
            setImageResource(R.drawable.ic_eye)
            setPadding(dp(8), dp(8), dp(8), dp(8))
            setOnClickListener {
                passwordVisible = !passwordVisible
                passwordInput.inputType = InputType.TYPE_CLASS_TEXT or
                    if (passwordVisible) InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
                    else InputType.TYPE_TEXT_VARIATION_PASSWORD
                passwordInput.setSelection(passwordInput.text.length)
            }
        }
        addView(eye, LinearLayout.LayoutParams(dp(40), dp(40)))
    }

    private fun socialButton(label: String, iconRes: Int) = LinearLayout(context).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER
        background = HydraTheme.rounded(
            context, bgColor = Color.WHITE, radiusDp = 30,
            strokeColor = FIELD_BORDER, strokeWidthDp = 1,
        )
        setPadding(dp(16), dp(15), dp(16), dp(15))
        setOnClickListener { onLogin("") }

        addView(ImageView(context).apply {
            setImageResource(iconRes)
        }, LinearLayout.LayoutParams(dp(24), dp(24)))
        addView(TextView(context).apply {
            text = label
            textSize = 16f
            typeface = HydraTheme.fontRegular
            setTextColor(TEXT)
            gravity = Gravity.CENTER_VERTICAL
        }, LinearLayout.LayoutParams(-2, -2).apply { marginStart = dp(10) })
    }

    private fun refreshButton() {
        emailError.visibility = View.GONE
        val ready = emailInput.text.isNotBlank() && passwordInput.text.isNotBlank()
        signInButton.isEnabled = ready
        signInButton.alpha = if (ready) 1f else 0.85f
        signInButton.background = HydraTheme.rounded(
            context,
            if (ready) Color.rgb(35, 83, 62) else BTN_DISABLED,
            radiusDp = 30,
        )
    }

    /** Validação profissional: email/telefone válido + senha ≥4, erro inline + loading. */
    private fun tryLogin() {
        val email = emailInput.text.toString().trim()
        val pass = passwordInput.text.toString()
        val emailOk = android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches() ||
            email.matches(Regex("^[0-9+()\\- ]{8,}$"))
        if (!emailOk) {
            emailError.text = "Digite um email ou telefone válido."
            emailError.visibility = View.VISIBLE
            emailInput.requestFocus()
            return
        }
        if (pass.length < 4) {
            emailError.text = "A senha precisa de ao menos 4 caracteres."
            emailError.visibility = View.VISIBLE
            passwordInput.requestFocus()
            return
        }
        emailError.visibility = View.GONE
        signInButton.setLoading(true, "Entrando…")
        signInButton.postDelayed({
            signInButton.setLoading(false)
            onLogin(email)
        }, 900)
    }

    private fun label(value: String, size: Float, color: Int) = TextView(context).apply {
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

    private class SimpleWatcher(val onChange: () -> Unit) : android.text.TextWatcher {
        override fun beforeTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
        override fun onTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) = onChange()
        override fun afterTextChanged(s: android.text.Editable?) {}
    }
}
