// File: app/src/main/java/com/example/chatlogger/repositories/AuthRepository.kt
package com.example.chatlogger.repositories

import android.net.Uri
import android.util.Log
import com.example.chatlogger.models.User
import com.example.chatlogger.utils.Constants
import com.example.chatlogger.utils.PreferenceUtils
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.tasks.await
import java.util.Date
import java.util.UUID

class AuthRepository {
    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()
    private val storage = FirebaseStorage.getInstance()
    private val TAG = "AuthRepository"

    // Check if user is logged in
    fun isUserLoggedIn(): Boolean {
        return auth.currentUser != null
    }

    // Get current user ID
    fun getCurrentUserId(): String {
        return auth.currentUser?.uid ?: ""
    }

    // Register a new user
    suspend fun registerUser(email: String, password: String, displayName: String): Result<FirebaseUser> {
        return try {
            val authResult = auth.createUserWithEmailAndPassword(email, password).await()
            val firebaseUser = authResult.user ?: throw Exception("User registration failed")

            // Update user profile
            val profileUpdates = UserProfileChangeRequest.Builder()
                .setDisplayName(displayName)
                .build()

            firebaseUser.updateProfile(profileUpdates).await()

            // Create user document in Firestore
            createUserInFirestore(firebaseUser, displayName)

            // Save user ID in preferences
            PreferenceUtils.setUserId(firebaseUser.uid)

            Result.success(firebaseUser)
        } catch (e: Exception) {
            Log.e(TAG, "Error registering user", e)
            Result.failure(e)
        }
    }

    // Login existing user
    suspend fun loginUser(email: String, password: String): Result<FirebaseUser> {
        return try {
            val authResult = auth.signInWithEmailAndPassword(email, password).await()
            val firebaseUser = authResult.user ?: throw Exception("Login failed")

            // Save user ID in preferences
            PreferenceUtils.setUserId(firebaseUser.uid)

            // Update user online status and FCM token
            updateUserOnlineStatus(true)

            Result.success(firebaseUser)
        } catch (e: Exception) {
            Log.e(TAG, "Error logging in user", e)
            Result.failure(e)
        }
    }

    // Create user document in Firestore
    private suspend fun createUserInFirestore(firebaseUser: FirebaseUser, displayName: String) {
        try {
            val fcmToken = try {
                FirebaseMessaging.getInstance().token.await()
            } catch (e: Exception) {
                ""
            }

            val user = User(
                id = firebaseUser.uid,
                email = firebaseUser.email ?: "",
                displayName = displayName,
                photoUrl = firebaseUser.photoUrl?.toString() ?: "",
                phoneNumber = firebaseUser.phoneNumber ?: "",
                status = "Hey, I'm using ChatLogger",
                lastSeen = Date(),
                isOnline = true,
                createdAt = Date(),
                updatedAt = Date(),
                fcmToken = fcmToken
            )

            firestore.collection(Constants.COLLECTION_USERS)
                .document(firebaseUser.uid)
                .set(user.toMap())
                .await()

            Log.d(TAG, "User document created in Firestore")
        } catch (e: Exception) {
            Log.e(TAG, "Error creating user document", e)
            throw e
        }
    }

    // Logout user
    suspend fun logoutUser(): Result<Unit> {
        return try {
            // Update online status before logging out
            updateUserOnlineStatus(false)

            // Clear user ID from preferences
            PreferenceUtils.setUserId("")

            // Sign out from Firebase
            auth.signOut()

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error logging out user", e)
            Result.failure(e)
        }
    }

    // Update user profile
    suspend fun updateUserProfile(
        displayName: String? = null,
        status: String? = null,
        photoUri: Uri? = null
    ): Result<User> {
        val user = auth.currentUser ?: return Result.failure(Exception("User not logged in"))

        return try {
            val updates = mutableMapOf<String, Any>()
            var photoUrl = user.photoUrl?.toString() ?: ""

            // Update photo if provided
            if (photoUri != null) {
                photoUrl = uploadProfileImage(photoUri)
                updates["photoUrl"] = photoUrl

                // Update Auth profile with new photo
                val profileUpdates = UserProfileChangeRequest.Builder()
                    .setPhotoUri(Uri.parse(photoUrl))
                    .build()
                user.updateProfile(profileUpdates).await()
            }

            // Update display name if provided
            if (!displayName.isNullOrBlank() && displayName != user.displayName) {
                updates["displayName"] = displayName

                // Update Auth profile with new name
                val profileUpdates = UserProfileChangeRequest.Builder()
                    .setDisplayName(displayName)
                    .build()
                user.updateProfile(profileUpdates).await()
            }

            // Update status if provided
            if (!status.isNullOrBlank()) {
                updates["status"] = status
            }

            // Add updated timestamp
            updates["updatedAt"] = Date()

            if (updates.isNotEmpty()) {
                // Update Firestore document
                firestore.collection(Constants.COLLECTION_USERS)
                    .document(user.uid)
                    .update(updates)
                    .await()
            }

            // Get updated user data
            val userDoc = firestore.collection(Constants.COLLECTION_USERS)
                .document(user.uid)
                .get()
                .await()

            val userData = userDoc.data
            if (userData != null) {
                val updatedUser = User(
                    id = user.uid,
                    email = userData["email"] as? String ?: "",
                    displayName = userData["displayName"] as? String ?: "",
                    photoUrl = userData["photoUrl"] as? String ?: "",
                    phoneNumber = userData["phoneNumber"] as? String ?: "",
                    status = userData["status"] as? String ?: "",
                    lastSeen = userData["lastSeen"] as? Date ?: Date(),
                    isOnline = userData["isOnline"] as? Boolean ?: false,
                    createdAt = userData["createdAt"] as? Date ?: Date(),
                    updatedAt = userData["updatedAt"] as? Date ?: Date(),
                    fcmToken = userData["fcmToken"] as? String ?: ""
                )

                Result.success(updatedUser)
            } else {
                Result.failure(Exception("Failed to retrieve updated user data"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error updating user profile", e)
            Result.failure(e)
        }
    }

    // Upload profile image to Firebase Storage
    private suspend fun uploadProfileImage(imageUri: Uri): String {
        val user = auth.currentUser ?: throw Exception("User not logged in")

        val fileName = "profile_${user.uid}_${UUID.randomUUID()}.jpg"
        val storageRef = storage.reference.child("profile_images/$fileName")

        return try {
            storageRef.putFile(imageUri).await()
            val downloadUrl = storageRef.downloadUrl.await()
            downloadUrl.toString()
        } catch (e: Exception) {
            Log.e(TAG, "Error uploading profile image", e)
            throw e
        }
    }

    // Update user online status
    suspend fun updateUserOnlineStatus(isOnline: Boolean) {
        val user = auth.currentUser ?: return

        try {
            val updates = mapOf(
                "isOnline" to isOnline,
                "lastSeen" to Date()
            )

            firestore.collection(Constants.COLLECTION_USERS)
                .document(user.uid)
                .update(updates)
                .await()
        } catch (e: Exception) {
            Log.e(TAG, "Error updating online status", e)
        }
    }

    // Get user by ID
    suspend fun getUserById(userId: String): Result<User> {
        return try {
            val documentSnapshot = firestore.collection(Constants.COLLECTION_USERS)
                .document(userId)
                .get()
                .await()

            if (documentSnapshot.exists()) {
                val data = documentSnapshot.data ?: return Result.failure(Exception("User data is null"))

                val user = User(
                    id = userId,
                    email = data["email"] as? String ?: "",
                    displayName = data["displayName"] as? String ?: "",
                    photoUrl = data["photoUrl"] as? String ?: "",
                    phoneNumber = data["phoneNumber"] as? String ?: "",
                    status = data["status"] as? String ?: "",
                    lastSeen = data["lastSeen"] as? Date ?: Date(),
                    isOnline = data["isOnline"] as? Boolean ?: false,
                    createdAt = data["createdAt"] as? Date ?: Date(),
                    updatedAt = data["updatedAt"] as? Date ?: Date(),
                    fcmToken = data["fcmToken"] as? String ?: ""
                )

                Result.success(user)
            } else {
                Result.failure(Exception("User not found"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting user by ID", e)
            Result.failure(e)
        }
    }

    // Reset password
    suspend fun resetPassword(email: String): Result<Unit> {
        return try {
            auth.sendPasswordResetEmail(email).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error resetting password", e)
            Result.failure(e)
        }
    }
}