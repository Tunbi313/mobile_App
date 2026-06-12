package com.example.quanlyphongtro

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import org.json.JSONArray
import org.json.JSONObject
import java.util.Calendar

class BillActivity : AppCompatActivity() {

    private lateinit var edtRoomPrice: EditText
    private lateinit var edtElectricUsage: EditText
    private lateinit var txtElectricTotal: TextView
    private lateinit var edtWaterUsage: EditText
    private lateinit var txtWaterTotal: TextView
    private lateinit var edtServicePrice: EditText
    private lateinit var txtGrandTotal: TextView
    private lateinit var spinnerRoom: Spinner

    // Giá trị đơn giá mặc định
    private var electricUnitPrice = 3500L
    private var waterUnitPrice = 15000L

    // Lưu trữ thông tin phòng tải từ API: Tên Phòng -> (ID Phòng, Giá phòng)
    private val roomMap = HashMap<String, Pair<Int, Long>>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_bill)

        // Ánh xạ views
        val btnBack = findViewById<ImageView>(R.id.btnBack)
        val spinnerMonth = findViewById<Spinner>(R.id.spinnerMonth)
        spinnerRoom = findViewById(R.id.spinnerRoom)
        edtRoomPrice = findViewById(R.id.edtRoomPrice)
        edtElectricUsage = findViewById(R.id.edtElectricUsage)
        txtElectricTotal = findViewById(R.id.txtElectricTotal)
        edtWaterUsage = findViewById(R.id.edtWaterUsage)
        txtWaterTotal = findViewById(R.id.txtWaterTotal)
        edtServicePrice = findViewById(R.id.edtServicePrice)
        txtGrandTotal = findViewById(R.id.txtGrandTotal)
        val btnSubmit = findViewById<Button>(R.id.btnSubmit)

        // Quay lại
        btnBack.setOnClickListener {
            finish()
        }

        // Cài đặt dữ liệu Spinner Tháng
        val months = arrayOf(
            "Tháng 1", "Tháng 2", "Tháng 3", "Tháng 4", "Tháng 5", "Tháng 6",
            "Tháng 7", "Tháng 8", "Tháng 9", "Tháng 10", "Tháng 11", "Tháng 12"
        )
        val monthAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, months)
        monthAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerMonth.adapter = monthAdapter
        
        // Chọn tháng hiện tại làm mặc định
        val currentMonth = Calendar.getInstance().get(Calendar.MONTH)
        spinnerMonth.setSelection(currentMonth)

        // Tải đơn giá của chủ trọ trước để dùng cho tính toán
        loadUnitPrices()

        // Tải danh sách phòng từ API của chủ trọ
        loadRooms()

        // Lắng nghe thay đổi giá trị để tính hóa đơn động
        val textWatcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                calculateBill()
            }
        }

        edtRoomPrice.addTextChangedListener(textWatcher)
        edtElectricUsage.addTextChangedListener(textWatcher)
        edtWaterUsage.addTextChangedListener(textWatcher)
        edtServicePrice.addTextChangedListener(textWatcher)

        // Khi chọn phòng khác nhau, tự động điền giá của phòng đó
        spinnerRoom.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: android.view.View?, position: Int, id: Long) {
                val selectedRoomName = spinnerRoom.selectedItem?.toString() ?: ""
                val roomInfo = roomMap[selectedRoomName]
                if (roomInfo != null) {
                    edtRoomPrice.setText(roomInfo.second.toString())
                }
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        // Sự kiện gửi hóa đơn -> Gửi API lưu hóa đơn
        btnSubmit.setOnClickListener {
            val selectedRoomName = spinnerRoom.selectedItem?.toString() ?: ""
            val roomInfo = roomMap[selectedRoomName]
            if (roomInfo == null) {
                Toast.makeText(this, "Vui lòng chọn phòng hợp lệ", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val roomId = roomInfo.first
            val month = spinnerMonth.selectedItemPosition + 1
            val year = Calendar.getInstance().get(Calendar.YEAR)

            val roomPrice = edtRoomPrice.text.toString().replace(".", "").replace(",", "").toLongOrNull() ?: 0L
            val electricityUsage = edtElectricUsage.text.toString().toIntOrNull() ?: 0
            val waterUsage = edtWaterUsage.text.toString().toIntOrNull() ?: 0
            val servicePrice = edtServicePrice.text.toString().replace(".", "").replace(",", "").toLongOrNull() ?: 0L

            val jsonBody = JSONObject()
            jsonBody.put("room", roomId)
            jsonBody.put("month", month)
            jsonBody.put("year", year)
            jsonBody.put("room_price", roomPrice)
            jsonBody.put("electricity_usage", electricityUsage)
            jsonBody.put("water_usage", waterUsage)
            jsonBody.put("service_price", servicePrice)

            ApiClient.post(this, "/api/invoices/", jsonBody, object : ApiClient.ApiCallback {
                override fun onSuccess(response: String) {
                    Toast.makeText(this@BillActivity, "Hóa đơn đã được lưu và gửi thành công!", Toast.LENGTH_SHORT).show()
                    finish()
                }

                override fun onError(error: String) {
                    try {
                        val errorJson = JSONObject(error)
                        val message = errorJson.optString("error", "Lỗi gửi hóa đơn")
                        Toast.makeText(this@BillActivity, message, Toast.LENGTH_SHORT).show()
                    } catch (e: Exception) {
                        Toast.makeText(this@BillActivity, "Lỗi kết nối máy chủ gửi hóa đơn", Toast.LENGTH_SHORT).show()
                    }
                }
            })
        }
    }

    private fun loadUnitPrices() {
        ApiClient.get(this, "/api/invoices/unit-price/", object : ApiClient.ApiCallback {
            override fun onSuccess(response: String) {
                try {
                    val json = JSONObject(response)
                    electricUnitPrice = json.optLong("electricity_price", 3500L)
                    waterUnitPrice = json.optLong("water_price", 15000L)
                    val commonFee = json.optLong("common_service_fee", 100000L)
                    edtServicePrice.setText(commonFee.toString())
                    
                    // Tính lại hóa đơn với đơn giá mới
                    calculateBill()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            override fun onError(error: String) {
                // Sử dụng đơn giá mặc định
            }
        })
    }

    private fun loadRooms() {
        ApiClient.get(this, "/api/rooms/", object : ApiClient.ApiCallback {
            override fun onSuccess(response: String) {
                try {
                    val jsonArray = JSONArray(response)
                    val roomNames = ArrayList<String>()
                    
                    for (i in 0 until jsonArray.length()) {
                        val obj = jsonArray.getJSONObject(i)
                        val id = obj.getInt("id")
                        val name = obj.getString("name")
                        val price = obj.getString("price").toDoubleOrNull()?.toLong() ?: 3000000L
                        
                        roomMap[name] = Pair(id, price)
                        roomNames.add(name)
                    }

                    val roomAdapter = ArrayAdapter(this@BillActivity, android.R.layout.simple_spinner_item, roomNames)
                    roomAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                    spinnerRoom.adapter = roomAdapter
                } catch (e: Exception) {
                    Toast.makeText(this@BillActivity, "Lỗi nạp danh sách phòng", Toast.LENGTH_SHORT).show()
                    fallbackRooms()
                }
            }

            override fun onError(error: String) {
                fallbackRooms()
            }
        })
    }

    private fun fallbackRooms() {
        // Dự phòng nếu API phòng lỗi
        val fallbackNames = arrayOf("Phòng 101", "Phòng 102", "Phòng 103", "Phòng 201", "Phòng 202")
        for ((index, name) in fallbackNames.withIndex()) {
            roomMap[name] = Pair(index + 1, 3000000L)
        }
        val roomAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, fallbackNames)
        roomAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerRoom.adapter = roomAdapter
    }

    private fun calculateBill() {
        val roomPriceText = edtRoomPrice.text.toString().replace(".", "").replace(",", "")
        val servicePriceText = edtServicePrice.text.toString().replace(".", "").replace(",", "")

        val roomPrice = roomPriceText.toLongOrNull() ?: 0L
        val electricityUsage = edtElectricUsage.text.toString().toLongOrNull() ?: 0L
        val waterUsage = edtWaterUsage.text.toString().toLongOrNull() ?: 0L
        val servicePrice = servicePriceText.toLongOrNull() ?: 0L

        // Tính tiền điện
        val electricTotal = electricityUsage * electricUnitPrice
        txtElectricTotal.text = String.format("Thành tiền điện: %,d VND", electricTotal)

        // Tính tiền nước
        val waterTotal = waterUsage * waterUnitPrice
        txtWaterTotal.text = String.format("Thành tiền nước: %,d VND", waterTotal)

        // Tính tổng cộng
        val grandTotal = roomPrice + electricTotal + waterTotal + servicePrice
        txtGrandTotal.text = String.format("%,d VNĐ", grandTotal)
    }
}
