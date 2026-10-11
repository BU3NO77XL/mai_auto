package br.com.hydra.automacao.automation

import android.content.Context
import org.json.JSONObject

data class Position(val fracX: Float, val fracY: Float) {
    fun isValid(): Boolean = fracX in 0f..1f && fracY in 0f..1f
}

data class Positions(
    val liveAndVideoTab: Position? = null,
    val captionScreen: Map<String, Position> = emptyMap(),
)

/**
 * Configuração local do motor de fluxo.
 *
 * Configuração dos seletores e tempos do motor, incluindo campos de conveniência
 * para o autoteste (galleryEntryText / galleryExpectedText).
 *
 * Prioridade dos mapas de seletores:
 *   resourceIds → descriptions → texts
 *
 * Chaves esperadas por etapa:
 * ─ Abertura Shopee ──────────────────────────────────────────
 *   texts["modoAviao"]      → texto do "Modo avião" (menu de energia)
 *   texts["desligar"]       → texto do "Desligar" (menu de energia)
 *
 * ─ Navegação para criação ───────────────────────────────────
 *   texts["botaoMais"]      → rótulo do botão "+"
 *   texts["botaoVideo"]     → rótulo do item "Vídeo" no menu
 *   activities["telaCriacao"] → fragmento da Activity de criação
 *
 * ─ Galeria ──────────────────────────────────────────────────
 *   texts["botaoGaleria"]   → rótulo do botão que abre a galeria
 *   texts["telaGaleria"]    → texto exclusivo da tela de galeria
 *   descriptions["botaoGaleria"] → descrição de acessibilidade alternativa
 *
 * ─ Produtos / Favoritar ─────────────────────────────────────
 *   texts["botaoFavoritar"] → rótulo do coração / favoritar
 *   descriptions["botaoFavoritar"] → descrição de acessibilidade alternativa
 *   resourceIds["botaoFavoritar"]  → ID de recurso alternativo
 *   texts["folhaVariacoes"] → texto que indica a folha de variações aberta
 *
 * ─ Legenda ──────────────────────────────────────────────────
 *   resourceIds["campoLegenda"] → ID do campo de legenda
 *   texts["campoLegenda"]       → placeholder alternativo
 *
 * ─ Publicação ───────────────────────────────────────────────
 *   texts["botaoPublicar"]  → rótulo do botão publicar
 *   texts["botaoRascunho"]  → rótulo do botão salvar rascunho
 *   resourceIds["botaoPublicar"]  → ID alternativo
 *   resourceIds["botaoRascunho"]  → ID alternativo
 *
 * ─ Pausas ───────────────────────────────────────────────────
 *   pausasMs["pollRapido"]      → intervalo de polling (padrão 300 ms)
 *   pausasMs["curta"]           → pausa curta (padrão 1 500 ms)
 *   pausasMs["media"]           → pausa média (padrão 2 000 ms)
 *   pausasMs["longa"]           → pausa longa (padrão 3 000 ms)
 *   pausasMs["shopeeAbrir"]     → timeout abertura Shopee (padrão 10 000 ms)
 *   pausasMs["galeria"]         → timeout abertura galeria (padrão 6 000 ms)
 *   pausasMs["produtoCarregar"] → aguarda produto carregar (padrão 3 000 ms)
 *   pausasMs["legenda"]         → timeout campo legenda (padrão 6 000 ms)
 *   pausasMs["publicacao"]      → timeout botão publicar (padrão 8 000 ms)
 */
data class FlowConfig(
    val version: Int = 1,
    val packageName: String = "com.shopee.br",
    val pluginPackagePrefix: String = "",
    // Atalhos para autoteste rápido (também carregados de texts["botaoGaleria"] / texts["telaGaleria"])
    val galleryEntryText: String = "",
    val galleryExpectedText: String = "",
    // Mapas de seletores organizados por tipo de identificador
    val descriptions: Map<String, String> = emptyMap(),
    val texts: Map<String, String> = emptyMap(),
    val activities: Map<String, String> = emptyMap(),
    val resourceIds: Map<String, String> = emptyMap(),
    val positions: Positions = Positions(),
    val pausesMs: Map<String, Long> = emptyMap(),
    val maxAttempts: Int = 3,
) {
    fun validate(): String? = when {
        !packageName.startsWith("com.shopee") -> "O pacote deve começar com com.shopee."
        version < 1 -> "A versão da configuração precisa ser positiva."
        maxAttempts !in 1..5 -> "O limite de tentativas deve ficar entre 1 e 5."
        pausesMs.values.any { it !in 50..30_000 } -> "Pausas devem ficar entre 50 e 30.000 ms."
        positions.liveAndVideoTab?.isValid() == false -> "Posição abaLiveEVideo inválida (valores devem ser 0–1)."
        positions.captionScreen.values.any { !it.isValid() } -> "Posição em telaLegenda inválida (valores devem ser 0–1)."
        else -> null
    }

    /** Retorna cópia com campos do config remoto mesclados.
     *  Valores locais não vazios têm precedência, permitindo overrides manuais. */
    fun mergeRemote(remote: FlowConfig): FlowConfig = copy(
        version = remote.version,
        packageName = if (packageName.isNotBlank()) packageName else remote.packageName,
        pluginPackagePrefix = if (pluginPackagePrefix.isNotBlank()) pluginPackagePrefix else remote.pluginPackagePrefix,
        galleryEntryText = if (galleryEntryText.isNotBlank()) galleryEntryText else remote.galleryEntryText,
        galleryExpectedText = if (galleryExpectedText.isNotBlank()) galleryExpectedText else remote.galleryExpectedText,
        descriptions = remote.descriptions + descriptions,   // local sobrepõe remoto
        texts = remote.texts + texts,
        activities = remote.activities + activities,
        resourceIds = remote.resourceIds + resourceIds,
        positions = remote.positions.copy(
            liveAndVideoTab = positions.liveAndVideoTab ?: remote.positions.liveAndVideoTab,
            captionScreen = remote.positions.captionScreen + positions.captionScreen,
        ),
        pausesMs = remote.pausesMs + pausesMs,
        maxAttempts = if (maxAttempts != 3) maxAttempts else remote.maxAttempts,
    )
}

/** Persistência e serialização local da FlowConfig em JSON. */
class FlowConfigStore(context: Context) {
    private val prefs = context.getSharedPreferences("flow_config", Context.MODE_PRIVATE)

    fun load(): FlowConfig {
        val raw = prefs.getString("json", null) ?: return FlowConfig()
        return runCatching { parse(JSONObject(raw)) }.getOrElse { FlowConfig() }
    }

    fun save(config: FlowConfig) {
        require(config.validate() == null) { config.validate() ?: "Configuração inválida" }
        prefs.edit().putString("json", toJson(config).toString()).apply()
    }

    fun exportJson(): String = toJson(load()).toString(2)

    fun importJson(raw: String) { save(parse(JSONObject(raw))) }

    // ── Serialização ──────────────────────────────────────────────────────

    private fun toJson(config: FlowConfig): JSONObject = JSONObject()
        .put("version", config.version)
        .put("pacote", config.packageName)
        .put("prefixoPluginPacote", config.pluginPackagePrefix)
        .put("galleryEntryText", config.galleryEntryText)
        .put("galleryExpectedText", config.galleryExpectedText)
        .put("descricoes", JSONObject(config.descriptions))
        .put("textos", JSONObject(config.texts))
        .put("activities", JSONObject(config.activities))
        .put("resourceIds", JSONObject(config.resourceIds))
        .put("posicoes", JSONObject()
            .put("abaLiveEVideo", config.positions.liveAndVideoTab?.toJson())
            .put("telaLegenda", JSONObject(config.positions.captionScreen.mapValues { it.value.toJson() })))
        .put("pausasMs", JSONObject(config.pausesMs))
        .put("maxTentativas", config.maxAttempts)

    // ── Desserialização ───────────────────────────────────────────────────

    private fun parse(json: JSONObject): FlowConfig = FlowConfig(
        version = json.optInt("version", 1),
        packageName = json.optString("pacote", "com.shopee.br"),
        pluginPackagePrefix = json.optString("prefixoPluginPacote", ""),
        galleryEntryText = json.optString("galleryEntryText", ""),
        galleryExpectedText = json.optString("galleryExpectedText", ""),
        descriptions = json.optJSONObject("descricoes").stringMap(),
        texts = json.optJSONObject("textos").stringMap(),
        activities = json.optJSONObject("activities").stringMap(),
        resourceIds = json.optJSONObject("resourceIds").stringMap(),
        positions = json.optJSONObject("posicoes")?.let { positions ->
            Positions(
                liveAndVideoTab = positions.optJSONObject("abaLiveEVideo")?.toPosition(),
                captionScreen = positions.optJSONObject("telaLegenda")?.let { screen ->
                    screen.keys().asSequence().mapNotNull { key ->
                        screen.optJSONObject(key)?.toPosition()?.let { key to it }
                    }.toMap()
                } ?: emptyMap(),
            )
        } ?: Positions(),
        pausesMs = json.optJSONObject("pausasMs").longMap(),
        maxAttempts = json.optInt("maxTentativas", 3),
    ).also { require(it.validate() == null) { it.validate()!! } }

    // ── Helpers ───────────────────────────────────────────────────────────

    private fun JSONObject?.stringMap(): Map<String, String> = this?.let { obj ->
        obj.keys().asSequence().associateWith { obj.optString(it) }
    } ?: emptyMap()

    private fun JSONObject?.longMap(): Map<String, Long> = this?.let { obj ->
        obj.keys().asSequence().associateWith { obj.optLong(it) }
    } ?: emptyMap()

    private fun Position.toJson(): JSONObject = JSONObject()
        .put("fracX", fracX)
        .put("fracY", fracY)

    private fun JSONObject.toPosition(): Position =
        Position(optDouble("fracX").toFloat(), optDouble("fracY").toFloat())
}
