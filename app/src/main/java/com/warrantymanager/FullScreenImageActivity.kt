package com.warrantymanager

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.github.chrisbanes.photoview.PhotoView
import com.warrantymanager.databinding.ActivityFullScreenImageBinding

class FullScreenImageActivity : AppCompatActivity() {

    private lateinit var binding: ActivityFullScreenImageBinding
    private var imageUrl: String? = null
    private var productName: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityFullScreenImageBinding.inflate(layoutInflater)
        setContentView(binding.root)

        productName = intent.getStringExtra("productName")
        imageUrl = intent.getStringExtra("imageUrl")

        binding.textViewProductName.text = productName

        Glide.with(this)
            .load(imageUrl)
            .into(binding.photoView)

    }

}