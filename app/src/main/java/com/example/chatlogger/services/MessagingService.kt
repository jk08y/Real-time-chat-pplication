// File: app/src/main/java/com/example/chatlogger/services/MessagingService.kt
package com.example.chatlogger.services

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.chatlogger.R
import com.example.chatlogger.activities.ChatActivity
import com.example.chatlogger.utils.Constants
import com.example.chatlogger.utils.PreferenceUtils
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import java.util.Date

class MessagingService : FirebaseMessagingService() {
    private val TAG = "MessagingService"

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        Log.d(TAG, "Message received from: ${remoteMessage.from}")

        // Check if message contains a data payload
        if (remoteMessage.data.isNotEmpty()) {
            val data = remoteMessage.data
            Log.d(TAG, "Message data payload: $data")

            // Process different types of notifications
            when (data["type"]) {
                "new_message" -> {
                    handleNewMessageNotification(
                        data["chatId"] ?: "",
                        data["senderId"] ?: "",
                        data["senderName"] ?: "New message",
                        data["message"] ?: "",
                        data["timestamp"]?.toLongOrNull() ?: System.currentTimeMillis()
                    )
                }

                "typing" -> {
                    // Handle typing notification (update UI if chat is open)
                    // This doesn't need to show a notification to the user
                }

                else -> {
                    // Default handling for other notification types
                    if (remoteMessage.notification != null) {
                        val notification = remoteMessage.notification!!
                        sendNotification(
                            notification.title ?: "New Notification",
                            notification.body ?: "",
                            null
                        )
                    }
                }
            }
        }

        // Check if message contains a notification payload
        if (remoteMessage.notification != null) {
            val notification = remoteMessage.notification!!
            Log.d(TAG, "Message Notification Title: ${notification.title}")
            Log.d(TAG, "Message Notification Body: ${notification.body}")

            sendNotification(
                notification.title ?: "New Notification",
                notification.body ?: "",
                null
            )
        }
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d(TAG, "Refreshed FCM token: $token")

        // Update token in Firestore
        sendRegistrationToServer(token)
    }

    private fun sendRegistrationToServer(token: String) {
        val currentUser = FirebaseAuth.getInstance().currentUser ?: return

        val userRef = FirebaseFirestore.getInstance()
            .collection(Constants.COLLECTION_USERS)
            .document(currentUser.uid)

        userRef.update("fcmToken", token)
            .addOnSuccessListener {
                Log.d(TAG, "FCM token updated successfully")
            }
            .addOnFailureListener { e ->
                Log.w(TAG, "Error updating FCM token", e)
            }
    }

    private fun handleNewMessageNotification(
        chatId: String,
        senderId: String,
        senderName: String,
        message: String,
        timestamp: Long
    ) {
        // Skip notification if the sender is the current user
        val currentUserId = PreferenceUtils.getUserId()
        if (currentUserId == senderId) {
            return
        }

        // Create intent to open the chat
        val intent = Intent(this, ChatActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            putExtra(Constants.EXTRA_CHAT_ID, chatId)
        }

        val pendingIntent = PendingIntent.getActivity(
            this,
            chatId.hashCode(),
            intent,
            PendingIntent.FLAG_IMMUTABLE
        )

        // Send notification
        sendNotification(senderName, message, pendingIntent)
    }

    private fun sendNotification(title: String, messageBody: String, pendingIntent: PendingIntent?) {
        val defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

        val notificationBuilder = NotificationCompat.Builder(this, Constants.CHANNEL_MESSAGES)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(messageBody)
            .setAutoCancel(true)
            .setSound(defaultSoundUri)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)

        if (pendingIntent != null) {
            notificationBuilder.setContentIntent(pendingIntent)
        }

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Generate unique notification ID based on timestamp
        val notificationId = Date().time.toInt()

        notificationManager.notify(notificationId, notificationBuilder.build())
    }
}