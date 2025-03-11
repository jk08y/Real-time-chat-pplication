// File: app/src/main/java/com/example/chatlogger/logger/MessageLogger.kt
package com.example.chatlogger.logger

import android.content.Context
import com.example.chatlogger.App
import com.example.chatlogger.models.LoggedMessage
import com.example.chatlogger.utils.Constants
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.Date

class MessageLogger(private val context: Context) {
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
    private val loggerScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    companion object {
        private var instance: MessageLogger? = null

        fun getInstance(context: Context): MessageLogger {
            return instance ?: synchronized(this) {
                instance ?: MessageLogger(context.applicationContext).also { instance = it }
            }
        }
    }

    // Log a new SMS message
    fun logSmsMessage(sender: String, message: String) {
        val userId = com.example.chatlogger.utils.PreferenceUtils.getUserId()
        if (userId.isEmpty()) return // Not logged in

        loggerScope.launch {
            try {
                val loggedMessage = LoggedMessage(
                    source = LoggedMessage.SOURCE_SMS,
                    sender = sender,
                    message = message,
                    timestamp = Date(),
                    deviceId = App.getDeviceId(),
                    userId = userId
                )

                saveLoggedMessage(loggedMessage)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // Log a new WhatsApp message
    fun logWhatsAppMessage(sender: String, message: String, rawJson: String? = null) {
        val userId = com.example.chatlogger.utils.PreferenceUtils.getUserId()
        if (userId.isEmpty()) return // Not logged in

        loggerScope.launch {
            try {
                val loggedMessage = LoggedMessage(
                    source = LoggedMessage.SOURCE_WHATSAPP,
                    sender = sender,
                    message = message,
                    timestamp = Date(),
                    deviceId = App.getDeviceId(),
                    userId = userId,
                    rawJson = rawJson
                )

                saveLoggedMessage(loggedMessage)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // Save message to Firestore
    private suspend fun saveLoggedMessage(loggedMessage: LoggedMessage) = withContext(Dispatchers.IO) {
        try {
            val collection = firestore.collection(Constants.COLLECTION_LOGGED_MESSAGES)
            collection.add(loggedMessage.toMap()).await()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // Get all logged messages for current user
    suspend fun getLoggedMessages(limit: Long = 100): List<LoggedMessage> = withContext(Dispatchers.IO) {
        val userId = com.example.chatlogger.utils.PreferenceUtils.getUserId()
        if (userId.isEmpty()) return@withContext emptyList<LoggedMessage>()

        try {
            val querySnapshot = firestore.collection(Constants.COLLECTION_LOGGED_MESSAGES)
                .whereEqualTo("userId", userId)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .limit(limit)
                .get()
                .await()

            return@withContext querySnapshot.documents.mapNotNull { document ->
                try {
                    val data = document.data ?: return@mapNotNull null
                    LoggedMessage(
                        id = document.id,
                        source = data["source"] as? String ?: "",
                        sender = data["sender"] as? String ?: "",
                        message = data["message"] as? String ?: "",
                        timestamp = data["timestamp"] as? Date ?: Date(),
                        deviceId = data["deviceId"] as? String ?: "",
                        userId = data["userId"] as? String ?: "",
                        rawJson = data["rawJson"] as? String
                    )
                } catch (e: Exception) {
                    null
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            return@withContext emptyList<LoggedMessage>()
        }
    }

    // Delete a logged message
    suspend fun deleteLoggedMessage(messageId: String): Boolean = withContext(Dispatchers.IO) {
        try {
            firestore.collection(Constants.COLLECTION_LOGGED_MESSAGES)
                .document(messageId)
                .delete()
                .await()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    // Clear all logged messages for current user
    suspend fun clearAllLoggedMessages(): Boolean = withContext(Dispatchers.IO) {
        val userId = com.example.chatlogger.utils.PreferenceUtils.getUserId()
        if (userId.isEmpty()) return@withContext false

        try {
            val batch = firestore.batch()
            val querySnapshot = firestore.collection(Constants.COLLECTION_LOGGED_MESSAGES)
                .whereEqualTo("userId", userId)
                .get()
                .await()

            for (document in querySnapshot) {
                batch.delete(document.reference)
            }

            batch.commit().await()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}