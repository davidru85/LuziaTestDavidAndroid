package com.ruizurraca.luziatestdavid.architecture

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.File

/**
 * Enforces the Import Guard from TECHNICAL_SPEC.md: the `presentation/` package
 * must never depend on `...data.*` — it consumes the domain layer only.
 */
class PresentationPurityTest {

    private val forbiddenPrefixes = listOf(
        "com.ruizurraca.luziatestdavid.data."
    )

    private val presentationDir: File = listOf(
        File("src/main/java/com/ruizurraca/luziatestdavid/presentation"),
        File("app/src/main/java/com/ruizurraca/luziatestdavid/presentation")
    ).first { it.exists() }

    @Test
    fun `presentation sources exist`() {
        assertTrue(presentationDir.exists() && presentationDir.isDirectory) {
            "Presentation directory not found at ${presentationDir.absolutePath}"
        }
        val kotlinFiles = presentationDir.walkTopDown().filter { it.extension == "kt" }.toList()
        assertTrue(kotlinFiles.isNotEmpty()) { "No Kotlin sources found under presentation/" }
    }

    @Test
    fun `presentation sources import no data layer packages`() {
        val importRegex = Regex("""^\s*import\s+([\w.]+)""")
        val violations = presentationDir.walkTopDown()
            .filter { it.isFile && it.extension == "kt" }
            .flatMap { file ->
                file.readLines()
                    .mapNotNull { line -> importRegex.find(line)?.groupValues?.get(1) }
                    .filter { imp -> forbiddenPrefixes.any { imp.startsWith(it) } }
                    .map { imp -> "${file.relativeTo(presentationDir)} imports $imp" }
            }
            .toList()

        assertEquals(emptyList<String>(), violations) {
            "Presentation purity violated:\n" + violations.joinToString("\n")
        }
    }
}
