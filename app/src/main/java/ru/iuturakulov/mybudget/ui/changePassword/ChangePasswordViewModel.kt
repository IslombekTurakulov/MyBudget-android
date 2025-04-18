package ru.iuturakulov.mybudget.ui.changePassword

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import ru.iuturakulov.mybudget.core.NetworkErrorHandler
import ru.iuturakulov.mybudget.data.remote.auth.ChangePasswordAuthService
import ru.iuturakulov.mybudget.data.remote.auth.ChangePasswordRequest
import javax.inject.Inject

@HiltViewModel
class ChangePasswordViewModel @Inject constructor(
    private val authService: ChangePasswordAuthService
) : ViewModel() {

    private val _changePasswordState = MutableLiveData<ChangePasswordState>()
    val changePasswordState: LiveData<ChangePasswordState> = _changePasswordState

    fun changePassword(email: String, oldPassword: String, newPassword: String) {
        _changePasswordState.value = ChangePasswordState.Loading
        viewModelScope.launch {
            try {
                val response = authService.changePassword(
                    ChangePasswordRequest(
                        email = email,
                        oldPassword = oldPassword,
                        newPassword = newPassword,
                    )
                )
                if (response.isSuccessful) {
                    _changePasswordState.value = ChangePasswordState.Success
                } else {
                    _changePasswordState.value =
                        ChangePasswordState.Error("Ошибка смены пароля")
                }
            } catch (e: Exception) {
                val errorMessage = NetworkErrorHandler.getErrorMessage(e)
                _changePasswordState.value = ChangePasswordState.Error("Ошибка: $errorMessage")
            }
        }
    }

    sealed class ChangePasswordState {
        object Loading : ChangePasswordState()
        object Success : ChangePasswordState()
        data class Error(val message: String) : ChangePasswordState()
    }
}
