package com.ruizurraca.luziatestdavid.presentation.viewmodel

import app.cash.turbine.test
import com.ruizurraca.luziatestdavid.domain.audio.AudioRecorder
import com.ruizurraca.luziatestdavid.domain.catalog.PersonaCatalog
import com.ruizurraca.luziatestdavid.domain.common.AppError
import com.ruizurraca.luziatestdavid.domain.common.Resource
import com.ruizurraca.luziatestdavid.domain.model.ChatMessage
import com.ruizurraca.luziatestdavid.domain.model.MessageRole
import com.ruizurraca.luziatestdavid.domain.model.MessageStatus
import com.ruizurraca.luziatestdavid.domain.model.Persona
import com.ruizurraca.luziatestdavid.domain.model.PersonaEntry
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
    private val personaCatalog: PersonaCatalog = mockk()

    private val conversation = MutableStateFlow<List<ChatMessage>>(emptyList())

    private val tutorPrompt = "You are a patient, educational tutor. " +
        "Explain concepts step by step and encourage learning."
    private val scientistPrompt = "You are a rigorous scientist. " +
        "Provide evidence-based, analytical, and precise answers."
    private val artistPrompt = "You are a creative artist. " +
        "Think imaginatively, brainstorm ideas, and inspire creativity."

    private val personaEntries = listOf(
        PersonaEntry(Persona.STUDENT, "Student", tutorPrompt),
        PersonaEntry(Persona.SCIENTIST, "Scientist", scientistPrompt),
        PersonaEntry(Persona.ARTIST, "Artist", artistPrompt)
    )

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

    private fun assistantMessage(
        id: String = "a1",
        content: String = "¡Hola!",
        status: MessageStatus = MessageStatus.DELIVERED
    ) = ChatMessage(
        id = id,
        role = MessageRole.ASSISTANT,
        content = content,
        timestamp = 2_000L,
        status = status
    )

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        every { repository.observeConversation() } returns conversation
        coEvery { repository.saveMessage(any()) } just Runs
        coEvery { repository.deleteMessage(any()) } just Runs
        coEvery { repository.clearConversation() } just Runs
        every { personaCatalog.entries() } returns personaEntries
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel() = ChatViewModel(
        audioRecorder = audioRecorder,
        transcribeAudio = transcribeAudio,
        streamAssistantReply = streamAssistantReply,
        repository = repository,
        personaCatalog = personaCatalog
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
            assertTrue(event is ChatEvent.Tier1 && event.message == "mic busy") {
                "expected ChatEvent.Tier1('mic busy'), got $event"
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
            assertTrue(event is ChatEvent.Tier1 && event.message == "transcription failed") {
                "expected ChatEvent.Tier1('transcription failed'), got $event"
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
            assertTrue(event is ChatEvent.Tier1 && event.message == "backend down") {
                "expected ChatEvent.Tier1('backend down'), got $event"
            }
        }

        assertTrue(vm.state.value is ChatUiState.Idle)
    }

    @Test
    fun `stream error carrying AppError Internal emits Tier3 event`() = runTest {
        val internalError = AppError.Internal()
        every { streamAssistantReply(any()) } returns flowOf(
            Resource.Error(
                message = internalError.message,
                error = internalError
            )
        )

        val vm = createViewModel()

        vm.events.test {
            vm.onDraftChange("hola")
            vm.onSendTap()

            val event = awaitItem()
            assertTrue(event is ChatEvent.Tier3) { "expected ChatEvent.Tier3, got $event" }
            assertEquals(internalError.message, (event as ChatEvent.Tier3).message)
        }

        assertTrue(vm.state.value is ChatUiState.Idle)
    }

    @Test
    fun `stream error carrying AppError BadRequest emits Tier1 event with AppError message`() = runTest {
        val badRequest = AppError.BadRequest()
        every { streamAssistantReply(any()) } returns flowOf(
            Resource.Error(
                message = badRequest.message,
                error = badRequest
            )
        )

        val vm = createViewModel()

        vm.events.test {
            vm.onDraftChange("hola")
            vm.onSendTap()

            val event = awaitItem()
            assertTrue(event is ChatEvent.Tier1) { "expected ChatEvent.Tier1, got $event" }
            assertEquals(badRequest.message, (event as ChatEvent.Tier1).message)
        }
    }

    // ----- Clear conversation ------------------------------------------------

    @Test
    fun `onClearConversation delegates to repository`() = runTest {
        val vm = createViewModel()

        vm.onClearConversation()

        coVerify(exactly = 1) { repository.clearConversation() }
    }

    // ----- Persona selection (Phase 5.5.E, MEMORY.md Fork 1) -----------------

    @Test
    fun `selectedPersona defaults to STUDENT`() = runTest {
        val vm = createViewModel()

        assertEquals(Persona.STUDENT, vm.selectedPersona.value)
    }

    @Test
    fun `onPersonaSelected updates selectedPersona`() = runTest {
        val vm = createViewModel()

        vm.onPersonaSelected(Persona.ARTIST)

        assertEquals(Persona.ARTIST, vm.selectedPersona.value)
    }

    @Test
    fun `onPersonaSelected accepts every Persona value`() = runTest {
        val vm = createViewModel()

        vm.onPersonaSelected(Persona.SCIENTIST)
        assertEquals(Persona.SCIENTIST, vm.selectedPersona.value)

        vm.onPersonaSelected(Persona.ARTIST)
        assertEquals(Persona.ARTIST, vm.selectedPersona.value)

        vm.onPersonaSelected(Persona.STUDENT)
        assertEquals(Persona.STUDENT, vm.selectedPersona.value)
    }

    // ----- Per-message persona capture on send --------------------------------

    @Test
    fun `onSendTap attaches the default persona prompt on first send`() = runTest {
        every { streamAssistantReply(any()) } returns flowOf(Resource.Success(Unit))

        val vm = createViewModel()
        vm.onDraftChange("hola")

        vm.onSendTap()

        coVerify(exactly = 1) {
            repository.saveMessage(match {
                it.role == MessageRole.USER && it.personaPrompt == tutorPrompt
            })
        }
    }

    @Test
    fun `onSendTap attaches the currently selected persona prompt`() = runTest {
        every { streamAssistantReply(any()) } returns flowOf(Resource.Success(Unit))

        val vm = createViewModel()
        vm.onPersonaSelected(Persona.ARTIST)
        vm.onDraftChange("escríbelo como un poema")

        vm.onSendTap()

        coVerify(exactly = 1) {
            repository.saveMessage(match {
                it.role == MessageRole.USER && it.personaPrompt == artistPrompt
            })
        }
    }

    @Test
    fun `changing persona mid-conversation affects only subsequent user messages`() = runTest {
        every { streamAssistantReply(any()) } returns flowOf(Resource.Success(Unit))

        val vm = createViewModel()

        // First send under STUDENT default.
        vm.onDraftChange("pregunta 1")
        vm.onSendTap()

        // Switch persona, send again.
        vm.onPersonaSelected(Persona.ARTIST)
        vm.onDraftChange("pregunta 2")
        vm.onSendTap()

        coVerify(exactly = 1) {
            repository.saveMessage(match {
                it.content == "pregunta 1" && it.personaPrompt == tutorPrompt
            })
        }
        coVerify(exactly = 1) {
            repository.saveMessage(match {
                it.content == "pregunta 2" && it.personaPrompt == artistPrompt
            })
        }
    }

    // ----- Retry flow (Phase 5.5.E, MEMORY.md Fork 2) -------------------------

    @Test
    fun `onRetryLastFailure deletes the FAILED assistant and re-streams cleaned history`() = runTest {
        every { streamAssistantReply(any()) } returns flowOf(Resource.Success(Unit))

        val user = userMessage(id = "u1", content = "¿Cómo funciona la fotosíntesis?")
        val failed = assistantMessage(id = "a-failed", content = "", status = MessageStatus.FAILED)
        conversation.value = listOf(user, failed)

        val vm = createViewModel()

        vm.onRetryLastFailure()

        coVerify(exactly = 1) { repository.deleteMessage("a-failed") }
        coVerify(exactly = 1) {
            streamAssistantReply(match { history ->
                history.size == 1 && history.single().id == "u1"
            })
        }
    }

    @Test
    fun `onRetryLastFailure is a no-op when conversation is empty`() = runTest {
        val vm = createViewModel()

        vm.onRetryLastFailure()

        coVerify(exactly = 0) { repository.deleteMessage(any()) }
        coVerify(exactly = 0) { streamAssistantReply(any()) }
    }

    @Test
    fun `onRetryLastFailure is a no-op when no FAILED assistant is present`() = runTest {
        conversation.value = listOf(
            userMessage(id = "u1"),
            assistantMessage(id = "a1", status = MessageStatus.DELIVERED)
        )

        val vm = createViewModel()

        vm.onRetryLastFailure()

        coVerify(exactly = 0) { repository.deleteMessage(any()) }
        coVerify(exactly = 0) { streamAssistantReply(any()) }
    }
}
