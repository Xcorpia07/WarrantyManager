package com.warrantymanager

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.warrantymanager.databinding.ActivityInvoicesBinding

class InvoicesActivity : AppCompatActivity() {

    private lateinit var binding: ActivityInvoicesBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityInvoicesBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Configurar el RecyclerView y el adaptador para mostrar las facturas

        binding.fabAddInvoice.setOnClickListener {
            // Abrir la pantalla para agregar una nueva factura
        }
    }
}