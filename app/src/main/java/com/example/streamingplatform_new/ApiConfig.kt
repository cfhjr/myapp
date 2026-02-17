package com.example.streamingplatform_new

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object ApiConfig {
    // Shared IP for both App and Backend
    private const val COMPUTER_IP = "10.222.127.210"
    
    const val BASE_URL = "http://$COMPUTER_IP/cfhstreaming/"

    fun getApiService(): ApiService {
        val logging = HttpLoggingInterceptor()
        logging.setLevel(HttpLoggingInterceptor.Level.BODY)
        
        val client = OkHttpClient.Builder()
            .addInterceptor(logging)
            .connectTimeout(10, TimeUnit.MINUTES)
            .readTimeout(10, TimeUnit.MINUTES)
            .writeTimeout(10, TimeUnit.MINUTES)
            .retryOnConnectionFailure(true)
            .build()

        return Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .client(client)
            .build()
            .create(ApiService::class.java)
    }

    private fun normalizePath(path: String?): String {
        if (path.isNullOrEmpty()) return ""
        if (path.startsWith("http")) return path
        
        var relativePath = path.trimStart('/')
        
        // Remove 'cfhstreaming/' if it's erroneously included in the path from DB
        if (relativePath.contains("cfhstreaming/")) {
            relativePath = relativePath.substringAfter("cfhstreaming/").trimStart('/')
        }

        // The user specified path: haule/uploads/videos/vid_6993827a457be2.00424211.mp4
        // This implies the structure is directly under the web root or COMPUTER_IP
        
        return if (relativePath.startsWith("haule/")) {
             "http://$COMPUTER_IP/$relativePath"
        } else {
             // Fallback or old logic if needed, but per request we use haule/
             "http://$COMPUTER_IP/$relativePath"
        }
    }

    fun getVideoUrl(videoPath: String?) = normalizePath(videoPath)
    fun getThumbnailUrl(thumbnailPath: String?) = normalizePath(thumbnailPath)
}
