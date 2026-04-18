package com.ruizurraca.luziatestdavid.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ruizurraca.luziatestdavid.domain.audio.AudioRecorder
import com.ruizurraca.luziatestdavid.domain.catalog.PersonaCatalog
import com.ruizurraca.luziatestdavid.domain.common.Resource
import com.ruizurraca.luziatestdavid.domain.model.ChatMessage
import com.ruizurraca.luziatestdavid.domain.model.MessageRole
import com.ruizurraca.luziatestdavid.domain.model.MessageStatus
import com.ruizurraca.luziatestdavid.domain.model.Persona
import com.ruizurraca.luziatestdavid.domain.repository.ChatRepository
import com.ruizurraca.luziatestdavid.domain.usecase.StreamAssistantReplyUseCase
import com.ruizurraca.luziatestdavid.domain.usecase.TranscribeAudioUseCase
import com.ruizurraca.luziatestdavid.presentation.model.toUiModels
import com.ruizurraca.luziatestdavid.presentation.state.ChatEvent
import com.ruizurraca.luziatestdavid.presentation.state.ChatUiState
import com.ruizurraca.luziatestdavid.presentation.state.ProcessingKind
import com.ruizurraca.luziatestdavid.presentation.state.Tier1Kind
import com.ruizurraca.luziatestdavid.presentation.state.toChatEvent
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val audioRecorder: AudioRecorder,
    private val transcribeAudio: TranscribeAudioUseCase,
    private val streamAssistantReply: StreamAssistantReplyUseCase,
    private val repository: ChatRepository,
    private val personaCatalog: PersonaCatalog
) : ViewModel() {

    private sealed interface Phase {
        data object Idle : Phase
        data object Listening : Phase
        data class Processing(val kind: ProcessingKind) : Phase
        data object Streaming : Phase
    }

    private val domainMessages = MutableStateFlow<List<ChatMessage>>(emptyList())
    private val phase = MutableStateFlow<Phase>(Phase.Idle)
    private val draft = MutableStateFlow("")

    private val _selectedPersona = MutableStateFlow(Persona.STUDENT)
    val selectedPersona: StateFlow<Persona> = _selectedPersona.asStateFlow()

    private val _events = MutableSharedFlow<ChatEvent>(extraBufferCapacity = 8)
    val events: SharedFlow<ChatEvent> = _events.asSharedFlow()

    val state: StateFlow<ChatUiState> = combine(
        domainMessages, phase, draft
    ) { msgs, currentPhase, currentDraft ->
        val ui = msgs.toUiModels()
        when (currentPhase) {
            Phase.Idle -> ChatUiState.Idle(ui, currentDraft)
            Phase.Listening -> ChatUiState.Listening(ui, currentDraft)
            is Phase.Processing -> ChatUiState.Processing(ui, currentDraft, currentPhase.kind)
            Phase.Streaming -> ChatUiState.Streaming(ui, currentDraft)
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = ChatUiState.Idle()
    )

    init {
        viewModelScope.launch {
            repository.observeConversation().collect { domainMessages.value = it }
        }
    }

    fun onDraftChange(text: String) {
        draft.value = text
    }

    fun startRecording() {
        viewModelScope.launch {
            when (val result = audioRecorder.start()) {
                is Resource.Success -> phase.value = Phase.Listening
                is Resource.Error -> _events.tryEmit(result.toTieredEvent())
            }
        }
    }

    fun stopRecording() {
        viewModelScope.launch {
            phase.value = Phase.Processing(ProcessingKind.TRANSCRIBING)
            when (val stopResult = audioRecorder.stop()) {
                is Resource.Success -> {
                    when (val transcribeResult = transcribeAudio(stopResult.data)) {
                        is Resource.Success -> draft.value = transcribeResult.data
                        is Resource.Error -> _events.tryEmit(transcribeResult.toTieredEvent())
                    }
                }
                is Resource.Error -> _events.tryEmit(stopResult.toTieredEvent())
            }
            phase.value = Phase.Idle
        }
    }

    fun onSendTap() {
        val currentDraft = draft.value.trim()
        if (currentDraft.isEmpty()) return

        val userMsg = ChatMessage(
            id = UUID.randomUUID().toString(),
            role = MessageRole.USER,
            content = currentDraft,
            timestamp = System.currentTimeMillis(),
            status = MessageStatus.DELIVERED,
            personaPrompt = activePersonaPrompt()
        )

        viewModelScope.launch {
            val historySnapshot = domainMessages.value
            repository.saveMessage(userMsg)
            draft.value = ""
            phase.value = Phase.Processing(ProcessingKind.AWAITING_REPLY)

            consumeAssistantStream(streamAssistantReply(historySnapshot + userMsg))
        }
    }

    fun onPersonaSelected(persona: Persona) {
        _selectedPersona.value = persona
    }

    fun onRetryLastFailure() {
        val snapshot = domainMessages.value
        val failedIndex = snapshot.indexOfLast {
            it.role == MessageRole.ASSISTANT && it.status == MessageStatus.FAILED
        }
        if (failedIndex < 0) return

        val failedId = snapshot[failedIndex].id
        val cleaned = snapshot.toMutableList().apply { removeAt(failedIndex) }

        viewModelScope.launch {
            repository.deleteMessage(failedId)
            phase.value = Phase.Processing(ProcessingKind.AWAITING_REPLY)
            consumeAssistantStream(streamAssistantReply(cleaned))
        }
    }

    fun onClearConversation() {
        viewModelScope.launch {
            repository.clearConversation()
        }
    }

    private fun activePersonaPrompt(): String =
        personaCatalog.entries().first { it.persona == _selectedPersona.value }.prompt

    private suspend fun consumeAssistantStream(stream: Flow<Resource<Unit>>) {
        stream.collect { resource ->
            when (resource) {
                is Resource.Success -> phase.value = Phase.Streaming
                is Resource.Error -> {
                    _events.tryEmit(resource.toTieredEvent())
                    phase.value = Phase.Idle
                }
            }
        }
        phase.value = Phase.Idle
    }

    /**
     * Route a `Resource.Error` to a tier-classified [ChatEvent]. When the error
     * carries a populated [com.ruizurraca.luziatestdavid.domain.common.AppError],
     * [toChatEvent] handles resolution. Otherwise (legacy / untyped errors — e.g.
     * test fixtures) we surface the raw message as an Unknown Tier-1 so the
     * composable shows it verbatim.
     */
    private fun Resource.Error.toTieredEvent(): ChatEvent =
        error?.toChatEvent() ?: ChatEvent.Tier1(
            kind = Tier1Kind.Unknown,
            backendMessage = message.takeIf { it.isNotBlank() }
        )
}
