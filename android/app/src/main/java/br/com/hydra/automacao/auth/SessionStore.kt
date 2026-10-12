package br.com.hydra.automacao.auth

import android.content.Context

/**
 * Sessão simulada: guarda login local até o backend existir.
 * Trocar por chamada de API depois sem mexer nas telas.
 */
class SessionStore(context: Context) {

    private val prefs = context.getSharedPreferences("mai_session", Context.MODE_PRIVATE)

    fun isLoggedIn(): Boolean = prefs.getBoolean("logged_in", false)

    fun hasSeenOnboarding(): Boolean = prefs.getBoolean("onboarding_seen", false)

    fun markOnboardingSeen() {
        prefs.edit().putBoolean("onboarding_seen", true).apply()
    }

    fun login(email: String) {
        prefs.edit()
            .putBoolean("logged_in", true)
            .putString("email", email)
            .apply()
    }

    fun logout() {
        prefs.edit().clear().apply()
    }

    fun email(): String = prefs.getString("email", "").orEmpty()
}
