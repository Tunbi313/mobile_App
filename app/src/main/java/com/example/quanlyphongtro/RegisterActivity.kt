package com.example.quanlyphongtro

import android.content.Intent
import android.os.Bundle
import android.text.method.PasswordTransformationMethod
import android.text.method.SingleLineTransformationMethod
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import org.json.JSONObject

class RegisterActivity : AppCompatActivity() {

    private var isPasswordVisible = false
    private var isConfirmPasswordVisible = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        val etFullName = findViewById<EditText>(R.id.etFullName)
        val etPhone = findViewById<EditText>(R.id.etPhone)
        val etEmail = findViewById<EditText>(R.id.etEmail)
        val etPassword = findViewById<EditText>(R.id.etPassword)
        val etConfirmPassword = findViewById<EditText>(R.id.etConfirmPassword)
        val ivTogglePassword = findViewById<ImageView>(R.id.ivTogglePassword)
        val ivToggleConfirmPassword = findViewById<ImageView>(R.id.ivToggleConfirmPassword)
        val btnRegister = findViewById<Button>(R.id.btnRegister)
        val tvLoginLink = findViewById<TextView>(R.id.tvLoginLink)

        // Toggle hiện/ẩn mật khẩu
        ivTogglePassword.setOnClickListener {
            isPasswordVisible = !isPasswordVisible
            etPassword.transformationMethod = if (isPasswordVisible)
                SingleLineTransformationMethod.getInstance()
            else
                PasswordTransformationMethod.getInstance()
            ivTogglePassword.setImageResource(
                if (isPasswordVisible) R.drawable.ic_eye_off else R.drawable.ic_eye
            )
            etPassword.setSelection(etPassword.text?.length ?: 0)
        }

        // Toggle hiện/ẩn xác nhận mật khẩu
        ivToggleConfirmPassword.setOnClickListener {
            isConfirmPasswordVisible = !isConfirmPasswordVisible
            etConfirmPassword.transformationMethod = if (isConfirmPasswordVisible)
                SingleLineTransformationMethod.getInstance()
            else
                PasswordTransformationMethod.getInstance()
            ivToggleConfirmPassword.setImageResource(
                if (isConfirmPasswordVisible) R.drawable.ic_eye_off else R.drawable.ic_eye
            )
            etConfirmPassword.setSelection(etConfirmPassword.text?.length ?: 0)
        }

        // Nút Đăng ký
        btnRegister.setOnClickListener {
            val fullName = etFullName.text.toString().trim()
            val phone = etPhone.text.toString().trim()
            val email = etEmail.text.toString().trim()
            val password = etPassword.text.toString().trim()
            val confirmPassword = etConfirmPassword.text.toString().trim()

            // Validate
            if (fullName.isEmpty()) {
                etFullName.error = "Vui lòng nhập họ tên"
                return@setOnClickListener
            }
            if (phone.isEmpty()) {
                etPhone.error = "Vui lòng nhập số điện thoại"
                return@setOnClickListener
            }
            if (email.isEmpty()) {
                etEmail.error = "Vui lòng nhập email"
                return@setOnClickListener
            }
            if (password.isEmpty()) {
                etPassword.error = "Vui lòng nhập mật khẩu"
                return@setOnClickListener
            }
            if (password.length < 6) {
                etPassword.error = "Mật khẩu tối thiểu 6 ký tự"
                return@setOnClickListener
            }
            if (password != confirmPassword) {
                etConfirmPassword.error = "Mật khẩu xác nhận không khớp"
                return@setOnClickListener
            }

            val jsonBody = JSONObject()
            jsonBody.put("username", phone) // Dùng số điện thoại làm username
            jsonBody.put("phone", phone)
            jsonBody.put("first_name", fullName)
            jsonBody.put("email", email)
            jsonBody.put("password", password)

            ApiClient.post(this, "/auth/register/", jsonBody, object : ApiClient.ApiCallback {
                override fun onSuccess(response: String) {
                    Toast.makeText(this@RegisterActivity, "Đăng ký tài khoản thành công!", Toast.LENGTH_SHORT).show()
                    val intent = Intent(this@RegisterActivity, LoginActivity::class.java)
                    startActivity(intent)
                    finish()
                }

                override fun onError(error: String) {
                    try {
                        val errorJson = JSONObject(error)
                        val keys = errorJson.keys()
                        if (keys.hasNext()) {
                            val key = keys.next()
                            val errorVal = errorJson.opt(key)
                            val message = if (errorVal is org.json.JSONArray) {
                                errorVal.getString(0)
                            } else {
                                errorJson.optString("error", "Lỗi đăng ký tài khoản")
                            }
                            Toast.makeText(this@RegisterActivity, message, Toast.LENGTH_LONG).show()
                        } else {
                            Toast.makeText(this@RegisterActivity, "Lỗi đăng ký tài khoản", Toast.LENGTH_SHORT).show()
                        }
                    } catch (e: Exception) {
                        Toast.makeText(this@RegisterActivity, "Không thể kết nối máy chủ", Toast.LENGTH_SHORT).show()
                    }
                }
            })
        }

        // Link quay về đăng nhập
        tvLoginLink.setOnClickListener {
            finish() // Quay về LoginActivity
        }
    }
}
