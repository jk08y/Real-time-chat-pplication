// File: app/src/main/java/com/example/chatlogger/repositories/LoggerRepository.kt
package com.example.chatlogger.repositories

import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.example.chatlogger.models.LoggedMessage
import com.example.chatlogger.services.LoggerService
import com.example.chatlogger.utils.Constants
import com.example.chatlogger.utils.PermissionUtils
import com.example.chatlogger.utils.PreferenceUtils
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await
import java.util.Date

class LoggerRepository(private val context: Context) {
    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val TAG = "LoggerRepository"

    // Get current user ID
    private fun getCurrentUserId(): String {
        return auth.currentUser?.uid ?: ""
    }

    // Start the background logger service
    fun startLoggerService() {
        if (getCurrentUserId().isEmpty()) {
            Log.d(TAG, "Cannot start logger: User not logged in")
            return
        }

        // Create intent for the service
        val serviceIntent = Intent(context, LoggerService::class.java).apply {
            action = Constants.ACTION_START_LOGGER_SERVICE
        }

        // Start the service
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(serviceIntent)
        } else {
            context.startService(serviceIntent)
        }

        PreferenceUtils.setLoggerEnabled(true)
        Log.d(TAG, "Logger service started")
    }

    // Stop the background logger service
    fun stopLoggerService() {
        val serviceIntent = Intent(context, LoggerService::class.java).apply {
            action = Constants.ACTION_STOP_LOGGER_SERVICE
        }

        context.stopService(serviceIntent)
        PreferenceUtils.setLoggerEnabled(false)
        Log.d(TAG, "Logger service stopped")
    }

    // Check if logger is running
    fun isLoggerRunning(): Boolean {
        return PreferenceUtils.isLoggerEnabled()
    }

    // Check if all permissions for logger are granted
    fun hasLoggerPermissions(): Boolean {
        // Check SMS permissions
        if (!PermissionUtils.isPermissionGranted(context, android.Manifest.permission.RECEIVE_SMS) ||
            !PermissionUtils.isPermissionGranted(context, android.Manifest.permission.READ_SMS)) {
            return false
        }

        // Check notification listener permission
        if (!PermissionUtils.isNotificationListenerEnabled(context)) {
            return false
        }

        // Check foreground service permission for Android 9+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P &&
            !PermissionUtils.isPermissionGranted(context, android.Manifest.permission.FOREGROUND_SERVICE)) {
            return false
        }

        // Check notification permission for Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            !PermissionUtils.isPermissionGranted(context, android.Manifest.permission.POST_NOTIFICATIONS)) {
            return false
        }

        return true
    }

    // Get all logged messages
    suspend fun getLoggedMessages(limit: Long = 100): Result<List<LoggedMessage>> {
        val userId = getCurrentUserId()
        if (userId.isEmpty()) {
            return Result.failure(Exception("User not logged in"))
        }

        return try {
            val querySnapshot = firestore.collection(Constants.COLLECTION_LOGGED_MESSAGES)
                .whereEqualTo("userId", userId)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .limit(limit)
                .get()
                .await()

            val messages = querySnapshot.documents.mapNotNull { document ->
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
                    Log.e(TAG, "Error parsing logged message document", e)
                    null
                }
            }

            Result.success(messages)
        } catch (e: Exception) {
            Log.e(TAG, "Error getting logged messages", e)
            Result.failure(e)
        }
    }

    // Delete a logged message
    suspend fun deleteLoggedMessage(messageId: String): Result<Unit> {
        val userId = getCurrentUserId()
        if (userId.isEmpty()) {
            return Result.failure(Exception("User not logged in"))
        }

        return try {
            firestore.collection(Constants.COLLECTION_LOGGED_MESSAGES)
                .document(messageId)
                .delete()
                .await()

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting logged message", e)
            Result.failure(e)
        }
    }

    // Clear all logged messages
    suspend fun clearAllLoggedMessages(): Result<Unit> {
        val userId = getCurrentUserId()
        if (userId.isEmpty()) {
            return Result.failure(Exception("User not logged in"))
        }

        return try {
            val batch = firestore.batch()
            val querySnapshot = firestore.collection(Constants.COLLECTION_LOGGED_MESSAGES)
                .whereEqualTo("userId", userId)
                .get()
                .await()

            for (document in querySnapshot.documents) {
                batch.delete(document.reference)
            }

            batch.commit().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error clearing logged messages", e)
            Result.failure(e)
        }
    }

    // Get message statistics
    suspend fun getMessageStatistics(): Result<Map<String, Any>> {
        val userId = getCurrentUserId()
        if (userId.isEmpty()) {
            return Result.failure(Exception("User not logged in"))
        }

        return try {
            val querySnapshot = firestore.collection(Constants.COLLECTION_LOGGED_MESSAGES)
                .whereEqualTo("userId", userId)
                .get()
                .await()

            val messages = querySnapshot.documents.mapNotNull { document ->
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

            // Calculate statistics
            val totalMessages = messages.size

            val smsCount = messages.count { it.source == LoggedMessage.SOURCE_SMS }
            val whatsappCount = messages.count { it.source == LoggedMessage.SOURCE_WHATSAPP }

            val senderFrequency = messages.groupBy { it.sender }
                .mapValues { it.value.size }
                .toList()
                .sortedByDescending { it.second }
                .take(5)
                .toMap()

            val hourlyDistribution = messages.groupBy {
                it.timestamp.hours
            }.mapValues { it.value.size }

            val statistics = mapOf(
                "totalMessages" to totalMessages,
                "smsCount" to smsCount,
                "whatsappCount" to whatsappCount,
                "topSenders" to senderFrequency,
                "hourlyDistribution" to hourlyDistribution
            )

            Result.success(statistics)
        } catch (e: Exception) {
            Log.e(TAG, "Error getting message statistics", e)
            Result.failure(e)
        }
    }
}