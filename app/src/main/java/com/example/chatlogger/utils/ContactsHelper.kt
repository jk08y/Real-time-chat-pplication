// File: app/src/main/java/com/example/chatlogger/utils/ContactsHelper.kt
package com.example.chatlogger.utils

import android.content.ContentResolver
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.provider.ContactsContract
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ContactsHelper(private val context: Context) {

    // Model class for contact
    data class Contact(
        val id: String,
        val name: String,
        val phoneNumber: String,
        val photoUri: String?
    )

    // Get name from phone number
    suspend fun getContactNameFromNumber(phoneNumber: String): String? = withContext(Dispatchers.IO) {
        val normalizedNumber = normalizePhoneNumber(phoneNumber)
        if (normalizedNumber.isBlank()) return@withContext null

        val uri = Uri.withAppendedPath(
            ContactsContract.PhoneLookup.CONTENT_FILTER_URI,
            Uri.encode(normalizedNumber)
        )

        val projection = arrayOf(ContactsContract.PhoneLookup.DISPLAY_NAME)

        context.contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                return@withContext cursor.getString(0)
            }
        }

        return@withContext null
    }

    // Get all contacts
    suspend fun getAllContacts(): List<Contact> = withContext(Dispatchers.IO) {
        val contacts = mutableListOf<Contact>()

        val projection = arrayOf(
            ContactsContract.Contacts._ID,
            ContactsContract.Contacts.DISPLAY_NAME,
            ContactsContract.Contacts.HAS_PHONE_NUMBER,
            ContactsContract.Contacts.PHOTO_URI
        )

        context.contentResolver.query(
            ContactsContract.Contacts.CONTENT_URI,
            projection,
            null,
            null,
            ContactsContract.Contacts.DISPLAY_NAME + " ASC"
        )?.use { cursor ->
            while (cursor.moveToNext()) {
                val id = cursor.getString(cursor.getColumnIndexOrThrow(ContactsContract.Contacts._ID))
                val name = cursor.getString(cursor.getColumnIndexOrThrow(ContactsContract.Contacts.DISPLAY_NAME))
                val hasPhoneNumber = cursor.getInt(cursor.getColumnIndexOrThrow(ContactsContract.Contacts.HAS_PHONE_NUMBER))
                val photoUri = cursor.getString(cursor.getColumnIndexOrThrow(ContactsContract.Contacts.PHOTO_URI))

                if (hasPhoneNumber > 0) {
                    getContactPhoneNumbers(id).forEach { phoneNumber ->
                        contacts.add(Contact(id, name, phoneNumber, photoUri))
                    }
                }
            }
        }

        return@withContext contacts
    }

    // Get contact phone numbers
    private fun getContactPhoneNumbers(contactId: String): List<String> {
        val phoneNumbers = mutableListOf<String>()

        val projection = arrayOf(ContactsContract.CommonDataKinds.Phone.NUMBER)
        val selection = ContactsContract.CommonDataKinds.Phone.CONTACT_ID + " = ?"
        val selectionArgs = arrayOf(contactId)

        context.contentResolver.query(
            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
            projection,
            selection,
            selectionArgs,
            null
        )?.use { cursor ->
            while (cursor.moveToNext()) {
                val phoneNumber = cursor.getString(cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.NUMBER))
                val normalizedNumber = normalizePhoneNumber(phoneNumber)

                if (normalizedNumber.isNotBlank() && !phoneNumbers.contains(normalizedNumber)) {
                    phoneNumbers.add(normalizedNumber)
                }
            }
        }

        return phoneNumbers
    }

    // Normalize phone number (remove spaces, dashes, etc.)
    fun normalizePhoneNumber(phoneNumber: String): String {
        return phoneNumber.replace(Regex("[^\\d+]"), "")
    }
}