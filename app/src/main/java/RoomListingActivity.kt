package com.example.quanlyphongtro

import android.content.Intent
import android.graphics.Typeface
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Button
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

class RoomListingActivity : AppCompatActivity() {

    private lateinit var tokenManager: TokenManager
    private lateinit var adapter: RoomAdapter
    private var allRooms = listOf<RoomData>()

    override fun onResume() {
        super.onResume()
        checkIfAssignedRoom()
    }

    private fun checkIfAssignedRoom() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                RetrofitClient.instance.getTenantDashboard(tokenManager.getBearer())
                withContext(Dispatchers.Main) {
                    tokenManager.saveHasRoom(true)
                    startActivity(Intent(this@RoomListingActivity, TenantMainActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
                    })
                    finish()
                }
            } catch (e: retrofit2.HttpException) {
                // 404 = chưa có phòng, ở lại đây
            } catch (e: Exception) {
                // Lỗi kết nối, bỏ qua
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_room_listing)

        tokenManager = TokenManager(this)

        val tvGreeting  = findViewById<TextView>(R.id.tvGreeting)
        val tvSubtitle  = findViewById<TextView>(R.id.tvSubtitle)
        val chipAll     = findViewById<TextView>(R.id.chipAll)
        val chipUnder3m = findViewById<TextView>(R.id.chipUnder3m)
        val chipAC      = findViewById<TextView>(R.id.chipAC)
        val chipHN      = findViewById<TextView>(R.id.chipHN)
        val progressBar = findViewById<ProgressBar>(R.id.progressBar)
        val tvEmpty     = findViewById<TextView>(R.id.tvEmptyState)
        val rv          = findViewById<RecyclerView>(R.id.rvRooms)
        val btnCall     = findViewById<Button>(R.id.btnCall)
        val btnZalo     = findViewById<Button>(R.id.btnZalo)
        val navHome     = findViewById<LinearLayout>(R.id.navHome)
        val navManage   = findViewById<LinearLayout>(R.id.navManage)
        val navNotify   = findViewById<LinearLayout>(R.id.navNotify)
        val navSettings = findViewById<LinearLayout>(R.id.navSettings)

        tvGreeting.text = "Chào bạn!"
        tvSubtitle.text = "Bạn chưa thuê phòng. Hãy chọn phòng phù hợp bên dưới!"

        adapter = RoomAdapter(mutableListOf()) { room ->
            startActivity(Intent(this, RoomDetailActivity::class.java).apply {
                putExtra("room_id",   room.id)
                putExtra("room_name", room.name)
                putExtra("price",     "${formatMoney(room.price)}/tháng")
                putExtra("area",      "${room.area.toInt()} m²")
                putExtra("capacity",  "${room.capacity ?: 2} người")
                putExtra("floor",     room.floor ?: "")
                putExtra("amenities", room.amenities ?: "")
                putExtra("description", room.description ?: "")
            })
        }
        rv.layoutManager = LinearLayoutManager(this)
        rv.adapter = adapter

        // ── Filter chips ────────────────────────────────────────────────────
        val allChips = listOf(chipAll, chipUnder3m, chipAC, chipHN)

        fun setActiveChip(active: TextView) {
            allChips.forEach { chip ->
                val isActive = chip === active
                chip.setBackgroundResource(
                    if (isActive) R.drawable.bg_chip_active else R.drawable.bg_chip_inactive
                )
                chip.setTextColor(
                    if (isActive) getColor(R.color.white) else getColor(R.color.text_primary)
                )
                chip.setTypeface(null, if (isActive) Typeface.BOLD else Typeface.NORMAL)
            }
        }

        fun applyFilter(rooms: List<RoomData>): List<RoomData> {
            val activeChip = allChips.firstOrNull { it.typeface?.isBold == true } ?: chipAll
            return when (activeChip) {
                chipUnder3m -> rooms.filter { it.price.toLongOrNull()?.let { p -> p < 3_000_000 } == true }
                chipAC      -> rooms.filter { it.amenities?.contains("Điều hòa", ignoreCase = true) == true || it.description?.contains("điều hòa", ignoreCase = true) == true }
                else        -> rooms
            }
        }

        fun showFiltered(filtered: List<RoomData>) {
            adapter.updateList(filtered)
            tvEmpty.visibility = if (filtered.isEmpty()) View.VISIBLE else View.GONE
        }

        chipAll.setOnClickListener     { setActiveChip(chipAll);     showFiltered(applyFilter(allRooms)) }
        chipUnder3m.setOnClickListener { setActiveChip(chipUnder3m); showFiltered(applyFilter(allRooms)) }
        chipAC.setOnClickListener      { setActiveChip(chipAC);      showFiltered(applyFilter(allRooms)) }
        chipHN.setOnClickListener      { setActiveChip(chipHN);      showFiltered(allRooms) }

        // ── Load rooms from API ──────────────────────────────────────────────
        progressBar.visibility = View.VISIBLE
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val rooms = RetrofitClient.instance.getAvailableRooms(tokenManager.getBearer())
                withContext(Dispatchers.Main) {
                    progressBar.visibility = View.GONE
                    allRooms = rooms
                    showFiltered(rooms)
                    tvSubtitle.text = if (rooms.isEmpty())
                        "Hiện tại không có phòng trống."
                    else "${rooms.size} phòng đang trống"
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    progressBar.visibility = View.GONE
                    Toast.makeText(this@RoomListingActivity,
                        "Không thể tải danh sách phòng", Toast.LENGTH_SHORT).show()
                }
            }
        }

        btnCall.setOnClickListener {
            startActivity(Intent(Intent.ACTION_DIAL).apply { data = Uri.parse("tel:0901234567") })
        }
        btnZalo.setOnClickListener {
            startActivity(Intent(Intent.ACTION_SENDTO).apply { data = Uri.parse("smsto:0901234567") })
        }

        navHome.setOnClickListener { checkIfAssignedRoom() }
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
