package br.com.hydra.automacao.automation

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.os.SystemClock
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean

private const val TAG = "HydraProducao"

/**
 * Serviço de foreground que executa o fluxo completo de produção:
 * abertura → criação → galeria → vídeo → legenda → produtos → publicação.
 */
class ProductionFlowService : Service() {
    private val running = AtomicBoolean(false)
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private val results by lazy { TestResultStore(this) }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action != ACTION_RUN || !running.compareAndSet(false, true)) {
            return START_NOT_STICKY
        }
        isRunning = true

        val jobId = intent.getStringExtra(EXTRA_JOB_ID) ?: run {
            Log.w(TAG, "Iniciado sem job_id — encerrando")
            running.set(false)
            isRunning = false
            stopSelf(startId)
            return START_NOT_STICKY
        }

        val caption = intent.getStringExtra(EXTRA_CAPTION) ?: ""
        val productLinks = intent.getStringArrayListExtra(EXTRA_PRODUCTS) ?: emptyList()
        val isDraft = intent.getStringExtra(EXTRA_MODE)?.equals("rascunho", ignoreCase = true) ?: false
        val useAiLabel = intent.getBooleanExtra(EXTRA_AI_LABEL, false)

        Log.i(TAG, "ProductionFlowService iniciado: jobId=$jobId produtos=${productLinks.size} modo=${if (isDraft) "rascunho" else "publicar"}")

        setupNotification(caption, isDraft)

        scope.launch {
            val power = getSystemService(PowerManager::class.java)
            val lock = power.newWakeLock(
                PowerManager.PARTIAL_WAKE_LOCK or PowerManager.ON_AFTER_RELEASE,
                "hydra:producao"
            )
            try {
                lock.acquire(10 * 60 * 1_000L) // 10 minutos máximo
                runProduction(caption, productLinks, isDraft, useAiLabel)
            } catch (e: Exception) {
                Log.e(TAG, "Erro inesperado na produção", e)
                results.save("Erro", e.message ?: "Falha inesperada", false)
            } finally {
                if (lock.isHeld) lock.release()
                running.set(false)
                isRunning = false
                stopSelf(startId)
            }
        }

        return START_NOT_STICKY
    }

    private fun setupNotification(caption: String, isDraft: Boolean) {
        val notifications = getSystemService(NotificationManager::class.java)
        val channel = NotificationChannel(
            CHANNEL_PRODUCTION,
            "Automação em produção",
            NotificationManager.IMPORTANCE_LOW
        )
        notifications.createNotificationChannel(channel)

        val text = if (caption.isNotBlank()) {
            "Publicando: ${caption.take(60)}${if (caption.length > 60) "…" else ""}"
        } else {
            "Publicando seu vídeo…"
        }

        val notification = Notification.Builder(this, CHANNEL_PRODUCTION)
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setContentTitle("LUNA trabalhando")
            .setContentText(text)
            .setOngoing(true)
            .build()

        if (Build.VERSION.SDK_INT >= 29) {
            startForeground(NOTIFICATION_ID, notification, android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    /**
     * Orquestra o fluxo completo de produção.
     */
    private suspend fun runProduction(caption: String, productLinks: List<String>, isDraft: Boolean, useAiLabel: Boolean) {
        val config = FlowConfigStore(this).load()
        config.validate()?.let {
            results.save("Configuração", it, false)
            return
        }

        // Aguarda o serviço de acessibilidade estar pronto
        results.save("Preparação", "Aguardando controle de tela")
        val service = waitForService(10_000)
        if (service == null) {
            results.save(
                "Acessibilidade",
                "O serviço de acessibilidade não está conectado. Ative o controle de tela em Permissões.",
                false
            )
            return
        }

        val engine = FlowEngine(service, config, this)

        // ── 1. Abrir Shopee ──
        results.save("Abrindo Shopee", "Iniciando aplicativo")
        when (val result = engine.openShopee()) {
            is StepResult.Failure -> {
                results.save("Abertura", result.reason, false)
                return
            }
            else -> results.save("Shopee aberta", "App em foco")
        }

        // ── 2. Navegar para criação (se implementado o seletor) ──
        // Aqui será adicionada a navegação completa para tela de criação quando
        // os seletores de "botaoMais" / "botaoVideo" estiverem configurados.
        // Por ora, assume que já está na tela certa após abrir.

        // ── 3. Abrir galeria ──
        results.save("Galeria", "Abrindo seleção de vídeo")
        when (val result = engine.openGallery()) {
            is StepResult.Failure -> {
                results.save("Galeria", result.reason, false)
                return
            }
            else -> results.save("Galeria aberta", "Aguardando seleção de vídeo")
        }

        // ── 4. Selecionar vídeo ──
        // Aguarda usuário selecionar (ou implementar lógica de seleção automática futura)
        delay(5_000) // timeout para seleção manual
        results.save("Vídeo selecionado", "Aguardando tela de edição")

        // Aguarda tela de edição aparecer
        delay(3_000)

        // ── 5. Preencher legenda ──
        if (caption.isNotBlank()) {
            results.save("Legenda", "Preenchendo campo")
            when (val result = engine.fillCaption(caption)) {
                is StepResult.Failure -> {
                    results.save("Legenda", result.reason, false)
                    return
                }
                else -> results.save("Legenda preenchida", caption.take(50))
            }
        }

        // ── 5b. Rótulo de conteúdo gerado por IA ──
        results.save("Rótulo IA", if (useAiLabel) "Ligando rótulo de IA" else "Mantendo rótulo IA desligado")
        when (val result = engine.setAiLabel(useAiLabel)) {
            is StepResult.Failure -> Log.w(TAG, "setAiLabel: ${result.reason}") // não bloqueia
            else -> {}
        }

        // ── 6. Anexar produtos ──
        if (productLinks.isNotEmpty()) {
            results.save("Produtos", "Anexando ${productLinks.size} produto(s)")
            productLinks.forEachIndexed { index, url ->
                val isFirst = index == 0
                results.save("Produto ${index + 1}/${productLinks.size}", "Favoritando $url")
                when (val result = engine.attachProduct(url, isFirst)) {
                    is StepResult.Failure -> {
                        results.save("Produto ${index + 1}", result.reason, false)
                        Log.e(TAG, "Parou de anexar no produto ${index + 1}: ${result.reason}")
                        // Decide se continua ou aborta
                        if (!result.userAction) return // erro crítico
                        // Se for erro do produto, pode tentar o próximo
                    }
                    else -> {
                        results.save("Produto ${index + 1} anexado", "Ok")
                        delay(1_500) // pausa entre produtos
                    }
                }
            }
        }

        // ── 7. Publicar ou salvar rascunho ──
        val mode = if (isDraft) "rascunho" else "publicação"
        results.save("Finalizando", "Aguardando botão de $mode")
        when (val result = engine.publish(isDraft)) {
            is StepResult.Failure -> {
                results.save("Publicação", result.reason, false)
                return
            }
            is StepResult.Success -> {
                results.save("Concluído", result.step, true)
                Log.i(TAG, "Fluxo concluído com sucesso: ${result.step}")
            }
        }
    }

    private suspend fun waitForService(timeoutMs: Long): HydraAccessibilityService? {
        val deadline = SystemClock.elapsedRealtime() + timeoutMs
        while (SystemClock.elapsedRealtime() < deadline) {
            HydraAccessibilityService.current()?.let { return it }
            delay(300)
        }
        return null
    }

    override fun onDestroy() {
        running.set(false)
        isRunning = false
        scope.cancel()
        super.onDestroy()
    }

    companion object {
        const val ACTION_RUN = "br.com.hydra.automacao.RUN_PRODUCTION"
        const val EXTRA_JOB_ID = "job_id"
        const val EXTRA_CAPTION = "legenda"
        const val EXTRA_PRODUCTS = "produtos"
        const val EXTRA_MODE = "modo"       // "publicar" ou "rascunho"
        const val EXTRA_AI_LABEL = "rotulo_ia" // boolean — ligar rótulo IA
        private const val CHANNEL_PRODUCTION = "production"
        private const val NOTIFICATION_ID = 2

        @Volatile
        var isRunning: Boolean = false
            internal set
    }
}
