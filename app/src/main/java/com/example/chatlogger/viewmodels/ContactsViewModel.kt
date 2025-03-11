// File: app/src/main/java/com/example/chatlogger/viewmodels/ContactsViewModel.kt
package com.example.chatlogger.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.chatlogger.models.User
import com.example.chatlogger.repositories.ContactsRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class ContactsViewModel(application: Application) : AndroidViewModel(application) {
    private val contactsRepository = ContactsRepository(application)

    // LiveData for contacts list
    private val _contacts = MutableLiveData<List<User>>()
    val contacts: LiveData<List<User>> = _contacts

    // LiveData for search results
    private val _searchResults = MutableLiveData<List<User>>()
    val searchResults: LiveData<List<User>> = _searchResults

    // Loading state
    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    // Error state
    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?> = _errorMessage

    // For search debounce
    private var searchJob: Job? = null

    // Load all registered contacts
    fun loadContacts() {
        _isLoading.value = true

        viewModelScope.launch {
            contactsRepository.getRegisteredContacts().fold(
                onSuccess = { contactsList ->
                    _contacts.value = contactsList
                    _isLoading.value = false
                },
                onFailure = { error ->
                    _errorMessage.value = error.message ?: "Failed to load contacts"
                    _isLoading.value = false
                }
            )
        }
    }

    // Search for users with debounce
    fun searchUsers(query: String) {
        searchJob?.cancel()

        if (query.length < 3) {
            _searchResults.value = emptyList()
            return
        }

        searchJob = viewModelScope.launch {
            // Add delay for debounce
            delay(300)

            _isLoading.value = true

            contactsRepository.searchUsers(query).fold(
                onSuccess = { results ->
                    _searchResults.value = results
                    _isLoading.value = false
                },
                onFailure = { error ->
                    _errorMessage.value = error.message ?: "Search failed"
                    _isLoading.value = false
                }
            )
        }
    }

    // Update phone number
    fun updatePhoneNumber(phoneNumber: String, callback: (Boolean) -> Unit) {
        viewModelScope.launch {
            contactsRepository.updatePhoneNumber(phoneNumber).fold(
                onSuccess = {
                    callback(true)
                },
                onFailure = { error ->
                    _errorMessage.value = error.message ?: "Failed to update phone number"
                    callback(false)
                }
            )
        }
    }

    // Clear search results
    fun clearSearch() {
        searchJob?.cancel()
        _searchResults.value = emptyList()
    }

    // Clear error message
    fun clearError() {
        _errorMessage.value = null
    }
}