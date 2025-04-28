package com.android.simkanti.ui.history

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.android.simkanti.databinding.FragmentTransactionDetailBinding
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Locale

class TransactionDetailFragment : Fragment() {
    
    private var _binding: FragmentTransactionDetailBinding? = null
    private val binding get() = _binding!!
    
    private val args: TransactionDetailFragmentArgs by navArgs()
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTransactionDetailBinding.inflate(inflater, container, false)
        return binding.root
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        val transaction = args.transaction
        
        // Format tanggal dan waktu
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val displayFormat = SimpleDateFormat("dd MMMM yyyy", Locale.getDefault())
        
        val date = try {
            val parsedDate = dateFormat.parse(transaction.date)
            parsedDate?.let { displayFormat.format(it) } ?: transaction.date
        } catch (e: Exception) {
            transaction.date
        }
        
        // Format harga
        val priceFormat = NumberFormat.getCurrencyInstance(Locale("id", "ID"))
        val formattedPrice = try {
            priceFormat.format(transaction.totalHarga.toDouble())
        } catch (e: Exception) {
            "Rp${transaction.totalHarga}"
        }
        
        // Tampilkan data ke UI
        with(binding) {
            tvTransactionId.text = "#${transaction.id}"
            tvTransactionDate.text = date
            tvTransactionTime.text = transaction.time
            tvProductId.text = transaction.idBarang
            tvProductName.text = transaction.namaBarang
            tvProductCategory.text = transaction.kategori
            tvTransactionTotal.text = formattedPrice
            tvTransactionNim.text = transaction.nim
            
            btnBack.setOnClickListener {
                findNavController().popBackStack()
            }
        }
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
} 