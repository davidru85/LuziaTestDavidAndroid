package com.ruizurraca.luziatestdavid.domain.common

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ResourceTest {

    @Test
    fun `Success wraps payload`() {
        val resource: Resource<String> = Resource.Success("hello")

        assertTrue(resource is Resource.Success)
        assertEquals("hello", (resource as Resource.Success).data)
    }

    @Test
    fun `Error carries message and optional throwable`() {
        val cause = IllegalStateException("boom")
        val resource: Resource<String> = Resource.Error("Upload failed", cause)

        assertTrue(resource is Resource.Error)
        val error = resource as Resource.Error
        assertEquals("Upload failed", error.message)
        assertSame(cause, error.throwable)
    }

    @Test
    fun `Error throwable defaults to null`() {
        val resource: Resource<Int> = Resource.Error("bad input")

        assertTrue(resource is Resource.Error)
        assertNull((resource as Resource.Error).throwable)
    }

    @Test
    fun `Error error field defaults to null for backward compatibility`() {
        val resource: Resource<Int> = Resource.Error("legacy error")

        assertTrue(resource is Resource.Error)
        assertNull((resource as Resource.Error).error)
    }

    @Test
    fun `Error carries optional AppError alongside message and throwable`() {
        val cause = IllegalStateException("socket closed")
        val networkError = AppError.Network()
        val resource: Resource<String> = Resource.Error(
            message = "Network connection failed.",
            throwable = cause,
            error = networkError
        )

        assertTrue(resource is Resource.Error)
        val err = resource as Resource.Error
        assertEquals("Network connection failed.", err.message)
        assertSame(cause, err.throwable)
        assertEquals(networkError, err.error)
    }

    @Test
    fun `Resource is exhaustive when matched`() {
        val result: Resource<Int> = Resource.Success(42)

        val label: String = when (result) {
            is Resource.Success -> "ok-${result.data}"
            is Resource.Error -> "err-${result.message}"
        }

        assertEquals("ok-42", label)
    }
}
