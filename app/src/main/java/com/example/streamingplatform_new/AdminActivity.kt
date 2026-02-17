package com.example.streamingplatform_new

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.PopupMenu
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.bumptech.glide.Glide
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.navigation.NavigationView
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class AdminActivity : AppCompatActivity() {

    private lateinit var webView: WebView
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var rvVideos: RecyclerView
    private lateinit var swipeRefresh: SwipeRefreshLayout
    private lateinit var adapter: VideoManagementAdapter
    private var showAll = true

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin)

        val userName = intent.getStringExtra("USER_NAME") ?: "Admin"
        val userEmail = intent.getStringExtra("USER_EMAIL") ?: "admin@admin.com"

        drawerLayout = findViewById(R.id.drawer_layout)
        val toolbar = findViewById<MaterialToolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        toolbar.title = "Admin Dashboard"

        val toggle = ActionBarDrawerToggle(
            this, drawerLayout, toolbar,
            android.R.string.ok, android.R.string.cancel
        )
        drawerLayout.addDrawerListener(toggle)
        toggle.syncState()

        val navView = findViewById<NavigationView>(R.id.nav_view)
        
        // Update Nav Header with name and email
        val headerView = navView.getHeaderView(0)
        val tvName = headerView.findViewById<TextView>(R.id.tv_admin_name)
        val tvEmail = headerView.findViewById<TextView>(R.id.tv_admin_email)
        tvName.text = userName
        tvEmail.text = userEmail

        webView = findViewById(R.id.admin_webview)
        rvVideos = findViewById(R.id.rv_admin_videos)
        swipeRefresh = findViewById(R.id.swipe_refresh)

        webView.settings.javaScriptEnabled = true
        webView.settings.domStorageEnabled = true
        webView.webViewClient = WebViewClient()

        rvVideos.layoutManager = LinearLayoutManager(this)
        adapter = VideoManagementAdapter(mutableListOf())
        rvVideos.adapter = adapter

        swipeRefresh.setOnRefreshListener { loadVideos() }

        navView.setNavigationItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.nav_dashboard -> {
                    showAll = true
                    webView.visibility = View.GONE
                    rvVideos.visibility = View.VISIBLE
                    toolbar.title = "Admin Dashboard"
                    loadVideos()
                }
                R.id.nav_all_videos -> {
                    showAll = true
                    webView.visibility = View.GONE
                    rvVideos.visibility = View.VISIBLE
                    toolbar.title = "All Videos"
                    loadVideos()
                }
                R.id.nav_pending_denied -> {
                    showAll = false
                    webView.visibility = View.GONE
                    rvVideos.visibility = View.VISIBLE
                    toolbar.title = "Pending & Denied"
                    loadVideos()
                }
                R.id.nav_logout -> {
                    val intent = Intent(this, LoginActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(intent)
                    finish()
                }
            }
            drawerLayout.closeDrawer(GravityCompat.START)
            true
        }

        loadVideos()
    }

    private fun loadVideos() {
        swipeRefresh.isRefreshing = true
        val call = ApiConfig.getApiService().getAllVideos()

        call.enqueue(object : Callback<List<VideoData>> {
            override fun onResponse(call: Call<List<VideoData>>, response: Response<List<VideoData>>) {
                swipeRefresh.isRefreshing = false
                if (response.isSuccessful) {
                    var videos = response.body() ?: emptyList()
                    if (!showAll) {
                        videos = videos.filter { 
                            val status = it.status.lowercase().trim()
                            status == "pending" || status == "denied" || status == "deny" || status == "denny"
                        }
                    }
                    adapter.updateData(videos)
                } else {
                    Toast.makeText(this@AdminActivity, "Failed to load videos", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<List<VideoData>>, t: Throwable) {
                swipeRefresh.isRefreshing = false
                Toast.makeText(this@AdminActivity, "Network error", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun updateVideoStatus(videoId: Int, status: String) {
        ApiConfig.getApiService().approveVideo(videoId, status).enqueue(object : Callback<BaseResponse> {
            override fun onResponse(call: Call<BaseResponse>, response: Response<BaseResponse>) {
                if (response.isSuccessful && response.body()?.success == true) {
                    Toast.makeText(this@AdminActivity, "Video $status", Toast.LENGTH_SHORT).show()
                    loadVideos()
                } else {
                    Toast.makeText(this@AdminActivity, "Action failed", Toast.LENGTH_SHORT).show()
                }
            }
            override fun onFailure(call: Call<BaseResponse>, t: Throwable) {
                Toast.makeText(this@AdminActivity, "Error occurred", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun deleteVideo(videoId: Int) {
        ApiConfig.getApiService().deleteVideo(videoId).enqueue(object : Callback<BaseResponse> {
            override fun onResponse(call: Call<BaseResponse>, response: Response<BaseResponse>) {
                if (response.isSuccessful && response.body()?.success == true) {
                    Toast.makeText(this@AdminActivity, "Video deleted", Toast.LENGTH_SHORT).show()
                    loadVideos()
                } else {
                    Toast.makeText(this@AdminActivity, "Delete failed", Toast.LENGTH_SHORT).show()
                }
            }
            override fun onFailure(call: Call<BaseResponse>, t: Throwable) {
                Toast.makeText(this@AdminActivity, "Error occurred", Toast.LENGTH_SHORT).show()
            }
        })
    }

    override fun onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START)
        } else if (webView.visibility == View.VISIBLE && webView.canGoBack()) {
            webView.goBack()
        } else {
            @Suppress("DEPRECATION")
            super.onBackPressed()
        }
    }

    inner class VideoManagementAdapter(private val videos: MutableList<VideoData>) : RecyclerView.Adapter<VideoManagementAdapter.ViewHolder>() {

        fun updateData(newVideos: List<VideoData>) {
            videos.clear()
            videos.addAll(newVideos)
            notifyDataSetChanged()
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_video_approval, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val video = videos[position]
            holder.tvTitle.text = video.title
            holder.tvDesc.text = video.description
            
            val uploaderText = if (!video.uploaderName.isNullOrEmpty()) "By: ${video.uploaderName}" else "Uploader ID: ${video.userId}"
            holder.tvInfo.text = uploaderText
            
            val statusClean = video.status.lowercase().trim()
            holder.tvStatus.text = "Status: ${video.status.replaceFirstChar { it.uppercase() }}"
            when (statusClean) {
                "pending" -> holder.tvStatus.setTextColor(holder.itemView.context.getColor(android.R.color.holo_orange_dark))
                "approved" -> holder.tvStatus.setTextColor(holder.itemView.context.getColor(android.R.color.holo_green_dark))
                "denied", "deny", "denny" -> holder.tvStatus.setTextColor(holder.itemView.context.getColor(android.R.color.holo_red_dark))
                else -> holder.tvStatus.setTextColor(holder.itemView.context.getColor(android.R.color.darker_gray))
            }

            val videoUrl = ApiConfig.getVideoUrl(video.videoPath)
            val thumbnailUrl = ApiConfig.getThumbnailUrl(video.thumbnailPath)
            val imageToLoad = if (!video.thumbnailPath.isNullOrEmpty()) thumbnailUrl else videoUrl

            Glide.with(holder.itemView.context)
                .load(imageToLoad)
                .frame(1000000)
                .placeholder(android.R.drawable.ic_menu_gallery)
                .error(android.R.drawable.ic_menu_report_image)
                .centerCrop()
                .into(holder.ivThumbnail)

            holder.btnMenu.setOnClickListener { view ->
                val popup = PopupMenu(holder.itemView.context, view)
                popup.menuInflater.inflate(R.menu.video_action_menu, popup.menu)
                
                if (statusClean == "approved") popup.menu.findItem(R.id.action_approve).isVisible = false
                if (statusClean == "denied" || statusClean == "deny" || statusClean == "denny") popup.menu.findItem(R.id.action_deny).isVisible = false
                
                popup.setOnMenuItemClickListener { item ->
                    when (item.itemId) {
                        R.id.action_approve -> {
                            updateVideoStatus(video.id, "approved")
                            true
                        }
                        R.id.action_deny -> {
                            updateVideoStatus(video.id, "denied")
                            true
                        }
                        R.id.action_delete -> {
                            deleteVideo(video.id)
                            true
                        }
                        else -> false
                    }
                }
                popup.show()
            }

            val playListener = View.OnClickListener {
                val intent = Intent(holder.itemView.context, VideoPlayerActivity::class.java)
                val vUrl = ApiConfig.getVideoUrl(video.videoPath)
                intent.putExtra("VIDEO_URL", vUrl)
                holder.itemView.context.startActivity(intent)
            }
            holder.ivThumbnail.setOnClickListener(playListener)
            holder.tvTitle.setOnClickListener(playListener)
        }

        override fun getItemCount() = videos.size

        inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val ivThumbnail: ImageView = view.findViewById(R.id.iv_thumbnail)
            val tvTitle: TextView = view.findViewById(R.id.tv_video_title)
            val tvStatus: TextView = view.findViewById(R.id.tv_video_status)
            val tvDesc: TextView = view.findViewById(R.id.tv_video_desc)
            val tvInfo: TextView = view.findViewById(R.id.tv_uploader_info)
            val btnMenu: ImageButton = view.findViewById(R.id.btn_video_menu)
        }
    }
}
