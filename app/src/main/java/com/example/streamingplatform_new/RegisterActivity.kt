package com.example.streamingplatform_new

import android.app.DatePickerDialog
import android.os.Bundle
import android.util.Log
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.textfield.TextInputEditText
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class RegisterActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        val etName = findViewById<TextInputEditText>(R.id.et_name)
        val etPhone = findViewById<TextInputEditText>(R.id.et_phone)
        val actUserType = findViewById<AutoCompleteTextView>(R.id.act_user_type)
        val etBirthDate = findViewById<TextInputEditText>(R.id.et_birth_date)
        val etEmail = findViewById<TextInputEditText>(R.id.et_email)
        val etPassword = findViewById<TextInputEditText>(R.id.et_password)
        val btnRegister = findViewById<Button>(R.id.btn_register)
        val tvLogin = findViewById<TextView>(R.id.tv_login)

        val userTypes = arrayOf("User", "Creator")
        val adapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, userTypes)
        actUserType.setAdapter(adapter)

        etBirthDate.setOnClickListener {
            val calendar = Calendar.getInstance()
            val year = calendar.get(Calendar.YEAR)
            val month = calendar.get(Calendar.MONTH)
            val day = calendar.get(Calendar.DAY_OF_MONTH)

            val datePickerDialog = DatePickerDialog(this, { _, selectedYear, selectedMonth, selectedDay ->
                val monthStr = if (selectedMonth + 1 < 10) "0${selectedMonth + 1}" else "${selectedMonth + 1}"
                val dayStr = if (selectedDay < 10) "0$selectedDay" else "$selectedDay"
                val date = "$selectedYear-$monthStr-$dayStr"
                etBirthDate.setText(date)
            }, year, month, day)

            datePickerDialog.show()
        }

        btnRegister.setOnClickListener {
            val name = etName.text.toString().trim()
            val phone = etPhone.text.toString().trim()
            val userType = actUserType.text.toString().trim()
            val birthDate = etBirthDate.text.toString().trim()
            val email = etEmail.text.toString().trim()
            val password = etPassword.text.toString().trim()
            
            val createdAt = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())

            if (name.isEmpty() || phone.isEmpty() || userType.isEmpty() || 
                birthDate.isEmpty() || email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Tafadhali jaza nafasi zote", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Sending raw password to match LoginActivity. 
            // The PHP backend should handle the hashing (e.g. password_hash) for security.
            ApiConfig.getApiService().register(name, phone, userType, birthDate, email, password, createdAt)
                .enqueue(object : Callback<RegisterResponse> {
                    override fun onResponse(call: Call<RegisterResponse>, response: Response<RegisterResponse>) {
                        if (response.isSuccessful) {
                            val registerResponse = response.body()
                            if (registerResponse != null && registerResponse.success) {
                                Toast.makeText(this@RegisterActivity, "Usajili Umefanikiwa", Toast.LENGTH_LONG).show()
                                finish() 
                            } else {
                                val errorMsg = registerResponse?.message ?: "Unknown error"
                                Toast.makeText(this@RegisterActivity, "Imeshindikana: $errorMsg", Toast.LENGTH_SHORT).show()
                            }
                        } else {
                            Toast.makeText(this@RegisterActivity, "Tatizo la Seva (Code: ${response.code()})", Toast.LENGTH_SHORT).show()
                        }
                    }

                    override fun onFailure(call: Call<RegisterResponse>, t: Throwable) {
                        Toast.makeText(this@RegisterActivity, "Network Error: ${t.message}", Toast.LENGTH_LONG).show()
                    }
                })
        }

        tvLogin.setOnClickListener {
            finish()
        }
    }
}
