package com.example.quanlyphongtro

import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import com.bumptech.glide.Glide
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

        val btnBack          = findViewById<ImageView>(R.id.btnBack)
        val fabDownload      = findViewById<CardView>(R.id.fabDownload)
        val tvContractTitle  = findViewById<TextView>(R.id.tvContractTitle)
        val tvSignDate       = findViewById<TextView>(R.id.tvSignDate)
        val tvDuration       = findViewById<TextView>(R.id.tvDuration)
        val tvLandlordName   = findViewById<TextView>(R.id.tvLandlordName)
        val tvTenantName     = findViewById<TextView>(R.id.tvTenantName)
        val ivContractImage  = findViewById<ImageView>(R.id.ivContractImage)
        val navHome          = findViewById<LinearLayout>(R.id.navHome)
        val navContract      = findViewById<LinearLayout>(R.id.navContract)
        val navNotify        = findViewById<LinearLayout>(R.id.navNotify)
        val navSettings      = findViewById<LinearLayout>(R.id.navSettings)

        btnBack.setOnClickListener { finish() }

        fabDownload.setOnClickListener {
            Toast.makeText(this, "Tính năng tải PDF đang phát triển", Toast.LENGTH_SHORT).show()
        }

        loadContract(tvContractTitle, tvSignDate, tvDuration, tvLandlordName, tvTenantName, ivContractImage)

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
        tvTitle: TextView, tvDate: TextView, tvDuration: TextView,
        tvLandlord: TextView, tvTenant: TextView, ivImage: ImageView
    ) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val contract = RetrofitClient.instance.getTenantContract(tokenManager.getBearer())
                withContext(Dispatchers.Main) {
                    tvTitle.text    = "Hợp đồng thuê phòng ${contract.room_name}"
                    tvDate.text     = contract.move_in
                    tvDuration.text = "${contract.duration_months} tháng"
                    tvLandlord.text = contract.landlord_name ?: "Chủ nhà"
                    tvTenant.text   = contract.tenant_name ?: tokenManager.getUsername() ?: "Khách thuê"

                    if (!contract.contract_image_url.isNullOrEmpty()) {
                        Glide.with(this@ContractActivity)
                            .load(contract.contract_image_url)
                            .placeholder(R.drawable.ic_house)
                            .error(R.drawable.ic_house)
                            .into(ivImage)
                    }
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
