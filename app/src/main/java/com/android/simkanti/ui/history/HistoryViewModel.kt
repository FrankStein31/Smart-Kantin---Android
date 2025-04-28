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
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

// Data class to represent a single history transaction
//data class HistoryTransaction(
//    val id: Int,
//    val nim: String,
//    val totalHarga: String,
//    val idBarang: String,
//    val namaBarang: String,
//    val kategori: String,
//    val date: String,
//    val time: String? = null
//)

// Data class untuk batasan harian
data class DailyLimit(
    val limitAmount: Double,
    val spentToday: Double,
    val remaining: Double
)

// Data class untuk total pengeluaran harian
data class DailyTotal(
    val date: String,
    val total: Double,
    val formattedDate: String
)

// Data class untuk total pengeluaran bulanan
data class MonthlyTotal(
    val yearMonth: String,
    val total: Double,
    val formattedMonth: String
)

// Response data class
data class ApiResponse(
    val success: Boolean,
    val message: String
)

class HistoryViewModel : ViewModel() {
    private val baseUrl = "http://192.168.1.155/smart_kantin/api_android"

    private val _historyList = MutableLiveData<List<HistoryTransaction>>()
    val historyList: LiveData<List<HistoryTransaction>> = _historyList

    private val _errorMessage = MutableLiveData<String>()
    val errorMessage: LiveData<String> = _errorMessage
    
    private val _dailyLimit = MutableLiveData<DailyLimit>()
    val dailyLimit: LiveData<DailyLimit> = _dailyLimit
    
    private val _limitUpdateResult = MutableLiveData<ApiResponse>()
    val limitUpdateResult: LiveData<ApiResponse> = _limitUpdateResult
    
    private val _dailyTotals = MutableLiveData<List<DailyTotal>>()
    val dailyTotals: LiveData<List<DailyTotal>> = _dailyTotals
    
    private val _monthlyTotals = MutableLiveData<List<MonthlyTotal>>()
    val monthlyTotals: LiveData<List<MonthlyTotal>> = _monthlyTotals
    
    private val _filterTotal = MutableLiveData<Double>()
    val filterTotal: LiveData<Double> = _filterTotal
    
    // Filter dates
    private var startDate: String? = null
    private var endDate: String? = null

    fun fetchHistoryForNIM(nim: String) {
        viewModelScope.launch {
            try {
                val result = fetchHistoryData(nim, startDate, endDate)
                _historyList.value = result.transactions
                _dailyTotals.value = result.dailyTotals
                _monthlyTotals.value = result.monthlyTotals
                _filterTotal.value = result.filterTotal
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
    
    fun setDateFilter(startDate: String?, endDate: String?) {
        this.startDate = startDate
        this.endDate = endDate
    }
    
    fun clearDateFilter() {
        startDate = null
        endDate = null
    }

    private suspend fun fetchHistoryData(nim: String, startDate: String?, endDate: String?): HistoryDataResult =
        withContext(Dispatchers.IO) {
            val urlBuilder = StringBuilder("$baseUrl/get_history.php?nim=$nim")
            
            if (!startDate.isNullOrEmpty()) {
                urlBuilder.append("&start_date=$startDate")
            }
            
            if (!endDate.isNullOrEmpty()) {
                urlBuilder.append("&end_date=$endDate")
            }
            
            val url = URL(urlBuilder.toString())
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

                    parseHistoryResponseWithTotals(response)
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
    
    private data class HistoryDataResult(
        val transactions: List<HistoryTransaction>,
        val dailyTotals: List<DailyTotal>,
        val monthlyTotals: List<MonthlyTotal>,
        val filterTotal: Double
    )

    private fun parseHistoryResponseWithTotals(jsonResponse: String): HistoryDataResult {
        val transactions = mutableListOf<HistoryTransaction>()
        val dailyTotals = mutableListOf<DailyTotal>()
        val monthlyTotals = mutableListOf<MonthlyTotal>()
        var filterTotal = 0.0

        try {
            val jsonObject = JSONObject(jsonResponse)
            val success = jsonObject.getBoolean("success")

            if (success) {
                // Parse transactions
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
                
                // Parse daily totals
                if (jsonObject.has("daily_totals")) {
                    val dailyArray = jsonObject.getJSONArray("daily_totals")
                    val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                    val displayFormat = SimpleDateFormat("dd MMM", Locale.getDefault())
                    
                    for (i in 0 until dailyArray.length()) {
                        val item = dailyArray.getJSONObject(i)
                        val date = item.getString("date")
                        val total = item.getDouble("total")
                        
                        // Format the date for display
                        val parsedDate = dateFormat.parse(date)
                        val formattedDate = if (parsedDate != null) {
                            displayFormat.format(parsedDate)
                        } else {
                            date
                        }
                        
                        dailyTotals.add(
                            DailyTotal(
                                date = date,
                                total = total,
                                formattedDate = formattedDate
                            )
                        )
                    }
                }
                
                // Parse monthly totals
                if (jsonObject.has("monthly_totals")) {
                    val monthlyArray = jsonObject.getJSONArray("monthly_totals")
                    val monthFormat = SimpleDateFormat("MMM yyyy", Locale.getDefault())
                    
                    for (i in 0 until monthlyArray.length()) {
                        val item = monthlyArray.getJSONObject(i)
                        val yearMonth = item.getString("month_year")
                        val total = item.getDouble("total")
                        
                        // Create a date from year and month
                        val year = item.getInt("year")
                        val month = item.getInt("month") - 1 // Calendar months are 0-based
                        
                        val calendar = Calendar.getInstance()
                        calendar.set(Calendar.YEAR, year)
                        calendar.set(Calendar.MONTH, month)
                        calendar.set(Calendar.DAY_OF_MONTH, 1)
                        
                        val formattedMonth = monthFormat.format(calendar.time)
                        
                        monthlyTotals.add(
                            MonthlyTotal(
                                yearMonth = yearMonth,
                                total = total,
                                formattedMonth = formattedMonth
                            )
                        )
                    }
                }
                
                // Get filter total
                if (jsonObject.has("filter_total")) {
                    filterTotal = jsonObject.getDouble("filter_total")
                }
            } else {
                val message = jsonObject.getString("message")
                throw Exception(message)
            }
        } catch (e: Exception) {
            Log.e("HistoryViewModel", "Parsing error", e)
            throw e
        }

        return HistoryDataResult(
            transactions = transactions,
            dailyTotals = dailyTotals,
            monthlyTotals = monthlyTotals,
            filterTotal = filterTotal
        )
    }
}