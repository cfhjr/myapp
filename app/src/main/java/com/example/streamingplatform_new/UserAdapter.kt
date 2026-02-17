package com.example.streamingplatform_new

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class UserAdapter(
    private var users: List<UserData>,
    private val onEditClick: (UserData) -> Unit,
    private val onDeleteClick: (UserData) -> Unit
) : RecyclerView.Adapter<UserAdapter.UserViewHolder>() {

    class UserViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvName: TextView = view.findViewById(R.id.tv_user_name)
        val tvEmail: TextView = view.findViewById(R.id.tv_user_email)
        val tvType: TextView = view.findViewById(R.id.tv_user_type)
        val ivEdit: ImageView = view.findViewById(R.id.iv_edit)
        val ivDelete: ImageView = view.findViewById(R.id.iv_delete)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_user, parent, false)
        return UserViewHolder(view)
    }

    override fun onBindViewHolder(holder: UserViewHolder, position: Int) {
        val user = users[position]
        holder.tvName.text = user.name
        holder.tvEmail.text = user.email
        holder.tvType.text = user.userType

        holder.ivEdit.setOnClickListener { onEditClick(user) }
        holder.ivDelete.setOnClickListener { onDeleteClick(user) }
    }

    override fun getItemCount() = users.size

    fun updateData(newUsers: List<UserData>) {
        users = newUsers
        notifyDataSetChanged()
    }
}
