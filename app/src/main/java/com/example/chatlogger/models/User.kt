// File: app/src/main/java/com/example/chatlogger/models/User.kt
package com.example.chatlogger.models

import android.os.Parcelable
import com.google.firebase.firestore.DocumentId
import kotlinx.parcelize.Parcelize
import java.util.Date

@Parcelize
data class User(
    @DocumentId
    val id: String = "",
    val email: String = "",
    val displayName: String = "",
    val photoUrl: String = "",
    val phoneNumber: String = "",
    val status: String = "Hey, I'm using ChatLogger",
    val lastSeen: Date = Date(),
    val isOnline: Boolean = false,
    val createdAt: Date = Date(),
    val updatedAt: Date = Date(),
    val fcmToken: String = ""
) : Parcelable {
    fun toMap(): Map<String, Any?> {
        return mapOf(
            "email" to email,
            "displayName" to displayName,
            "photoUrl" to photoUrl,
            "phoneNumber" to phoneNumber,
            "status" to status,
            "lastSeen" to lastSeen,
            "isOnline" to isOnline,
            "createdAt" to createdAt,
            "updatedAt" to updatedAt,
            "fcmToken" to fcmToken
        )
    }
}