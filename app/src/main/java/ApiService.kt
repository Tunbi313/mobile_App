package com.example.quanlyphongtro

import android.content.Context
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.*

// ===== DATA CLASSES =====
data class LoginRequest(
    val username: String,
    val password: String
)

// Khớp với response Django trả về:
// { "user": {...}, "access": "...", "refresh": "..." }
data class UserData(
    val id: Int,
    val username: String,
    val email: String
)

data class LoginResponse(
    val user: UserData,
    val access: String,
    val refresh: String
)


data class RegisterResponse(
    val user: UserData,
    val access: String,
    val refresh: String
)

// ===== API SERVICE =====
interface ApiService {

    // URL khớp với Django: path('api/auth/', include('users.urls'))
    @POST("auth/login/")
    suspend fun login(@Body request: LoginRequest): LoginResponse

    @POST("auth/register/")
    suspend fun register(@Body request: RegisterRequest): RegisterResponse
}

// ===== RETROFIT CLIENT =====
object RetrofitClient {

    // Dùng máy thật: IP WiFi máy Ubuntu
    private const val BASE_URL = "http://192.168.88.132:8000/"

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

// ===== TOKEN MANAGER =====
class TokenManager(context: Context) {
    private val prefs = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)

    fun saveToken(token: String) {
        prefs.edit().putString("access_token", token).apply()
    }

    fun getToken(): String? {
        return prefs.getString("access_token", null)
    }

    fun clearToken() {
        prefs.edit().clear().apply()
    }

    fun getBearer(): String {
        return "Bearer ${getToken()}"
    }
}