package com.example.studyhub

import android.Manifest
import android.annotation.SuppressLint
import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.webkit.CookieManager
import android.webkit.URLUtil
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class MainActivity : AppCompatActivity() {
    private lateinit var webView: WebView

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            Toast.makeText(this, "Permission granted", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "Permission denied. Notifications might not show.", Toast.LENGTH_LONG).show()
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        
        webView = findViewById(R.id.webView)
        
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        checkNotificationPermission()

        webView.settings.javaScriptEnabled = true
        webView.settings.domStorageEnabled = true
        webView.settings.allowFileAccess = true
        webView.settings.allowContentAccess = true
        
        webView.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView, url: String): Boolean {
                if (url.endsWith(".pdf")) {
                    val intent = Intent(Intent.ACTION_VIEW)
                    intent.data = Uri.parse(url)
                    try {
                        startActivity(intent)
                    } catch (e: Exception) {
                        view.loadUrl("https://docs.google.com/gview?embedded=true&url=$url")
                    }
                    return true
                }
                return false
            }
        }

        webView.setDownloadListener { url, userAgent, contentDisposition, mimetype, _ ->
            try {
                val request = DownloadManager.Request(Uri.parse(url))
                val cookies = CookieManager.getInstance().getCookie(url)
                
                request.addRequestHeader("cookie", cookies)
                request.addRequestHeader("User-Agent", userAgent)
                request.setDescription("Downloading file from StudyHub...")
                request.setTitle(URLUtil.guessFileName(url, contentDisposition, mimetype))
                
                // This ensures the download is visible in the notification bar and shows completion
                request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                
                request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, URLUtil.guessFileName(url, contentDisposition, mimetype))
                
                val dm = getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
                dm.enqueue(request)
                
                Toast.makeText(applicationContext, "Download Started...", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(applicationContext, "Download failed: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }

        webView.loadUrl("https://studyhubb.great-site.net/")

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (webView.canGoBack()) {
                    webView.goBack()
                } else {
                    isEnabled = false
                    onBackPressedDispatcher.onBackPressed()
                }
            }
        })
    }

    private fun checkNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }
}