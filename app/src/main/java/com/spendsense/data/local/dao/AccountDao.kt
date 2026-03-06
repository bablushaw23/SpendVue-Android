package com.spendsense.data.local.dao

import androidx.room.*
import com.spendsense.data.local.entity.Account
import kotlinx.coroutines.flow.Flow

@Dao
interface AccountDao {

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(account: Account): Long

    @Query("SELECT * FROM accounts WHERE user_id = :userId AND is_active = 1 ORDER BY account_type, created_at")
    fun getActiveAccounts(userId: String): Flow<List<Account>>

    @Query("SELECT * FROM accounts WHERE user_id = :userId AND is_active = 0 ORDER BY archived_at DESC")
    fun getArchivedAccounts(userId: String): Flow<List<Account>>

    @Query("SELECT * FROM accounts WHERE user_id = :userId ORDER BY is_active DESC, account_type, created_at")
    fun getAllAccounts(userId: String): Flow<List<Account>>

    @Query("SELECT * FROM accounts WHERE id = :accountId AND user_id = :userId")
    suspend fun getById(accountId: Int, userId: String): Account?

    @Query("SELECT * FROM accounts WHERE user_id = :userId AND last_4_digits = :last4 AND is_active = 1")
    suspend fun getByLast4Digits(userId: String, last4: String): Account?

    @Query("SELECT COUNT(*) FROM accounts WHERE user_id = :userId AND is_active = 1")
    suspend fun countActiveAccounts(userId: String): Int

    @Update
    suspend fun update(account: Account)

    @Query("UPDATE accounts SET current_balance = :newBalance, last_synced_at = :timestamp, updated_at = :timestamp WHERE id = :accountId")
    suspend fun updateBalance(accountId: Int, newBalance: Double, timestamp: Long)

    @Query("UPDATE accounts SET is_active = 0, archived_at = :timestamp, updated_at = :timestamp WHERE id = :accountId")
    suspend fun archive(accountId: Int, timestamp: Long)

    @Query("UPDATE accounts SET is_active = 1, archived_at = NULL, updated_at = :timestamp WHERE id = :accountId")
    suspend fun unarchive(accountId: Int, timestamp: Long)
}
