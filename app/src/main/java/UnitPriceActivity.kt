package com.example.quanlyphongtro

import android.content.Intent
import android.os.Bundle
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import org.json.JSONObject

class UnitPriceActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_unit_price)

        val btnBack = findViewById<ImageView>(R.id.btnBack)
        val navHome = findViewById<LinearLayout>(R.id.navHome)
        val navManage = findViewById<LinearLayout>(R.id.navManage)
        val navNotify = findViewById<LinearLayout>(R.id.navNotify)
        val navSettings = findViewById<LinearLayout>(R.id.navSettings)
        val btnSave = findViewById<LinearLayout>(R.id.btnSave)

        val etElectricityPrice = findViewById<EditText>(R.id.etElectricityPrice)
        val etWaterPrice = findViewById<EditText>(R.id.etWaterPrice)
        val etCommonFee = findViewById<EditText>(R.id.etCommonFee)
        val txtLastUpdated = findViewById<TextView>(R.id.txtLastUpdated)

        // Tải đơn giá từ API khi vào màn hình
        ApiClient.get(this, "/api/invoices/unit-price/", object : ApiClient.ApiCallback {
            override fun onSuccess(response: String) {
                try {
                    val json = JSONObject(response)
                    etElectricityPrice.setText(json.optString("electricity_price", "3.500"))
                    etWaterPrice.setText(json.optString("water_price", "15.000"))
                    etCommonFee.setText(json.optString("common_service_fee", "100.000"))
                    
                    val lastUpdated = json.optString("last_updated", "")
                    if (lastUpdated.isNotEmpty()) {
                        txtLastUpdated.text = "Lần cập nhật cuối: $lastUpdated"
                    }
                } catch (e: Exception) {
                    Toast.makeText(this@UnitPriceActivity, "Lỗi phân tích đơn giá", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onError(error: String) {
                Toast.makeText(this@UnitPriceActivity, "Không thể tải đơn giá từ server", Toast.LENGTH_SHORT).show()
            }
        })

        // Quay lại
        btnBack.setOnClickListener {
            finish()
        }

        // Click nút Lưu -> Gửi API cập nhật đơn giá
        btnSave.setOnClickListener {
            // Chuẩn hóa định dạng chuỗi số trước khi gửi (ví dụ: "3.500" -> 3500)
            val elecText = etElectricityPrice.text.toString().replace(".", "").replace(",", "")
            val waterText = etWaterPrice.text.toString().replace(".", "").replace(",", "")
            val commonText = etCommonFee.text.toString().replace(".", "").replace(",", "")

            val jsonBody = JSONObject()
            jsonBody.put("electricity_price", elecText.toLongOrNull() ?: 3500L)
            jsonBody.put("water_price", waterText.toLongOrNull() ?: 15000L)
            jsonBody.put("common_service_fee", commonText.toLongOrNull() ?: 100000L)

            ApiClient.post(this, "/api/invoices/unit-price/", jsonBody, object : ApiClient.ApiCallback {
                override fun onSuccess(response: String) {
                    Toast.makeText(this@UnitPriceActivity, "Lưu cài đặt thành công!", Toast.LENGTH_SHORT).show()
                    finish()
                }

                override fun onError(error: String) {
                    Toast.makeText(this@UnitPriceActivity, "Không thể lưu cài đặt đơn giá", Toast.LENGTH_SHORT).show()
                }
            })
        }

        // Bottom Navigation (Hành động chuyển đổi màn hình mẫu)
        navHome.setOnClickListener {
            startActivity(Intent(this, OwnerMainActivity::class.java))
            finish()
        }

        navManage.setOnClickListener {
            startActivity(Intent(this, InvoiceListActivity::class.java))
            finish()
        }

        navNotify.setOnClickListener {
            startActivity(Intent(this, PaymentApprovalActivity::class.java))
            finish()
        }

        navSettings.setOnClickListener {
            // Đang ở màn hình cài đặt đơn giá
        }
    }
}
