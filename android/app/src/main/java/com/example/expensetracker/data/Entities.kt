package com.example.expensetracker.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "expenses")
data class ExpenseEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val description: String,
    val amount: Double,
    val type: String, // "income" or "expense"
    val category: String,
    val date: String, // YYYY-MM-DD
    val notes: String? = null
)
