package com.example.quanlyphongtro

import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.example.quanlyphongtro.network.RetrofitClient
import com.example.quanlyphongtro.network.SessionManager
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody

class ContractActivity : AppCompatActivity() {

    private lateinit var session: SessionManager
    private var roomId: Int = -1

    private lateinit var ivContractImage: ImageView
    private lateinit var layoutUploadHint: LinearLayout
    private lateinit var btnUploadImage: Button
    private lateinit var tvSignDate: TextView
    private lateinit var tvDuration: TextView
    private lateinit var tvLandlordName: TextView
    private lateinit var tvTenantName: TextView

    private val pickImageLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) uploadContractImage(uri)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_contract)

        session = SessionManager(this)
        roomId  = intent.getIntExtra("ROOM_ID", -1)

        val btnBack     = findViewById<ImageView>(R.id.btnBack)
        val fabDownload = findViewById<CardView>(R.id.fabDownload)

        ivContractImage  = findViewById(R.id.ivContractImage)
        layoutUploadHint = findViewById(R.id.layoutUploadHint)
        btnUploadImage   = findViewById(R.id.btnUploadImage)
        tvSignDate       = findViewById(R.id.tvSignDate)
        tvDuration       = findViewById(R.id.tvDuration)
        tvLandlordName   = findViewById(R.id.tvLandlordName)
        tvTenantName     = findViewById(R.id.tvTenantName)

        // Hiển thị dữ liệu từ Intent ngay lập tức (khi API chưa trả về)
        tvSignDate.text     = intent.getStringExtra("sign_date")      ?: "--"
        tvDuration.text     = intent.getStringExtra("duration")       ?: "--"
        tvLandlordName.text = intent.getStringExtra("landlord_name")  ?: "--"
        tvTenantName.text   = intent.getStringExtra("tenant_name")    ?: "--"

        btnBack.setOnClickListener { finish() }
        fabDownload.setOnClickListener {
            Toast.makeText(this, "Chức năng tải PDF đang phát triển", Toast.LENGTH_SHORT).show()
        }

        btnUploadImage.setOnClickListener {
            pickImageLauncher.launch("image/*")
        }

        // Nếu có room_id thì tải thêm dữ liệu hợp đồng từ API (kể cả ảnh)
        if (roomId != -1) {
            loadContract()
        }
    }

    private fun loadContract() {
        lifecycleScope.launch {
            try {
                val resp = RetrofitClient.api.getRoomContract(session.bearerToken(), roomId)
                if (resp.isSuccessful) {
                    val contract = resp.body() ?: return@launch
                    tvSignDate.text    = contract.move_in
                    tvDuration.text    = "${contract.duration_months} tháng"
                    tvLandlordName.text = contract.landlord_name ?: "--"
                    tvTenantName.text  = contract.tenant_name   ?: "--"
                    showContractImage(contract.contract_image_url)
                }
            } catch (_: Exception) {
                // Giữ dữ liệu từ Intent nếu API lỗi
            }
        }
    }

    private fun showContractImage(url: String?) {
        if (!url.isNullOrEmpty()) {
            layoutUploadHint.visibility = View.GONE
            Glide.with(this)
                .load(url)
                .placeholder(R.drawable.ic_house)
                .error(R.drawable.ic_house)
                .centerCrop()
                .into(ivContractImage)
        } else {
            layoutUploadHint.visibility = View.VISIBLE
        }
    }

    private fun uploadContractImage(uri: Uri) {
        lifecycleScope.launch {
            try {
                val bytes    = contentResolver.openInputStream(uri)?.readBytes() ?: return@launch
                val mimeType = contentResolver.getType(uri) ?: "image/jpeg"
                val reqBody  = bytes.toRequestBody(mimeType.toMediaTypeOrNull())
                val part     = MultipartBody.Part.createFormData("contract_image", "contract.jpg", reqBody)
                val resp     = RetrofitClient.api.uploadContractImage(session.bearerToken(), roomId, part)
                if (resp.isSuccessful && resp.body() != null) {
                    showContractImage(resp.body()!!.contract_image_url)
                    Toast.makeText(this@ContractActivity, "Tải ảnh hợp đồng thành công!", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this@ContractActivity, "Tải ảnh thất bại (${resp.code()})", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@ContractActivity, "Lỗi: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
