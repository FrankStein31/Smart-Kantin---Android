package com.android.simkanti.ui.profile

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.android.simkanti.LoginActivity
import com.android.simkanti.R
import com.android.simkanti.databinding.FragmentProfileBinding
import com.android.volley.Response
import com.android.volley.Request
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import org.json.JSONObject
import java.text.NumberFormat
import java.util.Locale
import com.android.volley.DefaultRetryPolicy

class ProfileFragment : Fragment() {
    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!
    private lateinit var profileViewModel: ProfileViewModel
    private val profileUrl by lazy { getString(R.string.ip_api) + "get_profile.php" }
    private val editProfileUrl by lazy { getString(R.string.ip_api) + "edit_profile.php" }

    private var userEmail = ""
    private var userPhone = ""

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        profileViewModel = ViewModelProvider(this).get(ProfileViewModel::class.java)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Get saved user data
        val sharedPref = requireActivity().getSharedPreferences("login_data", 0)
        val nim = sharedPref.getString("nim", "") ?: ""
        val nama = sharedPref.getString("nama", "") ?: ""

        // Set initial data
        binding.tvNama.text = nama
        binding.tvNim.text = nim
        binding.tvNamaHeader.text = nama
        binding.tvNimHeader.text = nim

        // Load profile data including saldo
        loadProfileData(nim)

        // Setup edit profile button
        binding.btnEditProfile.setOnClickListener {
            showEditProfileDialog(nim, nama)
        }

        // Setup logout button
        binding.btnLogout.setOnClickListener {
            val builder = androidx.appcompat.app.AlertDialog.Builder(requireContext())
            builder.setTitle("Konfirmasi Logout")
            builder.setMessage("Apakah Anda yakin ingin logout?")
            builder.setPositiveButton("Ya") { dialog, _ ->
                // Clear shared preferences
                requireActivity().getSharedPreferences("login_data", 0)
                    .edit()
                    .clear()
                    .apply()

                // Redirect to login
                val intent = Intent(requireActivity(), LoginActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
                requireActivity().finish()

                dialog.dismiss() // Tutup dialog
            }
            builder.setNegativeButton("Batal") { dialog, _ ->
                dialog.dismiss() // Tutup dialog jika pengguna memilih batal
            }
            builder.create().show() // Tampilkan dialog
        }
    }

    private fun loadProfileData(nim: String) {
        binding.progressBar.visibility = View.VISIBLE
        
        // Fix URL, pastikan tidak ada double slash
        val profileUrl = getString(R.string.ip_api).let {
            if (it.endsWith("/")) it + "get_profile.php" else it + "/get_profile.php"
        }
        
        Log.d("ProfileFragment", "Loading profile data from URL: $profileUrl")
        
        // Create a request queue
        val queue = Volley.newRequestQueue(requireContext())

        // Create string request
        val stringRequest = object : StringRequest(
            Method.POST,
            profileUrl,
            Response.Listener { response ->
                binding.progressBar.visibility = View.GONE
                
                try {
                    Log.d("ProfileResponse", "Raw response: $response")
                    
                    val jsonResponse = JSONObject(response)
                    if (jsonResponse.getBoolean("success")) {
                        val data = jsonResponse.getJSONObject("data")
                        val saldo = data.getDouble("saldo")
                        
                        // Save additional data
                        userEmail = data.optString("email", "")
                        userPhone = data.optString("no_hp", "")
                        
                        // Update email and phone fields
                        binding.tvEmail.text = userEmail.ifEmpty { "Belum diatur" }
                        binding.tvPhone.text = userPhone.ifEmpty { "Belum diatur" }

                        // Format saldo to Indonesian Rupiah
                        val formatRupiah = NumberFormat.getCurrencyInstance(Locale("id", "ID"))
                        binding.tvSaldo.text = formatRupiah.format(saldo)
                    } else {
                        Toast.makeText(context, jsonResponse.getString("message"), Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    Log.e("ProfileFragment", "Error parsing response: ${e.message}")
                    Log.e("ProfileFragment", "Response data: $response")
                    Toast.makeText(context, "Error saat memuat profil: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            },
            Response.ErrorListener { error ->
                binding.progressBar.visibility = View.GONE
                
                // Log error yang lebih detail
                val errorMessage = when {
                    error.networkResponse != null -> "Error code: ${error.networkResponse.statusCode}"
                    error.message != null -> error.message
                    else -> "Unknown network error"
                }
                
                Log.e("ProfileFragment", "Volley error: $errorMessage")
                Toast.makeText(context, "Error jaringan: $errorMessage", Toast.LENGTH_SHORT).show()
            }) {
            override fun getParams(): Map<String, String> {
                val params = HashMap<String, String>()
                params["nim"] = nim
                Log.d("ProfileFragment", "Sending params: nim=$nim")
                return params
            }
        }

        // Set timeout lebih lama
        stringRequest.retryPolicy = DefaultRetryPolicy(
            30000, // 30 detik timeout
            DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
            DefaultRetryPolicy.DEFAULT_BACKOFF_MULT
        )

        // Add request to queue
        queue.add(stringRequest)
    }
    
    private fun showEditProfileDialog(nim: String, nama: String) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_edit_profile, null)
        
        // Find views
        val etNama = dialogView.findViewById<EditText>(R.id.et_nama)
        val etEmail = dialogView.findViewById<EditText>(R.id.et_email)
        val etPhone = dialogView.findViewById<EditText>(R.id.et_phone)
        val btnSave = dialogView.findViewById<Button>(R.id.btn_save)
        val btnCancel = dialogView.findViewById<Button>(R.id.btn_cancel)
        
        // Set initial values
        etNama.setText(nama)
        etEmail.setText(userEmail)
        etPhone.setText(userPhone)
        
        // Create dialog
        val alertDialog = AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .setCancelable(true)
            .create()
        
        // Button listeners
        btnSave.setOnClickListener {
            val newNama = etNama.text.toString()
            val newEmail = etEmail.text.toString()
            val newPhone = etPhone.text.toString()
            
            if (newNama.isEmpty()) {
                Toast.makeText(requireContext(), "Nama tidak boleh kosong", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            
            updateProfile(nim, newNama, newEmail, newPhone)
            alertDialog.dismiss()
        }
        
        btnCancel.setOnClickListener {
            alertDialog.dismiss()
        }
        
        alertDialog.show()
    }
    
    private fun updateProfile(nim: String, nama: String, email: String, noHp: String) {
        binding.progressBar.visibility = View.VISIBLE
        
        // Create a request queue
        val queue = Volley.newRequestQueue(requireContext())
        
        // Create string request
        val stringRequest = object : StringRequest(
            Method.POST,
            editProfileUrl,
            Response.Listener { response ->
                binding.progressBar.visibility = View.GONE
                try {
                    val jsonResponse = JSONObject(response)
                    if (jsonResponse.getBoolean("success")) {
                        // Update UI
                        binding.tvNama.text = nama
                        binding.tvNamaHeader.text = nama
                        binding.tvEmail.text = if (email.isEmpty()) "Belum diatur" else email
                        binding.tvPhone.text = if (noHp.isEmpty()) "Belum diatur" else noHp
                        
                        // Update saved data
                        userEmail = email
                        userPhone = noHp
                        
                        // Update SharedPreferences
                        requireActivity().getSharedPreferences("login_data", 0)
                            .edit()
                            .putString("nama", nama)
                            .apply()
                        
                        Toast.makeText(requireContext(), "Profil berhasil diperbarui", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(requireContext(), jsonResponse.getString("message"), Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    Toast.makeText(requireContext(), "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            },
            Response.ErrorListener { error ->
                binding.progressBar.visibility = View.GONE
                Toast.makeText(requireContext(), "Error: ${error.message}", Toast.LENGTH_SHORT).show()
            }) {
            override fun getParams(): Map<String, String> {
                val params = HashMap<String, String>()
                params["nim"] = nim
                params["nama"] = nama
                params["email"] = email
                params["nohp"] = noHp
                return params
            }
        }
        
        // Add request to queue
        queue.add(stringRequest)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}