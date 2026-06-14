package com.example.quanlyphongtro

import android.content.Intent
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

class PaymentNotificationActivity : AppCompatActivity() {

    private lateinit var tokenManager: TokenManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_payment_notification)

        tokenManager = TokenManager(this)

        val btnBack          = findViewById<android.widget.ImageView>(R.id.btnBack)
        val btnConfirmPayment = findViewById<Button>(R.id.btnConfirmPayment)
        val tvUpload         = findViewById<TextView>(R.id.tvUpload)
        val tvTotalAmount    = findViewById<TextView>(R.id.tvTotalAmount)
        val tvRoomFee        = findViewById<TextView>(R.id.tvRoomFee)
        val tvElectricFee    = findViewById<TextView>(R.id.tvElectricFee)
        val tvWaterFee       = findViewById<TextView>(R.id.tvWaterFee)
        val tvLandlordName   = findViewById<TextView>(R.id.tvLandlordName)
        val tvBankAccount    = findViewById<TextView>(R.id.tvBankAccount)
        val navHome          = findViewById<LinearLayout>(R.id.navHome)
        val navManage        = findViewById<LinearLayout>(R.id.navManage)
        val navNotify        = findViewById<LinearLayout>(R.id.navNotify)
        val navSettings      = findViewById<LinearLayout>(R.id.navSettings)

        btnBack.setOnClickListener { finish() }

        loadCurrentUnpaidInvoice(tvTotalAmount, tvRoomFee, tvElectricFee, tvWaterFee, tvLandlordName, tvBankAccount)

        btnConfirmPayment.setOnClickListener {
            Toast.makeText(this, "Đã gửi xác nhận thanh toán!", Toast.LENGTH_SHORT).show()
        }

        tvUpload.setOnClickListener {
            Toast.makeText(this, "Tính năng tải ảnh đang phát triển", Toast.LENGTH_SHORT).show()
        }

        navHome.setOnClickListener {
            startActivity(Intent(this, TenantMainActivity::class.java)); finish()
        }
        navManage.setOnClickListener {
            startActivity(Intent(this, ContractActivity::class.java)); finish()
        }
        navNotify.setOnClickListener {
            startActivity(Intent(this, NotificationActivity::class.java)); finish()
        }
        navSettings.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java)); finish()
        }
    }

    private fun loadCurrentUnpaidInvoice(
        tvTotal: TextView, tvRoom: TextView,
        tvElec: TextView, tvWater: TextView,
        tvLandlord: TextView, tvBank: TextView
    ) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val resp = RetrofitClient.instance.getCurrentUnpaidInvoice(tokenManager.getBearer())
                withContext(Dispatchers.Main) {
                    val inv = resp.invoice
                    tvTotal.text = formatMoney(inv.grand_total)
                    tvRoom.text  = formatMoney(inv.room_price)
                    tvElec.text  = "${formatMoney(inv.total_electric)} (${inv.electricity_usage}kWh)"
                    tvWater.text = "${formatMoney(inv.total_water)} (${inv.water_usage}m³)"

                    val bank = resp.bank_info
                    tvBank.text = "STK: ${bank.account_number} - ${bank.bank_name}"
                    tvLandlord.text = "Chủ nhà: ${bank.account_name}"
                }
            } catch (e: retrofit2.HttpException) {
                withContext(Dispatchers.Main) {
                    val msg = if (e.code() == 404)
                        "Bạn không có hóa đơn chưa thanh toán"
                    else "Lỗi tải hóa đơn: ${e.code()}"
                    Toast.makeText(this@PaymentNotificationActivity, msg, Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@PaymentNotificationActivity,
                        "Không thể kết nối đến máy chủ", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}
