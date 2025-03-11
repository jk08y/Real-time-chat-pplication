// File: app/src/main/java/com/example/chatlogger/activities/ChatActivity.kt
package com.example.chatlogger.activities

import android.content.Intent
import android.media.MediaRecorder
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher
import android.view.MenuItem
import android.view.View
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.chatlogger.R
import com.example.chatlogger.adapters.MessagesAdapter
import com.example.chatlogger.models.User
import com.example.chatlogger.repositories.AuthRepository
import com.example.chatlogger.utils.Constants
import com.example.chatlogger.viewmodels.ChatViewModel
import com.google.android.material.appbar.MaterialToolbar
import de.hdodenhof.circleimageview.CircleImageView
import java.io.File
import java.util.Date
import java.util.UUID

class ChatActivity : AppCompatActivity() {

    private lateinit var chatViewModel: ChatViewModel
    private lateinit var messagesAdapter: MessagesAdapter

    private lateinit var messageRecyclerView: RecyclerView
    private lateinit var messageEditText: EditText
    private lateinit var sendButton: ImageButton
    private lateinit var attachmentButton: ImageButton
    private lateinit var voiceRecordButton: ImageButton
    private lateinit var typingIndicator: TextView
    private lateinit var toolbar: MaterialToolbar
    private lateinit var contactAvatar: CircleImageView
    private lateinit var contactName: TextView
    private lateinit var contactStatus: TextView
    private lateinit var voiceRecordingLayout: ConstraintLayout
    private lateinit var recordingTime: TextView
    private lateinit var recordingCancel: TextView

    private var chatId: String = ""
    private var otherUser: User? = null
    private var isTyping = false
    private var typingHandler = Handler(Looper.getMainLooper())
    private var typingRunnable: Runnable? = null

    // Voice recording
    private var mediaRecorder: MediaRecorder? = null
    private var isRecording = false
    private var audioFile: File? = null
    private var recordingStartTime: Long = 0
    private val recordingHandler = Handler(Looper.getMainLooper())
    private val recordingRunnable = object : Runnable {
        override fun run() {
            updateRecordingTime()
            recordingHandler.postDelayed(this, 1000)
        }
    }

    // Image picker
    private val getContent = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let { sendImage(it) }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat)

        // Get chat ID from intent
        chatId = intent.getStringExtra(Constants.EXTRA_CHAT_ID) ?: ""
        if (chatId.isEmpty()) {
            Toast.makeText(this, "Invalid chat ID", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        // Initialize views
        initViews()

        // Setup toolbar
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowTitleEnabled(false)

        // Initialize ViewModel
        chatViewModel = ViewModelProvider(this)[ChatViewModel::class.java]

        // Setup RecyclerView
        setupRecyclerView()

        // Setup click listeners
        setupClickListeners()

        // Setup text change listener for typing indicator
        setupTypingListener()

        // Observe data
        observeViewModel()

        // Load chat data
        chatViewModel.getChatById(chatId)
        chatViewModel.loadMessages(chatId)
    }

    private fun initViews() {
        messageRecyclerView = findViewById(R.id.messages_recycler_view)
        messageEditText = findViewById(R.id.message_edit_text)
        sendButton = findViewById(R.id.send_button)
        attachmentButton = findViewById(R.id.attachment_button)
        voiceRecordButton = findViewById(R.id.voice_record_button)
        typingIndicator = findViewById(R.id.typing_indicator)
        toolbar = findViewById(R.id.chat_toolbar)
        contactAvatar = findViewById(R.id.contact_avatar)
        contactName = findViewById(R.id.contact_name)
        contactStatus = findViewById(R.id.contact_status)
        voiceRecordingLayout = findViewById(R.id.voice_recording_layout)
        recordingTime = findViewById(R.id.recording_time)
        recordingCancel = findViewById(R.id.recording_cancel)
    }

    private fun setupRecyclerView() {
        messagesAdapter = MessagesAdapter()
        messageRecyclerView.adapter = messagesAdapter

        val layoutManager = LinearLayoutManager(this)
        layoutManager.stackFromEnd = true
        messageRecyclerView.layoutManager = layoutManager

        // Scroll to bottom when keyboard appears
        messageRecyclerView.addOnLayoutChangeListener { _, _, _, _, bottom, _, _, _, oldBottom ->
            if (bottom < oldBottom) {
                messageRecyclerView.postDelayed({
                    if (messagesAdapter.itemCount > 0) {
                        messageRecyclerView.smoothScrollToPosition(messagesAdapter.itemCount - 1)
                    }
                }, 100)
            }
        }
    }

    private fun setupClickListeners() {
        // Send button
        sendButton.setOnClickListener {
            val message = messageEditText.text.toString().trim()
            if (message.isNotEmpty()) {
                chatViewModel.sendTextMessage(chatId, message) { success ->
                    if (success) {
                        messageEditText.text.clear()
                    } else {
                        Toast.makeText(this, "Failed to send message", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }

        // Attachment button
        attachmentButton.setOnClickListener {
            getContent.launch("image/*")
        }

        // Voice record button
        voiceRecordButton.setOnClickListener {
            if (!isRecording) {
                startRecording()
            } else {
                stopRecording()
                sendVoiceMessage()
            }
        }

        // Recording cancel
        recordingCancel.setOnClickListener {
            cancelRecording()
        }
    }

    private fun setupTypingListener() {
        // Initialize typing runnable
        typingRunnable = Runnable {
            if (isTyping) {
                chatViewModel.updateTypingStatus(chatId, false)
                isTyping = false
            }
        }

        // Add text change listener
        messageEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                typingHandler.removeCallbacks(typingRunnable!!)

                if (!isTyping) {
                    chatViewModel.updateTypingStatus(chatId, true)
                    isTyping = true
                }

                // Set timeout for typing status (5 seconds)
                typingHandler.postDelayed(typingRunnable!!, 5000)
            }

            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun observeViewModel() {
        // Observe current chat
        chatViewModel.currentChat.observe(this) { chat ->
            if (chat != null) {
                // Get other user ID
                val currentUserId = AuthRepository().getCurrentUserId()
                val otherUserId = chat.getOtherParticipantId(currentUserId)

                // Get other user details
                if (otherUserId.isNotEmpty()) {
                    AuthRepository().getUserById(otherUserId).fold(
                        onSuccess = { user ->
                            otherUser = user
                            updateUserUI(user)
                        },
                        onFailure = {
                            // Handle error
                            Toast.makeText(this, "Failed to get user details", Toast.LENGTH_SHORT).show()
                        }
                    )
                }

                // Observe typing status
                val typingUsers = chat.typing.filter { it != currentUserId }
                if (typingUsers.isNotEmpty()) {
                    typingIndicator.visibility = View.VISIBLE
                } else {
                    typingIndicator.visibility = View.GONE
                }
            }
        }

        // Observe messages
        chatViewModel.messages.observe(this) { messages ->
            messagesAdapter.submitList(messages)

            // Scroll to bottom if new message
            if (messages.isNotEmpty()) {
                messageRecyclerView.post {
                    messageRecyclerView.smoothScrollToPosition(messages.size - 1)
                }
            }
        }

        // Observe loading state
        chatViewModel.isLoading.observe(this) { isLoading ->
            // Handle loading state if needed
        }

        // Observe error messages
        chatViewModel.errorMessage.observe(this) { errorMessage ->
            if (!errorMessage.isNullOrEmpty()) {
                Toast.makeText(this, errorMessage, Toast.LENGTH_SHORT).show()
                // Clear error after showing
                chatViewModel.clearError()
            }
        }
    }

    private fun updateUserUI(user: User) {
        contactName.text = user.displayName

        // Set status
        if (user.isOnline) {
            contactStatus.text = getString(R.string.online)
        } else {
            contactStatus.text = "Last seen ${com.example.chatlogger.utils.TimeUtils.formatLastSeen(user.lastSeen)}"
        }

        // Load profile image
        if (user.photoUrl.isNotEmpty()) {
            Glide.with(this)
                .load(user.photoUrl)
                .placeholder(R.drawable.default_avatar)
                .error(R.drawable.default_avatar)
                .into(contactAvatar)
        } else {
            contactAvatar.setImageResource(R.drawable.default_avatar)
        }
    }

    private fun sendImage(imageUri: Uri) {
        chatViewModel.sendImageMessage(chatId, imageUri) { success ->
            if (!success) {
                Toast.makeText(this, "Failed to send image", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun startRecording() {
        try {
            // Setup file
            val fileName = "${UUID.randomUUID()}.m4a"
            val dir = File(externalCacheDir?.absolutePath ?: cacheDir.absolutePath, "voice_messages")
            if (!dir.exists()) {
                dir.mkdirs()
            }
            audioFile = File(dir, fileName)

            // Setup recorder
            mediaRecorder = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                MediaRecorder(this)
            } else {
                MediaRecorder()
            }

            mediaRecorder?.apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setOutputFile(audioFile?.absolutePath)
                prepare()
                start()
            }

            // Update UI
            messageEditText.visibility = View.GONE
            voiceRecordingLayout.visibility = View.VISIBLE
            isRecording = true
            recordingStartTime = System.currentTimeMillis()

            // Start time counter
            recordingTime.text = "0:00"
            recordingHandler.post(recordingRunnable)

        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Failed to start recording", Toast.LENGTH_SHORT).show()
            cancelRecording()
        }
    }

    private fun stopRecording() {
        try {
            mediaRecorder?.apply {
                stop()
                release()
            }
            mediaRecorder = null
            recordingHandler.removeCallbacks(recordingRunnable)

            // Reset UI
            messageEditText.visibility = View.VISIBLE
            voiceRecordingLayout.visibility = View.GONE
            isRecording = false
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun cancelRecording() {
        stopRecording()
        audioFile?.delete()
        audioFile = null
    }

    private fun sendVoiceMessage() {
        if (audioFile == null || !audioFile!!.exists()) {
            return
        }

        val duration = System.currentTimeMillis() - recordingStartTime
        val uri = Uri.fromFile(audioFile)

        chatViewModel.sendVoiceMessage(chatId, uri, duration) { success ->
            if (!success) {
                Toast.makeText(this, "Failed to send voice message", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun updateRecordingTime() {
        val duration = System.currentTimeMillis() - recordingStartTime
        val seconds = (duration / 1000).toInt()
        val minutes = seconds / 60
        val remainingSeconds = seconds % 60

        recordingTime.text = String.format("%d:%02d", minutes, remainingSeconds)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                finish()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onResume() {
        super.onResume()
        // Mark messages as read
        if (chatId.isNotEmpty()) {
            chatViewModel.markMessagesAsRead(chatId)
        }
    }

    override fun onPause() {
        super.onPause()
        // Update typing status when leaving
        if (isTyping) {
            chatViewModel.updateTypingStatus(chatId, false)
            isTyping = false
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // Release resources
        messagesAdapter.releaseMediaPlayer()

        // Cancel any ongoing recording
        if (isRecording) {
            cancelRecording()
        }
    }
}