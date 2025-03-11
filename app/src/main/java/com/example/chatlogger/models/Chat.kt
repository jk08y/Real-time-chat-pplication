// File: app/src/main/java/com/example/chatlogger/models/Chat.kt
package com.example.chatlogger.models

import android.os.Parcelable
import com.google.firebase.firestore.DocumentId
import kotlinx.parcelize.Parcelize
import java.util.Date

@Parcelize
data class Chat(
    @DocumentId
    val id: String = "",
    val participants: List<String> = emptyList(),
    val lastMessage: String? = null,
    val lastMessageSenderId: String? = null,
    val lastMessageTimestamp: Date = Date(),
    val lastMessageType: String = "TEXT",
    val unreadCount: Map<String, Int> = emptyMap(),
    val typing: List<String> = emptyList(),
    val createdAt: Date = Date(),
    val updatedAt: Date = Date()
) : Parcelable {
    fun toMap(): Map<String, Any?> {
        return mapOf(
            "participants" to participants,
            "lastMessage" to lastMessage,
            "lastMessageSenderId" to lastMessageSenderId,
            "lastMessageTimestamp" to lastMessageTimestamp,
            "lastMessageType" to lastMessageType,
            "unreadCount" to unreadCount,
            "typing" to typing,
            "createdAt" to createdAt,
            "updatedAt" to updatedAt
        )
    }

    fun getOtherParticipantId(currentUserId: String): String {
        return participants.firstOrNull { it != currentUserId } ?: ""
    }
}