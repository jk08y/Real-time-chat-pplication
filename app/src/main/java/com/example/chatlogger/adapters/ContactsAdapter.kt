// File: app/src/main/java/com/example/chatlogger/adapters/ContactsAdapter.kt
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
import com.example.chatlogger.models.User
import com.example.chatlogger.utils.TimeUtils
import de.hdodenhof.circleimageview.CircleImageView

class ContactsAdapter(private val onClick: (User) -> Unit) :
    ListAdapter<User, ContactsAdapter.ContactViewHolder>(ContactDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ContactViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_contact, parent, false)
        return ContactViewHolder(view, onClick)
    }

    override fun onBindViewHolder(holder: ContactViewHolder, position: Int) {
        val contact = getItem(position)
        holder.bind(contact)
    }

    class ContactViewHolder(
        itemView: View,
        private val onClick: (User) -> Unit
    ) : RecyclerView.ViewHolder(itemView) {

        private val avatarImageView: CircleImageView = itemView.findViewById(R.id.contact_avatar)
        private val nameTextView: TextView = itemView.findViewById(R.id.contact_name)
        private val statusTextView: TextView = itemView.findViewById(R.id.contact_status)
        private val onlineStatusView: View = itemView.findViewById(R.id.online_status_indicator)

        private var currentContact: User? = null

        init {
            itemView.setOnClickListener {
                currentContact?.let {
                    onClick(it)
                }
            }
        }

        fun bind(contact: User) {
            currentContact = contact

            nameTextView.text = contact.displayName
            statusTextView.text = if (contact.isOnline) "Online" else "Last seen ${TimeUtils.formatLastSeen(contact.lastSeen)}"

            // Set online status indicator
            onlineStatusView.visibility = if (contact.isOnline) View.VISIBLE else View.GONE

            // Load avatar
            if (contact.photoUrl.isNotEmpty()) {
                Glide.with(itemView.context)
                    .load(contact.photoUrl)
                    .placeholder(R.drawable.default_avatar)
                    .error(R.drawable.default_avatar)
                    .into(avatarImageView)
            } else {
                avatarImageView.setImageResource(R.drawable.default_avatar)
            }
        }
    }

    private class ContactDiffCallback : DiffUtil.ItemCallback<User>() {
        override fun areItemsTheSame(oldItem: User, newItem: User): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: User, newItem: User): Boolean {
            return oldItem.displayName == newItem.displayName &&
                    oldItem.photoUrl == newItem.photoUrl &&
                    oldItem.isOnline == newItem.isOnline &&
                    oldItem.lastSeen == newItem.lastSeen &&
                    oldItem.status == newItem.status
        }
    }
}