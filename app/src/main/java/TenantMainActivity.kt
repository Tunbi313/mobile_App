package com.example.quanlyphongtro

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class TenantMainActivity : AppCompatActivity() {

    private lateinit var tokenManager: TokenManager
    private var landlordPhone = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_tenant_main)

        tokenManager = TokenManager(this)

        val tvAmountDue       = findViewById<TextView>(R.id.tvAmountDue)
        val tvRoomNumber      = findViewById<TextView>(R.id.tvRoomNumber)
        val tvRentPrice       = findViewById<TextView>(R.id.tvRentPrice)
        val tvServiceFee      = findViewById<TextView>(R.id.tvServiceFee)
        val tvLandlordName    = findViewById<TextView>(R.id.tvLandlordName)
        val tvLandlordPhone   = findViewById<TextView>(R.id.tvLandlordPhone)
        val tvLandlordInitial = findViewById<TextView>(R.id.tvLandlordInitial)
        val tvGreeting        = findViewById<TextView>(R.id.tvGreeting)
        val tvHeaderName      = findViewById<TextView>(R.id.tvHeaderName)
        val ivAvatar          = findViewById<ImageView>(R.id.ivAvatar)
        val btnPayNow         = findViewById<Button>(R.id.btnPayNow)
        val btnMessage        = findViewById<Button>(R.id.btnMessage)
        val btnCall           = findViewById<Button>(R.id.btnCall)
        val navHome           = findViewById<LinearLayout>(R.id.navHome)
        val navManage         = findViewById<LinearLayout>(R.id.navManage)
        val navNotify         = findViewById<LinearLayout>(R.id.navNotify)
        val navSettings       = findViewById<LinearLayout>(R.id.navSettings)

        loadDashboard(tvAmountDue, tvRoomNumber, tvRentPrice, tvServiceFee,
            tvLandlordName, tvLandlordPhone, tvLandlordInitial, tvGreeting, tvHeaderName, ivAvatar)

        btnPayNow.setOnClickListener {
            startActivity(Intent(this, PaymentNotificationActivity::class.java))
        }

        btnMessage.setOnClickListener {
            val smsIntent = Intent(Intent.ACTION_SENDTO).apply {
                data = Uri.parse("smsto:$landlordPhone")
            }
            if (landlordPhone.isNotEmpty()) startActivity(smsIntent)
            else Toast.makeText(this, "Không có số điện thoại chủ nhà", Toast.LENGTH_SHORT).show()
        }

        btnCall.setOnClickListener {
            if (landlordPhone.isNotEmpty()) {
                val callIntent = Intent(Intent.ACTION_DIAL).apply {
                    data = Uri.parse("tel:$landlordPhone")
                }
                startActivity(callIntent)
            } else {
                Toast.makeText(this, "Không có số điện thoại chủ nhà", Toast.LENGTH_SHORT).show()
            }
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
        tvService: TextView, tvName: TextView, tvPhone: TextView,
        tvInitial: TextView, tvGreeting: TextView, tvHeaderName: TextView,
        ivAvatar: ImageView
    ) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = RetrofitClient.instance.getTenantDashboard(tokenManager.getBearer())

                val profileDeferred = async {
                    try { RetrofitClient.instance.getProfile(tokenManager.getBearer()) } catch (e: Exception) { null }
                }
                val invoiceDeferred = async {
                    try { RetrofitClient.instance.getCurrentUnpaidInvoice(tokenManager.getBearer()) } catch (e: Exception) { null }
                }

                val profile = profileDeferred.await()
                val invoiceResp = invoiceDeferred.await()

                withContext(Dispatchers.Main) {
                    tvRoom.text  = response.room.name
                    tvPrice.text = "${formatMoney(response.room.price)}/tháng"
                    tvService.text = if (invoiceResp != null)
                        "${formatMoney(invoiceResp.invoice.service_price)}/tháng" else ""
                    tvAmount.text = formatMoney(response.unpaid_total)

                    val landlordName = response.landlord.full_name ?: "Chủ nhà"
                    tvName.text    = landlordName
                    tvPhone.text   = response.landlord.phone ?: ""
                    landlordPhone  = response.landlord.phone ?: ""
                    tvInitial.text = landlordName.firstOrNull()?.toString() ?: "C"

                    val displayName = profile?.full_name
                        ?: profile?.username
                        ?: tokenManager.getUsername()
                        ?: "Người dùng"
                    tvGreeting.text   = "Xin chào, $displayName"
                    tvHeaderName.text = displayName

                    if (!profile?.avatar_url.isNullOrEmpty()) {
                        Glide.with(this@TenantMainActivity)
                            .load(profile!!.avatar_url)
                            .circleCrop()
                            .placeholder(R.drawable.img_6)
                            .error(R.drawable.img_6)
                            .into(ivAvatar)
                    }

                    tokenManager.saveHasRoom(true)
                }
            } catch (e: retrofit2.HttpException) {
                if (e.code() == 404) {
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
