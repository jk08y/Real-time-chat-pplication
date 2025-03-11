// File: app/src/main/java/com/example/chatlogger/viewmodels/AuthViewModel.kt
package com.example.chatlogger.viewmodels

import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.chatlogger.models.User
import com.example.chatlogger.repositories.AuthRepository
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.launch

class AuthViewModel : ViewModel() {
    private val authRepository = AuthRepository()

    // LiveData for authentication state
    private val _authState = MutableLiveData<AuthState>()
    val authState: LiveData<AuthState> = _authState

    // LiveData for user profile
    private val _userProfile = MutableLiveData<User?>()
    val userProfile: LiveData<User?> = _userProfile

    // Data classes for auth states
    sealed class AuthState {
        object Loading : AuthState()
        data class Authenticated(val user: FirebaseUser) : AuthState()
        object Unauthenticated : AuthState()
        data class Error(val message: String) : AuthState()
    }

    init {
        checkAuthState()
    }

    // Check current authentication state
    fun checkAuthState() {
        _authState.value = AuthState.Loading

        if (authRepository.isUserLoggedIn()) {
            val currentUser = authRepository.getCurrentUserId()

            viewModelScope.launch {
                // Retrieve user profile from Firestore
                authRepository.getUserById(currentUser).fold(
                    onSuccess = { user ->
                        _userProfile.value = user
                        _authState.value = AuthState.Authenticated(
                            com.google.firebase.auth.FirebaseAuth.getInstance().currentUser!!
                        )
                    },
                    onFailure = { error ->
                        _authState.value = AuthState.Error(error.message ?: "Error loading user profile")
                    }
                )
            }
        } else {
            _authState.value = AuthState.Unauthenticated
        }
    }

    // Register a new user
    fun registerUser(email: String, password: String, displayName: String) {
        _authState.value = AuthState.Loading

        viewModelScope.launch {
            authRepository.registerUser(email, password, displayName).fold(
                onSuccess = { user ->
                    _authState.value = AuthState.Authenticated(user)
                    getUserProfile(user.uid)
                },
                onFailure = { error ->
                    _authState.value = AuthState.Error(error.message ?: "Registration failed")
                }
            )
        }
    }

    // Login existing user
    fun loginUser(email: String, password: String) {
        _authState.value = AuthState.Loading

        viewModelScope.launch {
            authRepository.loginUser(email, password).fold(
                onSuccess = { user ->
                    _authState.value = AuthState.Authenticated(user)
                    getUserProfile(user.uid)
                },
                onFailure = { error ->
                    _authState.value = AuthState.Error(error.message ?: "Login failed")
                }
            )
        }
    }

    // Logout user
    fun logout() {
        _authState.value = AuthState.Loading

        viewModelScope.launch {
            authRepository.logoutUser().fold(
                onSuccess = {
                    _userProfile.value = null
                    _authState.value = AuthState.Unauthenticated
                },
                onFailure = { error ->
                    _authState.value = AuthState.Error(error.message ?: "Logout failed")
                }
            )
        }
    }

    // Reset password
    fun resetPassword(email: String, callback: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            authRepository.resetPassword(email).fold(
                onSuccess = {
                    callback(true, null)
                },
                onFailure = { error ->
                    callback(false, error.message)
                }
            )
        }
    }

    // Get user profile
    private fun getUserProfile(userId: String) {
        viewModelScope.launch {
            authRepository.getUserById(userId).fold(
                onSuccess = { user ->
                    _userProfile.value = user
                },
                onFailure = { error ->
                    // Just log this error, don't update auth state
                    android.util.Log.e("AuthViewModel", "Error loading user profile", error)
                }
            )
        }
    }

    // Update user profile
    fun updateUserProfile(displayName: String? = null, status: String? = null, photoUri: Uri? = null) {
        viewModelScope.launch {
            authRepository.updateUserProfile(displayName, status, photoUri).fold(
                onSuccess = { user ->
                    _userProfile.value = user
                },
                onFailure = { error ->
                    // Handle error (could add specific LiveData for this)
                    android.util.Log.e("AuthViewModel", "Error updating profile", error)
                }
            )
        }
    }

    // Update online status
    fun updateOnlineStatus(isOnline: Boolean) {
        viewModelScope.launch {
            authRepository.updateUserOnlineStatus(isOnline)
        }
    }
}