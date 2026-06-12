package com.example.quanlyphongtro

import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.quanlyphongtro.network.SessionManager
import java.text.NumberFormat
import java.util.Calendar
import java.util.Locale

class InvoiceListActivity : AppCompatActivity() {

    private lateinit var session: SessionManager
    private lateinit var invoiceAdapter: InvoiceAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_invoice_list)

        session = SessionManager(this)

        setupHeader()
        setupRecyclerView()
        setupFilters()
        setupBottomNav()
    }

    override fun onResume() {
        super.onResume()
        loadInvoices()
    }

    private fun setupHeader() {
        val logoutAction = {
            session.clearSession()
            val intent = Intent(this, LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
            Toast.makeText(this, "Đã đăng xuất", Toast.LENGTH_SHORT).show()
        }
        findViewById<android.view.View>(R.id.btnSwitchRole).setOnClickListener { logoutAction() }
        findViewById<android.view.View>(R.id.cardAvatar).setOnClickListener { logoutAction() }
    }

    private fun setupRecyclerView() {
        val rvInvoices = findViewById<RecyclerView>(R.id.rvInvoices)
        rvInvoices.layoutManager = LinearLayoutManager(this)
        invoiceAdapter = InvoiceAdapter(emptyList()) { invoice ->
            Toast.makeText(this, "Hóa đơn ${invoice.roomName}", Toast.LENGTH_SHORT).show()
        }
        rvInvoices.adapter = invoiceAdapter
    }

    private fun setupFilters() {
        val month = Calendar.getInstance().get(Calendar.MONTH) + 1
        findViewById<TextView>(R.id.tvMonthFilter).text = "Tháng $month"

        findViewById<android.view.View>(R.id.btnMonthFilter).setOnClickListener {
            Toast.makeText(this, "Chọn tháng", Toast.LENGTH_SHORT).show()
        }
        findViewById<android.view.View>(R.id.btnFilter).setOnClickListener {
            Toast.makeText(this, "Bộ lọc", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupBottomNav() {
        // Tab Trang chủ → quay về OwnerMainActivity
        findViewById<android.view.View>(R.id.navHome).setOnClickListener {
            startActivity(Intent(this, OwnerMainActivity::class.java))
            finish()
        }
        // Tab Quản lý → đang ở đây rồi, không làm gì
        findViewById<android.view.View>(R.id.navManage).setOnClickListener { /* đang ở đây */ }
        // Tab Thông báo → hiển thị thông báo
        findViewById<android.view.View>(R.id.navNotify).setOnClickListener {
            Toast.makeText(this, "Tính năng Thông báo đang phát triển", Toast.LENGTH_SHORT).show()
        }
        // Tab Cài đặt -> mở màn cài đặt đơn giá
        findViewById<android.view.View>(R.id.navSettings).setOnClickListener {
            startActivity(Intent(this, UnitPriceActivity::class.java))
        }
    }

    private fun loadInvoices() {
        ApiClient.get(this, "/api/invoices/", object : ApiClient.ApiCallback {
            override fun onSuccess(response: String) {
                try {
                    val jsonArray = org.json.JSONArray(response)
                    val items = ArrayList<InvoiceItem>()
                    var totalCollected = 0L
                    var totalUncollected = 0L
                    var countCollected = 0
                    var countUncollected = 0

                    for (i in 0 until jsonArray.length()) {
                        val obj = jsonArray.getJSONObject(i)
                        val roomObj = obj.optJSONObject("room_details")
                        val roomName = roomObj?.optString("name", "") ?: ""
                        val roomNumber = roomName.replace(Regex("[^0-9]"), "")
                        val tenantName = roomObj?.optString("tenant_name", "Khách thuê") ?: "Khách thuê"
                        val grandTotal = obj.optDouble("grand_total", 0.0).toLong()
                        val isPaid = obj.optBoolean("is_paid", false)
                        val month = obj.optInt("month", 0)
                        val year = obj.optInt("year", 0)

                        val dateLabel = if (isPaid) {
                            "Thanh toán: Tháng $month/$year"
                        } else {
                            "Chưa thu: Tháng $month/$year"
                        }

                        items.add(
                            InvoiceItem(
                                roomNumber = if (roomNumber.isNotEmpty()) roomNumber else roomName,
                                roomName = roomName,
                                tenantName = tenantName,
                                amount = grandTotal,
                                isPaid = isPaid,
                                dateLabel = dateLabel
                            )
                        )

                        if (isPaid) {
                            totalCollected += grandTotal
                            countCollected++
                        } else {
                            totalUncollected += grandTotal
                            countUncollected++
                        }
                    }

                    invoiceAdapter.updateInvoices(items)

                    val formatter = NumberFormat.getNumberInstance(Locale("vi", "VN"))
                    findViewById<TextView>(R.id.tvTotalInvoices).text = jsonArray.length().toString()
                    findViewById<TextView>(R.id.tvCollectedAmount).text = "${formatter.format(totalCollected)} đ"
                    findViewById<TextView>(R.id.tvCollectedCount).text = "$countCollected hóa đơn"
                    findViewById<TextView>(R.id.tvUncollectedAmount).text = "${formatter.format(totalUncollected)} đ"
                    findViewById<TextView>(R.id.tvUncollectedCount).text = "$countUncollected hóa đơn"

                } catch (e: Exception) {
                    e.printStackTrace()
                    Toast.makeText(this@InvoiceListActivity, "Lỗi phân tích danh sách hóa đơn", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onError(error: String) {
                Toast.makeText(this@InvoiceListActivity, "Không thể kết nối danh sách hóa đơn", Toast.LENGTH_SHORT).show()
            }
        })
    }
}
