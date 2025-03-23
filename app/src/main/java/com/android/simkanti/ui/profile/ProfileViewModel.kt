package com.android.simkanti.ui.profile

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

data class ProfileData(
    val nim: String,
    val nama: String,
    val email: String,
    val nohp: String,
    val saldo: Double
)

data class ApiResponse(
    val success: Boolean,
    val message: String,
    val data: ProfileData? = null
)

class ProfileViewModel : ViewModel() {
    
    private val _profileData = MutableLiveData<ProfileData>()
    val profileData: LiveData<ProfileData> = _profileData
    
    private val _updateResult = MutableLiveData<ApiResponse>()
    val updateResult: LiveData<ApiResponse> = _updateResult
    
    fun updateProfile(baseUrl: String, nim: String, nama: String, email: String, nohp: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val url = URL("${baseUrl}edit_profile.php")
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "POST"
                connection.doOutput = true
                connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded")
                
                val postData = "nim=$nim&nama=$nama&email=$email&nohp=$nohp"
                val wr = OutputStreamWriter(connection.outputStream)
                wr.write(postData)
                wr.flush()
                
                val responseCode = connection.responseCode
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    val response = connection.inputStream.bufferedReader().use { it.readText() }
                    val jsonResponse = JSONObject(response)
                    
                    val success = jsonResponse.getBoolean("success")
                    val message = jsonResponse.getString("message")
                    
                    _updateResult.postValue(ApiResponse(success, message))
                } else {
                    _updateResult.postValue(ApiResponse(false, "Error: Response code $responseCode"))
                }
                
                connection.disconnect()
            } catch (e: Exception) {
                _updateResult.postValue(ApiResponse(false, "Error: ${e.message}"))
            }
        }
    }
}