package com.spendvue.data.repository

import com.spendvue.auth.AuthManager
import com.spendvue.data.local.dao.AccountDao
import com.spendvue.data.local.dao.TransactionDao
import com.spendvue.data.local.entity.Transaction
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TransactionRepository @Inject constructor(
    private val transactionDao: TransactionDao,
    private val accountDao: AccountDao,
    private val authManager: AuthManager
) {
    // userId comes from stored JWT — no hardcoded values
    private val currentUserId: String
        get() = authManager.getUserId()

    fun getAllTransactions(): Flow<List<Transaction>> =
        transactionDao.getAllForUser(currentUserId)

    fun getTransactionsByAccount(accountId: Int): Flow<List<Transaction>> =
        transactionDao.getByAccount(accountId)

    fun getFlaggedTransactions(): Flow<List<Transaction>> =
        transactionDao.getFlaggedTransactions()

    fun getPartialTransactions(): Flow<List<Transaction>> =
        transactionDao.getPartialTransactions()

    suspend fun insertTransaction(transaction: Transaction): Result<Long> {
        return try {
            // Ensure userId matches current user
            if (transaction.userId != currentUserId) {
                return Result.failure(Exception("Transaction user ID mismatch"))
            }
            val id = transactionDao.insert(transaction)
            Result.success(id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun createSmsTransaction(
        accountId: Int,
        amount: Double,
        merchant: String?,
        category: String?,
        transactionType: String,
        timestamp: Long,
        rawSms: String?
    ): Result<Long> {
        return try {
            // Get account to ensure it belongs to user and get userId
            val account = accountDao.getById(accountId, currentUserId)
                ?: return Result.failure(Exception("Account not found"))
            
            val transaction = Transaction(
                accountId = accountId,
                userId = currentUserId,
                amount = amount,
                merchant = merchant,
                category = category,
                transactionType = transactionType,
                timestamp = timestamp,
                source = "SMS_AUTO",
                isFlagged = merchant == null || category == null,
                flagReason = if (merchant == null || category == null) "MISSING_MERCHANT_OR_CATEGORY" else null,
                status = if (merchant == null || category == null) "PARTIAL" else "CONFIRMED",
                rawSms = rawSms
            )
            val id = transactionDao.insert(transaction)
            Result.success(id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun confirmPartialTransaction(
        transactionId: Int,
        merchant: String,
        category: String
    ): Result<Unit> {
        return try {
            transactionDao.confirmPartialTransaction(transactionId, merchant, category)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateFlagStatus(
        transactionId: Int,
        isFlagged: Boolean,
        reason: String?
    ): Result<Unit> {
        return try {
            transactionDao.updateFlagStatus(transactionId, isFlagged, reason)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun checkDuplicate(
        accountId: Int,
        amount: Double,
        timestamp: Long
    ): Boolean {
        // Check for duplicate transactions within ±5 minutes
        val startTime = timestamp - 5 * 60 * 1000
        val endTime = timestamp + 5 * 60 * 1000
        val duplicates = transactionDao.getPotentialDuplicates(accountId, amount, startTime, endTime)
        return duplicates.isNotEmpty()
    }
}