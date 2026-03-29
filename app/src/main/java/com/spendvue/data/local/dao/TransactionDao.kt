package com.spendvue.data.local.dao

import androidx.room.*
import com.spendvue.data.local.entity.Transaction
import kotlinx.coroutines.flow.Flow
import java.util.*

@Dao
interface TransactionDao {

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(transaction: Transaction): Long

    @Update
    suspend fun update(transaction: Transaction)

    @Query("SELECT * FROM transactions WHERE id = :id")
    suspend fun getById(id: Int): Transaction?

    @Query("SELECT * FROM transactions WHERE account_id = :accountId ORDER BY timestamp DESC")
    fun getByAccount(accountId: Int): Flow<List<Transaction>>

    @Query("SELECT * FROM transactions WHERE user_id = :userId ORDER BY timestamp DESC")
    fun getAllForUser(userId: String): Flow<List<Transaction>>

    @Query("SELECT * FROM transactions WHERE account_id = :accountId AND timestamp BETWEEN :startTime AND :endTime")
    suspend fun getTransactionsInTimeRange(accountId: Int, startTime: Long, endTime: Long): List<Transaction>

    @Query("SELECT * FROM transactions WHERE account_id = :accountId AND amount = :amount AND timestamp BETWEEN :startTime AND :endTime")
    suspend fun getPotentialDuplicates(accountId: Int, amount: Double, startTime: Long, endTime: Long): List<Transaction>

    @Query("SELECT * FROM transactions WHERE is_flagged = 1 ORDER BY timestamp DESC")
    fun getFlaggedTransactions(): Flow<List<Transaction>>

    @Query("SELECT * FROM transactions WHERE status = 'PARTIAL' ORDER BY timestamp DESC")
    fun getPartialTransactions(): Flow<List<Transaction>>

    @Query("UPDATE transactions SET is_flagged = :isFlagged, flag_reason = :reason WHERE id = :id")
    suspend fun updateFlagStatus(id: Int, isFlagged: Boolean, reason: String?)

    @Query("UPDATE transactions SET merchant = :merchant, category = :category, is_flagged = 0, status = 'CONFIRMED' WHERE id = :id")
    suspend fun confirmPartialTransaction(id: Int, merchant: String, category: String)
}