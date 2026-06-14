package com.example.quanlyphongtro.network

// ── Request bodies ──────────────────────────────────────────────
data class LoginRequest(
    val username: String,
    val password: String
)

data class RegisterRequest(
    val username: String,
    val password: String,
    val email: String = "",
    val phone: String = ""
)

// ── Response bodies ─────────────────────────────────────────────
data class UserResponse(
    val id: Int,
    val username: String,
    val email: String,
    val phone: String?,
    val full_name: String?,
    val is_owner: Boolean
)

data class AuthResponse(
    val user: UserResponse,
    val access: String,
    val refresh: String
)

data class RoomResponse(
    val id: Int,
    val name: String,
    val price: String,
    val area: Double,
    val status: String,          // "available" | "occupied"
    val description: String,
    val created_at: String,
    val tenant_name: String?,
    val tenant_phone: String?,
    val move_in: String?,
    val deposit: String?,
    val duration_months: Int?,
    val room_image_url: String?
)

data class ErrorResponse(
    val error: String
)

data class RoomRequest(
    val name: String,
    val price: String,
    val area: Double,
    val status: String,
    val description: String
)

data class PendingPaymentData(
    val id: Int,
    val room_name: String,
    val tenant_name: String,
    val month: Int,
    val year: Int,
    val grand_total: String,
    val proof_url: String?,
    val payment_status: String,
    val updated_at: String?
)

data class MessageResponse(val message: String)

data class LogoutRequest(val refresh: String)

data class ContractData(
    val id: Int,
    val room_name: String,
    val move_in: String,
    val move_out: String?,
    val deposit: String,
    val duration_months: Int,
    val is_active: Boolean,
    val landlord_name: String?,
    val tenant_name: String?,
    val contract_image_url: String?
)
