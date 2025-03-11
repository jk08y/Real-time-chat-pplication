// File: app/src/main/java/com/example/chatlogger/viewmodels/ChatViewModel.kt
package com.example.chatlogger.viewmodels

import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.chatlogger.models.Chat
import com.example.chatlogger.models.Message
import com.example.chatlogger.repositories.ChatRepository
import kotlinx.coroutines.launch

class ChatViewModel : ViewModel() {
    private val chatRepository = ChatRepository()

    // LiveData for chats list
    private val _chats = MutableLiveData<List<Chat>>()
    val chats: LiveData<List<Chat>> = _chats

    // LiveData for current chat
    private val _currentChat = MutableLiveData<Chat?>()
    val currentChat: LiveData<Chat?> = _currentChat

    // LiveData for messages
    private val _messages = MutableLiveData<List<Message>>()
    val messages: LiveData<List<Message>> = _messages

    // Loading state
    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    // Error state
    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?> = _errorMessage

    // Get all chats
    fun loadChats() {
        _isLoading.value = true

        viewModelScope.launch {
            chatRepository.getChats().fold(
                onSuccess = { chatsList ->
                    _chats.value = chatsList
                    _isLoading.value = false
                },
                onFailure = { error ->
                    _errorMessage.value = error.message ?: "Failed to load chats"
                    _isLoading.value = false
                }
            )
        }
    }

    // Get or create a chat with a user
    fun getOrCreateChat(otherUserId: String, callback: (Chat) -> Unit) {
        _isLoading.value = true

        viewModelScope.launch {
            chatRepository.getOrCreateChat(otherUserId).fold(
                onSuccess = { chat ->
                    _currentChat.value = chat
                    _isLoading.value = false
                    callback(chat)
                },
                onFailure = { error ->
                    _errorMessage.value = error.message ?: "Failed to create chat"
                    _isLoading.value = false
                }
            )
        }
    }

    // Get chat by ID
    fun getChatById(chatId: String) {
        _isLoading.value = true

        viewModelScope.launch {
            chatRepository.getChatById(chatId).fold(
                onSuccess = { chat ->
                    _currentChat.value = chat
                    _isLoading.value = false
                },
                onFailure = { error ->
                    _errorMessage.value = error.message ?: "Failed to load chat"
                    _isLoading.value = false
                }
            )
        }
    }

    // Get messages for current chat
    fun loadMessages(chatId: String) {
        _isLoading.value = true

        viewModelScope.launch {
            chatRepository.getMessages(chatId).fold(
                onSuccess = { messagesList ->
                    _messages.value = messagesList.sortedBy { it.timestamp }
                    _isLoading.value = false

                    // Mark messages as read
                    markMessagesAsRead(chatId)
                },
                onFailure = { error ->
                    _errorMessage.value = error.message ?: "Failed to load messages"
                    _isLoading.value = false
                }
            )
        }
    }

    // Send a text message
    fun sendTextMessage(chatId: String, text: String, callback: (Boolean) -> Unit) {
        if (text.isBlank()) {
            callback(false)
            return
        }

        viewModelScope.launch {
            chatRepository.sendTextMessage(chatId, text).fold(
                onSuccess = { message ->
                    // Add to the messages list if available
                    _messages.value?.let { currentMessages ->
                        _messages.value = currentMessages + message
                    }
                    callback(true)
                },
                onFailure = { error ->
                    _errorMessage.value = error.message ?: "Failed to send message"
                    callback(false)
                }
            )
        }
    }

    // Send an image message
    fun sendImageMessage(chatId: String, imageUri: Uri, callback: (Boolean) -> Unit) {
        viewModelScope.launch {
            chatRepository.sendImageMessage(chatId, imageUri).fold(
                onSuccess = { message ->
                    // Add to the messages list if available
                    _messages.value?.let { currentMessages ->
                        _messages.value = currentMessages + message
                    }
                    callback(true)
                },
                onFailure = { error ->
                    _errorMessage.value = error.message ?: "Failed to send image"
                    callback(false)
                }
            )
        }
    }

    // Send a voice message
    fun sendVoiceMessage(chatId: String, audioUri: Uri, duration: Long, callback: (Boolean) -> Unit) {
        viewModelScope.launch {
            chatRepository.sendVoiceMessage(chatId, audioUri, duration).fold(
                onSuccess = { message ->
                    // Add to the messages list if available
                    _messages.value?.let { currentMessages ->
                        _messages.value = currentMessages + message
                    }
                    callback(true)
                },
                onFailure = { error ->
                    _errorMessage.value = error.message ?: "Failed to send voice message"
                    callback(false)
                }
            )
        }
    }

    // Mark messages as read
    fun markMessagesAsRead(chatId: String) {
        viewModelScope.launch {
            chatRepository.markMessagesAsRead(chatId).fold(
                onSuccess = {
                    // Update local chat data if needed
                    _currentChat.value?.let { chat ->
                        val currentUserId = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid
                        if (currentUserId != null) {
                            val updatedUnreadCount = chat.unreadCount.toMutableMap().apply {
                                this[currentUserId] = 0
                            }
                            _currentChat.value = chat.copy(unreadCount = updatedUnreadCount)
                        }
                    }
                },
                onFailure = { error ->
                    android.util.Log.e("ChatViewModel", "Error marking messages as read", error)
                }
            )
        }
    }

    // Update typing status
    fun updateTypingStatus(chatId: String, isTyping: Boolean) {
        viewModelScope.launch {
            chatRepository.updateTypingStatus(chatId, isTyping)
        }
    }

    // Delete a message
    fun deleteMessage(messageId: String, callback: (Boolean) -> Unit) {
        viewModelScope.launch {
            chatRepository.deleteMessage(messageId).fold(
                onSuccess = {
                    // Update local messages list
                    _messages.value?.let { currentMessages ->
                        _messages.value = currentMessages.map { message ->
                            if (message.id == messageId) {
                                message.copy(isDeleted = true)
                            } else {
                                message
                            }
                        }
                    }
                    callback(true)
                },
                onFailure = { error ->
                    _errorMessage.value = error.message ?: "Failed to delete message"
                    callback(false)
                }
            )
        }
    }

    // Clear error message
    fun clearError() {
        _errorMessage.value = null
    }
}