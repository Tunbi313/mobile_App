package com.example.quanlyphongtro

import android.content.Intent
import android.os.Bundle
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Toast
import android.widget.Spinner
import android.widget.ArrayAdapter
import android.widget.AdapterView
import androidx.appcompat.app.AppCompatActivity
import org.json.JSONObject

class AddTenantActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_tenant)

        val btnBack = findViewById<ImageView>(R.id.btnBack)
        val btnCreateContract = findViewById<LinearLayout>(R.id.btnCreateContract)

        val spinnerTenantName = findViewById<Spinner>(R.id.spinnerTenantName)
        val etTenantPhone = findViewById<EditText>(R.id.etTenantPhone)
        val etTenantIdCard = findViewById<EditText>(R.id.etTenantIdCard)
        val etStartDate = findViewById<EditText>(R.id.etStartDate)
        val etDepositPrice = findViewById<EditText>(R.id.etDepositPrice)
        val etContractDuration = findViewById<EditText>(R.id.etContractDuration)

        val tenantList = ArrayList<JSONObject>()
        val tenantNames = ArrayList<String>()

        // Tải danh sách khách thuê đã đăng ký từ API
        ApiClient.get(this, "/auth/tenants/", object : ApiClient.ApiCallback {
            override fun onSuccess(response: String) {
                try {
                    val jsonArray = org.json.JSONArray(response)
                    tenantNames.clear()
                    tenantList.clear()

                    tenantNames.add("-- Chọn khách thuê đã đăng ký --")

                    for (i in 0 until jsonArray.length()) {
                        val obj = jsonArray.getJSONObject(i)
                        tenantList.add(obj)

                        val firstName = obj.optString("first_name", "").trim()
                        val username = obj.optString("username", "")
                        val displayName = if (firstName.isNotEmpty()) "$firstName ($username)" else username
                        tenantNames.add(displayName)
                    }

                    val adapter = ArrayAdapter(this@AddTenantActivity, android.R.layout.simple_spinner_item, tenantNames)
                    adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                    spinnerTenantName.adapter = adapter
                } catch (e: Exception) {
                    Toast.makeText(this@AddTenantActivity, "Lỗi nạp danh sách khách thuê", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onError(error: String) {
                Toast.makeText(this@AddTenantActivity, "Không thể kết nối danh sách khách thuê", Toast.LENGTH_SHORT).show()
            }
        })

        // Khi chọn một người thuê trong Spinner
        spinnerTenantName.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: android.view.View?, position: Int, id: Long) {
                if (position > 0) {
                    val selectedUser = tenantList[position - 1]
                    val phone  = if (selectedUser.isNull("phone"))   "" else selectedUser.optString("phone",   "")
                    val idCard = if (selectedUser.isNull("id_card")) "" else selectedUser.optString("id_card", "")

                    etTenantPhone.setText(phone)
                    etTenantIdCard.setText(idCard)
                } else {
                    etTenantPhone.setText("")
                    etTenantIdCard.setText("")
                }
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        val roomId = intent.getIntExtra("ROOM_ID", -1)
        if (roomId == -1) {
            Toast.makeText(this, "Lỗi: Không tìm thấy ID phòng!", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        // Date Picker cho ngày bắt đầu hợp đồng
        etStartDate.setOnClickListener {
            val calendar = java.util.Calendar.getInstance()
            val year = calendar.get(java.util.Calendar.YEAR)
            val month = calendar.get(java.util.Calendar.MONTH)
            val day = calendar.get(java.util.Calendar.DAY_OF_MONTH)

            val datePickerDialog = android.app.DatePickerDialog(
                this,
                { _, selectedYear, selectedMonth, selectedDay ->
                    val formattedDate = String.format(java.util.Locale.US, "%02d/%02d/%04d", selectedDay, selectedMonth + 1, selectedYear)
                    etStartDate.setText(formattedDate)
                },
                year,
                month,
                day
            )
            datePickerDialog.show()
        }

        // Quay lại
        btnBack.setOnClickListener {
            finish()
        }

        // Tạo hợp đồng -> Gửi API gán người thuê
        btnCreateContract.setOnClickListener {
            val selectedPosition = spinnerTenantName.selectedItemPosition
            if (selectedPosition <= 0) {
                Toast.makeText(this, "Vui lòng chọn khách thuê", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val selectedUser = tenantList[selectedPosition - 1]
            val firstName = selectedUser.optString("first_name", "").trim()
            val username = selectedUser.optString("username", "")
            val name = if (firstName.isNotEmpty()) firstName else username

            val phone = etTenantPhone.text.toString().trim()
            val idCard = etTenantIdCard.text.toString().trim()
            val startDate = etStartDate.text.toString().trim()
            val depositText = etDepositPrice.text.toString().replace(".", "").replace(",", "").trim()
            val durationText = etContractDuration.text.toString().replace(Regex("[^0-9]"), "").trim()

            if (name.isEmpty() || phone.isEmpty() || idCard.isEmpty() || startDate.isEmpty()) {
                Toast.makeText(this, "Vui lòng nhập đầy đủ thông tin bắt buộc", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Chuyển đổi định dạng ngày: DD/MM/YYYY -> YYYY-MM-DD để tương thích với Django DateField
            val dateParts = startDate.split("/")
            val formattedStartDate = if (dateParts.size == 3) {
                "${dateParts[2]}-${dateParts[1]}-${dateParts[0]}"
            } else {
                startDate
            }

            val jsonBody = JSONObject()
            jsonBody.put("name", name)
            jsonBody.put("phone", phone)
            jsonBody.put("id_card", idCard)
            jsonBody.put("move_in", formattedStartDate)
            jsonBody.put("deposit", depositText.toLongOrNull() ?: 0L)
            jsonBody.put("duration_months", durationText.toIntOrNull() ?: 12)

            ApiClient.post(this, "/api/rooms/$roomId/assign/", jsonBody, object : ApiClient.ApiCallback {
                override fun onSuccess(response: String) {
                    try {
                        val json = JSONObject(response)
                        Toast.makeText(this@AddTenantActivity, "Tạo hợp đồng thành công!", Toast.LENGTH_SHORT).show()
                        
                        // Chuyển sang màn hình hiển thị Hợp đồng
                        val intent = Intent(this@AddTenantActivity, ContractActivity::class.java)
                        intent.putExtra("sign_date", startDate)
                        intent.putExtra("duration", "$durationText tháng")
                        intent.putExtra("landlord_name", "Chủ nhà trọ")
                        intent.putExtra("tenant_name", name)
                        startActivity(intent)
                        finish()
                    } catch (e: Exception) {
                        Toast.makeText(this@AddTenantActivity, "Lỗi phân tích kết quả gán phòng", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onError(error: String) {
                    try {
                        val errorJson = JSONObject(error)
                        val message = errorJson.optString("error", "Lỗi tạo hợp đồng")
                        Toast.makeText(this@AddTenantActivity, message, Toast.LENGTH_SHORT).show()
                    } catch (e: Exception) {
                        Toast.makeText(this@AddTenantActivity, "Không thể kết nối API tạo hợp đồng", Toast.LENGTH_SHORT).show()
                    }
                }
            })
        }
    }
}
