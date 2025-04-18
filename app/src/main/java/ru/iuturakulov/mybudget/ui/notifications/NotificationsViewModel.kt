package ru.iuturakulov.mybudget.ui.notifications

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import ru.iuturakulov.mybudget.domain.repositories.NotificationsRepository
import javax.inject.Inject

@HiltViewModel
class NotificationsViewModel @Inject constructor(
    private val repository: NotificationsRepository
) : ViewModel() {

    private val _state = MutableStateFlow<NotificationsUiState>(NotificationsUiState.Loading)
    val state: StateFlow<NotificationsUiState> = _state.asStateFlow()

    init { refresh() }

    fun refresh() = viewModelScope.launch {
//        _state.value = NotificationsUiState.Loading
//        repository.fetchNotifications()
//            .map { list ->
//                if (list.isEmpty()) NotificationsUiState.Empty()
//                else NotificationsUiState.Success(list.map { it.toUi() })
//            }
//            .catch { _state.value = NotificationsUiState.Error(it.message.orEmpty()) }
//            .collect { _state.value = it }
    }

    /** помечаем уведомление прочитанным */
    fun markAsRead(id: String) = viewModelScope.launch {
        // repository.markAsRead(id)
    }

    sealed interface NotificationsUiState {
        object Loading : NotificationsUiState
        data class Success(val data: List<NotificationUi>) : NotificationsUiState
        data class Empty(val message: String = "У вас пока нет уведомлений") : NotificationsUiState
        data class Error(val message: String) : NotificationsUiState
    }

    data class NotificationUi(
        val id: String,
        val title: String,
        val body: String,
        val date: String,
        val read: Boolean
    )
}

