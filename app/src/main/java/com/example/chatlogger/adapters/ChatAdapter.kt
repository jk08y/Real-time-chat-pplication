// File: app/src/main/java/com/example/chatlogger/adapters/ChatAdapter.kt
package com.example.chatlogger.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.chatlogger.R
import com.example.chatlogger.models.Chat
import com.example.chatlogger.models.Message
import com.example.chatlogger.models.User
import com.example.chatlogger.repositories.AuthRepository
import com.example.chatlogger.utils.TimeUtils
import com.google.firebase.auth.FirebaseAuth
import de.hdodenhof.circleimageview.CircleImageView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ChatAdapter(private val onClick: (Chat) -> Unit) :
    ListAdapter<Chat, ChatAdapter.ChatViewHolder>(ChatDiffCallback()) {

    private val authRepository = AuthRepository()
    private val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: ""
    private val userCache = mutableMapOf<String, User>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChatViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_chat, parent, false)
        return ChatViewHolder(view, onClick)
    }

    override fun onBindViewHolder(holder: ChatViewHolder, position: Int) {
        val chat = getItem(position)

        // Get other user ID
        val otherUserId = chat.getOtherParticipantId(currentUserId)

        // Lookup user in cache
        val cachedUser = userCache[otherUserId]
        if (cachedUser != null) {
            holder.bind(chat, cachedUser)
        } else {
            // Load user data
            holder.bindLoading(chat)
            loadUserData(chat, otherUserId, holder)
        }
    }

    private fun loadUserData(chat: Chat, userId: String, holder: ChatViewHolder) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                authRepository.getUserById(userId).fold(
                    onSuccess = { user ->
                        // Cache user
                        userCache[userId] = user

                        // Update UI on main thread
                        withContext(Dispatchers.Main) {
                            if (holder.adapterPosition != RecyclerView.NO_POSITION) {
                                holder.bind(chat, user)
                            }
                        }
                    },
                    onFailure = { /* Handle error */ }
                )
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    class ChatViewHolder(
        itemView: View,
        private val onClick: (Chat) -> Unit
    ) : RecyclerView.ViewHolder(itemView) {

        private val contactAvatar: CircleImageView = itemView.findViewById(R.id.contact_avatar)
        private val contactName: TextView = itemView.findViewById(R.id.contact_name)
        private val lastMessage: TextView = itemView.findViewById(R.id.last_message)
        private val lastMessageTime: TextView = itemView.findViewById(R.id.last_message_time)
        private val unreadCount: TextView = itemView.findViewById(R.id.unread_count)
        private val onlineStatusIndicator: View = itemView.findViewById(R.id.online_status_indicator)

        private var currentChat: Chat? = null

        init {
            itemView.setOnClickListener {
                currentChat?.let {
                    onClick(it)
                }
            }
        }

        fun bindLoading(chat: Chat) {
            currentChat = chat

            // Show loading state
            contactName.text = "Loading..."
            lastMessage.text = ""
            unreadCount.visibility = View.GONE
            onlineStatusIndicator.visibility = View.GONE
            contactAvatar.setImageResource(R.drawable.default_avatar)

            // Set time
            if (chat.lastMessageTimestamp != null) {
                lastMessageTime.text = TimeUtils.formatChatListDate(chat.lastMessageTimestamp)
            } else {
                lastMessageTime.text = ""
            }
        }

        fun bind(chat: Chat, user: User) {
            currentChat = chat

            // Set user info
            contactName.text = user.displayName

            // Set last message
            if (chat.lastMessage != null) {
                val isFromCurrentUser = chat.lastMessageSenderId == FirebaseAuth.getInstance().currentUser?.uid
                val prefix = if (isFromCurrentUser) "You: " else ""

                when (chat.lastMessageType) {
                    "IMAGE" -> lastMessage.text = "$prefix 📷 Image"
                    "VOICE" -> lastMessage.text = "$prefix 🎤 Voice message"
                    else -> lastMessage.text = "$prefix ${chat.lastMessage}"
                }
            } else {
                lastMessage.text = ""
            }

            // Set time
            if (chat.lastMessageTimestamp != null) {
                lastMessageTime.text = TimeUtils.formatChatListDate(chat.lastMessageTimestamp)
            } else {
                lastMessageTime.text = ""
            }

            // Set unread count
            val count = chat.unreadCount[FirebaseAuth.getInstance().currentUser?.uid] ?: 0
            if (count > 0) {
                unreadCount.visibility = View.VISIBLE
                unreadCount.text = count.toString()
            } else {
                unreadCount.visibility = View.GONE
            }

            // Set online status
            onlineStatusIndicator.visibility = if (user.isOnline) View.VISIBLE else View.GONE

            // Load avatar
            if (user.photoUrl.isNotEmpty()) {
                Glide.with(itemView.context)
                    .load(user.photoUrl)
                    .placeholder(R.drawable.default_avatar)
                    .error(R.drawable.default_avatar)
                    .into(contactAvatar)
            } else {
                contactAvatar.setImageResource(R.drawable.default_avatar)
            }
        }
    }

    private class ChatDiffCallback : DiffUtil.ItemCallback<Chat>() {
        override fun areItemsTheSame(oldItem: Chat, newItem: Chat): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Chat, newItem: Chat): Boolean {
            return oldItem.lastMessageTimestamp == newItem.lastMessageTimestamp &&
                    oldItem.lastMessage == newItem.lastMessage &&
                    oldItem.unreadCount == newItem.unreadCount &&
                    oldItem.typing == newItem.typing
        }
    }
}