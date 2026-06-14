package com.example.quanlyphongtro

import android.content.Context
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.*

// ════════════════════════════════════════════════════════════════
//  AUTH DATA CLASSES
// ════════════════════════════════════════════════════════════════

data class LoginRequest(val username: String, val password: String)
// RegisterRequest is defined in RegisterActivity.kt

data class UserData(
    val id: Int, val username: String, val email: String,
    val phone: String?, val full_name: String?, val id_card: String?,
    val is_owner: Boolean
)

data class LoginResponse(val user: UserData, val access: String, val refresh: String)
data class RegisterResponse(val user: UserData, val access: String, val refresh: String)

data class UpdateProfileRequest(
    val full_name: String? = null,
    val email: String? = null,
    val phone: String? = null,
    val id_card: String? = null,
    val password: String? = null
)

// ════════════════════════════════════════════════════════════════
//  ROOM DATA CLASSES
// ════════════════════════════════════════════════════════════════

data class RoomData(
    val id: Int,
    val name: String,
    val price: String,
    val area: Double,
    val status: String,
    val floor: String?,
    val capacity: Int?,
    val amenities: String?,
    val description: String?,
    val tenant_name: String?,
    val tenant_phone: String?,
    val move_in: String?,
    val deposit: String?
)

data class LandlordData(val full_name: String?, val phone: String?, val email: String?)
data class BankInfoData(val bank_name: String?, val account_number: String?, val account_name: String?)

data class TenantDashboardResponse(
    val room: RoomData,
    val unpaid_total: String,
    val unpaid_count: Int,
    val landlord: LandlordData,
    val bank_info: BankInfoData
)

data class ContractData(
    val id: Int,
    val room_name: String,
    val move_in: String,
    val move_out: String?,
    val deposit: String,
    val duration_months: Int,
    val is_active: Boolean,
    val landlord_name: String?,
    val tenant_name: String?
)

// ════════════════════════════════════════════════════════════════
//  INVOICE DATA CLASSES
// ════════════════════════════════════════════════════════════════

data class InvoiceData(
    val id: Int,
    val room: Int,
    val month: Int,
    val year: Int,
    val room_price: String,
    val electricity_usage: Int,
    val water_usage: Int,
    val service_price: String,
    val electricity_price: String,
    val water_price: String,
    val total_electric: String,
    val total_water: String,
    val grand_total: String,
    val is_paid: Boolean,
    val created_at: String?,
    val updated_at: String?
)

data class InvoiceHistoryData(
    val id: Int,
    val month: Int,
    val year: Int,
    val grand_total: String,
    val is_paid: Boolean,
    val paid_at: String?
)

data class CurrentUnpaidResponse(
    val invoice: InvoiceData,
    val bank_info: BankInfoData,
    val payment_history: List<InvoiceHistoryData>
)

// ════════════════════════════════════════════════════════════════
//  NOTIFICATION DATA CLASSES
// ════════════════════════════════════════════════════════════════

data class NotificationData(
    val id: Int,
    val title: String,
    val body: String,
    val notif_type: String,
    val is_read: Boolean,
    val invoice_id: Int?,
    val created_at: String?,
    val time_display: String?
)

// ════════════════════════════════════════════════════════════════
//  API SERVICE INTERFACE
// ════════════════════════════════════════════════════════════════

interface ApiService {

    // ── Auth ──────────────────────────────────────────────────
    @POST("auth/login/")
    suspend fun login(@Body request: LoginRequest): LoginResponse

    @POST("auth/register/")
    suspend fun register(@Body request: RegisterRequest): RegisterResponse

    @GET("auth/profile/")
    suspend fun getProfile(@Header("Authorization") token: String): UserData

    @PATCH("auth/profile/")
    suspend fun updateProfile(
        @Header("Authorization") token: String,
        @Body body: UpdateProfileRequest
    ): UserData

    // ── Rooms ─────────────────────────────────────────────────
    @GET("api/rooms/available/")
    suspend fun getAvailableRooms(@Header("Authorization") token: String): List<RoomData>

    @GET("api/rooms/{id}/")
    suspend fun getRoomDetail(
        @Header("Authorization") token: String,
        @Path("id") id: Int
    ): RoomData

    @GET("api/rooms/tenant-room/")
    suspend fun getTenantDashboard(@Header("Authorization") token: String): TenantDashboardResponse

    @GET("api/rooms/tenant-contract/")
    suspend fun getTenantContract(@Header("Authorization") token: String): ContractData

    // ── Invoices ──────────────────────────────────────────────
    @GET("api/invoices/")
    suspend fun getInvoices(@Header("Authorization") token: String): List<InvoiceData>

    @GET("api/invoices/current-unpaid/")
    suspend fun getCurrentUnpaidInvoice(@Header("Authorization") token: String): CurrentUnpaidResponse

    // ── Notifications ─────────────────────────────────────────
    @GET("api/notifications/")
    suspend fun getNotifications(@Header("Authorization") token: String): List<NotificationData>

    @PATCH("api/notifications/{id}/read/")
    suspend fun markNotificationRead(
        @Header("Authorization") token: String,
        @Path("id") id: Int
    ): NotificationData

    @POST("api/notifications/read-all/")
    suspend fun markAllNotificationsRead(@Header("Authorization") token: String): Map<String, String>
}

// ════════════════════════════════════════════════════════════════
//  RETROFIT CLIENT
// ════════════════════════════════════════════════════════════════

object RetrofitClient {
    // Emulator: 10.0.2.2 = host localhost. Real device: dùng IP WiFi của máy host
    private const val BASE_URL = "http://10.0.2.2:8000/"
    // private const val BASE_URL = "http://192.168.88.132:8000/"

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(loggingInterceptor)
        .connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
        .readTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
        .build()

    val instance: ApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)
    }
}

// ════════════════════════════════════════════════════════════════
//  TOKEN MANAGER
// ════════════════════════════════════════════════════════════════

class TokenManager(context: Context) {
    private val prefs = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)

    fun saveToken(token: String) = prefs.edit().putString("access_token", token).apply()
    fun getToken(): String?       = prefs.getString("access_token", null)
    fun clearToken()              = prefs.edit().clear().apply()
    fun getBearer(): String       = "Bearer ${getToken()}"

    fun saveHasRoom(hasRoom: Boolean) = prefs.edit().putBoolean("has_room", hasRoom).apply()
    fun hasRoom(): Boolean            = prefs.getBoolean("has_room", false)

    fun saveUserId(id: Int)    = prefs.edit().putInt("user_id", id).apply()
    fun getUserId(): Int       = prefs.getInt("user_id", -1)
    fun saveUsername(u: String) = prefs.edit().putString("username", u).apply()
    fun getUsername(): String? = prefs.getString("username", null)
}

// ════════════════════════════════════════════════════════════════
//  HELPERS
// ════════════════════════════════════════════════════════════════

fun formatMoney(amount: String?): String {
    if (amount.isNullOrEmpty()) return "0đ"
    return try {
        val value = amount.toBigDecimal().toLong()
        "%,d".format(value).replace(',', '.') + "đ"
    } catch (e: Exception) {
        "${amount}đ"
    }
}
