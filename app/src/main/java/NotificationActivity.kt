package com.example.quanlyphongtro

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView

class NotificationActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_notification)

        val navHome = findViewById<LinearLayout>(R.id.navHome)
        val navManage = findViewById<LinearLayout>(R.id.navManage)
        val navNotify = findViewById<LinearLayout>(R.id.navNotify)
        val navSettings = findViewById<LinearLayout>(R.id.navSettings)

        // Bottom Navigation
        navHome.setOnClickListener {
            startActivity(Intent(this, TenantMainActivity::class.java))
            finish()
        }
        navManage.setOnClickListener {
            startActivity(Intent(this, ContractActivity::class.java))
            finish()
        }
        navNotify.setOnClickListener {
            // Already on this screen
        }
        navSettings.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
            finish()
        }

        // Notification card click — mark as read
        setupNotifCard(R.id.cardNotif1, R.id.dotUnread1, openPayment = true)
        setupNotifCard(R.id.cardNotif2, R.id.dotUnread2)
    }

    private fun setupNotifCard(cardId: Int, dotId: Int, openPayment: Boolean = false) {
        val card = findViewById<CardView>(cardId)
        val dot = findViewById<View>(dotId)
        card.setOnClickListener {
            card.setCardBackgroundColor(0xFFE0E0E0.toInt())
            dot.visibility = View.GONE
            if (openPayment) {
                startActivity(Intent(this, PaymentNotificationActivity::class.java))
            }
        }
    }
}
