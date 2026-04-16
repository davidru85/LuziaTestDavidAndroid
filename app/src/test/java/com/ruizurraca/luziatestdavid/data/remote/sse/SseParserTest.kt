package com.ruizurraca.luziatestdavid.data.remote.sse

import app.cash.turbine.test
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * RED-phase contract tests for [SseParser].
 *
 * The parser consumes a [kotlinx.coroutines.flow.Flow] of already-split SSE lines
 * (as would be produced by `ByteReadChannel.readUTF8Line()` per TECHNICAL_SPEC §Networking)
 * and emits a typed [SseEvent] stream.
 *
 * Protocol reference (TECHNICAL_SPEC §API.2):
 *   - Token chunk:  `data: <token>\n\n`
 *   - End sentinel: `data: [DONE]\n\n`
 *   - Error event:  `event: error\ndata: {"code":"...","message":"..."}\n\n`
 */
class SseParserTest {

    private val parser = SseParser()

    @Test
    fun `emits Token for a single data line`() = runTest {
        val lines = flowOf("data: hello", "")

        parser.parse(lines).test {
            assertEquals(SseEvent.Token("hello"), awaitItem())
            awaitComplete()
        }
    }

    @Test
    fun `emits multiple Tokens in arrival order`() = runTest {
        val lines = flowOf(
            "data: Hello",
            "",
            "data: , ",
            "",
            "data: world",
            ""
        )

        parser.parse(lines).test {
            assertEquals(SseEvent.Token("Hello"), awaitItem())
            assertEquals(SseEvent.Token(", "), awaitItem())
            assertEquals(SseEvent.Token("world"), awaitItem())
            awaitComplete()
        }
    }

    @Test
    fun `completes on DONE sentinel without emitting a Token`() = runTest {
        val lines = flowOf(
            "data: Hi",
            "",
            "data: [DONE]",
            ""
        )

        parser.parse(lines).test {
            assertEquals(SseEvent.Token("Hi"), awaitItem())
            awaitComplete()
        }
    }

    @Test
    fun `ignores data after DONE sentinel`() = runTest {
        val lines = flowOf(
            "data: [DONE]",
            "",
            "data: ghost",
            ""
        )

        parser.parse(lines).test {
            awaitComplete()
        }
    }

    @Test
    fun `emits Error for error event and then completes`() = runTest {
        val lines = flowOf(
            "event: error",
            "data: {\"code\":\"INTERNAL_ERROR\",\"message\":\"boom\"}",
            ""
        )

        parser.parse(lines).test {
            val event = awaitItem()
            assertTrue(event is SseEvent.Error) { "expected Error, got $event" }
            val error = event as SseEvent.Error
            assertEquals("INTERNAL_ERROR", error.code)
            assertEquals("boom", error.message)
            awaitComplete()
        }
    }

    @Test
    fun `ignores blank separator lines between events`() = runTest {
        val lines = flowOf(
            "",
            "data: a",
            "",
            "",
            "data: b",
            ""
        )

        parser.parse(lines).test {
            assertEquals(SseEvent.Token("a"), awaitItem())
            assertEquals(SseEvent.Token("b"), awaitItem())
            awaitComplete()
        }
    }

    @Test
    fun `preserves internal colons and whitespace in token content`() = runTest {
        val lines = flowOf(
            "data: key:value  trailing ",
            ""
        )

        parser.parse(lines).test {
            assertEquals(SseEvent.Token("key:value  trailing "), awaitItem())
            awaitComplete()
        }
    }

    @Test
    fun `completes silently when upstream completes without DONE`() = runTest {
        val lines = flowOf("data: partial", "")

        parser.parse(lines).test {
            assertEquals(SseEvent.Token("partial"), awaitItem())
            awaitComplete()
        }
    }
}
