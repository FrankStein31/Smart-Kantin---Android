package com.android.simkanti.ui.history

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.android.simkanti.databinding.FragmentHistoryBinding
import java.text.NumberFormat
import java.util.*

class HistoryFragment : Fragment() {
    private var _binding: FragmentHistoryBinding? = null
    private val binding get() = _binding!!

    private lateinit var historyViewModel: HistoryViewModel
    private lateinit var historyAdapter: HistoryAdapter
    private var nim: String = ""

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHistoryBinding.inflate(inflater, container, false)
        val root: View = binding.root

        // Initialize ViewModel
        historyViewModel = ViewModelProvider(this).get(HistoryViewModel::class.java)

        // Setup RecyclerView
        historyAdapter = HistoryAdapter(emptyList())
        binding.recyclerViewHistory.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = historyAdapter
        }

        // Get NIM from SharedPreferences
        val sharedPref = requireActivity().getSharedPreferences("login_data", Context.MODE_PRIVATE)
        nim = sharedPref.getString("nim", "") ?: ""

        // Setup daily limit UI
        setupDailyLimit()

        // Show loading
        binding.progressBarHistory.visibility = View.VISIBLE

        // Observe history data
        historyViewModel.historyList.observe(viewLifecycleOwner) { transactions ->
            binding.progressBarHistory.visibility = View.GONE

            if (transactions.isNotEmpty()) {
                historyAdapter.updateData(transactions)
                binding.textNoHistory.visibility = View.GONE
                binding.recyclerViewHistory.visibility = View.VISIBLE
            } else {
                binding.textNoHistory.visibility = View.VISIBLE
                binding.recyclerViewHistory.visibility = View.GONE
            }
        }

        // Observe daily limit data
        historyViewModel.dailyLimit.observe(viewLifecycleOwner) { dailyLimit ->
            updateDailyLimitUI(dailyLimit)
        }

        // Observe error messages
        historyViewModel.errorMessage.observe(viewLifecycleOwner) { errorMsg ->
            binding.progressBarHistory.visibility = View.GONE
            Toast.makeText(requireContext(), errorMsg, Toast.LENGTH_SHORT).show()
        }

        // Observe limit update result
        historyViewModel.limitUpdateResult.observe(viewLifecycleOwner) { result ->
            Toast.makeText(requireContext(), result.message, Toast.LENGTH_SHORT).show()
        }

        // Fetch data
        if (nim.isNotEmpty()) {
            historyViewModel.fetchHistoryForNIM(nim)
            historyViewModel.fetchDailyLimit(nim)
        } else {
            Toast.makeText(requireContext(), "NIM tidak ditemukan", Toast.LENGTH_SHORT).show()
        }

        return root
    }

    private fun setupDailyLimit() {
        binding.btnSetLimit.setOnClickListener {
            val limitText = binding.editDailyLimit.text.toString()
            if (limitText.isNotEmpty()) {
                try {
                    val limitAmount = limitText.toDouble()
                    historyViewModel.setDailyLimit(nim, limitAmount)
                    binding.editDailyLimit.text.clear()
                } catch (e: Exception) {
                    Toast.makeText(requireContext(), "Format batasan tidak valid", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(requireContext(), "Masukkan nilai batasan", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun updateDailyLimitUI(dailyLimit: DailyLimit) {
        val formatRupiah = NumberFormat.getCurrencyInstance(Locale("id", "ID"))
        binding.textDailyLimit.text = formatRupiah.format(dailyLimit.limitAmount)
        binding.textDailySpent.text = formatRupiah.format(dailyLimit.spentToday)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}