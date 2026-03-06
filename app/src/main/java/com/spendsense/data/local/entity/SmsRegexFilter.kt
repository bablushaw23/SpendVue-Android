package com.spendsense.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "sms_regex_filters")
data class SmsRegexFilter(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,

    @ColumnInfo(name = "bank_name")
    val bankName: String,

    @ColumnInfo(name = "sender_id_pattern")
    val senderIdPattern: String, // e.g., ".*HDFCBK.*"

    @ColumnInfo(name = "message_pattern")
    val messagePattern: String, // Regex to identify financial eligibility

    @ColumnInfo(name = "is_active")
    val isActive: Boolean = true
)
