package com.android.simkanti.ui.history

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

// Data class to represent a single history transaction
data class HistoryTransaction(
    val id: Int,
    val nim: String,
    val totalHarga: String,
    val idBarang: String,
    val namaBarang: String,
    val kategori: String,
    val date: String,
    val time: String? = null
)

// Data class untuk batasan harian
data class DailyLimit(
    val limitAmount: Double,
    val spentToday: Double,
    val remaining: Double
)

// Response data class
data class ApiResponse(
    val success: Boolean,
    val message: String
)

class HistoryViewModel : ViewModel() {
    private val baseUrl = "http://192.168.0.56/smart_kantin/api_android"

    private val _historyList = MutableLiveData<List<HistoryTransaction>>()
    val historyList: LiveData<List<HistoryTransaction>> = _historyList

    private val _errorMessage = MutableLiveData<String>()
    val errorMessage: LiveData<String> = _errorMessage
    
    private val _dailyLimit = MutableLiveData<DailyLimit>()
    val dailyLimit: LiveData<DailyLimit> = _dailyLimit
    
    private val _limitUpdateResult = MutableLiveData<ApiResponse>()
    val limitUpdateResult: LiveData<ApiResponse> = _limitUpdateResult

    fun fetchHistoryForNIM(nim: String) {
        viewModelScope.launch {
            try {
                val result = fetchHistoryData(nim)
                _historyList.value = result
            } catch (e: Exception) {
                _errorMessage.value = "Error fetching history: ${e.message}"
                Log.e("HistoryViewModel", "Error fetching history", e)
            }
        }
    }
    
    fun fetchDailyLimit(nim: String) {
        viewModelScope.launch {
            try {
                val result = fetchDailyLimitData(nim)
                _dailyLimit.value = result
            } catch (e: Exception) {
                _errorMessage.value = "Error fetching daily limit: ${e.message}"
                Log.e("HistoryViewModel", "Error fetching daily limit", e)
            }
        }
    }
    
    fun setDailyLimit(nim: String, limitAmount: Double) {
        viewModelScope.launch {
            try {
                val result = setDailyLimitData(nim, limitAmount)
                _limitUpdateResult.value = result
                // Re-fetch daily limit to update UI
                fetchDailyLimit(nim)
            } catch (e: Exception) {
                _errorMessage.value = "Error setting daily limit: ${e.message}"
                Log.e("HistoryViewModel", "Error setting daily limit", e)
            }
        }
    }

    private suspend fun fetchHistoryData(nim: String): List<HistoryTransaction> =
        withContext(Dispatchers.IO) {
            val url = URL("$baseUrl/get_history.php?nim=$nim")
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.connectTimeout = 5000
            connection.readTimeout = 5000

            try {
                val responseCode = connection.responseCode
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    val reader = BufferedReader(InputStreamReader(connection.inputStream))
                    val response = reader.readText()
                    reader.close()

                    parseHistoryResponse(response)
                } else {
                    throw Exception("Server error: $responseCode")
                }
            } finally {
                connection.disconnect()
            }
        }
        
    private suspend fun fetchDailyLimitData(nim: String): DailyLimit =
        withContext(Dispatchers.IO) {
            val url = URL("$baseUrl/get_daily_limit.php?nim=$nim")
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.connectTimeout = 5000
            connection.readTimeout = 5000

            try {
                val responseCode = connection.responseCode
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    val reader = BufferedReader(InputStreamReader(connection.inputStream))
                    val response = reader.readText()
                    reader.close()

                    val jsonObject = JSONObject(response)
                    if (jsonObject.getBoolean("success")) {
                        val data = jsonObject.getJSONObject("data")
                        DailyLimit(
                            limitAmount = data.getDouble("limit_amount"),
                            spentToday = data.getDouble("spent_today"),
                            remaining = data.getDouble("remaining")
                        )
                    } else {
                        DailyLimit(0.0, 0.0, 0.0)
                    }
                } else {
                    throw Exception("Server error: $responseCode")
                }
            } finally {
                connection.disconnect()
            }
        }
        
    private suspend fun setDailyLimitData(nim: String, limitAmount: Double): ApiResponse =
        withContext(Dispatchers.IO) {
            val url = URL("$baseUrl/set_daily_limit.php")
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "POST"
            connection.doOutput = true
            connection.setRequestProperty("Content-Type", "application/json")
            connection.connectTimeout = 5000
            connection.readTimeout = 5000

            val jsonData = JSONObject()
            jsonData.put("nim", nim)
            jsonData.put("limit_amount", limitAmount)

            try {
                val outputStreamWriter = OutputStreamWriter(connection.outputStream)
                outputStreamWriter.write(jsonData.toString())
                outputStreamWriter.flush()

                val responseCode = connection.responseCode
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    val reader = BufferedReader(InputStreamReader(connection.inputStream))
                    val response = reader.readText()
                    reader.close()

                    val jsonObject = JSONObject(response)
                    ApiResponse(
                        success = jsonObject.getBoolean("success"),
                        message = jsonObject.getString("message")
                    )
                } else {
                    throw Exception("Server error: $responseCode")
                }
            } finally {
                connection.disconnect()
            }
        }

    private fun parseHistoryResponse(jsonResponse: String): List<HistoryTransaction> {
        val transactions = mutableListOf<HistoryTransaction>()

        try {
            val jsonObject = JSONObject(jsonResponse)
            val success = jsonObject.getBoolean("success")

            if (success) {
                val dataArray = jsonObject.getJSONArray("data")

                for (i in 0 until dataArray.length()) {
                    val item = dataArray.getJSONObject(i)
                    transactions.add(
                        HistoryTransaction(
                            id = item.getInt("id_h"),
                            nim = item.getString("nim"),
                            totalHarga = item.getString("totalharga"),
                            idBarang = item.optString("id_barang", "-"),
                            namaBarang = item.optString("nama_barang", "Produk tidak diketahui"),
                            kategori = item.optString("nama_kategori", "Umum"),
                            date = item.getString("date"),
                            time = item.optString("time", null)
                        )
                    )
                }
            } else {
                val message = jsonObject.getString("message")
                throw Exception(message)
            }
        } catch (e: Exception) {
            Log.e("HistoryViewModel", "Parsing error", e)
            throw e
        }

        return transactions
    }
}