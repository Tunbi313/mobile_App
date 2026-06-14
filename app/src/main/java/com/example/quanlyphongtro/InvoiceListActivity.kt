package com.example.quanlyphongtro

import android.content.Intent
import android.os.Bundle
import android.widget.LinearLayout
import android.widget.NumberPicker
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
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

    private var selectedMonth      = Calendar.getInstance().get(Calendar.MONTH) + 1
    private var selectedYear       = Calendar.getInstance().get(Calendar.YEAR)
    private var selectedStatus     = "all" // "all" | "true" | "false"
    private var isMonthFilterActive = false

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
        updateMonthFilterText()

        findViewById<android.view.View>(R.id.btnMonthFilter).setOnClickListener {
            showMonthPicker()
        }
        findViewById<android.view.View>(R.id.btnFilter).setOnClickListener {
            showStatusFilter()
        }
    }

    private fun updateMonthFilterText() {
        val label = if (isMonthFilterActive) "Tháng $selectedMonth/$selectedYear" else "Tất cả tháng"
        findViewById<TextView>(R.id.tvMonthFilter).text = label
    }

    private fun showMonthPicker() {
        val currentYear = Calendar.getInstance().get(Calendar.YEAR)

        val monthPicker = NumberPicker(this).apply {
            minValue = 1
            maxValue = 12
            value = selectedMonth
            displayedValues = (1..12).map { "Tháng $it" }.toTypedArray()
            wrapSelectorWheel = true
        }

        val yearPicker = NumberPicker(this).apply {
            minValue = 2020
            maxValue = currentYear + 1
            value = selectedYear
            wrapSelectorWheel = false
        }

        val container = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = android.view.Gravity.CENTER
            setPadding(48, 32, 48, 16)
        }
        container.addView(monthPicker)
        container.addView(yearPicker)

        AlertDialog.Builder(this)
            .setTitle("Chọn tháng")
            .setView(container)
            .setPositiveButton("Xem") { _, _ ->
                selectedMonth       = monthPicker.value
                selectedYear        = yearPicker.value
                isMonthFilterActive = true
                updateMonthFilterText()
                loadInvoices()
            }
            .setNegativeButton("Huỷ", null)
            .show()
    }

    private fun showStatusFilter() {
        val options = arrayOf("Tất cả", "Đã thu", "Chưa thu")
        val currentIndex = when (selectedStatus) {
            "true"  -> 1
            "false" -> 2
            else    -> 0
        }

        AlertDialog.Builder(this)
            .setTitle("Lọc trạng thái")
            .setSingleChoiceItems(options, currentIndex) { dialog, which ->
                selectedStatus = when (which) {
                    1    -> "true"
                    2    -> "false"
                    else -> "all"
                }
                dialog.dismiss()
                loadInvoices()
            }
            .setNegativeButton("Huỷ", null)
            .show()
    }

    private fun buildEndpoint(): String {
        val params = mutableListOf<String>()
        if (isMonthFilterActive) {
            params.add("month=$selectedMonth")
            params.add("year=$selectedYear")
        }
        if (selectedStatus != "all") params.add("is_paid=$selectedStatus")
        return if (params.isEmpty()) "/api/invoices/" else "/api/invoices/?${params.joinToString("&")}"
    }

    private fun setupBottomNav() {
        findViewById<android.view.View>(R.id.navHome).setOnClickListener {
            startActivity(Intent(this, OwnerMainActivity::class.java))
            finish()
        }
        findViewById<android.view.View>(R.id.navManage).setOnClickListener { /* đang ở đây */ }
        findViewById<android.view.View>(R.id.navNotify).setOnClickListener {
            startActivity(Intent(this, PaymentApprovalActivity::class.java))
            finish()
        }
        findViewById<android.view.View>(R.id.navSettings).setOnClickListener {
            startActivity(Intent(this, UnitPriceActivity::class.java))
        }
    }

    private fun loadInvoices() {
        ApiClient.get(this, buildEndpoint(), object : ApiClient.ApiCallback {
            override fun onSuccess(response: String) {
                try {
                    val jsonArray = org.json.JSONArray(response)
                    val items = ArrayList<InvoiceItem>()
                    var totalCollected   = 0L
                    var totalUncollected = 0L
                    var countCollected   = 0
                    var countUncollected = 0

                    for (i in 0 until jsonArray.length()) {
                        val obj      = jsonArray.getJSONObject(i)
                        val roomObj  = obj.optJSONObject("room_details")
                        val roomName = roomObj?.optString("name", "") ?: ""
                        val roomNumber = roomName.replace(Regex("[^0-9]"), "")
                        val tenantName = roomObj?.optString("tenant_name", "Khách thuê") ?: "Khách thuê"
                        val grandTotal = obj.optDouble("grand_total", 0.0).toLong()
                        val isPaid     = obj.optBoolean("is_paid", false)
                        val month      = obj.optInt("month", 0)
                        val year       = obj.optInt("year", 0)

                        val dateLabel = if (isPaid) "Đã thu: Tháng $month/$year"
                                        else        "Chưa thu: Tháng $month/$year"

                        items.add(
                            InvoiceItem(
                                roomNumber = if (roomNumber.isNotEmpty()) roomNumber else roomName,
                                roomName   = roomName,
                                tenantName = tenantName,
                                amount     = grandTotal,
                                isPaid     = isPaid,
                                dateLabel  = dateLabel
                            )
                        )

                        if (isPaid) { totalCollected   += grandTotal; countCollected++ }
                        else        { totalUncollected += grandTotal; countUncollected++ }
                    }

                    invoiceAdapter.updateInvoices(items)

                    val formatter = NumberFormat.getNumberInstance(Locale("vi", "VN"))
                    findViewById<TextView>(R.id.tvTotalInvoices).text      = jsonArray.length().toString()
                    findViewById<TextView>(R.id.tvCollectedAmount).text    = "${formatter.format(totalCollected)} đ"
                    findViewById<TextView>(R.id.tvCollectedCount).text     = "$countCollected hóa đơn"
                    findViewById<TextView>(R.id.tvUncollectedAmount).text  = "${formatter.format(totalUncollected)} đ"
                    findViewById<TextView>(R.id.tvUncollectedCount).text   = "$countUncollected hóa đơn"

                } catch (e: Exception) {
                    Toast.makeText(this@InvoiceListActivity, "Lỗi phân tích danh sách hóa đơn", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onError(error: String) {
                Toast.makeText(this@InvoiceListActivity, "Không thể kết nối danh sách hóa đơn", Toast.LENGTH_SHORT).show()
            }
        })
    }
}
