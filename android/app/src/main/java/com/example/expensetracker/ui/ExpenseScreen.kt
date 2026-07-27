package com.example.expensetracker.ui

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.expensetracker.R
import com.example.expensetracker.data.ExpenseEntity
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*
import androidx.compose.ui.res.stringResource

enum class AppTheme(val displayName: String, val background: Color, val cardBackground: Color, val primary: Color, val outlineColor: Color, val textColor: Color, val secondaryTextColor: Color, val isDark: Boolean) {
    SLATE_DARK("Sötét", Color(0xFF0F172A), Color(0xFF1E293B), Color(0xFF38BDF8), Color(0xFF334155), Color(0xFFF8FAFC), Color(0xFF94A3B8), true),
    LIGHT_THEME("Világos", Color(0xFFF8FAFC), Color.White, Color(0xFF3B82F6), Color(0xFFE2E8F0), Color(0xFF0F172A), Color(0xFF475569), false),
    EMERALD_DARK("Sötét (Zöld)", Color(0xFF064E3B), Color(0xFF065F46), Color(0xFF10B981), Color(0xFF047857), Color(0xFFECFDF5), Color(0xFFA7F3D0), true),
    OCEAN_DARK("Sötét (Kék)", Color(0xFF082F49), Color(0xFF064E3B), Color(0xFF0EA5E9), Color(0xFF0369A1), Color(0xFFF0F9FF), Color(0xFFBAE6FD), true)
}

val PRESET_COLORS = listOf(
    "#F43F5E", "#EAB308", "#3B82F6", "#A855F7", "#6366F1", "#F97316", "#14B8A6", "#10B981", "#06B6D4", "#6B7280"
)

fun formatHuf(amount: Double): String {
    val format = NumberFormat.getNumberInstance(Locale("hu", "HU"))
    return "${format.format(amount)} Ft"
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpenseScreen(
    viewModel: ExpenseViewModel,
    modifier: Modifier = Modifier
) {
    val expenses by viewModel.expenses.collectAsState()
    val dynamicCategories by viewModel.categories.collectAsState()
    val selectedThemeKey by viewModel.selectedTheme.collectAsState()
    val isCloudSyncEnabledMain by viewModel.isCloudSyncEnabled.collectAsState()

    val appTheme = try {
        AppTheme.valueOf(selectedThemeKey)
    } catch (e: Exception) {
        AppTheme.SLATE_DARK
    }

    var showSettings by remember { mutableStateOf(false) }
    var showAddDialog by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var typeFilter by remember { mutableStateOf("All") }
    val incomeFilterName = stringResource(R.string.type_income)
    val expenseFilterName = stringResource(R.string.type_expense)

    // Dialog state for delete confirmation
    var transactionToDelete by remember { mutableStateOf<ExpenseEntity?>(null) }
    var showClearAllConfirm by remember { mutableStateOf(false) }

    val totalIncome = expenses.filter { it.type == "income" }.sumOf { it.amount }
    val totalExpense = expenses.filter { it.type == "expense" }.sumOf { it.amount }
    val balance = totalIncome - totalExpense
    val savingsRate = if (totalIncome > 0) ((totalIncome - totalExpense) / totalIncome * 100).coerceAtLeast(0.0) else 0.0

    if (showSettings) {
        SettingsScreen(
            viewModel = viewModel,
            appTheme = appTheme,
            dynamicCategories = dynamicCategories,
            onBack = { showSettings = false }
        )
    } else {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Column {
                            Text(
                                text = stringResource(R.string.app_name),
                                fontWeight = FontWeight.Black,
                                style = MaterialTheme.typography.titleLarge,
                                color = Color.White
                            )
                            Text(
                                text = stringResource(R.string.dashboard),
                                fontSize = 11.sp,
                                color = Color.LightGray
                            )
                        }
                    },
                    actions = {
                        // Cloud Status Indicator
                        Box(
                            modifier = Modifier
                                .size(12.dp)
                                .background(
                                    color = if (isCloudSyncEnabledMain) Color(0xFF10B981) else Color(0xFF9CA3AF),
                                    shape = RoundedCornerShape(6.dp)
                                )
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        IconButton(onClick = { showSettings = true }) {
                            Icon(Icons.Default.Settings, contentDescription = stringResource(R.string.settings), tint = Color.White)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = if (appTheme.isDark) appTheme.cardBackground else appTheme.primary
                    )
                )
            },
            floatingActionButton = {
                FloatingActionButton(
                    onClick = { showAddDialog = true },
                    containerColor = appTheme.primary,
                    contentColor = if (appTheme.isDark) Color(0xFF020617) else Color.White
                ) {
                    Icon(Icons.Default.Add, contentDescription = stringResource(R.string.add_transaction))
                }
            },
            containerColor = appTheme.background
        ) { innerPadding ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Dashboard Cards
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Card(
                            modifier = Modifier.weight(1f),
                            colors = CardDefaults.cardColors(containerColor = appTheme.cardBackground),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(stringResource(R.string.total_balance), color = appTheme.secondaryTextColor, fontSize = 12.sp)
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    formatHuf(balance),
                                    color = appTheme.textColor,
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Card(
                            modifier = Modifier.weight(1f),
                            colors = CardDefaults.cardColors(containerColor = appTheme.cardBackground),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier.size(24.dp).background(Color(0xFF10B981).copy(alpha = 0.2f), RoundedCornerShape(8.dp)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(Icons.Default.ArrowDownward, contentDescription = null, tint = Color(0xFF10B981), modifier = Modifier.size(16.dp))
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(stringResource(R.string.total_income), color = appTheme.secondaryTextColor, fontSize = 11.sp)
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(formatHuf(totalIncome), color = Color(0xFF10B981), fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            }
                        }
                        Card(
                            modifier = Modifier.weight(1f),
                            colors = CardDefaults.cardColors(containerColor = appTheme.cardBackground),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier.size(24.dp).background(Color(0xFFF43F5E).copy(alpha = 0.2f), RoundedCornerShape(8.dp)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(Icons.Default.ArrowUpward, contentDescription = null, tint = Color(0xFFF43F5E), modifier = Modifier.size(16.dp))
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(stringResource(R.string.total_expenses), color = appTheme.secondaryTextColor, fontSize = 11.sp)
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(formatHuf(totalExpense), color = Color(0xFFF43F5E), fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            }
                        }
                    }
                }

                // Simple Chart showing expense distribution
                if (totalExpense > 0) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = appTheme.cardBackground),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text("Kiadások Eloszlása", color = appTheme.textColor, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.height(16.dp))

                                val expenseList = expenses.filter { it.type == "expense" }
                                val categoryTotals = expenseList.groupBy { it.category }.mapValues { it.value.sumOf { exp -> exp.amount } }.toList().sortedByDescending { it.second }

                                Row(
                                    modifier = Modifier.fillMaxWidth().height(8.dp).background(appTheme.outlineColor, RoundedCornerShape(4.dp))
                                ) {
                                    categoryTotals.forEach { (catName, amount) ->
                                        val weight = (amount / totalExpense).toFloat()
                                        val catData = dynamicCategories.find { it.name == catName }
                                        val catColor = try {
                                            if (catData != null) Color(android.graphics.Color.parseColor(catData.colorHex)) else Color.Gray
                                        } catch (e: Exception) { Color.Gray }

                                        if (weight > 0f) {
                                            Box(modifier = Modifier.weight(weight).fillMaxHeight().background(catColor))
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                categoryTotals.take(3).forEach { (catName, amount) ->
                                    val percentage = (amount / totalExpense * 100).toInt()
                                    val catData = dynamicCategories.find { it.name == catName }
                                    val catColor = try {
                                        if (catData != null) Color(android.graphics.Color.parseColor(catData.colorHex)) else Color.Gray
                                    } catch (e: Exception) { Color.Gray }

                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Box(modifier = Modifier.size(8.dp).background(catColor, RoundedCornerShape(4.dp)))
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(catName, color = appTheme.secondaryTextColor, fontSize = 12.sp)
                                        }
                                        Text("$percentage%", color = appTheme.textColor, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }

                item {
                    Text(
                        stringResource(R.string.transaction_history),
                        color = appTheme.textColor,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }

                // Search & Filter
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = { Text(stringResource(R.string.search_placeholder), color = appTheme.secondaryTextColor) },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = appTheme.secondaryTextColor) },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = appTheme.primary,
                                unfocusedBorderColor = appTheme.outlineColor,
                                focusedTextColor = appTheme.textColor,
                                unfocusedTextColor = appTheme.textColor
                            )
                        )
                        var filterExpanded by remember { mutableStateOf(false) }
                        Box {
                            Button(
                                onClick = { filterExpanded = true },
                                colors = ButtonDefaults.buttonColors(containerColor = appTheme.cardBackground),
                                modifier = Modifier.height(56.dp)
                            ) {
                                Text(typeFilter, color = appTheme.textColor)
                                Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = appTheme.textColor)
                            }
                            DropdownMenu(
                                expanded = filterExpanded,
                                onDismissRequest = { filterExpanded = false },
                                modifier = Modifier.background(appTheme.cardBackground)
                            ) {
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.filter_all), color = appTheme.textColor) },
                                    onClick = { typeFilter = "All"; filterExpanded = false }
                                )
                                DropdownMenuItem(
                                    text = { Text(incomeFilterName, color = appTheme.textColor) },
                                    onClick = { typeFilter = incomeFilterName; filterExpanded = false }
                                )
                                DropdownMenuItem(
                                    text = { Text(expenseFilterName, color = appTheme.textColor) },
                                    onClick = { typeFilter = expenseFilterName; filterExpanded = false }
                                )
                            }
                        }
                    }
                }

                // Transactions List
                val filteredExpenses = expenses.filter {
                    val matchesSearch = it.description.contains(searchQuery, ignoreCase = true) ||
                                        it.category.contains(searchQuery, ignoreCase = true)
                    val matchesType = when (typeFilter) {
                        incomeFilterName -> it.type == "income"
                        expenseFilterName -> it.type == "expense"
                        else -> true
                    }
                    matchesSearch && matchesType
                }

                if (filteredExpenses.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier.fillMaxWidth().padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(stringResource(R.string.no_recent_transactions), color = appTheme.secondaryTextColor)
                        }
                    }
                } else {
                    items(filteredExpenses) { expense ->
                        val catData = dynamicCategories.find { it.name.equals(expense.category, ignoreCase = true) }
                        val catColor = try {
                            if (catData != null) Color(android.graphics.Color.parseColor(catData.colorHex)) else Color.Gray
                        } catch (e: Exception) { Color.Gray }

                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = appTheme.cardBackground),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                    Box(
                                        modifier = Modifier.size(40.dp).background(catColor.copy(alpha = 0.15f), RoundedCornerShape(10.dp)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = if (expense.type == "income") Icons.Default.ArrowDownward else Icons.Default.ArrowUpward,
                                            contentDescription = null,
                                            tint = catColor
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text(expense.description, color = appTheme.textColor, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                        Text(expense.category + " • " + expense.date, color = appTheme.secondaryTextColor, fontSize = 12.sp)
                                        if (!expense.notes.isNullOrBlank()) {
                                            Text(expense.notes, color = appTheme.secondaryTextColor, fontSize = 11.sp, maxLines = 1)
                                        }
                                    }
                                }

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = if (expense.type == "income") "+${formatHuf(expense.amount)}" else "-${formatHuf(expense.amount)}",
                                        color = if (expense.type == "income") Color(0xFF10B981) else Color(0xFFF43F5E),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    IconButton(onClick = { transactionToDelete = expense }, modifier = Modifier.size(24.dp)) {
                                        Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.delete), tint = Color(0xFFF43F5E), modifier = Modifier.size(16.dp))
                                    }
                                }
                            }
                        }
                    }
                }

                item { Spacer(modifier = Modifier.height(80.dp)) }
            }
        }
    }

    if (showAddDialog) {
        AddTransactionDialog(
            onDismiss = { showAddDialog = false },
            onSave = { desc, amount, type, cat, date, notes ->
                viewModel.addExpense(desc, amount, type, cat, date, notes)
                showAddDialog = false
            },
            appTheme = appTheme,
            dynamicCategories = dynamicCategories
        )
    }

    // Delete Confirmation Dialog
    if (transactionToDelete != null) {
        AlertDialog(
            onDismissRequest = { transactionToDelete = null },
            title = { Text(stringResource(R.string.delete), fontWeight = FontWeight.Bold, color = appTheme.textColor) },
            text = { Text("Biztosan törölni szeretné ezt a tételt: ${transactionToDelete?.description}?", color = appTheme.textColor) },
            containerColor = appTheme.cardBackground,
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteExpense(transactionToDelete!!)
                        transactionToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF43F5E))
                ) {
                    Text(stringResource(R.string.delete), color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { transactionToDelete = null }) {
                    Text(stringResource(R.string.cancel), color = appTheme.secondaryTextColor)
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTransactionDialog(
    onDismiss: () -> Unit,
    onSave: (String, Double, String, String, String, String?) -> Unit,
    appTheme: AppTheme,
    dynamicCategories: List<CategoryData>
) {
    var description by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var type by remember { mutableStateOf("expense") } // "expense" or "income"
    var category by remember { mutableStateOf(dynamicCategories.firstOrNull()?.name ?: "Egyéb") }
    var notes by remember { mutableStateOf("") }

    val formatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    val date = formatter.format(Date())

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = appTheme.cardBackground,
        title = {
            Text(stringResource(R.string.new_transaction), fontWeight = FontWeight.Bold, color = appTheme.textColor)
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Type Selector (Income/Expense)
                Row(modifier = Modifier.fillMaxWidth()) {
                    Button(
                        onClick = { type = "expense" },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (type == "expense") Color(0xFFF43F5E) else appTheme.outlineColor
                        ),
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(topStart = 8.dp, bottomStart = 8.dp, topEnd = 0.dp, bottomEnd = 0.dp)
                    ) {
                        Text(stringResource(R.string.type_expense), color = if (type == "expense") Color.White else appTheme.textColor)
                    }
                    Button(
                        onClick = { type = "income" },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (type == "income") Color(0xFF10B981) else appTheme.outlineColor
                        ),
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(topStart = 0.dp, bottomStart = 0.dp, topEnd = 8.dp, bottomEnd = 8.dp)
                    ) {
                        Text(stringResource(R.string.type_income), color = if (type == "income") Color.White else appTheme.textColor)
                    }
                }

                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it },
                    label = { Text(stringResource(R.string.amount), color = appTheme.secondaryTextColor) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = appTheme.primary,
                        unfocusedBorderColor = appTheme.outlineColor,
                        focusedTextColor = appTheme.textColor,
                        unfocusedTextColor = appTheme.textColor
                    )
                )

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text(stringResource(R.string.description), color = appTheme.secondaryTextColor) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = appTheme.primary,
                        unfocusedBorderColor = appTheme.outlineColor,
                        focusedTextColor = appTheme.textColor,
                        unfocusedTextColor = appTheme.textColor
                    )
                )

                // Category Dropdown
                var expanded by remember { mutableStateOf(false) }
                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = category,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text(stringResource(R.string.category), color = appTheme.secondaryTextColor) },
                        modifier = Modifier.fillMaxWidth().clickable { expanded = true },
                        trailingIcon = { Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = appTheme.secondaryTextColor) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = appTheme.primary,
                            unfocusedBorderColor = appTheme.outlineColor,
                            focusedTextColor = appTheme.textColor,
                            unfocusedTextColor = appTheme.textColor,
                            disabledTextColor = appTheme.textColor,
                            disabledBorderColor = appTheme.outlineColor,
                            disabledLabelColor = appTheme.secondaryTextColor
                        ),
                        enabled = false
                    )
                    DropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false },
                        modifier = Modifier.background(appTheme.cardBackground)
                    ) {
                        dynamicCategories.forEach { cat ->
                            DropdownMenuItem(
                                text = { Text(cat.name, color = appTheme.textColor) },
                                onClick = {
                                    category = cat.name
                                    expanded = false
                                }
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text(stringResource(R.string.notes), color = appTheme.secondaryTextColor) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = appTheme.primary,
                        unfocusedBorderColor = appTheme.outlineColor,
                        focusedTextColor = appTheme.textColor,
                        unfocusedTextColor = appTheme.textColor
                    )
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amountDouble = amount.toDoubleOrNull()
                    if (description.isNotBlank() && amountDouble != null && amountDouble > 0) {
                        onSave(description, amountDouble, type, category, date, notes)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = appTheme.primary)
            ) {
                Text(stringResource(R.string.save), color = if (appTheme.isDark) Color(0xFF020617) else Color.White)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel), color = appTheme.secondaryTextColor)
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: ExpenseViewModel,
    appTheme: AppTheme,
    dynamicCategories: List<CategoryData>,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current

    var newCatName by remember { mutableStateOf("") }
    var newCatBudget by remember { mutableStateOf("") }
    var selectedColorHex by remember { mutableStateOf(PRESET_COLORS.first()) }

    var categoryToRename by remember { mutableStateOf<CategoryData?>(null) }
    var renameNewName by remember { mutableStateOf("") }
    var renameNewBudget by remember { mutableStateOf("") }

    var categoryToDelete by remember { mutableStateOf<CategoryData?>(null) }

    var showImportDialog by remember { mutableStateOf(false) }
    var importText by remember { mutableStateOf("") }

    var showResetConfirm by remember { mutableStateOf(false) }
    var showWipeConfirm by remember { mutableStateOf(false) }
    var isCategoriesExpanded by rememberSaveable { mutableStateOf(false) }

    val isSettingsCloudSyncEnabled by viewModel.isCloudSyncEnabled.collectAsState()
    val settingsLastSyncTime by viewModel.lastSyncTime.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings), fontWeight = FontWeight.Bold, color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = stringResource(R.string.cancel), tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = if (appTheme.isDark) appTheme.cardBackground else appTheme.primary
                )
            )
        },
        containerColor = appTheme.background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(12.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. Theme Selector Section
            Card(
                colors = CardDefaults.cardColors(containerColor = appTheme.cardBackground),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = stringResource(R.string.theme_selection),
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = appTheme.textColor
                    )
                    Spacer(modifier = Modifier.height(4.dp))

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        AppTheme.values().forEach { theme ->
                            val isSelected = theme.name == appTheme.name
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(
                                        if (isSelected) appTheme.primary.copy(alpha = 0.15f) else Color.Transparent,
                                        RoundedCornerShape(8.dp)
                                    )
                                    .clickable { viewModel.setTheme(theme.name) }
                                    .padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    // Theme color palette preview circles
                                    Box(modifier = Modifier.size(16.dp).background(theme.background, RoundedCornerShape(8.dp)))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Box(modifier = Modifier.size(16.dp).background(theme.cardBackground, RoundedCornerShape(8.dp)))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Box(modifier = Modifier.size(16.dp).background(theme.primary, RoundedCornerShape(8.dp)))
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(theme.displayName, color = appTheme.textColor, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                }
                                if (isSelected) {
                                    Icon(Icons.Default.Check, contentDescription = "Kiválasztva", tint = appTheme.primary, modifier = Modifier.size(18.dp))
                                }
                            }
                        }
                    }
                }
            }

            // 2. Category Management Section (New, Rename, Delete)
            Card(
                colors = CardDefaults.cardColors(containerColor = appTheme.cardBackground),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { isCategoriesExpanded = !isCategoriesExpanded }
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(R.string.category_edit_title),
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = appTheme.textColor
                        )
                        Icon(
                            if (isCategoriesExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                            contentDescription = "Lenyit",
                            tint = appTheme.textColor
                        )
                    }

                    if (isCategoriesExpanded) {
                        // Form to Add New Category
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(stringResource(R.string.add_new_category), fontSize = 11.sp, color = appTheme.secondaryTextColor, fontWeight = FontWeight.Bold)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = newCatName,
                                onValueChange = { newCatName = it },
                                placeholder = { Text(stringResource(R.string.category_name), fontSize = 12.sp, color = appTheme.secondaryTextColor) },
                                modifier = Modifier.weight(1.5f),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = appTheme.primary,
                                    unfocusedBorderColor = appTheme.outlineColor,
                                    focusedTextColor = appTheme.textColor,
                                    unfocusedTextColor = appTheme.textColor
                                )
                            )
                            OutlinedTextField(
                                value = newCatBudget,
                                onValueChange = { newCatBudget = it },
                                placeholder = { Text(stringResource(R.string.budget_huf), fontSize = 12.sp, color = appTheme.secondaryTextColor) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.weight(1f),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = appTheme.primary,
                                    unfocusedBorderColor = appTheme.outlineColor,
                                    focusedTextColor = appTheme.textColor,
                                    unfocusedTextColor = appTheme.textColor
                                )
                            )
                        }

                        // Preset Colors Row
                        Text(stringResource(R.string.choose_color), fontSize = 11.sp, color = appTheme.secondaryTextColor, fontWeight = FontWeight.Bold)
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            PRESET_COLORS.take(8).forEach { hex ->
                                val color = Color(android.graphics.Color.parseColor(hex))
                                Box(
                                    modifier = Modifier
                                        .size(24.dp)
                                        .background(color, RoundedCornerShape(12.dp))
                                        .clickable { selectedColorHex = hex }
                                        .padding(4.dp)
                                ) {
                                    if (selectedColorHex == hex) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .background(Color.White.copy(alpha = 0.6f), RoundedCornerShape(10.dp))
                                        )
                                    }
                                }
                            }
                        }

                        Button(
                            onClick = {
                                val budgetVal = newCatBudget.toDoubleOrNull() ?: 0.0
                                if (newCatName.isNotBlank()) {
                                    val success = viewModel.addCategory(newCatName, selectedColorHex, budgetVal)
                                    if (success) {
                                        Toast.makeText(context, context.getString(R.string.category_added), Toast.LENGTH_SHORT).show()
                                        newCatName = ""
                                        newCatBudget = ""
                                    } else {
                                        Toast.makeText(context, context.getString(R.string.category_add_failed), Toast.LENGTH_SHORT).show()
                                    }
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = appTheme.primary),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Hozzáadás", tint = if (appTheme.isDark) Color(0xFF020617) else Color.White)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Hozzáadás", color = if (appTheme.isDark) Color(0xFF020617) else Color.White)
                        }

                        Divider(color = appTheme.outlineColor, thickness = 1.dp)

                        // List of categories with Edit and Delete
                        Text("Létező kategóriák", fontSize = 11.sp, color = appTheme.secondaryTextColor, fontWeight = FontWeight.Bold)
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            dynamicCategories.forEach { cat ->
                                val isOthers = cat.name.equals("Others", ignoreCase = true)
                                val catColor = try {
                                    Color(android.graphics.Color.parseColor(cat.colorHex))
                                } catch (e: Exception) {
                                    Color.Gray
                                }

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(appTheme.background, RoundedCornerShape(8.dp))
                                        .padding(8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                        Box(modifier = Modifier.size(12.dp).background(catColor, RoundedCornerShape(6.dp)))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Column {
                                            Text(cat.name, color = appTheme.textColor, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                            if (cat.budget > 0) {
                                                Text("Keret: ${formatHuf(cat.budget)}", color = appTheme.secondaryTextColor, fontSize = 10.sp)
                                            }
                                        }
                                    }

                                    Row {
                                        IconButton(onClick = {
                                            categoryToRename = cat
                                            renameNewName = cat.name
                                            renameNewBudget = if (cat.budget > 0) cat.budget.toLong().toString() else ""
                                        }, modifier = Modifier.size(28.dp)) {
                                            Icon(Icons.Default.Edit, contentDescription = stringResource(R.string.rename), tint = appTheme.primary, modifier = Modifier.size(16.dp))
                                        }
                                        if (!isOthers) {
                                            IconButton(onClick = {
                                                categoryToDelete = cat
                                            }, modifier = Modifier.size(28.dp)) {
                                                Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.delete), tint = Color(0xFFF43F5E), modifier = Modifier.size(16.dp))
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        }
                    }
                }
            }

            // 2.5 Cloud Sync Section
            Card(
                colors = CardDefaults.cardColors(containerColor = appTheme.cardBackground),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(R.string.sync_cloud),
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = appTheme.textColor
                        )
                        Switch(
                            checked = isSettingsCloudSyncEnabled,
                            onCheckedChange = { viewModel.toggleCloudSync(it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = appTheme.primary,
                                uncheckedThumbColor = appTheme.secondaryTextColor,
                                uncheckedTrackColor = appTheme.background
                            )
                        )
                    }

                    if (isSettingsCloudSyncEnabled) {
                        Text(
                            text = if (settingsLastSyncTime.isNotEmpty()) stringResource(R.string.last_sync, settingsLastSyncTime) else stringResource(R.string.never_synced),
                            color = appTheme.secondaryTextColor,
                            fontSize = 12.sp
                        )
                        Button(
                            onClick = {
                                viewModel.syncNow { success ->
                                    val msg = if (success) context.getString(R.string.msg_sync_success) else context.getString(R.string.msg_sync_failed)
                                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = appTheme.primary),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.CloudSync, contentDescription = "Sync", modifier = Modifier.size(16.dp), tint = if (appTheme.isDark) Color(0xFF020617) else Color.White)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(stringResource(R.string.sync_now), color = if (appTheme.isDark) Color(0xFF020617) else Color.White)
                        }
                    } else {
                        Text(
                            text = "Csak helyi mentés",
                            color = appTheme.secondaryTextColor,
                            fontSize = 12.sp
                        )
                    }
                }
            }

            // 3. Backup and Restore Section
            Card(
                colors = CardDefaults.cardColors(containerColor = appTheme.cardBackground),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Biztonsági mentés és visszaállítás",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = appTheme.textColor
                    )
                    Spacer(modifier = Modifier.height(4.dp))

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Button(
                            onClick = {
                                val json = viewModel.exportBackup()
                                clipboardManager.setText(AnnotatedString(json))
                                Toast.makeText(context, "Mentés a vágólapra másolva!", Toast.LENGTH_LONG).show()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = appTheme.primary),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Exportálás", fontSize = 11.sp, color = if (appTheme.isDark) Color(0xFF020617) else Color.White)
                        }

                        Button(
                            onClick = {
                                importText = ""
                                showImportDialog = true
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF06B6D4)),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Importálás", fontSize = 11.sp, color = Color.White)
                        }
                    }
                }
            }

            // 4. Data Deletion / Factory Reset Section
            Card(
                colors = CardDefaults.cardColors(containerColor = appTheme.cardBackground),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Törlési lehetőség",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = appTheme.textColor
                    )
                    Spacer(modifier = Modifier.height(4.dp))

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Button(
                            onClick = { showWipeConfirm = true },
                            colors = ButtonDefaults.buttonColors(containerColor = appTheme.background),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(stringResource(R.string.delete_transactions), fontSize = 11.sp, color = appTheme.textColor)
                        }

                        Button(
                            onClick = { showResetConfirm = true },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF43F5E)),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(stringResource(R.string.reset_app), fontSize = 11.sp, color = Color.White)
                        }
                    }
                }
            }

            // 5. Version Card
            Card(
                colors = CardDefaults.cardColors(containerColor = appTheme.cardBackground),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.fillMaxWidth().padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(stringResource(R.string.version), color = appTheme.textColor, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Text(stringResource(R.string.developed_by), color = appTheme.secondaryTextColor, fontSize = 11.sp)
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }

    // Rename Category Dialog
    if (categoryToRename != null) {
        val cat = categoryToRename!!
        AlertDialog(
            onDismissRequest = { categoryToRename = null },
            title = { Text(stringResource(R.string.rename_category), fontWeight = FontWeight.Bold, color = appTheme.textColor) },
            containerColor = appTheme.cardBackground,
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(stringResource(R.string.old_name, cat.name), color = appTheme.secondaryTextColor, fontSize = 12.sp)
                    OutlinedTextField(
                        value = renameNewName,
                        onValueChange = { renameNewName = it },
                        label = { Text(stringResource(R.string.new_category_name), color = appTheme.secondaryTextColor) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = appTheme.primary,
                            unfocusedBorderColor = appTheme.outlineColor,
                            focusedTextColor = appTheme.textColor,
                            unfocusedTextColor = appTheme.textColor
                        )
                    )
                    OutlinedTextField(
                        value = renameNewBudget,
                        onValueChange = { renameNewBudget = it },
                        label = { Text(stringResource(R.string.budget_huf), color = appTheme.secondaryTextColor) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = appTheme.primary,
                            unfocusedBorderColor = appTheme.outlineColor,
                            focusedTextColor = appTheme.textColor,
                            unfocusedTextColor = appTheme.textColor
                        )
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (renameNewName.isNotBlank()) {
                            val budgetVal = renameNewBudget.toDoubleOrNull() ?: 0.0
                            val success = viewModel.renameCategory(cat.name, renameNewName) && viewModel.updateCategoryBudget(renameNewName, budgetVal)
                            if (success) {
                                Toast.makeText(context, context.getString(R.string.msg_category_renamed), Toast.LENGTH_SHORT).show()
                                categoryToRename = null
                            } else {
                                Toast.makeText(context, context.getString(R.string.category_add_failed), Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = appTheme.primary)
                ) {
                    Text(stringResource(R.string.save), color = if (appTheme.isDark) Color(0xFF020617) else Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { categoryToRename = null }) {
                    Text(stringResource(R.string.cancel), color = appTheme.secondaryTextColor)
                }
            }
        )
    }

    // Delete Category Confirmation Dialog
    if (categoryToDelete != null) {
        val cat = categoryToDelete!!
        AlertDialog(
            onDismissRequest = { categoryToDelete = null },
            title = { Text(stringResource(R.string.delete_category_title), fontWeight = FontWeight.Bold, color = appTheme.textColor) },
            containerColor = appTheme.cardBackground,
            text = {
                Text(stringResource(R.string.delete_category_message, cat.name), color = appTheme.textColor)
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteCategory(cat.name)
                        Toast.makeText(context, context.getString(R.string.msg_category_deleted), Toast.LENGTH_SHORT).show()
                        categoryToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF43F5E))
                ) {
                    Text(stringResource(R.string.delete), color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { categoryToDelete = null }) {
                    Text(stringResource(R.string.cancel), color = appTheme.secondaryTextColor)
                }
            }
        )
    }

    // Import Backup Dialog
    if (showImportDialog) {
        AlertDialog(
            onDismissRequest = { showImportDialog = false },
            title = { Text("Mentés visszaállítása", fontWeight = FontWeight.Bold, color = appTheme.textColor) },
            containerColor = appTheme.cardBackground,
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Illessze be az exportált biztonsági mentés JSON szövegét:", color = appTheme.secondaryTextColor, fontSize = 12.sp)
                    OutlinedTextField(
                        value = importText,
                        onValueChange = { importText = it },
                        placeholder = { Text("Paste JSON here...", color = appTheme.secondaryTextColor) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = appTheme.primary,
                            unfocusedBorderColor = appTheme.outlineColor,
                            focusedTextColor = appTheme.textColor,
                            unfocusedTextColor = appTheme.textColor
                        )
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (importText.isNotBlank()) {
                            val success = viewModel.importBackup(importText)
                            if (success) {
                                Toast.makeText(context, context.getString(R.string.msg_sync_success), Toast.LENGTH_LONG).show()
                                showImportDialog = false
                            } else {
                                Toast.makeText(context, context.getString(R.string.msg_sync_failed), Toast.LENGTH_LONG).show()
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = appTheme.primary)
                ) {
                    Text(stringResource(R.string.restore_action), color = if (appTheme.isDark) Color(0xFF020617) else Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showImportDialog = false }) {
                    Text(stringResource(R.string.cancel), color = appTheme.secondaryTextColor)
                }
            }
        )
    }

    // Reset Confirm Dialog
    if (showResetConfirm) {
        AlertDialog(
            onDismissRequest = { showResetConfirm = false },
            title = { Text(stringResource(R.string.confirm_reset_title), fontWeight = FontWeight.Bold, color = appTheme.textColor) },
            containerColor = appTheme.cardBackground,
            text = {
                Text("Biztosan alaphelyzetbe szeretné állítani az alkalmazást?\n\nEz törli az összes tranzakciót és visszaállítja az alapértelmezett kategóriákat és témát.", color = appTheme.textColor)
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.resetToDefaults()
                        Toast.makeText(context, context.getString(R.string.msg_reset_done), Toast.LENGTH_SHORT).show()
                        showResetConfirm = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF43F5E))
                ) {
                    Text(stringResource(R.string.restore_action), color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetConfirm = false }) {
                    Text(stringResource(R.string.cancel), color = appTheme.secondaryTextColor)
                }
            }
        )
    }

    // Wipe Transactions Confirm Dialog
    if (showWipeConfirm) {
        AlertDialog(
            onDismissRequest = { showWipeConfirm = false },
            title = { Text("Tranzakciók törlése", fontWeight = FontWeight.Bold, color = appTheme.textColor) },
            containerColor = appTheme.cardBackground,
            text = {
                Text("Biztosan törölni szeretné az összes tranzakciót?", color = appTheme.textColor)
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.clearAll()
                        Toast.makeText(context, context.getString(R.string.msg_all_deleted), Toast.LENGTH_SHORT).show()
                        showWipeConfirm = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF43F5E))
                ) {
                    Text(stringResource(R.string.delete), color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showWipeConfirm = false }) {
                    Text(stringResource(R.string.cancel), color = appTheme.secondaryTextColor)
                }
            }
        )
    }
}
