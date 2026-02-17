package com.example.streamingplatform_new

import android.app.DatePickerDialog
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.textfield.TextInputEditText
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class UserManagementActivity : AppCompatActivity() {

    private lateinit var rvUsers: RecyclerView
    private lateinit var userAdapter: UserAdapter
    private lateinit var fabAddUser: FloatingActionButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_user_management)

        val toolbar = findViewById<com.google.android.material.appbar.MaterialToolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener { finish() }

        rvUsers = findViewById(R.id.rv_users)
        fabAddUser = findViewById(R.id.fab_add_user)

        rvUsers.layoutManager = LinearLayoutManager(this)
        userAdapter = UserAdapter(emptyList(), { user -> showEditDialog(user) }, { user -> showDeleteDialog(user) })
        rvUsers.adapter = userAdapter

        fabAddUser.setOnClickListener {
            showAddUserDialog()
        }

        loadUsers()
    }

    private fun loadUsers() {
        ApiConfig.getApiService().getUsers().enqueue(object : Callback<List<UserData>> {
            override fun onResponse(call: Call<List<UserData>>, response: Response<List<UserData>>) {
                if (response.isSuccessful) {
                    val users = response.body() ?: emptyList()
                    Log.d("UserManagement", "Loaded ${users.size} users")
                    userAdapter.updateData(users)
                } else {
                    Log.e("UserManagement", "Failed to load users: ${response.code()}")
                    Toast.makeText(this@UserManagementActivity, "Failed to load users", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<List<UserData>>, t: Throwable) {
                Log.e("UserManagement", "Error loading users", t)
                Toast.makeText(this@UserManagementActivity, "Error: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun showAddUserDialog() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_user, null)
        val etName = dialogView.findViewById<TextInputEditText>(R.id.et_add_name)
        val etPhone = dialogView.findViewById<TextInputEditText>(R.id.et_add_phone)
        val etEmail = dialogView.findViewById<TextInputEditText>(R.id.et_add_email)
        val etPassword = dialogView.findViewById<TextInputEditText>(R.id.et_add_password)
        val etBirthDate = dialogView.findViewById<TextInputEditText>(R.id.et_add_birth_date)
        val actUserType = dialogView.findViewById<AutoCompleteTextView>(R.id.act_add_user_type)

        // Setup User Type Dropdown
        val userTypes = arrayOf("User", "Creator", "Admin")
        val adapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, userTypes)
        actUserType.setAdapter(adapter)

        // Setup Date Picker
        etBirthDate.setOnClickListener {
            val calendar = Calendar.getInstance()
            DatePickerDialog(this, { _, year, month, dayOfMonth ->
                val monthStr = if (month + 1 < 10) "0${month + 1}" else "${month + 1}"
                val dayStr = if (dayOfMonth < 10) "0$dayOfMonth" else "$dayOfMonth"
                val date = "$year-$monthStr-$dayStr"
                etBirthDate.setText(date)
            }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show()
        }

        MaterialAlertDialogBuilder(this)
            .setTitle("Add New User")
            .setView(dialogView)
            .setPositiveButton("Add") { _, _ ->
                val name = etName.text.toString()
                val phone = etPhone.text.toString()
                val email = etEmail.text.toString()
                val password = etPassword.text.toString()
                val birthDate = etBirthDate.text.toString()
                val userType = actUserType.text.toString()

                if (name.isNotEmpty() && email.isNotEmpty() && password.isNotEmpty()) {
                    val hashedPassword = hashPassword(password)
                    addNewUser(name, phone, userType, birthDate, email, hashedPassword)
                } else {
                    Toast.makeText(this, "Please fill required fields", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun addNewUser(name: String, phone: String, userType: String, birthDate: String, email: String, password: String) {
        val createdAt = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
        ApiConfig.getApiService().addUser(name, phone, userType, birthDate, email, password, createdAt).enqueue(object : Callback<BaseResponse> {
            override fun onResponse(call: Call<BaseResponse>, response: Response<BaseResponse>) {
                if (response.isSuccessful && response.body()?.success == true) {
                    Toast.makeText(this@UserManagementActivity, "User added successfully", Toast.LENGTH_SHORT).show()
                    loadUsers()
                } else {
                    Toast.makeText(this@UserManagementActivity, "Failed to add user", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<BaseResponse>, t: Throwable) {
                Toast.makeText(this@UserManagementActivity, "Error: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun showEditDialog(user: UserData) {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_edit_user, null)
        val etName = dialogView.findViewById<TextInputEditText>(R.id.et_edit_name)
        val etPhone = dialogView.findViewById<TextInputEditText>(R.id.et_edit_phone)
        val etEmail = dialogView.findViewById<TextInputEditText>(R.id.et_edit_email)
        val actUserType = dialogView.findViewById<AutoCompleteTextView>(R.id.act_edit_user_type)

        etName.setText(user.name)
        etPhone.setText(user.phone)
        etEmail.setText(user.email)
        actUserType.setText(user.userType)

        val userTypes = arrayOf("User", "Creator", "Admin")
        val adapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, userTypes)
        actUserType.setAdapter(adapter)

        MaterialAlertDialogBuilder(this)
            .setTitle("Edit User")
            .setView(dialogView)
            .setPositiveButton("Update") { _, _ ->
                val newName = etName.text.toString()
                val newPhone = etPhone.text.toString()
                val newEmail = etEmail.text.toString()
                val newType = actUserType.text.toString()
                
                updateUser(user.id, newName, newPhone, newEmail, newType)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun updateUser(id: Int, name: String, phone: String, email: String, type: String) {
        ApiConfig.getApiService().updateUser(id, name, phone, email, type).enqueue(object : Callback<BaseResponse> {
            override fun onResponse(call: Call<BaseResponse>, response: Response<BaseResponse>) {
                if (response.isSuccessful && response.body()?.success == true) {
                    Toast.makeText(this@UserManagementActivity, "User updated", Toast.LENGTH_SHORT).show()
                    loadUsers()
                } else {
                    Toast.makeText(this@UserManagementActivity, "Update failed", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<BaseResponse>, t: Throwable) {
                Toast.makeText(this@UserManagementActivity, "Error: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun showDeleteDialog(user: UserData) {
        MaterialAlertDialogBuilder(this)
            .setTitle("Delete User")
            .setMessage("Are you sure you want to delete ${user.name}?")
            .setPositiveButton("Delete") { _, _ ->
                deleteUser(user.id)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun deleteUser(id: Int) {
        ApiConfig.getApiService().deleteUser(id).enqueue(object : Callback<BaseResponse> {
            override fun onResponse(call: Call<BaseResponse>, response: Response<BaseResponse>) {
                if (response.isSuccessful && response.body()?.success == true) {
                    Toast.makeText(this@UserManagementActivity, "User deleted", Toast.LENGTH_SHORT).show()
                    loadUsers()
                } else {
                    Toast.makeText(this@UserManagementActivity, "Deletion failed", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<BaseResponse>, t: Throwable) {
                Toast.makeText(this@UserManagementActivity, "Error: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun hashPassword(password: String): String {
        val bytes = password.toByteArray()
        val md = MessageDigest.getInstance("SHA-256")
        val digest = md.digest(bytes)
        return digest.fold("") { str, it -> str + "%02x".format(it) }
    }
}
