package com.android.simkanti.ui.product

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import org.json.JSONArray
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import android.util.Log

data class Product(
    val id: String,
    val idBarang: String,
    val idKategori: String,
    val namaBarang: String,
    val merk: String,
    val hargaBeli: String,
    val hargaJual: String,
    val satuanBarang: String,
    val stok: String,
    val expired: String,
    val tglInput: String,
    val tglUpdate: String?,
    val namaKategori: String?
)

data class ProductResult(
    val success: Boolean,
    val message: String,
    val data: List<Product> = emptyList(),
    val hasRestriction: Boolean = false
)

data class OperationResult(
    val success: Boolean,
    val message: String
)

class ProductViewModel : ViewModel() {
    private val baseUrl = "http://192.168.1.155/smart_kantin/api_android"

    fun getAllProducts(): LiveData<ProductResult> {
        val resultLiveData = MutableLiveData<ProductResult>()

        viewModelScope.launch {
            val result = getProductsRequest("$baseUrl/get_barang.php")
            resultLiveData.value = result
        }

        return resultLiveData
    }

    fun getAllowedProducts(nim: String): LiveData<ProductResult> {
        val resultLiveData = MutableLiveData<ProductResult>()

        viewModelScope.launch {
            val result = getProductsRequest("$baseUrl/get_allowed_food.php?nim=$nim")
            resultLiveData.value = result
        }

        return resultLiveData
    }

    fun setAllowedProducts(nim: String, allowedProducts: List<String>): LiveData<OperationResult> {
        val resultLiveData = MutableLiveData<OperationResult>()

        viewModelScope.launch {
            val result = setAllowedProductsRequest(nim, allowedProducts)
            resultLiveData.value = result
        }

        return resultLiveData
    }

    private suspend fun getProductsRequest(urlString: String): ProductResult =
        withContext(Dispatchers.IO) {
            try {
                val url = URL(urlString)
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                connection.connectTimeout = 10000
                connection.readTimeout = 10000

                val responseCode = connection.responseCode
                Log.d("ProductsRequest", "Response Code: $responseCode")

                val inputStream = if (responseCode == HttpURLConnection.HTTP_OK) {
                    connection.inputStream
                } else {
                    connection.errorStream
                }

                val reader = BufferedReader(InputStreamReader(inputStream))
                val response = reader.readText()

                Log.d("ProductsRequest", "Raw Response: $response")

                try {
                    val jsonResponse = JSONObject(response)
                    val success = jsonResponse.optBoolean("success", false)
                    
                    if (success) {
                        val productList = mutableListOf<Product>()
                        val jsonArray = jsonResponse.getJSONArray("data")
                        
                        for (i in 0 until jsonArray.length()) {
                            val item = jsonArray.getJSONObject(i)
                            productList.add(
                                Product(
                                    id = item.optString("id", ""),
                                    idBarang = item.optString("id_barang", ""),
                                    idKategori = item.optString("id_kategori", ""),
                                    namaBarang = item.optString("nama_barang", ""),
                                    merk = item.optString("merk", ""),
                                    hargaBeli = item.optString("harga_beli", ""),
                                    hargaJual = item.optString("harga_jual", ""),
                                    satuanBarang = item.optString("satuan_barang", ""),
                                    stok = item.optString("stok", ""),
                                    expired = item.optString("expired", ""),
                                    tglInput = item.optString("tgl_input", ""),
                                    tglUpdate = item.optString("tgl_update", ""),
                                    namaKategori = item.optString("nama_kategori", "")
                                )
                            )
                        }
                        
                        ProductResult(
                            success = true,
                            message = "Berhasil mendapatkan data produk",
                            data = productList,
                            hasRestriction = jsonResponse.optBoolean("has_restriction", false)
                        )
                    } else {
                        ProductResult(
                            success = false,
                            message = jsonResponse.optString("message", "Gagal mendapatkan data produk")
                        )
                    }
                } catch (jsonException: Exception) {
                    Log.e("ProductsRequest", "JSON Parsing Error: ${jsonException.message}")
                    ProductResult(
                        success = false,
                        message = "Error parsing server response"
                    )
                }
            } catch (e: Exception) {
                Log.e("ProductsRequest", "Network Error: ${e.message}")
                ProductResult(false, "Network Error: ${e.message}")
            }
        }

    private suspend fun setAllowedProductsRequest(nim: String, allowedProducts: List<String>): OperationResult =
        withContext(Dispatchers.IO) {
            try {
                val url = URL("$baseUrl/set_allowed_food.php")
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "POST"
                connection.doOutput = true
                connection.connectTimeout = 10000
                connection.readTimeout = 10000
                connection.setRequestProperty("Content-Type", "application/json; charset=UTF-8")

                // Buat JSONArray untuk allowed_food
                val allowedJsonArray = JSONArray()
                for (product in allowedProducts) {
                    allowedJsonArray.put(product)
                }
                
                val jsonBody = JSONObject().apply {
                    put("nim", nim)
                    put("allowed_food", allowedJsonArray)
                }

                val jsonString = jsonBody.toString()
                Log.d("SetAllowedProducts", "Sending JSON: $jsonString")

                val outputStream = connection.outputStream
                outputStream.write(jsonString.toByteArray())
                outputStream.close()

                val responseCode = connection.responseCode
                Log.d("SetAllowedProducts", "Response Code: $responseCode")

                val inputStream = if (responseCode == HttpURLConnection.HTTP_OK) {
                    connection.inputStream
                } else {
                    connection.errorStream
                }

                val reader = BufferedReader(InputStreamReader(inputStream))
                val response = reader.readText()

                Log.d("SetAllowedProducts", "Raw Response: $response")

                try {
                    // Cek apakah ada warning HTML dalam respons
                    if (response.contains("<br") || response.contains("<b>Warning</b>")) {
                        // Jika ada warning, coba ekstrak JSON-nya
                        val jsonStartIndex = response.lastIndexOf("{")
                        val jsonEndIndex = response.lastIndexOf("}") + 1
                        
                        if (jsonStartIndex >= 0 && jsonEndIndex > jsonStartIndex) {
                            val jsonPart = response.substring(jsonStartIndex, jsonEndIndex)
                            Log.d("SetAllowedProducts", "Extracted JSON part: $jsonPart")
                            val jsonResponse = JSONObject(jsonPart)
                            return@withContext OperationResult(
                                success = jsonResponse.optBoolean("success", false),
                                message = jsonResponse.optString("message", "Unknown error")
                            )
                        }
                        
                        // Jika tidak berhasil ekstrak JSON
                        return@withContext OperationResult(
                            success = false,
                            message = "Server responded with warnings"
                        )
                    }
                    
                    // Jika tidak ada warning, parse respons sebagai JSON normal
                    val jsonResponse = JSONObject(response)
                    OperationResult(
                        success = jsonResponse.optBoolean("success", false),
                        message = jsonResponse.optString("message", "Unknown error")
                    )
                } catch (jsonException: Exception) {
                    Log.e("SetAllowedProducts", "JSON Parsing Error: ${jsonException.message}")
                    OperationResult(
                        success = false,
                        message = "Error parsing server response"
                    )
                }
            } catch (e: Exception) {
                Log.e("SetAllowedProducts", "Network Error: ${e.message}")
                OperationResult(false, "Network Error: ${e.message}")
            }
        }
}