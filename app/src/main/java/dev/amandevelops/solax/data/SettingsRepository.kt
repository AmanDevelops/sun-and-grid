package dev.amandevelops.solax.data

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import java.io.IOException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.solaxDataStore by preferencesDataStore(name = "solax_settings")

enum class ThemeMode {
    SYSTEM,
    LIGHT,
    DARK,
}

data class AppSettings(
    val host: String = "192.168.10.10",
    val dongleSerial: String = "",
    val demoMode: Boolean = true,
    val weeklyGoalKwh: Double = 140.0,
    val refreshSeconds: Int = 30,
    val theme: ThemeMode = ThemeMode.SYSTEM,
    val invertGridSign: Boolean = false,
    val backgroundSync: Boolean = true,
    val uppclUsername: String = "",
    val uppclPassword: String = "",
    val uppclAccountNumber: String = "",
) {
    val isConfigured: Boolean get() = host.isNotBlank() && dongleSerial.isNotBlank()
    val isUppclConfigured: Boolean
        get() = uppclUsername.isNotBlank() &&
            uppclPassword.isNotBlank() &&
            uppclAccountNumber.isNotBlank()
}

class SettingsRepository(private val context: Context) {

    val settings: Flow<AppSettings> = context.solaxDataStore.data
        .catch { error ->
            if (error is IOException) emit(emptyPreferences()) else throw error
        }
        .map { preferences -> preferences.toSettings() }

    suspend fun current(): AppSettings = settings.first()

    suspend fun update(transform: (AppSettings) -> AppSettings) {
        context.solaxDataStore.edit { preferences ->
            val next = transform(preferences.toSettings())
            preferences[Keys.HOST] = next.host
            preferences[Keys.DONGLE_SERIAL] = next.dongleSerial
            preferences[Keys.DEMO_MODE] = next.demoMode
            preferences[Keys.WEEKLY_GOAL] = next.weeklyGoalKwh.toFloat()
            preferences[Keys.REFRESH_SECONDS] = next.refreshSeconds
            preferences[Keys.THEME] = next.theme.name
            preferences[Keys.INVERT_GRID_SIGN] = next.invertGridSign
            preferences[Keys.BACKGROUND_SYNC] = next.backgroundSync
            preferences[Keys.UPPCL_USERNAME] = next.uppclUsername
            preferences[Keys.UPPCL_PASSWORD] = next.uppclPassword
            preferences[Keys.UPPCL_ACCOUNT_NUMBER] = next.uppclAccountNumber
        }
    }

    private fun Preferences.toSettings(): AppSettings {
        val theme = get(Keys.THEME)?.let { stored ->
            ThemeMode.entries.firstOrNull { it.name == stored }
        } ?: ThemeMode.SYSTEM

        return AppSettings(
            host = get(Keys.HOST)?.takeIf { it.isNotBlank() } ?: "192.168.10.10",
            dongleSerial = get(Keys.DONGLE_SERIAL).orEmpty(),
            demoMode = get(Keys.DEMO_MODE) ?: true,
            weeklyGoalKwh = (get(Keys.WEEKLY_GOAL) ?: 140f).toDouble().coerceIn(10.0, 1000.0),
            refreshSeconds = (get(Keys.REFRESH_SECONDS) ?: 30).coerceIn(10, 300),
            theme = theme,
            invertGridSign = get(Keys.INVERT_GRID_SIGN) ?: false,
            backgroundSync = get(Keys.BACKGROUND_SYNC) ?: true,
            uppclUsername = get(Keys.UPPCL_USERNAME).orEmpty(),
            uppclPassword = get(Keys.UPPCL_PASSWORD).orEmpty(),
            uppclAccountNumber = get(Keys.UPPCL_ACCOUNT_NUMBER).orEmpty(),
        )
    }

    private object Keys {
        val HOST = stringPreferencesKey("host")
        val DONGLE_SERIAL = stringPreferencesKey("dongle_serial")
        val DEMO_MODE = booleanPreferencesKey("demo_mode")
        val WEEKLY_GOAL = floatPreferencesKey("weekly_goal_kwh")
        val REFRESH_SECONDS = intPreferencesKey("refresh_seconds")
        val THEME = stringPreferencesKey("theme")
        val INVERT_GRID_SIGN = booleanPreferencesKey("invert_grid_sign")
        val BACKGROUND_SYNC = booleanPreferencesKey("background_sync")
        val UPPCL_USERNAME = stringPreferencesKey("uppcl_username")
        val UPPCL_PASSWORD = stringPreferencesKey("uppcl_password")
        val UPPCL_ACCOUNT_NUMBER = stringPreferencesKey("uppcl_account_number")
    }
}
