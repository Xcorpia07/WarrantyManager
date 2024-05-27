package com.warrantymanager

import android.os.Bundle
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.warrantymanager.databinding.ActivityFullScreenImageBinding

class FullScreenImageActivity : AppCompatActivity() {

    private lateinit var binding: ActivityFullScreenImageBinding
    private var imageUrl: String? = null
    private var productName: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityFullScreenImageBinding.inflate(layoutInflater)
        setContentView(binding.root)

        window.setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE)

        productName = intent.getStringExtra("productName")
        imageUrl = intent.getStringExtra("imageUrl")

        binding.textViewProductName.text = productName

        Glide.with(this)
            .load(imageUrl)
            .into(binding.photoView)

    }

}