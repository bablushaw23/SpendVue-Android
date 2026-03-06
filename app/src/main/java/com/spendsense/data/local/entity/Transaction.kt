package com.spendsense.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "transactions",
    foreignKeys = [
        ForeignKey(
            entity = Account::class,
            parentColumns = ["id"],
            childColumns = ["account_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["account_id"]),
        Index(value = ["category"])
    ]
)
data class Transaction(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,

    @ColumnInfo(name = "account_id")
    val accountId: Int,

    @ColumnInfo(name = "amount")
    val amount: Double,

    @ColumnInfo(name = "merchant")
    val merchant: String?,

    @ColumnInfo(name = "category")
    val category: String?,

    @ColumnInfo(name = "transaction_type")
    val transactionType: String, // DEBIT, CREDIT

    @ColumnInfo(name = "timestamp")
    val timestamp: Long, // Epoch ms

    @ColumnInfo(name = "source")
    val source: String, // SMS_AUTO, MANUAL

    @ColumnInfo(name = "is_flagged")
    val isFlagged: Boolean = false,

    @ColumnInfo(name = "flag_reason")
    val flagReason: String? = null,

    @ColumnInfo(name = "status")
    val status: String, // CONFIRMED, PARTIAL

    @ColumnInfo(name = "notes")
    val notes: String? = null,

    @ColumnInfo(name = "raw_sms")
    val rawSms: String? = null
)
