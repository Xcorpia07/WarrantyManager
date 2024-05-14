package com.warrantymanager

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import com.warrantymanager.databinding.ActivityRegisterBinding

class RegisterActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRegisterBinding
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()

        binding.registerButton.setOnClickListener { registerUser() }

    }

    private fun registerUser() {
        val name = binding.nameEditText.text.toString().trim()
        val email = binding.emailEditText.text.toString().trim()
        val password = binding.passwordEditText.text.toString().trim()
        val confirmPassword = binding.confirmPasswordEditText.text.toString().trim()

        if (name.isEmpty()) {
            binding.nameEditText.error = getString(R.string.error_empty_name)
            return
        }

        if (email.isEmpty()) {
            binding.emailEditText.error = getString(R.string.error_empty_email)
            return
        }

        if (password.isEmpty()) {
            binding.passwordEditText.error = getString(R.string.error_empty_password)
            return
        }

        if (confirmPassword.isEmpty()) {
            binding.confirmPasswordEditText.error = getString(R.string.error_empty_confirm_password)
            return
        }

        if (password != confirmPassword) {
            binding.confirmPasswordEditText.error = getString(R.string.error_password_mismatch)
            return
        }

        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    val user = auth.currentUser
                    val profileUpdates = UserProfileChangeRequest.Builder()
                        .setDisplayName(name)
                        .build()

                    user?.updateProfile(profileUpdates)
                        ?.addOnCompleteListener { updateTask ->
                            if (updateTask.isSuccessful) {
                                startActivity(Intent(this, MainActivity::class.java))
                                finish()
                            }
                        }
                } else {
                    val errorMessage = task.exception?.message ?: getString(R.string.error_unknown)
                    Toast.makeText(this, getString(R.string.error_registration_failed, errorMessage), Toast.LENGTH_SHORT
                    ).show()
                }
            }
    }
}