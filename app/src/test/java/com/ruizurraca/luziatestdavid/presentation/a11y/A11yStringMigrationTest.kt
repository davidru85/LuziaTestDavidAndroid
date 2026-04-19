package com.ruizurraca.luziatestdavid.presentation.a11y

import com.ruizurraca.luziatestdavid.R
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

/**
 * Phase 7.3.1.A — guards against regressions in the a11y string resources
 * migrated out of hardcoded Compose literals and into `res/values/strings.xml`.
 *
 * Each test pins the expected English value of an `R.string.*` identifier so
 * that renaming a resource (or drifting its value) fails the suite loudly.
 * Dialog copy is pinned with the same intent — translation files will override
 * the value per locale, but the default (English) is the baseline both the
 * codebase and these tests key off.
 */
@RunWith(RobolectricTestRunner::class)
class A11yStringMigrationTest {

    private val context = RuntimeEnvironment.getApplication()

    // region contentDescription strings (mic / send / conversation / bubbles)

    @Test
    fun `cd_record_voice_message matches expected English value`() {
        assertEquals("Record voice message", context.getString(R.string.cd_record_voice_message))
    }

    @Test
    fun `cd_stop_recording matches expected English value`() {
        assertEquals("Stop recording", context.getString(R.string.cd_stop_recording))
    }

    @Test
    fun `cd_send_message matches expected English value`() {
        assertEquals("Send message", context.getString(R.string.cd_send_message))
    }

    @Test
    fun `cd_clear_conversation matches expected English value`() {
        assertEquals("Clear conversation", context.getString(R.string.cd_clear_conversation))
    }

    @Test
    fun `cd_message_input matches expected English value`() {
        assertEquals("Message input", context.getString(R.string.cd_message_input))
    }

    @Test
    fun `cd_loading_response matches expected English value`() {
        assertEquals("Loading response", context.getString(R.string.cd_loading_response))
    }

    @Test
    fun `cd_reply_failed matches expected English value`() {
        assertEquals("Reply failed", context.getString(R.string.cd_reply_failed))
    }

    @Test
    fun `cd_retry_reply matches expected English value`() {
        assertEquals("Retry reply", context.getString(R.string.cd_retry_reply))
    }

    @Test
    fun `cd_sending_message matches expected English value`() {
        assertEquals("Sending message", context.getString(R.string.cd_sending_message))
    }

    @Test
    fun `cd_message_sent matches expected English value`() {
        assertEquals("Message sent", context.getString(R.string.cd_message_sent))
    }

    @Test
    fun `cd_failed_to_send matches expected English value`() {
        assertEquals("Failed to send", context.getString(R.string.cd_failed_to_send))
    }

    @Test
    fun `cd_tts_play matches expected English value`() {
        assertEquals("Read aloud", context.getString(R.string.cd_tts_play))
    }

    @Test
    fun `cd_tts_stop matches expected English value`() {
        assertEquals("Stop reading aloud", context.getString(R.string.cd_tts_stop))
    }

    // endregion

    // region Dialog copy (mic-rationale + BlockingErrorDialog dismiss)

    @Test
    fun `dialog_mic_permission_title matches expected English value`() {
        assertEquals(
            "Microphone permission needed",
            context.getString(R.string.dialog_mic_permission_title)
        )
    }

    @Test
    fun `dialog_mic_permission_message matches expected English value`() {
        assertEquals(
            "Luzia needs microphone access to transcribe your voice into text.",
            context.getString(R.string.dialog_mic_permission_message)
        )
    }

    @Test
    fun `dialog_retry matches expected English value`() {
        assertEquals("Retry", context.getString(R.string.dialog_retry))
    }

    @Test
    fun `dialog_cancel matches expected English value`() {
        assertEquals("Cancel", context.getString(R.string.dialog_cancel))
    }

    @Test
    fun `dialog_ok matches expected English value`() {
        assertEquals("OK", context.getString(R.string.dialog_ok))
    }

    // endregion

    // region Clear-conversation confirmation dialog (ChatTopAppBar)

    @Test
    fun `dialog_clear_conversation_title matches expected English value`() {
        assertEquals(
            "Clear conversation?",
            context.getString(R.string.dialog_clear_conversation_title)
        )
    }

    @Test
    fun `dialog_clear_conversation_message matches expected English value`() {
        assertEquals(
            "This will remove all messages.",
            context.getString(R.string.dialog_clear_conversation_message)
        )
    }

    @Test
    fun `dialog_clear matches expected English value`() {
        assertEquals("Clear", context.getString(R.string.dialog_clear))
    }

    // endregion

    // region Input-bar placeholder

    @Test
    fun `input_placeholder matches expected English value`() {
        assertEquals(
            "Type a message or tap the mic…",
            context.getString(R.string.input_placeholder)
        )
    }

    // endregion

    // region Failed assistant bubble copy (Phase 7.3.3.B)

    @Test
    fun `bubble_failed_latest_message matches expected English value`() {
        assertEquals(
            "I've gone blank. Mind retrying?",
            context.getString(R.string.bubble_failed_latest_message)
        )
    }

    @Test
    fun `bubble_failed_older_message matches expected English value`() {
        assertEquals(
            "Sorry, empty message",
            context.getString(R.string.bubble_failed_older_message)
        )
    }

    // endregion

    // region BlockingErrorDialog copy (Phase 7.3.3.C)

    @Test
    fun `blocking_error_dialog_service_unavailable_title matches expected English value`() {
        assertEquals(
            "Service unavailable",
            context.getString(R.string.blocking_error_dialog_service_unavailable_title)
        )
    }

    @Test
    fun `blocking_error_dialog_service_unavailable_body matches expected English value`() {
        assertEquals(
            "We can't reach Luzia right now. Please try again in a moment.",
            context.getString(R.string.blocking_error_dialog_service_unavailable_body)
        )
    }

    @Test
    fun `blocking_error_dialog_internal_title matches expected English value`() {
        assertEquals(
            "Something went wrong",
            context.getString(R.string.blocking_error_dialog_internal_title)
        )
    }

    @Test
    fun `blocking_error_dialog_internal_body matches expected English value`() {
        assertEquals(
            "Luzia hit an unexpected error. You can try sending your message again.",
            context.getString(R.string.blocking_error_dialog_internal_body)
        )
    }

    @Test
    fun `blocking_error_dialog_unexpected_title matches expected English value`() {
        assertEquals(
            "Unexpected error",
            context.getString(R.string.blocking_error_dialog_unexpected_title)
        )
    }

    @Test
    fun `blocking_error_dialog_unexpected_body matches expected English value`() {
        assertEquals(
            "Something we didn't anticipate happened. Please try again — if it keeps happening, let us know.",
            context.getString(R.string.blocking_error_dialog_unexpected_body)
        )
    }

    @Test
    fun `blocking_error_dialog_show_details matches expected English value`() {
        assertEquals("Show details", context.getString(R.string.blocking_error_dialog_show_details))
    }

    @Test
    fun `blocking_error_dialog_hide_details matches expected English value`() {
        assertEquals("Hide details", context.getString(R.string.blocking_error_dialog_hide_details))
    }

    // endregion

    // region Streaming indicator labels + brand (Phase 7.3.3.G)

    @Test
    fun `label_recording matches expected English value`() {
        assertEquals("Recording…", context.getString(R.string.label_recording))
    }

    @Test
    fun `label_transcribing matches expected English value`() {
        assertEquals("Transcribing…", context.getString(R.string.label_transcribing))
    }

    @Test
    fun `label_thinking matches expected English value`() {
        assertEquals("Thinking…", context.getString(R.string.label_thinking))
    }

    @Test
    fun `app_title is the Luzia brand and does not vary by locale`() {
        // translatable="false" in strings.xml — no per-locale overrides.
        assertEquals("Luzia", context.getString(R.string.app_title))
    }

    // endregion

    // region TransientSnackbar copy (Phase 7.3.3.H.2 — composable-resolved)

    @Test
    fun `transient_snackbar_bad_request matches expected English value`() {
        assertEquals("The request was invalid.", context.getString(R.string.transient_snackbar_bad_request))
    }

    @Test
    fun `transient_snackbar_file_too_large matches expected English value`() {
        assertEquals("The audio file is too large.", context.getString(R.string.transient_snackbar_file_too_large))
    }

    @Test
    fun `transient_snackbar_timeout matches expected English value`() {
        assertEquals("The request timed out.", context.getString(R.string.transient_snackbar_timeout))
    }

    @Test
    fun `transient_snackbar_network matches expected English value`() {
        assertEquals("Network connection failed.", context.getString(R.string.transient_snackbar_network))
    }

    @Test
    fun `transient_snackbar_validation_error matches expected English value`() {
        assertEquals(
            "Please check your input and try again.",
            context.getString(R.string.transient_snackbar_validation_error)
        )
    }

    @Test
    fun `transient_snackbar_recorder_already_running matches expected English value`() {
        assertEquals(
            "Recording is already in progress.",
            context.getString(R.string.transient_snackbar_recorder_already_running)
        )
    }

    @Test
    fun `transient_snackbar_recorder_not_active matches expected English value`() {
        assertEquals("No active recording.", context.getString(R.string.transient_snackbar_recorder_not_active))
    }

    @Test
    fun `transient_snackbar_recorder_no_output matches expected English value`() {
        assertEquals(
            "No output file for the recording.",
            context.getString(R.string.transient_snackbar_recorder_no_output)
        )
    }

    @Test
    fun `transient_snackbar_recorder_start_failed matches expected English value`() {
        assertEquals(
            "Couldn't start the recording. Please try again.",
            context.getString(R.string.transient_snackbar_recorder_start_failed)
        )
    }

    @Test
    fun `transient_snackbar_recorder_stop_failed matches expected English value`() {
        assertEquals(
            "Couldn't stop the recording. Please try again.",
            context.getString(R.string.transient_snackbar_recorder_stop_failed)
        )
    }

    @Test
    fun `transient_snackbar_empty_audio_file matches expected English value`() {
        assertEquals(
            "The audio file is empty. Please record again.",
            context.getString(R.string.transient_snackbar_empty_audio_file)
        )
    }

    @Test
    fun `transient_snackbar_empty_conversation_history matches expected English value`() {
        assertEquals(
            "Start a conversation first.",
            context.getString(R.string.transient_snackbar_empty_conversation_history)
        )
    }

    @Test
    fun `transient_snackbar_streaming_failed matches expected English value`() {
        assertEquals(
            "The reply stream was interrupted. Please try again.",
            context.getString(R.string.transient_snackbar_streaming_failed)
        )
    }

    @Test
    fun `transient_snackbar_unexpected_failure matches expected English value`() {
        assertEquals(
            "Something went wrong. Please try again.",
            context.getString(R.string.transient_snackbar_unexpected_failure)
        )
    }

    @Test
    fun `transient_snackbar_tts_unavailable matches expected English value`() {
        assertEquals(
            "Read aloud isn't available on this device.",
            context.getString(R.string.transient_snackbar_tts_unavailable)
        )
    }

    // endregion
}
