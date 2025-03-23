package com.android.simkanti.ui.product

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.android.simkanti.R
import android.util.Log

class ProductAdapter(
    private val onProductChecked: (String, Boolean) -> Unit
) : ListAdapter<Product, ProductAdapter.ProductViewHolder>(ProductDiffCallback()) {

    private val checkedItems = mutableSetOf<String>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProductViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_product, parent, false)
        return ProductViewHolder(view)
    }

    override fun onBindViewHolder(holder: ProductViewHolder, position: Int) {
        val product = getItem(position)
        holder.bind(product, checkedItems.contains(product.idBarang))
    }

    fun setCheckedItems(items: List<String>) {
        checkedItems.clear()
        checkedItems.addAll(items)
        notifyDataSetChanged()
    }

    inner class ProductViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvProductName: TextView = itemView.findViewById(R.id.tv_product_name)
        private val tvProductPrice: TextView = itemView.findViewById(R.id.tv_product_price)
        private val tvProductCategory: TextView = itemView.findViewById(R.id.tv_product_category)
        private val cbAllowed: CheckBox = itemView.findViewById(R.id.cb_product_allowed)

        fun bind(product: Product, isChecked: Boolean) {
            tvProductName.text = product.namaBarang
            tvProductPrice.text = "Rp ${product.hargaJual}"
            
            // Ambil nama kategori dari database jika tersedia
            val kategoriText = when {
                product.namaKategori != null && product.namaKategori.isNotEmpty() -> {
                    product.namaKategori
                }
                product.idKategori.isNotEmpty() -> {
                    when (product.idKategori) {
                        "1" -> "Beverage (Minuman)"
                        "2" -> "Food (Makanan)"
                        "3" -> "Snack"
                        else -> "Kategori ${product.idKategori}"
                    }
                }
                else -> {
                    "Tidak ada kategori"
                }
            }
            
            // Set kategori ke TextView dan pastikan visibility VISIBLE
            tvProductCategory.text = kategoriText
            tvProductCategory.visibility = View.VISIBLE
            
            // Log informasi kategori untuk debugging
            Log.d("ProductAdapter", "Product: ${product.namaBarang}, Kategori: $kategoriText, " +
                    "ID Kategori: ${product.idKategori}, Nama Kategori: ${product.namaKategori}")

            cbAllowed.visibility = View.VISIBLE
            cbAllowed.isChecked = isChecked
            cbAllowed.setOnCheckedChangeListener { _, checked ->
                onProductChecked(product.idBarang, checked)
            }
        }
    }

    private class ProductDiffCallback : DiffUtil.ItemCallback<Product>() {
        override fun areItemsTheSame(oldItem: Product, newItem: Product): Boolean {
            return oldItem.idBarang == newItem.idBarang
        }

        override fun areContentsTheSame(oldItem: Product, newItem: Product): Boolean {
            return oldItem == newItem
        }
    }
} 