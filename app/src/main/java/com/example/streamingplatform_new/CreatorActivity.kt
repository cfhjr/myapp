package com.example.streamingplatform_new

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.bumptech.glide.Glide
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.tabs.TabLayout
import com.google.android.material.textfield.TextInputEditText
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.File
import java.io.FileOutputStream

class CreatorActivity : AppCompatActivity() {

    private lateinit var rvVideos: RecyclerView
    private lateinit var adapter: CreatorVideoAdapter
    private lateinit var tabLayout: TabLayout
    private lateinit var swipeRefresh: SwipeRefreshLayout
    private var currentStatus = "approved"
    private var userId: Int = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_creator)

        userId = intent.getIntExtra("USER_ID", -1)
        val userName = intent.getStringExtra("USER_NAME") ?: "Creator"
        
        val toolbar = findViewById<com.google.android.material.appbar.MaterialToolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        toolbar.title = getString(R.string.welcome_user, userName)
        
        // Add logout menu
        toolbar.inflateMenu(R.menu.main_menu)
        toolbar.setOnMenuItemClickListener {
            when (it.itemId) {
                R.id.action_logout -> {
                    logout()
                    true
                }
                else -> false
            }
        }

        rvVideos = findViewById(R.id.rv_creator_videos)
        tabLayout = findViewById(R.id.tab_layout)
        swipeRefresh = findViewById(R.id.swipe_refresh)
        val fabUpload = findViewById<FloatingActionButton>(R.id.fab_upload)

        rvVideos.layoutManager = LinearLayoutManager(this)
        adapter = CreatorVideoAdapter(mutableListOf())
        rvVideos.adapter = adapter

        tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                currentStatus = when (tab?.position) {
                    0 -> "approved"
                    1 -> "denied"
                    else -> "approved"
                }
                loadVideos()
            }
            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })

        swipeRefresh.setOnRefreshListener { loadVideos() }
        fabUpload.setOnClickListener { showUploadDialog() }

        if (userId != -1) loadVideos()
    }

    private fun loadVideos() {
        swipeRefresh.isRefreshing = true
        ApiConfig.getApiService().getMyVideos(userId, currentStatus, 1).enqueue(object : Callback<List<VideoData>> {
            override fun onResponse(call: Call<List<VideoData>>, response: Response<List<VideoData>>) {
                swipeRefresh.isRefreshing = false
                if (response.isSuccessful) {
                    adapter.updateData(response.body() ?: emptyList())
                } else {
                    Log.e("LOAD_VIDEOS", "Error: ${response.code()} ${response.message()}")
                }
            }
            override fun onFailure(call: Call<List<VideoData>>, t: Throwable) {
                swipeRefresh.isRefreshing = false
                Log.e("LOAD_VIDEOS", "Failure: ${t.message}")
            }
        })
    }

    private fun logout() {
        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    private var selectedVideoUri: Uri? = null
    private val videoPickerLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) selectedVideoUri = uri
    }

    private fun showUploadDialog() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_upload_video, null)
        val etTitle = dialogView.findViewById<TextInputEditText>(R.id.et_upload_title)
        val etDesc = dialogView.findViewById<TextInputEditText>(R.id.et_upload_description)
        val btnSelectVideo = dialogView.findViewById<Button>(R.id.btn_select_video)

        btnSelectVideo.setOnClickListener { videoPickerLauncher.launch("video/*") }

        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.upload_video)
            .setView(dialogView)
            .setPositiveButton(R.string.upload) { _, _ ->
                val title = etTitle.text.toString().trim()
                if (title.isNotEmpty() && selectedVideoUri != null) {
                    uploadVideo(title, etDesc.text.toString().trim())
                } else {
                    Toast.makeText(this, "Please select a video and enter a title", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun uploadVideo(title: String, description: String) {
        val uri = selectedVideoUri ?: return
        val videoFile = getFileFromUri(uri) ?: return

        Toast.makeText(this, R.string.uploading, Toast.LENGTH_SHORT).show()

        val userIdRB = userId.toString().toRequestBody("text/plain".toMediaTypeOrNull())
        val categoryIdRB = "1".toRequestBody("text/plain".toMediaTypeOrNull())
        val titleRB = title.toRequestBody("text/plain".toMediaTypeOrNull())
        val descriptionRB = description.toRequestBody("text/plain".toMediaTypeOrNull())
        
        val videoPart = MultipartBody.Part.createFormData(
            "video", 
            videoFile.name, 
            videoFile.asRequestBody("video/mp4".toMediaTypeOrNull())
        )

        ApiConfig.getApiService().uploadVideo(
            userIdRB, 
            categoryIdRB, 
            titleRB, 
            descriptionRB,
            videoPart, 
            null
        ).enqueue(object : Callback<BaseResponse> {
            override fun onResponse(call: Call<BaseResponse>, response: Response<BaseResponse>) {
                if (response.isSuccessful) {
                    val body = response.body()
                    if (body?.success == true) {
                        Toast.makeText(this@CreatorActivity, R.string.upload_successful, Toast.LENGTH_SHORT).show()
                        loadVideos()
                    } else {
                        val msg = body?.message ?: "Unknown server error"
                        Log.e("UPLOAD_FAILED", "Server message: $msg")
                        Toast.makeText(this@CreatorActivity, getString(R.string.upload_failed, msg), Toast.LENGTH_LONG).show()
                    }
                } else {
                    val errorBody = response.errorBody()?.string() ?: ""
                    Log.e("UPLOAD_ERROR", "Code: ${response.code()}, Body: $errorBody")
                    Toast.makeText(this@CreatorActivity, R.string.server_error, Toast.LENGTH_LONG).show()
                }
            }

            override fun onFailure(call: Call<BaseResponse>, t: Throwable) {
                Log.e("UPLOAD_FAILURE", t.message ?: "Network error")
                Toast.makeText(this@CreatorActivity, getString(R.string.network_error, t.message ?: ""), Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun getFileFromUri(uri: Uri): File? {
        val file = File(cacheDir, "upload_${System.currentTimeMillis()}.mp4")
        return try {
            contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(file).use { output -> input.copyTo(output) }
            }
            file
        } catch (e: Exception) { null }
    }

    inner class CreatorVideoAdapter(private val videos: MutableList<VideoData>) : RecyclerView.Adapter<CreatorVideoAdapter.ViewHolder>() {
        fun updateData(newVideos: List<VideoData>) {
            videos.clear()
            videos.addAll(newVideos)
            notifyDataSetChanged()
        }
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_video_creator, parent, false)
            return ViewHolder(view)
        }
        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val video = videos[position]
            holder.tvTitle.text = video.title
            holder.tvDate.text = getString(R.string.uploaded_on, video.createdAt ?: "N/A")
            holder.tvLikes.text = video.likeCount.toString()
            holder.tvComments.text = video.commentCount.toString()
            
            // Set and color status
            val statusClean = video.status.lowercase().trim()
            holder.tvStatus.text = video.status.uppercase()
            when (statusClean) {
                "approved" -> {
                    holder.tvStatus.setTextColor(getColor(android.R.color.holo_green_dark))
                }
                "denied", "deny", "denny" -> {
                    holder.tvStatus.setTextColor(getColor(android.R.color.holo_red_dark))
                }
                else -> {
                    holder.tvStatus.setTextColor(getColor(android.R.color.holo_orange_dark))
                }
            }
            
            val context = holder.itemView.context
            val vidUrl = ApiConfig.getVideoUrl(video.videoPath)
            val thumbUrl = ApiConfig.getThumbnailUrl(video.thumbnailPath)
            
            Glide.with(context)
                .load(if (thumbUrl.isNotEmpty()) thumbUrl else vidUrl)
                .frame(1000000)
                .placeholder(android.R.drawable.ic_menu_gallery)
                .centerCrop()
                .into(holder.ivThumbnail)

            holder.itemView.setOnClickListener {
                if (vidUrl.isNotEmpty()) {
                    val intent = Intent(context, VideoPlayerActivity::class.java)
                    intent.putExtra("VIDEO_URL", vidUrl)
                    context.startActivity(intent)
                } else {
                    Toast.makeText(context, R.string.video_url_not_found, Toast.LENGTH_SHORT).show()
                }
            }
        }
        override fun getItemCount() = videos.size
        inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val ivThumbnail: ImageView = view.findViewById(R.id.iv_thumbnail)
            val tvTitle: TextView = view.findViewById(R.id.tv_video_title)
            val tvDate: TextView = view.findViewById(R.id.tv_video_date)
            val tvStatus: TextView = view.findViewById(R.id.tv_video_status)
            val tvLikes: TextView = view.findViewById(R.id.tv_like_count)
            val tvComments: TextView = view.findViewById(R.id.tv_comment_count)
        }
    }
}
