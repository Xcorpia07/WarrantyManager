package com.warrantymanager

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.warrantymanager.databinding.ItemInvoiceBinding

class InvoiceAdapter(private val invoices: List<Invoice>) : RecyclerView.Adapter<InvoiceAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemInvoiceBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val invoice = invoices[position]
        holder.bind(invoice)
    }

    override fun getItemCount(): Int {
        return invoices.size
    }

    inner class ViewHolder(private val binding: ItemInvoiceBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(invoice: Invoice) {
            binding.textViewProviderName.text = invoice.providerName
            binding.textViewAmount.text = invoice.amount.toString()
            binding.textViewDate.text = invoice.date.toString()
        }
    }
}