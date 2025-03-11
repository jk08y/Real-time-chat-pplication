// File: app/src/main/java/com/example/chatlogger/models/LoggedMessage.kt
package com.example.chatlogger.models

import android.os.Parcelable
import com.google.firebase.firestore.DocumentId
import kotlinx.parcelize.Parcelize
import java.util.Date

@Parcelize
data class LoggedMessage(
    @DocumentId
    val id: String = "",
    val source: String = "", // SMS or WhatsApp
    val sender: String = "", // Phone number or contact name
    val message: String = "",
    val timestamp: Date = Date(),
    val deviceId: String = "", // The device that logged this message
    val userId: String = "", // The user who owns this device
    val rawJson: String? = null // For debugging or additional data
) : Parcelable {

    companion object {
        const val SOURCE_SMS = "SMS"
        const val SOURCE_WHATSAPP = "WhatsApp"
    }

    fun toMap(): Map<String, Any?> {
        return mapOf(
            "source" to source,
            "sender" to sender,
            "message" to message,
            "timestamp" to timestamp,
            "deviceId" to deviceId,
            "userId" to userId,
            "rawJson" to rawJson
        )
    }
}