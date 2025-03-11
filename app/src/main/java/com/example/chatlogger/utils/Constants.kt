// File: app/src/main/java/com/example/chatlogger/utils/Constants.kt
package com.example.chatlogger.utils

object Constants {
    // Notification Channels
    const val CHANNEL_MESSAGES = "messages_channel"
    const val CHANNEL_LOGGER = "logger_channel"

    // Notification IDs
    const val NOTIFICATION_ID_LOGGER = 1001
    const val NOTIFICATION_ID_NEW_MESSAGE = 1002

    // Intent Actions
    const val ACTION_START_LOGGER_SERVICE = "com.example.chatlogger.START_LOGGER_SERVICE"
    const val ACTION_STOP_LOGGER_SERVICE = "com.example.chatlogger.STOP_LOGGER_SERVICE"

    // Intent Extras
    const val EXTRA_USER_ID = "extra_user_id"
    const val EXTRA_CHAT_ID = "extra_chat_id"
    const val EXTRA_MESSAGE_ID = "extra_message_id"

    // Firestore Collections
    const val COLLECTION_USERS = "users"
    const val COLLECTION_CHATS = "chats"
    const val COLLECTION_MESSAGES = "messages"
    const val COLLECTION_LOGGED_MESSAGES = "logged_messages"

    // WhatsApp Package Name
    const val WHATSAPP_PACKAGE = "com.whatsapp"
    const val WHATSAPP_BUSINESS_PACKAGE = "com.whatsapp.w4b"

    // Permission Request Codes
    const val REQUEST_CODE_NOTIFICATION_PERMISSION = 1234
    const val REQUEST_CODE_PERMISSIONS = 2345
    const val REQUEST_CODE_IMAGE_PICK = 3456
    const val REQUEST_CODE_AUDIO_PERMISSION = 4567

    // File Size Limits
    const val MAX_IMAGE_SIZE = 1024 * 1024 * 5 // 5MB
    const val MAX_VOICE_SIZE = 1024 * 1024 * 10 // 10MB

    // Maximum Recording Duration (milliseconds)
    const val MAX_VOICE_RECORDING_DURATION = 5 * 60 * 1000 // 5 minutes

    // Timeouts
    const val TYPING_TIMEOUT = 5000L // 5 seconds

    // Preferences
    const val PREF_LOGGER_ENABLED = "pref_logger_enabled"
    const val PREF_DARK_MODE = "pref_dark_mode"
    const val PREF_USER_ID = "pref_user_id"
    const val PREF_DEVICE_ID = "device_id"
    const val PREF_NOTIFICATION_TOKEN = "pref_notification_token"

    // Cache
    const val CACHE_DIR_VOICE = "voice_messages"
    const val CACHE_DIR_IMAGES = "image_cache"

    // File MIME Types
    const val MIME_TYPE_AUDIO = "audio/m4a"
    const val MIME_TYPE_IMAGE = "image/jpeg"
}