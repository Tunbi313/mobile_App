package com.example.quanlyphongtro

import android.content.Intent
import android.graphics.Typeface
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView

class RoomListingActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_room_listing)

        val tvGreeting    = findViewById<TextView>(R.id.tvGreeting)
        val tvSubtitle    = findViewById<TextView>(R.id.tvSubtitle)
        val chipAll       = findViewById<TextView>(R.id.chipAll)
        val chipUnder3m   = findViewById<TextView>(R.id.chipUnder3m)
        val chipAC        = findViewById<TextView>(R.id.chipAC)
        val chipHN        = findViewById<TextView>(R.id.chipHN)
        val cardRoom302   = findViewById<CardView>(R.id.cardRoom302)
        val cardRoom105   = findViewById<CardView>(R.id.cardRoom105)
        val cardRoom401   = findViewById<CardView>(R.id.cardRoom401)
        val tvEmptyState  = findViewById<TextView>(R.id.tvEmptyState)
        val btnCall       = findViewById<Button>(R.id.btnCall)
        val btnZalo       = findViewById<Button>(R.id.btnZalo)
        val navHome       = findViewById<LinearLayout>(R.id.navHome)
        val navManage     = findViewById<LinearLayout>(R.id.navManage)
        val navNotify     = findViewById<LinearLayout>(R.id.navNotify)
        val navSettings   = findViewById<LinearLayout>(R.id.navSettings)

        // User=2 has no rented room — update greeting
        tvGreeting.text = "Chào bạn!"
        tvSubtitle.text = "Bạn chưa thuê phòng. Hãy chọn phòng phù hợp bên dưới!"

        // ── Filter chips ──────────────────────────────────────────────────────
        val allChips = listOf(chipAll, chipUnder3m, chipAC, chipHN)

        fun setActiveChip(active: TextView) {
            allChips.forEach { chip ->
                if (chip === active) {
                    chip.setBackgroundResource(R.drawable.bg_chip_active)
                    chip.setTextColor(getColor(R.color.white))
                    chip.setTypeface(null, Typeface.BOLD)
                } else {
                    chip.setBackgroundResource(R.drawable.bg_chip_inactive)
                    chip.setTextColor(getColor(R.color.text_primary))
                    chip.setTypeface(null, Typeface.NORMAL)
                }
            }
        }

        fun showRooms(show302: Boolean, show105: Boolean, show401: Boolean) {
            cardRoom302.visibility  = if (show302) View.VISIBLE else View.GONE
            cardRoom105.visibility  = if (show105) View.VISIBLE else View.GONE
            cardRoom401.visibility  = if (show401) View.VISIBLE else View.GONE
            tvEmptyState.visibility = if (!show302 && !show105 && !show401) View.VISIBLE else View.GONE
        }

        chipAll.setOnClickListener {
            setActiveChip(chipAll)
            showRooms(true, true, true)
        }

        // No rooms are under 3 million, show empty state
        chipUnder3m.setOnClickListener {
            setActiveChip(chipUnder3m)
            showRooms(false, false, false)
        }

        // Phòng 302 and 105 have AC
        chipAC.setOnClickListener {
            setActiveChip(chipAC)
            showRooms(true, true, false)
        }

        // All rooms are in Hà Nội
        chipHN.setOnClickListener {
            setActiveChip(chipHN)
            showRooms(true, true, true)
        }

        // ── Room card clicks ──────────────────────────────────────────────────
        cardRoom302.setOnClickListener {
            startActivity(Intent(this, RoomDetailActivity::class.java).apply {
                putExtra("room_name", "Phòng 302")
                putExtra("price", "3.500.000đ/tháng")
                putExtra("area", "25 m²")
                putExtra("capacity", "2 người")
                putExtra("floor", "Tầng 3")
                putExtra("phone", "0901234567")
            })
        }
        cardRoom105.setOnClickListener {
            startActivity(Intent(this, RoomDetailActivity::class.java).apply {
                putExtra("room_name", "Phòng 105")
                putExtra("price", "4.200.000đ/tháng")
                putExtra("area", "30 m²")
                putExtra("capacity", "3 người")
                putExtra("floor", "Tầng 1")
                putExtra("phone", "0901234567")
            })
        }
        cardRoom401.setOnClickListener {
            startActivity(Intent(this, RoomDetailActivity::class.java).apply {
                putExtra("room_name", "Phòng 401")
                putExtra("price", "3.800.000đ/tháng")
                putExtra("area", "28 m²")
                putExtra("capacity", "2 người")
                putExtra("floor", "Tầng 4")
                putExtra("phone", "0901234567")
            })
        }

        // ── Support banner buttons ────────────────────────────────────────────
        btnCall.setOnClickListener {
            startActivity(Intent(Intent.ACTION_DIAL).apply {
                data = Uri.parse("tel:0901234567")
            })
        }

        btnZalo.setOnClickListener {
            startActivity(Intent(Intent.ACTION_SENDTO).apply {
                data = Uri.parse("smsto:0901234567")
            })
        }

        // ── Bottom navigation ─────────────────────────────────────────────────
        navHome.setOnClickListener { /* Already on this screen */ }

        navManage.setOnClickListener {
            Toast.makeText(this, "Bạn chưa thuê phòng nào", Toast.LENGTH_SHORT).show()
        }

        navNotify.setOnClickListener {
            startActivity(Intent(this, NotificationActivity::class.java))
        }

        navSettings.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }
    }
}
