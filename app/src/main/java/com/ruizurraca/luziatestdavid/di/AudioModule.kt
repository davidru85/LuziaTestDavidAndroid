package com.ruizurraca.luziatestdavid.di

import com.ruizurraca.luziatestdavid.data.local.audio.AndroidTextSpeaker
import com.ruizurraca.luziatestdavid.data.local.audio.MediaRecorderAudioRecorder
import com.ruizurraca.luziatestdavid.domain.audio.AudioRecorder
import com.ruizurraca.luziatestdavid.domain.audio.TextSpeaker
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class AudioModule {

    @Binds
    @Singleton
    abstract fun bindAudioRecorder(impl: MediaRecorderAudioRecorder): AudioRecorder

    @Binds
    @Singleton
    abstract fun bindTextSpeaker(impl: AndroidTextSpeaker): TextSpeaker
}
