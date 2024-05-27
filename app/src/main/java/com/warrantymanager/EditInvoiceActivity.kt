package com.warrantymanager

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.warrantymanager.databinding.ActivityEditInvoiceBinding


class EditInvoiceActivity : AppCompatActivity() {

    private lateinit var binding: ActivityEditInvoiceBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEditInvoiceBinding.inflate(layoutInflater)
        setContentView(binding.root)


    }

}