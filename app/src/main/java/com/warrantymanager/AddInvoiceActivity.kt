package com.warrantymanager

import android.app.AlertDialog
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.AdapterView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.warrantymanager.databinding.ActivityAddInvoiceBinding
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class AddInvoiceActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAddInvoiceBinding
    private var selectedDate: Long = System.currentTimeMillis()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddInvoiceBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.textViewPurchaseDate.setOnClickListener{
            showDatePicker()
        }

        binding.spinnerWarrantyPeriod.prompt = getString(R.string.hint_warranty_period)
        binding.spinnerWarrantyPeriod.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                updateWarrantyDate(position)
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                // No se seleccionó ningún periodo de garantía
            }
        }

        binding.buttonAttachDocument.setOnClickListener {
            showAttachmentOptions()
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
            binding.textViewPurchaseDate.text = dateString
            updateWarrantyDate(binding.spinnerWarrantyPeriod.selectedItemPosition)
        }

        datePicker.show(supportFragmentManager, "DatePicker")
    }

    private fun updateWarrantyDate(warrantyPeriodIndex: Int) {
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = selectedDate

        when (warrantyPeriodIndex) {
            0 -> calendar.add(Calendar.MONTH, 6)
            1 -> calendar.add(Calendar.YEAR, 1)
            2 -> calendar.add(Calendar.YEAR, 2)
            3 -> calendar.add(Calendar.YEAR, 3)
        }

        val warrantyDateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        val warrantyDateString = warrantyDateFormat.format(calendar.time)
        binding.textViewWarrantyDate.text = "Fecha de garantía: $warrantyDateString"
    }

    private fun showAttachmentOptions() {
        val options = arrayOf("Adjuntar archivo", "Adjuntar imagen", "Tomar foto")
        AlertDialog.Builder(this)
            .setTitle("Seleccionar opción")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> attachFile()
                    1 -> attachImage()
                    2 -> takePicture()
                }
            }
            .show()
    }

    private fun attachFile() {
        // Lógica para adjuntar un archivo PDF desde el almacenamiento interno
        // Puedes usar Intent.ACTION_GET_CONTENT para abrir el selector de archivos
        // y obtener la URI del archivo seleccionado
    }

    private fun attachImage() {
        // Lógica para adjuntar una imagen desde la galería del teléfono
        // Puedes usar Intent.ACTION_PICK y MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        // para abrir la galería y obtener la URI de la imagen seleccionada
    }

    private fun takePicture() {
        // Lógica para tomar una foto con la cámara del teléfono
        // Puedes usar Intent(MediaStore.ACTION_IMAGE_CAPTURE) para abrir la cámara
        // y obtener la imagen capturada
    }

    private fun saveInvoiceToFirestore(invoice: Invoice) {
        val userId = FirebaseAuth.getInstance().currentUser?.uid
        if (userId != null) {
            val db = FirebaseFirestore.getInstance()
            val userInvoicesCollection = db.collection("users").document(userId).collection("invoices")

            userInvoicesCollection.add(invoice)
                .addOnSuccessListener { documentReference ->
                    val invoiceId = documentReference.id
                    Log.d("AddInvoiceActivity", "Invoice added with ID: $invoiceId")

                    val updatedInvoice = invoice.copy(id = invoiceId)

                    documentReference.set(updatedInvoice)
                        .addOnSuccessListener {
                            Log.d("AddInvoiceActivity", "Invoice ID saved in the document")
                            finish()
                        }
                        .addOnFailureListener { exception ->
                            Log.e("AddInvoiceActivity", "Error saving invoice ID: ", exception)
                            showErrorMessage("Error al guardar el ID de la factura: ${exception.message}")
                        }
                }
                .addOnFailureListener { exception ->
                    Log.e("AddInvoiceActivity", "Error adding invoice: ", exception)
                    showErrorMessage("Error al guardar la factura: ${exception.message}")
                }
        } else {
            Log.e("AddInvoiceActivity", "No user is authenticated")
            showErrorMessage("No hay un usuario autenticado")
        }
    }

    private fun saveInvoice() {
        val manufacturer = binding.editTextManufacturer.text.toString().trim()
        val productName = binding.editTextProductName.text.toString().trim()
        val price = binding.editTextPrice.text.toString().toDoubleOrNull() ?: 0.0
        val supplier = binding.editTextSupplier.text.toString().trim()

        // Obtener la fecha de compra y la fecha de garantía

        if (manufacturer.isNotBlank() && productName.isNotBlank() && price > 0.0 && supplier.isNotBlank()) {
            val purchaseDate = Date(selectedDate)
            val warrantyPeriodIndex = binding.spinnerWarrantyPeriod.selectedItemPosition
            val warrantyDate = getWarrantyDate(purchaseDate, warrantyPeriodIndex)

            val invoice = Invoice(
                manufacturer = manufacturer,
                productName = productName,
                price = price,
                supplier = supplier,
                purchaseDate = purchaseDate,
                warrantyDate = warrantyDate,
            )
            saveInvoiceToFirestore(invoice)
        } else {
            showErrorMessage("Por favor, completa todos los campos obligatorios.")
        }
    }

    private fun getWarrantyDate(purchaseDate: Date, warrantyPeriodIndex: Int): Date {
        val calendar = Calendar.getInstance()
        calendar.time = purchaseDate

        when (warrantyPeriodIndex) {
            0 -> calendar.add(Calendar.MONTH, 6)
            1 -> calendar.add(Calendar.YEAR, 1)
            2 -> calendar.add(Calendar.YEAR, 2)
            3 -> calendar.add(Calendar.YEAR, 3)
        }

        return calendar.time
    }

    private fun showErrorMessage(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}