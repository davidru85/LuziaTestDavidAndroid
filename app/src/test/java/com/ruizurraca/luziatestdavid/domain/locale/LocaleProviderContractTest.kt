package com.ruizurraca.luziatestdavid.domain.locale

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

/**
 * Compile-time + behavioural contract test for [LocaleProvider]. Mirrors the
 * `TextSpeakerContractTest` pattern: the anonymous-object implementation below
 * will not compile unless [LocaleProvider] declares the exact signature the
 * domain layer expects.
 *
 * Contract (from `TECHNICAL_SPEC.md §API Contracts #1 + #2`):
 *   - `currentLanguage()` returns an ISO 639-1 two-letter lowercase code when
 *     the device exposes a non-empty locale language subtag.
 *   - Returns `null` when the OS locale is unresolvable (empty list, empty
 *     language subtag) — callers must omit the `lang` field from the wire
 *     rather than guessing a default.
 *   - No whitelist / normalization; whatever the OS reports passes through
 *     verbatim so the backend can honour any language the user has configured.
 */
class LocaleProviderContractTest {

    @Test
    fun `currentLanguage returns a non-null ISO 639-1 code when the OS locale resolves`() {
        val fake = object : LocaleProvider {
            override fun currentLanguage(): String? = "es"
        }

        assertEquals("es", fake.currentLanguage())
    }

    @Test
    fun `currentLanguage returns null when the OS locale is unresolvable`() {
        val fake = object : LocaleProvider {
            override fun currentLanguage(): String? = null
        }

        assertNull(fake.currentLanguage())
    }

    @Test
    fun `contract forwards exotic language codes verbatim - no client-side whitelist`() {
        // Explicitly documents the "no whitelist" invariant: if the OS returns
        // "ja" or "fr" or anything else, the provider forwards it, and the
        // backend is free to accept or reject. The client does not clamp to
        // {en, es, pt}.
        val fake = object : LocaleProvider {
            override fun currentLanguage(): String? = "ja"
        }

        assertEquals("ja", fake.currentLanguage())
    }
}
