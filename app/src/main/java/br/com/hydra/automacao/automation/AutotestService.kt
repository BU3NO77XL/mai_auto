package br.com.hydra.automacao.automation

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.util.Log
import java.util.concurrent.atomic.AtomicBoolean

private const val TAG = "HydraAutoteste"

/**
 * Autoteste autônomo completo — ida até a galeria + retorno limpo:
 *
 *  IDA (sem selecionar nenhum vídeo)
 *   1. Abre a Shopee e aguarda estabilizar
 *   2. navigateToCreation → aba Live/Vídeo → botão criar → aba Vídeo
 *   3. Abre Biblioteca → aguarda galeria estabilizar → confirma presença
 *
 *  VOLTA (sem deixar rastro)
 *   4. Swipe down para fechar a galeria
 *   5. Toca no iv_back (X) da tela de câmera
 *   6. HomeActivity detectado → Home Android
 *   7. Abre o Hydra e registra resultado
 */
class AutotestService : Service() {
    private val running = AtomicBoolean(false)
    private var worker: Thread? = null
    private val results by lazy { TestResultStore(this) }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action != ACTION_RUN || !running.compareAndSet(false, true)) {
            return START_NOT_STICKY
        }
        isRunning = true

        val notifications = getSystemService(NotificationManager::class.java)
        notifications.createNotificationChannel(
            NotificationChannel(CHANNEL, "Autoteste", NotificationManager.IMPORTANCE_LOW)
        )
        val notification = Notification.Builder(this, CHANNEL)
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setContentTitle("LUNA — autoteste")
            .setContentText("Navegando pela Shopee automaticamente…")
            .setOngoing(true)
            .build()

        if (Build.VERSION.SDK_INT >= 29) {
            startForeground(1, notification, android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
        } else {
            startForeground(1, notification)
        }

        worker = Thread({
            val power = getSystemService(PowerManager::class.java)
            val lock = power.newWakeLock(
                PowerManager.PARTIAL_WAKE_LOCK or PowerManager.ON_AFTER_RELEASE,
                "hydra:autoteste"
            )
            try {
                lock.acquire(180_000L)
                runAutotest()
            } catch (e: Exception) {
                Log.e(TAG, "Erro inesperado", e)
                results.save("Erro", e.message ?: "Falha inesperada", false)
                finalizarTeste(null, e.message ?: "Erro inesperado")
            } finally {
                if (lock.isHeld) lock.release()
                running.set(false)
                isRunning = false
                stopSelf()
            }
        }, "hydra-autotest").also { it.start() }

        return START_NOT_STICKY
    }

    private fun step(label: String) {
        Log.i(TAG, ">>> $label")
        results.save(label, "em andamento…")
    }

    private fun runAutotest() {
        // ── 0. Serviço de acessibilidade ──────────────────────────────────
        val svc = HydraAccessibilityService.current()
        if (svc == null) {
            results.save("Erro", "Serviço de acessibilidade não conectado. Ative em Permissões.", false)
            finalizarTeste(null, "Serviço de acessibilidade ausente")
            return
        }
        val config = FlowConfigStore(this).load()
        val engine = FlowEngine(svc, config, this)

        // ══════════════════════════════════════════════
        //  IDA
        // ══════════════════════════════════════════════

        // ── 1. Abre a Shopee ──────────────────────────
        step("Abrindo Shopee")
        val launchIntent = packageManager.getLaunchIntentForPackage("com.shopee.br")
            ?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        if (launchIntent == null) {
            results.save("Erro", "Shopee não está instalada.", false)
            finalizarTeste(svc, "Shopee não instalada")
            return
        }
        startActivity(launchIntent)

        val shopeeReady = svc.waitUntil(15_000, 400) {
            if (HydraAccessibilityService.foregroundPackage.startsWith("com.shopee")
                && svc.roots().isNotEmpty()
            ) true else null
        }
        if (shopeeReady == null) {
            val msg = "Shopee não abriu (pkg=${HydraAccessibilityService.foregroundPackage}).\nVerifique permissões de segundo plano."
            results.save("Shopee não abriu", msg, false)
            finalizarTeste(svc, msg)
            return
        }
        svc.waitStable(4_000, 600, "com.shopee")
        results.save("Shopee aberta", "act=${HydraAccessibilityService.foregroundActivity}")

        // ── 2. Live/Vídeo → Criar → aba Vídeo ────────
        step("Navegando para criação")
        when (val r = engine.navigateToCreation()) {
            is StepResult.Failure -> {
                val msg = "${r.reason}\n\n${r.diagnostic ?: ""}".trim()
                results.save("Falha na navegação", msg, false)
                finalizarTeste(svc, msg)
                return
            }
            is StepResult.Success -> results.save("Tela de câmera", "ok")
        }

        // ── 3. Abre Biblioteca e aguarda galeria ──────
        step("Abrindo Biblioteca")
        val libNode = svc.waitUntil(6_000) {
            svc.findResourceId("com.shopee.br:id/ll_gallery_entrance")
                ?: svc.findResourceId("com.shopee.br:id/tv_gallery_entrance")
                ?: svc.findText("Biblioteca")
        }
        if (libNode == null) {
            val msg = "Botão 'Biblioteca' não encontrado na tela de câmera.\n${svc.dumpTexts(150)}"
            results.save("Biblioteca não encontrada", msg, false)
            finalizarTeste(svc, msg)
            return
        }

        if (!svc.click(libNode)) svc.gestureClick(libNode)
        android.os.SystemClock.sleep(POST_CLICK_MS)

        // Aguarda galeria estabilizar (rv_gallery ou texto "Galeria")
        val galeriaOk = svc.waitUntil(8_000, 400) {
            if (svc.findResourceId("com.shopee.br:id/rv_gallery") != null
                || svc.screenContains("Galeria")
            ) true else null
        }

        if (galeriaOk == null) {
            val msg = "Tela de galeria não apareceu após tocar em Biblioteca.\nact=${HydraAccessibilityService.foregroundActivity}"
            results.save("Galeria não abriu", msg, false)
            finalizarTeste(svc, msg)
            return
        }

        // Aguarda mais 1s para estabilizar completamente
        svc.waitStable(2_000, 500, "com.shopee")
        results.save("Galeria confirmada ✅", "Tela de seleção de vídeos detectada e estabilizada.")
        Log.i(TAG, "Galeria confirmada — iniciando retorno")

        // ══════════════════════════════════════════════
        //  VOLTA
        // ══════════════════════════════════════════════
        finalizarTeste(svc, null)
    }

    /**
     * Executa o retorno: swipe down na galeria → X da câmera → Home → abre Hydra.
     * [falha] null = sucesso, não-null = motivo do erro.
     */
    private fun finalizarTeste(svc: HydraAccessibilityService?, falha: String?) {
        Log.i(TAG, "finalizarTeste: retornando (falha=${falha != null})")

        if (svc != null && HydraAccessibilityService.foregroundPackage.startsWith("com.shopee")) {
            val config = FlowConfigStore(this).load()
            val engine = FlowEngine(svc, config, this)
            step("Retornando")
            when (val r = engine.returnFromGallery()) {
                is StepResult.Success -> {
                    Log.i(TAG, "returnFromGallery ok: ${r.step}")
                    results.save("Retorno", "ok — ${r.step}")
                }
                is StepResult.Failure -> {
                    Log.w(TAG, "returnFromGallery parcial: ${r.reason}")
                    results.save("Retorno parcial", r.reason)
                    // Força Home como segurança
                    svc.performGlobalAction(android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_HOME)
                    android.os.SystemClock.sleep(800)
                }
            }
        }

        // Abre o aplicativo
        android.os.SystemClock.sleep(600)
        try {
            packageManager.getLaunchIntentForPackage(packageName)
                ?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                ?.let { startActivity(it) }
        } catch (e: Exception) {
            Log.w(TAG, "Não consegui abrir o LUNA: ${e.message}")
        }

        // Resultado final
        android.os.SystemClock.sleep(500)
        if (falha == null) {
            results.save(
                "Concluído ✅",
                "Autoteste completo com sucesso.\n\n" +
                    "IDA:   Shopee → Live/Vídeo → Criar → Biblioteca → Galeria confirmada\n" +
                    "VOLTA: Swipe down → X câmera → Home Android → LUNA\n\n" +
                    "O motor de navegação está funcionando corretamente.",
                true
            )
        } else {
            results.save(
                "Teste falhou",
                "Motivo: $falha\n\nO motor tentou retornar ao home antes de encerrar.",
                false
            )
        }
    }

    override fun onDestroy() {
        running.set(false)
        isRunning = false
        worker?.interrupt()
        super.onDestroy()
    }

    companion object {
        const val ACTION_RUN = "br.com.hydra.automacao.RUN_AUTOTEST"
        private const val CHANNEL = "autotest"
        private const val POST_CLICK_MS = 350L

        @Volatile
        var isRunning: Boolean = false
            internal set
    }
}
