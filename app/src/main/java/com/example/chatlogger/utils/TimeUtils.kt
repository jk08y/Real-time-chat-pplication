// File: app/src/main/java/com/example/chatlogger/utils/TimeUtils.kt
package com.example.chatlogger.utils

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

object TimeUtils {

    // Format for time display in messages (e.g., 12:34 PM)
    private val timeFormat = SimpleDateFormat("h:mm a", Locale.getDefault())

    // Format for date display in chats (e.g., Jan 15)
    private val dateFormat = SimpleDateFormat("MMM d", Locale.getDefault())

    // Format for full date and time (e.g., Jan 15, 2023 12:34 PM)
    private val fullDateTimeFormat = SimpleDateFormat("MMM d, yyyy h:mm a", Locale.getDefault())

    // Format date based on recency
    fun formatMessageTime(date: Date): String {
        val now = Calendar.getInstance()
        val messageTime = Calendar.getInstance().apply {
            time = date
        }

        return when {
            // If same day
            isSameDay(now, messageTime) -> timeFormat.format(date)

            // If yesterday
            isYesterday(now, messageTime) -> "Yesterday"

            // If within a week
            isWithinDays(now, messageTime, 7) -> {
                val dayFormat = SimpleDateFormat("EEEE", Locale.getDefault())
                dayFormat.format(date)
            }

            // If within the same year
            isSameYear(now, messageTime) -> dateFormat.format(date)

            // Otherwise, include year
            else -> {
                val yearFormat = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
                yearFormat.format(date)
            }
        }
    }

    // Format date for chat list
    fun formatChatListDate(date: Date): String {
        val now = Calendar.getInstance()
        val messageTime = Calendar.getInstance().apply {
            time = date
        }

        return when {
            // If today, show time
            isSameDay(now, messageTime) -> timeFormat.format(date)

            // If yesterday
            isYesterday(now, messageTime) -> "Yesterday"

            // If within a week
            isWithinDays(now, messageTime, 7) -> {
                val dayFormat = SimpleDateFormat("EEE", Locale.getDefault())
                dayFormat.format(date)
            }

            // Otherwise date
            else -> dateFormat.format(date)
        }
    }

    // Format last seen time
    fun formatLastSeen(date: Date): String {
        val now = Date()
        val diff = now.time - date.time
        val minutes = TimeUnit.MILLISECONDS.toMinutes(diff)

        return when {
            minutes < 1 -> "Just now"
            minutes < 60 -> "$minutes min ago"
            minutes < 60 * 24 -> "${TimeUnit.MILLISECONDS.toHours(diff)} hours ago"
            minutes < 60 * 24 * 7 -> "${TimeUnit.MILLISECONDS.toDays(diff)} days ago"
            else -> dateFormat.format(date)
        }
    }

    // Format voice message duration
    fun formatVoiceDuration(durationMillis: Long): String {
        val minutes = TimeUnit.MILLISECONDS.toMinutes(durationMillis)
        val seconds = TimeUnit.MILLISECONDS.toSeconds(durationMillis) % 60

        return if (minutes > 0) {
            String.format("%d:%02d", minutes, seconds)
        } else {
            String.format("0:%02d", seconds)
        }
    }

    // Format full date and time
    fun formatFullDateTime(date: Date): String {
        return fullDateTimeFormat.format(date)
    }

    // Helper methods for date comparison
    private fun isSameDay(cal1: Calendar, cal2: Calendar): Boolean {
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
                cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
    }

    private fun isYesterday(today: Calendar, other: Calendar): Boolean {
        val yesterday = Calendar.getInstance().apply {
            time = today.time
            add(Calendar.DAY_OF_YEAR, -1)
        }
        return isSameDay(yesterday, other)
    }

    private fun isWithinDays(today: Calendar, other: Calendar, days: Int): Boolean {
        val past = Calendar.getInstance().apply {
            time = today.time
            add(Calendar.DAY_OF_YEAR, -days)
        }
        return other.after(past) && other.before(today)
    }

    private fun isSameYear(cal1: Calendar, cal2: Calendar): Boolean {
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR)
    }
}