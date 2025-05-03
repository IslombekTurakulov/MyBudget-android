package ru.iuturakulov.mybudget.ui.settings

import android.content.SharedPreferences
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import ru.iuturakulov.mybudget.auth.TokenStorage
import ru.iuturakulov.mybudget.domain.models.UserSettings
import ru.iuturakulov.mybudget.domain.repositories.SettingsRepository
import javax.inject.Inject
import androidx.core.content.edit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import ru.iuturakulov.mybudget.auth.CodeTokenStorage
import ru.iuturakulov.mybudget.data.local.AppDatabase

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val repository: SettingsRepository,
    private val tokenStorage: TokenStorage,
    private val codeTokenStorage: CodeTokenStorage,
    private val encryptedSharedPreferences: SharedPreferences,
    private val db: AppDatabase,
) : ViewModel() {

    companion object {
        private const val DARK_THEME = "dark_theme"
        private const val LOCALE = "locale"
        private const val API_HOST = "api_host"
        private const val DEFAULT_HOST = "http://localhost:8080/"
    }

    val userSettings = MutableStateFlow<UserSettings?>(null)
    val message = MutableSharedFlow<String>()

    private val _host = MutableStateFlow(
        encryptedSharedPreferences.getString(API_HOST, DEFAULT_HOST)!!
    )
    val host: StateFlow<String> = _host


    fun fetchUserSettings() {
        viewModelScope.launch {
            try {
                val settings = repository.getUserSettings()
                userSettings.value = settings
            } catch (e: Exception) {
                message.emit("Ошибка загрузки настроек: ${e.localizedMessage}")
            }
        }
    }

    fun saveUserSettings(settings: UserSettings) {
        viewModelScope.launch {
            try {
                val updatedSettings = repository.updateUserSettings(settings)
                userSettings.value = updatedSettings
            } catch (e: Exception) {
                message.emit("Ошибка сохранения настроек: ${e.localizedMessage}")
            }
        }
    }

    fun toggleDarkTheme(isDarkTheme: Boolean) {
        viewModelScope.launch {
            encryptedSharedPreferences.edit()
                .putBoolean(DARK_THEME, isDarkTheme)
                .apply()
        }
    }

    fun saveCurrentLocale(locale: String) {
        viewModelScope.launch {
            encryptedSharedPreferences.edit()
                .putString(LOCALE, locale)
                .apply()
        }
    }

    fun logout() {
        viewModelScope.launch {
            tokenStorage.clearTokens()
            codeTokenStorage.clearCodeToken()
            withContext(Dispatchers.IO) {
                db.clearAllTables()
            }
        }
    }

    /** Обновить API-host и сохранить его вprefs */
    fun updateHost(newHost: String) {
        viewModelScope.launch {
            encryptedSharedPreferences.edit()
                .putString(API_HOST, newHost)
                .apply()
            _host.value = newHost
        }
    }
}
