package com.ruizurraca.luziatestdavid.presentation.viewmodel

import app.cash.turbine.test
import com.ruizurraca.luziatestdavid.domain.audio.AudioRecorder
import com.ruizurraca.luziatestdavid.domain.common.Resource
import com.ruizurraca.luziatestdavid.domain.model.ChatMessage
import com.ruizurraca.luziatestdavid.domain.model.MessageRole
import com.ruizurraca.luziatestdavid.domain.model.MessageStatus
import com.ruizurraca.luziatestdavid.domain.repository.ChatRepository
import com.ruizurraca.luziatestdavid.domain.usecase.StreamAssistantReplyUseCase
import com.ruizurraca.luziatestdavid.domain.usecase.TranscribeAudioUseCase
import com.ruizurraca.luziatestdavid.presentation.state.ChatEvent
import com.ruizurraca.luziatestdavid.presentation.state.ChatUiState
import com.ruizurraca.luziatestdavid.presentation.state.ProcessingKind
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.io.File

@OptIn(ExperimentalCoroutinesApi::class)
class ChatViewModelTest {

    private val audioRecorder: AudioRecorder = mockk()
    private val transcribeAudio: TranscribeAudioUseCase = mockk()
    private val streamAssistantReply: StreamAssistantReplyUseCase = mockk()
    private val repository: ChatRepository = mockk()

    private val conversation = MutableStateFlow<List<ChatMessage>>(emptyList())

    private fun userMessage(
        id: String = "u1",
        content: String = "hola",
        status: MessageStatus = MessageStatus.DELIVERED
    ) = ChatMessage(
        id = id,
        role = MessageRole.USER,
        content = content,
        timestamp = 1_000L,
        status = status
    )

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        every { repository.observeConversation() } returns conversation
        coEvery { repository.saveMessage(any()) } just Runs
        coEvery { repository.clearConversation() } just Runs
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel() = ChatViewModel(
        audioRecorder = audioRecorder,
        transcribeAudio = transcribeAudio,
        streamAssistantReply = streamAssistantReply,
        repository = repository
    )

    // ----- Initial state & observation ---------------------------------------

    @Test
    fun `initial state is Idle with empty messages and empty draft`() = runTest {
        val vm = createViewModel()

        val state = vm.state.value
        assertTrue(state is ChatUiState.Idle) { "expected Idle, got $state" }
        assertEquals(emptyList<Any>(), state.messages)
        assertEquals("", state.draft)
    }

    @Test
    fun `observeConversation emissions flow into state messages as UI models`() = runTest {
        val vm = createViewModel()

        conversation.value = listOf(
            userMessage(id = "u1", content = "hola"),
            ChatMessage(
                id = "a1",
                role = MessageRole.ASSISTANT,
                content = "¡Hola!",
                timestamp = 1_100L,
                status = MessageStatus.DELIVERED
            )
        )

        val messages = vm.state.value.messages
        assertEquals(2, messages.size)
        assertEquals("u1", messages[0].id)
        assertEquals("a1", messages[1].id)
    }

    // ----- Draft -------------------------------------------------------------

    @Test
    fun `onDraftChange updates state draft`() = runTest {
        val vm = createViewModel()

        vm.onDraftChange("escribiendo…")

        assertEquals("escribiendo…", vm.state.value.draft)
        assertTrue(vm.state.value is ChatUiState.Idle)
    }

    // ----- Voice flow --------------------------------------------------------

    @Test
    fun `startRecording transitions Idle to Listening when recorder succeeds`() = runTest {
        coEvery { audioRecorder.start() } returns Resource.Success(Unit)
        val vm = createViewModel()

        vm.startRecording()

        assertTrue(vm.state.value is ChatUiState.Listening)
        coVerify(exactly = 1) { audioRecorder.start() }
    }

    @Test
    fun `startRecording failure stays in Idle and emits error event`() = runTest {
        coEvery { audioRecorder.start() } returns Resource.Error("mic busy")
        val vm = createViewModel()

        vm.events.test {
            vm.startRecording()
            val event = awaitItem()
            assertTrue(event is ChatEvent.Error && event.message == "mic busy") {
                "expected ChatEvent.Error('mic busy'), got $event"
            }
        }

        assertTrue(vm.state.value is ChatUiState.Idle)
    }

    @Test
    fun `stopRecording happy path transcribes and populates draft`() = runTest {
        coEvery { audioRecorder.start() } returns Resource.Success(Unit)
        val fakeFile = File.createTempFile("luzia", ".m4a")
        coEvery { audioRecorder.stop() } returns Resource.Success(fakeFile)
        coEvery { transcribeAudio(fakeFile) } returns Resource.Success("hola qué tal")

        val vm = createViewModel()
        vm.startRecording()

        vm.stopRecording()

        val state = vm.state.value
        assertTrue(state is ChatUiState.Idle) { "expected Idle after transcription, got $state" }
        assertEquals("hola qué tal", state.draft)
        coVerify(exactly = 1) { transcribeAudio(fakeFile) }
        fakeFile.delete()
    }

    @Test
    fun `stopRecording transcription failure returns to Idle and emits error`() = runTest {
        coEvery { audioRecorder.start() } returns Resource.Success(Unit)
        val fakeFile = File.createTempFile("luzia", ".m4a")
        coEvery { audioRecorder.stop() } returns Resource.Success(fakeFile)
        coEvery { transcribeAudio(fakeFile) } returns Resource.Error("transcription failed")

        val vm = createViewModel()

        vm.events.test {
            vm.startRecording()
            vm.stopRecording()

            val event = awaitItem()
            assertTrue(event is ChatEvent.Error && event.message == "transcription failed") {
                "expected ChatEvent.Error('transcription failed'), got $event"
            }
        }

        assertTrue(vm.state.value is ChatUiState.Idle)
        assertEquals("", vm.state.value.draft)
        fakeFile.delete()
    }

    // ----- Text send flow ----------------------------------------------------

    @Test
    fun `onSendTap with empty draft is a no-op`() = runTest {
        val vm = createViewModel()

        vm.onSendTap()

        assertTrue(vm.state.value is ChatUiState.Idle)
        coVerify(exactly = 0) { repository.saveMessage(any()) }
        coVerify(exactly = 0) { streamAssistantReply(any()) }
    }

    @Test
    fun `onSendTap persists user message, clears draft, streams reply, returns to Idle`() =
        runTest {
            val streamChannel = Channel<Resource<Unit>>(capacity = Channel.UNLIMITED)
            every { streamAssistantReply(any()) } returns streamChannel.receiveAsFlow()

            val vm = createViewModel()
            vm.onDraftChange("hola")

            vm.onSendTap()

            val afterTap = vm.state.value
            assertTrue(afterTap is ChatUiState.Processing) { "expected Processing, got $afterTap" }
            assertEquals(
                ProcessingKind.AWAITING_REPLY,
                (afterTap as ChatUiState.Processing).kind
            )
            assertEquals("", afterTap.draft)

            coVerify(atLeast = 1) {
                repository.saveMessage(match {
                    it.role == MessageRole.USER && it.content == "hola"
                })
            }

            streamChannel.send(Resource.Success(Unit))
            assertTrue(vm.state.value is ChatUiState.Streaming) {
                "expected Streaming, got ${vm.state.value}"
            }

            streamChannel.close()
            assertTrue(vm.state.value is ChatUiState.Idle) {
                "expected Idle, got ${vm.state.value}"
            }
        }

    @Test
    fun `onSendTap stream error returns to Idle and emits error event`() = runTest {
        every { streamAssistantReply(any()) } returns flowOf(Resource.Error("backend down"))

        val vm = createViewModel()

        vm.events.test {
            vm.onDraftChange("hola")
            vm.onSendTap()

            val event = awaitItem()
            assertTrue(event is ChatEvent.Error && event.message == "backend down") {
                "expected ChatEvent.Error('backend down'), got $event"
            }
        }

        assertTrue(vm.state.value is ChatUiState.Idle)
    }

    // ----- Clear conversation ------------------------------------------------

    @Test
    fun `onClearConversation delegates to repository`() = runTest {
        val vm = createViewModel()

        vm.onClearConversation()

        coVerify(exactly = 1) { repository.clearConversation() }
    }
}
