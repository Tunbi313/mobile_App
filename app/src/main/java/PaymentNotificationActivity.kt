package com.example.quanlyphongtro

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class PaymentNotificationActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_payment_notification)

        val btnBack = findViewById<android.widget.ImageView>(R.id.btnBack)
        val btnConfirmPayment = findViewById<Button>(R.id.btnConfirmPayment)
        val tvUpload = findViewById<TextView>(R.id.tvUpload)
        val navHome = findViewById<LinearLayout>(R.id.navHome)
        val navManage = findViewById<LinearLayout>(R.id.navManage)
        val navNotify = findViewById<LinearLayout>(R.id.navNotify)
        val navSettings = findViewById<LinearLayout>(R.id.navSettings)

        // Quay lại
        btnBack.setOnClickListener { finish() }

        // Xác nhận đã chuyển khoản
        btnConfirmPayment.setOnClickListener {
            // TODO: Gọi API xác nhận thanh toán
        }

        // Tải ảnh lên
        tvUpload.setOnClickListener {
            // TODO: Mở gallery để chọn ảnh
        }

        // Bottom Navigation
        navHome.setOnClickListener {
            startActivity(Intent(this, TenantMainActivity::class.java))
            finish()
        }
        navManage.setOnClickListener {
            startActivity(Intent(this, ContractActivity::class.java))
            finish()
        }
        navNotify.setOnClickListener {
            startActivity(Intent(this, NotificationActivity::class.java))
            finish()
        }
        navSettings.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
            finish()
        }
    }
}
