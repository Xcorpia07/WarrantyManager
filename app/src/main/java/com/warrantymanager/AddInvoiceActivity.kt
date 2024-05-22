package com.warrantymanager

import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.webkit.MimeTypeMap
import android.widget.AdapterView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.storage
import com.warrantymanager.databinding.ActivityAddInvoiceBinding
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class AddInvoiceActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAddInvoiceBinding
    private val REQUEST_ATTACH_PDF = 1
    private val REQUEST_ATTACH_IMAGE = 2
    private val REQUEST_TAKE_PHOTO = 3
    private val REQUEST_ATTACH_PRODUCT_IMAGE = 4
    private val REQUEST_TAKE_PRODUCT_PHOTO = 5
    private var selectedInvoicePdfUri: Uri? = null
    private var selectedInvoiceImageUri: Uri? = null
    private var currentInvoicePhotoPath: String = ""
    private var selectedProductImageUri: Uri? = null
    private var currentProductPhotoPath: String = ""


    private var selectedDate: Long = System.currentTimeMillis()
    private val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddInvoiceBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val currentDate = Date(selectedDate)
        binding.textViewPurchaseDate.text = dateFormat.format(currentDate)
        binding.textViewPurchaseDate.setOnClickListener{
            showDatePicker()
        }

        binding.spinnerWarrantyPeriod.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                updateWarrantyDate(position)
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
            }
        }

        binding.buttonAddImage.setOnClickListener {
            showAddOptions()
        }

        binding.buttonAttachDocument.setOnClickListener {
            showAttachmentOptions()
        }

        binding.buttonSaveInvoice.setOnClickListener {
            saveInvoice()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == RESULT_OK) {
            when (requestCode) {
                REQUEST_ATTACH_PDF -> {
                    selectedInvoicePdfUri = data?.data
                    Toast.makeText(this, "PDF seleccionado", Toast.LENGTH_SHORT).show()
                }
                REQUEST_ATTACH_IMAGE -> {
                    selectedInvoiceImageUri = data?.data
                    Toast.makeText(this, "Imagen seleccionada", Toast.LENGTH_SHORT).show()
                }
                REQUEST_TAKE_PHOTO -> {
                    selectedInvoiceImageUri = Uri.fromFile(File(currentInvoicePhotoPath))
                    Toast.makeText(this, "Foto tomada", Toast.LENGTH_SHORT).show()
                }
                REQUEST_ATTACH_PRODUCT_IMAGE -> {
                    selectedProductImageUri = data?.data
                    binding.imageViewProduct.setImageURI(selectedProductImageUri)
                    Toast.makeText(this, "Imagen del producto seleccionada", Toast.LENGTH_SHORT).show()
                }
                REQUEST_TAKE_PRODUCT_PHOTO -> {
                    selectedProductImageUri = Uri.fromFile(File(currentProductPhotoPath))
                    binding.imageViewProduct.setImageURI(selectedProductImageUri)
                    Toast.makeText(this, "Foto del producto tomada", Toast.LENGTH_SHORT).show()
                }
            }
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

        val warrantyDateString = dateFormat.format(calendar.time)
        binding.textViewWarrantyDate.text = warrantyDateString
    }

    private fun showAttachmentOptions() {
        val options = arrayOf("Adjuntar archivo", "Adjuntar imagen", "Tomar foto")
        AlertDialog.Builder(this)
            .setTitle("Seleccionar opción")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> attachPdf()
                    1 -> attachImage(REQUEST_ATTACH_IMAGE)
                    2 -> takePicture(REQUEST_TAKE_PHOTO)
                }
            }
            .show()
    }

    private fun showAddOptions() {
        val options = arrayOf("Adjuntar imagen", "Tomar foto")
        AlertDialog.Builder(this)
            .setTitle("Seleccionar opción")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> attachImage(REQUEST_ATTACH_PRODUCT_IMAGE)
                    1 -> takePicture(REQUEST_TAKE_PRODUCT_PHOTO)
                }
            }
            .show()
    }


    private fun attachPdf() {
        val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
            type = "application/pdf"
            addCategory(Intent.CATEGORY_OPENABLE)
        }
        startActivityForResult(intent, REQUEST_ATTACH_PDF)
    }

    private fun attachImage(requestCode: Int) {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        startActivityForResult(intent, requestCode)
    }

    private fun takePicture(requestCode: Int) {
        Intent(MediaStore.ACTION_IMAGE_CAPTURE).also { takePictureIntent ->
            takePictureIntent.resolveActivity(packageManager)?.also {
                val photoFile: File? = try {
                    when (requestCode) {
                        REQUEST_TAKE_PHOTO -> createImageFile { currentInvoicePhotoPath = it }
                        REQUEST_TAKE_PRODUCT_PHOTO -> createImageFile { currentProductPhotoPath = it }
                        else -> null
                    }
                } catch (ex: IOException) {
                    null
                }
                photoFile?.also {
                    val photoURI: Uri = FileProvider.getUriForFile(
                        this,
                        "com.warrantymanager.fileprovider",
                        it
                    )
                    takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI)
                    startActivityForResult(takePictureIntent, requestCode)
                }
            }
        }
    }

    private fun createImageFile(path: (String) -> Unit): File {
        val timeStamp: String = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val storageDir: File? = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        return File.createTempFile(
            "JPEG_${timeStamp}_",
            ".jpg",
            storageDir
        ).apply {
            path(absolutePath)
        }
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

        if (manufacturer.isNotBlank() && productName.isNotBlank() && price > 0.0 && supplier.isNotBlank()) {
            val purchaseDate = Date(selectedDate)
            val warrantyPeriodIndex = binding.spinnerWarrantyPeriod.selectedItemPosition
            val warrantyDate = getWarrantyDate(purchaseDate, warrantyPeriodIndex)
            val warrantyPeriod = binding.spinnerWarrantyPeriod.selectedItem.toString()


            val invoice = Invoice(
                manufacturer = manufacturer,
                productName = productName,
                price = price,
                supplier = supplier,
                purchaseDate = purchaseDate,
                warrantyDate = warrantyDate,
                warrantyPeriod = warrantyPeriod
            )

            val productImageUri = selectedProductImageUri
            val invoiceFileUri = selectedInvoicePdfUri ?: selectedInvoiceImageUri

            if (productImageUri != null) {
                uploadFileToFirebase(productImageUri, "productImages") { productImageUrl ->
                    invoice.productImageUrl = productImageUrl
                    if (invoiceFileUri != null) {
                        uploadFileToFirebase(invoiceFileUri, "invoiceFiles") { invoiceFileUrl ->
                            invoice.invoiceFileUrl = invoiceFileUrl
                            saveInvoiceToFirestore(invoice)
                        }
                    } else {
                        saveInvoiceToFirestore(invoice)
                    }
                }
            } else {
                if (invoiceFileUri != null) {
                    uploadFileToFirebase(invoiceFileUri, "invoiceFiles") { invoiceFileUrl ->
                        invoice.invoiceFileUrl = invoiceFileUrl
                        saveInvoiceToFirestore(invoice)
                    }
                } else {
                    saveInvoiceToFirestore(invoice)
                }
            }


        } else {
            showErrorMessage("Por favor, completa todos los campos obligatorios.")
        }
    }

    private fun uploadFileToFirebase(fileUri: Uri, directory: String, onSuccess: (String) -> Unit) {
        val userId = FirebaseAuth.getInstance().currentUser?.uid
        if (userId != null) {
            val storageRef = Firebase.storage.reference
            val fileRef = storageRef.child("users/$userId/$directory/${System.currentTimeMillis()}.${getFileExtension(fileUri)}")
            fileRef.putFile(fileUri)
                .addOnSuccessListener { taskSnapshot ->
                    fileRef.downloadUrl.addOnSuccessListener { uri ->
                        onSuccess(uri.toString())
                    }
                }
                .addOnFailureListener { exception ->
                    Log.e("AddInvoiceActivity", "Error al subir el archivo", exception)
                    showErrorMessage("Error al subir el archivo")
                }
        } else {
            Log.e("AddInvoiceActivity", "No user is authenticated")
            showErrorMessage("No hay un usuario autenticado")
        }
    }

    private fun getFileExtension(uri: Uri): String? {
        val contentResolver = contentResolver
        val mimeTypeMap = MimeTypeMap.getSingleton()
        return mimeTypeMap.getExtensionFromMimeType(contentResolver.getType(uri))
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