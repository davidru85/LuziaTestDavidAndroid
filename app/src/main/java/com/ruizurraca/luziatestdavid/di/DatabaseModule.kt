package com.ruizurraca.luziatestdavid.di

import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    // Room database + DAOs arrive in Phase 4 (data layer).
}
