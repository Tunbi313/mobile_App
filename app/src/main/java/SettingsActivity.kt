package com.example.quanlyphongtro

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity

class SettingsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        val hasRoom = getSharedPreferences("app_prefs", MODE_PRIVATE)
            .getBoolean("has_room", true)

        val btnChangePassword = findViewById<Button>(R.id.btnChangePassword)
        val btnSave = findViewById<Button>(R.id.btnSave)
        val btnLogout = findViewById<TextView>(R.id.btnLogout)
        val navHome = findViewById<LinearLayout>(R.id.navHome)
        val navManage = findViewById<LinearLayout>(R.id.navManage)
        val navNotify = findViewById<LinearLayout>(R.id.navNotify)
        val navSettings = findViewById<LinearLayout>(R.id.navSettings)

        // Đổi mật khẩu
        btnChangePassword.setOnClickListener {
            // TODO: Mở màn đổi mật khẩu
        }

        // Lưu thay đổi
        btnSave.setOnClickListener {
            val fullName = findViewById<android.widget.EditText>(R.id.etFullName).text.toString()
            val email = findViewById<android.widget.EditText>(R.id.etEmail).text.toString()
            val cccd = findViewById<android.widget.EditText>(R.id.etCCCD).text.toString()
            // TODO: Gọi API cập nhật thông tin
        }

        // Đăng xuất
        btnLogout.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("Đăng xuất")
                .setMessage("Bạn có chắc muốn đăng xuất không?")
                .setPositiveButton("Đăng xuất") { _, _ ->
                    // Xóa token
                    val prefs = getSharedPreferences("app_prefs", MODE_PRIVATE)
                    prefs.edit().clear().apply()
                    // Về màn đăng nhập
                    val intent = Intent(this, LoginActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(intent)
                }
                .setNegativeButton("Huỷ", null)
                .show()
        }

        // Bottom Navigation — home/manage destination depends on user type
        navHome.setOnClickListener {
            if (hasRoom) {
                startActivity(Intent(this, TenantMainActivity::class.java))
            } else {
                startActivity(Intent(this, RoomListingActivity::class.java))
            }
            finish()
        }
        navManage.setOnClickListener {
            if (hasRoom) {
                startActivity(Intent(this, ContractActivity::class.java))
                finish()
            } else {
                Toast.makeText(this, "Bạn chưa thuê phòng nào", Toast.LENGTH_SHORT).show()
            }
        }
        navNotify.setOnClickListener {
            startActivity(Intent(this, NotificationActivity::class.java))
            finish()
        }
        navSettings.setOnClickListener { /* Already on this screen */ }
    }
}
