package com.android.simkanti.ui.product

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.GridLayoutManager
import com.android.simkanti.databinding.FragmentProductBinding

class ProductFragment : Fragment() {
    private var _binding: FragmentProductBinding? = null
    private val binding get() = _binding!!
    private lateinit var productViewModel: ProductViewModel
    private lateinit var productAdapter: ProductAdapter
    private var allowedProducts = mutableListOf<String>()
    private var nim: String = ""
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProductBinding.inflate(inflater, container, false)
        val root: View = binding.root

        // Initialize ViewModel
        productViewModel = ViewModelProvider(this).get(ProductViewModel::class.java)

        // Retrieve NIM dari SharedPreferences
        val sharedPref = requireActivity().getSharedPreferences("login_data", Context.MODE_PRIVATE)
        nim = sharedPref.getString("nim", "") ?: ""

        // Setup RecyclerView
        setupRecyclerView()
        
        // Setup save button
        binding.btnSaveRestrictions.setOnClickListener {
            saveProductRestrictions()
        }

        // Ambil data produk
        loadProducts()

        return root
    }

    private fun setupRecyclerView() {
        productAdapter = ProductAdapter { productId, isChecked ->
            if (isChecked) {
                if (!allowedProducts.contains(productId)) {
                    allowedProducts.add(productId)
                }
            } else {
                allowedProducts.remove(productId)
            }
        }
        binding.rvProducts.apply {
            layoutManager = GridLayoutManager(context, 2)
            adapter = productAdapter
        }
    }

    private fun loadProducts() {
        binding.progressBar.visibility = View.VISIBLE
        
        // Ambil semua produk
        productViewModel.getAllProducts().observe(viewLifecycleOwner) { result ->
            if (result.success) {
                productAdapter.submitList(result.data)
                // Setelah mendapatkan semua produk, ambil pembatasan yang sudah disimpan
                loadExistingRestrictions()
            } else {
                binding.progressBar.visibility = View.GONE
                Toast.makeText(requireContext(), result.message, Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    private fun loadExistingRestrictions() {
        if (nim.isEmpty()) {
            binding.progressBar.visibility = View.GONE
            return
        }
        
        productViewModel.getAllowedProducts(nim).observe(viewLifecycleOwner) { result ->
            binding.progressBar.visibility = View.GONE
            if (result.success && result.hasRestriction) {
                allowedProducts.clear()
                result.data.forEach { product ->
                    allowedProducts.add(product.idBarang)
                }
                productAdapter.setCheckedItems(allowedProducts)
            }
        }
    }
    
    private fun saveProductRestrictions() {
        if (nim.isEmpty()) {
            Toast.makeText(requireContext(), "NIM tidak ditemukan", Toast.LENGTH_SHORT).show()
            return
        }
        
        binding.progressBar.visibility = View.VISIBLE
        productViewModel.setAllowedProducts(nim, allowedProducts).observe(viewLifecycleOwner) { result ->
            binding.progressBar.visibility = View.GONE
            Toast.makeText(requireContext(), result.message, Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}