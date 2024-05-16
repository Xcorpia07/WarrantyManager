package com.warrantymanager

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.firestore.DocumentReference
import com.warrantymanager.databinding.ItemInvoiceBinding
import java.text.SimpleDateFormat
import java.util.Locale

class InvoiceAdapter(
    private val invoiceRefs: List<DocumentReference>,
    private val onItemClickListener: (DocumentReference) -> Unit
) : RecyclerView.Adapter<InvoiceAdapter.ViewHolder>() {

    val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemInvoiceBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val invoiceRef = invoiceRefs[position]
        holder.bind(invoiceRef)
        holder.itemView.setOnClickListener { onItemClickListener(invoiceRef) }
    }

    override fun getItemCount(): Int {
        return invoiceRefs.size
    }

    inner class ViewHolder(private val binding: ItemInvoiceBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(invoiceRef: DocumentReference) {
            invoiceRef.get().addOnSuccessListener { documentSnapshot ->
                val invoice = documentSnapshot.toObject(Invoice::class.java)
                if (invoice != null) {
                    binding.textViewProductName.text = invoice.productName
                    binding.textViewPrice.text = invoice.price.toString()
                    binding.textViewPurchaseDate.text = dateFormat.format(invoice.purchaseDate)
                    binding.textViewWarrantyDate.text = dateFormat.format(invoice.warrantyDate)
                }
            }
            itemView.setOnClickListener { onItemClickListener(invoiceRef) }
        }
    }
}