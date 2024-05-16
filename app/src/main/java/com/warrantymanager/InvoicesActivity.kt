package com.warrantymanager

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.firestore.FirebaseFirestore
import com.warrantymanager.databinding.ActivityInvoicesBinding

class InvoicesActivity : AppCompatActivity() {

    private lateinit var binding: ActivityInvoicesBinding
    private lateinit var adapter: InvoiceAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityInvoicesBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.recyclerViewInvoices.layoutManager = LinearLayoutManager(this)
        getInvoices()

        binding.fabAddInvoice.setOnClickListener {
            startActivity(Intent(this, AddInvoiceActivity::class.java))
        }
    }

    private fun getInvoices() {
        val db = FirebaseFirestore.getInstance()
        val invoicesCollection = db.collection("invoices")

        invoicesCollection.addSnapshotListener { snapshot, error ->
            if (error != null) {
                Log.e("InvoicesActivity", "Error getting invoices: ", error)
                return@addSnapshotListener
            }

            val invoices = mutableListOf<Invoice>()
            for (document in snapshot?.documents ?: emptyList()) {
                val invoice = document.toObject(Invoice::class.java)
                if (invoice != null) {
                    invoices.add(invoice)
                }
            }
            updateInvoiceList(invoices)
        }
    }

    private fun updateInvoiceList(invoices: List<Invoice>) {
        val adapter = InvoiceAdapter(invoices)
        binding.recyclerViewInvoices.adapter = adapter
    }
}