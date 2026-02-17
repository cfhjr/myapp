package com.example.streamingplatform_new

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.android.material.appbar.MaterialToolbar
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class MainActivity : AppCompatActivity() {

    private lateinit var rvVideos: RecyclerView
    private lateinit var swipeRefreshLayout: SwipeRefreshLayout
    private lateinit var adapter: VideoAdapter
    private var userId: Int = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        userId = intent.getIntExtra("USER_ID", -1)
        val userName = intent.getStringExtra("USER_NAME") ?: "User"

        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout)
        rvVideos = findViewById(R.id.rv_videos)
        val toolbar = findViewById<MaterialToolbar>(R.id.toolbar)

        toolbar.title = getString(R.string.welcome_user, userName)
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

        rvVideos.layoutManager = LinearLayoutManager(this)
        adapter = VideoAdapter(mutableListOf(), userId)
        rvVideos.adapter = adapter

        swipeRefreshLayout.setOnRefreshListener {
            loadApprovedVideos()
        }

        loadApprovedVideos()
    }

    private fun loadApprovedVideos() {
        swipeRefreshLayout.isRefreshing = true
        Log.d("API_CALL", "Fetching approved videos from: ${ApiConfig.BASE_URL}get_approved_videos.php")
        
        ApiConfig.getApiService().getApprovedVideos().enqueue(object : Callback<List<VideoData>> {
            override fun onResponse(call: Call<List<VideoData>>, response: Response<List<VideoData>>) {
                swipeRefreshLayout.isRefreshing = false
                if (response.isSuccessful) {
                    val videos = response.body() ?: emptyList()
                    Log.d("API_RESPONSE", "Received ${videos.size} videos")
                    adapter.updateData(videos)
                    if (videos.isEmpty()) {
                        Toast.makeText(this@MainActivity, "No approved videos found", Toast.LENGTH_LONG).show()
                    }
                } else {
                    val errorMsg = getString(R.string.error_response, response.code(), response.message())
                    Log.e("API_ERROR", errorMsg)
                    Toast.makeText(this@MainActivity, errorMsg, Toast.LENGTH_LONG).show()
                }
            }

            override fun onFailure(call: Call<List<VideoData>>, t: Throwable) {
                swipeRefreshLayout.isRefreshing = false
                Log.e("API_FAILURE", "Network error: ${t.message}")
                Toast.makeText(this@MainActivity, "Connection Failed", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun logout() {
        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
}
