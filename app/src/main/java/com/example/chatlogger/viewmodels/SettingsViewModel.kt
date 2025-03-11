// File: app/src/main/java/com/example/chatlogger/viewmodels/SettingsViewModel.kt
package com.example.chatlogger.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.chatlogger.models.LoggedMessage
import com.example.chatlogger.repositories.LoggerRepository
import com.example.chatlogger.utils.PermissionUtils
import com.example.chatlogger.utils.PreferenceUtils
import kotlinx.coroutines.launch

class SettingsViewModel(application: Application) : AndroidViewModel(application) {
    private val loggerRepository = LoggerRepository(application)

    // LiveData for logger state
    private val _loggerEnabled = MutableLiveData<Boolean>()
    val loggerEnabled: LiveData<Boolean> = _loggerEnabled

    // LiveData for dark mode state
    private val _darkModeEnabled = MutableLiveData<Boolean>()
    val darkModeEnabled: LiveData<Boolean> = _darkModeEnabled

    // LiveData for logged messages
    private val _loggedMessages = MutableLiveData<List<LoggedMessage>>()
    val loggedMessages: LiveData<List<LoggedMessage>> = _loggedMessages

    // LiveData for message statistics
    private val _messageStats = MutableLiveData<Map<String, Any>>()
    val messageStats: LiveData<Map<String, Any>> = _messageStats

    // Loading state
    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    // Error state
    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?> = _errorMessage

    // Initialize with current preferences
    init {
        _loggerEnabled.value = PreferenceUtils.isLoggerEnabled()
        _darkModeEnabled.value = PreferenceUtils.isDarkModeEnabled()
    }

    // Toggle message logger
    fun toggleLogger(enabled: Boolean) {
        if (enabled) {
            loggerRepository.startLoggerService()
        } else {
            loggerRepository.stopLoggerService()
        }

        _loggerEnabled.value = enabled
    }

    // Check if all required permissions are granted
    fun hasLoggerPermissions(): Boolean {
        return loggerRepository.hasLoggerPermissions()
    }

    // Toggle dark mode
    fun toggleDarkMode(enabled: Boolean) {
        PreferenceUtils.setDarkModeEnabled(enabled)
        _darkModeEnabled.value = enabled
    }

    // Load logged messages
    fun loadLoggedMessages() {
        _isLoading.value = true

        viewModelScope.launch {
            loggerRepository.getLoggedMessages().fold(
                onSuccess = { messages ->
                    _loggedMessages.value = messages
                    _isLoading.value = false
                },
                onFailure = { error ->
                    _errorMessage.value = error.message ?: "Failed to load logged messages"
                    _isLoading.value = false
                }
            )
        }
    }

    // Delete a single logged message
    fun deleteLoggedMessage(messageId: String, callback: (Boolean) -> Unit) {
        viewModelScope.launch {
            loggerRepository.deleteLoggedMessage(messageId).fold(
                onSuccess = {
                    // Update local list
                    _loggedMessages.value = _loggedMessages.value?.filter { it.id != messageId }
                    callback(true)
                },
                onFailure = { error ->
                    _errorMessage.value = error.message ?: "Failed to delete message"
                    callback(false)
                }
            )
        }
    }

    // Clear all logged messages
    fun clearAllLoggedMessages(callback: (Boolean) -> Unit) {
        _isLoading.value = true

        viewModelScope.launch {
            loggerRepository.clearAllLoggedMessages().fold(
                onSuccess = {
                    _loggedMessages.value = emptyList()
                    _isLoading.value = false
                    callback(true)
                },
                onFailure = { error ->
                    _errorMessage.value = error.message ?: "Failed to clear messages"
                    _isLoading.value = false
                    callback(false)
                }
            )
        }
    }

    // Load message statistics
    fun loadMessageStatistics() {
        _isLoading.value = true

        viewModelScope.launch {
            loggerRepository.getMessageStatistics().fold(
                onSuccess = { stats ->
                    _messageStats.value = stats
                    _isLoading.value = false
                },
                onFailure = { error ->
                    _errorMessage.value = error.message ?: "Failed to load statistics"
                    _isLoading.value = false
                }
            )
        }
    }

    // Clear error message
    fun clearError() {
        _errorMessage.value = null
    }
}