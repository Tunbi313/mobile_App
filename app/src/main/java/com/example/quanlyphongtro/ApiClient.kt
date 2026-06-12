package com.example.quanlyphongtro

import android.content.Context
import android.os.Handler
import android.os.Looper
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.Executors

object ApiClient {
    private val BASE_URL = "http://${com.example.quanlyphongtro.BuildConfig.BACKEND_IP}:8000"
    private val executor = Executors.newSingleThreadExecutor()
    private val mainHandler = Handler(Looper.getMainLooper())

    interface ApiCallback {
        fun onSuccess(response: String)
        fun onError(error: String)
    }

    fun get(context: Context, endpoint: String, callback: ApiCallback) {
        executor.execute {
            var connection: HttpURLConnection? = null
            try {
                val session = com.example.quanlyphongtro.network.SessionManager(context)
                val token = session.getAccessToken()
                val url = URL("$BASE_URL$endpoint")
                connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                connection.setRequestProperty("Content-Type", "application/json")
                if (token != null) {
                    connection.setRequestProperty("Authorization", "Bearer $token")
                }
                connection.connectTimeout = 10000
                connection.readTimeout = 10000

                val responseCode = connection.responseCode
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    val reader = BufferedReader(InputStreamReader(connection.inputStream))
                    val response = StringBuilder()
                    var line: String?
                    while (reader.readLine().also { line = it } != null) {
                        response.append(line)
                    }
                    reader.close()
                    mainHandler.post { callback.onSuccess(response.toString()) }
                } else {
                    val responseStream = connection.errorStream ?: connection.inputStream
                    val reader = BufferedReader(InputStreamReader(responseStream))
                    val errorResponse = StringBuilder()
                    var line: String?
                    while (reader.readLine().also { line = it } != null) {
                        errorResponse.append(line)
                    }
                    reader.close()
                    mainHandler.post { callback.onError(errorResponse.toString()) }
                }
            } catch (e: Exception) {
                mainHandler.post { callback.onError(e.message ?: "Unknown error") }
            } finally {
                connection?.disconnect()
            }
        }
    }

    fun post(context: Context, endpoint: String, jsonBody: JSONObject, callback: ApiCallback) {
        executor.execute {
            var connection: HttpURLConnection? = null
            try {
                val session = com.example.quanlyphongtro.network.SessionManager(context)
                val token = session.getAccessToken()
                val url = URL("$BASE_URL$endpoint")
                connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "POST"
                connection.setRequestProperty("Content-Type", "application/json")
                if (token != null) {
                    connection.setRequestProperty("Authorization", "Bearer $token")
                }
                connection.doOutput = true
                connection.connectTimeout = 10000
                connection.readTimeout = 10000

                val writer = OutputStreamWriter(connection.outputStream)
                writer.write(jsonBody.toString())
                writer.flush()
                writer.close()

                val responseCode = connection.responseCode
                if (responseCode == HttpURLConnection.HTTP_OK || responseCode == HttpURLConnection.HTTP_CREATED) {
                    val reader = BufferedReader(InputStreamReader(connection.inputStream))
                    val response = StringBuilder()
                    var line: String?
                    while (reader.readLine().also { line = it } != null) {
                        response.append(line)
                    }
                    reader.close()
                    mainHandler.post { callback.onSuccess(response.toString()) }
                } else {
                    val errorStream = connection.errorStream
                    val responseStream = errorStream ?: connection.inputStream
                    val reader = BufferedReader(InputStreamReader(responseStream))
                    val errorResponse = StringBuilder()
                    var line: String?
                    while (reader.readLine().also { line = it } != null) {
                        errorResponse.append(line)
                    }
                    reader.close()
                    mainHandler.post { callback.onError(errorResponse.toString()) }
                }
            } catch (e: Exception) {
                mainHandler.post { callback.onError(e.message ?: "Unknown error") }
            } finally {
                connection?.disconnect()
            }
        }
    }
}
