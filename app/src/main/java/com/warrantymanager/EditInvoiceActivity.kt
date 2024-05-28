package com.warrantymanager

import android.app.AlertDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.webkit.MimeTypeMap
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.storage
import com.warrantymanager.databinding.ActivityEditInvoiceBinding
import com.warrantymanager.databinding.LoadingViewBinding
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class EditInvoiceActivity : AppCompatActivity() {

    private lateinit var binding: ActivityEditInvoiceBinding
    private lateinit var loadingViewBinding: LoadingViewBinding
    private val REQUEST_ATTACH_PDF = 1
    private val REQUEST_ATTACH_IMAGE = 2
    private val REQUEST_TAKE_PHOTO = 3
    private val REQUEST_ATTACH_PRODUCT_IMAGE = 4
    private val REQUEST_TAKE_PRODUCT_PHOTO = 5
    private var selectedInvoicePdfUri: Uri? = null
    private var selectedInvoiceImageUri: Uri? = null
    private var selectedProductImageUri: Uri? = null
    private var currentInvoicePhotoPath: String = ""
    private var currentProductPhotoPath: String = ""


    private var selectedDate: Long = System.currentTimeMillis()
    private val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

    private lateinit var resultLauncher: ActivityResultLauncher<Intent>
    private var requestCode: Int = 0

    private lateinit var invoice: Invoice
    private lateinit var invoiceEditPath: String

    private var isProductImageRemoved = false
    private var isInvoiceFileRemoved = false



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEditInvoiceBinding.inflate(layoutInflater)
        setContentView(binding.root)

        loadingViewBinding = LoadingViewBinding.inflate(layoutInflater)

        invoiceEditPath = intent.getStringExtra("invoiceEditPath")!!
        fetchInvoiceData()

        binding.textViewPurchaseDate.setOnClickListener {
            showDatePicker()
        }

        binding.buttonReplaceProductImage.setOnClickListener {
            showAddOptions()
        }

        binding.buttonAddProductImage.setOnClickListener {
            showAddOptions()
        }

        binding.buttonRemoveProductImage.setOnClickListener {
            removeProductImage()
        }

        binding.buttonReplaceInvoiceFile.setOnClickListener {
            showAttachmentOptions()
        }

        binding.buttonAddInvoiceFile.setOnClickListener {
            showAttachmentOptions()
        }

        binding.buttonRemoveInvoiceFile.setOnClickListener {
            removeInvoiceFile()
        }

        binding.buttonUpdateInvoice.setOnClickListener {
            updateInvoice()
        }

        resultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            handleActivityResult(result.resultCode, requestCode, result.data)
        }
    }

    private fun fetchInvoiceData() {
        showLoadingView()

        val db = FirebaseFirestore.getInstance()
        val invoiceRef = db.document(invoiceEditPath)
        invoiceRef.get()
            .addOnSuccessListener { documentSnapshot ->
                invoice = documentSnapshot.toObject(Invoice::class.java)!!
                bindInvoiceData()
                hideLoadingView()
            }
            .addOnFailureListener { exception ->
                Log.e("EditInvoiceActivity", "Error getting invoice details: ", exception)
                hideLoadingView()
                showErrorMessage("Error al obtener los detalles de la factura: ${exception.message}")
            }
    }

    private fun bindInvoiceData() {
        binding.editTextProductName.setText(invoice.productName)
        binding.editTextManufacturer.setText(invoice.manufacturer)
        binding.editTextPrice.setText(invoice.price.toString())
        binding.editTextSupplier.setText(invoice.supplier)
        binding.textViewPurchaseDate.setText(dateFormat.format(invoice.purchaseDate))
        binding.autoCompleteWarrantyPeriod.setText(invoice.warrantyPeriod, false)

        selectedDate = invoice.purchaseDate.time

        if (invoice.productImageUrl.isNotEmpty()) {
            Glide.with(this)
                .load(invoice.productImageUrl)
                .transform(RoundedCorners(20))
                .into(binding.imageViewProduct)
            binding.buttonAddProductImage.visibility = View.GONE
            binding.buttonReplaceProductImage.visibility = View.VISIBLE
            binding.buttonRemoveProductImage.visibility = View.VISIBLE
        } else {
            binding.buttonAddProductImage.visibility = View.VISIBLE
            binding.buttonReplaceProductImage.visibility = View.GONE
            binding.buttonRemoveProductImage.visibility = View.GONE
        }

        if (invoice.invoiceFileUrl.isNotEmpty()) {
            val storageRef = Firebase.storage.getReferenceFromUrl(invoice.invoiceFileUrl)
            storageRef.metadata.addOnSuccessListener { metadata ->
                val contentType = metadata.contentType
                if (contentType == "application/pdf") {
                    binding.imageViewInvoiceFile.setImageResource(R.drawable.ic_placeholder_pdf)
                } else {
                    Glide.with(this)
                        .load(invoice.invoiceFileUrl)
                        .transform(RoundedCorners(20))
                        .into(binding.imageViewInvoiceFile)
                }
            }.addOnFailureListener { exception ->
                Log.e("EditInvoiceActivity", "Error getting file metadata: ", exception)
            }
            binding.buttonAddInvoiceFile.visibility = View.GONE
            binding.buttonReplaceInvoiceFile.visibility = View.VISIBLE
            binding.buttonRemoveInvoiceFile.visibility = View.VISIBLE
        } else {
            binding.buttonAddInvoiceFile.visibility = View.VISIBLE
            binding.buttonReplaceInvoiceFile.visibility = View.GONE
            binding.buttonRemoveInvoiceFile.visibility = View.GONE
        }
    }

    private fun handleActivityResult(resultCode: Int, requestCode: Int, data: Intent?) {
        if (resultCode == RESULT_OK) {
            when (requestCode) {
                REQUEST_ATTACH_PDF -> {
                    selectedInvoicePdfUri = data?.data
                    Toast.makeText(this, "PDF seleccionado", Toast.LENGTH_SHORT).show()
                }
                REQUEST_ATTACH_IMAGE, REQUEST_ATTACH_PRODUCT_IMAGE -> {
                    val uri = data?.data
                    if (requestCode == REQUEST_ATTACH_IMAGE) {
                        selectedInvoiceImageUri = uri
                        binding.imageViewInvoiceFile.setImageURI(selectedInvoiceImageUri)
                        binding.buttonAddInvoiceFile.visibility = View.GONE
                        binding.buttonReplaceInvoiceFile.visibility = View.VISIBLE
                        binding.buttonRemoveInvoiceFile.visibility = View.VISIBLE
                        Toast.makeText(this, "Imagen seleccionada", Toast.LENGTH_SHORT).show()
                    } else {
                        selectedProductImageUri = uri
                        binding.imageViewProduct.setImageURI(selectedProductImageUri)
                        binding.buttonAddProductImage.visibility = View.GONE
                        binding.buttonReplaceProductImage.visibility = View.VISIBLE
                        binding.buttonRemoveProductImage.visibility = View.VISIBLE
                        Toast.makeText(this, "Imagen del producto seleccionada", Toast.LENGTH_SHORT).show()
                    }
                }
                REQUEST_TAKE_PHOTO, REQUEST_TAKE_PRODUCT_PHOTO -> {
                    val uri = Uri.fromFile(File(if (requestCode == REQUEST_TAKE_PHOTO) currentInvoicePhotoPath else currentProductPhotoPath))
                    if (requestCode == REQUEST_TAKE_PHOTO) {
                        selectedInvoiceImageUri = uri
                        binding.imageViewInvoiceFile.setImageURI(selectedInvoiceImageUri)
                        binding.buttonAddInvoiceFile.visibility = View.GONE
                        binding.buttonReplaceInvoiceFile.visibility = View.VISIBLE
                        binding.buttonRemoveInvoiceFile.visibility = View.VISIBLE
                        Toast.makeText(this, "Foto tomada", Toast.LENGTH_SHORT).show()
                    } else {
                        selectedProductImageUri = uri
                        binding.imageViewProduct.setImageURI(selectedProductImageUri)
                        binding.buttonAddProductImage.visibility = View.GONE
                        binding.buttonReplaceProductImage.visibility = View.VISIBLE
                        binding.buttonRemoveProductImage.visibility = View.VISIBLE
                        Toast.makeText(this, "Foto del producto tomada", Toast.LENGTH_SHORT).show()
                    }
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
            binding.textViewPurchaseDate.setText(dateString)
        }

        datePicker.show(supportFragmentManager, "DatePicker")
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
        requestCode = REQUEST_ATTACH_PDF
        resultLauncher.launch(intent)
    }

    private fun attachImage(requestCode: Int) {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        this.requestCode = requestCode
        resultLauncher.launch(intent)
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
                    this.requestCode = requestCode
                    resultLauncher.launch(takePictureIntent)
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

    private fun removeProductImage() {
        selectedProductImageUri = null
        binding.imageViewProduct.setImageResource(R.drawable.ic_product_placeholder)
        binding.buttonAddProductImage.visibility = View.VISIBLE
        binding.buttonReplaceProductImage.visibility = View.GONE
        binding.buttonRemoveProductImage.visibility = View.GONE
        isProductImageRemoved = true
    }

    private fun removeInvoiceFile() {
        selectedInvoicePdfUri = null
        selectedInvoiceImageUri = null
        binding.buttonAddInvoiceFile.visibility = View.VISIBLE
        binding.buttonReplaceInvoiceFile.visibility = View.GONE
        binding.buttonRemoveInvoiceFile.visibility = View.GONE
        isInvoiceFileRemoved = true
    }

    private fun updateInvoiceToFirestore(invoice: Invoice) {
        val db = FirebaseFirestore.getInstance()
        val invoiceRef = db.document(invoiceEditPath)

        invoiceRef.set(invoice)
            .addOnSuccessListener {
                Log.d("EditInvoiceActivity", "Invoice updated successfully")
                finish()
            }
            .addOnFailureListener { exception ->
                Log.e("EditInvoiceActivity", "Error updating invoice: ", exception)
                showErrorMessage("Error al actualizar la factura: ${exception.message}")
            }
    }

    private fun updateInvoice() {
        showLoadingView()

        val manufacturer = binding.editTextManufacturer.text.toString().trim()
        val productName = binding.editTextProductName.text.toString().trim()
        val price = binding.editTextPrice.text.toString().toDoubleOrNull() ?: 0.0
        val supplier = binding.editTextSupplier.text.toString().trim()

        if (manufacturer.isNotBlank() && productName.isNotBlank() && price > 0.0 && supplier.isNotBlank()) {
            val purchaseDate = Date(selectedDate)
            val warrantyPeriodIndex = binding.autoCompleteWarrantyPeriod.getText().toString()
            val warrantyDate = getWarrantyDate(purchaseDate, warrantyPeriodIndex)
            val warrantyPeriod = binding.autoCompleteWarrantyPeriod.getText().toString()

            val updatedInvoice = invoice.copy(
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

            when {
                productImageUri != null -> {
                    uploadFileToFirebase(productImageUri, "productImages") { productImageUrl ->
                        updatedInvoice.productImageUrl = productImageUrl
                        handleInvoiceFileUpdate(updatedInvoice, invoiceFileUri)
                    }
                }
                isProductImageRemoved -> {
                    deleteFileFromFirebase(invoice.productImageUrl) {
                        updatedInvoice.productImageUrl = ""
                        handleInvoiceFileUpdate(updatedInvoice, invoiceFileUri)
                    }
                }
                else -> {
                    updatedInvoice.productImageUrl = invoice.productImageUrl
                    handleInvoiceFileUpdate(updatedInvoice, invoiceFileUri)
                }
            }
        } else {
            hideLoadingView()
            showErrorMessage("Por favor, completa todos los campos obligatorios.")
        }
    }

    private fun handleInvoiceFileUpdate(updatedInvoice: Invoice, invoiceFileUri: Uri?) {
        when {
            invoiceFileUri != null -> {
                uploadFileToFirebase(invoiceFileUri, "invoiceFiles") { invoiceFileUrl ->
                    updatedInvoice.invoiceFileUrl = invoiceFileUrl
                    updateInvoiceToFirestore(updatedInvoice)
                }
            }
            isInvoiceFileRemoved -> {
                deleteFileFromFirebase(invoice.invoiceFileUrl) {
                    updatedInvoice.invoiceFileUrl = ""
                    updateInvoiceToFirestore(updatedInvoice)
                }
            }
            else -> {
                updatedInvoice.invoiceFileUrl = invoice.invoiceFileUrl
                updateInvoiceToFirestore(updatedInvoice)
            }
        }
    }

    private fun deleteFileFromFirebase(fileUrl: String, onSuccess: () -> Unit) {
        if (fileUrl.isNotEmpty()) {
            val storageRef = Firebase.storage.getReferenceFromUrl(fileUrl)
            storageRef.delete()
                .addOnSuccessListener {
                    onSuccess()
                }
                .addOnFailureListener { exception ->
                    Log.e("EditInvoiceActivity", "Error deleting file from Firebase", exception)
                    showErrorMessage("Error al eliminar el archivo")
                }
        } else {
            onSuccess()
        }
    }

    private fun uploadFileToFirebase(fileUri: Uri, directory: String, onSuccess: (String) -> Unit) {
        val userId = FirebaseAuth.getInstance().currentUser?.uid
        if (userId != null) {
            val storageRef = Firebase.storage.reference
            val fileRef = storageRef.child("users/$userId/$directory/${System.currentTimeMillis()}.${getFileExtension(fileUri)}")
            fileRef.putFile(fileUri)
                .addOnSuccessListener {
                    fileRef.downloadUrl.addOnSuccessListener { uri ->
                        onSuccess(uri.toString())
                    }
                }
                .addOnFailureListener { exception ->
                    Log.e("EditInvoiceActivity", "Error al subir el archivo", exception)
                    showErrorMessage("Error al subir el archivo")
                }
        } else {
            Log.e("EditInvoiceActivity", "No user is authenticated")
            showErrorMessage("No hay un usuario autenticado")
        }
    }

    private fun getFileExtension(uri: Uri): String? {
        val contentResolver = contentResolver
        val mimeTypeMap = MimeTypeMap.getSingleton()
        return mimeTypeMap.getExtensionFromMimeType(contentResolver.getType(uri))
    }

    private fun getWarrantyDate(purchaseDate: Date, warrantyPeriod: String): Date {
        val calendar = Calendar.getInstance()
        calendar.time = purchaseDate

        when (warrantyPeriod) {
            "6 meses" -> calendar.add(Calendar.MONTH, 6)
            "1 año" -> calendar.add(Calendar.YEAR, 1)
            "2 años" -> calendar.add(Calendar.YEAR, 2)
            "3 años" -> calendar.add(Calendar.YEAR, 3)
        }
        return calendar.time
    }

    private fun showLoadingView() {
        if (loadingViewBinding.root.parent != null) {
            (loadingViewBinding.root.parent as? ViewGroup)?.removeView(loadingViewBinding.root)
        }
        addContentView(loadingViewBinding.root, ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
    }

    private fun hideLoadingView() {
        loadingViewBinding.root.let { view ->
            (view.parent as? ViewGroup)?.removeView(view)
        }
    }

    private fun showErrorMessage(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}