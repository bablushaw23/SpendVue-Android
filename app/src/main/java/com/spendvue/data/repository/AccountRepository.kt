package com.spendvue.data.repository

import com.spendvue.auth.AuthManager
import com.spendvue.data.local.dao.AccountDao
import com.spendvue.data.local.entity.Account
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AccountRepository @Inject constructor(
    private val accountDao: AccountDao,
    private val authManager: AuthManager
) {
    // userId comes from stored JWT — no hardcoded values
    private val currentUserId: String
        get() = authManager.getUserId()

    fun getActiveAccounts(): Flow<List<Account>> =
        accountDao.getActiveAccounts(currentUserId)

    fun getArchivedAccounts(): Flow<List<Account>> =
        accountDao.getArchivedAccounts(currentUserId)

    fun getAllAccounts(): Flow<List<Account>> =
        accountDao.getAllAccounts(currentUserId)

    suspend fun getAccountById(id: Int): Account? =
        accountDao.getById(id, currentUserId)

    suspend fun getAccountByLast4Digits(last4Digits: String): Account? =
        accountDao.getByLast4Digits(currentUserId, last4Digits)

    suspend fun createAccount(
        accountName: String,
        bankName: String,
        accountType: String,
        last4Digits: String,
        currentBalance: Double
    ): Result<Long> {
        return try {
            if (accountName.isBlank())
                return Result.failure(Exception("Account name is required"))
            if (last4Digits.length != 4 || !last4Digits.all { it.isDigit() })
                return Result.failure(Exception("Enter exactly 4 digits"))

            val activeCount = accountDao.countActiveAccounts(currentUserId)
            if (activeCount >= 11)
                return Result.failure(Exception("Maximum 11 active accounts allowed. Archive one first."))

            val existing = accountDao.getByLast4Digits(currentUserId, last4Digits)
            if (existing != null)
                return Result.failure(Exception("An account with these last 4 digits already exists"))

            val now = System.currentTimeMillis()
            val account = Account(
                userId = currentUserId,
                accountName = accountName,
                bankName = bankName,
                accountType = accountType,
                last4Digits = last4Digits,
                currentBalance = currentBalance,
                isActive = true,
                createdAt = now,
                updatedAt = now
            )
            Result.success(accountDao.insert(account))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateAccountNameAndBank(
        accountId: Int,
        newName: String,
        newBank: String
    ): Result<Unit> {
        return try {
            val account = accountDao.getById(accountId, currentUserId)
                ?: return Result.failure(Exception("Account not found"))
            val updated = account.copy(
                accountName = newName,
                bankName = newBank,
                updatedAt = System.currentTimeMillis()
            )
            accountDao.update(updated)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun archiveAccount(accountId: Int): Result<Unit> {
        return try {
            accountDao.archive(accountId, System.currentTimeMillis())
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun unarchiveAccount(accountId: Int): Result<Unit> {
        return try {
            val activeCount = accountDao.countActiveAccounts(currentUserId)
            if (activeCount >= 11)
                return Result.failure(Exception("Maximum 11 accounts. Archive one first."))
            accountDao.unarchive(accountId, System.currentTimeMillis())
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateBalance(accountId: Int, newBalance: Double): Result<Unit> {
        return try {
            accountDao.updateBalance(accountId, newBalance, System.currentTimeMillis())
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
