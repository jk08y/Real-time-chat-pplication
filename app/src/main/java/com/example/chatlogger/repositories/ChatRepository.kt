// File: app/src/main/java/com/example/chatlogger/repositories/ChatRepository.kt
package com.example.chatlogger.repositories

import android.net.Uri
import android.util.Log
import com.example.chatlogger.models.Chat
import com.example.chatlogger.models.Message
import com.example.chatlogger.utils.Constants
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.tasks.await
import java.util.Date
import java.util.UUID

class ChatRepository {
    private val firestore = FirebaseFirestore.getInstance()
    private val storage = FirebaseStorage.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val TAG = "ChatRepository"

    // Get current user ID
    private fun getCurrentUserId(): String {
        return auth.currentUser?.uid ?: throw IllegalStateException("User not logged in")
    }

    // Get all chats for current user
    suspend fun getChats(): Result<List<Chat>> {
        val userId = getCurrentUserId()

        return try {
            val querySnapshot = firestore.collection(Constants.COLLECTION_CHATS)
                .whereArrayContains("participants", userId)
                .orderBy("lastMessageTimestamp", Query.Direction.DESCENDING)
                .get()
                .await()

            val chats = querySnapshot.documents.mapNotNull { document ->
                try {
                    val data = document.data ?: return@mapNotNull null

                    Chat(
                        id = document.id,
                        participants = (data["participants"] as? List<*>)?.filterIsInstance<String>() ?: emptyList(),
                        lastMessage = data["lastMessage"] as? String,
                        lastMessageSenderId = data["lastMessageSenderId"] as? String,
                        lastMessageTimestamp = data["lastMessageTimestamp"] as? Date ?: Date(),
                        lastMessageType = data["lastMessageType"] as? String ?: "TEXT",
                        unreadCount = (data["unreadCount"] as? Map<*, *>)?.mapNotNull {
                            it.key.toString() to (it.value as? Long)?.toInt() ?: 0
                        }?.toMap() ?: emptyMap(),
                        typing = (data["typing"] as? List<*>)?.filterIsInstance<String>() ?: emptyList(),
                        createdAt = data["createdAt"] as? Date ?: Date(),
                        updatedAt = data["updatedAt"] as? Date ?: Date()
                    )
                } catch (e: Exception) {
                    Log.e(TAG, "Error parsing chat document", e)
                    null
                }
            }

            Result.success(chats)
        } catch (e: Exception) {
            Log.e(TAG, "Error getting chats", e)
            Result.failure(e)
        }
    }

    // Get chat by ID
    suspend fun getChatById(chatId: String): Result<Chat> {
        return try {
            val document = firestore.collection(Constants.COLLECTION_CHATS)
                .document(chatId)
                .get()
                .await()

            if (document.exists()) {
                val data = document.data ?: return Result.failure(Exception("Chat data is null"))

                val chat = Chat(
                    id = document.id,
                    participants = (data["participants"] as? List<*>)?.filterIsInstance<String>() ?: emptyList(),
                    lastMessage = data["lastMessage"] as? String,
                    lastMessageSenderId = data["lastMessageSenderId"] as? String,
                    lastMessageTimestamp = data["lastMessageTimestamp"] as? Date ?: Date(),
                    lastMessageType = data["lastMessageType"] as? String ?: "TEXT",
                    unreadCount = (data["unreadCount"] as? Map<*, *>)?.mapNotNull {
                        it.key.toString() to (it.value as? Long)?.toInt() ?: 0
                    }?.toMap() ?: emptyMap(),
                    typing = (data["typing"] as? List<*>)?.filterIsInstance<String>() ?: emptyList(),
                    createdAt = data["createdAt"] as? Date ?: Date(),
                    updatedAt = data["updatedAt"] as? Date ?: Date()
                )

                Result.success(chat)
            } else {
                Result.failure(Exception("Chat not found"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting chat by ID", e)
            Result.failure(e)
        }
    }

    // Get or create a chat with a user
    suspend fun getOrCreateChat(otherUserId: String): Result<Chat> {
        val currentUserId = getCurrentUserId()

        return try {
            // Check if a chat already exists between these users
            val querySnapshot = firestore.collection(Constants.COLLECTION_CHATS)
                .whereArrayContains("participants", currentUserId)
                .get()
                .await()

            // Find chat with just these two participants
            val existingChat = querySnapshot.documents.find { document ->
                val participants = document.get("participants") as? List<*>
                participants?.size == 2 && participants.contains(otherUserId) && participants.contains(currentUserId)
            }

            if (existingChat != null) {
                // Chat exists, return it
                val data = existingChat.data ?: return Result.failure(Exception("Chat data is null"))

                val chat = Chat(
                    id = existingChat.id,
                    participants = (data["participants"] as? List<*>)?.filterIsInstance<String>() ?: emptyList(),
                    lastMessage = data["lastMessage"] as? String,
                    lastMessageSenderId = data["lastMessageSenderId"] as? String,
                    lastMessageTimestamp = data["lastMessageTimestamp"] as? Date ?: Date(),
                    lastMessageType = data["lastMessageType"] as? String ?: "TEXT",
                    unreadCount = (data["unreadCount"] as? Map<*, *>)?.mapNotNull {
                        it.key.toString() to (it.value as? Long)?.toInt() ?: 0
                    }?.toMap() ?: emptyMap(),
                    typing = (data["typing"] as? List<*>)?.filterIsInstance<String>() ?: emptyList(),
                    createdAt = data["createdAt"] as? Date ?: Date(),
                    updatedAt = data["updatedAt"] as? Date ?: Date()
                )

                return Result.success(chat)
            } else {
                // Create a new chat
                val now = Date()
                val unreadCount = mapOf(
                    currentUserId to 0,
                    otherUserId to 0
                )

                val newChat = Chat(
                    participants = listOf(currentUserId, otherUserId),
                    unreadCount = unreadCount,
                    createdAt = now,
                    updatedAt = now
                )

                val documentRef = firestore.collection(Constants.COLLECTION_CHATS)
                    .add(newChat.toMap())
                    .await()

                val createdChat = newChat.copy(id = documentRef.id)

                Result.success(createdChat)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting or creating chat", e)
            Result.failure(e)
        }
    }

    // Send a text message
    suspend fun sendTextMessage(chatId: String, text: String): Result<Message> {
        val userId = getCurrentUserId()

        return try {
            val message = Message(
                chatId = chatId,
                senderId = userId,
                text = text,
                timestamp = Date(),
                deliveredTo = listOf(userId)
            )

            // Save message to Firestore
            val messageRef = firestore.collection(Constants.COLLECTION_MESSAGES)
                .add(message.toMap())
                .await()

            // Update chat with last message info
            updateChatLastMessage(chatId, message, messageRef.id)

            // Increment unread count for other participants
            incrementUnreadCount(chatId, userId)

            val sentMessage = message.copy(id = messageRef.id)
            Result.success(sentMessage)
        } catch (e: Exception) {
            Log.e(TAG, "Error sending text message", e)
            Result.failure(e)
        }
    }

    // Send an image message
    suspend fun sendImageMessage(chatId: String, imageUri: Uri): Result<Message> {
        val userId = getCurrentUserId()

        return try {
            // Upload image to Firebase Storage
            val fileName = "chat_${UUID.randomUUID()}.jpg"
            val storageRef = storage.reference.child("chat_images/$fileName")

            storageRef.putFile(imageUri).await()
            val downloadUrl = storageRef.downloadUrl.await().toString()

            // Create message
            val message = Message(
                chatId = chatId,
                senderId = userId,
                imageUrl = downloadUrl,
                timestamp = Date(),
                deliveredTo = listOf(userId)
            )

            // Save message to Firestore
            val messageRef = firestore.collection(Constants.COLLECTION_MESSAGES)
                .add(message.toMap())
                .await()

            // Update chat with last message info
            updateChatLastMessage(chatId, message, messageRef.id)

            // Increment unread count for other participants
            incrementUnreadCount(chatId, userId)

            val sentMessage = message.copy(id = messageRef.id)
            Result.success(sentMessage)
        } catch (e: Exception) {
            Log.e(TAG, "Error sending image message", e)
            Result.failure(e)
        }
    }

    // Send a voice message
    suspend fun sendVoiceMessage(chatId: String, audioUri: Uri, duration: Long): Result<Message> {
        val userId = getCurrentUserId()

        return try {
            // Upload audio to Firebase Storage
            val fileName = "voice_${UUID.randomUUID()}.m4a"
            val storageRef = storage.reference.child("voice_messages/$fileName")

            storageRef.putFile(audioUri).await()
            val downloadUrl = storageRef.downloadUrl.await().toString()

            // Create message
            val message = Message(
                chatId = chatId,
                senderId = userId,
                voiceUrl = downloadUrl,
                voiceDuration = duration,
                timestamp = Date(),
                deliveredTo = listOf(userId)
            )

            // Save message to Firestore
            val messageRef = firestore.collection(Constants.COLLECTION_MESSAGES)
                .add(message.toMap())
                .await()

            // Update chat with last message info
            updateChatLastMessage(chatId, message, messageRef.id)

            // Increment unread count for other participants
            incrementUnreadCount(chatId, userId)

            val sentMessage = message.copy(id = messageRef.id)
            Result.success(sentMessage)
        } catch (e: Exception) {
            Log.e(TAG, "Error sending voice message", e)
            Result.failure(e)
        }
    }

    // Update chat's last message information
    private suspend fun updateChatLastMessage(chatId: String, message: Message, messageId: String) {
        val updates = mapOf(
            "lastMessage" to when (message.getType()) {
                Message.Type.TEXT -> message.text
                Message.Type.IMAGE -> "📷 Image"
                Message.Type.VOICE -> "🎤 Voice message"
            },
            "lastMessageSenderId" to message.senderId,
            "lastMessageTimestamp" to message.timestamp,
            "lastMessageType" to message.getType().toString(),
            "updatedAt" to Date()
        )

        firestore.collection(Constants.COLLECTION_CHATS)
            .document(chatId)
            .update(updates)
            .await()
    }

    // Increment unread count for other participants
    private suspend fun incrementUnreadCount(chatId: String, senderId: String) {
        val chatDoc = firestore.collection(Constants.COLLECTION_CHATS)
            .document(chatId)
            .get()
            .await()

        val participants = chatDoc.get("participants") as? List<*> ?: return

        for (participant in participants) {
            if (participant is String && participant != senderId) {
                firestore.collection(Constants.COLLECTION_CHATS)
                    .document(chatId)
                    .update("unreadCount.$participant", FieldValue.increment(1))
                    .await()
            }
        }
    }

    // Get messages for a chat
    suspend fun getMessages(chatId: String, limit: Long = 50): Result<List<Message>> {
        return try {
            val querySnapshot = firestore.collection(Constants.COLLECTION_MESSAGES)
                .whereEqualTo("chatId", chatId)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .limit(limit)
                .get()
                .await()

            val messages = querySnapshot.documents.mapNotNull { document ->
                try {
                    val data = document.data ?: return@mapNotNull null

                    Message(
                        id = document.id,
                        chatId = data["chatId"] as? String ?: "",
                        senderId = data["senderId"] as? String ?: "",
                        text = data["text"] as? String,
                        imageUrl = data["imageUrl"] as? String,
                        voiceUrl = data["voiceUrl"] as? String,
                        voiceDuration = data["voiceDuration"] as? Long,
                        timestamp = data["timestamp"] as? Date ?: Date(),
                        readBy = (data["readBy"] as? List<*>)?.filterIsInstance<String>() ?: emptyList(),
                        deliveredTo = (data["deliveredTo"] as? List<*>)?.filterIsInstance<String>() ?: emptyList(),
                        isDeleted = data["isDeleted"] as? Boolean ?: false
                    )
                } catch (e: Exception) {
                    Log.e(TAG, "Error parsing message document", e)
                    null
                }
            }

            Result.success(messages)
        } catch (e: Exception) {
            Log.e(TAG, "Error getting messages", e)
            Result.failure(e)
        }
    }

    // Mark messages as read
    suspend fun markMessagesAsRead(chatId: String): Result<Unit> {
        val userId = getCurrentUserId()

        return try {
            // Reset unread count for current user
            firestore.collection(Constants.COLLECTION_CHATS)
                .document(chatId)
                .update("unreadCount.$userId", 0)
                .await()

            // Mark unread messages as read
            val querySnapshot = firestore.collection(Constants.COLLECTION_MESSAGES)
                .whereEqualTo("chatId", chatId)
                .whereNotEqualTo("senderId", userId)
                .whereNotIn("readBy", listOf(userId))
                .get()
                .await()

            val batch = firestore.batch()

            for (document in querySnapshot.documents) {
                val messageRef = document.reference
                batch.update(messageRef, "readBy", FieldValue.arrayUnion(userId))
            }

            batch.commit().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error marking messages as read", e)
            Result.failure(e)
        }
    }

    // Update typing status
    suspend fun updateTypingStatus(chatId: String, isTyping: Boolean): Result<Unit> {
        val userId = getCurrentUserId()

        return try {
            val chatRef = firestore.collection(Constants.COLLECTION_CHATS).document(chatId)

            if (isTyping) {
                chatRef.update("typing", FieldValue.arrayUnion(userId)).await()
            } else {
                chatRef.update("typing", FieldValue.arrayRemove(userId)).await()
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error updating typing status", e)
            Result.failure(e)
        }
    }

    // Delete message
    suspend fun deleteMessage(messageId: String): Result<Unit> {
        return try {
            firestore.collection(Constants.COLLECTION_MESSAGES)
                .document(messageId)
                .update("isDeleted", true)
                .await()

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting message", e)
            Result.failure(e)
        }
    }
}