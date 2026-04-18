package com.ruizurraca.luziatestdavid.domain.common

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class AppErrorTest {

    @Test
    fun `BadRequest carries BAD_REQUEST code and human message`() {
        val error: AppError = AppError.BadRequest

        assertEquals("BAD_REQUEST", error.code)
        assertTrue(error.message.isNotBlank())
    }

    @Test
    fun `FileTooLarge carries FILE_TOO_LARGE code`() {
        val error: AppError = AppError.FileTooLarge

        assertEquals("FILE_TOO_LARGE", error.code)
        assertTrue(error.message.isNotBlank())
    }

    @Test
    fun `Timeout carries TIMEOUT code`() {
        val error: AppError = AppError.Timeout

        assertEquals("TIMEOUT", error.code)
        assertTrue(error.message.isNotBlank())
    }

    @Test
    fun `Network carries NETWORK_ERROR code`() {
        val error: AppError = AppError.Network

        assertEquals("NETWORK_ERROR", error.code)
        assertTrue(error.message.isNotBlank())
    }

    @Test
    fun `ServiceUnavailable carries SERVICE_UNAVAILABLE code`() {
        val error: AppError = AppError.ServiceUnavailable

        assertEquals("SERVICE_UNAVAILABLE", error.code)
        assertTrue(error.message.isNotBlank())
    }

    @Test
    fun `Internal carries INTERNAL_ERROR code`() {
        val error: AppError = AppError.Internal

        assertEquals("INTERNAL_ERROR", error.code)
        assertTrue(error.message.isNotBlank())
    }

    @Test
    fun `Unknown preserves raw code and raw message`() {
        val error: AppError = AppError.Unknown(rawCode = "WEIRD_THING", rawMessage = "Something odd")

        assertEquals("WEIRD_THING", error.code)
        assertEquals("Something odd", error.message)
    }

    @Test
    fun `fromCode maps BAD_REQUEST to BadRequest singleton`() {
        assertSame(AppError.BadRequest, AppError.fromCode("BAD_REQUEST"))
    }

    @Test
    fun `fromCode maps FILE_TOO_LARGE to FileTooLarge singleton`() {
        assertSame(AppError.FileTooLarge, AppError.fromCode("FILE_TOO_LARGE"))
    }

    @Test
    fun `fromCode maps TIMEOUT to Timeout singleton`() {
        assertSame(AppError.Timeout, AppError.fromCode("TIMEOUT"))
    }

    @Test
    fun `fromCode maps NETWORK_ERROR to Network singleton`() {
        assertSame(AppError.Network, AppError.fromCode("NETWORK_ERROR"))
    }

    @Test
    fun `fromCode maps SERVICE_UNAVAILABLE to ServiceUnavailable singleton`() {
        assertSame(AppError.ServiceUnavailable, AppError.fromCode("SERVICE_UNAVAILABLE"))
    }

    @Test
    fun `fromCode maps INTERNAL_ERROR to Internal singleton`() {
        assertSame(AppError.Internal, AppError.fromCode("INTERNAL_ERROR"))
    }

    @Test
    fun `fromCode falls back to Unknown preserving original code and message`() {
        val error = AppError.fromCode(code = "CUSTOM_XYZ", message = "details here")

        assertTrue(error is AppError.Unknown)
        assertEquals("CUSTOM_XYZ", error.code)
        assertEquals("details here", error.message)
    }

    @Test
    fun `fromCode with unknown code and null message still produces Unknown with non-blank message`() {
        val error = AppError.fromCode(code = "MYSTERY", message = null)

        assertTrue(error is AppError.Unknown)
        assertEquals("MYSTERY", error.code)
        assertTrue(error.message.isNotBlank())
    }

    @Test
    fun `sealed hierarchy is exhaustive in when expression`() {
        val error: AppError = AppError.Network

        val label: String = when (error) {
            AppError.BadRequest -> "tier1"
            AppError.FileTooLarge -> "tier1"
            AppError.Timeout -> "tier2"
            AppError.Network -> "tier2"
            AppError.ServiceUnavailable -> "tier3"
            AppError.Internal -> "tier3"
            is AppError.Unknown -> "tier3"
        }

        assertEquals("tier2", label)
    }
}
