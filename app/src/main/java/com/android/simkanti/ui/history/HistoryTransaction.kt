package com.android.simkanti.ui.history

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class HistoryTransaction(
    val id: Int,
    val nim: String,
    val totalHarga: String,
    val idBarang: String,
    val namaBarang: String,
    val kategori: String,
    val date: String,
    val time: String? = null
) : Parcelable 