package com.ruizurraca.luziatestdavid.data.local.locale

import android.content.Context
import com.ruizurraca.luziatestdavid.domain.locale.LocaleProvider
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Android-backed [LocaleProvider]. Reads the top-preference device locale
 * from `context.resources.configuration.locales[0].language` and returns
 * its ISO 639-1 two-letter lowercase language subtag. Returns `null` when
 * the locale list is empty or the first locale's language subtag is empty
 * (both are corrupt-configuration edge cases, guarded defensively).
 */
@Singleton
class AndroidLocaleProvider @Inject constructor(
    @param:ApplicationContext private val context: Context
) : LocaleProvider {

    override fun currentLanguage(): String? {
        val locales = context.resources.configuration.locales
        if (locales.isEmpty) return null
        return locales[0].language.takeIf { it.isNotEmpty() }
    }
}
