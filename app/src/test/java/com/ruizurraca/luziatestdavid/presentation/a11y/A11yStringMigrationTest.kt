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

    // endregion

    // region Dialog copy (mic-rationale + Tier-3 dismiss)

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
}
