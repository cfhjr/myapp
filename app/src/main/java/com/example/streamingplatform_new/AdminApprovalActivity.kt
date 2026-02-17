package com.example.streamingplatform_new

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.PopupMenu
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.bumptech.glide.Glide
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class AdminApprovalActivity : AppCompatActivity() {

    private lateinit var rvVideos: RecyclerView
    private lateinit var swipeRefresh: SwipeRefreshLayout
    private lateinit var adapter: VideoManagementAdapter
    private var showAll = true 

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin_approval)

        showAll = intent.getBooleanExtra("SHOW_ALL", true)

        val toolbar = findViewById<com.google.android.material.appbar.MaterialToolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        toolbar.setNavigationOnClickListener { finish() }
        
        toolbar.title = if (showAll) "All Videos" else "Pending & Denied"
        
        toolbar.inflateMenu(R.menu.admin_video_menu)
        val filterItem = toolbar.menu.findItem(R.id.action_filter)
        filterItem.title = if (showAll) "Show Pending & Denied" else "Show All"

        toolbar.setOnMenuItemClickListener {
            when (it.itemId) {
                R.id.action_filter -> {
                    showAll = !showAll
                    it.title = if (showAll) "Show Pending & Denied" else "Show All"
                    toolbar.title = if (showAll) "All Videos" else "Pending & Denied"
                    loadVideos()
                    true
                }
                else -> false
            }
        }

        rvVideos = findViewById(R.id.rv_pending_videos)
        swipeRefresh = findViewById(R.id.swipe_refresh)

        rvVideos.layoutManager = LinearLayoutManager(this)
        adapter = VideoManagementAdapter(mutableListOf())
        rvVideos.adapter = adapter

        swipeRefresh.setOnRefreshListener { loadVideos() }
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
                        // More robust filtering: trim and handle common variations
                        videos = videos.filter { 
                            val status = it.status.lowercase().trim()
                            status == "pending" || status == "denied" || status == "deny"
                        }
                    }

                    Log.d("AdminApproval", "Loaded ${videos.size} videos (Filter: ${if(showAll) "None" else "Pending/Denied"})")
                    adapter.updateData(videos)
                } else {
                    Toast.makeText(this@AdminApprovalActivity, "Failed to load", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<List<VideoData>>, t: Throwable) {
                swipeRefresh.isRefreshing = false
                Toast.makeText(this@AdminApprovalActivity, "Network error", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun updateVideoStatus(videoId: Int, status: String) {
        ApiConfig.getApiService().approveVideo(videoId, status).enqueue(object : Callback<BaseResponse> {
            override fun onResponse(call: Call<BaseResponse>, response: Response<BaseResponse>) {
                if (response.isSuccessful && response.body()?.success == true) {
                    Toast.makeText(this@AdminApprovalActivity, "Video $status", Toast.LENGTH_SHORT).show()
                    loadVideos()
                }
            }
            override fun onFailure(call: Call<BaseResponse>, t: Throwable) {}
        })
    }

    private fun deleteVideo(videoId: Int) {
        ApiConfig.getApiService().deleteVideo(videoId).enqueue(object : Callback<BaseResponse> {
            override fun onResponse(call: Call<BaseResponse>, response: Response<BaseResponse>) {
                if (response.isSuccessful && response.body()?.success == true) {
                    Toast.makeText(this@AdminApprovalActivity, "Video deleted", Toast.LENGTH_SHORT).show()
                    loadVideos()
                }
            }
            override fun onFailure(call: Call<BaseResponse>, t: Throwable) {}
        })
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
            
            val statusClean = video.status.trim().lowercase()
            holder.tvStatus.text = "Status: ${video.status.replaceFirstChar { it.uppercase() }}"
            when (statusClean) {
                "pending" -> holder.tvStatus.setTextColor(holder.itemView.context.getColor(android.R.color.holo_orange_dark))
                "approved" -> holder.tvStatus.setTextColor(holder.itemView.context.getColor(android.R.color.holo_green_dark))
                "denied", "deny" -> holder.tvStatus.setTextColor(holder.itemView.context.getColor(android.R.color.holo_red_dark))
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
                if (statusClean == "denied" || statusClean == "deny") popup.menu.findItem(R.id.action_deny).isVisible = false
                
                popup.setOnMenuItemClickListener { item ->
                    when (item.itemId) {
                        R.id.action_approve -> { updateVideoStatus(video.id, "approved"); true }
                        R.id.action_deny -> { updateVideoStatus(video.id, "denied"); true }
                        R.id.action_delete -> { deleteVideo(video.id); true }
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
