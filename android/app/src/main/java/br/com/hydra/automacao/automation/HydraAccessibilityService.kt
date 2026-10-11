package br.com.hydra.automacao.automation

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.Bitmap
import android.graphics.Path
import android.graphics.Rect
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.view.accessibility.AccessibilityWindowInfo
import java.lang.ref.WeakReference
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

private const val TAG_ACTIONS = "HydraAcoes"

data class ShopeeToast(val text: String, val timestamp: Long)

class HydraAccessibilityService : AccessibilityService() {
    companion object {
        @Volatile private var active: WeakReference<HydraAccessibilityService>? = null
        @Volatile var foregroundPackage: String = ""
            private set
        @Volatile var foregroundActivity: String = ""
            private set
        @Volatile var lastToast: ShopeeToast? = null
            private set
        fun current(): HydraAccessibilityService? = active?.get()
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        active = WeakReference(this)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        when (event?.eventType) {
            AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED -> {
                val pkg = event.packageName?.toString().orEmpty()
                if (pkg.startsWith("com.shopee")) {
                    foregroundPackage = pkg
                    foregroundActivity = event.className?.toString().orEmpty()
                } else if (pkg.isNotEmpty()) {
                    foregroundPackage = pkg
                    foregroundActivity = event.className?.toString().orEmpty()
                }
            }
            AccessibilityEvent.TYPE_NOTIFICATION_STATE_CHANGED -> {
                val pkg = event.packageName?.toString().orEmpty()
                if (pkg.startsWith("com.shopee")) {
                    val text = event.text?.joinToString(" ")?.trim().orEmpty()
                    if (text.isNotEmpty()) {
                        lastToast = ShopeeToast(text, System.currentTimeMillis())
                    }
                }
            }
        }
    }

    override fun onInterrupt() = Unit

    override fun onDestroy() {
        active = null
        foregroundPackage = ""
        foregroundActivity = ""
        lastToast = null
        super.onDestroy()
    }

    fun roots(): List<AccessibilityNodeInfo> {
        // A tela de publicação usa um plugin dinâmico com pacote diferente.
        // Aceita tanto "com.shopee.br" quanto qualquer subpacote "com.shopee.br.*"
        val windowRoots = windows.orEmpty()
            .filter { it.type == AccessibilityWindowInfo.TYPE_APPLICATION }
            .mapNotNull { it.root }
            .filter { it.packageName?.toString()?.startsWith("com.shopee") == true }
        return windowRoots.ifEmpty {
            listOfNotNull(rootInActiveWindow)
                .filter { it.packageName?.toString()?.startsWith("com.shopee") == true }
        }
    }

    fun findText(text: String): AccessibilityNodeInfo? = roots().asSequence()
        .flatMap { it.findAccessibilityNodeInfosByText(text).asSequence() }
        .firstOrNull { it.isVisibleToUser }

    fun findResourceId(id: String): AccessibilityNodeInfo? = roots().asSequence()
        .flatMap { it.findAccessibilityNodeInfosByViewId(id).asSequence() }
        .firstOrNull { it.isVisibleToUser }

    fun findDescription(description: String): AccessibilityNodeInfo? {
        fun walk(node: AccessibilityNodeInfo?): AccessibilityNodeInfo? {
            if (node == null) return null
            if (node.isVisibleToUser && node.contentDescription?.toString()?.contains(description, ignoreCase = true) == true) return node
            for (index in 0 until node.childCount) walk(node.getChild(index))?.let { return it }
            return null
        }
        return roots().firstNotNullOfOrNull { walk(it) }
    }

    fun click(node: AccessibilityNodeInfo): Boolean {
        val bounds = Rect().also { node.getBoundsInScreen(it) }
        Log.d(TAG_ACTIONS, "click: ${node.className} text='${node.text}' " +
            "clickable=${node.isClickable} enabled=${node.isEnabled} bounds=$bounds")

        // Sobe até 6 níveis procurando um ancestral clicável.
        // IMPORTANTE: se performAction retorna false, PARA (não continua subindo).
        var ancestor: AccessibilityNodeInfo? = node
        for (level in 0..5) {
            if (ancestor == null) break
            if (ancestor.isClickable) {
                val ok = ancestor.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                Log.d(TAG_ACTIONS, "  ACTION_CLICK em ${ancestor.className} (nível $level) -> $ok")
                if (ok) return true
                // performAction falhou — para de subir, vai direto pro gesto
                break
            }
            ancestor = ancestor.parent
        }

        // Fallback: gesto no centro do bounding rect do nó original.
        Log.d(TAG_ACTIONS, "  caindo pro gesto no centro")
        return gestureClick(node)
    }

    /**
     * Tap por gesto direto no centro do nó, sem tentar ACTION_CLICK.
     * Usar quando ACTION_CLICK é ignorado pela Shopee (React Native / Flutter / WebView).
     */
    fun gestureClick(node: AccessibilityNodeInfo): Boolean {
        val bounds = Rect().also { node.getBoundsInScreen(it) }
        if (bounds.isEmpty) {
            Log.w(TAG_ACTIONS, "gestureClick: bounds vazios em ${node.className}")
            return false
        }
        val x = bounds.centerX().toFloat()
        // Ajuste de borda inferior: se o nó está a menos de 24px da base da tela,
        // usa top + max(height/4, 8) para não tocar fora da área visível.
        val screenH = screenHeight()
        val y = if (screenH > 0 && bounds.bottom >= screenH - 24) {
            (bounds.top + maxOf(bounds.height() / 4, 8)).toFloat()
        } else {
            bounds.centerY().toFloat()
        }
        return tap(x, y)
    }

    /**
     * Tap por coordenada usando dispatchGesture com ponto estático de 140ms.
     */
    fun tap(x: Float, y: Float): Boolean {
        val path = Path().apply { moveTo(x, y) }
        return dispatchGesture(
            GestureDescription.Builder()
                .addStroke(GestureDescription.StrokeDescription(path, 0L, 140L))
                .build(),
            null, null,
        )
    }

    /** Altura real da tela em pixels (equivalente ao w() do C0454u). */
    private fun screenHeight(): Int =
        android.content.res.Resources.getSystem().displayMetrics.heightPixels

    fun setText(node: AccessibilityNodeInfo, value: String): Boolean {
        val args = Bundle().apply { putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, value) }
        return node.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args)
    }

    /** Optional visual fallback, available from Android 11. Never saved to disk. */
    fun screenshot(timeoutMs: Long = 2_000): Bitmap? {
        if (Build.VERSION.SDK_INT < 30) return null
        var bitmap: Bitmap? = null
        val done = CountDownLatch(1)
        takeScreenshot(0, mainExecutor, object : TakeScreenshotCallback {
            override fun onSuccess(screenshot: ScreenshotResult) {
                bitmap = Bitmap.wrapHardwareBuffer(screenshot.hardwareBuffer, screenshot.colorSpace)
                screenshot.hardwareBuffer.close()
                done.countDown()
            }

            override fun onFailure(errorCode: Int) { done.countDown() }
        })
        done.await(timeoutMs, TimeUnit.MILLISECONDS)
        return bitmap
    }

    fun back(): Boolean = performGlobalAction(GLOBAL_ACTION_BACK)

    /** Encontra o nó que contém um determinado texto no atributo text. Case-insensitive. */
    fun findTextContaining(substring: String): AccessibilityNodeInfo? =
        roots().asSequence()
            .flatMap { it.findAccessibilityNodeInfosByText(substring).asSequence() }
            .firstOrNull { it.isVisibleToUser }

    /** Coleta todos os textos visíveis na tela atual da Shopee. Usado para diagnóstico e validação. */
    fun dumpTexts(maxChars: Int = 150): String {
        val sb = StringBuilder()
        fun walk(node: AccessibilityNodeInfo?, depth: Int) {
            if (node == null || depth > 20) return
            val text = node.text?.toString()?.trim()
            val desc = node.contentDescription?.toString()?.trim()
            if (!text.isNullOrEmpty()) sb.append(text).append('\n')
            else if (!desc.isNullOrEmpty()) sb.append("[${desc}]").append('\n')
            for (i in 0 until node.childCount) walk(node.getChild(i), depth + 1)
        }
        roots().forEach { walk(it, 0) }
        val full = sb.toString()
        return if (full.length <= maxChars) full else full.take(maxChars) + "…"
    }

    /** Verifica se algum nó visível contém o texto fornecido (case-insensitive). */
    fun screenContains(substring: String, ignoreCase: Boolean = true): Boolean {
        fun walk(node: AccessibilityNodeInfo?): Boolean {
            if (node == null) return false
            if (node.isVisibleToUser) {
                if (node.text?.contains(substring, ignoreCase) == true) return true
                if (node.contentDescription?.contains(substring, ignoreCase) == true) return true
            }
            for (i in 0 until node.childCount) if (walk(node.getChild(i))) return true
            return false
        }
        return roots().any { walk(it) }
    }

    /** Swipe vertical na tela. Padrão: fracFrom=0.72, fracTo=0.30. */
    fun swipeVertical(fracFrom: Float = 0.72f, fracTo: Float = 0.30f, durationMs: Long = 450): Boolean {
        val display = android.content.res.Resources.getSystem().displayMetrics
        val centerX = display.widthPixels / 2f
        val fromY = display.heightPixels * fracFrom
        val toY = display.heightPixels * fracTo
        val path = Path().apply { moveTo(centerX, fromY); lineTo(centerX, toY) }
        return dispatchGesture(
            GestureDescription.Builder()
                .addStroke(GestureDescription.StrokeDescription(path, 0L, durationMs))
                .build(),
            null, null,
        )
    }

    /** Toca numa posição fracionada da tela (fracX, fracY em 0..1). */
    fun tapFraction(fracX: Float, fracY: Float): Boolean {
        val display = android.content.res.Resources.getSystem().displayMetrics
        return tap(display.widthPixels * fracX, display.heightPixels * fracY)
    }

    /** Aguarda (com pool) até que `probe` retorne não-nulo, dentro do timeout. */
    fun <T> waitUntil(timeoutMs: Long, pollMs: Long = 300, probe: () -> T?): T? {
        val deadline = android.os.SystemClock.elapsedRealtime() + timeoutMs
        while (!Thread.currentThread().isInterrupted && android.os.SystemClock.elapsedRealtime() < deadline) {
            probe()?.let { return it }
            android.os.SystemClock.sleep(pollMs)
        }
        return null
    }

    /** Aguarda toast da Shopee contendo `substring` emitido após `afterTimestamp`. */
    fun waitForToast(substring: String, timeoutMs: Long, afterTimestamp: Long): Boolean {
        val deadline = android.os.SystemClock.elapsedRealtime() + timeoutMs
        while (!Thread.currentThread().isInterrupted && android.os.SystemClock.elapsedRealtime() < deadline) {
            val t = lastToast
            if (t != null && t.timestamp >= afterTimestamp && t.text.contains(substring, ignoreCase = true)) {
                return true
            }
            android.os.SystemClock.sleep(200)
        }
        return false
    }

    /**
     * Aguarda a árvore de acessibilidade estabilizar — duas leituras consecutivas
     * com o mesmo número de nós e o mesmo foreground package, separadas por [checkMs].
     *
     * Garante que uma transição de tela terminou antes de tentar clicar.
     */
    fun waitStable(
        timeoutMs: Long = 4_000,
        checkMs: Long = 600,
        requirePackage: String? = null,
    ): Boolean {
        val deadline = android.os.SystemClock.elapsedRealtime() + timeoutMs
        var prevSignature = ""
        while (!Thread.currentThread().isInterrupted && android.os.SystemClock.elapsedRealtime() < deadline) {
            val pkg = foregroundPackage
            val nodeCount = roots().sumOf { countNodes(it) }
            val sig = "$pkg|$nodeCount"
            if (sig == prevSignature && nodeCount > 0) {
                // Estabilizou — verifica pacote se exigido
                return requirePackage == null || pkg.startsWith(requirePackage)
            }
            prevSignature = sig
            android.os.SystemClock.sleep(checkMs)
        }
        return false
    }

    private fun countNodes(node: AccessibilityNodeInfo?, depth: Int = 0): Int {
        if (node == null || depth > 20) return 0
        var count = if (node.isVisibleToUser) 1 else 0
        for (i in 0 until node.childCount) count += countNodes(node.getChild(i), depth + 1)
        return count
    }
}
