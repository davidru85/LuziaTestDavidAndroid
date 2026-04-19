package com.ruizurraca.luziatestdavid.di

import android.content.Context
import androidx.room.Room
import com.ruizurraca.luziatestdavid.data.local.LuziaDatabase
import com.ruizurraca.luziatestdavid.data.local.dao.ChatMessageDao
import com.ruizurraca.luziatestdavid.domain.audio.AudioRecorder
import com.ruizurraca.luziatestdavid.domain.audio.TextSpeaker
import com.ruizurraca.luziatestdavid.domain.common.Resource
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.url
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import java.io.File
import java.util.Locale
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object TestNetworkModule {

    @Provides
    @Singleton
    fun provideJson(): Json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        explicitNulls = false
    }

    @Provides
    @Singleton
    fun provideHttpClient(json: Json): HttpClient = HttpClient(
        MockEngine { _ ->
            respond(
                content = "{}",
                status = HttpStatusCode.OK,
                headers = headersOf(
                    HttpHeaders.ContentType,
                    ContentType.Application.Json.toString()
                )
            )
        }
    ) {
        expectSuccess = true
        install(ContentNegotiation) {
            json(json)
        }
        defaultRequest {
            url("http://test/")
        }
    }
}

@Module
@InstallIn(SingletonComponent::class)
object TestDatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): LuziaDatabase =
        Room.inMemoryDatabaseBuilder(context, LuziaDatabase::class.java)
            .allowMainThreadQueries()
            .build()

    @Provides
    @Singleton
    fun provideChatMessageDao(database: LuziaDatabase): ChatMessageDao =
        database.chatMessageDao()
}

@Module
@InstallIn(SingletonComponent::class)
object TestAudioModule {

    @Provides
    @Singleton
    fun provideAudioRecorder(): AudioRecorder = FakeAudioRecorder()

    @Provides
    @Singleton
    fun provideTextSpeaker(): TextSpeaker = FakeTextSpeaker()
}

private class FakeAudioRecorder : AudioRecorder {
    override suspend fun start(): Resource<Unit> = Resource.Success(Unit)

    override suspend fun stop(): Resource<File> =
        Resource.Error("FakeAudioRecorder.stop() called in a test that did not override TestAudioModule")

    override fun release() = Unit
}

private class FakeTextSpeaker : TextSpeaker {
    override suspend fun speak(text: String, locale: Locale): Resource<Unit> =
        Resource.Success(Unit)

    override fun stop() = Unit
    override fun release() = Unit
}
