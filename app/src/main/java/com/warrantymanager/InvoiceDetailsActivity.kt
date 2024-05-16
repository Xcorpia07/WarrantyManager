package com.warrantymanager

import android.os.Bundle
import android.util.Log
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.firebase.firestore.FirebaseFirestore
import com.warrantymanager.databinding.ActivityInvoiceDetailsBinding
import java.text.SimpleDateFormat
import java.util.Locale

class InvoiceDetailsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityInvoiceDetailsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityInvoiceDetailsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val invoicePath = intent.getStringExtra("invoicePath")!!

        val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

        val db = FirebaseFirestore.getInstance()
        val invoiceRef = db.document(invoicePath)
        invoiceRef.get()
            .addOnSuccessListener { documentSnapshot ->
                val invoice = documentSnapshot.toObject(Invoice::class.java)
                if (invoice != null) {
                    binding.textViewManufacturer.text = invoice.manufacturer
                    binding.textViewProductName.text = invoice.productName
                    binding.textViewPrice.text = "Precio: ${invoice.price}"
                    binding.textViewSupplier.text = invoice.supplier
                    binding.textViewPurchaseDate.text = dateFormat.format(invoice.purchaseDate)
                    binding.textViewWarrantyDate.text = dateFormat.format(invoice.warrantyDate)
                }
            }
            .addOnFailureListener { exception ->
                Log.e("InvoiceDetailsActivity", "Error getting invoice details: ", exception)
            }
    }
}