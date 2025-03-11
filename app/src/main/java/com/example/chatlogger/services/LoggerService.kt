// File: app/src/main/java/com/example/chatlogger/services/LoggerService.kt
package com.example.chatlogger.services

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.chatlogger.R
import com.example.chatlogger.activities.MainActivity
import com.example.chatlogger.utils.Constants
import com.example.chatlogger.utils.PermissionUtils
import com.example.chatlogger.utils.PreferenceUtils

class LoggerService : Service() {
    private val TAG = "LoggerService"

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "Logger service created")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "Logger service started")

        // Check if notification listener is enabled
        if (!PermissionUtils.isNotificationListenerEnabled(this)) {
            Log.w(TAG, "Notification listener permission not granted")
        }

        // Create and show foreground notification
        startForeground(Constants.NOTIFICATION_ID_LOGGER, createNotification())

        // Mark logger as enabled in preferences
        PreferenceUtils.setLoggerEnabled(true)

        return START_STICKY
    }

    private fun createNotification(): Notification {
        // Create pending intent for notification click
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE
        )

        // Create stop action
        val stopIntent = Intent(this, LoggerService::class.java).apply {
            action = Constants.ACTION_STOP_LOGGER_SERVICE
        }
        val stopPendingIntent = PendingIntent.getService(
            this,
            0,
            stopIntent,
            PendingIntent.FLAG_IMMUTABLE
        )

        // Build the notification
        val builder = NotificationCompat.Builder(this, Constants.CHANNEL_LOGGER)
            .setContentTitle("Message Logger Active")
            .setContentText("Logging incoming messages in background")
            .setSmallIcon(R.drawable.ic_notification)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setOngoing(true)
            .addAction(R.drawable.ic_stop, "Stop Logger", stopPendingIntent)

        // For Android 12+, add a foreground service type
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            builder.setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
        }

        return builder.build()
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "Logger service destroyed")

        // Mark logger as disabled in preferences
        PreferenceUtils.setLoggerEnabled(false)
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }
}