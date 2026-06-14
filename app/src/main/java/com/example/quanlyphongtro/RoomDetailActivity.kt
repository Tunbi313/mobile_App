package com.example.quanlyphongtro

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.example.quanlyphongtro.network.RetrofitClient
import com.example.quanlyphongtro.network.SessionManager
import com.example.quanlyphongtro.network.RoomResponse
import com.google.android.material.card.MaterialCardView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.text.NumberFormat
import java.util.Locale

class RoomDetailActivity : AppCompatActivity() {

    private lateinit var session: SessionManager
    private var roomId: Int = -1
    private var isModified = false
    private var initialStatus: String? = null

    // ── Header ──────────────────────────────────────────────────────
    private lateinit var btnBack: ImageView
    private lateinit var tvTitle: TextView

    // ── Thông tin phòng ─────────────────────────────────────────────
    private lateinit var tvStatusBadge: TextView
    private lateinit var tvRoomNameLarge: TextView
    private lateinit var tvAreaValue: TextView
    private lateinit var tvPriceValue: TextView
    private lateinit var layoutAmenities: LinearLayout
    private lateinit var tvDetailElectricPrice: TextView
    private lateinit var tvDetailWaterPrice: TextView
    private lateinit var tvDetailServiceFee: TextView

    // ── Người thuê ───────────────────────────────────────────────────
    private lateinit var cardTenantInfo: MaterialCardView
    private lateinit var tvTenantAvatar: TextView
    private lateinit var tvTenantNameDetail: TextView
    private lateinit var tvTenantPhoneDetail: TextView
    private lateinit var btnCallTenant: MaterialCardView
    private lateinit var tvMoveInDate: TextView
    private lateinit var tvDepositValue: TextView

    // ── Nút chức năng ────────────────────────────────────────────────
    private lateinit var btnContract: MaterialCardView
    private lateinit var btnInvoice: MaterialCardView
    private lateinit var fabEditRoom: FloatingActionButton
    private lateinit var ivRoomImage: ImageView
    private lateinit var btnUploadRoomImage: CardView

    private val editRoomLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            isModified = true
            loadRoomDetail()
        }
    }

    private val pickImageLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) uploadRoomImage(uri)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_room_detail)

        session = SessionManager(this)
        roomId  = intent.getIntExtra("ROOM_ID", -1)

        if (roomId == -1) {
            Toast.makeText(this, "Lỗi: Không tìm thấy ID phòng!", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        bindViews()
        setupListeners()

        // Hiện loading ngay khi vào màn hình
        tvTitle.text          = "Đang tải..."
        tvRoomNameLarge.text  = "..."
        tvAreaValue.text      = "--"
        tvPriceValue.text     = "--"
        cardTenantInfo.visibility = View.GONE

        loadRoomDetail()
    }

    private fun bindViews() {
        btnBack              = findViewById(R.id.btnBack)
        tvTitle              = findViewById(R.id.tvTitle)
        tvStatusBadge        = findViewById(R.id.tvStatusBadge)
        tvRoomNameLarge      = findViewById(R.id.tvRoomNameLarge)
        tvAreaValue          = findViewById(R.id.tvAreaValue)
        tvPriceValue         = findViewById(R.id.tvPriceValue)
        layoutAmenities      = findViewById(R.id.layoutAmenities)
        tvDetailElectricPrice = findViewById(R.id.tvDetailElectricPrice)
        tvDetailWaterPrice   = findViewById(R.id.tvDetailWaterPrice)
        tvDetailServiceFee   = findViewById(R.id.tvDetailServiceFee)

        cardTenantInfo       = findViewById(R.id.cardTenantInfo)
        tvTenantAvatar       = findViewById(R.id.tvTenantAvatar)
        tvTenantNameDetail   = findViewById(R.id.tvTenantNameDetail)
        tvTenantPhoneDetail  = findViewById(R.id.tvTenantPhoneDetail)
        btnCallTenant        = findViewById(R.id.btnCallTenant)
        tvMoveInDate         = findViewById(R.id.tvMoveInDate)
        tvDepositValue       = findViewById(R.id.tvDepositValue)

        btnContract          = findViewById(R.id.btnContract)
        btnInvoice           = findViewById(R.id.btnInvoice)
        fabEditRoom          = findViewById(R.id.fabEditRoom)
        ivRoomImage          = findViewById(R.id.ivRoomImage)
        btnUploadRoomImage   = findViewById(R.id.btnUploadRoomImage)
    }

    private fun setupListeners() {
        btnBack.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        fabEditRoom.setOnClickListener {
            val intent = Intent(this, EditRoomActivity::class.java).apply {
                putExtra("ROOM_ID", roomId)
            }
            editRoomLauncher.launch(intent)
        }

        btnUploadRoomImage.setOnClickListener {
            pickImageLauncher.launch("image/*")
        }
    }

    override fun finish() {
        if (isModified) setResult(RESULT_OK)
        super.finish()
    }

    override fun onResume() {
        super.onResume()
        if (roomId != -1) {
            loadRoomDetail()
            loadUnitPrices()
        }
    }

    // ── Gọi API lấy chi tiết phòng ───────────────────────────────────
    private fun loadRoomDetail() {
        lifecycleScope.launch {
            try {
                val token    = session.bearerToken()
                val response = RetrofitClient.api.getRoomDetail(token, roomId)

                if (response.isSuccessful && response.body() != null) {
                    bindRoomData(response.body()!!)
                } else {
                    when (response.code()) {
                        401 -> {
                            Toast.makeText(
                                this@RoomDetailActivity,
                                "Phiên đăng nhập hết hạn. Vui lòng đăng nhập lại!",
                                Toast.LENGTH_LONG
                            ).show()
                            session.clearSession()
                            val intent = Intent(this@RoomDetailActivity, LoginActivity::class.java)
                            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                            startActivity(intent)
                            finish()
                        }
                        else -> {
                            Toast.makeText(
                                this@RoomDetailActivity,
                                "Lấy thông tin phòng thất bại! (${response.code()})",
                                Toast.LENGTH_SHORT
                            ).show()
                            tvTitle.text = "Chi tiết phòng"
                        }
                    }
                }
            } catch (e: Exception) {
                Toast.makeText(this@RoomDetailActivity, "Lỗi kết nối máy chủ!", Toast.LENGTH_SHORT).show()
                tvTitle.text = "Chi tiết phòng"
            }
        }
    }

    // ── Bind dữ liệu thật lên UI ─────────────────────────────────────
    private fun bindRoomData(room: RoomResponse) {
        if (initialStatus == null) {
            initialStatus = room.status
        } else if (initialStatus != room.status) {
            isModified = true
        }

        // Header
        tvTitle.text         = "Chi tiết ${room.name}"
        tvRoomNameLarge.text = room.name

        // Ảnh phòng
        if (!room.room_image_url.isNullOrEmpty()) {
            Glide.with(this)
                .load(room.room_image_url)
                .placeholder(R.drawable.img_6)
                .error(R.drawable.img_6)
                .into(ivRoomImage)
        }

        // Diện tích
        val areaStr = room.area.toLong().toString()
        tvAreaValue.text = "$areaStr m²"

        // Giá phòng
        val priceLong      = room.price.toDoubleOrNull()?.toLong() ?: 0L
        val formattedPrice = NumberFormat.getCurrencyInstance(Locale("vi", "VN")).format(priceLong)
        tvPriceValue.text  = formattedPrice

        // ── Trạng thái ──────────────────────────────────────────────
        if (room.status == "occupied") {
            tvStatusBadge.text = "✔ Đang cho thuê"
            tvStatusBadge.backgroundTintList =
                ContextCompat.getColorStateList(this, R.color.badge_occupied_bg)
            tvStatusBadge.setTextColor(ContextCompat.getColor(this, R.color.badge_occupied_text))

            // Hiện card người thuê CHỈ KHI có dữ liệu thật từ DB
            if (room.tenant_name != null) {
                cardTenantInfo.visibility = View.VISIBLE

                val tenantName = room.tenant_name
                tvTenantNameDetail.text  = tenantName
                tvTenantPhoneDetail.text = room.tenant_phone ?: "Chưa cập nhật SĐT"
                tvTenantAvatar.text      = tenantName.take(1).uppercase()

                // Ngày vào
                val rawMoveIn = room.move_in ?: "--"
                tvMoveInDate.text = try {
                    val parts = rawMoveIn.split("T")[0].split("-")
                    if (parts.size == 3) "${parts[2]}/${parts[1]}/${parts[0]}" else rawMoveIn
                } catch (e: Exception) { rawMoveIn }

                // Tiền cọc
                val depositLong     = room.deposit?.toDoubleOrNull()?.toLong() ?: 0L
                val formattedDeposit = NumberFormat.getCurrencyInstance(Locale("vi", "VN")).format(depositLong)
                tvDepositValue.text  = formattedDeposit

                // Gọi điện
                btnCallTenant.setOnClickListener {
                    val phone = room.tenant_phone
                    if (!phone.isNullOrEmpty()) {
                        startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phone")))
                    } else {
                        Toast.makeText(this, "Không có số điện thoại người thuê!", Toast.LENGTH_SHORT).show()
                    }
                }
            } else {
                // Phòng có trạng thái occupied nhưng DB chưa có tenant record
                cardTenantInfo.visibility = View.GONE
            }
        } else {
            // Phòng trống
            tvStatusBadge.text = "✔ Còn trống"
            tvStatusBadge.backgroundTintList =
                ContextCompat.getColorStateList(this, R.color.badge_available_bg)
            tvStatusBadge.setTextColor(ContextCompat.getColor(this, R.color.badge_available_text))

            cardTenantInfo.visibility = View.GONE
        }

        // ── Tiện ích ─────────────────────────────────────────────────
        bindAmenities(room.description)

        // ── Nút Hợp đồng / Hóa đơn ──────────────────────────────────
        btnContract.setOnClickListener {
            if (room.status == "occupied") {
                startActivity(Intent(this, ContractActivity::class.java).apply {
                    putExtra("ROOM_ID", room.id)
                    putExtra("sign_date", room.move_in ?: "")
                    putExtra("duration", "${room.duration_months ?: 12} tháng")
                    putExtra("landlord_name", session.getDisplayName())
                    putExtra("tenant_name", room.tenant_name ?: "")
                })
            } else {
                startActivity(Intent(this, AddTenantActivity::class.java).apply {
                    putExtra("ROOM_ID", room.id)
                })
            }
        }
        btnInvoice.setOnClickListener {
            Toast.makeText(this, "Hóa đơn phòng ${room.name}", Toast.LENGTH_SHORT).show()
        }
    }

    // ── Bind tiện ích dạng chip ───────────────────────────────────────
    private fun bindAmenities(description: String) {
        layoutAmenities.removeAllViews()

        val list = description
            .split(Regex("[,;\n]"))
            .map { it.trim() }
            .filter { it.isNotEmpty() }

        if (list.isEmpty()) {
            val tvEmpty = TextView(this).apply {
                text = "Chưa cập nhật tiện ích"
                setTextColor(ContextCompat.getColor(this@RoomDetailActivity, R.color.gray_light))
                textSize = 13f
            }
            layoutAmenities.addView(tvEmpty)
        } else {
            val density       = resources.displayMetrics.density
            val padH          = (12 * density).toInt()
            val padV          = (6 * density).toInt()
            val rightMarginPx = (8 * density).toInt()

            for (amenity in list) {
                val tv = TextView(this).apply {
                    text = amenity
                    setBackgroundResource(R.drawable.bg_badge_gray)
                    setPadding(padH, padV, padH, padV)
                    setTextColor(ContextCompat.getColor(this@RoomDetailActivity, R.color.badge_available_text))
                    textSize     = 12f
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    ).apply { rightMargin = rightMarginPx }
                }
                layoutAmenities.addView(tv)
            }
        }
    }

    private fun uploadRoomImage(uri: Uri) {
        lifecycleScope.launch {
            try {
                val bytes = contentResolver.openInputStream(uri)?.readBytes() ?: return@launch
                val mimeType = contentResolver.getType(uri) ?: "image/jpeg"
                val requestBody = bytes.toRequestBody(mimeType.toMediaTypeOrNull())
                val part = MultipartBody.Part.createFormData(
                    "room_image", "room_image.jpg", requestBody
                )
                val token = session.bearerToken()
                val resp = RetrofitClient.api.uploadRoomImage(token, roomId, part)
                if (resp.isSuccessful && resp.body() != null) {
                    val url = resp.body()!!.room_image_url
                    if (!url.isNullOrEmpty()) {
                        Glide.with(this@RoomDetailActivity)
                            .load(url)
                            .placeholder(R.drawable.img_6)
                            .error(R.drawable.img_6)
                            .into(ivRoomImage)
                    }
                    Toast.makeText(this@RoomDetailActivity, "Tải ảnh lên thành công!", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this@RoomDetailActivity, "Tải ảnh thất bại (${resp.code()})", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@RoomDetailActivity, "Lỗi: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun loadUnitPrices() {
        ApiClient.get(this, "/api/invoices/unit-price/", object : ApiClient.ApiCallback {
            override fun onSuccess(response: String) {
                try {
                    val json = org.json.JSONObject(response)
                    val elec = json.optLong("electricity_price", 3500L)
                    val water = json.optLong("water_price", 15000L)
                    val service = json.optLong("common_service_fee", 100000L)

                    val format = java.text.NumberFormat.getNumberInstance(Locale("vi", "VN"))
                    tvDetailElectricPrice.text = "${format.format(elec)} đ/kWh"
                    tvDetailWaterPrice.text = "${format.format(water)} đ/m³"
                    tvDetailServiceFee.text = "${format.format(service)} đ"
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            override fun onError(error: String) {
                // Keep default
            }
        })
    }
}
