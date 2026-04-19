package com.ruizurraca.luziatestdavid.di

import com.ruizurraca.luziatestdavid.data.local.locale.AndroidLocaleProvider
import com.ruizurraca.luziatestdavid.domain.locale.LocaleProvider
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class LocaleModule {

    @Binds
    @Singleton
    abstract fun bindLocaleProvider(impl: AndroidLocaleProvider): LocaleProvider
}
