// File: app/src/main/java/com/example/chatlogger/logger/SmsReceiver.kt
package com.example.chatlogger.logger

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import android.util.Log
import com.example.chatlogger.utils.ContactsHelper
import com.example.chatlogger.utils.PreferenceUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class SmsReceiver : BroadcastReceiver() {
    private val TAG = "SmsReceiver"
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onReceive(context: Context, intent: Intent) {
        // Check if logger is enabled in preferences
        if (!PreferenceUtils.isLoggerEnabled()) {
            Log.d(TAG, "Message logger is disabled. Ignoring SMS.")
            return
        }

        // Check if this is an SMS received intent
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) {
            return
        }

        val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
        if (messages.isEmpty()) {
            Log.d(TAG, "No SMS messages found in intent")
            return
        }

        scope.launch {
            try {
                // Process each message
                for (smsMessage in messages) {
                    val phoneNumber = smsMessage.displayOriginatingAddress
                    val messageBody = smsMessage.displayMessageBody

                    // Try to resolve contact name
                    val contactsHelper = ContactsHelper(context)
                    val contactName = contactsHelper.getContactNameFromNumber(phoneNumber)
                    val sender = contactName ?: phoneNumber

                    // Log message
                    val logger = MessageLogger.getInstance(context)
                    logger.logSmsMessage(sender, messageBody)

                    Log.d(TAG, "Logged SMS from $sender: ${messageBody.take(20)}...")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error processing SMS", e)
            }
        }
    }
}