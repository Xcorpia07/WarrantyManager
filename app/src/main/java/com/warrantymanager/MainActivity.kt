package com.warrantymanager

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.warrantymanager.databinding.ActivityMainBinding


class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()

        val currentUser = auth.currentUser
        if (currentUser != null) {
            binding.textViewUserEmail.text = currentUser.email
        } else {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }

        binding.buttonLogout.setOnClickListener { logout() }
        binding.buttonInvoices.setOnClickListener {
            startActivity(Intent(this, InvoicesActivity::class.java))
        }
    }

    private fun logout() {
        auth.signOut()
        startActivity(Intent(this, LoginActivity::class.java))
        finish()
    }
}