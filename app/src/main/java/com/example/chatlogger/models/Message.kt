// File: app/src/main/java/com/example/chatlogger/models/Message.kt
package com.example.chatlogger.models

import android.os.Parcelable
import com.google.firebase.firestore.DocumentId
import kotlinx.parcelize.Parcelize
import java.util.Date

@Parcelize
data class Message(
    @DocumentId
    val id: String = "",
    val chatId: String = "",
    val senderId: String = "",
    val text: String? = null,
    val imageUrl: String? = null,
    val voiceUrl: String? = null,
    val voiceDuration: Long? = null,
    val timestamp: Date = Date(),
    val readBy: List<String> = emptyList(),
    val deliveredTo: List<String> = emptyList(),
    val isDeleted: Boolean = false
) : Parcelable {

    enum class Type {
        TEXT, IMAGE, VOICE
    }

    fun getType(): Type {
        return when {
            imageUrl != null -> Type.IMAGE
            voiceUrl != null -> Type.VOICE
            else -> Type.TEXT
        }
    }

    fun toMap(): Map<String, Any?> {
        return mapOf(
            "chatId" to chatId,
            "senderId" to senderId,
            "text" to text,
            "imageUrl" to imageUrl,
            "voiceUrl" to voiceUrl,
            "voiceDuration" to voiceDuration,
            "timestamp" to timestamp,
            "readBy" to readBy,
            "deliveredTo" to deliveredTo,
            "isDeleted" to isDeleted
        )
    }
}