// File: app/src/main/java/com/example/chatlogger/activities/SplashActivity.kt
package com.example.chatlogger.activities

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.example.chatlogger.R
import com.example.chatlogger.viewmodels.AuthViewModel

class SplashActivity : AppCompatActivity() {

    private lateinit var authViewModel: AuthViewModel
    private val splashTimeOut: Long = 2000 // 2 seconds

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        // Initialize ViewModel
        authViewModel = ViewModelProvider(this)[AuthViewModel::class.java]

        // Observe authentication state
        authViewModel.authState.observe(this) { authState ->
            if (authState is AuthViewModel.AuthState.Authenticated) {
                navigateToMainActivity()
            } else if (authState is AuthViewModel.AuthState.Unauthenticated) {
                navigateToAuthActivity()
            }
            // In case of loading or error, just wait for the timeout
        }

        // Set a timeout to ensure the splash screen doesn't hang indefinitely
        Handler(Looper.getMainLooper()).postDelayed({
            if (!isFinishing) {
                // Default to auth activity if no decision was made
                navigateToAuthActivity()
            }
        }, splashTimeOut)
    }

    private fun navigateToMainActivity() {
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        finish()
    }

    private fun navigateToAuthActivity() {
        val intent = Intent(this, AuthActivity::class.java)
        startActivity(intent)
        finish()
    }
}