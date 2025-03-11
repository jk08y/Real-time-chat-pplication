// File: app/src/main/java/com/example/chatlogger/fragments/SettingsFragment.kt
package com.example.chatlogger.fragments

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.bumptech.glide.Glide
import com.example.chatlogger.R
import com.example.chatlogger.repositories.LoggerRepository
import com.example.chatlogger.utils.PermissionUtils
import com.example.chatlogger.viewmodels.AuthViewModel
import com.example.chatlogger.viewmodels.SettingsViewModel
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import de.hdodenhof.circleimageview.CircleImageView

class SettingsFragment : Fragment(), PermissionUtils.PermissionCallback {

    private lateinit var authViewModel: AuthViewModel
    private lateinit var settingsViewModel: SettingsViewModel
    private lateinit var loggerRepository: LoggerRepository

    private lateinit var profileImageView: CircleImageView
    private lateinit var profileNameText: TextView
    private lateinit var profileEmailText: TextView
    private lateinit var profilePhoneText: TextView
    private lateinit var editProfileLayout: ConstraintLayout
    private lateinit var updatePhoneLayout: ConstraintLayout
    private lateinit var darkModeSwitch: SwitchCompat
    private lateinit var loggerEnabledSwitch: SwitchCompat
    private lateinit var viewLogsLayout: ConstraintLayout
    private lateinit var clearLogsLayout: ConstraintLayout
    private lateinit var logoutButton: Button
    private lateinit var progressBar: ProgressBar

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_settings, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize views
        profileImageView = view.findViewById(R.id.profile_image)
        profileNameText = view.findViewById(R.id.profile_name)
        profileEmailText = view.findViewById(R.id.profile_email)
        profilePhoneText = view.findViewById(R.id.profile_phone)
        editProfileLayout = view.findViewById(R.id.edit_profile_layout)
        updatePhoneLayout = view.findViewById(R.id.update_phone_layout)
        darkModeSwitch = view.findViewById(R.id.dark_mode_switch)
        loggerEnabledSwitch = view.findViewById(R.id.logger_enabled_switch)
        viewLogsLayout = view.findViewById(R.id.view_logs_layout)
        clearLogsLayout = view.findViewById(R.id.clear_logs_layout)
        logoutButton = view.findViewById(R.id.logout_button)
        progressBar = view.findViewById(R.id.progress_bar)

        // Initialize ViewModels
        authViewModel = ViewModelProvider(requireActivity())[AuthViewModel::class.java]
        settingsViewModel = ViewModelProvider(requireActivity())[SettingsViewModel::class.java]

        // Initialize logger repository
        loggerRepository = LoggerRepository(requireContext())

        // Setup click listeners
        setupClickListeners()

        // Setup switches
        setupSwitches()

        // Observe data
        observeViewModel()
    }

    private fun setupClickListeners() {
        // Edit profile
        editProfileLayout.setOnClickListener {
            // Show edit profile dialog
            showEditProfileDialog()
        }

        // Update phone
        updatePhoneLayout.setOnClickListener {
            // Show update phone dialog
            showUpdatePhoneDialog()
        }

        // View logs
        viewLogsLayout.setOnClickListener {
            // Navigate to logged messages screen
            showLoggedMessagesDialog()
        }

        // Clear logs
        clearLogsLayout.setOnClickListener {
            // Show confirmation dialog
            showClearLogsConfirmationDialog()
        }

        // Logout
        logoutButton.setOnClickListener {
            // Show confirmation dialog
            showLogoutConfirmationDialog()
        }
    }

    private fun setupSwitches() {
        // Dark mode switch
        darkModeSwitch.isChecked = settingsViewModel.darkModeEnabled.value ?: false
        darkModeSwitch.setOnCheckedChangeListener { _, isChecked ->
            settingsViewModel.toggleDarkMode(isChecked)
        }

        // Logger enabled switch
        loggerEnabledSwitch.isChecked = settingsViewModel.loggerEnabled.value ?: false
        loggerEnabledSwitch.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked && !settingsViewModel.hasLoggerPermissions()) {
                PermissionUtils.requestLoggerPermissions(requireActivity(), this)
                loggerEnabledSwitch.isChecked = false
            } else {
                settingsViewModel.toggleLogger(isChecked)
            }
        }
    }

    private fun observeViewModel() {
        // Observe user profile
        authViewModel.userProfile.observe(viewLifecycleOwner) { user ->
            if (user != null) {
                // Update profile UI
                profileNameText.text = user.displayName
                profileEmailText.text = user.email
                profilePhoneText.text = user.phoneNumber.ifEmpty { "Add phone number" }

                // Load profile image
                if (user.photoUrl.isNotEmpty()) {
                    Glide.with(this)
                        .load(user.photoUrl)
                        .placeholder(R.drawable.default_avatar)
                        .error(R.drawable.default_avatar)
                        .into(profileImageView)
                } else {
                    profileImageView.setImageResource(R.drawable.default_avatar)
                }
            }
        }

        // Observe dark mode state
        settingsViewModel.darkModeEnabled.observe(viewLifecycleOwner) { isEnabled ->
            darkModeSwitch.isChecked = isEnabled

            // Update app theme
            (requireActivity() as? AppCompatActivity)?.delegate?.apply {
                localNightMode = if (isEnabled) {
                    androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_YES
                } else {
                    androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_NO
                }
            }
        }

        // Observe logger state
        settingsViewModel.loggerEnabled.observe(viewLifecycleOwner) { isEnabled ->
            loggerEnabledSwitch.isChecked = isEnabled
        }

        // Observe loading state
        settingsViewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        }

        // Observe error messages
        settingsViewModel.errorMessage.observe(viewLifecycleOwner) { errorMessage ->
            if (!errorMessage.isNullOrEmpty()) {
                Toast.makeText(requireContext(), errorMessage, Toast.LENGTH_SHORT).show()
                settingsViewModel.clearError()
            }
        }
    }

    private fun showEditProfileDialog() {
        // In a real app, you'd create a dialog with fields for name, status, and photo
        Toast.makeText(requireContext(), "Edit profile feature would go here", Toast.LENGTH_SHORT).show()
    }

    private fun showUpdatePhoneDialog() {
        // In a real app, you'd create a dialog with a field for phone number
        Toast.makeText(requireContext(), "Update phone feature would go here", Toast.LENGTH_SHORT).show()
    }

    private fun showLoggedMessagesDialog() {
        // In a real app, you'd navigate to a new screen to show logged messages
        // For simplicity, we'll just load them and show a toast
        settingsViewModel.loadLoggedMessages()
        Toast.makeText(requireContext(), "Logged messages would be displayed here", Toast.LENGTH_SHORT).show()
    }

    private fun showClearLogsConfirmationDialog() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.clear_logs)
            .setMessage(R.string.confirm_clear_logs)
            .setPositiveButton(R.string.delete) { _, _ ->
                settingsViewModel.clearAllLoggedMessages { success ->
                    if (success) {
                        Toast.makeText(requireContext(), R.string.logs_cleared, Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun showLogoutConfirmationDialog() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.logout)
            .setMessage("Are you sure you want to logout?")
            .setPositiveButton(R.string.logout) { _, _ ->
                authViewModel.logout()
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    override fun onPermissionsGranted() {
        // Check if notification listener is enabled
        if (!PermissionUtils.isNotificationListenerEnabled(requireContext())) {
            showNotificationAccessDialog()
        } else {
            // Enable logger
            loggerEnabledSwitch.isChecked = true
            settingsViewModel.toggleLogger(true)
        }
    }

    override fun onPermissionsDenied(deniedPermissions: List<String>) {
        Toast.makeText(requireContext(), R.string.permissions_denied, Toast.LENGTH_SHORT).show()
    }

    private fun showNotificationAccessDialog() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.enable_notification_access)
            .setMessage(R.string.notification_access_required)
            .setPositiveButton(R.string.open_settings) { _, _ ->
                PermissionUtils.openNotificationListenerSettings(requireActivity())
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }
}