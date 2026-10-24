package com.example.muzzio.di

import android.content.ContentResolver
import android.content.Context
import com.example.muzzio.data.repository.TrackRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideContentResolver(
        @ApplicationContext context: Context
    ): ContentResolver = context.contentResolver

    @Provides
    @Singleton
    fun provideTrackRepository(
        contentResolver: ContentResolver
    ): TrackRepository = TrackRepository(contentResolver)
}
