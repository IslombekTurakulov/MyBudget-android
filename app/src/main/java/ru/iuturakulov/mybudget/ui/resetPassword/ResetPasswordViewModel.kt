package ru.iuturakulov.mybudget.ui.resetPassword

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import ru.iuturakulov.mybudget.core.NetworkErrorHandler
import ru.iuturakulov.mybudget.data.remote.AuthService
import javax.inject.Inject

@HiltViewModel
class ResetPasswordViewModel @Inject constructor(
    private val authService: AuthService
) : ViewModel() {

    private val _resetPasswordState = MutableLiveData<ResetPasswordState>()
    val resetPasswordState: LiveData<ResetPasswordState> = _resetPasswordState

    fun resetPassword(email: String) {
        _resetPasswordState.value = ResetPasswordState.Loading
        viewModelScope.launch {
            try {
                val response = authService.resetPassword(email)
                if (response.isSuccessful) {
                    _resetPasswordState.value = ResetPasswordState.Success
                } else {
                    _resetPasswordState.value =
                        ResetPasswordState.Error("Ошибка восстановления пароля")
                }
            } catch (e: Exception) {
                val errorMessage = NetworkErrorHandler.getErrorMessage(e)
                _resetPasswordState.value = ResetPasswordState.Error("Ошибка: $errorMessage")
            }
        }
    }

    sealed class ResetPasswordState {
        object Loading : ResetPasswordState()
        object Success : ResetPasswordState()
        data class Error(val message: String) : ResetPasswordState()
    }
}
