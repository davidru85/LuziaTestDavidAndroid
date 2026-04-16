package com.ruizurraca.luziatestdavid.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ruizurraca.luziatestdavid.domain.audio.AudioRecorder
import com.ruizurraca.luziatestdavid.domain.common.Resource
import com.ruizurraca.luziatestdavid.domain.model.ChatMessage
import com.ruizurraca.luziatestdavid.domain.model.MessageRole
import com.ruizurraca.luziatestdavid.domain.model.MessageStatus
import com.ruizurraca.luziatestdavid.domain.repository.ChatRepository
import com.ruizurraca.luziatestdavid.domain.usecase.StreamAssistantReplyUseCase
import com.ruizurraca.luziatestdavid.domain.usecase.TranscribeAudioUseCase
import com.ruizurraca.luziatestdavid.presentation.model.toUiModels
import com.ruizurraca.luziatestdavid.presentation.state.ChatEvent
import com.ruizurraca.luziatestdavid.presentation.state.ChatUiState
import com.ruizurraca.luziatestdavid.presentation.state.ProcessingKind
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
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
    private val repository: ChatRepository
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
                is Resource.Error -> _events.tryEmit(ChatEvent.Error(result.message))
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
                        is Resource.Error -> _events.tryEmit(
                            ChatEvent.Error(transcribeResult.message)
                        )
                    }
                }
                is Resource.Error -> _events.tryEmit(ChatEvent.Error(stopResult.message))
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
            status = MessageStatus.DELIVERED
        )

        viewModelScope.launch {
            val historySnapshot = domainMessages.value
            repository.saveMessage(userMsg)
            draft.value = ""
            phase.value = Phase.Processing(ProcessingKind.AWAITING_REPLY)

            streamAssistantReply(historySnapshot + userMsg).collect { resource ->
                when (resource) {
                    is Resource.Success -> phase.value = Phase.Streaming
                    is Resource.Error -> {
                        _events.tryEmit(ChatEvent.Error(resource.message))
                        phase.value = Phase.Idle
                    }
                }
            }
            phase.value = Phase.Idle
        }
    }

    fun onClearConversation() {
        viewModelScope.launch {
            repository.clearConversation()
        }
    }
}
