package com.example.streamingplatform_new

import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class VideoAdapter(
    private val videos: MutableList<VideoData>,
    private val userId: Int
) : RecyclerView.Adapter<VideoAdapter.ViewHolder>() {

    fun updateData(newVideos: List<VideoData>) {
        val diffCallback = VideoDiffCallback(videos, newVideos)
        val diffResult = DiffUtil.calculateDiff(diffCallback)
        videos.clear()
        videos.addAll(newVideos)
        diffResult.dispatchUpdatesTo(this)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_video, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val video = videos[position]
        val context = holder.itemView.context
        
        holder.tvTitle.text = video.title
        holder.tvDescription.text = video.description
        holder.tvInfo.text = context.getString(R.string.uploaded_on, video.createdAt ?: "")
        holder.tvLikeCount.text = context.getString(R.string.count_format, video.likeCount)
        holder.tvDislikeCount.text = context.getString(R.string.count_format, video.dislikeCount)
        holder.tvCommentCount.text = context.getString(R.string.count_format, video.commentCount)

        // Initialize Like/Dislike icons and tags from database state
        val likedTag = context.getString(R.string.liked)
        val unlikedTag = context.getString(R.string.unliked)
        val dislikedTag = context.getString(R.string.disliked)
        val undislikedTag = context.getString(R.string.undisliked)

        if (video.isLiked) {
            holder.ivLikeIcon.setImageResource(android.R.drawable.btn_star_big_on)
            holder.ivLikeIcon.tag = likedTag
        } else {
            holder.ivLikeIcon.setImageResource(android.R.drawable.btn_star_big_off)
            holder.ivLikeIcon.tag = unlikedTag
        }

        if (video.isDisliked) {
            holder.ivDislikeIcon.setImageResource(android.R.drawable.ic_delete)
            holder.ivDislikeIcon.tag = dislikedTag
        } else {
            holder.ivDislikeIcon.setImageResource(android.R.drawable.ic_menu_close_clear_cancel)
            holder.ivDislikeIcon.tag = undislikedTag
        }

        val videoUrl = ApiConfig.getVideoUrl(video.videoPath)
        val thumbnailUrl = ApiConfig.getThumbnailUrl(video.thumbnailPath)
        val imageToLoad = if (!video.thumbnailPath.isNullOrEmpty()) thumbnailUrl else videoUrl

        Glide.with(context)
            .load(imageToLoad)
            .frame(1000000)
            .placeholder(android.R.drawable.ic_menu_gallery)
            .error(android.R.drawable.ic_menu_report_image)
            .centerCrop()
            .into(holder.ivThumbnail)

        // Like Button Click
        holder.btnLike.setOnClickListener {
            ApiConfig.getApiService().likeVideo(userId, video.id).enqueue(object : Callback<BaseResponse> {
                override fun onResponse(call: Call<BaseResponse>, response: Response<BaseResponse>) {
                    if (response.isSuccessful && response.body()?.success == true) {
                        val isLiked = holder.ivLikeIcon.tag == likedTag
                        
                        if (isLiked) {
                            video.isLiked = false
                            video.likeCount--
                            holder.ivLikeIcon.setImageResource(android.R.drawable.btn_star_big_off)
                            holder.ivLikeIcon.tag = unlikedTag
                        } else {
                            // If dislike was active, reset it locally
                            if (video.isDisliked) {
                                video.isDisliked = false
                                video.dislikeCount--
                                holder.ivDislikeIcon.setImageResource(android.R.drawable.ic_menu_close_clear_cancel)
                                holder.ivDislikeIcon.tag = undislikedTag
                                holder.tvDislikeCount.text = context.getString(R.string.count_format, video.dislikeCount)
                            }
                            video.isLiked = true
                            video.likeCount++
                            holder.ivLikeIcon.setImageResource(android.R.drawable.btn_star_big_on)
                            holder.ivLikeIcon.tag = likedTag
                        }
                        holder.tvLikeCount.text = context.getString(R.string.count_format, video.likeCount)
                    }
                }
                override fun onFailure(call: Call<BaseResponse>, t: Throwable) {}
            })
        }

        // Dislike Button Click
        holder.btnDislike.setOnClickListener {
            ApiConfig.getApiService().dislikeVideo(userId, video.id).enqueue(object : Callback<BaseResponse> {
                override fun onResponse(call: Call<BaseResponse>, response: Response<BaseResponse>) {
                    if (response.isSuccessful && response.body()?.success == true) {
                        val isDisliked = holder.ivDislikeIcon.tag == dislikedTag

                        if (isDisliked) {
                            video.isDisliked = false
                            video.dislikeCount--
                            holder.ivDislikeIcon.setImageResource(android.R.drawable.ic_menu_close_clear_cancel)
                            holder.ivDislikeIcon.tag = undislikedTag
                        } else {
                            // If like was active, reset it locally
                            if (video.isLiked) {
                                video.isLiked = false
                                video.likeCount--
                                holder.ivLikeIcon.setImageResource(android.R.drawable.btn_star_big_off)
                                holder.ivLikeIcon.tag = unlikedTag
                                holder.tvLikeCount.text = context.getString(R.string.count_format, video.likeCount)
                            }
                            video.isDisliked = true
                            video.dislikeCount++
                            holder.ivDislikeIcon.setImageResource(android.R.drawable.ic_delete)
                            holder.ivDislikeIcon.tag = dislikedTag
                        }
                        holder.tvDislikeCount.text = context.getString(R.string.count_format, video.dislikeCount)
                    }
                }
                override fun onFailure(call: Call<BaseResponse>, t: Throwable) {}
            })
        }

        holder.btnComment.setOnClickListener {
            showCommentDialog(context, video.id, holder.tvCommentCount, video)
        }

        holder.itemView.setOnClickListener {
            val intent = Intent(context, VideoPlayerActivity::class.java)
            val vUrl = ApiConfig.getVideoUrl(video.videoPath)
            intent.putExtra("VIDEO_URL", vUrl)
            context.startActivity(intent)
        }
    }

    private fun showCommentDialog(context: android.content.Context, videoId: Int, tvCount: TextView, video: VideoData) {
        val builder = AlertDialog.Builder(context)
        builder.setTitle(R.string.add_comment)
        val input = EditText(context)
        builder.setView(input)
        builder.setPositiveButton(R.string.post) { _, _ ->
            val comment = input.text.toString().trim()
            if (comment.isNotEmpty()) {
                ApiConfig.getApiService().commentVideo(userId, videoId, comment).enqueue(object : Callback<BaseResponse> {
                    override fun onResponse(call: Call<BaseResponse>, response: Response<BaseResponse>) {
                        if (response.isSuccessful && response.body()?.success == true) {
                            Toast.makeText(context, R.string.comment_posted, Toast.LENGTH_SHORT).show()
                            video.commentCount++
                            tvCount.text = context.getString(R.string.count_format, video.commentCount)
                        }
                    }
                    override fun onFailure(call: Call<BaseResponse>, t: Throwable) {}
                })
            }
        }
        builder.setNegativeButton(R.string.cancel, null)
        builder.show()
    }

    override fun getItemCount() = videos.size

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val ivThumbnail: ImageView = view.findViewById(R.id.iv_thumbnail)
        val tvTitle: TextView = view.findViewById(R.id.tv_video_title)
        val tvDescription: TextView = view.findViewById(R.id.tv_video_description)
        val tvInfo: TextView = view.findViewById(R.id.tv_video_info)
        val btnLike: LinearLayout = view.findViewById(R.id.btn_like)
        val btnDislike: LinearLayout = view.findViewById(R.id.btn_dislike)
        val btnComment: LinearLayout = view.findViewById(R.id.btn_comment)
        val tvLikeCount: TextView = view.findViewById(R.id.tv_like_count)
        val tvDislikeCount: TextView = view.findViewById(R.id.tv_dislike_count)
        val tvCommentCount: TextView = view.findViewById(R.id.tv_comment_count)
        val ivLikeIcon: ImageView = view.findViewById(R.id.iv_like_icon)
        val ivDislikeIcon: ImageView = view.findViewById(R.id.iv_dislike_icon)
    }

    class VideoDiffCallback(private val oldList: List<VideoData>, private val newList: List<VideoData>) : DiffUtil.Callback() {
        override fun getOldListSize() = oldList.size
        override fun getNewListSize() = newList.size
        override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int) = oldList[oldItemPosition].id == newList[newItemPosition].id
        override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int) = oldList[oldItemPosition] == newList[newItemPosition]
    }
}
