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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.stringResource
import com.example.expensetracker.R
import com.example.expensetracker.data.ExpenseEntity
import java.text.NumberFormat
import java.util.*

enum class AppTheme(
    val displayName: String,
    val isDark: Boolean,
    val background: Color,
    val cardBackground: Color,
    val primary: Color,
    val textColor: Color,
    val secondaryTextColor: Color,
    val outlineColor: Color
) {
    SLATE_DARK(
        displayName = "Sötét pala",
        isDark = true,
        background = Color(0xFF020617),
        cardBackground = Color(0xFF0F172A),
        primary = Color(0xFF10B981),
        textColor = Color.White,
        secondaryTextColor = Color(0xFF94A3B8),
        outlineColor = Color(0xFF1E293B)
    ),
    LIGHT_THEME(
        displayName = "Világos téma",
        isDark = false,
        background = Color(0xFFF8FAFC),
        cardBackground = Color.White,
        primary = Color(0xFF059669),
        textColor = Color(0xFF0F172A),
        secondaryTextColor = Color(0xFF475569),
        outlineColor = Color(0xFFE2E8F0)
    ),
    EMERALD_DARK(
        displayName = "Smaragdzöld sötét",
        isDark = true,
        background = Color(0xFF064E3B),
        cardBackground = Color(0xFF065F46),
        primary = Color(0xFF34D399),
        textColor = Color.White,
        secondaryTextColor = Color(0xFFD1FAE5),
        outlineColor = Color(0xFF047857)
    ),
    OCEAN_DARK(
        displayName = "Óceánkék sötét",
        isDark = true,
        background = Color(0xFF0B192C),
        cardBackground = Color(0xFF1E3E62),
        primary = Color(0xFF00D2C4),
        textColor = Color.White,
        secondaryTextColor = Color(0xFFE2F1E7),
        outlineColor = Color(0xFF3F72AF)
    )
}

data class CategoryInfo(val name: String, val color: Color, val budget: Double = 0.0)

val PRESET_COLORS = listOf(
    "#F43F5E", "#EAB308", "#3B82F6", "#A855F7", "#6366F1", "#F97316", "#14B8A6", "#10B981", "#06B6D4", "#6B7280"
)

fun getCategoryColor(categoryName: String, dynamicCategories: List<CategoryData>): Color {
    val cat = dynamicCategories.find { it.name.equals(categoryName, ignoreCase = true) }
    if (cat != null) {
        return try {
            Color(android.graphics.Color.parseColor(cat.colorHex))
        } catch (e: Exception) {
            Color.Gray
        }
    }
    return Color.Gray
}

fun formatHuf(amount: Double): String {
    val format = NumberFormat.getCurrencyInstance(Locale("hu", "HU"))
    format.maximumFractionDigits = 0
    return format.format(amount)
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
                        IconButton(onClick = { showClearAllConfirm = true }) {
                            Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.delete_transactions), tint = Color(0xFFF43F5E))
                        }
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
            containerColor = appTheme.background,
            modifier = modifier
        ) { innerPadding ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 12.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 1. KPI Cards
                item {
                    StatsSection(balance, totalIncome, totalExpense, savingsRate, appTheme)
                }

                // 2. Charts Section (Donut Spending + Balance Trend)
                item {
                    ChartsSection(expenses, dynamicCategories, appTheme)
                }

                // 3. Budgets & Limits Section
                item {
                    BudgetLimitsSection(expenses, dynamicCategories, appTheme)
                }

                // 4. Filters & List Header
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = stringResource(R.string.transaction_history),
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = appTheme.textColor
                        )

                        // Search Bar
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = { Text(stringResource(R.string.search_placeholder), fontSize = 12.sp, color = appTheme.secondaryTextColor) },
                            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(16.dp), tint = appTheme.secondaryTextColor) },
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = appTheme.primary,
                                unfocusedBorderColor = appTheme.outlineColor,
                                focusedContainerColor = appTheme.cardBackground,
                                unfocusedContainerColor = appTheme.cardBackground,
                                focusedTextColor = appTheme.textColor,
                                unfocusedTextColor = appTheme.textColor
                            ),
                            shape = RoundedCornerShape(12.dp)
                        )

                        // Type filter Row
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            listOf("Minden", stringResource(R.string.type_expense), stringResource(R.string.type_income)).forEach { filter ->
                                val isSelected = typeFilter == filter
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .background(
                                            if (isSelected) appTheme.primary else appTheme.cardBackground,
                                            shape = RoundedCornerShape(8.dp)
                                        )
                                        .clickable { typeFilter = filter }
                                        .padding(vertical = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = filter,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) (if (appTheme.isDark) Color(0xFF020617) else Color.White) else appTheme.secondaryTextColor
                                    )
                                }
                            }
                        }
                    }
                }

                // 5. Transaction list items
                val filteredList = expenses.filter { tx ->
                    val matchesSearch = tx.description.contains(searchQuery, ignoreCase = true) ||
                            (tx.notes != null && tx.notes.contains(searchQuery, ignoreCase = true))
                    val matchesType = when (typeFilter) {
                        incomeFilterName, "Income", "Bevétel" -> tx.type == "income"
                        expenseFilterName, "Expense", "Kiadás" -> tx.type == "expense"
                        else -> true
                    }
                    matchesSearch && matchesType
                }

                if (filteredList.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(stringResource(R.string.no_recent_transactions), color = appTheme.secondaryTextColor, fontSize = 13.sp)
                        }
                    }
                } else {
                    items(filteredList) { tx ->
                        TransactionItem(
                            tx = tx,
                            onDelete = { transactionToDelete = tx },
                            dynamicCategories = dynamicCategories,
                            appTheme = appTheme
                        )
                    }
                }

                // Spacer bottom
                item {
                    Spacer(modifier = Modifier.height(80.dp))
                }
            }
        }
    }

    // Single Transaction Delete Confirmation Dialog
    if (transactionToDelete != null) {
        AlertDialog(
            onDismissRequest = { transactionToDelete = null },
            title = { Text("Tranzakció törlése", fontWeight = FontWeight.Bold, color = appTheme.textColor) },
            text = { Text("Biztosan törölni szeretné ezt a tranzakciót?", color = appTheme.textColor) },
            containerColor = appTheme.cardBackground,
            confirmButton = {
                Button(
                    onClick = {
                        transactionToDelete?.let { viewModel.deleteExpense(it) }
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

    // Clear All Confirmation Dialog
    if (showClearAllConfirm) {
        AlertDialog(
            onDismissRequest = { showClearAllConfirm = false },
            title = { Text("Összes tranzakció törlése", fontWeight = FontWeight.Bold, color = appTheme.textColor) },
            text = { Text("Biztosan törölni szeretné az összes tranzakciót? Ez a folyamat nem vonható vissza.", color = appTheme.textColor) },
            containerColor = appTheme.cardBackground,
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.clearAll()
                        showClearAllConfirm = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF43F5E))
                ) {
                    Text(stringResource(R.string.delete_transactions), color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearAllConfirm = false }) {
                    Text(stringResource(R.string.cancel), color = appTheme.secondaryTextColor)
                }
            }
        )
    }

    // Add Transaction Dialog
    if (showAddDialog) {
        AddTransactionDialog(
            onDismiss = { showAddDialog = false },
            dynamicCategories = dynamicCategories,
            appTheme = appTheme,
            onConfirm = { desc, amount, type, category, date, notes ->
                viewModel.addExpense(desc, amount, type, category, date, notes)
                showAddDialog = false
            }
        )
    }
}

@Composable
fun StatsSection(balance: Double, income: Double, expense: Double, savingsRate: Double, appTheme: AppTheme) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        // Main balance
        Card(
            colors = CardDefaults.cardColors(containerColor = appTheme.cardBackground),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(stringResource(R.string.total_balance), color = appTheme.secondaryTextColor, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = formatHuf(balance),
                    fontWeight = FontWeight.Black,
                    style = MaterialTheme.typography.headlineSmall,
                    color = if (balance >= 0) Color(0xFF10B981) else Color(0xFFF43F5E)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(stringResource(R.string.total_balance), color = appTheme.secondaryTextColor, fontSize = 10.sp)
            }
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            // Income card
            Card(
                colors = CardDefaults.cardColors(containerColor = appTheme.cardBackground),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.weight(1f)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(stringResource(R.string.total_income), color = appTheme.secondaryTextColor, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(formatHuf(income), fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color(0xFF10B981))
                }
            }

            // Expense card
            Card(
                colors = CardDefaults.cardColors(containerColor = appTheme.cardBackground),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.weight(1f)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(stringResource(R.string.total_expenses), color = appTheme.secondaryTextColor, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(formatHuf(expense), fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color(0xFFF43F5E))
                }
            }
        }

        // Savings Rate card
        Card(
            colors = CardDefaults.cardColors(containerColor = appTheme.cardBackground),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(stringResource(R.string.savings_rate), color = appTheme.secondaryTextColor, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Text(String.format("%.1f%%", savingsRate), color = Color(0xFF06B6D4), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
                Spacer(modifier = Modifier.height(6.dp))
                LinearProgressIndicator(
                    progress = (savingsRate / 100f).toFloat().coerceIn(0f, 1f),
                    modifier = Modifier.fillMaxWidth(),
                    color = Color(0xFF06B6D4),
                    trackColor = appTheme.background
                )
            }
        }
    }
}

@Composable
fun ChartsSection(expenses: List<ExpenseEntity>, dynamicCategories: List<CategoryData>, appTheme: AppTheme) {
    val expenseList = expenses.filter { it.type == "expense" }
    if (expenseList.isEmpty()) return

    // Group expenses by category
    val categoryTotals = expenseList.groupBy { it.category }
        .mapValues { entry -> entry.value.sumOf { it.amount } }
        .toList()
        .sortedByDescending { it.second }

    val totalSpent = expenseList.sumOf { it.amount }

    Card(
        colors = CardDefaults.cardColors(containerColor = appTheme.cardBackground),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(stringResource(R.string.filter_expense), fontWeight = FontWeight.Bold, fontSize = 14.sp, color = appTheme.textColor)
            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Donut Chart Canvas (Simple Vector drawing)
                Canvas(
                    modifier = Modifier
                        .size(100.dp)
                        .padding(8.dp)
                ) {
                    var startAngle = 0f
                    categoryTotals.forEach { (catName, sum) ->
                        val catColor = getCategoryColor(catName, dynamicCategories)
                        val sweep = (sum / totalSpent * 360f).toFloat()
                        drawArc(
                            color = catColor,
                            startAngle = startAngle,
                            sweepAngle = sweep,
                            useCenter = false,
                            style = Stroke(width = 24f)
                        )
                        startAngle += sweep
                    }
                }

                Spacer(modifier = Modifier.width(16.dp))

                // Legend
                Column(
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    categoryTotals.take(4).forEach { (catName, sum) ->
                        val catColor = getCategoryColor(catName, dynamicCategories)
                        val percentage = (sum / totalSpent * 100)
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .background(catColor, RoundedCornerShape(4.dp))
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = catName,
                                    fontSize = 11.sp,
                                    color = appTheme.secondaryTextColor,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Text(
                                text = String.format("%.0f%%", percentage),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = appTheme.textColor,
                                modifier = Modifier.padding(4.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun BudgetLimitsSection(expenses: List<ExpenseEntity>, dynamicCategories: List<CategoryData>, appTheme: AppTheme) {
    val expenseList = expenses.filter { it.type == "expense" }
    val categoriesWithBudgets = dynamicCategories.filter { it.budget > 0 }

    if (categoriesWithBudgets.isEmpty()) return

    Card(
        colors = CardDefaults.cardColors(containerColor = appTheme.cardBackground),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(stringResource(R.string.manage_categories), fontWeight = FontWeight.Bold, fontSize = 14.sp, color = appTheme.textColor)

            categoriesWithBudgets.forEach { cat ->
                val spent = expenseList.filter { it.category.equals(cat.name, ignoreCase = true) }.sumOf { it.amount }
                val progress = (spent / cat.budget).toFloat()
                val isOver = spent > cat.budget

                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(cat.name, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = appTheme.secondaryTextColor)
                        Text(
                            text = "${formatHuf(spent)} / ${formatHuf(cat.budget)}",
                            fontSize = 11.sp,
                            color = if (isOver) Color(0xFFF43F5E) else appTheme.textColor,
                            fontWeight = if (isOver) FontWeight.Bold else FontWeight.Normal
                        )
                    }

                    LinearProgressIndicator(
                        progress = progress.coerceIn(0f, 1f),
                        modifier = Modifier.fillMaxWidth(),
                        color = if (isOver) Color(0xFFF43F5E) else if (progress > 0.85) Color(0xFFEAB308) else Color(0xFF10B981),
                        trackColor = appTheme.background
                    )
                }
            }
        }
    }
}

@Composable
fun TransactionItem(tx: ExpenseEntity, onDelete: () -> Unit, dynamicCategories: List<CategoryData>, appTheme: AppTheme) {
    val catColor = getCategoryColor(tx.category, dynamicCategories)

    Card(
        colors = CardDefaults.cardColors(containerColor = appTheme.cardBackground),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                // Category accent line/dot
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .background(catColor, RoundedCornerShape(6.dp))
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = tx.description,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = appTheme.textColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(tx.category, fontSize = 10.sp, color = appTheme.secondaryTextColor)
                        Text(tx.date, fontSize = 10.sp, color = appTheme.secondaryTextColor)
                    }
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "${if (tx.type == "income") "+" else "-"}${formatHuf(tx.amount)}",
                    fontWeight = FontWeight.Black,
                    fontSize = 14.sp,
                    color = if (tx.type == "income") Color(0xFF10B981) else Color(0xFFF43F5E)
                )
                Spacer(modifier = Modifier.width(8.dp))
                IconButton(onClick = onDelete, modifier = Modifier.size(24.dp)) {
                    Icon(
                        Icons.Default.Clear,
                        contentDescription = stringResource(R.string.delete),
                        tint = Color.Gray,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTransactionDialog(
    onDismiss: () -> Unit,
    dynamicCategories: List<CategoryData>,
    appTheme: AppTheme,
    onConfirm: (description: String, amount: Double, type: String, category: String, date: String, notes: String?) -> Unit
) {
    var description by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var type by remember { mutableStateOf("expense") } // "expense" or "income"
    var category by remember { mutableStateOf("Others") }
    var notes by remember { mutableStateOf("") }

    val date = remember {
        val sdf = java.text.SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        sdf.format(Date())
    }

    val salaryStr = stringResource(R.string.cat_salary)
    val investmentsStr = stringResource(R.string.cat_investments)
    val othersStr = stringResource(R.string.cat_others)
    val filteredCategories = remember(type, dynamicCategories, salaryStr, investmentsStr, othersStr) {
        if (type == "income") {
            dynamicCategories.filter { it.name in listOf(salaryStr, investmentsStr, othersStr) || it.budget <= 0 }
        } else {
            dynamicCategories.filter { it.name !in listOf(salaryStr, investmentsStr) }
        }
    }

    // Set valid category on type toggle
    LaunchedEffect(filteredCategories) {
        if (category !in filteredCategories.map { it.name }) {
            category = filteredCategories.firstOrNull()?.name ?: "Others"
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.new_transaction), fontWeight = FontWeight.Bold, color = appTheme.textColor) },
        containerColor = appTheme.cardBackground,
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                // Type Toggles
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Button(
                        onClick = { type = "expense" },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (type == "expense") Color(0xFFF43F5E) else appTheme.background
                        ),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(stringResource(R.string.type_expense), color = Color.White)
                    }

                    Button(
                        onClick = { type = "income" },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (type == "income") Color(0xFF10B981) else appTheme.background
                        ),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(stringResource(R.string.type_income), color = Color.White)
                    }
                }

                // Description Input
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text(stringResource(R.string.description), color = appTheme.secondaryTextColor) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = appTheme.primary,
                        unfocusedBorderColor = appTheme.outlineColor,
                        focusedTextColor = appTheme.textColor,
                        unfocusedTextColor = appTheme.textColor
                    )
                )

                // Amount Input
                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it },
                    label = { Text(stringResource(R.string.amount), color = appTheme.secondaryTextColor) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = appTheme.primary,
                        unfocusedBorderColor = appTheme.outlineColor,
                        focusedTextColor = appTheme.textColor,
                        unfocusedTextColor = appTheme.textColor
                    )
                )

                // Category selection dropdown
                Text(stringResource(R.string.category), fontSize = 11.sp, color = appTheme.secondaryTextColor, fontWeight = FontWeight.Bold)
                Box(
                    modifier = Modifier
                        .height(110.dp)
                        .fillMaxWidth()
                        .background(appTheme.background, RoundedCornerShape(8.dp))
                        .padding(4.dp)
                ) {
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        items(filteredCategories) { cat ->
                            val catColor = try {
                                Color(android.graphics.Color.parseColor(cat.colorHex))
                            } catch (e: Exception) {
                                Color.Gray
                            }
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { category = cat.name }
                                    .background(if (category == cat.name) appTheme.primary.copy(alpha = 0.2f) else Color.Transparent, RoundedCornerShape(4.dp))
                                    .padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .background(catColor, RoundedCornerShape(5.dp))
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(cat.name, color = appTheme.textColor, fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amtDouble = amount.toDoubleOrNull() ?: 0.0
                    if (description.isNotBlank() && amtDouble > 0) {
                        onConfirm(description, amtDouble, type, category, date, notes.ifBlank { null })
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

    var categoryToDelete by remember { mutableStateOf<CategoryData?>(null) }

    var showImportDialog by remember { mutableStateOf(false) }
    var importText by remember { mutableStateOf("") }

    var showResetConfirm by remember { mutableStateOf(false) }
    var showWipeConfirm by remember { mutableStateOf(false) }

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
                    Text(
                        text = stringResource(R.string.category_edit_title),
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = appTheme.textColor
                    )

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
                            Text("Hozzáadás", color = if (appTheme.isDark) Color(0xFF020617) else Color.White)
                        }
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
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF43F5E)),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Tranzakciók törlése", fontSize = 11.sp, color = Color.White)
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
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (renameNewName.isNotBlank()) {
                            val success = viewModel.renameCategory(cat.name, renameNewName)
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
