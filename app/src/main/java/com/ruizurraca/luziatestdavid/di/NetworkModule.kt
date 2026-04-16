package com.ruizurraca.luziatestdavid.di

import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {
    // Ktor HttpClient + BASE_URL wiring arrives in Phase 4 (data layer).
}
