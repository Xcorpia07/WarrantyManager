package com.warrantymanager

import android.app.AlertDialog
import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.StorageException
import com.google.firebase.storage.storage
import com.warrantymanager.databinding.ActivityInvoiceDetailsBinding
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

class InvoiceDetailsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityInvoiceDetailsBinding
    private lateinit var invoice: Invoice
    private val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
    private lateinit var invoiceEditPath: String
    private lateinit var invoicePath: String


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityInvoiceDetailsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        invoicePath = intent.getStringExtra("invoicePath")!!


        val db = FirebaseFirestore.getInstance()
        val invoiceRef = db.document(invoicePath)
        invoiceRef.get()
            .addOnSuccessListener { documentSnapshot ->
                invoice = documentSnapshot.toObject(Invoice::class.java)!!
                bindInvoiceData(dateFormat)
                invoiceEditPath = invoiceRef.path
                setupClickListeners()
            }
            .addOnFailureListener { exception ->
                Log.e("InvoiceDetailsActivity", "Error getting invoice details: ", exception)
            }
    }

    override fun onResume() {
        super.onResume()
        fetchInvoiceData()
    }

    private fun fetchInvoiceData() {
        val db = FirebaseFirestore.getInstance()
        val invoiceRef = db.document(invoicePath)
        invoiceRef.get()
            .addOnSuccessListener { documentSnapshot ->
                invoice = documentSnapshot.toObject(Invoice::class.java)!!
                bindInvoiceData(dateFormat)
            }
            .addOnFailureListener { exception ->
                Log.e("InvoiceDetailsActivity", "Error getting invoice details: ", exception)
            }
    }

    private fun bindInvoiceData(dateFormat: SimpleDateFormat) {
        binding.textViewProductName.text = invoice.productName
        binding.editTextManufacturer.setText(invoice.manufacturer)
        binding.editTextPrice.setText(invoice.price.toString())
        binding.editTextSupplier.setText(invoice.supplier)
        binding.editTextPurchaseDate.setText(dateFormat.format(invoice.purchaseDate))
        binding.editTextWarrantyDate.setText(dateFormat.format(invoice.warrantyDate))
        binding.editTextWarrantyPeriod.setText(invoice.warrantyPeriod)

        val remainingMonths = getRemainingWarrantyMonths(invoice.warrantyDate)
        binding.textViewRemainingWarranty.text = resources.getString(R.string.label_remaining_warranty, remainingMonths)

        Glide.with(this)
            .load(invoice.productImageUrl)
            .transform(RoundedCorners(20))
            .placeholder(R.drawable.ic_product_placeholder)
            .into(binding.imageViewProduct)
    }

    private fun setupClickListeners() {
        binding.imageButtonEdit.setOnClickListener {
            openEditInvoiceActivity()
        }

        binding.imageViewProduct.setOnClickListener {
            showImageFullScreen(invoice.productImageUrl)
        }

        binding.buttonViewInvoice.setOnClickListener {
            showImageFullScreen(invoice.invoiceFileUrl)
        }

        binding.textViewDeleteProduct.setOnClickListener {
            showDeleteConfirmationDialog()
        }
    }

    private fun openEditInvoiceActivity() {
        val intent = Intent(this, EditInvoiceActivity::class.java).apply {
            putExtra("invoiceEditPath", invoiceEditPath)
        }
        startActivity(intent)
    }

    private fun showImageFullScreen(fileUrl: String, productName: String = invoice.productName) {
        if (fileUrl.isNotEmpty()) {
            val storageRef = Firebase.storage.getReferenceFromUrl(fileUrl)
            storageRef.metadata.addOnSuccessListener { metadata ->
                val contentType = metadata.contentType
                if (contentType == "application/pdf") {
                    val intent = Intent(this, PdfViewerActivity::class.java)
                    intent.putExtra("fileUrl", fileUrl)
                    intent.putExtra("productName", productName)
                    startActivity(intent)
                } else {
                    val intent = Intent(this, FullScreenImageActivity::class.java)
                    intent.putExtra("imageUrl", fileUrl)
                    intent.putExtra("productName", productName)
                    startActivity(intent)
                }
            }.addOnFailureListener { exception ->
                Log.e("InvoiceDetailsActivity", "Error getting file metadata: ", exception)
                // Manejar el error al obtener los metadatos del archivo
            }
        }
    }


    private fun showDeleteConfirmationDialog() {
        AlertDialog.Builder(this)
            .setTitle("Eliminar producto")
            .setMessage("¿Estás seguro de que deseas eliminar este producto?")
            .setPositiveButton("Sí") { _, _ ->
                deleteFilesFromStorage()
            }
            .setNegativeButton("No", null)
            .show()
    }

    private fun deleteProduct() {
        val db = FirebaseFirestore.getInstance()
        val invoiceRef = db.document("users/${FirebaseAuth.getInstance().currentUser?.uid}/invoices/${invoice.id}")
        invoiceRef.delete()
            .addOnSuccessListener {
                finish()
            }
            .addOnFailureListener { exception ->
                Log.e("InvoiceDetailsActivity", "Error deleting invoice: ", exception)
                showErrorMessage("Error al eliminar el producto: ${exception.message}")
            }
    }

    private fun deleteFilesFromStorage() {
        val userId = FirebaseAuth.getInstance().currentUser?.uid
        if (userId != null) {

            if (invoice.productImageUrl.isNotEmpty()) {
                val productImageRef = Firebase.storage.getReferenceFromUrl(invoice.productImageUrl)
                productImageRef.delete()
                    .addOnSuccessListener {
                        Log.d("InvoiceDetailsActivity", "Imagen del producto eliminada: ${productImageRef.path}")
                    }
                    .addOnFailureListener { exception ->
                        if (exception is StorageException && exception.errorCode == StorageException.ERROR_OBJECT_NOT_FOUND) {
                            Log.w("InvoiceDetailsActivity", "La imagen del producto no existe en la ruta especificada: ${productImageRef.path}")
                        } else {
                            showErrorMessage("Error al eliminar la imagen del producto: ${exception.message}")
                        }
                    }
            }

            if (invoice.invoiceFileUrl.isNotEmpty()) {
                val invoiceFileRef = Firebase.storage.getReferenceFromUrl(invoice.invoiceFileUrl)
                invoiceFileRef.delete()
                    .addOnSuccessListener {
                        Log.d("InvoiceDetailsActivity", "Archivo de la factura eliminado: ${invoiceFileRef.path}")
                        deleteProduct()
                    }
                    .addOnFailureListener { exception ->
                        if (exception is StorageException && exception.errorCode == StorageException.ERROR_OBJECT_NOT_FOUND) {
                            Log.w("InvoiceDetailsActivity", "El archivo de la factura no existe en la ruta especificada: ${invoiceFileRef.path}")
                        } else {
                            showErrorMessage("Error al eliminar el archivo de la factura: ${exception.message}")
                        }
                    }
            } else {
                deleteProduct()
            }
        } else {
            showErrorMessage("No hay un usuario autenticado")
        }
    }

    private fun showErrorMessage(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    private fun getRemainingWarrantyMonths(warrantyDate: Date): Int {
        val currentDate = Date()
        val diffInMillis = warrantyDate.time - currentDate.time
        val diffInMonths = TimeUnit.MILLISECONDS.toDays(diffInMillis) / 30
        return if (diffInMonths <= 0) 0 else diffInMonths.toInt()
    }
}