package com.spendvue.data.remote

import kotlinx.coroutines.delay
import java.util.regex.Pattern

/**
 * Mock implementation of SmsApi for testing without backend.
 * Uses regex patterns to parse common Indian bank SMS formats.
 */
class MockSmsApi : SmsApi {
    
    companion object {
        private const val TAG = "MockSmsApi"
        
        // Regex patterns for common SMS formats
        private val DEBIT_PATTERNS = listOf(
            Pattern.compile("Rs\\.?\\s*(\\d+(?:,\\d+)*(?:\\.\\d{1,2})?)\\s*debited", Pattern.CASE_INSENSITIVE),
            Pattern.compile("INR\\s*(\\d+(?:,\\d+)*(?:\\.\\d{1,2})?)\\s*debited", Pattern.CASE_INSENSITIVE),
            Pattern.compile("debited\\s*(?:with|by)?\\s*Rs\\.?\\s*(\\d+(?:,\\d+)*(?:\\.\\d{1,2})?)", Pattern.CASE_INSENSITIVE)
        )
        
        private val CREDIT_PATTERNS = listOf(
            Pattern.compile("Rs\\.?\\s*(\\d+(?:,\\d+)*(?:\\.\\d{1,2})?)\\s*credited", Pattern.CASE_INSENSITIVE),
            Pattern.compile("INR\\s*(\\d+(?:,\\d+)*(?:\\.\\d{1,2})?)\\s*credited", Pattern.CASE_INSENSITIVE)
        )
        
        private val LAST4_PATTERNS = listOf(
            Pattern.compile("\\*\\*(\\d{4})"),
            Pattern.compile("Card\\s*\\*\\*(\\d{4})", Pattern.CASE_INSENSITIVE),
            Pattern.compile("A/c\\s*\\*\\*(\\d{4})", Pattern.CASE_INSENSITIVE),
            Pattern.compile("Account\\s*\\*\\*(\\d{4})", Pattern.CASE_INSENSITIVE)
        )
        
        private val MERCHANT_PATTERNS = listOf(
            Pattern.compile("at\\s+([A-Z][A-Z\\s]+)(?:\\.|\\s|$)", Pattern.CASE_INSENSITIVE),
            Pattern.compile("to\\s+([A-Z][A-Z\\s]+)(?:\\.|\\s|$)", Pattern.CASE_INSENSITIVE),
            Pattern.compile("via\\s+([A-Z][A-Z\\s]+)(?:\\.|\\s|$)", Pattern.CASE_INSENSITIVE)
        )
        
        // Merchant to category mapping
        private val CATEGORY_MAP = mapOf(
            "STARBUCKS" to "Dining",
            "SWIGGY" to "Dining",
            "ZOMATO" to "Dining",
            "UBEREATS" to "Dining",
            "MCDONALDS" to "Dining",
            "DOMINOS" to "Dining",
            "AMAZON" to "Shopping",
            "FLIPKART" to "Shopping",
            "MYNTRA" to "Shopping",
            "CROMA" to "Shopping",
            "RELIANCE" to "Shopping",
            "BIGBAZAAR" to "Groceries",
            "DMART" to "Groceries",
            "BIGBASKET" to "Groceries",
            "GROFERS" to "Groceries",
            "INDIANOIL" to "Fuel",
            "HPCL" to "Fuel",
            "BPCL" to "Fuel",
            "SHELL" to "Fuel",
            "NETFLIX" to "Entertainment",
            "PRIMEVIDEO" to "Entertainment",
            "HOTSTAR" to "Entertainment",
            "SPOTIFY" to "Entertainment",
            "YOUTUBE" to "Entertainment"
        )
    }
    
    override suspend fun parseSms(token: String, request: SmsParseRequest): SmsParseResponse {
        // Simulate network delay
        delay(1000)
        
        val smsText = request.smsText
        val sender = request.sender
        
        println("MockSmsApi parsing SMS: $smsText")
        
        // Extract amount
        val amount = extractAmount(smsText)
        if (amount == null) {
            return SmsParseResponse(success = false, error = "Could not extract amount from SMS")
        }
        
        // Determine transaction type
        val transactionType = if (smsText.contains("debited", ignoreCase = true)) {
            "DEBIT"
        } else if (smsText.contains("credited", ignoreCase = true)) {
            "CREDIT"
        } else {
            // Default to debit if cannot determine
            "DEBIT"
        }
        
        // Extract last 4 digits
        val last4Digits = extractLast4Digits(smsText) ?: "0000"
        
        // Extract merchant
        val merchant = extractMerchant(smsText)
        
        // Determine category
        val category = determineCategory(merchant, smsText)
        
        // Determine confidence
        val confidence = when {
            amount != null && last4Digits != "0000" && merchant != null -> "HIGH"
            amount != null && last4Digits != "0000" -> "MEDIUM"
            else -> "LOW"
        }
        
        val parsedData = ParsedSmsData(
            amount = amount,
            merchant = merchant,
            last4Digits = last4Digits,
            transactionType = transactionType,
            category = category,
            confidence = confidence
        )
        
        return SmsParseResponse(success = true, data = parsedData)
    }
    
    private fun extractAmount(sms: String): Double? {
        // Try debit patterns first
        for (pattern in DEBIT_PATTERNS) {
            val matcher = pattern.matcher(sms)
            if (matcher.find()) {
                val amountStr = matcher.group(1)?.replace(",", "")
                return amountStr?.toDoubleOrNull()
            }
        }
        
        // Try credit patterns
        for (pattern in CREDIT_PATTERNS) {
            val matcher = pattern.matcher(sms)
            if (matcher.find()) {
                val amountStr = matcher.group(1)?.replace(",", "")
                return amountStr?.toDoubleOrNull()
            }
        }
        
        // Fallback: look for Rs.XXXX pattern
        val fallbackPattern = Pattern.compile("Rs\\.?\\s*(\\d+(?:,\\d+)*(?:\\.\\d{1,2})?)")
        val matcher = fallbackPattern.matcher(sms)
        if (matcher.find()) {
            val amountStr = matcher.group(1)?.replace(",", "")
            return amountStr?.toDoubleOrNull()
        }
        
        return null
    }
    
    private fun extractLast4Digits(sms: String): String? {
        for (pattern in LAST4_PATTERNS) {
            val matcher = pattern.matcher(sms)
            if (matcher.find()) {
                return matcher.group(1)
            }
        }
        return null
    }
    
    private fun extractMerchant(sms: String): String? {
        for (pattern in MERCHANT_PATTERNS) {
            val matcher = pattern.matcher(sms)
            if (matcher.find()) {
                var merchant = matcher.group(1)?.trim()
                // Clean up merchant name
                merchant = merchant?.replace("\\s+".toRegex(), " ")
                merchant = merchant?.takeIf { it.length in 2..50 }
                return merchant
            }
        }
        return null
    }
    
    private fun determineCategory(merchant: String?, sms: String): String? {
        // If merchant is known, map to category
        merchant?.let {
            val upperMerchant = it.uppercase()
            for ((key, category) in CATEGORY_MAP) {
                if (upperMerchant.contains(key)) {
                    return category
                }
            }
        }
        
        // Guess from SMS keywords
        val upperSms = sms.uppercase()
        return when {
            upperSms.contains("FOOD") || upperSms.contains("RESTAURANT") || upperSms.contains("CAFE") -> "Dining"
            upperSms.contains("PETROL") || upperSms.contains("DIESEL") || upperSms.contains("FUEL") -> "Fuel"
            upperSms.contains("GROCERY") || upperSms.contains("VEGETABLE") || upperSms.contains("FRUIT") -> "Groceries"
            upperSms.contains("MOVIE") || upperSms.contains("CINEMA") || upperSms.contains("THEATER") -> "Entertainment"
            upperSms.contains("BILL") || upperSms.contains("ELECTRICITY") || upperSms.contains("WATER") -> "Bills"
            upperSms.contains("MEDICAL") || upperSms.contains("HOSPITAL") || upperSms.contains("PHARMACY") -> "Healthcare"
            else -> null
        }
    }
}