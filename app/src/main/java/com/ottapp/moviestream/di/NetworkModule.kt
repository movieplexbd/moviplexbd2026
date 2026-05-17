package com.ottapp.moviestream.di

import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.ottapp.moviestream.data.repository.ReelRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    private const val DB_URL = "https://movies-bee24-default-rtdb.firebaseio.com"

    @Provides
    @Singleton
    fun provideFirebaseDatabase(): FirebaseDatabase {
        return FirebaseDatabase.getInstance(DB_URL)
    }

    @Provides
    @Singleton
    fun provideDatabaseReference(database: FirebaseDatabase): DatabaseReference {
        return database.reference
    }

    @Provides
    @Singleton
    fun provideReelRepository(databaseReference: DatabaseReference): ReelRepository {
        return ReelRepository(databaseReference)
    }
}
