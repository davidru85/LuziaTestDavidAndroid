package com.ruizurraca.luziatestdavid.presentation.a11y

import com.ruizurraca.luziatestdavid.R
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

/**
 * Phase 7.3.3.F — pins the (neutral European) Portuguese translation of every
 * string resource in `values-pt/strings.xml`. Mirrors the English
 * [A11yStringMigrationTest] one-to-one. Robolectric's `@Config(qualifiers = "pt")`
 * forces the Portuguese Configuration for this class.
 *
 * Translation style: neutral European Portuguese (tu form, EU vocabulary — e.g.
 * "a carregar", "aceder", "novamente"). The `role_prompts` array entries are
 * lightly culturally adapted per the Fork 4 risk note — see
 * `MEMORY.md §Phase 7.3.3 — UX Decisions Locked`.
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "pt")
class A11yStringMigrationPtTest {

    private val context = RuntimeEnvironment.getApplication()

    // region Persona name + prompt arrays (Fork 4 translation risk — see MEMORY.md)

    @Test
    fun `role_names array matches expected Portuguese values`() {
        val expected = listOf("Estudante", "Cientista", "Artista")
        val actual = context.resources.getStringArray(R.array.role_names).toList()
        assertEquals(expected, actual)
    }

    @Test
    fun `role_prompts array matches expected Portuguese values`() {
        val expected = listOf(
            "És um tutor paciente e didático. Explica os conceitos passo a passo e incentiva a aprendizagem.",
            "És um cientista rigoroso. Oferece respostas analíticas, precisas e fundamentadas em evidências.",
            "És um artista criativo. Pensa com imaginação, propõe ideias e desperta a criatividade."
        )
        val actual = context.resources.getStringArray(R.array.role_prompts).toList()
        assertEquals(expected, actual)
    }

    // endregion

    // region contentDescription strings

    @Test
    fun `cd_record_voice_message matches Portuguese value`() {
        assertEquals("Gravar mensagem de voz", context.getString(R.string.cd_record_voice_message))
    }

    @Test
    fun `cd_stop_recording matches Portuguese value`() {
        assertEquals("Parar a gravação", context.getString(R.string.cd_stop_recording))
    }

    @Test
    fun `cd_send_message matches Portuguese value`() {
        assertEquals("Enviar mensagem", context.getString(R.string.cd_send_message))
    }

    @Test
    fun `cd_clear_conversation matches Portuguese value`() {
        assertEquals("Limpar conversa", context.getString(R.string.cd_clear_conversation))
    }

    @Test
    fun `cd_message_input matches Portuguese value`() {
        assertEquals("Entrada de mensagem", context.getString(R.string.cd_message_input))
    }

    @Test
    fun `cd_loading_response matches Portuguese value`() {
        assertEquals("A carregar resposta", context.getString(R.string.cd_loading_response))
    }

    @Test
    fun `cd_reply_failed matches Portuguese value`() {
        assertEquals("Falha na resposta", context.getString(R.string.cd_reply_failed))
    }

    @Test
    fun `cd_retry_reply matches Portuguese value`() {
        assertEquals("Tentar novamente", context.getString(R.string.cd_retry_reply))
    }

    @Test
    fun `cd_sending_message matches Portuguese value`() {
        assertEquals("A enviar mensagem", context.getString(R.string.cd_sending_message))
    }

    @Test
    fun `cd_message_sent matches Portuguese value`() {
        assertEquals("Mensagem enviada", context.getString(R.string.cd_message_sent))
    }

    @Test
    fun `cd_failed_to_send matches Portuguese value`() {
        assertEquals("Falha no envio", context.getString(R.string.cd_failed_to_send))
    }

    @Test
    fun `cd_tts_play matches Portuguese value`() {
        assertEquals("Ler em voz alta", context.getString(R.string.cd_tts_play))
    }

    @Test
    fun `cd_tts_stop matches Portuguese value`() {
        assertEquals("Parar leitura", context.getString(R.string.cd_tts_stop))
    }

    // endregion

    // region Dialog copy (mic permission + Tier-3 OK + clear-conversation)

    @Test
    fun `dialog_mic_permission_title matches Portuguese value`() {
        assertEquals(
            "É necessária permissão do microfone",
            context.getString(R.string.dialog_mic_permission_title)
        )
    }

    @Test
    fun `dialog_mic_permission_message matches Portuguese value`() {
        assertEquals(
            "A Luzia precisa de aceder ao microfone para transcrever a tua voz em texto.",
            context.getString(R.string.dialog_mic_permission_message)
        )
    }

    @Test
    fun `dialog_retry matches Portuguese value`() {
        assertEquals("Tentar novamente", context.getString(R.string.dialog_retry))
    }

    @Test
    fun `dialog_cancel matches Portuguese value`() {
        assertEquals("Cancelar", context.getString(R.string.dialog_cancel))
    }

    @Test
    fun `dialog_ok matches Portuguese value`() {
        assertEquals("OK", context.getString(R.string.dialog_ok))
    }

    @Test
    fun `dialog_clear_conversation_title matches Portuguese value`() {
        assertEquals(
            "Limpar a conversa?",
            context.getString(R.string.dialog_clear_conversation_title)
        )
    }

    @Test
    fun `dialog_clear_conversation_message matches Portuguese value`() {
        assertEquals(
            "Serão removidas todas as mensagens.",
            context.getString(R.string.dialog_clear_conversation_message)
        )
    }

    @Test
    fun `dialog_clear matches Portuguese value`() {
        assertEquals("Limpar", context.getString(R.string.dialog_clear))
    }

    // endregion

    // region Input-bar placeholder

    @Test
    fun `input_placeholder matches Portuguese value`() {
        assertEquals(
            "Escreve uma mensagem ou toca no microfone…",
            context.getString(R.string.input_placeholder)
        )
    }

    // endregion

    // region Failed assistant bubble copy

    @Test
    fun `bubble_failed_latest_message matches Portuguese value`() {
        assertEquals(
            "Fiquei em branco. Queres tentar novamente?",
            context.getString(R.string.bubble_failed_latest_message)
        )
    }

    @Test
    fun `bubble_failed_older_message matches Portuguese value`() {
        assertEquals("Desculpa, mensagem vazia", context.getString(R.string.bubble_failed_older_message))
    }

    // endregion

    // region Tier-3 AlertDialog copy

    @Test
    fun `dialog_tier3_service_unavailable_title matches Portuguese value`() {
        assertEquals(
            "Serviço indisponível",
            context.getString(R.string.dialog_tier3_service_unavailable_title)
        )
    }

    @Test
    fun `dialog_tier3_service_unavailable_body matches Portuguese value`() {
        assertEquals(
            "Neste momento não conseguimos ligar à Luzia. Tenta novamente daqui a pouco.",
            context.getString(R.string.dialog_tier3_service_unavailable_body)
        )
    }

    @Test
    fun `dialog_tier3_internal_title matches Portuguese value`() {
        assertEquals("Algo correu mal", context.getString(R.string.dialog_tier3_internal_title))
    }

    @Test
    fun `dialog_tier3_internal_body matches Portuguese value`() {
        assertEquals(
            "A Luzia teve um erro inesperado. Podes tentar enviar a mensagem novamente.",
            context.getString(R.string.dialog_tier3_internal_body)
        )
    }

    @Test
    fun `dialog_tier3_unexpected_title matches Portuguese value`() {
        assertEquals("Erro inesperado", context.getString(R.string.dialog_tier3_unexpected_title))
    }

    @Test
    fun `dialog_tier3_unexpected_body matches Portuguese value`() {
        assertEquals(
            "Aconteceu algo que não esperávamos. Tenta novamente — se continuar a acontecer, avisa-nos.",
            context.getString(R.string.dialog_tier3_unexpected_body)
        )
    }

    @Test
    fun `dialog_tier3_show_details matches Portuguese value`() {
        assertEquals("Mostrar detalhes", context.getString(R.string.dialog_tier3_show_details))
    }

    @Test
    fun `dialog_tier3_hide_details matches Portuguese value`() {
        assertEquals("Ocultar detalhes", context.getString(R.string.dialog_tier3_hide_details))
    }

    // endregion

    // region Streaming indicator labels (Phase 7.3.3.G)

    @Test
    fun `label_recording matches Portuguese value`() {
        assertEquals("A gravar…", context.getString(R.string.label_recording))
    }

    @Test
    fun `label_transcribing matches Portuguese value`() {
        assertEquals("A transcrever…", context.getString(R.string.label_transcribing))
    }

    @Test
    fun `label_thinking matches Portuguese value`() {
        assertEquals("A pensar…", context.getString(R.string.label_thinking))
    }

    // endregion

    // region Tier-1 Snackbar copy (Phase 7.3.3.H)

    @Test
    fun `tier1_bad_request matches Portuguese value`() {
        assertEquals("O pedido é inválido.", context.getString(R.string.tier1_bad_request))
    }

    @Test
    fun `tier1_file_too_large matches Portuguese value`() {
        assertEquals(
            "O ficheiro de áudio é demasiado grande.",
            context.getString(R.string.tier1_file_too_large)
        )
    }

    @Test
    fun `tier1_timeout matches Portuguese value`() {
        assertEquals("O pedido excedeu o tempo limite.", context.getString(R.string.tier1_timeout))
    }

    @Test
    fun `tier1_network matches Portuguese value`() {
        assertEquals("Falha na ligação à rede.", context.getString(R.string.tier1_network))
    }

    @Test
    fun `tier1_validation_error matches Portuguese value`() {
        assertEquals(
            "Verifica os dados introduzidos e tenta novamente.",
            context.getString(R.string.tier1_validation_error)
        )
    }

    @Test
    fun `tier1_recorder_already_running matches Portuguese value`() {
        assertEquals(
            "Já existe uma gravação em curso.",
            context.getString(R.string.tier1_recorder_already_running)
        )
    }

    @Test
    fun `tier1_recorder_not_active matches Portuguese value`() {
        assertEquals(
            "Não há nenhuma gravação ativa.",
            context.getString(R.string.tier1_recorder_not_active)
        )
    }

    @Test
    fun `tier1_recorder_no_output matches Portuguese value`() {
        assertEquals(
            "Não há ficheiro de saída para a gravação.",
            context.getString(R.string.tier1_recorder_no_output)
        )
    }

    @Test
    fun `tier1_recorder_start_failed matches Portuguese value`() {
        assertEquals(
            "Não foi possível iniciar a gravação. Tenta novamente.",
            context.getString(R.string.tier1_recorder_start_failed)
        )
    }

    @Test
    fun `tier1_recorder_stop_failed matches Portuguese value`() {
        assertEquals(
            "Não foi possível parar a gravação. Tenta novamente.",
            context.getString(R.string.tier1_recorder_stop_failed)
        )
    }

    @Test
    fun `tier1_empty_audio_file matches Portuguese value`() {
        assertEquals(
            "O ficheiro de áudio está vazio. Grava novamente, por favor.",
            context.getString(R.string.tier1_empty_audio_file)
        )
    }

    @Test
    fun `tier1_empty_conversation_history matches Portuguese value`() {
        assertEquals(
            "Começa uma conversa primeiro.",
            context.getString(R.string.tier1_empty_conversation_history)
        )
    }

    @Test
    fun `tier1_streaming_failed matches Portuguese value`() {
        assertEquals(
            "A transmissão da resposta foi interrompida. Tenta novamente.",
            context.getString(R.string.tier1_streaming_failed)
        )
    }

    @Test
    fun `tier1_unexpected_failure matches Portuguese value`() {
        assertEquals(
            "Algo correu mal. Tenta novamente.",
            context.getString(R.string.tier1_unexpected_failure)
        )
    }

    @Test
    fun `tier1_tts_unavailable matches Portuguese value`() {
        assertEquals(
            "A leitura em voz alta não está disponível neste dispositivo.",
            context.getString(R.string.tier1_tts_unavailable)
        )
    }

    // endregion
}
