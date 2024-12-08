package ru.iuturakulov.mybudget.ui.login

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import ru.iuturakulov.mybudget.domain.usecases.auth.LoginUseCase
import javax.inject.Inject

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val loginUseCase: LoginUseCase
) : ViewModel() {

    private val _loginState = MutableLiveData<LoginState>()
    val loginState: LiveData<LoginState> = _loginState

    /**
     * Выполняет попытку входа пользователя.
     *
     * @param email Email пользователя
     * @param password Пароль пользователя
     */
    fun login(email: String, password: String) {
        // Отображаем состояние загрузки
        _loginState.value = LoginState.Loading

        // Выполняем асинхронную операцию входа
        viewModelScope.launch {
            try {
                val result = loginUseCase(email, password)
                if (result) {
                    // Если вход успешен
                    _loginState.value = LoginState.Success
                } else {
                    // Если неверные данные
                    _loginState.value = LoginState.Error("Неверный email или пароль")
                }
            } catch (e: Exception) {
                // Если произошла ошибка
                _loginState.value = LoginState.Error("Ошибка: ${e.localizedMessage}")
            }
        }
    }

    /**
     * Состояние экрана авторизации
     */
    sealed class LoginState {
        object Loading : LoginState() // Отображение загрузки
        object Success : LoginState() // Успешный вход
        data class Error(val message: String) : LoginState() // Ошибка с сообщением
    }
}