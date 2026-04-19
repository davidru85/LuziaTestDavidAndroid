package com.ruizurraca.luziatestdavid.baselineprofile

import androidx.benchmark.macro.junit4.BaselineProfileRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Until
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Generates a baseline profile for the staging flavour of the Luzia app.
 *
 * Scope (honest): this profile covers cold start → Hilt graph → ChatScreen
 * Compose tree → role-selector interaction. It does NOT cover the mic →
 * transcribe → SSE → message-render path because no backend is reachable
 * during the generation run; re-run once a backend is available to extend
 * the profile.
 */
@RunWith(AndroidJUnit4::class)
class BaselineProfileGenerator {

    @get:Rule
    val rule = BaselineProfileRule()

    @Test
    fun generate() {
        rule.collect(packageName = PACKAGE_NAME) {
            pressHome()
            startActivityAndWait()

            // Wait for the chat screen's mic button to appear — same
            // selector used by MainActivitySmokeTest, proving the screen
            // fully rendered before we exercise interactions.
            device.wait(Until.hasObject(By.desc("Record voice message")), UI_TIMEOUT_MS)

            // Cycle the persona chips to exercise ChatViewModel state flow
            // + Compose recomposition + RoleSelectorChips semantics.
            listOf("Scientist", "Artist", "Student").forEach { personaLabel ->
                device.findObject(By.text(personaLabel))?.click()
                device.waitForIdle()
            }
        }
    }

    companion object {
        // applicationId for stagingBenchmark variant — matches the
        // staging flavour's applicationIdSuffix = ".staging".
        private const val PACKAGE_NAME = "com.ruizurraca.luziatestdavid.staging"
        private const val UI_TIMEOUT_MS = 10_000L
    }
}
