// File: app/src/main/java/com/example/chatlogger/activities/MainActivity.kt
package com.example.chatlogger.activities

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.example.chatlogger.R
import com.example.chatlogger.repositories.LoggerRepository
import com.example.chatlogger.utils.PermissionUtils
import com.example.chatlogger.viewmodels.AuthViewModel
import com.google.android.material.bottomnavigation.BottomNavigationView

class MainActivity : AppCompatActivity(), PermissionUtils.PermissionCallback {

    private lateinit var authViewModel: AuthViewModel
    private lateinit var navController: NavController
    private lateinit var loggerRepository: LoggerRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Setup Navigation
        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.main_nav_host_fragment) as NavHostFragment
        navController = navHostFragment.navController

        // Setup Bottom Navigation
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottom_navigation)
        bottomNav.setupWithNavController(navController)

        // Initialize ViewModel
        authViewModel = ViewModelProvider(this)[AuthViewModel::class.java]

        // Initialize Logger Repository
        loggerRepository = LoggerRepository(this)

        // Observe authentication state
        authViewModel.authState.observe(this) { authState ->
            if (authState is AuthViewModel.AuthState.Unauthenticated) {
                // User is logged out, navigate to auth activity
                navigateToAuthActivity()
            }
        }

        // Request necessary permissions
        PermissionUtils.requestChatPermissions(this, this)
    }

    private fun navigateToAuthActivity() {
        val intent = Intent(this, AuthActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    override fun onPermissionsGranted() {
        // All permissions granted, check if logger should be started
        if (loggerRepository.isLoggerRunning()) {
            // Check if notification listener is enabled
            if (!PermissionUtils.isNotificationListenerEnabled(this)) {
                PermissionUtils.openNotificationListenerSettings(this)
            } else {
                loggerRepository.startLoggerService()
            }
        }
    }

    override fun onPermissionsDenied(deniedPermissions: List<String>) {
        // Some permissions were denied
        // We could show a dialog explaining why permissions are needed
    }

    override fun onSupportNavigateUp(): Boolean {
        return navController.navigateUp() || super.onSupportNavigateUp()
    }

    override fun onResume() {
        super.onResume()
        // Update online status
        authViewModel.updateOnlineStatus(true)
    }

    override fun onPause() {
        super.onPause()
        // Update online status
        authViewModel.updateOnlineStatus(false)
    }
}