package com.example.quanlyphongtro

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView

class RoomDetailActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_room_detail)

        val btnBack = findViewById<CardView>(R.id.btnBack)
        val btnContact = findViewById<Button>(R.id.btnContact)
        val btnMessage = findViewById<CardView>(R.id.btnMessage)

        // Nhận data từ Intent
        val roomName = intent.getStringExtra("room_name") ?: "Phòng 302"
        val price = intent.getStringExtra("price") ?: "3.500.000 đ/tháng"
        val area = intent.getStringExtra("area") ?: "20m2"
        val capacity = intent.getStringExtra("capacity") ?: "3 người"
        val floor = intent.getStringExtra("floor") ?: "Tầng 2"
        val phone = intent.getStringExtra("phone") ?: "0901234567"

        // Set data
        findViewById<android.widget.TextView>(R.id.tvRoomName).text = roomName
        findViewById<android.widget.TextView>(R.id.tvPrice).text = price
        findViewById<android.widget.TextView>(R.id.tvArea).text = area
        findViewById<android.widget.TextView>(R.id.tvCapacity).text = capacity
        findViewById<android.widget.TextView>(R.id.tvFloor).text = floor

        // Quay lại
        btnBack.setOnClickListener { finish() }

        // Gọi điện
        btnContact.setOnClickListener {
            val intent = Intent(Intent.ACTION_DIAL).apply {
                data = Uri.parse("tel:$phone")
            }
            startActivity(intent)
        }

        // Nhắn tin
        btnMessage.setOnClickListener {
            val intent = Intent(Intent.ACTION_SENDTO).apply {
                data = Uri.parse("smsto:$phone")
            }
            startActivity(intent)
        }
    }
}
