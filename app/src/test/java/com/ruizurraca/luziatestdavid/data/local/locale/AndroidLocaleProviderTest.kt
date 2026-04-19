package com.ruizurraca.luziatestdavid.data.local.locale

import android.content.Context
import android.content.res.Configuration
import android.content.res.Resources
import android.os.LocaleList
import androidx.test.core.app.ApplicationProvider
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.Locale

/**
 * Robolectric-based tests for [AndroidLocaleProvider].
 *
 * Happy paths use `@Config(qualifiers = ...)` to force Robolectric to install
 * the requested Configuration on the application Context. The four language
 * qualifiers cover the two app-translated locales (`en`, `es`, `pt`), one
 * non-app-supported locale to pin the "no whitelist" invariant (`ja`), and
 * `fr` as a second non-app-supported sanity case. Edge cases (empty locale
 * list, empty language subtag) are exercised via MockK'd Context / Resources
 * / Configuration / LocaleList stacks — Robolectric qualifiers can't express
 * "no locale at all", so the edge path needs a manually-shaped stub.
 */
@RunWith(RobolectricTestRunner::class)
class AndroidLocaleProviderTest {

    // region Happy-path locale resolution via Robolectric qualifiers

    @Test
    @Config(qualifiers = "en")
    fun `currentLanguage returns en when device locale is English`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val provider = AndroidLocaleProvider(context)

        assertEquals("en", provider.currentLanguage())
    }

    @Test
    @Config(qualifiers = "es")
    fun `currentLanguage returns es when device locale is Spanish`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val provider = AndroidLocaleProvider(context)

        assertEquals("es", provider.currentLanguage())
    }

    @Test
    @Config(qualifiers = "pt")
    fun `currentLanguage returns pt when device locale is Portuguese`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val provider = AndroidLocaleProvider(context)

        assertEquals("pt", provider.currentLanguage())
    }

    @Test
    @Config(qualifiers = "ja")
    fun `currentLanguage returns ja for a non-app-supported locale (no whitelist)`() {
        // The app ships only en / es-ES / pt-PT translations, but the provider
        // forwards whatever the OS reports. Backend decides how to handle
        // exotic codes.
        val context = ApplicationProvider.getApplicationContext<Context>()
        val provider = AndroidLocaleProvider(context)

        assertEquals("ja", provider.currentLanguage())
    }

    @Test
    @Config(qualifiers = "fr")
    fun `currentLanguage returns fr for a non-app-supported locale (no whitelist)`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val provider = AndroidLocaleProvider(context)

        assertEquals("fr", provider.currentLanguage())
    }

    // endregion

    // region Edge-case null / empty guards

    @Test
    fun `currentLanguage returns null when the device locale language subtag is empty`() {
        // Locale.ROOT has empty language / country / variant — the canonical
        // "no specific locale" value. Stands in for a corrupt-configuration
        // edge case where the OS hands us a present-but-empty locale.
        val context = mockContextWithLocaleList(emptyLocaleList = false, firstLocale = Locale.ROOT)
        val provider = AndroidLocaleProvider(context)

        assertNull(provider.currentLanguage())
    }

    @Test
    fun `currentLanguage returns null when the device locale list is empty`() {
        // Extremely rare (typically only on stripped-down test Contexts), but
        // guarding the index access prevents an AIOOBE at runtime.
        val context = mockContextWithLocaleList(emptyLocaleList = true, firstLocale = null)
        val provider = AndroidLocaleProvider(context)

        assertNull(provider.currentLanguage())
    }

    private fun mockContextWithLocaleList(
        emptyLocaleList: Boolean,
        firstLocale: Locale?
    ): Context {
        val context = mockk<Context>()
        val resources = mockk<Resources>()
        val configuration = Configuration()
        val localeList = if (emptyLocaleList) LocaleList.getEmptyLocaleList()
            else LocaleList(firstLocale!!)
        configuration.setLocales(localeList)

        every { context.resources } returns resources
        every { resources.configuration } returns configuration

        return context
    }

    // endregion
}
