package com.example.streamingplatform_new

import com.google.gson.annotations.SerializedName
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Call
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Query

interface ApiService {
    @FormUrlEncoded
    @POST("login.php")
    fun login(
        @Field("email") email: String,
        @Field("password") password: String
    ): Call<LoginResponse>

    @FormUrlEncoded
    @POST("register.php")
    fun register(
        @Field("name") name: String,
        @Field("phone") phone: String,
        @Field("user_type") userType: String,
        @Field("birth_date") birthDate: String,
        @Field("email") email: String,
        @Field("password") password: String,
        @Field("created_at") createdAt: String
    ): Call<RegisterResponse>

    @GET("admin/get_users.php")
    fun getUsers(): Call<List<UserData>>

    @FormUrlEncoded
    @POST("admin/add_user.php")
    fun addUser(
        @Field("name") name: String,
        @Field("phone") phone: String,
        @Field("user_type") userType: String,
        @Field("birth_date") birthDate: String,
        @Field("email") email: String,
        @Field("password") password: String,
        @Field("created_at") createdAt: String
    ): Call<BaseResponse>

    @FormUrlEncoded
    @POST("admin/update_user.php")
    fun updateUser(
        @Field("id") id: Int,
        @Field("name") name: String,
        @Field("phone") phone: String,
        @Field("email") email: String,
        @Field("user_type") userType: String
    ): Call<BaseResponse>

    @FormUrlEncoded
    @POST("admin/delete_user.php")
    fun deleteUser(
        @Field("id") id: Int
    ): Call<BaseResponse>

    @GET("admin/get_all_videos.php")
    fun getAllVideos(): Call<List<VideoData>>

    @GET("admin/get_pending_videos.php")
    fun getPendingVideos(): Call<List<VideoData>>

    @FormUrlEncoded
    @POST("admin/approve_video.php")
    fun approveVideo(
        @Field("video_id") videoId: Int,
        @Field("status") status: String
    ): Call<BaseResponse>

    @FormUrlEncoded
    @POST("admin/delete_video.php")
    fun deleteVideo(
        @Field("video_id") videoId: Int
    ): Call<BaseResponse>

    @GET("get_approved_videos.php")
    fun getApprovedVideos(): Call<List<VideoData>>

    @GET("get_my_videos.php")
    fun getMyVideos(
        @Query("user_id") userId: Int,
        @Query("status") status: String,
        @Query("page") page: Int
    ): Call<List<VideoData>>

    @Multipart
    @POST("upload_video.php")
    fun uploadVideo(
        @Part("user_id") userId: RequestBody,
        @Part("category_id") categoryId: RequestBody,
        @Part("title") title: RequestBody,
        @Part("description") description: RequestBody,
        @Part video: MultipartBody.Part,
        @Part thumbnail: MultipartBody.Part?
    ): Call<BaseResponse>

    @FormUrlEncoded
    @POST("like_video.php")
    fun likeVideo(
        @Field("user_id") userId: Int,
        @Field("video_id") videoId: Int
    ): Call<BaseResponse>

    @FormUrlEncoded
    @POST("dislike_video.php")
    fun dislikeVideo(
        @Field("user_id") userId: Int,
        @Field("video_id") videoId: Int
    ): Call<BaseResponse>

    @FormUrlEncoded
    @POST("comment_video.php")
    fun commentVideo(
        @Field("user_id") userId: Int,
        @Field("video_id") videoId: Int,
        @Field("comment") comment: String
    ): Call<BaseResponse>
}

data class LoginResponse(
    @SerializedName("success") val success: Boolean,
    @SerializedName("message") val message: String,
    @SerializedName("user") val user: UserData?
)

data class RegisterResponse(
    @SerializedName("success") val success: Boolean,
    @SerializedName("message") val message: String
)

data class BaseResponse(
    @SerializedName("success") val success: Boolean,
    @SerializedName("message") val message: String
)

data class UserData(
    @SerializedName("id") val id: Int,
    @SerializedName("name") val name: String,
    @SerializedName("phone") val phone: String?,
    @SerializedName("email") val email: String,
    @SerializedName("user_type") val userType: String,
    @SerializedName("birth_date") val birthDate: String?,
    @SerializedName("created_at") val createdAt: String?
)

data class VideoData(
    @SerializedName("id") val id: Int,
    @SerializedName("title") val title: String,
    @SerializedName("description") val description: String?,
    @SerializedName("video_path") val videoPath: String?,
    @SerializedName("thumbnail_path") val thumbnailPath: String?,
    @SerializedName("user_id") val userId: Int,
    @SerializedName("uploader_name") val uploaderName: String?,
    @SerializedName("status") val status: String,
    @SerializedName("created_at") val createdAt: String?,
    @SerializedName("like_count") var likeCount: Int = 0,
    @SerializedName("dislike_count") var dislikeCount: Int = 0,
    @SerializedName("comment_count") var commentCount: Int = 0,
    @SerializedName("is_liked") var isLiked: Boolean = false,
    @SerializedName("is_disliked") var isDisliked: Boolean = false
)
