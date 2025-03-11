// File: app/src/main/java/com/example/chatlogger/fragments/ChatsFragment.kt
package com.example.chatlogger.fragments

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.example.chatlogger.R
import com.example.chatlogger.activities.ChatActivity
import com.example.chatlogger.adapters.ChatAdapter
import com.example.chatlogger.models.Chat
import com.example.chatlogger.models.User
import com.example.chatlogger.utils.Constants
import com.example.chatlogger.viewmodels.ChatViewModel
import com.example.chatlogger.viewmodels.ContactsViewModel
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class ChatsFragment : Fragment() {

    private lateinit var chatViewModel: ChatViewModel
    private lateinit var contactsViewModel: ContactsViewModel
    private lateinit var chatAdapter: ChatAdapter

    private lateinit var recyclerView: RecyclerView
    private lateinit var swipeRefreshLayout: SwipeRefreshLayout
    private lateinit var emptyView: TextView
    private lateinit var newChatFab: FloatingActionButton
    private lateinit var progressBar: ProgressBar

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_chats, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize views
        recyclerView = view.findViewById(R.id.chats_recycler_view)
        swipeRefreshLayout = view.findViewById(R.id.swipe_refresh_layout)
        emptyView = view.findViewById(R.id.empty_view)
        newChatFab = view.findViewById(R.id.new_chat_fab)
        progressBar = view.findViewById(R.id.progress_bar)

        // Initialize ViewModels
        chatViewModel = ViewModelProvider(requireActivity())[ChatViewModel::class.java]
        contactsViewModel = ViewModelProvider(requireActivity())[ContactsViewModel::class.java]

        // Setup RecyclerView
        setupRecyclerView()

        // Setup swipe to refresh
        swipeRefreshLayout.setOnRefreshListener {
            loadChats()
        }

        // Setup FAB
        newChatFab.setOnClickListener {
            // Navigate to contacts
            findNavController().navigate(R.id.contactsFragment)
        }

        // Observe data
        observeViewModel()

        // Load chats
        loadChats()
    }

    private fun setupRecyclerView() {
        chatAdapter = ChatAdapter { chat ->
            openChat(chat)
        }
        recyclerView.adapter = chatAdapter
    }

    private fun observeViewModel() {
        // Observe chats
        chatViewModel.chats.observe(viewLifecycleOwner) { chats ->
            chatAdapter.submitList(chats)

            // Show empty view if no chats
            if (chats.isEmpty()) {
                emptyView.visibility = View.VISIBLE
                recyclerView.visibility = View.GONE
            } else {
                emptyView.visibility = View.GONE
                recyclerView.visibility = View.VISIBLE
            }

            // Stop refresh animation
            swipeRefreshLayout.isRefreshing = false
        }

        // Observe loading state
        chatViewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        }

        // Observe error messages
        chatViewModel.errorMessage.observe(viewLifecycleOwner) { errorMessage ->
            if (!errorMessage.isNullOrEmpty()) {
                Toast.makeText(requireContext(), errorMessage, Toast.LENGTH_SHORT).show()
                // Clear error after showing
                chatViewModel.clearError()

                // Stop refresh animation
                swipeRefreshLayout.isRefreshing = false
            }
        }
    }

    private fun loadChats() {
        chatViewModel.loadChats()
    }

    private fun openChat(chat: Chat) {
        val intent = Intent(requireContext(), ChatActivity::class.java).apply {
            putExtra(Constants.EXTRA_CHAT_ID, chat.id)
        }
        startActivity(intent)
    }

    override fun onResume() {
        super.onResume()
        // Refresh chats
        loadChats()
    }
}