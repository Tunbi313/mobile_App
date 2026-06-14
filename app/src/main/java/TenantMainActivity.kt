package com.example.quanlyphongtro

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class TenantMainActivity : AppCompatActivity() {

    private lateinit var tokenManager: TokenManager
    private var landlordPhone = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_tenant_main)

        tokenManager = TokenManager(this)

        val tvAmountDue   = findViewById<TextView>(R.id.tvAmountDue)
        val tvRoomNumber  = findViewById<TextView>(R.id.tvRoomNumber)
        val tvRentPrice   = findViewById<TextView>(R.id.tvRentPrice)
        val tvServiceFee  = findViewById<TextView>(R.id.tvServiceFee)
        val tvLandlordName  = findViewById<TextView>(R.id.tvLandlordName)
        val tvLandlordPhone = findViewById<TextView>(R.id.tvLandlordPhone)
        val btnPayNow     = findViewById<Button>(R.id.btnPayNow)
        val btnMessage    = findViewById<Button>(R.id.btnMessage)
        val btnCall       = findViewById<Button>(R.id.btnCall)
        val navHome       = findViewById<LinearLayout>(R.id.navHome)
        val navManage     = findViewById<LinearLayout>(R.id.navManage)
        val navNotify     = findViewById<LinearLayout>(R.id.navNotify)
        val navSettings   = findViewById<LinearLayout>(R.id.navSettings)

        // Load dashboard data from API
        loadDashboard(tvAmountDue, tvRoomNumber, tvRentPrice, tvServiceFee, tvLandlordName, tvLandlordPhone)

        btnPayNow.setOnClickListener {
            startActivity(Intent(this, PaymentNotificationActivity::class.java))
        }

        btnMessage.setOnClickListener {
            val smsIntent = Intent(Intent.ACTION_SENDTO).apply {
                data = Uri.parse("smsto:${landlordPhone.ifEmpty { "0901234567" }}")
            }
            startActivity(smsIntent)
        }

        btnCall.setOnClickListener {
            val callIntent = Intent(Intent.ACTION_DIAL).apply {
                data = Uri.parse("tel:${landlordPhone.ifEmpty { "0901234567" }}")
            }
            startActivity(callIntent)
        }

        navHome.setOnClickListener { /* Already here */ }
        navManage.setOnClickListener {
            startActivity(Intent(this, ContractActivity::class.java))
        }
        navNotify.setOnClickListener {
            startActivity(Intent(this, NotificationActivity::class.java))
        }
        navSettings.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }
    }

    private fun loadDashboard(
        tvAmount: TextView, tvRoom: TextView, tvPrice: TextView,
        tvService: TextView, tvName: TextView, tvPhone: TextView
    ) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = RetrofitClient.instance.getTenantDashboard(tokenManager.getBearer())
                withContext(Dispatchers.Main) {
                    // Room info
                    tvRoom.text    = response.room.name
                    tvPrice.text   = "${formatMoney(response.room.price)}/tháng"
                    tvService.text = "100.000đ/tháng"

                    // Unpaid amount
                    tvAmount.text  = formatMoney(response.unpaid_total)

                    // Landlord
                    tvName.text    = response.landlord.full_name ?: "Chủ nhà"
                    tvPhone.text   = response.landlord.phone ?: ""
                    landlordPhone  = response.landlord.phone ?: ""

                    // Save state
                    tokenManager.saveHasRoom(true)
                }
            } catch (e: retrofit2.HttpException) {
                if (e.code() == 404) {
                    // Tenant has no room → go to listing
                    withContext(Dispatchers.Main) {
                        tokenManager.saveHasRoom(false)
                        startActivity(Intent(this@TenantMainActivity, RoomListingActivity::class.java))
                        finish()
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@TenantMainActivity,
                            "Lỗi tải dữ liệu: ${e.message()}", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@TenantMainActivity,
                        "Không thể kết nối đến máy chủ", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}
