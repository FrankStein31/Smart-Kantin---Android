package com.android.simkanti.ui.product

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.GridLayoutManager
import com.android.simkanti.databinding.FragmentProductBinding
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup

class ProductFragment : Fragment() {
    private var _binding: FragmentProductBinding? = null
    private val binding get() = _binding!!
    private lateinit var productViewModel: ProductViewModel
    private lateinit var productAdapter: ProductAdapter
    private var allowedProducts = mutableListOf<String>()
    private var nim: String = ""
    private var allProducts = listOf<Product>()
    private var selectedCategoryId: String = "0" // 0 untuk semua kategori
    
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

        // Setup Chip untuk filter "Semua"
        binding.chipAll.setOnClickListener {
            selectedCategoryId = "0"
            filterProducts()
        }

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
                allProducts = result.data
                productAdapter.submitList(result.data)
                
                // Setelah mendapatkan data, buat chip kategori
                createCategoryChips(extractCategories(result.data))
                
                // Setelah mendapatkan semua produk, ambil pembatasan yang sudah disimpan
                loadExistingRestrictions()
            } else {
                binding.progressBar.visibility = View.GONE
                Toast.makeText(requireContext(), result.message, Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    private fun extractCategories(products: List<Product>): List<Pair<String, String>> {
        val categories = mutableMapOf<String, String>()
        products.forEach { product ->
            val categoryId = product.idKategori
            val categoryName = product.namaKategori ?: when(categoryId) {
                "1" -> "Beverage (Minuman)"
                "2" -> "Food (Makanan)"
                "3" -> "Snack"
                else -> "Kategori $categoryId"
            }
            categories[categoryId] = categoryName
        }
        return categories.toList()
    }
    
    private fun createCategoryChips(categories: List<Pair<String, String>>) {
        // Chip group sudah memiliki chip "Semua"
        val chipGroup = binding.chipGroupCategories
        
        // Tambahkan chip untuk setiap kategori
        categories.forEach { (categoryId, categoryName) ->
            val chip = Chip(requireContext())
            
            chip.text = categoryName
            chip.id = View.generateViewId()
            chip.tag = categoryId
            chip.isCheckable = true
            
            // Set warna menggunakan selectors
            chip.setChipBackgroundColorResource(com.android.simkanti.R.color.chip_background_color)
            chip.setTextColor(ContextCompat.getColorStateList(requireContext(), com.android.simkanti.R.color.chip_text_color))
            
            chip.setOnClickListener {
                // Uncheck chip "Semua" jika chip kategori dipilih
                if (binding.chipAll.isChecked) {
                    binding.chipAll.isChecked = false
                }
                
                selectedCategoryId = categoryId
                filterProducts()
            }
            
            chipGroup.addView(chip)
        }
        
        // Set OnCheckedChangeListener untuk chip "Semua"
        binding.chipAll.setOnCheckedChangeListener { chip, isChecked ->
            if (isChecked) {
                // Uncheck semua chip kategori lain
                for (i in 0 until chipGroup.childCount) {
                    val childChip = chipGroup.getChildAt(i) as? Chip
                    if (childChip != null && childChip.id != binding.chipAll.id) {
                        childChip.isChecked = false
                    }
                }
                selectedCategoryId = "0"
                filterProducts()
            }
        }
    }
    
    private fun filterProducts() {
        if (selectedCategoryId == "0") {
            // Tampilkan semua produk
            productAdapter.submitList(allProducts)
        } else {
            // Filter produk berdasarkan kategori
            val filteredProducts = allProducts.filter { it.idKategori == selectedCategoryId }
            productAdapter.submitList(filteredProducts)
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