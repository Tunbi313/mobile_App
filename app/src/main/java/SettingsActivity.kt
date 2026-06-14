package com.example.quanlyphongtro

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SettingsActivity : AppCompatActivity() {

    private lateinit var tokenManager: TokenManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        tokenManager = TokenManager(this)

        val etFullName        = findViewById<EditText>(R.id.etFullName)
        val etEmail           = findViewById<EditText>(R.id.etEmail)
        val etCCCD            = findViewById<EditText>(R.id.etCCCD)
        val btnChangePassword = findViewById<Button>(R.id.btnChangePassword)
        val btnSave           = findViewById<Button>(R.id.btnSave)
        val btnLogout         = findViewById<TextView>(R.id.btnLogout)
        val navHome           = findViewById<LinearLayout>(R.id.navHome)
        val navManage         = findViewById<LinearLayout>(R.id.navManage)
        val navNotify         = findViewById<LinearLayout>(R.id.navNotify)
        val navSettings       = findViewById<LinearLayout>(R.id.navSettings)

        loadProfile(etFullName, etEmail, etCCCD)

        btnChangePassword.setOnClickListener {
            Toast.makeText(this, "Tính năng đổi mật khẩu đang phát triển", Toast.LENGTH_SHORT).show()
        }

        btnSave.setOnClickListener {
            val fullName = etFullName.text.toString().trim()
            val email    = etEmail.text.toString().trim()
            val cccd     = etCCCD.text.toString().trim()

            if (fullName.isEmpty()) {
                etFullName.error = "Vui lòng nhập họ tên"
                return@setOnClickListener
            }

            btnSave.isEnabled = false
            btnSave.text = "Đang lưu..."

            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val updated = RetrofitClient.instance.updateProfile(
                        tokenManager.getBearer(),
                        UpdateProfileRequest(full_name = fullName, email = email, id_card = cccd)
                    )
                    withContext(Dispatchers.Main) {
                        btnSave.isEnabled = true
                        btnSave.text = "Lưu thay đổi"
                        Toast.makeText(this@SettingsActivity,
                            "Cập nhật thông tin thành công!", Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        btnSave.isEnabled = true
                        btnSave.text = "Lưu thay đổi"
                        Toast.makeText(this@SettingsActivity,
                            "Lỗi cập nhật: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }

        btnLogout.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("Đăng xuất")
                .setMessage("Bạn có chắc muốn đăng xuất không?")
                .setPositiveButton("Đăng xuất") { _, _ ->
                    tokenManager.clearToken()
                    val intent = Intent(this, LoginActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(intent)
                }
                .setNegativeButton("Huỷ", null)
                .show()
        }

        val hasRoom = tokenManager.hasRoom()
        navHome.setOnClickListener {
            if (hasRoom) startActivity(Intent(this, TenantMainActivity::class.java))
            else         startActivity(Intent(this, RoomListingActivity::class.java))
            finish()
        }
        navManage.setOnClickListener {
            if (hasRoom) { startActivity(Intent(this, ContractActivity::class.java)); finish() }
            else Toast.makeText(this, "Bạn chưa thuê phòng nào", Toast.LENGTH_SHORT).show()
        }
        navNotify.setOnClickListener {
            startActivity(Intent(this, NotificationActivity::class.java)); finish()
        }
        navSettings.setOnClickListener { /* Already here */ }
    }

    private fun loadProfile(etName: EditText, etEmail: EditText, etCCCD: EditText) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val profile = RetrofitClient.instance.getProfile(tokenManager.getBearer())
                withContext(Dispatchers.Main) {
                    etName.setText(profile.full_name ?: "")
                    etEmail.setText(profile.email)
                    etCCCD.setText(profile.id_card ?: "")
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@SettingsActivity,
                        "Không thể tải hồ sơ", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}
