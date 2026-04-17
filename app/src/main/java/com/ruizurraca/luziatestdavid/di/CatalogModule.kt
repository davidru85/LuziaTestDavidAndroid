package com.ruizurraca.luziatestdavid.di

import android.content.Context
import com.ruizurraca.luziatestdavid.R
import com.ruizurraca.luziatestdavid.data.catalog.DefaultPersonaCatalog
import com.ruizurraca.luziatestdavid.domain.catalog.PersonaCatalog
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object CatalogModule {

    @Provides
    @Singleton
    fun providePersonaCatalog(@ApplicationContext context: Context): PersonaCatalog =
        DefaultPersonaCatalog(
            displayNames = context.resources.getStringArray(R.array.role_names).toList(),
            prompts = context.resources.getStringArray(R.array.role_prompts).toList()
        )
}
