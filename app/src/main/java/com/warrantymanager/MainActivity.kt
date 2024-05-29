package com.warrantymanager

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.PopupMenu
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageException
import com.warrantymanager.databinding.ActivityMainBinding

import android.content.DialogInterface
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import androidx.core.content.ContextCompat
import com.google.android.material.color.MaterialColors
import com.google.firebase.auth.FirebaseAuthRecentLoginRequiredException

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private var auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val currentUser = auth.currentUser
    private lateinit var invoiceAdapter: InvoiceAdapter


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

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

        setSupportActionBar(binding.toolbar);
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.invoicesMenuItem -> {
                true
            }
            R.id.orderMenuItem -> {
                showOrderMenu(findViewById(R.id.orderMenuItem))

                true
            }
            R.id.userMenuItem -> {
                showUserMenu(findViewById(R.id.userMenuItem))
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun showUserMenu(view: View) {
        val popupMenu = PopupMenu(this, view)
        popupMenu.menuInflater.inflate(R.menu.user_options_menu, popupMenu.menu)
        popupMenu.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.logout -> {
                    logout()
                    true
                }
                R.id.deleteAccount -> {
                    showDeleteAccountConfirmationDialog()
                    true
                }
                else -> false
            }
        }
        popupMenu.show()
    }

    private fun showOrderMenu(view: View) {
        val popupMenu = PopupMenu(this, view)
        popupMenu.menuInflater.inflate(R.menu.sort_menu, popupMenu.menu)
        popupMenu.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.sort_by_date -> {
                    sortInvoices(InvoiceSortOrder.DATE)
                    true
                }
                R.id.sort_by_name -> {
                    sortInvoices(InvoiceSortOrder.NAME)
                    true
                }
                R.id.sort_by_price -> {
                    sortInvoices(InvoiceSortOrder.PRICE)
                    true
                }
                else -> false
            }
        }
        popupMenu.show()
    }

    private fun sortInvoices(sortOrder: InvoiceSortOrder) {
        val userId = FirebaseAuth.getInstance().currentUser?.uid
        if (userId != null) {
            val db = FirebaseFirestore.getInstance()
            val userInvoicesCollection = db.collection("users").document(userId).collection("invoices")

            val query = when (sortOrder) {
                InvoiceSortOrder.DATE -> userInvoicesCollection.orderBy("purchaseDate", Query.Direction.DESCENDING)
                InvoiceSortOrder.NAME -> userInvoicesCollection.orderBy("productName", Query.Direction.ASCENDING)
                InvoiceSortOrder.PRICE -> userInvoicesCollection.orderBy("price", Query.Direction.DESCENDING)
            }

            query.get().addOnSuccessListener { snapshot ->
                val invoiceRefs = snapshot.documents.map { it.reference }
                setupInvoiceAdapter(invoiceRefs)
            }.addOnFailureListener { exception ->
                Log.e("InvoicesActivity", "Error sorting invoices: ", exception)
            }
        } else {
            Log.e("InvoicesActivity", "No user is authenticated")
        }
    }

    private fun getInvoices() {
        val userId = FirebaseAuth.getInstance().currentUser?.uid
        if (userId != null) {
            val db = FirebaseFirestore.getInstance()
            val userInvoicesCollection =
                db.collection("users").document(userId).collection("invoices")

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

    private fun showDeleteAccountConfirmationDialog() {
        val alertDialogBuilder = MaterialAlertDialogBuilder(this)
            .setTitle(getString(R.string.delete_account_title))
            .setMessage(getString(R.string.delete_account_message))
            .setIcon(R.drawable.ic_warning)
            .setPositiveButton(getString(R.string.delete_account_button)) { _, _ ->
                deleteAccount()
            }
            .setNegativeButton(getString(R.string.cancel_button), null)

        val alertDialog = alertDialogBuilder.create()
        alertDialog.show()
    }

    private fun deleteAccount() {
        if (currentUser != null) {
            val userId = currentUser.uid
            val db = FirebaseFirestore.getInstance()
            val storage = FirebaseStorage.getInstance()
            val userStorageRef = storage.reference.child("users/$userId")
            val userDocRef = db.collection("users").document(userId)

            userStorageRef.delete()
                .addOnSuccessListener {
                    Log.d("DeleteAccount", "User storage deleted successfully")
                    deleteUserDataAndAccount(userDocRef)
                }
                .addOnFailureListener { exception ->
                    if (exception is StorageException && exception.errorCode == StorageException.ERROR_OBJECT_NOT_FOUND) {
                        Log.d("DeleteAccount", "User storage does not exist")
                        deleteUserDataAndAccount(userDocRef)
                    } else {
                        Log.e("DeleteAccount", "Error deleting user storage: ", exception)
                    }
                }
        } else {
            Log.e("DeleteAccount", "No user is authenticated")
        }
    }

    private fun deleteUserDataAndAccount(userDocRef: DocumentReference) {
        userDocRef.delete()
            .addOnSuccessListener {
                Log.d("DeleteAccount", "User data deleted successfully")
                currentUser?.delete()
                    ?.addOnCompleteListener { deleteTask ->
                        if (deleteTask.isSuccessful) {
                            Log.d("DeleteAccount", "Account deleted successfully")
                            startActivity(Intent(this, LoginActivity::class.java))
                            finish()
                        } else {
                            if (deleteTask.exception is FirebaseAuthRecentLoginRequiredException) {
                                // Manejar el error FirebaseAuthRecentLoginRequiredException
                                showReauthenticationDialog()
                            } else {
                                Log.e("DeleteAccount", "Error deleting account: ", deleteTask.exception)
                            }
                        }
                    }
            }
            .addOnFailureListener { exception ->
                Log.e("DeleteAccount", "Error deleting user data: ", exception)
            }
    }

    private fun showReauthenticationDialog() {
        MaterialAlertDialogBuilder(this)
            .setTitle(getString(R.string.reauthentication_required_title))
            .setMessage(getString(R.string.reauthentication_required_message))
            .setIcon(R.drawable.ic_warning)
            .setPositiveButton(getString(R.string.reauthenticate_button)) { _, _ ->
                startActivity(Intent(this, LoginActivity::class.java))
            }
            .setNegativeButton(getString(R.string.cancel_button), null)
            .show()
    }


    private fun logout() {
        auth.signOut()
        startActivity(Intent(this, LoginActivity::class.java))
        finish()
    }

}