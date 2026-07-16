package com.example.expensetracker.ui

import androidx.compose.animation.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.expensetracker.data.ExpenseEntity
import java.text.NumberFormat
import java.util.*

// Hardcoded categories to match the React Web App
data class CategoryInfo(val name: String, val color: Color, val budget: Double = 0.0)

val APP_CATEGORIES = listOf(
    CategoryInfo("Food & Dining", Color(0xFFF43F5E), 150000.0),
    CategoryInfo("Shopping", Color(0xFFEAB308), 80000.0),
    CategoryInfo("Housing & Rent", Color(0xFF3B82F6), 250000.0),
    CategoryInfo("Transportation", Color(0xFFA855F7), 60000.0),
    CategoryInfo("Entertainment", Color(0xFF6366F1), 50000.0),
    CategoryInfo("Utilities", Color(0xFFF97316), 70000.0),
    CategoryInfo("Healthcare", Color(0xFF14B8A6), 40000.0),
    CategoryInfo("Salary", Color(0xFF10B981)),
    CategoryInfo("Investments", Color(0xFF06B6D4)),
    CategoryInfo("Others", Color(0xFF6B7280), 30000.0)
)

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

    var showAddDialog by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var typeFilter by remember { mutableStateOf("Minden") } // "Minden", "Bevétel", "Kiadás"

    val totalIncome = expenses.filter { it.type == "income" }.sumOf { it.amount }
    val totalExpense = expenses.filter { it.type == "expense" }.sumOf { it.amount }
    val balance = totalIncome - totalExpense
    val savingsRate = if (totalIncome > 0) ((totalIncome - totalExpense) / totalIncome * 100).coerceAtLeast(0.0) else 0.0

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "HES Költségkövető",
                            fontWeight = FontWeight.Black,
                            fontSize = 20.sp,
                            color = Color.White
                        )
                        Text(
                            text = "Helyi pénzügyi nyilvántartás",
                            fontSize = 11.sp,
                            color = Color.LightGray
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.clearAll() }) {
                        Icon(Icons.Default.Delete, contentDescription = "Összes törlése", tint = Color(0xFFF43F5E))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF020617)
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = Color(0xFF10B981),
                contentColor = Color(0xFF020617)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Új hozzáadása")
            }
        },
        containerColor = Color(0xFF020617),
        modifier = modifier
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. KPI Cards
            item {
                StatsSection(balance, totalIncome, totalExpense, savingsRate)
            }

            // 2. Charts Section (Donut Spending + Balance Trend)
            item {
                ChartsSection(expenses)
            }

            // 3. Budgets & Limits Section
            item {
                BudgetLimitsSection(expenses)
            }

            // 4. Filters & List Header
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Tranzakciók Előzménye",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = Color.White
                    )
                    
                    // Search Bar
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Keresés...", fontSize = 12.sp) },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, size = 16.dp) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF10B981),
                            unfocusedBorderColor = Color(0xFF1E293B),
                            focusedContainerColor = Color(0xFF0F172A),
                            unfocusedContainerColor = Color(0xFF0F172A)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    )

                    // Type filter Row
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        listOf("Minden", "Kiadás", "Bevétel").forEach { filter ->
                            val isSelected = typeFilter == filter
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .background(
                                        if (isSelected) Color(0xFF10B981) else Color(0xFF0F172A),
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
                                    color = if (isSelected) Color(0xFF020617) else Color.LightGray
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
                    "Bevétel" -> tx.type == "income"
                    "Kiadás" -> tx.type == "expense"
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
                        Text("Nincs a szűrésnek megfelelő tranzakció", color = Color.Gray, fontSize = 13.sp)
                    }
                }
            } else {
                items(filteredList) { tx ->
                    TransactionItem(tx = tx, onDelete = { viewModel.deleteExpense(tx) })
                }
            }

            // Spacer bottom
            item {
                Spacer(modifier = Modifier.height(80.dp))
            }
        }
    }

    // Add Transaction Dialog
    if (showAddDialog) {
        AddTransactionDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { desc, amount, type, category, date, notes ->
                viewModel.addExpense(desc, amount, type, category, date, notes)
                showAddDialog = false
            }
        )
    }
}

@Composable
fun StatsSection(balance: Double, income: Double, expense: Double, savingsRate: Double) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        // Main balance
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Aktuális Egyenleg", color = Color.Gray, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = formatHuf(balance),
                    fontWeight = FontWeight.Black,
                    fontSize = 24.sp,
                    color = if (balance >= 0) Color(0xFF10B981) else Color(0xFFF43F5E)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text("Összes bevétel és kiadás különbsége", color = Color.Gray, fontSize = 10.sp)
            }
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            // Income card
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.weight(1f)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text("Összes Bevétel", color = Color.Gray, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(formatHuf(income), fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color(0xFF10B981))
                }
            }

            // Expense card
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.weight(1f)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text("Összes Kiadás", color = Color.Gray, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(formatHuf(expense), fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color(0xFFF43F5E))
                }
            }
        }

        // Savings Rate card
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Megtakarítási Ráta", color = Color.Gray, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Text(String.format("%.1f%%", savingsRate), color = Color(0xFF06B6D4), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
                Spacer(modifier = Modifier.height(6.dp))
                LinearProgressIndicator(
                    progress = { (savingsRate / 100f).toFloat().coerceIn(0f, 1f) },
                    modifier = Modifier.fillMaxWidth(),
                    color = Color(0xFF06B6D4),
                    trackColor = Color(0xFF020617)
                )
            }
        }
    }
}

@Composable
fun ChartsSection(expenses: List<ExpenseEntity>) {
    val expenseList = expenses.filter { it.type == "expense" }
    if (expenseList.isEmpty()) return

    // Group expenses by category
    val categoryTotals = expenseList.groupBy { it.category }
        .mapValues { entry -> entry.value.sumOf { it.amount } }
        .toList()
        .sortedByDescending { it.second }

    val totalSpent = expenseList.sumOf { it.amount }

    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Kiadások Megoszlása", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color.White)
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
                        val catColor = APP_CATEGORIES.find { it.name == catName }?.color ?: Color.Gray
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
                        val catColor = APP_CATEGORIES.find { it.name == catName }?.color ?: Color.Gray
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
                                    color = Color.LightGray,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Text(
                                text = String.format("%.0f%%", percentage),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
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
fun BudgetLimitsSection(expenses: List<ExpenseEntity>) {
    val expenseList = expenses.filter { it.type == "expense" }
    val categoriesWithBudgets = APP_CATEGORIES.filter { it.budget > 0 }

    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Költségkeretek & Limitek", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color.White)

            categoriesWithBudgets.forEach { cat ->
                val spent = expenseList.filter { it.category == cat.name }.sumOf { it.amount }
                val progress = (spent / cat.budget).toFloat()
                val isOver = spent > cat.budget

                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(cat.name, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.LightGray)
                        Text(
                            text = "${formatHuf(spent)} / ${formatHuf(cat.budget)}",
                            fontSize = 11.sp,
                            color = if (isOver) Color(0xFFF43F5E) else Color.White,
                            fontWeight = if (isOver) FontWeight.Bold else FontWeight.Normal
                        )
                    }

                    LinearProgressIndicator(
                        progress = { progress.coerceIn(0f, 1f) },
                        modifier = Modifier.fillMaxWidth(),
                        color = if (isOver) Color(0xFFF43F5E) else if (progress > 0.85) Color(0xFFEAB308) else Color(0xFF10B981),
                        trackColor = Color(0xFF020617)
                    )
                }
            }
        }
    }
}

@Composable
fun TransactionItem(tx: ExpenseEntity, onDelete: () -> Unit) {
    val catColor = APP_CATEGORIES.find { it.name == tx.category }?.color ?: Color.Gray

    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
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
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(tx.category, fontSize = 10.sp, color = Color.LightGray)
                        Text(tx.date, fontSize = 10.sp, color = Color.Gray)
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
                        contentDescription = "Törlés",
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
    onConfirm: (description: String, amount: Double, type: String, category: String, date: String, notes: String?) -> Unit
) {
    var description by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var type by remember { mutableStateOf("expense") } // "expense" or "income"
    var category by remember { mutableStateOf("Food & Dining") }
    var notes by remember { mutableStateOf("") }

    val date = remember {
        val sdf = java.text.SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        sdf.format(Date())
    }

    val filteredCategories = if (type == "income") {
        listOf("Salary", "Investments", "Others")
    } else {
        listOf("Food & Dining", "Shopping", "Housing & Rent", "Transportation", "Entertainment", "Utilities", "Healthcare", "Others")
    }

    // Set valid category on type toggle
    LaunchedEffect(type) {
        if (type == "income" && category !in listOf("Salary", "Investments", "Others")) {
            category = "Salary"
        } else if (type == "expense" && category in listOf("Salary", "Investments")) {
            category = "Food & Dining"
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Új Tranzakció", fontWeight = FontWeight.Bold, color = Color.White) },
        containerColor = Color(0xFF0F172A),
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
                            containerColor = if (type == "expense") Color(0xFFF43F5E) else Color(0xFF020617)
                        ),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Kiadás")
                    }

                    Button(
                        onClick = { type = "income" },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (type == "income") Color(0xFF10B981) else Color(0xFF020617)
                        ),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Bevétel")
                    }
                }

                // Description Input
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Megnevezés") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF10B981),
                        unfocusedBorderColor = Color(0xFF1E293B)
                    )
                )

                // Amount Input
                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it },
                    label = { Text("Összeg (HUF)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF10B981),
                        unfocusedBorderColor = Color(0xFF1E293B)
                    )
                )

                // Category selection dropdown
                Text("Kategória választás", fontSize = 11.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // Safe categories wrap
                    LazyColumn(modifier = Modifier.height(100.dp).fillMaxWidth()) {
                        items(filteredCategories) { cat ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { category = cat }
                                    .background(if (category == cat) Color(0xFF10B981).copy(alpha = 0.2f) else Color.Transparent)
                                    .padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .background(APP_CATEGORIES.find { it.name == cat }?.color ?: Color.Gray, RoundedCornerShape(5.dp))
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(cat, color = Color.White, fontSize = 12.sp)
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
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981))
            ) {
                Text("Mentés", color = Color(0xFF020617))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Mégse", color = Color.LightGray)
            }
        }
    )
}
