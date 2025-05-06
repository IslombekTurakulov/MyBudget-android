package ru.iuturakulov.mybudget.ui.register

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import ru.iuturakulov.mybudget.domain.usecases.auth.RegisterUseCase
import javax.inject.Inject

@HiltViewModel
class RegisterViewModel @Inject constructor(
    private val registerUseCase: RegisterUseCase
) : ViewModel() {

    private val _registerState = MutableLiveData<RegisterState>()
    val registerState: LiveData<RegisterState> = _registerState

    fun register(name: String, email: String, password: String, verificationCode: String? = null) {
        _registerState.value = RegisterState.Loading
        viewModelScope.launch {
            try {
                if (verificationCode == null) {
                    // Если код не был предоставлен, отправляем запрос на отправку кода верификации
                    _registerState.value = RegisterState.SendingVerificationCode
                    val result = registerUseCase.sendVerificationCode(email)
                    if (result) {
                        _registerState.value =
                            RegisterState.VerificationCodeSent(name, email, password)
                    } else {
                        _registerState.value =
                            RegisterState.Error("Ошибка регистрации. Попробуйте еще раз.")
                    }
                } else {
                    // Если код был предоставлен, выполняем подтверждение регистрации
                    val result = registerUseCase.registerWithVerification(
                        name = name,
                        email = email,
                        password = password,
                        verificationCode = verificationCode
                    )
                    if (result) {
                        _registerState.value = RegisterState.Success
                    } else {
                        _registerState.value =
                            RegisterState.Error("Ошибка подтверждения. Попробуйте снова.")
                    }
                }
            } catch (e: Exception) {
                _registerState.value = RegisterState.Error("${e.localizedMessage}")
            }
        }
    }

    sealed class RegisterState {
        object Loading : RegisterState()
        object Success : RegisterState()
        data class Error(val message: String) : RegisterState()
        object SendingVerificationCode : RegisterState()
        data class VerificationCodeSent(val name: String, val email: String, val password: String) :
            RegisterState()
    }
}