// File: app/src/main/java/com/example/chatlogger/fragments/RegisterFragment.kt
package com.example.chatlogger.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.example.chatlogger.R
import com.example.chatlogger.viewmodels.AuthViewModel
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout

class RegisterFragment : Fragment() {

    private lateinit var authViewModel: AuthViewModel

    private lateinit var nameInputLayout: TextInputLayout
    private lateinit var emailInputLayout: TextInputLayout
    private lateinit var passwordInputLayout: TextInputLayout
    private lateinit var confirmPasswordInputLayout: TextInputLayout
    private lateinit var nameEditText: TextInputEditText
    private lateinit var emailEditText: TextInputEditText
    private lateinit var passwordEditText: TextInputEditText
    private lateinit var confirmPasswordEditText: TextInputEditText
    private lateinit var registerButton: Button
    private lateinit var loginText: TextView
    private lateinit var progressBar: ProgressBar

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_register, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize views
        nameInputLayout = view.findViewById(R.id.name_input_layout)
        emailInputLayout = view.findViewById(R.id.email_input_layout)
        passwordInputLayout = view.findViewById(R.id.password_input_layout)
        confirmPasswordInputLayout = view.findViewById(R.id.confirm_password_input_layout)
        nameEditText = view.findViewById(R.id.name_edit_text)
        emailEditText = view.findViewById(R.id.email_edit_text)
        passwordEditText = view.findViewById(R.id.password_edit_text)
        confirmPasswordEditText = view.findViewById(R.id.confirm_password_edit_text)
        registerButton = view.findViewById(R.id.register_button)
        loginText = view.findViewById(R.id.login_text)
        progressBar = view.findViewById(R.id.progress_bar)

        // Get ViewModel
        authViewModel = ViewModelProvider(requireActivity())[AuthViewModel::class.java]

        // Set up click listeners
        setupClickListeners()

        // Observe authentication state
        observeAuthState()
    }

    private fun setupClickListeners() {
        // Register button
        registerButton.setOnClickListener {
            if (validateInputs()) {
                val name = nameEditText.text.toString().trim()
                val email = emailEditText.text.toString().trim()
                val password = passwordEditText.text.toString().trim()

                // Attempt registration
                authViewModel.registerUser(email, password, name)
            }
        }

        // Login text
        loginText.setOnClickListener {
            findNavController().navigate(R.id.action_registerFragment_to_loginFragment)
        }
    }

    private fun validateInputs(): Boolean {
        var isValid = true

        // Validate name
        val name = nameEditText.text.toString().trim()
        if (name.isEmpty()) {
            nameInputLayout.error = "Name is required"
            isValid = false
        } else {
            nameInputLayout.error = null
        }

        // Validate email
        val email = emailEditText.text.toString().trim()
        if (email.isEmpty()) {
            emailInputLayout.error = "Email is required"
            isValid = false
        } else if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            emailInputLayout.error = "Enter a valid email"
            isValid = false
        } else {
            emailInputLayout.error = null
        }

        // Validate password
        val password = passwordEditText.text.toString().trim()
        if (password.isEmpty()) {
            passwordInputLayout.error = "Password is required"
            isValid = false
        } else if (password.length < 6) {
            passwordInputLayout.error = "Password must be at least 6 characters"
            isValid = false
        } else {
            passwordInputLayout.error = null
        }

        // Validate password confirmation
        val confirmPassword = confirmPasswordEditText.text.toString().trim()
        if (confirmPassword.isEmpty()) {
            confirmPasswordInputLayout.error = "Please confirm your password"
            isValid = false
        } else if (confirmPassword != password) {
            confirmPasswordInputLayout.error = getString(R.string.passwords_dont_match)
            isValid = false
        } else {
            confirmPasswordInputLayout.error = null
        }

        return isValid
    }

    private fun observeAuthState() {
        authViewModel.authState.observe(viewLifecycleOwner) { authState ->
            when (authState) {
                is AuthViewModel.AuthState.Loading -> {
                    showLoading(true)
                }
                is AuthViewModel.AuthState.Authenticated -> {
                    showLoading(false)
                    Toast.makeText(requireContext(), R.string.register_success, Toast.LENGTH_SHORT).show()
                    // Navigation handled by activity
                }
                is AuthViewModel.AuthState.Unauthenticated -> {
                    showLoading(false)
                }
                is AuthViewModel.AuthState.Error -> {
                    showLoading(false)
                    Toast.makeText(requireContext(), authState.message, Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun showLoading(isLoading: Boolean) {
        if (isLoading) {
            progressBar.visibility = View.VISIBLE
            registerButton.isEnabled = false
            loginText.isEnabled = false
        } else {
            progressBar.visibility = View.GONE
            registerButton.isEnabled = true
            loginText.isEnabled = true
        }
    }
}