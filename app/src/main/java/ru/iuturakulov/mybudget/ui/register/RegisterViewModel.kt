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

    fun register(name: String, email: String, password: String) {
        _registerState.value = RegisterState.Loading
        viewModelScope.launch {
            try {
                val result = registerUseCase(name, email, password)
                if (result) {
                    _registerState.value = RegisterState.Success
                } else {
                    _registerState.value = RegisterState.Error("Ошибка регистрации")
                }
            } catch (e: Exception) {
                _registerState.value = RegisterState.Error("Ошибка: ${e.message}")
            }
        }
    }

    sealed class RegisterState {
        object Loading : RegisterState()
        object Success : RegisterState()
        data class Error(val message: String) : RegisterState()
    }
}