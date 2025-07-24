package com.example.walletlens.data.entity

import androidx.room.ColumnInfo

data class CategoryTotal(
    @ColumnInfo(name = "category")
    val category: String,
    @ColumnInfo(name = "total")
    val total: Double
) 