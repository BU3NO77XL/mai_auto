package br.com.hydra.automacao

import android.app.Activity
import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import br.com.hydra.automacao.automation.AutotestService
import br.com.hydra.automacao.automation.HydraAccessibilityService
import br.com.hydra.automacao.automation.TestResultStore
import br.com.hydra.automacao.auth.SessionStore
import br.com.hydra.automacao.ui.screens.LoginScreen
import br.com.hydra.automacao.ui.screens.OnboardingScreen
import br.com.hydra.automacao.update.AppUpdateChecker
import br.com.hydra.automacao.update.UpdateDialog
import br.com.hydra.automacao.ui.components.CozyFloatingNav
import br.com.hydra.automacao.ui.components.CozyPulseDot
import br.com.hydra.automacao.ui.screens.HistoricoScreen
import br.com.hydra.automacao.ui.screens.HomeScreen
import br.com.hydra.automacao.ui.screens.PermissionsScreen
import br.com.hydra.automacao.ui.screens.ProfileScreen
import br.com.hydra.automacao.ui.screens.TestScreen
import br.com.hydra.automacao.ui.theme.HydraTheme

/**
 * Atividade Principal do Hydra.
 * Atua como orquestrador de ciclo de vida e navegação entre telas especializadas.
 */
class MainActivity : Activity() {

    private lateinit var rootLayout: FrameLayout
    private lateinit var contentContainer: LinearLayout
    private lateinit var floatingNav: CozyFloatingNav
    private lateinit var brandHeader: View
    private lateinit var headerSpacer: View
    private var currentTab: CozyFloatingNav.NavTab = CozyFloatingNav.NavTab.HOME

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Configuração de cores do sistema (Status Bar e Navigation Bar claras e aconchegantes)
        window.statusBarColor = HydraTheme.background
        window.navigationBarColor = HydraTheme.background
        @Suppress("DEPRECATION")
        window.decorView.systemUiVisibility =
            View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR or View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR

        // Fluxo de entrada: onboarding (1ª vez) → login simulado → app.
        // O backend vai trocar o SessionStore por API depois.
        val session = SessionStore(this)
        if (!session.hasSeenOnboarding()) {
            showOnboarding()
            return
        }
        if (!session.isLoggedIn()) {
            showLogin()
            return
        }

        setupRootView()
        renderCurrentScreen()
        checkForUpdates()
    }

    private fun showOnboarding() {
        setContentView(OnboardingScreen(this) {
            SessionStore(this).markOnboardingSeen()
            showLogin()
        }.buildView())
    }

    private fun showLogin() {
        setContentView(LoginScreen(this) { email ->
            SessionStore(this).login(email.ifBlank { "visitante@mai.app" })
            recreate()
        }.buildView())
    }

    /**
     * Verifica o version.json no R2 uma vez por abertura.
     * Se houver build maior, mostra o diálogo "Nova versão disponível".
     */
    private fun checkForUpdates() {
        AppUpdateChecker.check(this) { result ->
            if (result is AppUpdateChecker.CheckResult.UpdateAvailable) {
                var dialog: UpdateDialog? = null
                dialog = UpdateDialog(this, result.remote) {
                    val started = AppUpdateChecker.downloadAndInstall(this, result.remote)
                    // Sem permissão de fontes desconhecidas: fecha e avisa para
                    // autorizar; o usuário toca em Atualizar de novo depois.
                    if (started == null) {
                        dialog?.dismiss()
                        android.widget.Toast.makeText(
                            this,
                            "Autorize \"Instalar apps desconhecidos\" e toque em Atualizar de novo.",
                            android.widget.Toast.LENGTH_LONG,
                        ).show()
                    }
                }
                dialog.show()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (::floatingNav.isInitialized) {
            updateServiceBadges()
            renderCurrentScreen()
        }
    }

    private fun setupRootView() {
        rootLayout = FrameLayout(this).apply {
            setBackgroundColor(HydraTheme.background)
        }

        val scrollView = ScrollView(this).apply {
            isFillViewport = true
            clipToPadding = false
            // Espaço inferior generoso para a barra flutuante não sobrepor o conteúdo
            val padBottom = HydraTheme.dp(this@MainActivity, 96)
            setPadding(0, 0, 0, padBottom)
        }

        val scrollContent = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            clipChildren = false
            // Permite que o carrossel da Home ultrapasse o padding horizontal de 16dp.
            clipToPadding = false
            val padH = HydraTheme.dp(this@MainActivity, 16)
            val padTop = HydraTheme.dp(this@MainActivity, 20)
            setPadding(padH, padTop, padH, 0)
        }

        // Cabeçalho superior elegante e discreto
        brandHeader = buildTopBrandHeader()
        scrollContent.addView(brandHeader)

        headerSpacer = View(this).apply {
            layoutParams = LinearLayout.LayoutParams(1, HydraTheme.dp(this@MainActivity, 16))
        }
        scrollContent.addView(headerSpacer)

        contentContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            clipChildren = false
        }
        scrollContent.addView(contentContainer)
        scrollView.addView(scrollContent)
        rootLayout.addView(scrollView)

        // Menu de navegação flutuante inferior
        floatingNav = CozyFloatingNav(this) { selectedTab ->
            currentTab = selectedTab
            renderCurrentScreen()
        }
        rootLayout.addView(floatingNav)

        setContentView(rootLayout)
    }

    private fun buildTopBrandHeader(): View = LinearLayout(this).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        val padV = HydraTheme.dp(this@MainActivity, 4)
        setPadding(0, padV, 0, padV)

        val brand = TextView(this@MainActivity).apply {
            text = "MAI  ·  AUTOMAÇÃO"
            textSize = 11.5f
            typeface = HydraTheme.fontMedium
            setTextColor(HydraTheme.primary)
            letterSpacing = 0.18f
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }
        addView(brand)

        // Chip de status com micro-pulso
        val accReady = accessibilityReady()
        val chip = LinearLayout(this@MainActivity).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            val padH = HydraTheme.dp(this@MainActivity, 9)
            val padV = HydraTheme.dp(this@MainActivity, 4)
            setPadding(padH, padV, padH, padV)
            background = HydraTheme.pill(
                this@MainActivity,
                if (accReady) HydraTheme.successBg else HydraTheme.warningBg
            )
        }

        val dot = CozyPulseDot(
            this@MainActivity,
            dotColor = if (accReady) HydraTheme.success else HydraTheme.warning
        ).apply {
            val size = HydraTheme.dp(this@MainActivity, 8)
            layoutParams = LinearLayout.LayoutParams(size, size).also {
                it.marginEnd = HydraTheme.dp(this@MainActivity, 6)
            }
        }
        chip.addView(dot)

        val statusText = TextView(this@MainActivity).apply {
            text = if (accReady) "Conectado" else "Pendente"
            textSize = 11f
            typeface = HydraTheme.fontMedium
            setTextColor(if (accReady) HydraTheme.successText else HydraTheme.warningText)
        }
        chip.addView(statusText)
        addView(chip)
    }

    private fun renderCurrentScreen() {
        contentContainer.removeAllViews()
        updateServiceBadges()

        // As telas redesenhadas no Figma compartilham o fundo branco e o menu preto.
        val isHome = currentTab == CozyFloatingNav.NavTab.HOME || currentTab == CozyFloatingNav.NavTab.TEST ||
            currentTab == CozyFloatingNav.NavTab.PRODUCTION || currentTab == CozyFloatingNav.NavTab.PERMISSIONS ||
            currentTab == CozyFloatingNav.NavTab.SETTINGS
        brandHeader.visibility = if (isHome) View.GONE else View.VISIBLE
        headerSpacer.visibility = if (isHome) View.GONE else View.VISIBLE
        val screenColor = if (isHome) android.graphics.Color.WHITE else HydraTheme.background
        rootLayout.setBackgroundColor(screenColor)
        window.statusBarColor = screenColor
        window.navigationBarColor = if (isHome) android.graphics.Color.rgb(18, 18, 18) else HydraTheme.background
        @Suppress("DEPRECATION")
        window.decorView.systemUiVisibility = if (isHome) {
            View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
        } else {
            View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR or View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
        }
        floatingNav.setHomeAppearance(isHome)

        val screenView: View = when (currentTab) {
            CozyFloatingNav.NavTab.HOME -> HomeScreen(
                context = this,
                isAccessibilityReady = { accessibilityReady() },
                isShopeeInstalled = { shopeeInstalled() },
                onNavigateToTest = { navigateTo(CozyFloatingNav.NavTab.TEST) },
                onNavigateToProduction = { navigateTo(CozyFloatingNav.NavTab.PRODUCTION) },
                onNavigateToPermissions = { navigateTo(CozyFloatingNav.NavTab.PERMISSIONS) }
            ).buildView()

            CozyFloatingNav.NavTab.TEST -> HistoricoScreen(
                context = this
            ).buildView()

            CozyFloatingNav.NavTab.PRODUCTION -> TestScreen(
                context = this,
                onStartTest = { startAutotest() },
                onRefresh = { renderCurrentScreen() }
            ).buildView()

            CozyFloatingNav.NavTab.PERMISSIONS -> PermissionsScreen(
                activity = this,
                isShopeeInstalled = { shopeeInstalled() },
                isAccessibilityReady = { accessibilityReady() },
                onToast = { showToast(it) }
            ).buildView()

            CozyFloatingNav.NavTab.SETTINGS -> ProfileScreen(this).buildView()
        }

        contentContainer.addView(screenView)
    }

    private fun navigateTo(tab: CozyFloatingNav.NavTab) {
        currentTab = tab
        floatingNav.selectTab(tab)
        renderCurrentScreen()
    }

    private fun updateServiceBadges() {
        // O autoteste roda na aba "Teste" (PRODUCTION); a aba Histórico não tem badge.
        floatingNav.setTabBadge(CozyFloatingNav.NavTab.PRODUCTION, AutotestService.isRunning)
    }

    // ── Disparo de Operações ──

    private fun startAutotest() {
        if (!shopeeInstalled()) {
            showToast("Shopee Brasil não está instalada")
            return
        }
        if (!accessibilityReady()) {
            showToast("Ative o Serviço de Acessibilidade em Permissões")
            navigateTo(CozyFloatingNav.NavTab.PERMISSIONS)
            return
        }
        val launch = packageManager.getLaunchIntentForPackage("com.shopee.br")
        if (launch == null) {
            showToast("Não consegui abrir a Shopee")
            return
        }
        try {
            TestResultStore(this).save("Iniciando", "Preparando autoteste na Shopee…")
            startForegroundService(
                Intent(this, AutotestService::class.java).setAction(AutotestService.ACTION_RUN)
            )
            startActivity(launch)
            renderCurrentScreen()
        } catch (error: Exception) {
            TestResultStore(this).save("Abertura", error.message ?: "Falha ao iniciar teste", false)
            showToast("Falha ao iniciar o teste: ${error.message?.take(50)}")
            renderCurrentScreen()
        }
    }

    // ── Checagens de Sistema ──

    private fun shopeeInstalled(): Boolean = try {
        packageManager.getPackageInfo("com.shopee.br", 0)
        true
    } catch (_: PackageManager.NameNotFoundException) {
        false
    }

    private fun accessibilityReady(): Boolean {
        val component = ComponentName(this, HydraAccessibilityService::class.java).flattenToString()
        val enabled = Settings.Secure.getString(
            contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ).orEmpty()
        return enabled.split(':').any { it.equals(component, ignoreCase = true) }
            && HydraAccessibilityService.current() != null
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}
