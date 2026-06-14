package com.example.quanlyphongtro

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class NotificationActivity : AppCompatActivity() {

    private lateinit var tokenManager: TokenManager
    private lateinit var adapter: NotificationAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_notification)

        tokenManager = TokenManager(this)

        val tvSubtitle  = findViewById<TextView>(R.id.tvSubtitle)
        val progressBar = findViewById<ProgressBar>(R.id.progressBar)
        val tvEmpty     = findViewById<TextView>(R.id.tvEmpty)
        val rv          = findViewById<RecyclerView>(R.id.rvNotifications)
        val navHome     = findViewById<LinearLayout>(R.id.navHome)
        val navManage   = findViewById<LinearLayout>(R.id.navManage)
        val navNotify   = findViewById<LinearLayout>(R.id.navNotify)
        val navSettings = findViewById<LinearLayout>(R.id.navSettings)

        adapter = NotificationAdapter(mutableListOf()) { notif ->
            // Open PaymentNotificationActivity for payment-related notifications
            if (notif.notif_type == "payment_due" || notif.notif_type == "new_invoice") {
                startActivity(Intent(this, PaymentNotificationActivity::class.java))
            }
            // Mark as read via API (fire and forget)
            if (!notif.is_read) {
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        RetrofitClient.instance.markNotificationRead(tokenManager.getBearer(), notif.id)
                    } catch (_: Exception) {}
                }
            }
        }

        rv.layoutManager = LinearLayoutManager(this)
        rv.adapter = adapter

        // Load notifications
        progressBar.visibility = View.VISIBLE
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val list = RetrofitClient.instance.getNotifications(tokenManager.getBearer())
                withContext(Dispatchers.Main) {
                    progressBar.visibility = View.GONE
                    if (list.isEmpty()) {
                        tvEmpty.visibility = View.VISIBLE
                        tvSubtitle.text = "Không có thông báo nào"
                    } else {
                        val unread = list.count { !it.is_read }
                        tvSubtitle.text = if (unread > 0)
                            "$unread thông báo chưa đọc" else "Tất cả đã đọc"
                        adapter.updateList(list)
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    progressBar.visibility = View.GONE
                    Toast.makeText(this@NotificationActivity,
                        "Không thể tải thông báo", Toast.LENGTH_SHORT).show()
                }
            }
        }

        val hasRoom = tokenManager.hasRoom()
        navHome.setOnClickListener {
            if (hasRoom) startActivity(Intent(this, TenantMainActivity::class.java))
            else         startActivity(Intent(this, RoomListingActivity::class.java))
            finish()
        }
        navManage.setOnClickListener {
            if (hasRoom) { startActivity(Intent(this, ContractActivity::class.java)); finish() }
            else Toast.makeText(this, "Bạn chưa thuê phòng nào", Toast.LENGTH_SHORT).show()
        }
        navNotify.setOnClickListener { /* Already here */ }
        navSettings.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java)); finish()
        }
    }
}
