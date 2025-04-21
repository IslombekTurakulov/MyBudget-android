package ru.iuturakulov.mybudget.ui.notifications

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import ru.iuturakulov.mybudget.core.UiState
import ru.iuturakulov.mybudget.data.remote.dto.NotificationDto
import ru.iuturakulov.mybudget.domain.repositories.NotificationsRepository
import java.io.IOException
import javax.inject.Inject

@HiltViewModel
class NotificationsViewModel @Inject constructor(
    private val repo: NotificationsRepository
) : ViewModel() {

    private val _state = MutableStateFlow<UiState<List<NotificationDto>>>(UiState.Idle)
    val state: StateFlow<UiState<List<NotificationDto>>> = _state.asStateFlow()

    init {
        refresh()
    }

    fun refresh() = viewModelScope.launch {
        _state.value = UiState.Loading
        try {
            val list = repo.getNotifications()
            _state.value = UiState.Success(list)
        } catch (e: IOException) {
            _state.value = UiState.Error("Проблемы с сетью")
        } catch (e: Exception) {
            _state.value = UiState.Error(e.localizedMessage ?: "Ошибка")
        }
    }

    fun markRead(id: String) = viewModelScope.launch {
        val current = (_state.value as? UiState.Success)?.data.orEmpty()
        // локально ставим прочитано
        val updated = current.map { if (it.id == id) it.copy(isRead = true) else it }
        _state.value = UiState.Success(updated)

        try {
            repo.markRead(id)
        } catch (e: Exception) {
            // при ошибке откатываем назад
            _state.value = UiState.Success(current)
            // здесь можно залогировать или показать тост
        }
    }

    fun remove(id: String) = viewModelScope.launch {
        val current = (_state.value as? UiState.Success)?.data.orEmpty()
        // локально убираем уведомление
        val updated = current.filterNot { it.id == id }
        _state.value = UiState.Success(updated)

        try {
            repo.removeNotification(id)
        } catch (e: Exception) {
            // при ошибке можно вернуть назад
            _state.value = UiState.Success(current)
            // и уведомить пользователя об ошибке
        }
    }
}
