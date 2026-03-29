package com.spendvue.sms

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat

import com.spendvue.MainActivity

import com.spendvue.auth.AuthManager
import com.spendvue.data.remote.SmsApi
import com.spendvue.data.remote.SmsParseRequest
import com.spendvue.data.repository.AccountRepository
import com.spendvue.data.repository.TransactionRepository

import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

/**
 * Foreground service that processes financial SMS.
 * 
 * Started by SmsReceiver when a financial SMS is detected.
 * Runs in foreground to avoid being killed by Android.
 * Calls backend API for AI parsing, matches account, saves transaction,
 * and shows notification to user.
 */
@AndroidEntryPoint
class SmsProcessingService : android.app.Service() {

    companion object {
        private const val TAG = "SmsProcessingService"
        private const val NOTIFICATION_CHANNEL_ID = "sms_processing_channel"
        private const val NOTIFICATION_ID = 1001
        private const val NOTIFICATION_TITLE = "Processing transactions"
    }

    @Inject
    lateinit var smsApi: SmsApi

    @Inject
    lateinit var authManager: AuthManager

    @Inject
    lateinit var accountRepository: AccountRepository

    @Inject
    lateinit var transactionRepository: TransactionRepository

    private val serviceScope = CoroutineScope(Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "Service created")
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "Service started")

        // Start foreground with notification
        val notification = buildNotification()
        startForeground(NOTIFICATION_ID, notification)

        // Process SMS if intent contains data
        intent?.let {
            val smsText = it.getStringExtra("sms_text")
            val sender = it.getStringExtra("sender")
            val timestamp = it.getLongExtra("timestamp", 0L)
            
            if (smsText != null && sender != null && timestamp > 0) {
                Log.d(TAG, "Processing SMS from $sender")
                serviceScope.launch {
                    processSms(smsText, sender, timestamp)
                }
            } else {
                Log.w(TAG, "Missing SMS data in intent")
                stopSelf()
            }
        } ?: run {
            Log.w(TAG, "Null intent, stopping service")
            stopSelf()
        }

        // Service will stop itself after processing
        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?) = null

    private suspend fun processSms(smsText: String, sender: String, timestamp: Long) {
        try {
            Log.d(TAG, "Starting SMS processing")
            
            // 1. Check if user is logged in
            if (!authManager.isLoggedIn()) {
                Log.w(TAG, "User not logged in, cannot process SMS")
                showErrorNotification("Please log in to process transactions")
                stopSelf()
                return
            }

            // 2. Convert timestamp to ISO 8601 string
            val timestampIso = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply {
                timeZone = TimeZone.getTimeZone("UTC")
            }.format(Date(timestamp))

            // 3. Get JWT token for authorization
            val token = authManager.getToken() ?: run {
                Log.w(TAG, "No authentication token")
                showErrorNotification("Authentication required")
                stopSelf()
                return
            }

            // 4. Call backend API for parsing
            val parsedData = withContext(Dispatchers.IO) {
                try {
                    val request = SmsParseRequest(smsText, sender, timestampIso)
                    val response = smsApi.parseSms("Bearer $token", request)
                    if (response.success && response.data != null) {
                        response.data
                    } else {
                        Log.e(TAG, "Backend parsing failed: ${response.error}")
                        null
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "API call failed: ${e.message}", e)
                    null
                }
            }

            if (parsedData == null) {
                showErrorNotification("Failed to parse SMS. Please check connection.")
                stopSelf()
                return
            }

            Log.d(TAG, "Parsed data: $parsedData")

            // 5. Match account using last 4 digits
            val account = withContext(Dispatchers.IO) {
                try {
                    accountRepository.getAccountByLast4Digits(parsedData.last4Digits)
                } catch (e: Exception) {
                    Log.e(TAG, "Account lookup failed: ${e.message}")
                    null
                }
            }

            if (account == null) {
                Log.w(TAG, "No account found with last 4 digits: ${parsedData.last4Digits}")
                showErrorNotification("No account found ending with ${parsedData.last4Digits}. Please add account.")
                stopSelf()
                return
            }

            // 6. Check for duplicate transaction
            val isDuplicate = withContext(Dispatchers.IO) {
                transactionRepository.checkDuplicate(account.id, parsedData.amount, timestamp)
            }
            
            if (isDuplicate) {
                Log.i(TAG, "Duplicate transaction detected, ignoring")
                showNotification("Duplicate transaction ignored", "₹${parsedData.amount} already recorded")
                stopSelf()
                return
            }

            // 7. Create transaction
            val transactionId = withContext(Dispatchers.IO) {
                transactionRepository.createSmsTransaction(
                    accountId = account.id,
                    amount = parsedData.amount,
                    merchant = parsedData.merchant,
                    category = parsedData.category,
                    transactionType = parsedData.transactionType,
                    timestamp = timestamp,
                    rawSms = smsText
                )
            }

            when {
                transactionId.isSuccess -> {
                    Log.i(TAG, "Transaction saved with ID: ${transactionId.getOrNull()}")
                    
                    // 8. Update account balance (simplified: just subtract/increment)
                    // FUTURE: Implement proper balance update based on transaction type
                    
                    // 9. Show success notification
                    val merchant = parsedData.merchant ?: "Unknown merchant"
                    val category = parsedData.category ?: "Uncategorized"
                    showSuccessNotification(
                        amount = parsedData.amount,
                        merchant = merchant,
                        category = category,
                        accountName = account.accountName
                    )
                }
                transactionId.isFailure -> {
                    Log.e(TAG, "Failed to save transaction: ${transactionId.exceptionOrNull()?.message}")
                    showErrorNotification("Failed to save transaction")
                }
            }

        } catch (e: Exception) {
            Log.e(TAG, "Unexpected error processing SMS", e)
            showErrorNotification("Error processing transaction")
        } finally {
            // Stop service after processing
            stopSelf()
        }
    }

    private fun showSuccessNotification(amount: Double, merchant: String, category: String, accountName: String) {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        
        val contentText = "₹$amount at $merchant\n$category • $accountName"
        
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info) // TODO: Add notification icon
            .setContentTitle("Transaction recorded")
            .setContentText(contentText)
            .setStyle(NotificationCompat.BigTextStyle().bigText(contentText))
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        notificationManager.notify(NOTIFICATION_ID + 1, notification)
    }

    private fun showNotification(title: String, text: String) {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        
        val notification = NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(text)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(NOTIFICATION_ID + 2, notification)
    }

    private fun showErrorNotification(message: String) {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        
        val notification = NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("SpendSense Error")
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(NOTIFICATION_ID + 3, notification)
    }

    private fun buildNotification(): Notification {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(NOTIFICATION_TITLE)
            .setContentText("Processing bank transaction...")
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .setContentIntent(pendingIntent)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                "Transaction Processing",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows when SpendSense is processing bank transactions"
            }
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
}