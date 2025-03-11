// File: app/src/main/java/com/example/chatlogger/repositories/ContactsRepository.kt
package com.example.chatlogger.repositories

import android.content.Context
import android.util.Log
import com.example.chatlogger.models.User
import com.example.chatlogger.utils.Constants
import com.example.chatlogger.utils.ContactsHelper
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class ContactsRepository(private val context: Context) {
    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val contactsHelper = ContactsHelper(context)
    private val TAG = "ContactsRepository"

    // Get current user ID
    private fun getCurrentUserId(): String {
        return auth.currentUser?.uid ?: throw IllegalStateException("User not logged in")
    }

    // Get all registered users from the app who are in the user's contacts
    suspend fun getRegisteredContacts(): Result<List<User>> = withContext(Dispatchers.IO) {
        try {
            // Get all local contacts
            val phoneContacts = contactsHelper.getAllContacts()
            if (phoneContacts.isEmpty()) {
                return@withContext Result.success(emptyList())
            }

            // Extract phone numbers
            val phoneNumbers = phoneContacts.map { contactsHelper.normalizePhoneNumber(it.phoneNumber) }
                .filter { it.isNotEmpty() }
                .distinct()

            if (phoneNumbers.isEmpty()) {
                return@withContext Result.success(emptyList())
            }

            // Query Firestore for users with these phone numbers
            val currentUserId = getCurrentUserId()

            // Split into batches of 10 for Firestore "in" query limit
            val allRegisteredUsers = mutableListOf<User>()

            for (i in phoneNumbers.indices step 10) {
                val batch = phoneNumbers.subList(i, minOf(i + 10, phoneNumbers.size))

                val querySnapshot = firestore.collection(Constants.COLLECTION_USERS)
                    .whereIn("phoneNumber", batch)
                    .get()
                    .await()

                val batchUsers = querySnapshot.documents.mapNotNull { document ->
                    try {
                        val userId = document.id
                        // Skip the current user
                        if (userId == currentUserId) return@mapNotNull null

                        val data = document.data ?: return@mapNotNull null

                        User(
                            id = userId,
                            email = data["email"] as? String ?: "",
                            displayName = data["displayName"] as? String ?: "",
                            photoUrl = data["photoUrl"] as? String ?: "",
                            phoneNumber = data["phoneNumber"] as? String ?: "",
                            status = data["status"] as? String ?: "",
                            lastSeen = data["lastSeen"] as? java.util.Date ?: java.util.Date(),
                            isOnline = data["isOnline"] as? Boolean ?: false,
                            createdAt = data["createdAt"] as? java.util.Date ?: java.util.Date(),
                            updatedAt = data["updatedAt"] as? java.util.Date ?: java.util.Date(),
                            fcmToken = data["fcmToken"] as? String ?: ""
                        )
                    } catch (e: Exception) {
                        Log.e(TAG, "Error parsing user document", e)
                        null
                    }
                }

                allRegisteredUsers.addAll(batchUsers)
            }

            // Match with contact names from phone
            val contactsMap = phoneContacts.associateBy { contactsHelper.normalizePhoneNumber(it.phoneNumber) }

            val usersWithContactNames = allRegisteredUsers.map { user ->
                val normalizedPhone = contactsHelper.normalizePhoneNumber(user.phoneNumber)
                val contact = contactsMap[normalizedPhone]

                if (contact != null) {
                    // If the user has a blank display name, use the contact name
                    if (user.displayName.isBlank()) {
                        user.copy(displayName = contact.name)
                    } else {
                        user
                    }
                } else {
                    user
                }
            }

            Result.success(usersWithContactNames.sortedBy { it.displayName })
        } catch (e: Exception) {
            Log.e(TAG, "Error getting registered contacts", e)
            Result.failure(e)
        }
    }

    // Search for users
    suspend fun searchUsers(query: String): Result<List<User>> {
        if (query.length < 3) {
            return Result.success(emptyList())
        }

        val currentUserId = getCurrentUserId()

        return try {
            // Search by display name (startsWith)
            val nameQuerySnapshot = firestore.collection(Constants.COLLECTION_USERS)
                .orderBy("displayName")
                .startAt(query)
                .endAt(query + "\uf8ff")
                .get()
                .await()

            // Search by phone number (contains)
            val phoneQuerySnapshot = firestore.collection(Constants.COLLECTION_USERS)
                .orderBy("phoneNumber")
                .startAt(query)
                .endAt(query + "\uf8ff")
                .get()
                .await()

            // Combine results
            val userIds = mutableSetOf<String>()
            val users = mutableListOf<User>()

            // Process name results
            for (document in nameQuerySnapshot.documents) {
                val userId = document.id
                if (userId == currentUserId || userIds.contains(userId)) continue

                val data = document.data ?: continue

                val user = User(
                    id = userId,
                    email = data["email"] as? String ?: "",
                    displayName = data["displayName"] as? String ?: "",
                    photoUrl = data["photoUrl"] as? String ?: "",
                    phoneNumber = data["phoneNumber"] as? String ?: "",
                    status = data["status"] as? String ?: "",
                    lastSeen = data["lastSeen"] as? java.util.Date ?: java.util.Date(),
                    isOnline = data["isOnline"] as? Boolean ?: false,
                    createdAt = data["createdAt"] as? java.util.Date ?: java.util.Date(),
                    updatedAt = data["updatedAt"] as? java.util.Date ?: java.util.Date(),
                    fcmToken = data["fcmToken"] as? String ?: ""
                )

                users.add(user)
                userIds.add(userId)
            }

            // Process phone results
            for (document in phoneQuerySnapshot.documents) {
                val userId = document.id
                if (userId == currentUserId || userIds.contains(userId)) continue

                val data = document.data ?: continue

                val user = User(
                    id = userId,
                    email = data["email"] as? String ?: "",
                    displayName = data["displayName"] as? String ?: "",
                    photoUrl = data["photoUrl"] as? String ?: "",
                    phoneNumber = data["phoneNumber"] as? String ?: "",
                    status = data["status"] as? String ?: "",
                    lastSeen = data["lastSeen"] as? java.util.Date ?: java.util.Date(),
                    isOnline = data["isOnline"] as? Boolean ?: false,
                    createdAt = data["createdAt"] as? java.util.Date ?: java.util.Date(),
                    updatedAt = data["updatedAt"] as? java.util.Date ?: java.util.Date(),
                    fcmToken = data["fcmToken"] as? String ?: ""
                )

                users.add(user)
                userIds.add(userId)
            }

            Result.success(users)
        } catch (e: Exception) {
            Log.e(TAG, "Error searching users", e)
            Result.failure(e)
        }
    }

    // Update user's phone number
    suspend fun updatePhoneNumber(phoneNumber: String): Result<Unit> {
        val userId = getCurrentUserId()

        return try {
            firestore.collection(Constants.COLLECTION_USERS)
                .document(userId)
                .update("phoneNumber", phoneNumber)
                .await()

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error updating phone number", e)
            Result.failure(e)
        }
    }
}