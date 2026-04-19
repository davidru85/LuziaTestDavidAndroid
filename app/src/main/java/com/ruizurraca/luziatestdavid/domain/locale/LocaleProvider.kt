package com.ruizurraca.luziatestdavid.domain.locale

/**
 * Resolves the current device locale language for the `lang` wire field on
 * `POST /chat` and `POST /transcribe` (see `TECHNICAL_SPEC.md §API Contracts
 * #1 + #2`). Pure-Kotlin contract; the Android-backed implementation lives
 * in `data/local/locale/AndroidLocaleProvider`.
 *
 * The resolved value is forwarded to the backend verbatim — the client does
 * not whitelist or normalize beyond the "non-empty" guard. The backend is
 * free to honour any language the user has configured regardless of whether
 * the app itself ships translated resources for it.
 */
interface LocaleProvider {

    /**
     * Returns the ISO 639-1 two-letter lowercase language subtag of the
     * device's top-preference locale (e.g. `"en"`, `"es"`, `"pt"`, `"fr"`,
     * `"ja"`), or `null` when the OS locale is unresolvable — i.e. the
     * locale list is empty or the first entry's language subtag is empty.
     *
     * Callers on the wire side must **omit** the `lang` field when this
     * returns `null` rather than substituting a default; the backend has its
     * own fallback heuristic for legacy (`lang`-absent) payloads.
     */
    fun currentLanguage(): String?
}
