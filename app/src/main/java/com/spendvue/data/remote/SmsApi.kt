package com.spendvue.data.remote

import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST

// ---------------------------------------------------------------------------
// SMS Parsing API - sends SMS to backend for AI-powered parsing
// ---------------------------------------------------------------------------

data class SmsParseRequest(
    val smsText: String,
    val sender: String,
    val timestamp: String // ISO 8601 format
)

data class ParsedSmsData(
    val amount: Double,
    val merchant: String?,
    val last4Digits: String,
    val transactionType: String, // "DEBIT" or "CREDIT"
    val category: String?,
    val confidence: String? // "HIGH", "MEDIUM", "LOW"
)

data class SmsParseResponse(
    val success: Boolean,
    val data: ParsedSmsData? = null,
    val error: String? = null
)

interface SmsApi {
    /**
     * Send SMS to backend for AI parsing.
     * Returns structured transaction data.
     */
    @POST("api/sms/parse")
    suspend fun parseSms(
        @Header("Authorization") token: String,
        @Body request: SmsParseRequest
    ): SmsParseResponse
}