package com.spendvue.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "accounts",
    indices = [
        Index(value = ["user_id", "is_active"]),
        Index(value = ["user_id", "last_4_digits"], unique = true)
    ]
)
data class Account(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    
    @ColumnInfo(name = "user_id")
    val userId: String,
    
    @ColumnInfo(name = "account_name")
    val accountName: String,
    
    @ColumnInfo(name = "bank_name")
    val bankName: String,
    
    @ColumnInfo(name = "account_type")
    val accountType: String,
    
    @ColumnInfo(name = "last_4_digits")
    val last4Digits: String,
    
    @ColumnInfo(name = "current_balance")
    val currentBalance: Double,
    
    @ColumnInfo(name = "is_active")
    val isActive: Boolean = true,
    
    @ColumnInfo(name = "created_at")
    val createdAt: Long,
    
    @ColumnInfo(name = "updated_at")
    val updatedAt: Long,
    
    @ColumnInfo(name = "archived_at")
    val archivedAt: Long? = null,
    
    @ColumnInfo(name = "last_synced_at")
    val lastSyncedAt: Long? = null
)
