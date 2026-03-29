package com.spendvue.sms

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Telephony
import android.util.Log
import androidx.core.content.ContextCompat

/**
 * Receives SMS broadcasts and forwards financial SMS to processing service.
 *
 * Important: This receiver must be registered in AndroidManifest.xml with
 * android:permission="android.permission.BROADCAST_SMS" and appropriate intent filter.
 *
 * For Android 8.0+ (Oreo), background execution limits require starting a
 * foreground service immediately.
 */
class SmsReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "SmsReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        Log.d(TAG, "SMS received intent action: ${intent.action}")
        
        // Only process SMS received action
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) {
            return
        }

        // Extract SMS messages
        val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
        if (messages.isNullOrEmpty()) {
            Log.w(TAG, "No SMS messages in intent")
            return
        }

        // Combine message parts
        val fullMessage = StringBuilder()
        var sender: String? = null
        var timestamp: Long = 0

        for (sms in messages) {
            fullMessage.append(sms.messageBody)
            if (sender == null) {
                sender = sms.originatingAddress
            }
            // Use the timestamp from the first message
            if (timestamp == 0L) {
                timestamp = sms.timestampMillis
            }
        }

        val smsText = fullMessage.toString()
        val senderId = sender ?: ""

        Log.d(TAG, "SMS from $senderId: ${smsText.take(50)}...")

        // Quick noise gate: check if sender looks like a bank/payment provider
        if (!isLikelyFinancialSms(senderId, smsText)) {
            Log.d(TAG, "Non-financial SMS ignored")
            return
        }

        Log.i(TAG, "Financial SMS detected, starting processing service")

        // Start foreground service to process SMS (keeps app alive)
        val serviceIntent = Intent(context, SmsProcessingService::class.java).apply {
            putExtra("sms_text", smsText)
            putExtra("sender", senderId)
            putExtra("timestamp", timestamp)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            ContextCompat.startForegroundService(context, serviceIntent)
        } else {
            context.startService(serviceIntent)
        }
    }

    /**
     * Simple noise gate to filter out non-financial SMS.
     * Returns true if SMS appears to be from a bank or payment provider.
     *
     * FUTURE: Replace with more sophisticated filtering using SmsRegexFilter table.
     */
    private fun isLikelyFinancialSms(sender: String, message: String): Boolean {
        // Check sender patterns (common Indian bank sender IDs)
        val bankSenderPatterns = listOf(
            ".*HDFCBK.*", ".*ICICIB.*", ".*AXISBK.*", ".*SBMSMS.*", ".*YESBNK.*",
            ".*CITIBK.*", ".*KOTAKB.*", ".*IDFBK.*", ".*BARODA.*", ".*SBI.*",
            ".*PNB.*", ".*BOB.*", ".*CANBNK.*", ".*UNIONB.*", ".*INDUSB.*",
            ".*GOOGLEPAY.*", ".*PHONEPE.*", ".*PAYTM.*", ".*AMAZONPAY.*"
        )

        val senderUpper = sender.uppercase()
        if (bankSenderPatterns.any { senderUpper.matches(it.toRegex()) }) {
            return true
        }

        // Check message keywords
        val financialKeywords = listOf(
            "debited", "credited", "rs.", "inr", "balance", "card", "account",
            "transaction", "withdrawal", "deposit", "upi", "payment", "paid"
        )

        val messageUpper = message.uppercase()
        return financialKeywords.any { keyword ->
            messageUpper.contains(keyword.uppercase())
        }
    }
}