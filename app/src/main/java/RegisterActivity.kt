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
import com.google.gson.annotations.SerializedName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class RegisterRequest(
    val username: String,
    val password: String,
    val phone: String,
    @SerializedName("full_name") val fullname: String,
    val email: String
)

class RegisterActivity : AppCompatActivity() {

    private var isPasswordVisible = false
    private var isConfirmPasswordVisible = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        val etFullName = findViewById<EditText>(R.id.etFullName)
        val etPhone = findViewById<EditText>(R.id.etPhone)
        val etEmail = findViewById<EditText>(R.id.etEmail)
        val etUsername = findViewById<EditText>(R.id.etUserName)
        val etPassword = findViewById<EditText>(R.id.etPassword)
        val etConfirmPassword = findViewById<EditText>(R.id.etConfirmPassword)
        val ivTogglePassword = findViewById<ImageView>(R.id.ivTogglePassword)
        val ivToggleConfirmPassword = findViewById<ImageView>(R.id.ivToggleConfirmPassword)
        val btnRegister = findViewById<Button>(R.id.btnRegister)
        val tvLoginLink = findViewById<TextView>(R.id.tvLoginLink)

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

        btnRegister.setOnClickListener {
            val fullName = etFullName.text.toString().trim()
            val phone = etPhone.text.toString().trim()
            val email = etEmail.text.toString().trim()
            val username = etUsername.text.toString().trim()
            val password = etPassword.text.toString().trim()
            val confirmPassword = etConfirmPassword.text.toString().trim()

            if (fullName.isEmpty()) {
                etFullName.error = "Vui lòng nhập họ tên"
                Toast.makeText(this, "Vui lòng nhập họ tên", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (phone.isEmpty()) {
                etPhone.error = "Vui lòng nhập số điện thoại"
                Toast.makeText(this, "Vui lòng nhập số điện thoại", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (email.isEmpty()) {
                etEmail.error = "Vui lòng nhập email"
                Toast.makeText(this, "Vui lòng nhập email", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (username.isEmpty()) {
                etUsername.error = "Vui lòng nhập tên đăng nhập"
                Toast.makeText(this, "Vui lòng nhập tên đăng nhập", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (password.isEmpty()) {
                etPassword.error = "Vui lòng nhập mật khẩu"
                Toast.makeText(this, "Vui lòng nhập mật khẩu", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (password.length < 6) {
                etPassword.error = "Mật khẩu tối thiểu 6 ký tự"
                Toast.makeText(this, "Mật khẩu tối thiểu 6 ký tự", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (password != confirmPassword) {
                etConfirmPassword.error = "Mật khẩu xác nhận không khớp"
                Toast.makeText(this, "Mật khẩu xác nhận không khớp", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            btnRegister.isEnabled = false
            btnRegister.text = "Đang đăng ký..."

            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val response = RetrofitClient.instance.register(
                        RegisterRequest(username, password, phone, fullName, email)
                    )
                    withContext(Dispatchers.Main) {
                        TokenManager(this@RegisterActivity).saveToken(response.access)
                        startActivity(Intent(this@RegisterActivity, TenantMainActivity::class.java))
                        finish()
                    }
                } catch (e: retrofit2.HttpException) {
                    withContext(Dispatchers.Main) {
                        btnRegister.isEnabled = true
                        btnRegister.text = "Đăng ký →"
                        val msg = "Tên đăng nhập đã tồn tại hoặc thông tin không hợp lệ (${e.code()})"
                        etUsername.error = msg
                        Toast.makeText(this@RegisterActivity, msg, Toast.LENGTH_LONG).show()
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        btnRegister.isEnabled = true
                        btnRegister.text = "Đăng ký →"
                        val msg = "Lỗi kết nối: ${e.message}"
                        etPassword.error = msg
                        Toast.makeText(this@RegisterActivity, msg, Toast.LENGTH_LONG).show()
                    }
                }
            }
        }

        tvLoginLink.setOnClickListener {
            finish()
        }
    }
}
