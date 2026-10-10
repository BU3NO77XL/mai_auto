package br.com.hydra.automacao.automation

import android.content.Context

data class TestResult(val step: String, val message: String, val success: Boolean?, val timestamp: Long)

class TestResultStore(context: Context) {
    private val prefs = context.getSharedPreferences("autotest", Context.MODE_PRIVATE)

    fun save(step: String, message: String, success: Boolean? = null) {
        prefs.edit()
            .putString("step", step)
            .putString("message", message)
            .putString("success", success?.toString() ?: "")
            .putLong("timestamp", System.currentTimeMillis())
            .apply()
    }

    fun load(): TestResult? {
        if (!prefs.contains("timestamp")) return null
        return TestResult(
            prefs.getString("step", "") ?: "",
            prefs.getString("message", "") ?: "",
            prefs.getString("success", "")?.takeIf { it.isNotEmpty() }?.toBoolean(),
            prefs.getLong("timestamp", 0),
        )
    }
}
