package com.example.quanlyphongtro

import android.content.Intent
import android.os.Bundle
import android.text.method.PasswordTransformationMethod
import android.text.method.SingleLineTransformationMethod
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class LoginActivity : AppCompatActivity() {

    private var isPasswordVisible = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        val etUsername = findViewById<EditText>(R.id.etUsername)
        val etPassword = findViewById<EditText>(R.id.etPassword)
        val ivTogglePassword = findViewById<ImageView>(R.id.ivTogglePassword)
        val btnLogin = findViewById<Button>(R.id.btnLogin)
        val btnRegister = findViewById<Button>(R.id.btnRegister)
        val tvForgotPassword = findViewById<TextView>(R.id.tvForgotPassword)

        // Toggle hiện/ẩn mật khẩu
        ivTogglePassword.setOnClickListener {
            isPasswordVisible = !isPasswordVisible
            if (isPasswordVisible) {
                etPassword.transformationMethod = SingleLineTransformationMethod.getInstance()
                ivTogglePassword.setImageResource(R.drawable.ic_eye_off)
            } else {
                etPassword.transformationMethod = PasswordTransformationMethod.getInstance()
                ivTogglePassword.setImageResource(R.drawable.ic_eye)
            }
            etPassword.setSelection(etPassword.text?.length ?: 0)
        }

        // Nút đăng nhập
        btnLogin.setOnClickListener {
            val username = etUsername.text.toString().trim()
            val password = etPassword.text.toString().trim()

            if (username.isEmpty()) {
                etUsername.error = "Vui lòng nhập tên đăng nhập"
                return@setOnClickListener
            }
            if (password.isEmpty()) {
                etPassword.error = "Vui lòng nhập mật khẩu"
                return@setOnClickListener
            }

            // TODO: Gọi API đăng nhập ở đây
            // Sau khi đăng nhập thành công:
            startActivity(Intent(this, TenantMainActivity::class.java))
            finish()
        }

        // Nút đăng ký
        btnRegister.setOnClickListener {
    startActivity(Intent(this, RegisterActivity::class.java))
    }   

        // Quên mật khẩu
        tvForgotPassword.setOnClickListener {
            // TODO: Mở màn hình quên mật khẩu
        }
    }
}