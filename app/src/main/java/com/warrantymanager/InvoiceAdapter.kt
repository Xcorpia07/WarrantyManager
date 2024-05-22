package com.warrantymanager

import android.view.LayoutInflater
import android.view.RoundedCorner
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.google.firebase.firestore.DocumentReference
import com.warrantymanager.databinding.ItemInvoiceBinding
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
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
                    binding.textViewManufacturer.text = invoice.manufacturer
                    binding.textViewProductName.text = invoice.productName
                    binding.textViewPurchaseDate.text = "Fecha de compra: ${dateFormat.format(invoice.purchaseDate)}"
                    binding.textViewWarrantyDate.text = "Garantía hasta: ${dateFormat.format(invoice.warrantyDate)}"

                    val currentDate = Date()
                    val remainingMonths = getRemainingWarrantyMonths(currentDate, invoice.warrantyDate)
                    binding.textViewRemainingWarranty.text = "Quedan: $remainingMonths meses"

                    Glide.with(binding.root.context)
                        .load(invoice.productImageUrl)
                        .into(binding.imageViewProduct)
                }
            }
            itemView.setOnClickListener { onItemClickListener(invoiceRef) }
        }

        private fun getRemainingWarrantyMonths(currentDate: Date, warrantyDate: Date): Int {
            val calendar = Calendar.getInstance()
            calendar.time = currentDate
            val currentMonth = calendar.get(Calendar.MONTH)
            val currentYear = calendar.get(Calendar.YEAR)

            calendar.time = warrantyDate
            val warrantyMonth = calendar.get(Calendar.MONTH)
            val warrantyYear = calendar.get(Calendar.YEAR)

            val monthsDiff = (warrantyYear - currentYear) * 12 + (warrantyMonth - currentMonth)
            return if (monthsDiff <= 0) 0 else monthsDiff
        }
    }
}