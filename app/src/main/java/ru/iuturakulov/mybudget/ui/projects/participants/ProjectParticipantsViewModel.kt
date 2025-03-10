package ru.iuturakulov.mybudget.ui.projects.participants

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import ru.iuturakulov.mybudget.core.UiState
import ru.iuturakulov.mybudget.data.local.entities.ParticipantEntity
import ru.iuturakulov.mybudget.domain.repositories.ParticipantsRepository
import javax.inject.Inject

@HiltViewModel
class ProjectParticipantsViewModel @Inject constructor(
    private val participantRepository: ParticipantsRepository
) : ViewModel() {

    private val _participants = MutableStateFlow<List<ParticipantEntity>>(emptyList())
    val participants: StateFlow<List<ParticipantEntity>> = _participants.asStateFlow()

    private val _uiState = MutableStateFlow<UiState<List<ParticipantEntity>>>(UiState.Idle)
    val uiState: StateFlow<UiState<List<ParticipantEntity>>> = _uiState.asStateFlow()

    private val _invitationCodeState =
        MutableStateFlow<InvitationState<Boolean>>(InvitationState.Idle)
    val invitationCodeState: StateFlow<InvitationState<Boolean>> =
        _invitationCodeState.asStateFlow()

    /**
     * Загрузка участников проекта по `projectId`.
     */
    fun loadParticipants(projectId: String) {
        viewModelScope.launch {
            _uiState.value = UiState.Loading
            try {
                participantRepository.getParticipantsForProject(projectId).collect { participants ->
                    _participants.value = participants
                    _uiState.value = UiState.Success(participants)
                }
            } catch (e: Exception) {
                _uiState.value = UiState.Error(e.localizedMessage ?: "Ошибка загрузки участников")
            }
        }
    }

    /**
     * Добавление или обновление участника.
     */
    fun saveParticipant(participant: ParticipantEntity) {
        viewModelScope.launch {
            try {
                participantRepository.saveParticipant(participant)
            } catch (e: Exception) {
                _uiState.value = UiState.Error(e.localizedMessage ?: "Ошибка сохранения участника")
            }
        }
    }

    /**
     * Удаление участника.
     */
    fun deleteParticipant(participant: ParticipantEntity) {
        viewModelScope.launch {
            try {
                participantRepository.deleteParticipant(participant.id)
            } catch (e: Exception) {
                _uiState.value = UiState.Error(e.localizedMessage ?: "Ошибка удаления участника")
            }
        }
    }

    /**
     * Синхронизация участников с сервером.
     */
    fun syncParticipants(projectId: String) {
        viewModelScope.launch {
            _uiState.value = UiState.Loading
            try {
                val participants = participantRepository.syncParticipants(projectId)
                _participants.value = participants
                _uiState.value = UiState.Success(participants)
            } catch (e: Exception) {
                _uiState.value =
                    UiState.Error(e.localizedMessage ?: "Ошибка синхронизации участников")
            }
        }
    }

    /**
     * Отправка приглашения участнику.
     */
    fun sendInvitation(projectId: String, email: String, role: String) {
        viewModelScope.launch {
            try {
                participantRepository.sendInvitation(projectId, email, role)
                _invitationCodeState.value = InvitationState.Success(true)
            } catch (e: Exception) {
                _invitationCodeState.value = InvitationState.Error(e.message.orEmpty())
            }
        }
    }


    sealed class InvitationState<out T> {
        object Idle : InvitationState<Nothing>()
        object Loading : InvitationState<Nothing>()
        data class Success<out T>(val data: T?) : InvitationState<T>()
        data class Error(val message: String) : InvitationState<Nothing>()
    }
}

