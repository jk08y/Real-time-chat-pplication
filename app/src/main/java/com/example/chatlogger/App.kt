// File: app/src/main/java/com/example/chatlogger/App.kt
package com.example.chatlogger

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.provider.Settings
import com.example.chatlogger.utils.Constants
import com.example.chatlogger.utils.PreferenceUtils
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.util.UUID

class App : Application() {

    companion object {
        lateinit var instance: App
            private set

        val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

        fun getDeviceId(): String {
            val savedDeviceId = PreferenceUtils.getString("device_id", "")
            if (savedDeviceId.isNotEmpty()) {
                return savedDeviceId
            }

            val deviceId = try {
                Settings.Secure.getString(instance.contentResolver, Settings.Secure.ANDROID_ID)
            } catch (e: Exception) {
                UUID.randomUUID().toString()
            }

            PreferenceUtils.putString("device_id", deviceId)
            return deviceId
        }
    }

    override fun onCreate() {
        super.onCreate()
        instance = this

        // Initialize preferences
        PreferenceUtils.init(this)

        // Initialize Firebase
        FirebaseApp.initializeApp(this)

        // Configure Firestore
        val settings = FirebaseFirestoreSettings.Builder()
            .setPersistenceEnabled(true)
            .setCacheSizeBytes(FirebaseFirestoreSettings.CACHE_SIZE_UNLIMITED)
            .build()
        FirebaseFirestore.getInstance().firestoreSettings = settings

        // Create notification channels
        createNotificationChannels()

        // Handle automatic login and online status
        FirebaseAuth.getInstance().addAuthStateListener { auth ->
            val user = auth.currentUser
            if (user != null) {
                applicationScope.launch {
                    updateUserOnlineStatus(true)
                }
            }
        }
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            // Channel for new messages
            val messagesChannel = NotificationChannel(
                Constants.CHANNEL_MESSAGES,
                "Messages",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifications for new messages"
                enableVibration(true)
            }

            // Channel for logger service
            val loggerChannel = NotificationChannel(
                Constants.CHANNEL_LOGGER,
                "Message Logger",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Notification for the background logger service"
                setShowBadge(false)
            }

            // Register channels
            notificationManager.createNotificationChannels(listOf(messagesChannel, loggerChannel))
        }
    }

    fun updateUserOnlineStatus(isOnline: Boolean) {
        val currentUser = FirebaseAuth.getInstance().currentUser ?: return
        val userRef = FirebaseFirestore.getInstance()
            .collection(Constants.COLLECTION_USERS)
            .document(currentUser.uid)

        val updates = hashMapOf<String, Any>(
            "isOnline" to isOnline,
            "lastSeen" to java.util.Date()
        )

        if (isOnline) {
            // Update FCM token when coming online
            FirebaseMessaging.getInstance().token.addOnSuccessListener { token ->
                updates["fcmToken"] = token
                userRef.update(updates)
            }
        } else {
            userRef.update(updates)
        }
    }

    override fun onTerminate() {
        super.onTerminate()
        // Update online status when app terminates
        updateUserOnlineStatus(false)
    }
}