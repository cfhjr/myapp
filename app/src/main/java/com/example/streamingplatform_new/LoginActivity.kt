package com.example.streamingplatform_new

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import com.google.android.material.textfield.TextInputEditText
import com.google.gson.JsonParser
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class LoginActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        val btnLogin = findViewById<Button>(R.id.btn_login)
        val tvRegister = findViewById<TextView>(R.id.tv_register)
        val etEmail = findViewById<TextInputEditText>(R.id.et_email)
        val etPassword = findViewById<TextInputEditText>(R.id.et_password)

        btnLogin.setOnClickListener {
            val email = etEmail.text.toString().trim()
            val password = etPassword.text.toString().trim()

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Please enter email and password", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Hardcoded check for admin
            if (email == "admin@admin" && password == "admin@123") {
                startActivity(Intent(this, AdminActivity::class.java))
                finish()
                return@setOnClickListener
            }

            Log.d("Login", "Attempting login at: ${ApiConfig.BASE_URL}login.php")

            // Send raw password. PHP usually handles hashing via password_verify().
            // Sending hashed passwords from Android is generally unnecessary and causes mismatches.
            ApiConfig.getApiService().login(email, password).enqueue(object : Callback<LoginResponse> {
                override fun onResponse(call: Call<LoginResponse>, response: Response<LoginResponse>) {
                    if (response.isSuccessful) {
                        val loginResponse = response.body()
                        if (loginResponse != null && loginResponse.success) {
                            val user = loginResponse.user
                            if (user != null) {
                                val userType = user.userType.lowercase().trim()
                                val userId = user.id
                                
                                Log.d("Login", "Success! Role: $userType, ID: $userId")
                                
                                val intent = when {
                                    userType.contains("admin") -> Intent(this@LoginActivity, AdminActivity::class.java)
                                    userType.contains("creator") -> {
                                        Intent(this@LoginActivity, CreatorActivity::class.java).apply {
                                            putExtra("USER_ID", userId)
                                            putExtra("USER_NAME", user.name)
                                        }
                                    }
                                    else -> { // Default is "user"
                                        Intent(this@LoginActivity, MainActivity::class.java).apply {
                                            putExtra("USER_ID", userId)
                                            putExtra("USER_NAME", user.name)
                                        }
                                    }
                                }
                                
                                Toast.makeText(this@LoginActivity, "Welcome ${user.name}", Toast.LENGTH_SHORT).show()
                                startActivity(intent)
                                finish()
                            }
                        } else {
                            val msg = loginResponse?.message ?: "Invalid credentials"
                            Toast.makeText(this@LoginActivity, msg, Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        val code = response.code()
                        val errorBody = response.errorBody()?.string() ?: ""
                        Log.e("LoginError", "Code: $code, Body: $errorBody")
                        
                        try {
                            val jsonElement = JsonParser().parse(errorBody)
                            val message = jsonElement.asJsonObject.get("message")?.asString ?: "Server error $code"
                            Toast.makeText(this@LoginActivity, message, Toast.LENGTH_SHORT).show()
                        } catch (e: Exception) {
                            Toast.makeText(this@LoginActivity, "Server Error $code. Check Logcat.", Toast.LENGTH_SHORT).show()
                        }
                    }
                }

                override fun onFailure(call: Call<LoginResponse>, t: Throwable) {
                    Log.e("LoginFailure", "Error: ${t.message}", t)
                    val errorMsg = when(t) {
                        is java.net.SocketTimeoutException -> "Connection Timeout. Server is slow."
                        is java.net.ConnectException -> "Cannot reach server. Check IP in ApiConfig and Wi-Fi."
                        else -> "Network Error: ${t.localizedMessage}"
                    }
                    Toast.makeText(this@LoginActivity, errorMsg, Toast.LENGTH_LONG).show()
                }
            })
        }

        tvRegister.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }
    }
}
