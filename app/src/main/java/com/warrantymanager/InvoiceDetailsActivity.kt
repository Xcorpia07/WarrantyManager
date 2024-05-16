package com.warrantymanager

import android.os.Bundle
import android.util.Log
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.firebase.firestore.FirebaseFirestore
import com.warrantymanager.databinding.ActivityInvoiceDetailsBinding

class InvoiceDetailsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityInvoiceDetailsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityInvoiceDetailsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val invoicePath = intent.getStringExtra("invoicePath")!!


        val db = FirebaseFirestore.getInstance()
        val invoiceRef = db.document(invoicePath)
        invoiceRef.get()
            .addOnSuccessListener { documentSnapshot ->
                val invoice = documentSnapshot.toObject(Invoice::class.java)
                if (invoice != null) {
                    binding.textViewProviderName.text = invoice.providerName
                    binding.textViewAmount.text = "Amount: ${invoice.amount}"
                    binding.textViewDate.text = "Date: ${invoice.date}"
                    // Cargar la imagen de la factura usando Glide o Picasso
                }
            }
            .addOnFailureListener { exception ->
                Log.e("InvoiceDetailsActivity", "Error getting invoice details: ", exception)
            }
    }
}