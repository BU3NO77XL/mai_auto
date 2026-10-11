package br.com.hydra.automacao.automation

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.SystemClock
import android.util.Log
import android.view.accessibility.AccessibilityNodeInfo

private const val TAG = "HydraFluxo"

/** Resultado de cada operação do motor de fluxo. */
sealed interface StepResult {
    data class Success(val step: String) : StepResult
    data class Failure(
        val step: String,
        val reason: String,
        val userAction: Boolean,       // true = erro que o usuário pode corrigir
        val diagnostic: String? = null // dump da tela para debug
    ) : StepResult
}

/** Motor de fluxo para navegação e automação da Shopee. */
class FlowEngine(
    private val service: HydraAccessibilityService,
    private val config: FlowConfig,
    private val context: Context,
) {
    // ────────────────────────────────────────────────────────
    // Prioridade dos seletores: resourceId > descrição > texto
    // ────────────────────────────────────────────────────────

    fun find(key: String): AccessibilityNodeInfo? {
        config.resourceIds[key]?.takeIf { it.isNotBlank() }?.let(service::findResourceId)?.let { return it }
        config.descriptions[key]?.takeIf { it.isNotBlank() }?.let(service::findDescription)?.let { return it }
        config.texts[key]?.takeIf { it.isNotBlank() }?.let(service::findText)?.let { return it }
        return null
    }

    /** Aguarda seletor aparecer na tela. */
    fun waitFor(key: String, timeoutMs: Long = 8_000): AccessibilityNodeInfo? {
        val poll = (config.pausesMs["pollRapido"] ?: 300L).coerceIn(50L, 2_000L)
        return service.waitUntil(timeoutMs, poll) { find(key) }
    }

    /** Aguarda activity específica aparecer no foreground. */
    fun awaitActivity(key: String, timeoutMs: Long = 8_000): Boolean {
        val expected = config.activities[key] ?: return false
        val poll = (config.pausesMs["pollRapido"] ?: 300L).coerceIn(50L, 2_000L)
        return service.waitUntil(timeoutMs, poll) {
            if (HydraAccessibilityService.foregroundActivity.contains(expected)) true else null
        } != null
    }

    // ────────────────────────────────────────────────────────
    // Ações atômicas
    // ────────────────────────────────────────────────────────

    /** Toca num seletor com estratégia em três camadas:
     *  1. service.click() — ACTION_CLICK subindo hierarquia (até 6 pais)
     *  2. service.gestureClick() — gesto direto no centro do nó (bypassa ACTION_CLICK)
     *  3. Repete tentativas até maxAttempts aguardando o nó reaparecer
     *
     *  Pausa POST_CLICK_MS após cada toque bem-sucedido para o app reagir.
     */
    fun tap(key: String, timeoutEach: Long = 3_000): StepResult {
        repeat(config.maxAttempts) { attempt ->
            val node = waitFor(key, timeoutEach)
            if (node == null) {
                Log.w(TAG, "tap($key): nó não encontrado na tentativa ${attempt + 1}")
                return@repeat
            }

            // 1ª camada: ACTION_CLICK com subida de hierarquia
            if (service.click(node)) {
                Log.i(TAG, "tap($key): click() ok na tentativa ${attempt + 1}")
                SystemClock.sleep(POST_CLICK_MS)
                return StepResult.Success(key)
            }

            // 2ª camada: gesto direto (bypassa ACTION_CLICK — útil para React Native / WebView)
            Log.d(TAG, "tap($key): click() falhou — tentando gestureClick()")
            if (service.gestureClick(node)) {
                Log.i(TAG, "tap($key): gestureClick() ok na tentativa ${attempt + 1}")
                SystemClock.sleep(POST_CLICK_MS)
                return StepResult.Success(key)
            }

            Log.w(TAG, "tap($key): ambas as estratégias falharam na tentativa ${attempt + 1}")
        }
        return failure(key, "Não consegui tocar em '$key' após ${config.maxAttempts} tentativa(s) — " +
            "ACTION_CLICK e gesto rejeitados")
    }

    companion object {
        /** Pausa mínima após qualquer clique bem-sucedido para o app processar a ação. */
        private const val POST_CLICK_MS = 350L
    }

    /** Preenche campo de texto. */
    fun fill(key: String, value: String): StepResult {
        val node = waitFor(key) ?: return failure(key, "Campo não encontrado")
        return if (service.setText(node, value)) StepResult.Success(key)
        else failure(key, "O campo não aceitou o texto")
    }

    /** Pausa usando o mapa de pausas do config ou o valor padrão. */
    fun pause(key: String, default: Long = 1_500) {
        val ms = (config.pausesMs[key] ?: default).coerceIn(50L, 30_000L)
        SystemClock.sleep(ms)
    }

    // ────────────────────────────────────────────────────────
    // 1. Abertura da Shopee
    // ────────────────────────────────────────────────────────

    /**
     * Abre a Shopee via Intent de lançamento e aguarda ela aparecer com
     * árvore de acessibilidade populada.
     */
    fun openShopee(): StepResult {
        val intent = context.packageManager.getLaunchIntentForPackage(config.packageName)
            ?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            ?: return StepResult.Failure("abertura", "Shopee (${config.packageName}) não está instalada", true)

        context.startActivity(intent)
        Log.i(TAG, "openShopee: Intent disparado, aguardando árvore")

        val timeoutMs = (config.pausesMs["shopeeAbrir"] ?: 10_000L).coerceIn(5_000L, 20_000L)
        val ready = service.waitUntil(timeoutMs) {
            service.takeIf {
                HydraAccessibilityService.foregroundPackage == config.packageName
                    && it.roots().isNotEmpty()
            }
        }
        return if (ready != null) {
            Log.i(TAG, "openShopee: ok — activity=${HydraAccessibilityService.foregroundActivity}")
            StepResult.Success("abertura")
        } else {
            failure("abertura", "A Shopee não apareceu com árvore acessível. Verifique permissões de segundo plano.")
        }
    }

    // ────────────────────────────────────────────────────────
    // 2. Abrir produto e navegar para a tela de publicação
    // ────────────────────────────────────────────────────────

    /**
     * Abre um link de produto da Shopee via URI e aguarda carregar.
     */
    fun openProductLink(url: String): StepResult {
        Log.i(TAG, "openProductLink: $url")
        return try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                setPackage(config.packageName)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            pause("produtoCarregar", 3_000)
            validateProductScreen()
        } catch (e: Exception) {
            failure("produto", "Não foi possível abrir o link: ${e.message}")
        }
    }

    /**
     * Valida que a tela do produto está carregada sem erros.
     */
    private fun validateProductScreen(): StepResult {
        if (service.screenContains("falhou ao carregar")) {
            return StepResult.Failure(
                "produto", "Produto com problema para carregar na Shopee — tente de novo mais tarde ou troque o link",
                userAction = true
            )
        }
        if (service.screenContains("404") && service.screenContains("something is missing")) {
            return StepResult.Failure(
                "produto", "Link quebrado ou expirado na Shopee — confira o link",
                userAction = true
            )
        }
        if (service.screenContains("Oops!!") && service.screenContains("cannot be f")) {
            return StepResult.Failure(
                "produto", "Link quebrado ou expirado na Shopee — confira o link",
                userAction = true
            )
        }
        if (service.screenContains("Saúde do Vídeo")) {
            return StepResult.Failure(
                "produto", "Conta Shopee com acesso a postar vídeos banido ou restrito (tela \"Saúde do Vídeo\")",
                userAction = true
            )
        }
        return StepResult.Success("produto carregado")
    }

    // ────────────────────────────────────────────────────────
    // 3. Fechar folha de variações
    // ────────────────────────────────────────────────────────

    /**
     * Detecta e fecha a folha inferior de variações se estiver aberta.
     */
    fun closeVariationSheetIfOpen(): Boolean {
        val hasVariation = config.texts["folhaVariacoes"]
            ?.let { service.screenContains(it) }
            ?: run {
                // fallback: detecta pelo padrão visual (node com role de bottom sheet)
                service.findDescription("Fechar") != null
                    && service.screenContains("Variações")
            }

        return if (hasVariation) {
            Log.i(TAG, "closeVariationSheetIfOpen: folha detectada — fechando com Voltar")
            service.back()
            pause("curta", 2_000)
            true
        } else false
    }

    // ────────────────────────────────────────────────────────
    // 4. Favoritar produto (coração)
    // ────────────────────────────────────────────────────────

    /**
     * Verifica se o produto já está favoritado lendo o estado do nó coração.
     */
    private fun isAlreadyFavorited(): Boolean {
        val heart = find("botaoFavoritar") ?: return false
        val desc = heart.contentDescription?.toString()?.lowercase() ?: ""
        val text = heart.text?.toString()?.lowercase() ?: ""
        // Estado "preenchido/ativo" = já favoritado
        return desc.contains("remov") || desc.contains("adicionad") || desc.contains("salvo")
            || text.contains("remov") || text.contains("adicionad")
    }

    /**
     * Desfavorita o produto e valida via toast.
     * Retorna false se não conseguiu validar.
     */
    private fun unfavorite(heart: AccessibilityNodeInfo): Boolean {
        val ts = System.currentTimeMillis()
        clickNode(heart, "desfavoritar")
        pause("curta", 1_500)
        val validated = service.waitForToast("removid", 2_500, ts)
        if (!validated) Log.w(TAG, "unfavorite: toast de remoção não detectado")
        return validated
    }

    /**
     * Favorita o produto e valida via toast ou estado do nó.
     */
    private fun favoriteAndValidate(heart: AccessibilityNodeInfo): Boolean {
        val ts = System.currentTimeMillis()
        clickNode(heart, "favoritar")
        pause("curta", 2_000)
        // Valida pelo toast primeiro
        if (service.waitForToast("adicionad", 2_500, ts)) return true
        // Fallback: verifica estado do nó
        val refreshed = find("botaoFavoritar")
        val desc = refreshed?.contentDescription?.toString()?.lowercase() ?: ""
        return desc.contains("adicionad") || desc.contains("salvo")
    }

    /**
     * Fluxo completo de anexação de um produto:
     * abre link → valida tela → fecha variações → favorita (com retry).
     *
     * [isFirst] true para o primeiro produto (usa Intent de link),
     *           false para os demais (já está na tela certa após o anterior).
     */
    fun attachProduct(url: String, isFirst: Boolean): StepResult {
        Log.i(TAG, "attachProduct: isFirst=$isFirst url=$url")

        if (isFirst) {
            val openResult = openProductLink(url)
            if (openResult is StepResult.Failure) return openResult
        } else {
            pause("produtoCarregar", 3_000)
            val validResult = validateProductScreen()
            if (validResult is StepResult.Failure) return validResult
        }

        // Fecha variações se aparecer
        closeVariationSheetIfOpen()

        // Localiza coração
        var heart = find("botaoFavoritar")
            ?: run {
                // Se não achou ainda, aguarda mais um momento
                pause("curta", 1_500)
                find("botaoFavoritar")
            }
            ?: return failure("favoritar", "Botão de favoritar não encontrado. Verifique os seletores em Ajustes.")

        val alreadyFavorited = isAlreadyFavorited()
        Log.i(TAG, "attachProduct: alreadyFavorited=$alreadyFavorited")

        if (alreadyFavorited) {
            Log.i(TAG, "attachProduct: produto já favoritado — desfavoritando pra refavoritar")
            val unfavorited = unfavorite(heart)
            if (!unfavorited) {
                // Reabre o link do zero antes de desistir
                Log.w(TAG, "attachProduct: desfavoritar falhou — reabrindo link")
                val retry = openProductLink(url)
                if (retry is StepResult.Failure) return retry
                closeVariationSheetIfOpen()
                heart = find("botaoFavoritar")
                    ?: return failure("favoritar", "Botão de favoritar desapareceu após reabrir o link")
            }
        }

        val succeeded = favoriteAndValidate(heart)
        if (!succeeded) {
            return failure("favoritar", "Não consegui confirmar a adição aos favoritos — tentei ${config.maxAttempts}x")
        }

        // Volta para tela de publicação
        service.back()
        pause("curta", 1_500)
        Log.i(TAG, "attachProduct: produto anexado com sucesso")
        return StepResult.Success("produto anexado")
    }

    // ────────────────────────────────────────────────────────
    // NAVEGAÇÃO AUTÔNOMA — tasks 2/3/4
    // ────────────────────────────────────────────────────────

    /**
     * Navega do home da Shopee até a tela de câmera/criação de vídeo:
     *   aba "Live e Vídeo" → botão criar (canto sup. direito) → aba "Vídeo"
     *
     * Seletores usados (mapeados via ADB):
     *   tab_bar_button_video_and_live  (content-desc)
     *   click top right create icon   (content-desc)
     *   tv_tab com text="Vídeo"        (resourceId + text)
     */
    fun navigateToCreation(): StepResult {
        Log.i(TAG, "navigateToCreation: step 1 — aba Live e Vídeo")

        // 1. Toca na aba "Live e Vídeo" na barra inferior
        val tabNode = service.waitUntil(6_000) {
            service.findDescription("tab_bar_button_video_and_live")
        } ?: return failure("nav_aba", "Aba 'Live e Vídeo' não encontrada. Verifique se está no home da Shopee.")

        if (!clickNode(tabNode, "abaLiveVideo")) {
            return failure("nav_aba", "Não foi possível tocar na aba 'Live e Vídeo'.")
        }
        service.waitStable(4_000, 600, "com.shopee")
        Log.i(TAG, "navigateToCreation: step 2 — botão criar")

        // 2. Toca no botão criar (ícone topo direito — content-desc "click top right create icon")
        val createNode = service.waitUntil(6_000) {
            service.findDescription("click top right create icon")
        } ?: return failure("nav_criar", "Botão criar não encontrado na tela Live/Vídeo.")

        if (!clickNode(createNode, "botaoCriar")) {
            return failure("nav_criar", "Não foi possível tocar no botão criar.")
        }
        service.waitStable(5_000, 600, "com.shopee")
        Log.i(TAG, "navigateToCreation: step 3 — aba Vídeo")

        // 3. Seleciona a aba "Vídeo" na tela de câmera
        //    resource-id: com.shopee.br:id/tv_tab, text="Vídeo"
        val videoTabNode = service.waitUntil(5_000) {
            service.findResourceId("com.shopee.br:id/tv_tab")?.takeIf {
                it.text?.toString() == "Vídeo"
            } ?: service.findText("Vídeo")
        }
        if (videoTabNode != null) {
            clickNode(videoTabNode, "abaVideo")
            SystemClock.sleep(600)
        } else {
            Log.w(TAG, "navigateToCreation: aba 'Vídeo' não encontrada — continuando")
        }

        Log.i(TAG, "navigateToCreation: tela de câmera pronta")
        return StepResult.Success("telaCriacao")
    }

    /**
     * Na tela de câmera, toca em "Biblioteca", aguarda estabilizar,
     * toca no primeiro vídeo da grade e toca em "Próximo".
     *
     * Seletores:
     *   ll_gallery_entrance / tv_gallery_entrance  → botão Biblioteca
     *   iv_picture                                  → primeiro vídeo da grade
     *   tv_pick_next (text="Próximo")               → avança
     */
    fun openGalleryAndSelectFirst(): StepResult {
        Log.i(TAG, "openGalleryAndSelectFirst: step 1 — Biblioteca")

        // 1. Toca em Biblioteca
        val libNode = service.waitUntil(6_000) {
            service.findResourceId("com.shopee.br:id/ll_gallery_entrance")
                ?: service.findResourceId("com.shopee.br:id/tv_gallery_entrance")
                ?: service.findText("Biblioteca")
        } ?: return failure("galeria_entrada", "Botão 'Biblioteca' não encontrado na tela de câmera.")

        if (!clickNode(libNode, "Biblioteca")) {
            return failure("galeria_entrada", "Não foi possível tocar em 'Biblioteca'.")
        }
        service.waitStable(5_000, 600, "com.shopee")
        Log.i(TAG, "openGalleryAndSelectFirst: step 2 — galeria aberta, selecionando primeiro vídeo")

        // 2. Valida que a galeria abriu (título "Galeria")
        val galeriaTitle = service.waitUntil(5_000) {
            service.findResourceId("com.shopee.br:id/tv_pick_title")
                ?.takeIf { it.text?.toString()?.contains("Galeria") == true }
                ?: service.findText("Galeria")
        }
        if (galeriaTitle == null) {
            Log.w(TAG, "openGalleryAndSelectFirst: título 'Galeria' não encontrado, tentando continuar")
        }

        // 3. Toca no primeiro item da grade (iv_picture)
        val firstItem = service.waitUntil(5_000) {
            service.findResourceId("com.shopee.br:id/iv_picture")
        } ?: return failure("galeria_item", "Nenhum vídeo encontrado na galeria. Verifique se há vídeos no dispositivo.")

        if (!clickNode(firstItem, "primeiroVideo")) {
            return failure("galeria_item", "Não foi possível selecionar o primeiro vídeo.")
        }
        SystemClock.sleep(800)
        Log.i(TAG, "openGalleryAndSelectFirst: step 3 — tocando Próximo")

        // 4. Toca em "Próximo" (tv_pick_next)
        val nextBtn = service.waitUntil(5_000) {
            service.findResourceId("com.shopee.br:id/tv_pick_next")
                ?: service.findText("Próximo")
        } ?: return failure("galeria_proximo", "Botão 'Próximo' não encontrado após selecionar o vídeo.")

        if (!clickNode(nextBtn, "ProximoGaleria")) {
            return failure("galeria_proximo", "Não foi possível tocar em 'Próximo' na galeria.")
        }
        service.waitStable(5_000, 600, "com.shopee")
        Log.i(TAG, "openGalleryAndSelectFirst: vídeo selecionado, avançou para preview")
        return StepResult.Success("videoSelecionado")
    }

    /**
     * Avança pelas telas intermediárias após selecionar o vídeo:
     *   preview (bottom_container / "Próximo") → edição (tv_compress / "Próximo")
     *
     * Se já estiver na PublishVideoActivity (edição pulada pelo app), retorna sucesso.
     */
    fun advanceThroughEditing(): StepResult {
        // Se o app pulou a edição e já está na tela de publicação, sucesso direto
        if (HydraAccessibilityService.foregroundActivity.contains("PublishVideo")) {
            Log.i(TAG, "advanceThroughEditing: já na PublishVideoActivity — sem edição pendente")
            return StepResult.Success("telaPublicacao")
        }

        Log.i(TAG, "advanceThroughEditing: step 1 — Próximo no preview")

        // 1. Próximo na tela de preview (container inferior com "Próximo")
        val previewNext = service.waitUntil(8_000) {
            service.findResourceId("com.shopee.br:id/bottom_container")
                ?.let { container -> findTextInNode(container, "Próximo") ?: container }
                ?: service.findText("Próximo")
        }

        if (previewNext != null) {
            clickNode(previewNext, "ProximoPreview")
            service.waitStable(5_000, 600, "com.shopee")
            Log.i(TAG, "advanceThroughEditing: avançou do preview")
        } else {
            Log.w(TAG, "advanceThroughEditing: tela de preview não encontrada — pulando")
        }

        // Chegou na publicação depois do preview?
        if (HydraAccessibilityService.foregroundActivity.contains("PublishVideo")) {
            Log.i(TAG, "advanceThroughEditing: PublishVideoActivity após preview")
            return StepResult.Success("telaPublicacao")
        }

        Log.i(TAG, "advanceThroughEditing: step 2 — Próximo na edição (tv_compress)")

        // 2. Próximo na tela de edição (tv_compress)
        val editNext = service.waitUntil(8_000) {
            service.findResourceId("com.shopee.br:id/tv_compress")
                ?: service.findText("Próximo")
        }

        if (editNext != null) {
            if (!clickNode(editNext, "ProximoEdicao")) {
                return failure("edicao_proximo", "Não foi possível tocar em 'Próximo' na edição.")
            }
            service.waitStable(6_000, 600, "com.shopee")
        } else {
            // tv_compress não encontrado — verifica se já está na publicação
            if (!HydraAccessibilityService.foregroundActivity.contains("PublishVideo")) {
                return failure("edicao_proximo", "Botão 'Próximo' não encontrado na edição e não está na tela de publicação.\nact=${HydraAccessibilityService.foregroundActivity}")
            }
        }

        Log.i(TAG, "advanceThroughEditing: chegou na tela de publicação")
        return StepResult.Success("telaPublicacao")
    }

    /** Busca um nó com determinado texto dentro de uma subárvore. */
    private fun findTextInNode(root: AccessibilityNodeInfo, text: String): AccessibilityNodeInfo? {
        if (root.text?.toString() == text) return root
        for (i in 0 until root.childCount) {
            findTextInNode(root.getChild(i) ?: continue, text)?.let { return it }
        }
        return null
    }

    // ────────────────────────────────────────────────────────
    // RETORNO AUTÔNOMO — volta da tela de publicação até fechar a Shopee
    // ────────────────────────────────────────────────────────

    /**
     * Retorno autônomo do fluxo de teste:
     *   Galeria (se aberta) → swipe down → câmera → iv_back → HomeActivity → Home Android
     *
     * Não seleciona nenhum vídeo. Só confirma presença e fecha.
     */
    fun returnFromGallery(): StepResult {
        Log.i(TAG, "returnFromGallery: iniciando retorno da galeria")
        val act = HydraAccessibilityService.foregroundActivity

        // ── Passo 1: se estiver na galeria, aguarda 1s e swipe down ───────
        val inGallery = service.findResourceId("com.shopee.br:id/rv_gallery") != null
            || service.findResourceId("com.shopee.br:id/tv_pick_title") != null
            || service.screenContains("Galeria")

        if (inGallery) {
            Log.i(TAG, "returnFromGallery: galeria confirmada — aguardando 1s")
            SystemClock.sleep(1_000)

            // Swipe down para fechar (do topo da grade até o fundo)
            Log.i(TAG, "returnFromGallery: swipe down para fechar galeria")
            val display = android.content.res.Resources.getSystem().displayMetrics
            val cx = display.widthPixels / 2f
            val fromY = display.heightPixels * 0.28f  // ~400px em 1466 de altura
            val toY   = display.heightPixels * 0.92f  // ~1350px
            val path = android.graphics.Path().apply { moveTo(cx, fromY); lineTo(cx, toY) }
            service.dispatchGesture(
                android.accessibilityservice.GestureDescription.Builder()
                    .addStroke(android.accessibilityservice.GestureDescription.StrokeDescription(path, 0L, 400L))
                    .build(),
                null, null
            )
            service.waitStable(3_000, 500, "com.shopee")
            Log.i(TAG, "returnFromGallery: após swipe — act=${HydraAccessibilityService.foregroundActivity}")
        } else {
            Log.i(TAG, "returnFromGallery: galeria não detectada, act=$act — continuando")
        }

        // ── Passo 2: na tela de câmera, toca no iv_back (X) ──────────────
        val cameraBack = service.findResourceId("com.shopee.br:id/iv_back")
            ?: service.findResourceId("com.shopee.br:id/iv_close")

        if (cameraBack != null) {
            Log.i(TAG, "returnFromGallery: tocando no iv_back da câmera")
            clickNode(cameraBack, "iv_back")
            service.waitStable(3_000, 500, "com.shopee")
            Log.i(TAG, "returnFromGallery: após iv_back — act=${HydraAccessibilityService.foregroundActivity}")
        } else {
            Log.w(TAG, "returnFromGallery: iv_back não encontrado — usando back()")
            service.back()
            service.waitStable(3_000, 500, null)
        }

        // ── Passo 3: se chegou no Home da Shopee, pressiona Home ──────────
        val actAfter = HydraAccessibilityService.foregroundActivity
        if (actAfter.contains("HomeActivity", ignoreCase = true)) {
            Log.i(TAG, "returnFromGallery: HomeActivity — pressionando Home Android")
            service.performGlobalAction(android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_HOME)
            SystemClock.sleep(800)
            return StepResult.Success("home")
        }

        // Qualquer outra tela da Shopee — back genérico + Home
        if (HydraAccessibilityService.foregroundPackage.startsWith("com.shopee")) {
            Log.i(TAG, "returnFromGallery: ainda na Shopee ($actAfter) — back + Home")
            service.back()
            SystemClock.sleep(800)
            service.performGlobalAction(android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_HOME)
            SystemClock.sleep(800)
        }

        return StepResult.Success("retorno concluído")
    }

    // ────────────────────────────────────────────────────────
    // 5. Galeria de vídeos (método legado — mantido para compatibilidade)
    // ────────────────────────────────────────────────────────

    /**
     * Abre a galeria a partir da tela de criação.
     * Usa os resource IDs reais mapeados via ADB:
     *   ll_gallery_entrance / tv_gallery_entrance (text="Biblioteca")
     */
    fun openGallery(): StepResult {
        Log.i(TAG, "openGallery: localizando botão Biblioteca")

        // Tenta por resource ID primeiro (mais confiável)
        val button = waitFor("botaoGaleriaContainer", 3_000)
            ?: waitFor("botaoGaleria", (config.pausesMs["galeria"] ?: 6_000L))
            ?: return failure(
                "galeria",
                "Botão 'Biblioteca' não encontrado. Verifique se está na tela de criação de vídeo."
            )

        if (!clickNode(button, "botaoGaleria")) {
            return failure("galeria", "Não foi possível tocar no botão Biblioteca")
        }

        // Valida que a tela de galeria abriu — procura pelo título "Galeria"
        val ready = service.waitUntil(8_000) {
            val pkg = HydraAccessibilityService.foregroundPackage
            if (pkg.startsWith("com.shopee") &&
                (find("telaGaleriaTab") != null || service.screenContains("Galeria"))
            ) true else null
        }
        return if (ready != null) {
            Log.i(TAG, "openGallery: galeria confirmada")
            StepResult.Success("galeria")
        } else {
            failure("galeria", "Tela de galeria não apareceu. Verifique se está na tela certa.")
        }
    }

    /**
     * Fecha a galeria e confirma que voltou para a Shopee.
     * Usado no autoteste.
     */
    fun closeGallery(): StepResult {
        Log.i(TAG, "closeGallery: voltando com back()")
        if (!service.back()) return failure("retorno", "Comando Voltar recusado pela galeria")

        val expectedText = config.texts["telaGaleria"] ?: config.galleryExpectedText
        val returned = service.waitUntil(6_000) {
            service.takeIf {
                HydraAccessibilityService.foregroundPackage == config.packageName
                    && (expectedText.isBlank()
                    || (it.findText(expectedText) == null && it.findDescription(expectedText) == null))
                    && it.roots().isNotEmpty()
            }
        }
        return if (returned != null) {
            Log.i(TAG, "closeGallery: retorno confirmado")
            StepResult.Success("retorno")
        } else {
            failure("retorno", "Galeria reconhecida, mas não foi possível confirmar o retorno à Shopee")
        }
    }

    // ────────────────────────────────────────────────────────
    // 6. Legenda
    // ────────────────────────────────────────────────────────

    /**
     * Preenche o campo de legenda na tela de publicação.
     * resource-id real: com.shopee.br.dfpluginshopee16:id/et_caption
     */
    fun fillCaption(caption: String): StepResult {
        Log.i(TAG, "fillCaption: ${caption.take(40)}")
        if (caption.isBlank()) return StepResult.Success("legenda vazia — ignorada")

        val node = waitFor("campoLegenda", (config.pausesMs["legenda"] ?: 6_000L))
            ?: return failure("legenda", "Campo de legenda não encontrado (et_caption). Verifique se está na tela de publicação.")

        node.performAction(AccessibilityNodeInfo.ACTION_FOCUS)
        SystemClock.sleep(300)
        return if (service.setText(node, caption)) {
            Log.i(TAG, "fillCaption: ok")
            StepResult.Success("legenda")
        } else {
            failure("legenda", "O campo de legenda não aceitou o texto")
        }
    }

    /**
     * Toca no painel de adicionar produto para abrir o seletor.
     * resource-id real: com.shopee.br.dfpluginshopee16:id/add_product_panel_view
     */
    fun tapAddProduct(): StepResult {
        Log.i(TAG, "tapAddProduct")
        val node = waitFor("adicionarProduto", 5_000)
            ?: return failure("produto", "Painel 'Adicionar produto' não encontrado na tela de publicação.")
        if (!clickNode(node, "adicionarProduto")) {
            return failure("produto", "Não foi possível tocar no painel de adicionar produto.")
        }
        pause("curta", 1_500)
        return StepResult.Success("painel produto aberto")
    }

    /**
     * Liga ou desliga o rótulo "Gerado por IA".
     * resource-id real: com.shopee.br.dfpluginshopee16:id/ai_generated_toggle
     *
     * @param enable true = ligar, false = desligar
     */
    fun setAiLabel(enable: Boolean): StepResult {
        Log.i(TAG, "setAiLabel: enable=$enable")
        val toggle = waitFor("rotuloIa", 5_000)
            ?: return failure("rotuloIa", "Toggle 'Rótulo gerado por IA' não encontrado.")

        // Lê estado atual pelo atributo checked/selected
        val currentlyChecked = toggle.isChecked || toggle.isSelected
        Log.d(TAG, "setAiLabel: estado atual checked=$currentlyChecked, enable=$enable")

        if (currentlyChecked == enable) {
            Log.i(TAG, "setAiLabel: já está no estado desejado — sem ação")
            return StepResult.Success("rotuloIa já ${if (enable) "ligado" else "desligado"}")
        }

        if (!clickNode(toggle, "rotuloIa")) {
            return failure("rotuloIa", "Não foi possível tocar no toggle de rótulo IA.")
        }
        pause("curta", 800)
        return StepResult.Success("rotuloIa ${if (enable) "ligado" else "desligado"}")
    }

    // ────────────────────────────────────────────────────────
    // 7. Publicar ou salvar rascunho
    // ────────────────────────────────────────────────────────

    /**
     * Detecta e fecha menu de energia do Android se estiver cobrindo a tela.
     */
    private fun closePowerMenuIfOpen(): Boolean {
        val modoAviao = config.texts["modoAviao"] ?: "Modo avião"
        val desligar = config.texts["desligar"] ?: "Desligar"
        return if (service.screenContains(modoAviao) || service.screenContains(desligar)) {
            Log.w(TAG, "closePowerMenuIfOpen: menu de energia detectado — fechando com Voltar")
            service.back()
            pause("curta", 1_500)
            true
        } else false
    }

    /**
     * Publica ou salva como rascunho.
     * IDs reais: btn_post="Postar" / tv_drafts_box="Rascunhos"
     * Pacote do plugin: com.shopee.br.dfpluginshopee16
     */
    fun publish(isDraft: Boolean): StepResult {
        val mode = if (isDraft) "rascunho" else "publicar"
        Log.i(TAG, "publish: modo=$mode")

        closePowerMenuIfOpen()

        // Usa resource ID real mapeado via ADB
        val buttonKey = if (isDraft) "botaoRascunho" else "botaoPostar"
        val button = waitFor(buttonKey, (config.pausesMs["publicacao"] ?: 8_000L))
            ?: return failure(
                "publicar",
                if (isDraft) "Botão 'Rascunhos' não encontrado (tv_drafts_box)."
                else "Botão 'Postar' não encontrado (btn_post)."
            )

        val ts = System.currentTimeMillis()
        if (!clickNode(button, "botaoPostar/botaoRascunho")) {
            return failure("publicar", "Não foi possível tocar no botão de $mode")
        }

        // Aguarda saída da tela de publicação como sinal de sucesso
        val successKeyword = if (isDraft) "rascunho" else "publicad"
        val confirmed = service.waitForToast(successKeyword, 8_000, ts)
            || service.waitUntil(5_000) {
                // Se saiu da PublishVideoActivity = publicou
                if (!HydraAccessibilityService.foregroundActivity.contains("PublishVideo")) true else null
            } != null

        return if (confirmed) {
            Log.i(TAG, "publish: confirmado ($mode)")
            StepResult.Success(if (isDraft) "📝 Rascunho salvo na Shopee" else "✅ Vídeo publicado")
        } else {
            Log.w(TAG, "publish: sem confirmação explícita — verificando erros")
            if (!service.screenContains("erro") && !service.screenContains("falhou")) {
                StepResult.Success(if (isDraft) "Rascunho salvo" else "Publicado (sem confirmação explícita)")
            } else {
                failure("publicar", "Não consegui confirmar a publicação — verifique manualmente")
            }
        }
    }

    // ────────────────────────────────────────────────────────
    // Diagnóstico
    // ────────────────────────────────────────────────────────

    /** Retorna diagnóstico completo do estado atual da tela. */
    fun diagnostic(): String = buildString {
        append("pkg=${HydraAccessibilityService.foregroundPackage}")
        append(" act=${HydraAccessibilityService.foregroundActivity}")
        append('\n')
        append(service.dumpTexts(200))
        val toast = HydraAccessibilityService.lastToast
        if (toast != null) append("\n[toast]: ${toast.text}")
    }

    // ────────────────────────────────────────────────────────
    // Helpers privados
    // ────────────────────────────────────────────────────────

    /** Tenta click() e depois gestureClick() num nó já localizado.
     *  Usado internamente para todos os toques fora do método tap(key). */
    private fun clickNode(node: AccessibilityNodeInfo, label: String = "?"): Boolean {
        if (service.click(node)) return true
        Log.d(TAG, "clickNode($label): click() falhou — tentando gestureClick()")
        return service.gestureClick(node)
    }

    private fun failure(step: String, reason: String, userAction: Boolean = false): StepResult.Failure {
        val diag = diagnostic()
        Log.e(TAG, "falhou em $step: $reason\n$diag")
        return StepResult.Failure(step, reason, userAction, diag)
    }
}
