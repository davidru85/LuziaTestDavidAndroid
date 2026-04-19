package com.ruizurraca.luziatestdavid.presentation.a11y

import com.ruizurraca.luziatestdavid.R
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

/**
 * Phase 7.3.3.F — pins the peninsular-Spanish translation of every string
 * resource in `values-es/strings.xml`. Mirrors the English [A11yStringMigrationTest]
 * one-to-one so divergence between locales is loudly visible. Robolectric's
 * `@Config(qualifiers = "es")` forces the Spanish Configuration for this class.
 *
 * Translation style: European/peninsular Spanish (tú form, Spain-specific
 * vocabulary). The `role_prompts` array entries are lightly culturally adapted
 * per the Fork 4 risk note — see `MEMORY.md §Phase 7.3.3 — UX Decisions Locked`.
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "es")
class A11yStringMigrationEsTest {

    private val context = RuntimeEnvironment.getApplication()

    // region Persona name + prompt arrays (Fork 4 translation risk — see MEMORY.md)

    @Test
    fun `role_names array matches expected Spanish values`() {
        val expected = listOf("Estudiante", "Científico", "Artista")
        val actual = context.resources.getStringArray(R.array.role_names).toList()
        assertEquals(expected, actual)
    }

    @Test
    fun `role_prompts array matches expected Spanish values`() {
        val expected = listOf(
            "Eres un tutor paciente y didáctico. Explica los conceptos paso a paso y fomenta el aprendizaje.",
            "Eres un científico riguroso. Ofrece respuestas analíticas, precisas y basadas en evidencias.",
            "Eres un artista creativo. Piensa con imaginación, propón ideas y despierta la creatividad."
        )
        val actual = context.resources.getStringArray(R.array.role_prompts).toList()
        assertEquals(expected, actual)
    }

    // endregion

    // region contentDescription strings

    @Test
    fun `cd_record_voice_message matches Spanish value`() {
        assertEquals("Grabar mensaje de voz", context.getString(R.string.cd_record_voice_message))
    }

    @Test
    fun `cd_stop_recording matches Spanish value`() {
        assertEquals("Detener grabación", context.getString(R.string.cd_stop_recording))
    }

    @Test
    fun `cd_send_message matches Spanish value`() {
        assertEquals("Enviar mensaje", context.getString(R.string.cd_send_message))
    }

    @Test
    fun `cd_clear_conversation matches Spanish value`() {
        assertEquals("Borrar conversación", context.getString(R.string.cd_clear_conversation))
    }

    @Test
    fun `cd_message_input matches Spanish value`() {
        assertEquals("Entrada de mensaje", context.getString(R.string.cd_message_input))
    }

    @Test
    fun `cd_loading_response matches Spanish value`() {
        assertEquals("Cargando respuesta", context.getString(R.string.cd_loading_response))
    }

    @Test
    fun `cd_reply_failed matches Spanish value`() {
        assertEquals("Respuesta fallida", context.getString(R.string.cd_reply_failed))
    }

    @Test
    fun `cd_retry_reply matches Spanish value`() {
        assertEquals("Reintentar respuesta", context.getString(R.string.cd_retry_reply))
    }

    @Test
    fun `cd_sending_message matches Spanish value`() {
        assertEquals("Enviando mensaje", context.getString(R.string.cd_sending_message))
    }

    @Test
    fun `cd_message_sent matches Spanish value`() {
        assertEquals("Mensaje enviado", context.getString(R.string.cd_message_sent))
    }

    @Test
    fun `cd_failed_to_send matches Spanish value`() {
        assertEquals("No se pudo enviar", context.getString(R.string.cd_failed_to_send))
    }

    @Test
    fun `cd_tts_play matches Spanish value`() {
        assertEquals("Leer en voz alta", context.getString(R.string.cd_tts_play))
    }

    @Test
    fun `cd_tts_stop matches Spanish value`() {
        assertEquals("Detener lectura", context.getString(R.string.cd_tts_stop))
    }

    // endregion

    // region Dialog copy (mic permission + BlockingErrorDialog OK + clear-conversation)

    @Test
    fun `dialog_mic_permission_title matches Spanish value`() {
        assertEquals(
            "Permiso de micrófono necesario",
            context.getString(R.string.dialog_mic_permission_title)
        )
    }

    @Test
    fun `dialog_mic_permission_message matches Spanish value`() {
        assertEquals(
            "Luzia necesita acceso al micrófono para transcribir tu voz en texto.",
            context.getString(R.string.dialog_mic_permission_message)
        )
    }

    @Test
    fun `dialog_retry matches Spanish value`() {
        assertEquals("Reintentar", context.getString(R.string.dialog_retry))
    }

    @Test
    fun `dialog_cancel matches Spanish value`() {
        assertEquals("Cancelar", context.getString(R.string.dialog_cancel))
    }

    @Test
    fun `dialog_ok matches Spanish value`() {
        assertEquals("Aceptar", context.getString(R.string.dialog_ok))
    }

    @Test
    fun `dialog_clear_conversation_title matches Spanish value`() {
        assertEquals(
            "¿Borrar la conversación?",
            context.getString(R.string.dialog_clear_conversation_title)
        )
    }

    @Test
    fun `dialog_clear_conversation_message matches Spanish value`() {
        assertEquals(
            "Se eliminarán todos los mensajes.",
            context.getString(R.string.dialog_clear_conversation_message)
        )
    }

    @Test
    fun `dialog_clear matches Spanish value`() {
        assertEquals("Borrar", context.getString(R.string.dialog_clear))
    }

    // endregion

    // region Input-bar placeholder

    @Test
    fun `input_placeholder matches Spanish value`() {
        assertEquals(
            "Escribe un mensaje o toca el micro…",
            context.getString(R.string.input_placeholder)
        )
    }

    // endregion

    // region Failed assistant bubble copy

    @Test
    fun `bubble_failed_latest_message matches Spanish value`() {
        assertEquals(
            "Me he quedado en blanco. ¿Quieres intentarlo de nuevo?",
            context.getString(R.string.bubble_failed_latest_message)
        )
    }

    @Test
    fun `bubble_failed_older_message matches Spanish value`() {
        assertEquals("Perdona, mensaje vacío", context.getString(R.string.bubble_failed_older_message))
    }

    // endregion

    // region BlockingErrorDialog copy

    @Test
    fun `blocking_error_dialog_service_unavailable_title matches Spanish value`() {
        assertEquals(
            "Servicio no disponible",
            context.getString(R.string.blocking_error_dialog_service_unavailable_title)
        )
    }

    @Test
    fun `blocking_error_dialog_service_unavailable_body matches Spanish value`() {
        assertEquals(
            "Ahora mismo no podemos conectar con Luzia. Prueba de nuevo en unos instantes.",
            context.getString(R.string.blocking_error_dialog_service_unavailable_body)
        )
    }

    @Test
    fun `blocking_error_dialog_internal_title matches Spanish value`() {
        assertEquals("Algo ha fallado", context.getString(R.string.blocking_error_dialog_internal_title))
    }

    @Test
    fun `blocking_error_dialog_internal_body matches Spanish value`() {
        assertEquals(
            "Luzia ha tenido un error inesperado. Puedes intentar enviar el mensaje otra vez.",
            context.getString(R.string.blocking_error_dialog_internal_body)
        )
    }

    @Test
    fun `blocking_error_dialog_unexpected_title matches Spanish value`() {
        assertEquals("Error inesperado", context.getString(R.string.blocking_error_dialog_unexpected_title))
    }

    @Test
    fun `blocking_error_dialog_unexpected_body matches Spanish value`() {
        assertEquals(
            "Ha pasado algo que no esperábamos. Vuelve a intentarlo — si sigue ocurriendo, avísanos.",
            context.getString(R.string.blocking_error_dialog_unexpected_body)
        )
    }

    @Test
    fun `blocking_error_dialog_show_details matches Spanish value`() {
        assertEquals("Mostrar detalles", context.getString(R.string.blocking_error_dialog_show_details))
    }

    @Test
    fun `blocking_error_dialog_hide_details matches Spanish value`() {
        assertEquals("Ocultar detalles", context.getString(R.string.blocking_error_dialog_hide_details))
    }

    // endregion

    // region Streaming indicator labels (Phase 7.3.3.G)

    @Test
    fun `label_recording matches Spanish value`() {
        assertEquals("Grabando…", context.getString(R.string.label_recording))
    }

    @Test
    fun `label_transcribing matches Spanish value`() {
        assertEquals("Transcribiendo…", context.getString(R.string.label_transcribing))
    }

    @Test
    fun `label_thinking matches Spanish value`() {
        assertEquals("Pensando…", context.getString(R.string.label_thinking))
    }

    // endregion

    // region TransientSnackbar copy (Phase 7.3.3.H)

    @Test
    fun `transient_snackbar_bad_request matches Spanish value`() {
        assertEquals("La solicitud no es válida.", context.getString(R.string.transient_snackbar_bad_request))
    }

    @Test
    fun `transient_snackbar_file_too_large matches Spanish value`() {
        assertEquals(
            "El archivo de audio es demasiado grande.",
            context.getString(R.string.transient_snackbar_file_too_large)
        )
    }

    @Test
    fun `transient_snackbar_timeout matches Spanish value`() {
        assertEquals("La solicitud ha caducado.", context.getString(R.string.transient_snackbar_timeout))
    }

    @Test
    fun `transient_snackbar_network matches Spanish value`() {
        assertEquals("Error de conexión de red.", context.getString(R.string.transient_snackbar_network))
    }

    @Test
    fun `transient_snackbar_validation_error matches Spanish value`() {
        assertEquals(
            "Revisa los datos introducidos e inténtalo de nuevo.",
            context.getString(R.string.transient_snackbar_validation_error)
        )
    }

    @Test
    fun `transient_snackbar_recorder_already_running matches Spanish value`() {
        assertEquals(
            "Ya hay una grabación en curso.",
            context.getString(R.string.transient_snackbar_recorder_already_running)
        )
    }

    @Test
    fun `transient_snackbar_recorder_not_active matches Spanish value`() {
        assertEquals(
            "No hay ninguna grabación activa.",
            context.getString(R.string.transient_snackbar_recorder_not_active)
        )
    }

    @Test
    fun `transient_snackbar_recorder_no_output matches Spanish value`() {
        assertEquals(
            "No hay archivo de salida para la grabación.",
            context.getString(R.string.transient_snackbar_recorder_no_output)
        )
    }

    @Test
    fun `transient_snackbar_recorder_start_failed matches Spanish value`() {
        assertEquals(
            "No se ha podido iniciar la grabación. Inténtalo de nuevo.",
            context.getString(R.string.transient_snackbar_recorder_start_failed)
        )
    }

    @Test
    fun `transient_snackbar_recorder_stop_failed matches Spanish value`() {
        assertEquals(
            "No se ha podido detener la grabación. Inténtalo de nuevo.",
            context.getString(R.string.transient_snackbar_recorder_stop_failed)
        )
    }

    @Test
    fun `transient_snackbar_empty_audio_file matches Spanish value`() {
        assertEquals(
            "El archivo de audio está vacío. Graba de nuevo, por favor.",
            context.getString(R.string.transient_snackbar_empty_audio_file)
        )
    }

    @Test
    fun `transient_snackbar_empty_conversation_history matches Spanish value`() {
        assertEquals(
            "Empieza una conversación primero.",
            context.getString(R.string.transient_snackbar_empty_conversation_history)
        )
    }

    @Test
    fun `transient_snackbar_streaming_failed matches Spanish value`() {
        assertEquals(
            "Se ha interrumpido la transmisión de la respuesta. Inténtalo de nuevo.",
            context.getString(R.string.transient_snackbar_streaming_failed)
        )
    }

    @Test
    fun `transient_snackbar_unexpected_failure matches Spanish value`() {
        assertEquals(
            "Algo ha fallado. Inténtalo de nuevo.",
            context.getString(R.string.transient_snackbar_unexpected_failure)
        )
    }

    @Test
    fun `transient_snackbar_tts_unavailable matches Spanish value`() {
        assertEquals(
            "La lectura en voz alta no está disponible en este dispositivo.",
            context.getString(R.string.transient_snackbar_tts_unavailable)
        )
    }

    // endregion
}
