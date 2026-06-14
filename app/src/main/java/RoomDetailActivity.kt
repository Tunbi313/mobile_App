package com.example.quanlyphongtro

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class RoomDetailActivity : AppCompatActivity() {

    private lateinit var tokenManager: TokenManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_room_detail)

        tokenManager = TokenManager(this)

        val btnBack    = findViewById<CardView>(R.id.btnBack)
        val btnContact = findViewById<Button>(R.id.btnContact)
        val btnMessage = findViewById<CardView>(R.id.btnMessage)

        val tvRoomName = findViewById<TextView>(R.id.tvRoomName)
        val tvPrice    = findViewById<TextView>(R.id.tvPrice)
        val tvArea     = findViewById<TextView>(R.id.tvArea)
        val tvCapacity = findViewById<TextView>(R.id.tvCapacity)
        val tvFloor    = findViewById<TextView>(R.id.tvFloor)

        // Data passed via Intent from RoomListingActivity
        val roomId      = intent.getIntExtra("room_id", -1)
        val roomName    = intent.getStringExtra("room_name") ?: "Phòng"
        val priceStr    = intent.getStringExtra("price")    ?: ""
        val areaStr     = intent.getStringExtra("area")     ?: ""
        val capacityStr = intent.getStringExtra("capacity") ?: "2 người"
        val floorStr    = intent.getStringExtra("floor")    ?: ""
        val amenities   = intent.getStringExtra("amenities") ?: ""
        val description = intent.getStringExtra("description") ?: ""

        // Populate from Intent (fast)
        tvRoomName.text = roomName
        tvPrice.text    = priceStr
        tvArea.text     = areaStr
        tvCapacity.text = capacityStr
        tvFloor.text    = floorStr

        var contactPhone = "0901234567"

        // If we have a roomId, refresh from API to get latest landlord phone
        if (roomId != -1) {
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val room = RetrofitClient.instance.getRoomDetail(tokenManager.getBearer(), roomId)
                    withContext(Dispatchers.Main) {
                        tvRoomName.text = room.name
                        tvPrice.text    = "${formatMoney(room.price)}/tháng"
                        tvArea.text     = "${room.area.toInt()} m²"
                        tvCapacity.text = "${room.capacity ?: 2} người"
                        tvFloor.text    = room.floor ?: ""
                        // Landlord phone not in room detail for non-tenants; keep default
                    }
                } catch (_: Exception) {
                    // Silently keep Intent data on error
                }
            }
        }

        btnBack.setOnClickListener { finish() }

        btnContact.setOnClickListener {
            startActivity(Intent(Intent.ACTION_DIAL).apply {
                data = Uri.parse("tel:$contactPhone")
            })
        }

        btnMessage.setOnClickListener {
            startActivity(Intent(Intent.ACTION_SENDTO).apply {
                data = Uri.parse("smsto:$contactPhone")
            })
        }
    }
}
