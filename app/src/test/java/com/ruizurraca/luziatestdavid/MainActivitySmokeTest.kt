package com.ruizurraca.luziatestdavid

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import com.ruizurraca.luziatestdavid.di.AudioModule
import com.ruizurraca.luziatestdavid.di.DatabaseModule
import com.ruizurraca.luziatestdavid.di.NetworkModule
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.HiltTestApplication
import dagger.hilt.android.testing.UninstallModules
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@HiltAndroidTest
@UninstallModules(
    NetworkModule::class,
    DatabaseModule::class,
    AudioModule::class
)
@Config(application = HiltTestApplication::class)
@RunWith(RobolectricTestRunner::class)
class MainActivitySmokeTest {

    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeRule = createEmptyComposeRule()

    @Test
    fun mainActivityRendersChatScreenCoreChrome() {
        hiltRule.inject()

        ActivityScenario.launch(MainActivity::class.java).use {
            val context = ApplicationProvider.getApplicationContext<Context>()
            val resources = context.resources

            composeRule
                .onNodeWithContentDescription(context.getString(R.string.cd_record_voice_message))
                .assertIsDisplayed()

            composeRule
                .onNodeWithText(context.getString(R.string.app_title))
                .assertIsDisplayed()

            resources.getStringArray(R.array.role_names).forEach { personaName ->
                composeRule.onNodeWithText(personaName).assertIsDisplayed()
            }

            composeRule
                .onNodeWithContentDescription(context.getString(R.string.cd_clear_conversation))
                .assertIsDisplayed()
                .assertIsNotEnabled()
        }
    }
}
