// File: app/src/main/java/com/example/chatlogger/logger/NotificationService.kt
package com.example.chatlogger.logger

import android.app.Notification
import android.content.Context
import android.os.Bundle
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import com.example.chatlogger.utils.Constants
import com.example.chatlogger.utils.PreferenceUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.json.JSONObject

class NotificationService : NotificationListenerService() {
    private val TAG = "NotificationService"
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        // Check if logger is enabled
        if (!PreferenceUtils.isLoggerEnabled()) {
            return
        }

        // Only interested in WhatsApp notifications
        if (sbn.packageName != Constants.WHATSAPP_PACKAGE) {
            return
        }

        // Get notification extras
        val extras: Bundle = sbn.notification.extras ?: return

        // Extract information
        val title = extras.getString(Notification.EXTRA_TITLE)
        val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString()
        val bigText = extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString()

        // Ignore if no title (sender) or message
        if (title.isNullOrEmpty() || (text.isNullOrEmpty() && bigText.isNullOrEmpty())) {
            return
        }

        // Skip notifications that aren't actual messages
        if (isStatusNotification(text ?: "") || isGroupAddNotification(text ?: "")) {
            return
        }

        // Extract message content (prefer big text if available)
        val messageContent = bigText ?: text ?: return

        // Create JSON for raw data (useful for debugging)
        val rawJson = JSONObject().apply {
            put("title", title)
            put("text", text)
            put("bigText", bigText)
            put("packageName", sbn.packageName)
            put("postTime", sbn.postTime)
            put("id", sbn.id)
        }.toString()

        scope.launch {
            try {
                // Log the WhatsApp message
                val logger = MessageLogger.getInstance(applicationContext)
                logger.logWhatsAppMessage(
                    sender = title,
                    message = messageContent,
                    rawJson = rawJson
                )

                Log.d(TAG, "Logged WhatsApp message from $title: ${messageContent.take(20)}...")
            } catch (e: Exception) {
                Log.e(TAG, "Error logging WhatsApp message", e)
            }
        }
    }

    // Helper to check if it's a status notification (typing, online, etc.)
    private fun isStatusNotification(text: String): Boolean {
        val statusPatterns = listOf(
            "typing...",
            "online",
            "recording audio...",
            "is video calling",
            "audio call",
            "video call"
        )

        return statusPatterns.any { text.contains(it, ignoreCase = true) }
    }

    // Helper to check if it's a group-related notification
    private fun isGroupAddNotification(text: String): Boolean {
        val groupPatterns = listOf(
            "added you",
            "created group",
            "changed the subject",
            "changed this group's icon",
            "left"
        )

        return groupPatterns.any { text.contains(it, ignoreCase = true) }
    }
}