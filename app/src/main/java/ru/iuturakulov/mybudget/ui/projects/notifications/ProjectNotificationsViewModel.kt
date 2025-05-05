package ru.iuturakulov.mybudget.ui.projects.notifications

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import ru.iuturakulov.mybudget.data.remote.dto.NotificationType
import ru.iuturakulov.mybudget.data.remote.dto.PreferencesRequest
import ru.iuturakulov.mybudget.domain.repositories.NotificationsRepository
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class ProjectNotificationsViewModel @Inject constructor(
    private val notificationsRepository: NotificationsRepository
) : ViewModel() {

    private val _prefs = MutableStateFlow<Set<NotificationType>>(emptySet())
    val prefs: StateFlow<Set<NotificationType>> = _prefs

    fun initNotifications(projectId: String) {
        viewModelScope.launch {
            runCatching {
                notificationsRepository.getFCMNotificationPreferences(projectId).toSet()
            }.onSuccess { _prefs.value = it }.onFailure(Timber::e)
        }
    }

    fun toggle(projectId: String, type: NotificationType, enabled: Boolean) {
        val updated = _prefs.value.toMutableSet().apply {
            if (enabled) add(type) else remove(type)
        }
        _prefs.value = updated
        viewModelScope.launch {
            notificationsRepository.setFCMNotificationPreferences(projectId, PreferencesRequest(updated.map { it.name }))
        }
    }
}