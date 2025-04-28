package com.android.simkanti.ui.history

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.android.simkanti.R
import java.text.NumberFormat
import java.util.Locale

class HistoryAdapter(
    private var transactions: List<HistoryTransaction>,
    private val onItemClick: (HistoryTransaction) -> Unit
) : RecyclerView.Adapter<HistoryAdapter.HistoryViewHolder>() {

    class HistoryViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val dateTextView: TextView = view.findViewById(R.id.text_history_date)
        val amountTextView: TextView = view.findViewById(R.id.text_history_amount)
        val timeTextView: TextView = view.findViewById(R.id.text_history_time)
        val productNameTextView: TextView = view.findViewById(R.id.text_product_name)
        val productIdTextView: TextView = view.findViewById(R.id.text_product_id)
        val rootView: View = view
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HistoryViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_history_transaction, parent, false)
        return HistoryViewHolder(view)
    }

    override fun onBindViewHolder(holder: HistoryViewHolder, position: Int) {
        val transaction = transactions[position]

        // Format amount with Indonesian Rupiah formatting
        val numberFormat = NumberFormat.getCurrencyInstance(Locale("id", "ID"))
        val formattedAmount = numberFormat.format(
            transaction.totalHarga.replace("Rp", "").replace(",", "").trim().toDoubleOrNull() ?: 0.0
        )

        holder.dateTextView.text = transaction.date
        holder.amountTextView.text = formattedAmount
        holder.timeTextView.text = transaction.time ?: "-"
        
        // Set product info
        holder.productNameTextView.text = "${transaction.kategori} - ${transaction.namaBarang}"
        holder.productIdTextView.visibility = View.GONE
        
        // Set click listener
        holder.rootView.setOnClickListener {
            onItemClick(transaction)
        }
    }

    override fun getItemCount() = transactions.size

    fun updateData(newTransactions: List<HistoryTransaction>) {
        transactions = newTransactions
        notifyDataSetChanged()
    }
}