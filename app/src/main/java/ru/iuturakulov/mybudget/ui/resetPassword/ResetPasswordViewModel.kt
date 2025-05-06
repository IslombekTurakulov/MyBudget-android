package ru.iuturakulov.mybudget.ui.resetPassword

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import ru.iuturakulov.mybudget.core.NetworkErrorHandler
import ru.iuturakulov.mybudget.data.remote.auth.AuthService
import ru.iuturakulov.mybudget.data.remote.auth.EmailRequest
import ru.iuturakulov.mybudget.data.remote.auth.VerifyEmailRequest
import javax.inject.Inject

@HiltViewModel
class ResetPasswordViewModel @Inject constructor(
    private val authService: AuthService
) : ViewModel() {

    private val _resetPasswordState = MutableLiveData<ResetPasswordState>()
    val resetPasswordState: LiveData<ResetPasswordState> = _resetPasswordState

    fun resetPassword(email: String, code: String? = null) {
        _resetPasswordState.value = ResetPasswordState.Loading
        viewModelScope.launch {
            try {
                if (code == null) {
                    // Запрос на отправку кода для сброса пароля
                    val response = authService.sendPasswordResetVerifyCode(EmailRequest(email))
                    if (response.isSuccessful) {
                        _resetPasswordState.value = ResetPasswordState.CodeSent
                    } else {
                        _resetPasswordState.value = ResetPasswordState.Error("Ошибка при отправке кода")
                    }
                } else {
                    // Проверка введенного кода и смена пароля
                    val response = authService.withVerifyResetPasswordWithCode(VerifyEmailRequest(email, code))
                    if (response.isSuccessful) {
                        _resetPasswordState.value = ResetPasswordState.PasswordReset
                    } else {
                        _resetPasswordState.value = ResetPasswordState.Error("Неверный код или ошибка восстановления пароля")
                    }
                }
            } catch (e: Exception) {
                val errorMessage = NetworkErrorHandler.getErrorMessage(e)
                _resetPasswordState.value = ResetPasswordState.Error(errorMessage)
            }
        }
    }

    // Состояния для разных этапов сброса пароля
    sealed class ResetPasswordState {
        object Loading : ResetPasswordState()
        object CodeSent : ResetPasswordState()  // Код отправлен
        object PasswordReset : ResetPasswordState()  // Пароль успешно сброшен
        data class Error(val message: String) : ResetPasswordState()
    }
}
