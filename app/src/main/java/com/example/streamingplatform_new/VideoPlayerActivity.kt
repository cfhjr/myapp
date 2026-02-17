package com.example.streamingplatform_new

import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.widget.FrameLayout
import android.widget.MediaController
import android.widget.ProgressBar
import android.widget.Toast
import android.widget.VideoView
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.floatingactionbutton.FloatingActionButton

class VideoPlayerActivity : AppCompatActivity() {

    private var resizeMode = 0 // 0: Fit, 1: Zoom/Fill
    private lateinit var videoView: VideoView
    private lateinit var videoContainer: FrameLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_video_player)

        // Enable immersive full-screen mode for Pixel 6 and other modern displays
        hideSystemUI()

        val videoUrl = intent.getStringExtra("VIDEO_URL")
        videoView = findViewById(R.id.video_view)
        videoContainer = findViewById(R.id.video_container)
        val progressBar = findViewById<ProgressBar>(R.id.progress_bar)
        val fabResize = findViewById<FloatingActionButton>(R.id.fab_resize)

        if (videoUrl != null) {
            val uri = Uri.parse(videoUrl)
            videoView.setVideoURI(uri)

            val mediaController = MediaController(this)
            mediaController.setAnchorView(videoContainer)
            videoView.setMediaController(mediaController)

            videoView.setOnPreparedListener {
                progressBar.visibility = View.GONE
                // Initial auto-scaling based on display
                applyScaleMode()
                videoView.start()
            }

            videoView.setOnErrorListener { _, _, _ ->
                progressBar.visibility = View.GONE
                Toast.makeText(this, "Error playing video", Toast.LENGTH_SHORT).show()
                false
            }

            fabResize.setOnClickListener {
                resizeMode = (resizeMode + 1) % 2
                applyScaleMode()
            }

        } else {
            Toast.makeText(this, "Video URL not found", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    private fun hideSystemUI() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.setDecorFitsSystemWindows(false)
            window.insetsController?.let {
                it.hide(WindowInsets.Type.statusBars() or WindowInsets.Type.navigationBars())
                it.systemBarsBehavior = WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
        } else {
            @Suppress("DEPRECATION")
            window.decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_FULLSCREEN
                    or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                    or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                    or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN)
        }
    }

    private fun applyScaleMode() {
        val params = videoView.layoutParams as FrameLayout.LayoutParams
        if (resizeMode == 1) {
            // Zoom to Fill (maintains aspect ratio but fills the screen)
            // Note: Standard VideoView doesn't support centerCrop easily without custom subclassing.
            // We'll use MATCH_PARENT for both which usually stretches in VideoView, 
            // but is the closest "Fill" option without a custom view.
            params.width = ViewGroup.LayoutParams.MATCH_PARENT
            params.height = ViewGroup.LayoutParams.MATCH_PARENT
            params.gravity = android.view.Gravity.CENTER
            Toast.makeText(this, "Mode: Zoom Fill", Toast.LENGTH_SHORT).show()
        } else {
            // Fit to Screen (Default letterboxing)
            params.width = ViewGroup.LayoutParams.MATCH_PARENT
            params.height = ViewGroup.LayoutParams.WRAP_CONTENT
            params.gravity = android.view.Gravity.CENTER
            Toast.makeText(this, "Mode: Fit to Screen", Toast.LENGTH_SHORT).show()
        }
        videoView.layoutParams = params
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            hideSystemUI()
        }
    }
}
