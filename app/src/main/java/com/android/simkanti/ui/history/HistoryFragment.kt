package com.android.simkanti.ui.history

import android.app.DatePickerDialog
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
import java.text.SimpleDateFormat
import java.util.*
import androidx.navigation.fragment.findNavController

class HistoryFragment : Fragment() {
    private var _binding: FragmentHistoryBinding? = null
    private val binding get() = _binding!!

    private lateinit var historyViewModel: HistoryViewModel
    private lateinit var historyAdapter: HistoryAdapter
    private lateinit var dailyTotalsAdapter: DailyTotalsAdapter
    private lateinit var monthlyTotalsAdapter: MonthlyTotalsAdapter
    
    private var nim: String = ""
    private var startDate: String? = null
    private var endDate: String? = null
    
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    private val displayDateFormat = SimpleDateFormat("dd MMMM yyyy", Locale.getDefault())

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHistoryBinding.inflate(inflater, container, false)
        val root: View = binding.root

        // Initialize ViewModel
        historyViewModel = ViewModelProvider(this).get(HistoryViewModel::class.java)

        // Setup RecyclerView untuk riwayat transaksi
        historyAdapter = HistoryAdapter(emptyList()) { transaction ->
            // Navigasi ke detail transaksi
            val action = HistoryFragmentDirections.actionNavigationHistoryToNavigationTransactionDetail(transaction)
            findNavController().navigate(action)
        }
        binding.recyclerViewHistory.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = historyAdapter
        }
        
        // Setup RecyclerView untuk total harian
        dailyTotalsAdapter = DailyTotalsAdapter()
        binding.recyclerViewDailyTotals.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = dailyTotalsAdapter
        }
        
        // Setup RecyclerView untuk total bulanan
        monthlyTotalsAdapter = MonthlyTotalsAdapter()
        binding.recyclerViewMonthlyTotals.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = monthlyTotalsAdapter
        }

        // Get NIM from SharedPreferences
        val sharedPref = requireActivity().getSharedPreferences("login_data", Context.MODE_PRIVATE)
        nim = sharedPref.getString("nim", "") ?: ""

        // Setup date filter pickers
        setupDatePickers()
        
        // Setup filter button
        binding.btnApplyFilter.setOnClickListener {
            applyFilter()
        }
        
        // Setup clear filter button
        binding.btnClearFilter.setOnClickListener {
            clearFilter()
        }
        
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
        
        // Observe daily totals
        historyViewModel.dailyTotals.observe(viewLifecycleOwner) { dailyTotals ->
            dailyTotalsAdapter.updateData(dailyTotals)
        }
        
        // Observe monthly totals
        historyViewModel.monthlyTotals.observe(viewLifecycleOwner) { monthlyTotals ->
            monthlyTotalsAdapter.updateData(monthlyTotals)
        }
        
        // Observe filter total
        historyViewModel.filterTotal.observe(viewLifecycleOwner) { total ->
            updateFilterTotal(total)
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
    
    private fun setupDatePickers() {
        binding.textStartDate.setOnClickListener {
            showDatePicker(binding.textStartDate, true)
        }
        
        binding.textEndDate.setOnClickListener {
            showDatePicker(binding.textEndDate, false)
        }
    }
    
    private fun showDatePicker(textView: TextView, isStartDate: Boolean) {
        val calendar = Calendar.getInstance()
        
        val datePickerDialog = DatePickerDialog(
            requireContext(),
            { _, year, month, dayOfMonth ->
                calendar.set(Calendar.YEAR, year)
                calendar.set(Calendar.MONTH, month)
                calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth)
                
                val selectedDate = dateFormat.format(calendar.time)
                val displayDate = displayDateFormat.format(calendar.time)
                
                textView.text = displayDate
                
                if (isStartDate) {
                    startDate = selectedDate
                } else {
                    endDate = selectedDate
                }
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )
        
        datePickerDialog.show()
    }
    
    private fun applyFilter() {
        if (startDate != null && endDate != null) {
            // Validasi tanggal
            try {
                val start = dateFormat.parse(startDate!!)
                val end = dateFormat.parse(endDate!!)
                
                if (start != null && end != null && start.after(end)) {
                    Toast.makeText(
                        requireContext(),
                        "Tanggal mulai tidak boleh setelah tanggal akhir",
                        Toast.LENGTH_SHORT
                    ).show()
                    return
                }
            } catch (e: Exception) {
                Toast.makeText(
                    requireContext(),
                    "Format tanggal tidak valid",
                    Toast.LENGTH_SHORT
                ).show()
                return
            }
            
            // Tampilkan rentang tanggal yang difilter
            updateFilterDateRange(startDate, endDate)
        } else if (startDate != null) {
            updateFilterDateRange(startDate, null)
        } else if (endDate != null) {
            updateFilterDateRange(null, endDate)
        } else {
            Toast.makeText(
                requireContext(),
                "Pilih setidaknya satu tanggal untuk filter",
                Toast.LENGTH_SHORT
            ).show()
            return
        }
        
        // Terapkan filter
        historyViewModel.setDateFilter(startDate, endDate)
        binding.progressBarHistory.visibility = View.VISIBLE
        historyViewModel.fetchHistoryForNIM(nim)
    }
    
    private fun clearFilter() {
        startDate = null
        endDate = null
        binding.textStartDate.text = "Pilih Tanggal"
        binding.textEndDate.text = "Pilih Tanggal"
        binding.textFilterDateRange.text = "Semua transaksi"
        
        historyViewModel.clearDateFilter()
        binding.progressBarHistory.visibility = View.VISIBLE
        historyViewModel.fetchHistoryForNIM(nim)
    }
    
    private fun updateFilterDateRange(start: String?, end: String?) {
        val startDisplayDate = if (start != null) {
            try {
                val date = dateFormat.parse(start)
                if (date != null) displayDateFormat.format(date) else start
            } catch (e: Exception) {
                start
            }
        } else "awal"
        
        val endDisplayDate = if (end != null) {
            try {
                val date = dateFormat.parse(end)
                if (date != null) displayDateFormat.format(date) else end
            } catch (e: Exception) {
                end
            }
        } else "sekarang"
        
        binding.textFilterDateRange.text = "Transaksi dari $startDisplayDate sampai $endDisplayDate"
    }
    
    private fun updateFilterTotal(total: Double) {
        val numberFormat = NumberFormat.getCurrencyInstance(Locale("id", "ID"))
        binding.textFilterTotal.text = numberFormat.format(total)
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