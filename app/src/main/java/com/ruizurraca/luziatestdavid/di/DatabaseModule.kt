package com.ruizurraca.luziatestdavid.di

import android.content.Context
import androidx.room.Room
import com.ruizurraca.luziatestdavid.data.local.LuziaDatabase
import com.ruizurraca.luziatestdavid.data.local.dao.ChatMessageDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): LuziaDatabase =
        Room.databaseBuilder(
            context,
            LuziaDatabase::class.java,
            "luzia.db"
        ).build()

    @Provides
    @Singleton
    fun provideChatMessageDao(database: LuziaDatabase): ChatMessageDao =
        database.chatMessageDao()
}
