package com.example.data.pref

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class SettingsManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("cod_settings", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_FIREBASE_URL = "firebase_url"
        private const val KEY_FIREBASE_KEY = "firebase_key"
        private const val KEY_MAPBOX_TOKEN = "mapbox_token"
        private const val KEY_ROLE = "current_role" // "ADMIN", "RIDER"
        private const val KEY_RIDER_ID = "rider_id"
        private const val KEY_SIMULATION = "simulation_mode"
    }

    private val _roleFlow = MutableStateFlow(getRole())
    val roleFlow: StateFlow<String> get() = _roleFlow

    fun getFirebaseUrl(): String {
        return prefs.getString(KEY_FIREBASE_URL, "https://cod-finder-qatar-default-rtdb.firebaseio.com") ?: "https://cod-finder-qatar-default-rtdb.firebaseio.com"
    }

    fun setFirebaseUrl(url: String) {
        prefs.edit().putString(KEY_FIREBASE_URL, url.trim().trimEnd('/')).apply()
    }

    fun getFirebaseApiKey(): String {
        return prefs.getString(KEY_FIREBASE_KEY, "") ?: ""
    }

    fun setFirebaseApiKey(key: String) {
        prefs.edit().putString(KEY_FIREBASE_KEY, key.trim()).apply()
    }

    fun getMapboxToken(): String {
        val token = prefs.getString(KEY_MAPBOX_TOKEN, "") ?: ""
        return if (token.isEmpty() || token == "YOUR_MAPBOX_TOKEN") "" else token
    }

    fun setMapboxToken(token: String) {
        prefs.edit().putString(KEY_MAPBOX_TOKEN, token.trim()).apply()
    }

    fun getRole(): String {
        return prefs.getString(KEY_ROLE, "ADMIN") ?: "ADMIN"
    }

    fun setRole(role: String) {
        prefs.edit().putString(KEY_ROLE, role).apply()
        _roleFlow.value = role
    }

    fun getRiderId(): String {
        return prefs.getString(KEY_RIDER_ID, "rider_1") ?: "rider_1"
    }

    fun setRiderId(id: String) {
        prefs.edit().putString(KEY_RIDER_ID, id.trim()).apply()
    }

    fun isSimulationEnabled(): Boolean {
        return prefs.getBoolean(KEY_SIMULATION, true)
    }

    fun setSimulationEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_SIMULATION, enabled).apply()
    }

    // New additions for Dark Theme, Biometrics, and Authentication status
    fun isDarkThemeEnabled(): Boolean {
        return prefs.getBoolean("dark_theme", false) // False by default (Starts in Waze Light Daylight Mode)
    }

    fun setDarkThemeEnabled(enabled: Boolean) {
        prefs.edit().putBoolean("dark_theme", enabled).apply()
    }

    fun isBiometricsEnabled(): Boolean {
        return prefs.getBoolean("biometrics_enabled", false)
    }

    fun setBiometricsEnabled(enabled: Boolean) {
        prefs.edit().putBoolean("biometrics_enabled", enabled).apply()
    }

    fun isLoggedIn(): Boolean {
        return prefs.getBoolean("user_logged_in", false)
    }

    fun setLoggedIn(loggedIn: Boolean) {
        prefs.edit().putBoolean("user_logged_in", loggedIn).apply()
    }

    fun getLoggedInUser(): String {
        return prefs.getString("logged_in_user_info", "") ?: ""
    }

    fun setLoggedInUser(user: String) {
        prefs.edit().putString("logged_in_user_info", user).apply()
    }

    // Check if Firebase settings have been configured by user
    fun isFirebaseConfigured(): Boolean {
        val url = getFirebaseUrl()
        return url.isNotEmpty() && !url.contains("default-rtdb") && !url.contains("your-project")
    }
}
