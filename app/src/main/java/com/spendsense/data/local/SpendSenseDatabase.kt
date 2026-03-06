package com.spendsense.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.spendsense.data.local.dao.AccountDao
import com.spendsense.data.local.entity.Account
import com.spendsense.data.local.entity.SmsRegexFilter
import com.spendsense.data.local.entity.Transaction

@Database(
    entities = [
        Account::class,
        Transaction::class,
        SmsRegexFilter::class
    ],
    version = 1,
    exportSchema = false
)
abstract class SpendSenseDatabase : RoomDatabase() {

    abstract fun accountDao(): AccountDao
    // abstract fun transactionDao(): TransactionDao
    // abstract fun filterDao(): SmsRegexFilterDao

    companion object {
        @Volatile
        private var INSTANCE: SpendSenseDatabase? = null

        fun getDatabase(context: Context): SpendSenseDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    SpendSenseDatabase::class.java,
                    "spendsense_database"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
