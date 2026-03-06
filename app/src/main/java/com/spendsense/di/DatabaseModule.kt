package com.spendsense.di

import android.content.Context
import com.spendsense.auth.AuthManager
import com.spendsense.data.local.SpendSenseDatabase
import com.spendsense.data.local.dao.AccountDao
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
    fun provideDatabase(@ApplicationContext context: Context): SpendSenseDatabase {
        return SpendSenseDatabase.getDatabase(context)
    }

    @Provides
    fun provideAccountDao(database: SpendSenseDatabase): AccountDao {
        return database.accountDao()
    }

    @Provides
    @Singleton
    fun provideAuthManager(@ApplicationContext context: Context): AuthManager {
        return AuthManager(context)
    }
}
