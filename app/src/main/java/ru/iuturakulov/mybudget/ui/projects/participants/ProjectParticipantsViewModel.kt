package ru.iuturakulov.mybudget.ui.projects.participants

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import ru.iuturakulov.mybudget.core.UiState
import ru.iuturakulov.mybudget.data.local.entities.ParticipantEntity
import ru.iuturakulov.mybudget.data.remote.dto.InvitationDto
import ru.iuturakulov.mybudget.data.remote.dto.InvitationRequest
import ru.iuturakulov.mybudget.domain.repositories.ParticipantsRepository
import java.io.IOException
import javax.inject.Inject

@HiltViewModel
class ProjectParticipantsViewModel @Inject constructor(
    private val participantRepository: ParticipantsRepository
) : ViewModel() {

    private val _participants = MutableStateFlow<List<ParticipantEntity>>(emptyList())
    val participants: StateFlow<List<ParticipantEntity>> = _participants
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _uiState = MutableStateFlow<UiState<List<ParticipantEntity>>>(UiState.Idle)
    val uiState: StateFlow<UiState<List<ParticipantEntity>>> = _uiState
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), UiState.Idle)

    private val _invitationState = MutableStateFlow<InvitationState<InvitationDto?>>(InvitationState.Idle)
    val invitationState: StateFlow<InvitationState<InvitationDto?>> = _invitationState
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), InvitationState.Idle)

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    /**
     * Загружает участников проекта с возможностью автоматического обновления через Flow
     * @param projectId ID проекта для загрузки участников
     * @param observeChanges если true, будет подписан на изменения в реальном времени
     */
    fun loadParticipants(projectId: String, observeChanges: Boolean = true) {
        viewModelScope.launch {
            _isLoading.value = true
            _uiState.value = UiState.Loading

            try {
                val participantsFlow = if (observeChanges) {
                    participantRepository.observeLocalParticipants(projectId)
                } else {
                    participantRepository.getParticipantsWithSync(projectId)
                }

                participantsFlow.collect { participants ->
                    _participants.value = participants
                    _uiState.value = UiState.Success(participants)
                    _isLoading.value = false
                }
            } catch (e: Exception) {
                handleError(e, "Ошибка загрузки участников")
                _isLoading.value = false
            }
        }
    }

    /**
     * Сохраняет или обновляет участника проекта
     * @param participant Данные участника для сохранения
     */
    fun saveParticipant(participant: ParticipantEntity) =  launchWithState({
        participantRepository.saveParticipant(participant)
        loadParticipants(participant.projectId, observeChanges = false)
        })

    /**
     * Удаляет участника из проекта
     * @param participant Участник для удаления
     */
    fun deleteParticipant(participant: ParticipantEntity) = launchWithState({
        participantRepository.deleteParticipant(participant.projectId, participant.userId)
    })

    /**
     * Синхронизирует список участников с сервером
     * @param projectId ID проекта для синхронизации
     */
    fun syncParticipants(projectId: String) = launchWithState({
        val participants = participantRepository.syncParticipants(projectId)
        _participants.value = participants
        _uiState.value = UiState.Success(participants)
    })

    /**
     * Отправляет приглашение новому участнику
     * @param projectId ID проекта
     * @param email Email приглашаемого
     * @param role Роль участника
     */
    fun sendInvitation(request: InvitationRequest) {
        viewModelScope.launch {
            _invitationState.value = InvitationState.Loading
            try {
                val invitationDto = participantRepository.sendInvitation(request)
                _invitationState.value = InvitationState.Success(invitationDto)
            } catch (e: Exception) {
                _invitationState.value = InvitationState.Error(
                    e.localizedMessage ?: "Ошибка отправки приглашения"
                )
            }
        }
    }

    private fun handleError(e: Exception, defaultMessage: String) {
        val message = when (e) {
            is IOException -> "Проблемы с сетью"
            else -> e.localizedMessage ?: defaultMessage
        }
        _uiState.value = UiState.Error(message)
    }

    private inline fun launchWithState(
        crossinline action: suspend () -> Unit,
        errorMessage: String = "Ошибка операции"
    ) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                action()
            } catch (e: Exception) {
                handleError(e, errorMessage)
            } finally {
                _isLoading.value = false
            }
        }
    }

    sealed class InvitationState<out T> {
        object Idle : InvitationState<Nothing>()
        object Loading : InvitationState<Nothing>()
        data class Success<out T>(val data: T) : InvitationState<T>()
        data class Error(val message: String) : InvitationState<Nothing>()
    }
}