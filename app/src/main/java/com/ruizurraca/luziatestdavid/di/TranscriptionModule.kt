package com.ruizurraca.luziatestdavid.di

import com.ruizurraca.luziatestdavid.data.transcription.OnDeviceTranscriptionDataSource
import com.ruizurraca.luziatestdavid.data.transcription.OnDeviceTranscriptionDataSourceImpl
import com.ruizurraca.luziatestdavid.data.transcription.RemoteTranscriptionDataSource
import com.ruizurraca.luziatestdavid.data.transcription.RemoteTranscriptionDataSourceImpl
import com.ruizurraca.luziatestdavid.data.transcription.SpeechRecognitionClientFactory
import com.ruizurraca.luziatestdavid.data.transcription.SpeechRecognitionClientFactoryImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class TranscriptionModule {

    @Binds
    @Singleton
    abstract fun bindRemoteTranscription(
        impl: RemoteTranscriptionDataSourceImpl
    ): RemoteTranscriptionDataSource

    @Binds
    @Singleton
    abstract fun bindOnDeviceTranscription(
        impl: OnDeviceTranscriptionDataSourceImpl
    ): OnDeviceTranscriptionDataSource

    @Binds
    @Singleton
    abstract fun bindSpeechRecognitionClientFactory(
        impl: SpeechRecognitionClientFactoryImpl
    ): SpeechRecognitionClientFactory
}
