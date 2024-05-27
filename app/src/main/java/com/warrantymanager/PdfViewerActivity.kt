package com.warrantymanager

import android.os.Bundle
import android.util.Log
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.warrantymanager.databinding.ActivityPdfViewerBinding
import com.warrantymanager.databinding.LoadingViewBinding
import es.voghdev.pdfviewpager.library.RemotePDFViewPager
import es.voghdev.pdfviewpager.library.adapter.PDFPagerAdapter
import es.voghdev.pdfviewpager.library.remote.DownloadFile

class PdfViewerActivity : AppCompatActivity() , DownloadFile.Listener {

    private lateinit var binding: ActivityPdfViewerBinding
    private lateinit var loadingViewBinding: LoadingViewBinding
    private lateinit var adapter: PDFPagerAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPdfViewerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        loadingViewBinding = LoadingViewBinding.inflate(layoutInflater)

        window.setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE)

        val fileUrl = intent.getStringExtra("fileUrl")
        Log.i("fileUrlInString", fileUrl ?: "")

        fileUrl?.let {
            val remotePDFViewPager = RemotePDFViewPager(this, it, this)
            binding.root.addView(remotePDFViewPager,
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
        }
    }

    override fun onSuccess(url: String, destinationPath: String) {
        adapter = PDFPagerAdapter(this, destinationPath)
        binding.pdfViewPager.adapter = adapter
        hideLoadingView()
    }

    override fun onFailure(e: Exception) {
        Log.e("PDFDownloadError", "Error downloading PDF: ${e.message}")
        Toast.makeText(this, "Error al descargar el PDF", Toast.LENGTH_SHORT).show()
        hideLoadingView()
    }

    override fun onProgressUpdate(progress: Int, total: Int) {
        if (loadingViewBinding.root.parent == null) {
            showLoadingView()
        }
        Log.d("PDFDownloadProgress", "Progress: $progress/$total")
    }

    override fun onDestroy() {
        super.onDestroy()
        (binding.pdfViewPager.adapter as? PDFPagerAdapter)?.close()
    }

    private fun showLoadingView() {
        if (loadingViewBinding.root.parent != null) {
            (loadingViewBinding.root.parent as? ViewGroup)?.removeView(loadingViewBinding.root)
        }
        addContentView(loadingViewBinding.root, ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
    }

    private fun hideLoadingView() {
        if (loadingViewBinding.root.parent != null) {
            (loadingViewBinding.root.parent as? ViewGroup)?.removeView(loadingViewBinding.root)
        }
    }
}

