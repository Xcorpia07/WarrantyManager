package com.warrantymanager

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.firebase.firestore.FirebaseFirestore
import com.warrantymanager.databinding.ActivityAddInvoiceBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class AddInvoiceActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAddInvoiceBinding
    private var selectedDate: Long = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddInvoiceBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.editTextDate.setOnClickListener{
            showDatePicker()
        }
        binding.buttonSaveInvoice.setOnClickListener {
            saveInvoice()
        }
    }
    private fun showDatePicker() {
        val currentDate = selectedDate
        val datePicker = MaterialDatePicker.Builder.datePicker()
            .setTitleText("Selecciona una fecha")
            .setSelection(currentDate)
            .build()

        datePicker.addOnPositiveButtonClickListener { selectedDateMillis ->
            selectedDate = selectedDateMillis
            val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
            val dateString = dateFormat.format(Date(selectedDateMillis))
            binding.editTextDate.setText(dateString)
        }

        datePicker.show(supportFragmentManager, "DatePicker")
    }

    private fun saveInvoiceToFirestore(invoice: Invoice) {
        val db = FirebaseFirestore.getInstance()
        val invoicesCollection = db.collection("invoices")

        invoicesCollection.add(invoice)
            .addOnSuccessListener { documentReference ->
                val invoiceId = documentReference.id
                Log.d("AddInvoiceActivity", "Invoice added with ID: ${invoiceId}")
                val updateInvoiceId = invoice.copy(id=invoiceId)

                documentReference.set(updateInvoiceId)
                    .addOnSuccessListener {
                        Log.d("AddInvoiceActivity", "Invoice ID saved in the document")
                        finish()
                    }
                    .addOnFailureListener { exception ->
                        Log.e("AddInvoiceActivity", "Error saving invoice ID: ", exception)
                        showErrorMessage("Error al guardar el ID de la factura: ${exception.message}")
                    }
                finish()
            }
            .addOnFailureListener { exception ->
                Log.e("AddInvoiceActivity", "Error adding invoice: ", exception)
                showErrorMessage("Error al guardar la factura: ${exception.message}")
            }
    }

    private fun saveInvoice() {
        val providerName = binding.editTextProviderName.text.toString().trim()
        val amount = binding.editTextAmount.text.toString().toDoubleOrNull() ?: 0.0
        val imageUrl = binding.editTextImageUrl.text.toString().trim()

        if (providerName.isNotBlank() && amount > 0.0 && selectedDate != 0L) {
            val date = Date(selectedDate)
            val invoice = Invoice(providerName = providerName, amount = amount, date = date, imageUrl = imageUrl)
            saveInvoiceToFirestore(invoice)
        } else {
            showErrorMessage("Por favor, ingresa un nombre de proveedor, un monto válido y selecciona una fecha.")
        }
    }

    private fun showErrorMessage(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}