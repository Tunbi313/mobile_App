package com.example.quanlyphongtro

import android.app.Dialog
import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Bundle
import android.view.View
import android.view.Window
import android.view.WindowManager
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.quanlyphongtro.network.PendingPaymentData
import com.example.quanlyphongtro.network.RetrofitClient
import com.example.quanlyphongtro.network.SessionManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.URL

class PaymentApprovalActivity : AppCompatActivity() {

    private lateinit var session: SessionManager
    private lateinit var adapter: PendingPaymentAdapter
    private var pendingList: MutableList<PendingPaymentData> = mutableListOf()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_payment)

        session = SessionManager(this)

        setupRecyclerView()
        setupBottomNav()

        findViewById<ImageView>(R.id.btnBack).setOnClickListener { finish() }
    }

    override fun onResume() {
        super.onResume()
        loadPendingPayments()
    }

    private fun setupRecyclerView() {
        val rv = findViewById<RecyclerView>(R.id.rvPendingPayments)
        rv.layoutManager = LinearLayoutManager(this)

        adapter = PendingPaymentAdapter(
            items      = emptyList(),
            onApprove  = { item -> confirmApprove(item) },
            onReject   = { item -> confirmReject(item) },
            onViewProof = { url -> showProofFullscreen(url) }
        )
        rv.adapter = adapter
    }

    private fun loadPendingPayments() {
        lifecycleScope.launch {
            try {
                val resp = RetrofitClient.api.getPendingPayments(session.bearerToken())
                if (resp.isSuccessful) {
                    pendingList = (resp.body() ?: emptyList()).toMutableList()
                    adapter.updateItems(pendingList)

                    val count = pendingList.size
                    findViewById<TextView>(R.id.tvPendingCount).text =
                        if (count > 0) "$count yêu cầu chờ duyệt" else ""

                    val llEmpty = findViewById<LinearLayout>(R.id.llEmpty)
                    val rv      = findViewById<RecyclerView>(R.id.rvPendingPayments)
                    llEmpty.visibility = if (count == 0) View.VISIBLE else View.GONE
                    rv.visibility      = if (count == 0) View.GONE   else View.VISIBLE
                } else {
                    Toast.makeText(this@PaymentApprovalActivity, "Lỗi tải danh sách", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@PaymentApprovalActivity, "Không thể kết nối máy chủ", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun confirmApprove(item: PendingPaymentData) {
        AlertDialog.Builder(this)
            .setTitle("Duyệt thanh toán")
            .setMessage("Xác nhận duyệt thanh toán ${formatDialogMoney(item.grand_total)} VNĐ của ${item.room_name} – ${item.tenant_name}?")
            .setPositiveButton("Duyệt") { _, _ -> approvePayment(item) }
            .setNegativeButton("Huỷ", null)
            .show()
    }

    private fun confirmReject(item: PendingPaymentData) {
        AlertDialog.Builder(this)
            .setTitle("Từ chối thanh toán")
            .setMessage("Bạn có chắc muốn từ chối thanh toán của ${item.room_name} – ${item.tenant_name}?")
            .setPositiveButton("Từ chối") { _, _ -> rejectPayment(item) }
            .setNegativeButton("Huỷ", null)
            .show()
    }

    private fun approvePayment(item: PendingPaymentData) {
        lifecycleScope.launch {
            try {
                val resp = RetrofitClient.api.approvePayment(session.bearerToken(), item.id)
                if (resp.isSuccessful) {
                    Toast.makeText(
                        this@PaymentApprovalActivity,
                        "Đã duyệt thanh toán cho ${item.room_name}",
                        Toast.LENGTH_SHORT
                    ).show()
                    loadPendingPayments()
                } else {
                    val errMsg = resp.errorBody()?.string() ?: "Lỗi không xác định"
                    Toast.makeText(this@PaymentApprovalActivity, "Lỗi: $errMsg", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@PaymentApprovalActivity, "Lỗi kết nối: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun rejectPayment(item: PendingPaymentData) {
        lifecycleScope.launch {
            try {
                val resp = RetrofitClient.api.rejectPayment(session.bearerToken(), item.id)
                if (resp.isSuccessful) {
                    Toast.makeText(
                        this@PaymentApprovalActivity,
                        "Đã từ chối thanh toán của ${item.room_name}",
                        Toast.LENGTH_SHORT
                    ).show()
                    loadPendingPayments()
                } else {
                    val errMsg = resp.errorBody()?.string() ?: "Lỗi không xác định"
                    Toast.makeText(this@PaymentApprovalActivity, "Lỗi: $errMsg", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@PaymentApprovalActivity, "Lỗi kết nối: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showProofFullscreen(url: String) {
        val iv = ImageView(this).apply {
            scaleType = ImageView.ScaleType.FIT_CENTER
            setBackgroundColor(0xFF000000.toInt())
        }

        val dialog = Dialog(this, android.R.style.Theme_Black_NoTitleBar_Fullscreen)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(iv)
        dialog.window?.apply {
            setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.MATCH_PARENT)
            setBackgroundDrawableResource(android.R.color.black)
        }
        dialog.setCanceledOnTouchOutside(true)
        iv.setOnClickListener { dialog.dismiss() }
        dialog.show()

        lifecycleScope.launch {
            try {
                val bitmap = withContext(Dispatchers.IO) {
                    BitmapFactory.decodeStream(URL(url).openStream())
                }
                iv.setImageBitmap(bitmap)
            } catch (_: Exception) {
                Toast.makeText(this@PaymentApprovalActivity, "Không thể tải ảnh", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun setupBottomNav() {
        findViewById<LinearLayout>(R.id.navHome).setOnClickListener {
            startActivity(Intent(this, OwnerMainActivity::class.java)); finish()
        }
        findViewById<LinearLayout>(R.id.navManage).setOnClickListener {
            startActivity(Intent(this, InvoiceListActivity::class.java)); finish()
        }
        findViewById<LinearLayout>(R.id.navNotify).setOnClickListener { /* đang ở đây */ }
        findViewById<LinearLayout>(R.id.navSettings).setOnClickListener {
            startActivity(Intent(this, UnitPriceActivity::class.java)); finish()
        }
    }
}

private fun formatDialogMoney(amount: String?): String {
    if (amount.isNullOrEmpty()) return "0"
    return try {
        "%,d".format(amount.toBigDecimal().toLong()).replace(',', '.')
    } catch (_: Exception) { amount }
}
