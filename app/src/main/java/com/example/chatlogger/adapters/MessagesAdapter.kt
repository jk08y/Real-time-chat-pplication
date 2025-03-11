// File: app/src/main/java/com/example/chatlogger/adapters/MessagesAdapter.kt
package com.example.chatlogger.adapters

import android.media.MediaPlayer
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.SeekBar
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.chatlogger.R
import com.example.chatlogger.models.Message
import com.example.chatlogger.utils.TimeUtils
import com.google.firebase.auth.FirebaseAuth
import java.util.HashMap

class MessagesAdapter : ListAdapter<Message, RecyclerView.ViewHolder>(MessageDiffCallback()) {

    companion object {
        private const val VIEW_TYPE_SENT = 1
        private const val VIEW_TYPE_RECEIVED = 2
    }

    // Media player for voice messages
    private var mediaPlayer: MediaPlayer? = null
    private var currentlyPlayingMessageId: String? = null
    private val handler = Handler(Looper.getMainLooper())
    private val updateRunnable = HashMap<String, Runnable>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            VIEW_TYPE_SENT -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_message_sent, parent, false)
                SentMessageViewHolder(view)
            }
            VIEW_TYPE_RECEIVED -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_message_received, parent, false)
                ReceivedMessageViewHolder(view)
            }
            else -> throw IllegalArgumentException("Invalid view type")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val message = getItem(position)

        when (holder) {
            is SentMessageViewHolder -> holder.bind(message)
            is ReceivedMessageViewHolder -> holder.bind(message)
        }
    }

    override fun getItemViewType(position: Int): Int {
        val message = getItem(position)
        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: ""

        return if (message.senderId == currentUserId) {
            VIEW_TYPE_SENT
        } else {
            VIEW_TYPE_RECEIVED
        }
    }

    // Clean up media player resources
    fun releaseMediaPlayer() {
        mediaPlayer?.release()
        mediaPlayer = null
        currentlyPlayingMessageId = null

        // Remove all callbacks
        updateRunnable.values.forEach { runnable ->
            handler.removeCallbacks(runnable)
        }
        updateRunnable.clear()
    }

    // Handle playing voice messages
    private fun handleVoiceMessage(
        messageId: String,
        voiceUrl: String,
        playButton: ImageView,
        pauseButton: ImageView,
        seekBar: SeekBar,
        progressBar: ProgressBar?
    ) {
        // If another message is playing, stop it
        if (currentlyPlayingMessageId != null && currentlyPlayingMessageId != messageId) {
            stopPlayback()
        }

        // If this message is already playing, pause it
        if (currentlyPlayingMessageId == messageId && mediaPlayer?.isPlaying == true) {
            mediaPlayer?.pause()
            playButton.visibility = View.VISIBLE
            pauseButton.visibility = View.GONE
            return
        }

        // If this message was paused, resume it
        if (currentlyPlayingMessageId == messageId && mediaPlayer != null) {
            mediaPlayer?.start()
            playButton.visibility = View.GONE
            pauseButton.visibility = View.VISIBLE

            // Resume progress updates
            updateRunnable[messageId]?.let { runnable ->
                handler.post(runnable)
            }
            return
        }

        // Otherwise, start playing this message
        currentlyPlayingMessageId = messageId
        playButton.visibility = View.GONE
        pauseButton.visibility = View.GONE
        progressBar?.visibility = View.VISIBLE

        // Create and prepare the media player
        mediaPlayer = MediaPlayer().apply {
            setOnPreparedListener {
                progressBar?.visibility = View.GONE
                playButton.visibility = View.GONE
                pauseButton.visibility = View.VISIBLE

                seekBar.max = it.duration
                seekBar.progress = 0

                start()

                // Create a runnable to update the seek bar
                val runnable = object : Runnable {
                    override fun run() {
                        if (mediaPlayer != null && mediaPlayer?.isPlaying == true) {
                            seekBar.progress = mediaPlayer?.currentPosition ?: 0
                            handler.postDelayed(this, 100)
                        }
                    }
                }

                updateRunnable[messageId] = runnable
                handler.post(runnable)
            }

            setOnCompletionListener {
                playButton.visibility = View.VISIBLE
                pauseButton.visibility = View.GONE
                seekBar.progress = 0
                currentlyPlayingMessageId = null

                // Remove the runnable
                updateRunnable[messageId]?.let { runnable ->
                    handler.removeCallbacks(runnable)
                }
                updateRunnable.remove(messageId)
            }

            setDataSource(voiceUrl)
            prepareAsync()
        }

        // Set up seek bar change listener
        seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    mediaPlayer?.seekTo(progress)
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}

            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
    }

    // Stop any playing audio
    private fun stopPlayback() {
        mediaPlayer?.stop()
        mediaPlayer?.reset()

        // Remove any ongoing updates
        currentlyPlayingMessageId?.let { messageId ->
            updateRunnable[messageId]?.let { runnable ->
                handler.removeCallbacks(runnable)
            }
            updateRunnable.remove(messageId)
        }

        currentlyPlayingMessageId = null
    }

    // View holder for sent messages
    inner class SentMessageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val timeTextView: TextView = itemView.findViewById(R.id.text_message_time)
        private val messageStatusView: ImageView = itemView.findViewById(R.id.message_status)

        // Text message views
        private val textMessageLayout: View = itemView.findViewById(R.id.layout_text_message)
        private val textMessageTextView: TextView = itemView.findViewById(R.id.text_message_content)

        // Image message views
        private val imageMessageLayout: View = itemView.findViewById(R.id.layout_image_message)
        private val imageMessageImageView: ImageView = itemView.findViewById(R.id.image_message_content)

        // Voice message views
        private val voiceMessageLayout: View = itemView.findViewById(R.id.layout_voice_message)
        private val voicePlayButton: ImageView = itemView.findViewById(R.id.voice_play_button)
        private val voicePauseButton: ImageView = itemView.findViewById(R.id.voice_pause_button)
        private val voiceSeekBar: SeekBar = itemView.findViewById(R.id.voice_seekbar)
        private val voiceDurationTextView: TextView = itemView.findViewById(R.id.voice_duration)
        private val voiceProgressBar: ProgressBar = itemView.findViewById(R.id.voice_progress)

        fun bind(message: Message) {
            // Set time
            timeTextView.text = TimeUtils.formatMessageTime(message.timestamp)

            // Set message status
            updateMessageStatus(message)

            // Hide all message types initially
            textMessageLayout.visibility = View.GONE
            imageMessageLayout.visibility = View.GONE
            voiceMessageLayout.visibility = View.GONE

            // Show the appropriate message type
            when (message.getType()) {
                Message.Type.TEXT -> {
                    textMessageLayout.visibility = View.VISIBLE

                    if (message.isDeleted) {
                        textMessageTextView.text = "This message has been deleted"
                        textMessageTextView.setTextColor(itemView.context.getColor(R.color.deleted_message_color))
                    } else {
                        textMessageTextView.text = message.text
                        textMessageTextView.setTextColor(itemView.context.getColor(R.color.message_text_color))
                    }
                }

                Message.Type.IMAGE -> {
                    imageMessageLayout.visibility = View.VISIBLE

                    if (message.isDeleted) {
                        imageMessageImageView.setImageResource(R.drawable.deleted_image_placeholder)
                    } else {
                        Glide.with(itemView.context)
                            .load(message.imageUrl)
                            .placeholder(R.drawable.image_placeholder)
                            .error(R.drawable.error_image_placeholder)
                            .into(imageMessageImageView)
                    }
                }

                Message.Type.VOICE -> {
                    voiceMessageLayout.visibility = View.VISIBLE

                    if (message.isDeleted) {
                        voicePlayButton.isEnabled = false
                        voiceSeekBar.isEnabled = false
                        voiceDurationTextView.text = "Deleted"
                    } else {
                        voicePlayButton.isEnabled = true
                        voiceSeekBar.isEnabled = true
                        voiceDurationTextView.text = message.voiceDuration?.let {
                            TimeUtils.formatVoiceDuration(it)
                        } ?: "0:00"

                        // Set up play button click
                        voicePlayButton.setOnClickListener {
                            message.voiceUrl?.let { url ->
                                handleVoiceMessage(
                                    message.id,
                                    url,
                                    voicePlayButton,
                                    voicePauseButton,
                                    voiceSeekBar,
                                    voiceProgressBar
                                )
                            }
                        }

                        // Set up pause button click
                        voicePauseButton.setOnClickListener {
                            mediaPlayer?.pause()
                            voicePlayButton.visibility = View.VISIBLE
                            voicePauseButton.visibility = View.GONE
                        }
                    }
                }
            }
        }

        private fun updateMessageStatus(message: Message) {
            // Set delivery/read status
            val statusIcon = when {
                message.readBy.isNotEmpty() -> R.drawable.ic_read
                message.deliveredTo.size > 1 -> R.drawable.ic_delivered
                else -> R.drawable.ic_sent
            }

            messageStatusView.setImageResource(statusIcon)
        }
    }

    // View holder for received messages
    inner class ReceivedMessageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val timeTextView: TextView = itemView.findViewById(R.id.text_message_time)

        // Text message views
        private val textMessageLayout: View = itemView.findViewById(R.id.layout_text_message)
        private val textMessageTextView: TextView = itemView.findViewById(R.id.text_message_content)

        // Image message views
        private val imageMessageLayout: View = itemView.findViewById(R.id.layout_image_message)
        private val imageMessageImageView: ImageView = itemView.findViewById(R.id.image_message_content)

        // Voice message views
        private val voiceMessageLayout: View = itemView.findViewById(R.id.layout_voice_message)
        private val voicePlayButton: ImageView = itemView.findViewById(R.id.voice_play_button)
        private val voicePauseButton: ImageView = itemView.findViewById(R.id.voice_pause_button)
        private val voiceSeekBar: SeekBar = itemView.findViewById(R.id.voice_seekbar)
        private val voiceDurationTextView: TextView = itemView.findViewById(R.id.voice_duration)
        private val voiceProgressBar: ProgressBar = itemView.findViewById(R.id.voice_progress)

        fun bind(message: Message) {
            // Set time
            timeTextView.text = TimeUtils.formatMessageTime(message.timestamp)

            // Hide all message types initially
            textMessageLayout.visibility = View.GONE
            imageMessageLayout.visibility = View.GONE
            voiceMessageLayout.visibility = View.GONE

            // Show the appropriate message type
            when (message.getType()) {
                Message.Type.TEXT -> {
                    textMessageLayout.visibility = View.VISIBLE

                    if (message.isDeleted) {
                        textMessageTextView.text = "This message has been deleted"
                        textMessageTextView.setTextColor(itemView.context.getColor(R.color.deleted_message_color))
                    } else {
                        textMessageTextView.text = message.text
                        textMessageTextView.setTextColor(itemView.context.getColor(R.color.message_text_color))
                    }
                }

                Message.Type.IMAGE -> {
                    imageMessageLayout.visibility = View.VISIBLE

                    if (message.isDeleted) {
                        imageMessageImageView.setImageResource(R.drawable.deleted_image_placeholder)
                    } else {
                        Glide.with(itemView.context)
                            .load(message.imageUrl)
                            .placeholder(R.drawable.image_placeholder)
                            .error(R.drawable.error_image_placeholder)
                            .into(imageMessageImageView)
                    }
                }

                Message.Type.VOICE -> {
                    voiceMessageLayout.visibility = View.VISIBLE

                    if (message.isDeleted) {
                        voicePlayButton.isEnabled = false
                        voiceSeekBar.isEnabled = false
                        voiceDurationTextView.text = "Deleted"
                    } else {
                        voicePlayButton.isEnabled = true
                        voiceSeekBar.isEnabled = true
                        voiceDurationTextView.text = message.voiceDuration?.let {
                            TimeUtils.formatVoiceDuration(it)
                        } ?: "0:00"

                        // Set up play button click
                        voicePlayButton.setOnClickListener {
                            message.voiceUrl?.let { url ->
                                handleVoiceMessage(
                                    message.id,
                                    url,
                                    voicePlayButton,
                                    voicePauseButton,
                                    voiceSeekBar,
                                    voiceProgressBar
                                )
                            }
                        }

                        // Set up pause button click
                        voicePauseButton.setOnClickListener {
                            mediaPlayer?.pause()
                            voicePlayButton.visibility = View.VISIBLE
                            voicePauseButton.visibility = View.GONE
                        }
                    }
                }
            }
        }
    }

    private class MessageDiffCallback : DiffUtil.ItemCallback<Message>() {
        override fun areItemsTheSame(oldItem: Message, newItem: Message): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Message, newItem: Message): Boolean {
            return oldItem.text == newItem.text &&
                    oldItem.imageUrl == newItem.imageUrl &&
                    oldItem.voiceUrl == newItem.voiceUrl &&
                    oldItem.isDeleted == newItem.isDeleted &&
                    oldItem.readBy.size == newItem.readBy.size
        }
    }
}