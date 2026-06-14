package com.example.quanlyphongtro

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import com.bumptech.glide.Glide
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody

class SettingsActivity : AppCompatActivity() {

    private lateinit var tokenManager: TokenManager
    private lateinit var ivAvatar:    ImageView
    private lateinit var ivTopAvatar: ImageView

    private val pickAvatarLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) uploadAvatar(uri)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        tokenManager = TokenManager(this)

        val tvTopBarName      = findViewById<TextView>(R.id.tvTopBarName)
        val tvUserName        = findViewById<TextView>(R.id.tvUserName)
        val etFullName        = findViewById<EditText>(R.id.etFullName)
        val etPhone           = findViewById<EditText>(R.id.etPhone)
        val etEmail           = findViewById<EditText>(R.id.etEmail)
        val etCCCD            = findViewById<EditText>(R.id.etCCCD)
        val btnChangePassword = findViewById<Button>(R.id.btnChangePassword)
        val btnSave           = findViewById<Button>(R.id.btnSave)
        val btnLogout         = findViewById<TextView>(R.id.btnLogout)
        val btnChangeAvatar   = findViewById<CardView>(R.id.btnChangeAvatar)
        val navHome           = findViewById<LinearLayout>(R.id.navHome)
        val navManage         = findViewById<LinearLayout>(R.id.navManage)
        val navNotify         = findViewById<LinearLayout>(R.id.navNotify)
        val navSettings       = findViewById<LinearLayout>(R.id.navSettings)

        ivAvatar    = findViewById(R.id.ivAvatar)


        btnChangeAvatar.setOnClickListener {
            pickAvatarLauncher.launch("image/*")
        }

        loadProfile(tvTopBarName, tvUserName, etFullName, etPhone, etEmail, etCCCD)

        btnChangePassword.setOnClickListener {
            Toast.makeText(this, "Tính năng đổi mật khẩu đang phát triển", Toast.LENGTH_SHORT).show()
        }

        btnSave.setOnClickListener {
            val fullName = etFullName.text.toString().trim()
            val email    = etEmail.text.toString().trim()
            val cccd     = etCCCD.text.toString().trim()

            if (fullName.isEmpty()) {
                etFullName.error = "Vui lòng nhập họ tên"
                return@setOnClickListener
            }

            btnSave.isEnabled = false
            btnSave.text = "Đang lưu..."

            CoroutineScope(Dispatchers.IO).launch {
                try {
                    RetrofitClient.instance.updateProfile(
                        tokenManager.getBearer(),
                        UpdateProfileRequest(full_name = fullName, email = email, id_card = cccd)
                    )
                    withContext(Dispatchers.Main) {
                        btnSave.isEnabled = true
                        btnSave.text = "Lưu thay đổi"
                        tvTopBarName.text = fullName
                        tvUserName.text   = fullName
                        Toast.makeText(this@SettingsActivity,
                            "Cập nhật thông tin thành công!", Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        btnSave.isEnabled = true
                        btnSave.text = "Lưu thay đổi"
                        Toast.makeText(this@SettingsActivity,
                            "Lỗi cập nhật: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }

        btnLogout.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("Đăng xuất")
                .setMessage("Bạn có chắc muốn đăng xuất không?")
                .setPositiveButton("Đăng xuất") { _, _ -> doLogout() }
                .setNegativeButton("Huỷ", null)
                .show()
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
        navNotify.setOnClickListener {
            startActivity(Intent(this, NotificationActivity::class.java)); finish()
        }
        navSettings.setOnClickListener { /* Already here */ }
    }

    private fun doLogout() {
        val refreshToken = tokenManager.getRefreshToken()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                if (refreshToken != null) {
                    RetrofitClient.instance.logout(
                        tokenManager.getBearer(),
                        LogoutRequest(refreshToken)
                    )
                }
            } catch (_: Exception) { }
            withContext(Dispatchers.Main) {
                tokenManager.clearToken()
                val intent = Intent(this@SettingsActivity, LoginActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
            }
        }
    }

    private fun loadAvatar(url: String?) {
        if (!url.isNullOrEmpty()) {
            Glide.with(this).load(url).circleCrop()
                .placeholder(R.drawable.ic_user).error(R.drawable.ic_user).into(ivAvatar)
            Glide.with(this).load(url).circleCrop()
                .placeholder(R.drawable.ic_user).error(R.drawable.ic_user).into(ivTopAvatar)
        }
    }

    private fun uploadAvatar(uri: Uri) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val bytes    = contentResolver.openInputStream(uri)?.readBytes() ?: return@launch
                val mimeType = contentResolver.getType(uri) ?: "image/jpeg"
                val reqBody  = bytes.toRequestBody(mimeType.toMediaTypeOrNull())
                val part     = MultipartBody.Part.createFormData("avatar", "avatar.jpg", reqBody)
                val profile  = RetrofitClient.instance.uploadAvatar(tokenManager.getBearer(), part)
                withContext(Dispatchers.Main) {
                    loadAvatar(profile.avatar_url)
                    Toast.makeText(this@SettingsActivity, "Cập nhật ảnh đại diện thành công!", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@SettingsActivity, "Lỗi tải ảnh: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun loadProfile(
        tvTopBarName: TextView, tvUserName: TextView,
        etName: EditText, etPhone: EditText, etEmail: EditText, etCCCD: EditText
    ) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val profile = RetrofitClient.instance.getProfile(tokenManager.getBearer())
                withContext(Dispatchers.Main) {
                    val displayName = profile.full_name ?: profile.username
                    tvTopBarName.text = displayName
                    tvUserName.text   = displayName
                    etName.setText(profile.full_name?.takeIf { it != "null" } ?: "")
                    etPhone.setText(profile.phone?.takeIf { it != "null" } ?: "")
                    etEmail.setText(profile.email)
                    etCCCD.setText(profile.id_card?.takeIf { it != "null" } ?: "")
                    loadAvatar(profile.avatar_url)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@SettingsActivity,
                        "Không thể tải hồ sơ", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}
