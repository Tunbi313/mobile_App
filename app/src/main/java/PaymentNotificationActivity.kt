package com.example.quanlyphongtro

import android.Manifest
import android.app.Activity
import android.app.Dialog
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.Window
import android.view.WindowManager
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import org.json.JSONObject
import java.io.File

class PaymentNotificationActivity : AppCompatActivity() {

    private lateinit var tokenManager: TokenManager
    private var selectedImageUri: Uri? = null
    private var currentInvoiceId: Int = -1

    private val pickImageLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            selectedImageUri = result.data?.data
            selectedImageUri?.let { uri ->
                findViewById<ImageView>(R.id.ivUploadPreview)?.apply {
                    setImageURI(uri)
                    visibility = View.VISIBLE
                }
                Toast.makeText(this, "Đã chọn ảnh, nhấn xác nhận để gửi", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) openGallery()
        else Toast.makeText(this, "Cần cấp quyền truy cập ảnh", Toast.LENGTH_SHORT).show()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_payment_notification)

        tokenManager = TokenManager(this)

        val btnBack           = findViewById<ImageView>(R.id.btnBack)
        val btnConfirmPayment = findViewById<Button>(R.id.btnConfirmPayment)
        val tvUpload          = findViewById<TextView>(R.id.tvUpload)
        val tvTotalAmount     = findViewById<TextView>(R.id.tvTotalAmount)
        val tvRoomFee         = findViewById<TextView>(R.id.tvRoomFee)
        val tvElectricFee     = findViewById<TextView>(R.id.tvElectricFee)
        val tvWaterFee        = findViewById<TextView>(R.id.tvWaterFee)
        val tvLandlordName    = findViewById<TextView>(R.id.tvLandlordName)
        val tvBankAccount     = findViewById<TextView>(R.id.tvBankAccount)
        val cardQrCode        = findViewById<CardView>(R.id.cardQrCode)
        val navHome           = findViewById<LinearLayout>(R.id.navHome)
        val navManage         = findViewById<LinearLayout>(R.id.navManage)
        val navNotify         = findViewById<LinearLayout>(R.id.navNotify)
        val navSettings       = findViewById<LinearLayout>(R.id.navSettings)

        btnBack.setOnClickListener { finish() }
        cardQrCode.setOnClickListener { showQrFullscreen() }

        tvUpload.setOnClickListener { checkPermissionAndOpenGallery() }

        btnConfirmPayment.setOnClickListener {
            if (selectedImageUri == null) {
                Toast.makeText(this, "Vui lòng chọn ảnh minh chứng trước khi xác nhận", Toast.LENGTH_SHORT).show()
            } else {
                uploadPaymentProof()
            }
        }

        loadCurrentUnpaidInvoice(tvTotalAmount, tvRoomFee, tvElectricFee, tvWaterFee, tvLandlordName, tvBankAccount)

        navHome.setOnClickListener {
            startActivity(Intent(this, TenantMainActivity::class.java)); finish()
        }
        navManage.setOnClickListener {
            startActivity(Intent(this, ContractActivity::class.java)); finish()
        }
        navNotify.setOnClickListener {
            startActivity(Intent(this, NotificationActivity::class.java)); finish()
        }
        navSettings.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java)); finish()
        }
    }

    private fun checkPermissionAndOpenGallery() {
        val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
            Manifest.permission.READ_MEDIA_IMAGES
        else
            Manifest.permission.READ_EXTERNAL_STORAGE

        if (ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED)
            openGallery()
        else
            requestPermissionLauncher.launch(permission)
    }

    private fun openGallery() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI).apply {
            type = "image/*"
        }
        pickImageLauncher.launch(intent)
    }

    private fun uploadPaymentProof() {
        val uri = selectedImageUri ?: return
        if (currentInvoiceId == -1) {
            Toast.makeText(this, "Không tìm thấy hóa đơn", Toast.LENGTH_SHORT).show()
            return
        }

        val btnConfirm = findViewById<Button>(R.id.btnConfirmPayment)
        btnConfirm.isEnabled = false
        btnConfirm.text = "Đang gửi..."

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val inputStream = contentResolver.openInputStream(uri)
                val file = File(cacheDir, "payment_proof.jpg")
                file.outputStream().use { inputStream?.copyTo(it) }

                val requestFile = file.asRequestBody("image/jpeg".toMediaTypeOrNull())
                val body = MultipartBody.Part.createFormData("payment_proof", file.name, requestFile)

                RetrofitClient.instance.submitPaymentProof(tokenManager.getBearer(), currentInvoiceId, body)

                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        this@PaymentNotificationActivity,
                        "Đã gửi xác nhận thanh toán, vui lòng chờ duyệt",
                        Toast.LENGTH_LONG
                    ).show()
                    updatePaymentStatusUI("pending")
                }
            } catch (e: retrofit2.HttpException) {
                withContext(Dispatchers.Main) {
                    val errBody = e.response()?.errorBody()?.string()
                    val msg = try {
                        JSONObject(errBody ?: "").optString("error", "Lỗi gửi ảnh")
                    } catch (_: Exception) { "Lỗi gửi ảnh: ${e.code()}" }
                    Toast.makeText(this@PaymentNotificationActivity, msg, Toast.LENGTH_LONG).show()
                    btnConfirm.isEnabled = true
                    btnConfirm.text = "Xác nhận đã chuyển khoản"
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        this@PaymentNotificationActivity,
                        "Lỗi kết nối: ${e.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                    btnConfirm.isEnabled = true
                    btnConfirm.text = "Xác nhận đã chuyển khoản"
                }
            }
        }
    }

    private fun loadCurrentUnpaidInvoice(
        tvTotal: TextView, tvRoom: TextView,
        tvElec: TextView, tvWater: TextView,
        tvLandlord: TextView, tvBank: TextView
    ) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val resp = RetrofitClient.instance.getCurrentUnpaidInvoice(tokenManager.getBearer())
                withContext(Dispatchers.Main) {
                    val inv = resp.invoice
                    currentInvoiceId = inv.id

                    tvTotal.text = formatMoney(inv.grand_total)
                    tvRoom.text  = formatMoney(inv.room_price)
                    tvElec.text  = "${formatMoney(inv.total_electric)} (${inv.electricity_usage} kWh)"
                    tvWater.text = "${formatMoney(inv.total_water)} (${inv.water_usage} m³)"

                    val bank = resp.bank_info
                    tvBank.text     = "STK: ${bank.account_number} - ${bank.bank_name}"
                    tvLandlord.text = "Chủ nhà: ${bank.account_name}"

                    findViewById<TextView>(R.id.tvMonth)?.text = "tháng ${inv.month}/${inv.year}"

                    updatePaymentStatusUI(inv.payment_status)
                    renderPaymentHistory(resp.payment_history)
                }
            } catch (e: retrofit2.HttpException) {
                withContext(Dispatchers.Main) {
                    val msg = if (e.code() == 404)
                        "Bạn không có hóa đơn chưa thanh toán"
                    else "Lỗi tải hóa đơn: ${e.code()}"
                    Toast.makeText(this@PaymentNotificationActivity, msg, Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        this@PaymentNotificationActivity,
                        "Không thể kết nối đến máy chủ",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    private fun updatePaymentStatusUI(status: String) {
        val llBadge    = findViewById<LinearLayout>(R.id.llPaymentBadge) ?: return
        val tvStatus   = findViewById<TextView>(R.id.tvPaymentStatus)    ?: return
        val ivIcon     = findViewById<ImageView>(R.id.ivStatusIcon)       ?: return
        val cardUpload = findViewById<CardView>(R.id.cardUpload)          ?: return
        val btnConfirm = findViewById<Button>(R.id.btnConfirmPayment)     ?: return
        val ivPreview  = findViewById<ImageView>(R.id.ivUploadPreview)

        when (status) {
            "pending" -> {
                val orange = Color.parseColor("#F59E0B")
                tvStatus.text = "Chờ xác nhận"
                tvStatus.setTextColor(orange)
                ivIcon.setColorFilter(orange)
                cardUpload.visibility = View.GONE
                ivPreview?.visibility = View.GONE
                btnConfirm.isEnabled = false
                btnConfirm.alpha = 0.5f
                btnConfirm.text = "Đang chờ admin duyệt..."
            }
            "approved" -> {
                val green = Color.parseColor("#22C55E")
                tvStatus.text = "Đã thanh toán"
                tvStatus.setTextColor(green)
                ivIcon.setColorFilter(green)
                cardUpload.visibility = View.GONE
                ivPreview?.visibility = View.GONE
                btnConfirm.visibility = View.GONE
            }
            "rejected" -> {
                val red = Color.parseColor("#E53935")
                tvStatus.text = "Bị từ chối – Gửi lại"
                tvStatus.setTextColor(red)
                ivIcon.setColorFilter(red)
                cardUpload.visibility = View.VISIBLE
                btnConfirm.isEnabled = true
                btnConfirm.alpha = 1f
                btnConfirm.visibility = View.VISIBLE
                btnConfirm.text = "Gửi lại minh chứng"
            }
            else -> { // "none"
                val red = Color.parseColor("#E53935")
                tvStatus.text = "Chưa thanh toán"
                tvStatus.setTextColor(red)
                ivIcon.setColorFilter(red)
                cardUpload.visibility = View.VISIBLE
                btnConfirm.isEnabled = true
                btnConfirm.alpha = 1f
                btnConfirm.visibility = View.VISIBLE
                btnConfirm.text = "Xác nhận đã chuyển khoản"
            }
        }
    }

    private fun renderPaymentHistory(history: List<InvoiceHistoryData>) {
        val container  = findViewById<LinearLayout>(R.id.llPaymentHistory) ?: return
        val tvNoHistory = findViewById<TextView>(R.id.tvNoHistory)
        container.removeAllViews()

        if (history.isEmpty()) {
            tvNoHistory?.visibility = View.VISIBLE
            return
        }
        tvNoHistory?.visibility = View.GONE

        val inflater = LayoutInflater.from(this)
        history.forEach { item ->
            val cardView = inflater.inflate(R.layout.item_payment_history, container, false)
            cardView.findViewById<TextView>(R.id.tvHistoryMonth)?.text =
                "Tháng ${item.month}/${item.year}"
            cardView.findViewById<TextView>(R.id.tvHistoryDate)?.text = item.paid_at ?: ""
            cardView.findViewById<TextView>(R.id.tvHistoryAmount)?.text = formatMoney(item.grand_total)
            container.addView(cardView)
        }
    }

    private fun showQrFullscreen() {
        val dialog = Dialog(this, android.R.style.Theme_Black_NoTitleBar_Fullscreen)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(R.layout.dialog_qr_fullscreen)
        dialog.window?.apply {
            setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.MATCH_PARENT)
            setBackgroundDrawableResource(android.R.color.transparent)
        }
        dialog.findViewById<ImageView>(R.id.btnCloseQr).setOnClickListener { dialog.dismiss() }
        dialog.setCanceledOnTouchOutside(true)
        dialog.show()
    }

    private fun formatMoney(amount: Long): String {
        return "%,d VNĐ".format(amount).replace(",", ".")
    }
}
