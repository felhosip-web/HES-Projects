package com.example.expensetracker.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.expensetracker.data.ExpenseDao
import com.example.expensetracker.data.ExpenseEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ExpenseViewModel @Inject constructor(
    private val expenseDao: ExpenseDao
) : ViewModel() {

    val expenses: StateFlow<List<ExpenseEntity>> = expenseDao.getAllExpenses()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun addExpense(description: String, amount: Double, type: String, category: String, date: String, notes: String?) {
        viewModelScope.launch {
            expenseDao.insertExpense(
                ExpenseEntity(
                    description = description,
                    amount = amount,
                    type = type,
                    category = category,
                    date = date,
                    notes = notes
                )
            )
        }
    }

    fun deleteExpense(expense: ExpenseEntity) {
        viewModelScope.launch {
            expenseDao.deleteExpense(expense)
        }
    }

    fun clearAll() {
        viewModelScope.launch {
            expenseDao.clearAll()
        }
    }
}
