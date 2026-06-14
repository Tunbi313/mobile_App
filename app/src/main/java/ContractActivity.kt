package com.example.quanlyphongtro

import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ContractActivity : AppCompatActivity() {

    private lateinit var tokenManager: TokenManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_contract)

        tokenManager = TokenManager(this)

        val btnBack     = findViewById<ImageView>(R.id.btnBack)
        val fabDownload = findViewById<CardView>(R.id.fabDownload)
        val tvSignDate   = findViewById<TextView>(R.id.tvSignDate)
        val tvDuration   = findViewById<TextView>(R.id.tvDuration)
        val tvLandlordName = findViewById<TextView>(R.id.tvLandlordName)
        val tvTenantName   = findViewById<TextView>(R.id.tvTenantName)
        val navHome      = findViewById<LinearLayout>(R.id.navHome)
        val navContract  = findViewById<LinearLayout>(R.id.navContract)
        val navNotify    = findViewById<LinearLayout>(R.id.navNotify)
        val navSettings  = findViewById<LinearLayout>(R.id.navSettings)

        btnBack.setOnClickListener { finish() }

        fabDownload.setOnClickListener {
            Toast.makeText(this, "Tính năng tải PDF đang phát triển", Toast.LENGTH_SHORT).show()
        }

        loadContract(tvSignDate, tvDuration, tvLandlordName, tvTenantName)

        navHome.setOnClickListener {
            startActivity(Intent(this, TenantMainActivity::class.java)); finish()
        }
        navContract.setOnClickListener { /* Already here */ }
        navNotify.setOnClickListener {
            startActivity(Intent(this, NotificationActivity::class.java)); finish()
        }
        navSettings.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java)); finish()
        }
    }

    private fun loadContract(
        tvDate: TextView, tvDuration: TextView,
        tvLandlord: TextView, tvTenant: TextView
    ) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val contract = RetrofitClient.instance.getTenantContract(tokenManager.getBearer())
                withContext(Dispatchers.Main) {
                    tvDate.text     = contract.move_in
                    tvDuration.text = "${contract.duration_months} tháng"
                    tvLandlord.text = contract.landlord_name ?: "Chủ nhà"
                    tvTenant.text   = contract.tenant_name ?: tokenManager.getUsername() ?: "Khách thuê"
                }
            } catch (e: retrofit2.HttpException) {
                withContext(Dispatchers.Main) {
                    val msg = if (e.code() == 404)
                        "Bạn chưa có hợp đồng nào" else "Lỗi tải hợp đồng"
                    Toast.makeText(this@ContractActivity, msg, Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@ContractActivity,
                        "Không thể kết nối đến máy chủ", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}
