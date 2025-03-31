package ru.iuturakulov.mybudget.ui.settings

import android.content.SharedPreferences
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import ru.iuturakulov.mybudget.auth.TokenStorage
import ru.iuturakulov.mybudget.domain.models.UserSettings
import ru.iuturakulov.mybudget.domain.repositories.SettingsRepository
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val repository: SettingsRepository,
    private val tokenStorage: TokenStorage,
    private val encryptedSharedPreferences: SharedPreferences
) : ViewModel() {

    val userSettings = MutableStateFlow<UserSettings?>(null)
    val message = MutableSharedFlow<String>()

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
                message.emit("Настройки успешно сохранены")
            } catch (e: Exception) {
                message.emit("Ошибка сохранения настроек: ${e.localizedMessage}")
            }
        }
    }

    fun toggleDarkTheme(isDarkTheme: Boolean) {
        viewModelScope.launch {
            encryptedSharedPreferences.edit().putBoolean(
                DARK_THEME,
                isDarkTheme
            ).apply()
        }
    }

    fun logout() {
        viewModelScope.launch {
            tokenStorage.clearTokens()
            message.emit("Выход выполнен успешно")
        }
    }

    companion object {
        private const val DARK_THEME = "dark_theme"
    }
}
