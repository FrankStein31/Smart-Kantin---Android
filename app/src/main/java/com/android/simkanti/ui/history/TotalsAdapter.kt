package com.android.simkanti.ui.history

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.android.simkanti.R
import java.text.NumberFormat
import java.util.Locale

class DailyTotalsAdapter : RecyclerView.Adapter<DailyTotalsAdapter.TotalViewHolder>() {
    private var items: List<DailyTotal> = emptyList()
    
    class TotalViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val dateTextView: TextView = view.findViewById(R.id.text_date)
        val totalTextView: TextView = view.findViewById(R.id.text_total)
    }
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TotalViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_total, parent, false)
        return TotalViewHolder(view)
    }
    
    override fun onBindViewHolder(holder: TotalViewHolder, position: Int) {
        val item = items[position]
        
        holder.dateTextView.text = item.formattedDate
        
        // Format amount with Indonesian Rupiah formatting
        val numberFormat = NumberFormat.getCurrencyInstance(Locale("id", "ID"))
        val formattedAmount = numberFormat.format(item.total)
        
        holder.totalTextView.text = formattedAmount
    }
    
    override fun getItemCount() = items.size
    
    fun updateData(newItems: List<DailyTotal>) {
        items = newItems
        notifyDataSetChanged()
    }
}

class MonthlyTotalsAdapter : RecyclerView.Adapter<MonthlyTotalsAdapter.TotalViewHolder>() {
    private var items: List<MonthlyTotal> = emptyList()
    
    class TotalViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val dateTextView: TextView = view.findViewById(R.id.text_date)
        val totalTextView: TextView = view.findViewById(R.id.text_total)
    }
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TotalViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_total, parent, false)
        return TotalViewHolder(view)
    }
    
    override fun onBindViewHolder(holder: TotalViewHolder, position: Int) {
        val item = items[position]
        
        holder.dateTextView.text = item.formattedMonth
        
        // Format amount with Indonesian Rupiah formatting
        val numberFormat = NumberFormat.getCurrencyInstance(Locale("id", "ID"))
        val formattedAmount = numberFormat.format(item.total)
        
        holder.totalTextView.text = formattedAmount
    }
    
    override fun getItemCount() = items.size
    
    fun updateData(newItems: List<MonthlyTotal>) {
        items = newItems
        notifyDataSetChanged()
    }
} 