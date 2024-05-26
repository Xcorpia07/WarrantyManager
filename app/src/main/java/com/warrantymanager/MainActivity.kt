package com.warrantymanager

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore
import com.warrantymanager.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var invoiceAdapter: InvoiceAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()

        val currentUser = auth.currentUser
        if (currentUser != null) {
            //binding.textViewUserEmail.text = currentUser.email
        } else {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }

        binding.recyclerViewInvoices.layoutManager = LinearLayoutManager(this)

        getInvoices()

        binding.fabAddInvoice.setOnClickListener {
            startActivity(Intent(this, AddInvoiceActivity::class.java))
        }
    }

    private fun getInvoices() {
        val userId = FirebaseAuth.getInstance().currentUser?.uid
        if (userId != null) {
            val db = FirebaseFirestore.getInstance()
            val userInvoicesCollection = db.collection("users").document(userId).collection("invoices")

            userInvoicesCollection.addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("InvoicesActivity", "Error getting invoices: ", error)
                    return@addSnapshotListener
                }

                val invoiceRefs = snapshot?.documents?.map { it.reference } ?: emptyList()
                setupInvoiceAdapter(invoiceRefs)
            }
        } else {
            Log.e("InvoicesActivity", "No user is authenticated")
        }
    }

    private fun setupInvoiceAdapter(invoiceRefs: List<DocumentReference>) {
        val onItemClickListener: (DocumentReference) -> Unit = { invoiceRef ->
            val intent = Intent(this, InvoiceDetailsActivity::class.java)
            intent.putExtra("invoicePath", invoiceRef.path)
            startActivity(intent)
        }

        invoiceAdapter = InvoiceAdapter(invoiceRefs, onItemClickListener)
        binding.recyclerViewInvoices.adapter = invoiceAdapter
    }

    private fun logout() {
        auth.signOut()
        startActivity(Intent(this, LoginActivity::class.java))
        finish()
    }

}