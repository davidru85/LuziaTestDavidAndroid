package com.ruizurraca.luziatestdavid.data.remote.mapper

import com.ruizurraca.luziatestdavid.data.remote.dto.ChatMessageDto
import com.ruizurraca.luziatestdavid.data.remote.dto.ChatRequestDto
import com.ruizurraca.luziatestdavid.domain.locale.LocaleProvider
import com.ruizurraca.luziatestdavid.domain.model.ChatMessage
import com.ruizurraca.luziatestdavid.domain.model.MessageRole
import com.ruizurraca.luziatestdavid.domain.model.MessageStatus
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

/**
 * Contract tests for [ChatMapper] against the **Fork 4** three-field wire shape
 * (`TECHNICAL_SPEC.md §API Contracts #2`):
 *
 *   { "messages": [
 *       { "role": "user", "role_prompt": "<persona>", "content": "..." },
 *       { "role": "assistant", "content": "..." }
 *   ] }
 *
 * - `role` is a strict enum: `"user"` / `"assistant"` (Android never emits `"system"`).
 * - `role_prompt` is required on user turns, omitted entirely on assistant turns
 *   (JSON field absent, not `null`).
 * - Null / blank `personaPrompt` on a user message is a client invariant violation —
 *   mapper throws `IllegalStateException` (Fork 4 addendum §3, user-confirmed
 *   option A).
 * - Domain-only fields (`id`, `timestamp`, `status`) MUST NOT appear on the wire.
 */
class ChatMapperTest {

    /**
     * Default mapper under test: no locale resolved. This matches the
     * pre-v1.4.0 behaviour (`lang` absent from the wire) so the Fork 4-era
     * tests below keep their byte-level JSON pins. The Phase 10.6.H lang
     * passthrough tests at the bottom of the file use a mapper built via
     * [mapperWith] with a non-null language instead.
     */
    private val mapper = mapperWith(lang = null)

    private fun mapperWith(lang: String?): ChatMapper = ChatMapper(
        localeProvider = object : LocaleProvider {
            override fun currentLanguage(): String? = lang
        }
    )

    /**
     * Matches the production Json config in [NetworkModule] so encoded-JSON
     * assertions in this test reflect the real wire shape.
     */
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        explicitNulls = false
    }

    private val tutor = "You are a patient, educational tutor. " +
        "Explain concepts step by step and encourage learning."
    private val artist = "You are a creative artist. " +
        "Think imaginatively, brainstorm ideas, and inspire creativity."

    // region Basic shape

    @Test
    fun `toRequestDto maps a single USER message to user role plus role_prompt plus content`() {
        val messages = listOf(
            ChatMessage(
                id = "u1",
                role = MessageRole.USER,
                content = "Hello",
                timestamp = 1_000L,
                personaPrompt = tutor
            )
        )

        val dto = mapper.toRequestDto(messages)

        assertEquals(
            ChatRequestDto(
                listOf(
                    ChatMessageDto(role = "user", rolePrompt = tutor, content = "Hello")
                )
            ),
            dto
        )
    }

    @Test
    fun `toRequestDto maps an ASSISTANT message to assistant role with null rolePrompt`() {
        val messages = listOf(
            ChatMessage(
                id = "a1",
                role = MessageRole.ASSISTANT,
                content = "La fotosíntesis es…",
                timestamp = 2L
            )
        )

        val dto = mapper.toRequestDto(messages)

        assertEquals(
            ChatRequestDto(
                listOf(
                    ChatMessageDto(role = "assistant", rolePrompt = null, content = "La fotosíntesis es…")
                )
            ),
            dto
        )
    }

    @Test
    fun `toRequestDto preserves message order`() {
        val messages = listOf(
            ChatMessage("a", MessageRole.USER, "first", 1L, personaPrompt = tutor),
            ChatMessage("b", MessageRole.ASSISTANT, "second", 2L),
            ChatMessage("c", MessageRole.USER, "third", 3L, personaPrompt = tutor),
            ChatMessage("d", MessageRole.ASSISTANT, "fourth", 4L)
        )

        val dto = mapper.toRequestDto(messages)

        assertEquals(
            listOf("first", "second", "third", "fourth"),
            dto.messages.map { it.content }
        )
    }

    @Test
    fun `toRequestDto preserves content verbatim including whitespace and unicode`() {
        val tricky = "  leading spaces\n\ttab + newline — em-dash 😀 привет "
        val messages = listOf(
            ChatMessage("u", MessageRole.USER, tricky, 1L, personaPrompt = tutor)
        )

        val dto = mapper.toRequestDto(messages)

        assertEquals(tricky, dto.messages.single().content)
    }

    @Test
    fun `toRequestDto emits empty messages list for empty input`() {
        val dto = mapper.toRequestDto(emptyList())

        assertEquals(ChatRequestDto(emptyList()), dto)
    }

    @Test
    fun `toRequestDto ignores domain-only fields (id, timestamp, status)`() {
        val messages = listOf(
            ChatMessage(
                id = "irrelevant-uuid",
                role = MessageRole.USER,
                content = "hi",
                timestamp = 9_999_999L,
                status = MessageStatus.PENDING,
                personaPrompt = tutor
            )
        )

        val encoded = json.encodeToString(
            ChatRequestDto.serializer(),
            mapper.toRequestDto(messages)
        )

        assertEquals(
            """{"messages":[{"role":"user","role_prompt":"$tutor","content":"hi"}]}""",
            encoded
        )
    }

    // endregion

    // region Null / blank personaPrompt on user message — IllegalStateException (Fork 4 §3)

    @Test
    fun `toRequestDto throws IllegalStateException when a USER message has null personaPrompt`() {
        val messages = listOf(
            ChatMessage(
                id = "u1",
                role = MessageRole.USER,
                content = "Hello",
                timestamp = 1L,
                personaPrompt = null
            )
        )

        assertThrows<IllegalStateException> {
            mapper.toRequestDto(messages)
        }
    }

    @Test
    fun `toRequestDto throws IllegalStateException when a USER message has blank personaPrompt`() {
        val messages = listOf(
            ChatMessage(
                id = "u1",
                role = MessageRole.USER,
                content = "Hello",
                timestamp = 1L,
                personaPrompt = "   "
            )
        )

        assertThrows<IllegalStateException> {
            mapper.toRequestDto(messages)
        }
    }

    @Test
    fun `toRequestDto throws IllegalStateException when any historical USER message has null personaPrompt`() {
        // Backend rejects missing role_prompt on the last user turn AND every historical
        // user turn (Fork 4 addendum §3). The mapper must surface that invariant
        // client-side before the request leaves the device.
        val messages = listOf(
            ChatMessage("u0", MessageRole.USER, "legacy", 1L, personaPrompt = null),
            ChatMessage("a0", MessageRole.ASSISTANT, "reply", 2L),
            ChatMessage("u1", MessageRole.USER, "current", 3L, personaPrompt = tutor)
        )

        assertThrows<IllegalStateException> {
            mapper.toRequestDto(messages)
        }
    }

    @Test
    fun `toRequestDto does not throw when an ASSISTANT message has null personaPrompt (null is expected)`() {
        val messages = listOf(
            ChatMessage("a1", MessageRole.ASSISTANT, "reply", 1L, personaPrompt = null)
        )

        val dto = mapper.toRequestDto(messages)

        assertEquals("assistant", dto.messages.single().role)
        assertEquals(null, dto.messages.single().rolePrompt)
    }

    // endregion

    // region Persona capture across a conversation

    @Test
    fun `toRequestDto carries each user message's personaPrompt independently`() {
        val messages = listOf(
            ChatMessage("u1", MessageRole.USER, "¿Cómo funciona la fotosíntesis?", 1L, personaPrompt = tutor),
            ChatMessage("a1", MessageRole.ASSISTANT, "La fotosíntesis es…", 2L),
            ChatMessage("u2", MessageRole.USER, "Ahora escríbelo como un poema", 3L, personaPrompt = artist)
        )

        val dto = mapper.toRequestDto(messages)

        assertEquals(listOf("user", "assistant", "user"), dto.messages.map { it.role })
        assertEquals(listOf(tutor, null, artist), dto.messages.map { it.rolePrompt })
    }

    @Test
    fun `toRequestDto matches the TECHNICAL_SPEC Fork 4 example JSON shape`() {
        val messages = listOf(
            ChatMessage("u1", MessageRole.USER, "¿Cómo funciona la fotosíntesis?", 1L, personaPrompt = tutor),
            ChatMessage("a1", MessageRole.ASSISTANT, "La fotosíntesis es…", 2L),
            ChatMessage("u2", MessageRole.USER, "Ahora escríbelo como un poema", 3L, personaPrompt = artist)
        )

        val encoded = json.encodeToString(
            ChatRequestDto.serializer(),
            mapper.toRequestDto(messages)
        )

        val expected = """{"messages":[""" +
            """{"role":"user","role_prompt":"$tutor","content":"¿Cómo funciona la fotosíntesis?"},""" +
            """{"role":"assistant","content":"La fotosíntesis es…"},""" +
            """{"role":"user","role_prompt":"$artist","content":"Ahora escríbelo como un poema"}""" +
            """]}"""

        assertEquals(expected, encoded)
    }

    @Test
    fun `toRequestDto omits role_prompt field entirely for assistant messages (not null-valued)`() {
        val messages = listOf(
            ChatMessage("a1", MessageRole.ASSISTANT, "hi", 1L)
        )

        val encoded = json.encodeToString(
            ChatRequestDto.serializer(),
            mapper.toRequestDto(messages)
        )

        assertEquals(
            """{"messages":[{"role":"assistant","content":"hi"}]}""",
            encoded
        )
    }

    // endregion

    // region Empty-content assistant filter (Phase 7.1.4, Fork 4 addendum §2)

    @Test
    fun `toRequestDto filters out assistant messages whose content is an empty string`() {
        val messages = listOf(
            ChatMessage("u1", MessageRole.USER, "ol", 1L, personaPrompt = tutor),
            ChatMessage("a-empty", MessageRole.ASSISTANT, "", 2L),
            ChatMessage("u2", MessageRole.USER, "Esto es una prueba", 3L, personaPrompt = tutor)
        )

        val dto = mapper.toRequestDto(messages)

        assertEquals(2, dto.messages.size)
        assertEquals(listOf("user", "user"), dto.messages.map { it.role })
        assertEquals(listOf("ol", "Esto es una prueba"), dto.messages.map { it.content })
    }

    @Test
    fun `toRequestDto filters out assistant messages whose content is blank whitespace`() {
        val messages = listOf(
            ChatMessage("u1", MessageRole.USER, "hi", 1L, personaPrompt = tutor),
            ChatMessage("a-blank", MessageRole.ASSISTANT, "   \n\t ", 2L)
        )

        val dto = mapper.toRequestDto(messages)

        assertEquals(1, dto.messages.size)
        assertEquals("user", dto.messages.single().role)
    }

    @Test
    fun `toRequestDto keeps assistant messages with non-empty content`() {
        val messages = listOf(
            ChatMessage("u1", MessageRole.USER, "hi", 1L, personaPrompt = tutor),
            ChatMessage("a1", MessageRole.ASSISTANT, "hello", 2L),
            ChatMessage("u2", MessageRole.USER, "again", 3L, personaPrompt = tutor)
        )

        val dto = mapper.toRequestDto(messages)

        assertEquals(3, dto.messages.size)
        assertEquals("hello", dto.messages[1].content)
    }

    @Test
    fun `toRequestDto preserves order across user and surviving assistant messages after filtering`() {
        val messages = listOf(
            ChatMessage("u1", MessageRole.USER, "first", 1L, personaPrompt = tutor),
            ChatMessage("a-empty-1", MessageRole.ASSISTANT, "", 2L),
            ChatMessage("u2", MessageRole.USER, "second", 3L, personaPrompt = artist),
            ChatMessage("a1", MessageRole.ASSISTANT, "reply", 4L),
            ChatMessage("u3", MessageRole.USER, "third", 5L, personaPrompt = tutor),
            ChatMessage("a-empty-2", MessageRole.ASSISTANT, "", 6L)
        )

        val dto = mapper.toRequestDto(messages)

        assertEquals(
            listOf("first", "second", "reply", "third"),
            dto.messages.map { it.content }
        )
    }

    @Test
    fun `toRequestDto does not filter user messages even if content would be empty`() {
        // The VM guards empty drafts (onSendTap), and the backend 422s empty user content.
        // The mapper deliberately does NOT silently drop user messages so any DB-level
        // pathology surfaces as a backend error rather than disappearing silently.
        val messages = listOf(
            ChatMessage("u1", MessageRole.USER, "", 1L, personaPrompt = tutor)
        )

        val dto = mapper.toRequestDto(messages)

        assertEquals(1, dto.messages.size)
        assertEquals("user", dto.messages.single().role)
        assertEquals("", dto.messages.single().content)
    }

    // endregion

    // region Phase 10.6.H — optional `lang` passthrough (API_SPEC v1.4.0)

    @Test
    fun `toRequestDto sets lang on every message when LocaleProvider returns a language`() {
        val mapper = mapperWith(lang = "es")
        val messages = listOf(
            ChatMessage("u1", MessageRole.USER, "hola", 1L, personaPrompt = tutor),
            ChatMessage("a1", MessageRole.ASSISTANT, "respuesta", 2L),
            ChatMessage("u2", MessageRole.USER, "otra", 3L, personaPrompt = tutor)
        )

        val dto = mapper.toRequestDto(messages)

        assertEquals(listOf("es", "es", "es"), dto.messages.map { it.lang })
    }

    @Test
    fun `toRequestDto emits null lang on every message when LocaleProvider returns null`() {
        val mapper = mapperWith(lang = null)
        val messages = listOf(
            ChatMessage("u1", MessageRole.USER, "hi", 1L, personaPrompt = tutor),
            ChatMessage("a1", MessageRole.ASSISTANT, "reply", 2L)
        )

        val dto = mapper.toRequestDto(messages)

        assertEquals(listOf<String?>(null, null), dto.messages.map { it.lang })
    }

    @Test
    fun `toRequestDto forwards exotic language codes verbatim - no whitelist clamping`() {
        // Pins the "no client-side normalization" invariant from TECHNICAL_SPEC
        // §API Contracts #2. A user on a Japanese device gets "ja" on the wire,
        // regardless of whether Luzia ships Japanese translations.
        val mapper = mapperWith(lang = "ja")
        val messages = listOf(
            ChatMessage("u1", MessageRole.USER, "こんにちは", 1L, personaPrompt = tutor)
        )

        val dto = mapper.toRequestDto(messages)

        assertEquals("ja", dto.messages.single().lang)
    }

    @Test
    fun `toRequestDto resolves lang once per request, not once per message`() {
        // Guards against a future drift where the mapper calls LocaleProvider
        // inside the per-message mapping loop. Resolution must happen once,
        // applied uniformly, so every message in a single payload shares the
        // same send-time locale (TECHNICAL_SPEC §API Contracts #2).
        var callCount = 0
        val mapper = ChatMapper(
            localeProvider = object : LocaleProvider {
                override fun currentLanguage(): String? {
                    callCount++
                    return "es"
                }
            }
        )
        val messages = listOf(
            ChatMessage("u1", MessageRole.USER, "a", 1L, personaPrompt = tutor),
            ChatMessage("a1", MessageRole.ASSISTANT, "b", 2L),
            ChatMessage("u2", MessageRole.USER, "c", 3L, personaPrompt = tutor),
            ChatMessage("a2", MessageRole.ASSISTANT, "d", 4L)
        )

        mapper.toRequestDto(messages)

        assertEquals(1, callCount)
    }

    @Test
    fun `encoded JSON carries lang on every message when LocaleProvider returns a language`() {
        val mapper = mapperWith(lang = "es")
        val messages = listOf(
            ChatMessage("u1", MessageRole.USER, "¿Cómo funciona la fotosíntesis?", 1L, personaPrompt = tutor),
            ChatMessage("a1", MessageRole.ASSISTANT, "La fotosíntesis es…", 2L),
            ChatMessage("u2", MessageRole.USER, "Ahora escríbelo como un poema", 3L, personaPrompt = artist)
        )

        val encoded = json.encodeToString(
            ChatRequestDto.serializer(),
            mapper.toRequestDto(messages)
        )

        val expected = """{"messages":[""" +
            """{"role":"user","role_prompt":"$tutor","content":"¿Cómo funciona la fotosíntesis?","lang":"es"},""" +
            """{"role":"assistant","content":"La fotosíntesis es…","lang":"es"},""" +
            """{"role":"user","role_prompt":"$artist","content":"Ahora escríbelo como un poema","lang":"es"}""" +
            """]}"""

        assertEquals(expected, encoded)
    }

    @Test
    fun `encoded JSON omits lang field entirely when LocaleProvider returns null`() {
        // `explicitNulls = false` on the NetworkModule Json (and on this test's
        // copy) drops null fields from the serialized output. The v1.4.0
        // contract treats an absent `lang` as "legacy" / accept-and-detect.
        val mapper = mapperWith(lang = null)
        val messages = listOf(
            ChatMessage("u1", MessageRole.USER, "hi", 1L, personaPrompt = tutor)
        )

        val encoded = json.encodeToString(
            ChatRequestDto.serializer(),
            mapper.toRequestDto(messages)
        )

        assertEquals(
            """{"messages":[{"role":"user","role_prompt":"$tutor","content":"hi"}]}""",
            encoded
        )
    }

    @Test
    fun `toRequestDto matches the TECHNICAL_SPEC v1_4_0 example JSON shape`() {
        // Byte-for-byte pin against the example in TECHNICAL_SPEC §API Contracts #2
        // after the Phase 10.6.H / API_SPEC v1.4.0 lang field was added. Mirrors
        // the Fork 4 example-shape test above but asserts the four-field shape.
        val mapper = mapperWith(lang = "es")
        val messages = listOf(
            ChatMessage("u1", MessageRole.USER, "¿Cómo funciona la fotosíntesis?", 1L, personaPrompt = tutor),
            ChatMessage("a1", MessageRole.ASSISTANT, "La fotosíntesis es…", 2L),
            ChatMessage("u2", MessageRole.USER, "Ahora escríbelo como un poema", 3L, personaPrompt = artist)
        )

        val encoded = json.encodeToString(
            ChatRequestDto.serializer(),
            mapper.toRequestDto(messages)
        )

        val expected = """{"messages":[""" +
            """{"role":"user","role_prompt":"$tutor","content":"¿Cómo funciona la fotosíntesis?","lang":"es"},""" +
            """{"role":"assistant","content":"La fotosíntesis es…","lang":"es"},""" +
            """{"role":"user","role_prompt":"$artist","content":"Ahora escríbelo como un poema","lang":"es"}""" +
            """]}"""

        assertEquals(expected, encoded)
    }

    // endregion
}
