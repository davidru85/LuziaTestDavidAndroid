package com.ruizurraca.luziatestdavid.architecture

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.File

/**
 * Enforces the Import Guard from TECHNICAL_SPEC.md: the `domain/` package must be
 * pure Kotlin/JVM and must not leak into `android.*`, `androidx.*`, or `...data.*`.
 */
class DomainPurityTest {

    private val forbiddenPrefixes = listOf(
        "android.",
        "androidx.",
        "com.ruizurraca.luziatestdavid.data."
    )

    private val domainDir: File = listOf(
        File("src/main/java/com/ruizurraca/luziatestdavid/domain"),
        File("app/src/main/java/com/ruizurraca/luziatestdavid/domain")
    ).first { it.exists() }

    @Test
    fun `domain sources exist`() {
        assertTrue(domainDir.exists() && domainDir.isDirectory) {
            "Domain directory not found at ${domainDir.absolutePath}"
        }
        val kotlinFiles = domainDir.walkTopDown().filter { it.extension == "kt" }.toList()
        assertTrue(kotlinFiles.isNotEmpty()) { "No Kotlin sources found under domain/" }
    }

    @Test
    fun `domain sources import no forbidden packages`() {
        val importRegex = Regex("""^\s*import\s+([\w.]+)""")
        val violations = domainDir.walkTopDown()
            .filter { it.isFile && it.extension == "kt" }
            .flatMap { file ->
                file.readLines()
                    .mapNotNull { line -> importRegex.find(line)?.groupValues?.get(1) }
                    .filter { imp -> forbiddenPrefixes.any { imp.startsWith(it) } }
                    .map { imp -> "${file.relativeTo(domainDir)} imports $imp" }
            }
            .toList()

        assertEquals(emptyList<String>(), violations) {
            "Domain purity violated:\n" + violations.joinToString("\n")
        }
    }
}
