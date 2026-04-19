package com.ruizurraca.luziatestdavid.presentation.viewmodel

import app.cash.turbine.test
import com.ruizurraca.luziatestdavid.domain.audio.AudioRecorder
import com.ruizurraca.luziatestdavid.domain.audio.TextSpeaker
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
import com.ruizurraca.luziatestdavid.presentation.state.TransientSnackbarKind
import com.ruizurraca.luziatestdavid.presentation.state.BlockingErrorDialogKind
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
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
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.io.File
import java.util.Locale

@OptIn(ExperimentalCoroutinesApi::class)
class ChatViewModelTest {

    private val audioRecorder: AudioRecorder = mockk()
    private val transcribeAudio: TranscribeAudioUseCase = mockk()
    private val streamAssistantReply: StreamAssistantReplyUseCase = mockk()
    private val repository: ChatRepository = mockk()
    private val personaCatalog: PersonaCatalog = mockk()
    private val textSpeaker: TextSpeaker = mockk(relaxed = true)

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
        personaCatalog = personaCatalog,
        textSpeaker = textSpeaker
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
            assertTrue(
                event is ChatEvent.TransientSnackbar &&
                    event.kind == TransientSnackbarKind.Unknown &&
                    event.backendMessage == "mic busy"
            ) {
                "expected legacy-path TransientSnackbar(Unknown, backendMessage='mic busy'), got $event"
            }
        }

        assertTrue(vm.state.value is ChatUiState.Idle)
    }

    @Test
    fun `stopRecording happy path transcribes populates draft and deletes temp audio file`() = runTest {
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
        // Temp .m4a deleted after successful transcription (Phase 7.4.A)
        assertFalse(fakeFile.exists()) { "expected temp audio file deleted, still at ${fakeFile.absolutePath}" }
    }

    @Test
    fun `stopRecording transcription failure returns to Idle, emits error, and still deletes temp audio`() = runTest {
        coEvery { audioRecorder.start() } returns Resource.Success(Unit)
        val fakeFile = File.createTempFile("luzia", ".m4a")
        coEvery { audioRecorder.stop() } returns Resource.Success(fakeFile)
        coEvery { transcribeAudio(fakeFile) } returns Resource.Error("transcription failed")

        val vm = createViewModel()

        vm.events.test {
            vm.startRecording()
            vm.stopRecording()

            val event = awaitItem()
            assertTrue(
                event is ChatEvent.TransientSnackbar &&
                    event.kind == TransientSnackbarKind.Unknown &&
                    event.backendMessage == "transcription failed"
            ) {
                "expected legacy-path TransientSnackbar(Unknown, backendMessage='transcription failed'), got $event"
            }
        }

        // Temp .m4a is deleted regardless of transcription outcome (Phase 7.4.A)
        assertFalse(fakeFile.exists()) { "expected temp audio file deleted, still at ${fakeFile.absolutePath}" }

        assertTrue(vm.state.value is ChatUiState.Idle)
        assertEquals("", vm.state.value.draft)
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
            assertTrue(
                event is ChatEvent.TransientSnackbar &&
                    event.kind == TransientSnackbarKind.Unknown &&
                    event.backendMessage == "backend down"
            ) {
                "expected legacy-path TransientSnackbar(Unknown, backendMessage='backend down'), got $event"
            }
        }

        assertTrue(vm.state.value is ChatUiState.Idle)
    }

    @Test
    fun `stream error carrying AppError Internal emits BlockingErrorDialog event with InternalError kind`() = runTest {
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
            assertTrue(event is ChatEvent.BlockingErrorDialog) { "expected ChatEvent.BlockingErrorDialog, got $event" }
            val blockingError = event as ChatEvent.BlockingErrorDialog
            assertEquals(BlockingErrorDialogKind.InternalError, blockingError.kind)
            assertEquals(internalError.message, blockingError.detailsMessage)
        }

        assertTrue(vm.state.value is ChatUiState.Idle)
    }

    @Test
    fun `stream error carrying AppError BadRequest emits TransientSnackbar event with AppError message`() = runTest {
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
            assertTrue(event is ChatEvent.TransientSnackbar) { "expected ChatEvent.TransientSnackbar, got $event" }
            val snackbar = event as ChatEvent.TransientSnackbar
            assertEquals(TransientSnackbarKind.BadRequest, snackbar.kind)
            // Default BadRequest rawMessage → backendMessage is null (composable
            // resolves the translated kind copy).
            assertNull(snackbar.backendMessage)
        }
    }

    // ----- Clear conversation ------------------------------------------------

    // region Phase 7.4.B — defensive recorder release on VM clear

    @Test
    fun `onCleared releases the audio recorder so background recordings are torn down`() = runTest {
        every { audioRecorder.release() } just Runs
        val vm = createViewModel()

        // Invoke the lifecycle hook directly — the override is widened to public
        // so tests can simulate what the framework does when the owning Activity
        // is destroyed while a recording is still in flight.
        vm.onCleared()

        verify(exactly = 1) { audioRecorder.release() }
    }

    // endregion

    // region Phase 10.6.D — TTS on last received assistant message

    private val englishLocale: Locale = Locale.forLanguageTag("en-US")

    @Test
    fun `currentlySpeakingId defaults to null`() = runTest {
        val vm = createViewModel()

        assertNull(vm.currentlySpeakingId.value)
    }

    @Test
    fun `onTtsTap on idle VM starts speaking and exposes messageId via currentlySpeakingId`() = runTest {
        // speak() suspends indefinitely — simulates an in-flight utterance so the
        // ID is observable while playback is active.
        val speakGate = kotlinx.coroutines.CompletableDeferred<Resource<Unit>>()
        coEvery { textSpeaker.speak(any(), any()) } coAnswers { speakGate.await() }
        val vm = createViewModel()

        vm.onTtsTap(messageId = "a1", text = "Hello", locale = englishLocale)

        assertEquals("a1", vm.currentlySpeakingId.value)
        coVerify(exactly = 1) { textSpeaker.speak("Hello", englishLocale) }
        // clean up the dangling coroutine so runTest doesn't complain
        speakGate.complete(Resource.Success(Unit))
    }

    @Test
    fun `onTtsTap a second time on the same message stops playback and clears currentlySpeakingId`() = runTest {
        val speakGate = kotlinx.coroutines.CompletableDeferred<Resource<Unit>>()
        coEvery { textSpeaker.speak(any(), any()) } coAnswers { speakGate.await() }
        val vm = createViewModel()
        vm.onTtsTap(messageId = "a1", text = "Hello", locale = englishLocale)
        assertEquals("a1", vm.currentlySpeakingId.value)

        vm.onTtsTap(messageId = "a1", text = "Hello", locale = englishLocale)

        assertNull(vm.currentlySpeakingId.value)
        verify(atLeast = 1) { textSpeaker.stop() }
        speakGate.complete(Resource.Success(Unit))
    }

    @Test
    fun `onTtsTap on a different message while one is playing switches playback to the new message`() = runTest {
        val firstGate = kotlinx.coroutines.CompletableDeferred<Resource<Unit>>()
        val secondGate = kotlinx.coroutines.CompletableDeferred<Resource<Unit>>()
        coEvery { textSpeaker.speak("First", any()) } coAnswers { firstGate.await() }
        coEvery { textSpeaker.speak("Second", any()) } coAnswers { secondGate.await() }
        val vm = createViewModel()
        vm.onTtsTap(messageId = "a1", text = "First", locale = englishLocale)
        assertEquals("a1", vm.currentlySpeakingId.value)

        vm.onTtsTap(messageId = "a2", text = "Second", locale = englishLocale)

        assertEquals("a2", vm.currentlySpeakingId.value)
        verify(atLeast = 1) { textSpeaker.stop() }
        coVerify(exactly = 1) { textSpeaker.speak("Second", englishLocale) }
        firstGate.complete(Resource.Success(Unit))
        secondGate.complete(Resource.Success(Unit))
    }

    @Test
    fun `onTtsTap clears currentlySpeakingId when speak completes naturally`() = runTest {
        coEvery { textSpeaker.speak(any(), any()) } returns Resource.Success(Unit)
        val vm = createViewModel()

        vm.onTtsTap(messageId = "a1", text = "Short utterance", locale = englishLocale)

        assertNull(vm.currentlySpeakingId.value)
    }

    @Test
    fun `onTtsTap emits TransientSnackbar TtsUnavailable event when speak returns TtsUnavailable error`() = runTest {
        coEvery {
            textSpeaker.speak(any(), any())
        } returns AppError.TtsUnavailable.toResourceError()
        val vm = createViewModel()

        vm.events.test {
            vm.onTtsTap(messageId = "a1", text = "Hello", locale = englishLocale)
            val event = awaitItem()
            assertTrue(
                event is ChatEvent.TransientSnackbar && event.kind == TransientSnackbarKind.TtsUnavailable
            ) {
                "expected TransientSnackbar(TtsUnavailable), got $event"
            }
        }
        assertNull(vm.currentlySpeakingId.value)
    }

    @Test
    fun `onCleared releases the TextSpeaker alongside the AudioRecorder`() = runTest {
        every { audioRecorder.release() } just Runs
        every { textSpeaker.release() } just Runs
        val vm = createViewModel()

        vm.onCleared()

        verify(exactly = 1) { textSpeaker.release() }
        verify(exactly = 1) { audioRecorder.release() }
    }

    // endregion

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
