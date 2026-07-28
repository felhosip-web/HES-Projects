package com.example.expensetracker.ui

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.expensetracker.data.ExpenseDao
import com.example.expensetracker.data.ExpenseEntity
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import com.example.expensetracker.sync.FirebaseSyncManager
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CategoryData(
    val name: String,
    val colorHex: String,
    val budget: Double = 0.0,
    val type: String = "expense" // "expense", "income", or "both"
)

data class BackupData(
    val version: Int = 1,
    val categories: List<CategoryData>,
    val expenses: List<ExpenseEntity>
)

@HiltViewModel
class ExpenseViewModel @Inject constructor(
    private val expenseDao: ExpenseDao,
    val syncManager: FirebaseSyncManager,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val sharedPrefs = context.getSharedPreferences("expense_tracker_prefs", Context.MODE_PRIVATE)
    private val gson = Gson()

    // Themes options keys: "SLATE_DARK", "LIGHT_THEME", "EMERALD_DARK", "OCEAN_DARK"
    private val _selectedTheme = MutableStateFlow(sharedPrefs.getString("selected_theme", "SLATE_DARK") ?: "SLATE_DARK")
    val selectedTheme: StateFlow<String> = _selectedTheme.asStateFlow()

    private val defaultCategories = listOf(
        CategoryData("Étkezés", "#F43F5E", 150000.0, "expense"),
        CategoryData("Vásárlás", "#EAB308", 80000.0, "expense"),
        CategoryData("Lakhatás", "#3B82F6", 250000.0, "expense"),
        CategoryData("Közlekedés", "#A855F7", 60000.0, "expense"),
        CategoryData("Szórakozás", "#6366F1", 50000.0, "expense"),
        CategoryData("Rezsi", "#F97316", 70000.0, "expense"),
        CategoryData("Egészségügy", "#14B8A6", 40000.0, "expense"),
        CategoryData("Fizetés", "#10B981", 0.0, "income"),
        CategoryData("Befektetés", "#06B6D4", 0.0, "income"),
        CategoryData("Egyéb", "#6B7280", 30000.0, "both")
    )

    private val _categories = MutableStateFlow<List<CategoryData>>(emptyList())
    val categories: StateFlow<List<CategoryData>> = _categories.asStateFlow()

    init {
        loadCategories()
    }

    private fun loadCategories() {
        val json = sharedPrefs.getString("custom_categories", null)
        if (json.isNullOrBlank()) {
            _categories.value = defaultCategories
        } else {
            try {
                val type = object : TypeToken<List<CategoryData>>() {}.type
                val list: List<CategoryData> = gson.fromJson(json, type)
                _categories.value = list.ifEmpty { defaultCategories }
            } catch (e: Exception) {
                _categories.value = defaultCategories
            }
        }
    }

    private fun saveCategories(list: List<CategoryData>) {
        _categories.value = list
        sharedPrefs.edit().putString("custom_categories", gson.toJson(list)).apply()
    }

    val expenses: StateFlow<List<ExpenseEntity>> = expenseDao.getAllExpenses()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun setTheme(themeKey: String) {
        _selectedTheme.value = themeKey
        sharedPrefs.edit().putString("selected_theme", themeKey).apply()
    }

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
            syncNow()
        }
    }

    fun deleteExpense(expense: ExpenseEntity) {
        viewModelScope.launch {
            expenseDao.deleteExpense(expense)
            syncNow()
        }
    }

    fun clearAll() {
        viewModelScope.launch {
            expenseDao.clearAll()
            syncNow()
        }
    }

    fun resetToDefaults() {
        viewModelScope.launch {
            expenseDao.clearAll()
            saveCategories(defaultCategories)
            setTheme("SLATE_DARK")
            syncNow()
        }
    }

    // Dynamic Category Management
    fun addCategory(name: String, colorHex: String, budget: Double = 0.0, type: String = "expense"): Boolean {
        val normalized = name.trim()
        if (normalized.isBlank()) return false
        if (_categories.value.any { it.name.equals(normalized, ignoreCase = true) }) return false

        val newList = _categories.value + CategoryData(normalized, colorHex, budget, type)
        saveCategories(newList)
        syncNow()
        return true
    }

    fun renameCategory(oldName: String, newName: String): Boolean {
        val normalized = newName.trim()
        if (normalized.isBlank()) return false
        if (normalized.equals(oldName, ignoreCase = true)) return true
        if (_categories.value.any { it.name.equals(normalized, ignoreCase = true) && !it.name.equals(oldName, ignoreCase = true) }) return false

        viewModelScope.launch {
            expenseDao.updateCategoryName(oldName, normalized)
            val newList = _categories.value.map {
                if (it.name.equals(oldName, ignoreCase = true)) {
                    it.copy(name = normalized)
                } else {
                    it
                }
            }
            saveCategories(newList)
            syncNow()
        }
        return true
    }

    fun updateCategoryBudget(name: String, newBudget: Double): Boolean {
        if (newBudget < 0) return false
        viewModelScope.launch {
            val newList = _categories.value.map {
                if (it.name.equals(name, ignoreCase = true)) {
                    it.copy(budget = newBudget)
                } else {
                    it
                }
            }
            saveCategories(newList)
            syncNow()
        }
        return true
    }

    fun deleteCategory(name: String) {
        if (name.equals("Egyéb", ignoreCase = true)) return // Prevent deleting Others
        viewModelScope.launch {
            expenseDao.updateCategoryName(name, "Egyéb")
            val newList = _categories.value.filter { !it.name.equals(name, ignoreCase = true) }
            saveCategories(newList)
            syncNow()
        }
    }

    // Backup & Restore
    fun exportBackup(): String {
        val backup = BackupData(
            categories = _categories.value,
            expenses = expenses.value
        )
        return gson.toJson(backup)
    }

    fun importBackup(json: String): Boolean {
        return try {
            val backup = gson.fromJson(json, BackupData::class.java)
            if (backup != null && backup.categories != null && backup.expenses != null) {
                viewModelScope.launch {
                    expenseDao.clearAll()
                    expenseDao.insertAll(backup.expenses)
                    saveCategories(backup.categories)
                }
                true
            } else {
                false
            }
        } catch (e: Exception) {
            false
        }
    }

    private val _isCloudSyncEnabled = MutableStateFlow(syncManager.isCloudSyncEnabled)
    val isCloudSyncEnabled: StateFlow<Boolean> = _isCloudSyncEnabled.asStateFlow()

    private val _lastSyncTime = MutableStateFlow(syncManager.lastSyncTime)
    val lastSyncTime: StateFlow<String> = _lastSyncTime.asStateFlow()

    fun toggleCloudSync(enabled: Boolean) {
        viewModelScope.launch {
            if (enabled) {
                val success = syncManager.enableCloudSync()
                if (success) {
                    _isCloudSyncEnabled.value = true
                    syncNow()
                }
            } else {
                syncManager.disableCloudSync()
                _isCloudSyncEnabled.value = false
            }
        }
    }

    fun syncNow(onResult: ((Boolean) -> Unit)? = null) {
        viewModelScope.launch {
            if (_isCloudSyncEnabled.value) {
                val success = syncManager.syncNow()
                if (success) {
                    _lastSyncTime.value = syncManager.lastSyncTime
                }
                onResult?.invoke(success)
            } else {
                onResult?.invoke(false)
            }
        }
    }
}
