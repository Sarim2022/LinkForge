package com.example.linkforge.Screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.size
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import com.example.linkforge.R
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.Calendar
import java.util.Locale
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.suspendCancellableCoroutine

data class MonthlyBudget(
    val totalBudget: Double,
    val categories: Map<String, Double>
)

data class CategoryBudgetInput(
    var categoryName: String = "",
    var amountText: String = ""
)

data class MonthOption(
    val id: String,
    val label: String
)

data class MonthlySpentData(
    val totalSpent: Double,
    val categorySpent: Map<String, Double>
)

private object BudgetFirestoreSchema {
    const val USERS = "users"
    const val BUDGETS = "budgets"
    const val TOTAL_BUDGET = "totalBudget"
    const val CATEGORIES = "categories"
    val MONTH_ID_REGEX = Regex("^\\d{4}-(0[1-9]|1[0-2])$")
}

private val defaultBudgetCategories = listOf(
    "Food", "Groceries", "Transport", "Rent", "Bills",
    "Shopping", "Entertainment", "Health", "Travel", "Other"
)

private suspend fun Task<Void>.awaitVoid() = suspendCancellableCoroutine { cont ->
    addOnCompleteListener { task ->
        val error = task.exception
        if (error != null) {
            cont.resumeWithException(error)
        } else {
            cont.resume(Unit)
        }
    }
}

private suspend fun <T> Task<T>.awaitTask(): T = suspendCancellableCoroutine { cont ->
    addOnCompleteListener { task ->
        val error = task.exception
        if (error != null) {
            cont.resumeWithException(error)
        } else {
            @Suppress("UNCHECKED_CAST")
            cont.resume(task.result as T)
        }
    }
}

private fun validateMonthId(monthId: String) {
    require(BudgetFirestoreSchema.MONTH_ID_REGEX.matches(monthId)) {
        "monthId must be in YYYY-MM format."
    }
}

private fun validateCategoryName(categoryName: String) {
    require(categoryName.isNotBlank()) { "categoryName cannot be blank." }
}

private fun monthlyBudgetDoc(db: FirebaseFirestore, userId: String, monthId: String) =
    db.collection(BudgetFirestoreSchema.USERS)
        .document(userId)
        .collection(BudgetFirestoreSchema.BUDGETS)
        .document(monthId)

suspend fun createMonthlyBudget(userId: String, monthId: String, totalBudget: Double) {
    validateMonthId(monthId)
    val db = Firebase.firestore
    val payload = mapOf(
        BudgetFirestoreSchema.TOTAL_BUDGET to totalBudget,
        BudgetFirestoreSchema.CATEGORIES to emptyMap<String, Double>()
    )
    monthlyBudgetDoc(db, userId, monthId)
        .set(payload, SetOptions.merge())
        .awaitVoid()
}

suspend fun addCategoryBudget(userId: String, monthId: String, categoryName: String, amount: Double) {
    validateMonthId(monthId)
    validateCategoryName(categoryName)
    val db = Firebase.firestore
    val docRef = monthlyBudgetDoc(db, userId, monthId)
    db.runTransaction { tx ->
        val snapshot = tx.get(docRef)
        if (!snapshot.exists()) {
            throw IllegalStateException("Monthly budget does not exist for $monthId.")
        }
        val categoryMap = (snapshot.get(BudgetFirestoreSchema.CATEGORIES) as? Map<*, *>)
            ?.entries
            ?.associate { it.key.toString() to (it.value as? Number)?.toDouble().orEmptyDouble() }
            ?.toMutableMap()
            ?: mutableMapOf()
        if (categoryMap.containsKey(categoryName)) {
            throw IllegalStateException("Category '$categoryName' already exists for $monthId.")
        }
        categoryMap[categoryName] = amount
        tx.update(docRef, BudgetFirestoreSchema.CATEGORIES, categoryMap)
        null
    }.awaitTask()
}

suspend fun updateCategoryBudget(
    userId: String,
    monthId: String,
    categoryName: String,
    amount: Double
) {
    validateMonthId(monthId)
    validateCategoryName(categoryName)
    val db = Firebase.firestore
    val docRef = monthlyBudgetDoc(db, userId, monthId)
    db.runTransaction { tx ->
        val snapshot = tx.get(docRef)
        if (!snapshot.exists()) {
            throw IllegalStateException("Monthly budget does not exist for $monthId.")
        }
        val categoryMap = (snapshot.get(BudgetFirestoreSchema.CATEGORIES) as? Map<*, *>)
            ?.entries
            ?.associate { it.key.toString() to (it.value as? Number)?.toDouble().orEmptyDouble() }
            ?.toMutableMap()
            ?: mutableMapOf()
        if (!categoryMap.containsKey(categoryName)) {
            throw IllegalStateException("Category '$categoryName' does not exist for $monthId.")
        }
        categoryMap[categoryName] = amount
        tx.update(docRef, BudgetFirestoreSchema.CATEGORIES, categoryMap)
        null
    }.awaitTask()
}

suspend fun getMonthlyBudget(userId: String, monthId: String): MonthlyBudget? {
    validateMonthId(monthId)
    val db = Firebase.firestore
    val snapshot = monthlyBudgetDoc(db, userId, monthId).get().awaitTask()
    if (!snapshot.exists()) return null

    val total = (snapshot.get(BudgetFirestoreSchema.TOTAL_BUDGET) as? Number)?.toDouble() ?: 0.0
    val categories = (snapshot.get(BudgetFirestoreSchema.CATEGORIES) as? Map<*, *>)
        ?.entries
        ?.associate { it.key.toString() to (it.value as? Number)?.toDouble().orEmptyDouble() }
        ?: emptyMap()

    return MonthlyBudget(
        totalBudget = total,
        categories = categories
    )
}

suspend fun getBudgetMonths(userId: String): List<String> {
    val db = Firebase.firestore
    val snapshots = db.collection(BudgetFirestoreSchema.USERS)
        .document(userId)
        .collection(BudgetFirestoreSchema.BUDGETS)
        .get()
        .awaitTask()

    return snapshots.documents
        .map { it.id }
        .filter { BudgetFirestoreSchema.MONTH_ID_REGEX.matches(it) }
        .sortedDescending()
}

private suspend fun getUserExpenseCategories(userId: String): List<String> {
    val db = Firebase.firestore
    val userDoc = db.collection(BudgetFirestoreSchema.USERS)
        .document(userId)
        .get()
        .awaitTask()
    @Suppress("UNCHECKED_CAST")
    val expense = (userDoc.get("expenseCategories") as? List<*>)
        ?.mapNotNull { it as? String }
        .orEmpty()
    @Suppress("UNCHECKED_CAST")
    val income = (userDoc.get("incomeCategories") as? List<*>)
        ?.mapNotNull { it as? String }
        .orEmpty()

    return (expense + income)
        .map { it.trim() }
        .filter { it.isNotBlank() }
        .distinct()
        .ifEmpty { defaultBudgetCategories }
}

private suspend fun upsertMonthlyCategoryBudgets(
    userId: String,
    monthId: String,
    categoryBudgets: Map<String, Double>
) {
    validateMonthId(monthId)
    val cleaned = categoryBudgets
        .filterKeys { it.isNotBlank() }
        .mapValues { (_, amount) -> amount.coerceAtLeast(0.0) }
    if (cleaned.isEmpty()) return

    val db = Firebase.firestore
    val docRef = monthlyBudgetDoc(db, userId, monthId)
    db.runTransaction { tx ->
        val existing = tx.get(docRef)
        val existingCategories = (existing.get(BudgetFirestoreSchema.CATEGORIES) as? Map<*, *>)
            ?.entries
            ?.associate { it.key.toString() to (it.value as? Number)?.toDouble().orEmptyDouble() }
            ?.toMutableMap()
            ?: mutableMapOf()

        existingCategories.putAll(cleaned)
        val totalBudget = existingCategories.values.sum()
        tx.set(
            docRef,
            mapOf(
                BudgetFirestoreSchema.TOTAL_BUDGET to totalBudget,
                BudgetFirestoreSchema.CATEGORIES to existingCategories
            ),
            SetOptions.merge()
        )
        null
    }.awaitTask()
}

private suspend fun removeCategoryBudget(userId: String, monthId: String, categoryName: String) {
    validateMonthId(monthId)
    validateCategoryName(categoryName)
    val db = Firebase.firestore
    val docRef = monthlyBudgetDoc(db, userId, monthId)
    db.runTransaction { tx ->
        val existing = tx.get(docRef)
        if (!existing.exists()) return@runTransaction null

        val categories = (existing.get(BudgetFirestoreSchema.CATEGORIES) as? Map<*, *>)
            ?.entries
            ?.associate { it.key.toString() to (it.value as? Number)?.toDouble().orEmptyDouble() }
            ?.toMutableMap()
            ?: mutableMapOf()

        categories.remove(categoryName)
        val totalBudget = categories.values.sum()

        tx.set(
            docRef,
            mapOf(
                BudgetFirestoreSchema.TOTAL_BUDGET to totalBudget,
                BudgetFirestoreSchema.CATEGORIES to categories
            ),
            SetOptions.merge()
        )
        null
    }.awaitTask()
}

private suspend fun getMonthlySpentData(userId: String, monthId: String): MonthlySpentData {
    validateMonthId(monthId)
    val db = Firebase.firestore
    val snapshots = db.collection(BudgetFirestoreSchema.USERS)
        .document(userId)
        .collection("transactions")
        .whereEqualTo("type", "expense")
        .get()
        .awaitTask()

    val monthPrefix = "$monthId-"
    val filtered = snapshots.documents.filter { doc ->
        (doc.getString("date") ?: "").startsWith(monthPrefix)
    }

    val byCategory = mutableMapOf<String, Double>()
    filtered.forEach { doc ->
        val amount = (doc.get("amount") as? Number)?.toDouble() ?: 0.0
        val category = (doc.getString("category") ?: "Other").ifBlank { "Other" }
        byCategory[category] = (byCategory[category] ?: 0.0) + amount
    }

    return MonthlySpentData(
        totalSpent = byCategory.values.sum(),
        categorySpent = byCategory
    )
}

private fun Double?.orEmptyDouble(): Double = this ?: 0.0
private fun formatMoney(amount: Double): String =
    NumberFormat.getCurrencyInstance(Locale.US).format(amount)
private fun spentForCategory(categorySpent: Map<String, Double>, categoryName: String): Double =
    categorySpent.entries
        .filter { it.key.equals(categoryName, ignoreCase = true) }
        .sumOf { it.value }

private fun getAllMonthOptionsForYear(year: Int): List<MonthOption> {
    val shortMonthNames = listOf(
        "Jan", "Feb", "Mar", "Apr", "May", "Jun",
        "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"
    )
    return (1..12).map { month ->
        val id = "%04d-%02d".format(year, month)
        val label = "${shortMonthNames[month - 1]}$year"
        MonthOption(id = id, label = label)
    }
}

@Composable
fun BudgetScreen(onBack: () -> Unit) {
    val isPreview = LocalInspectionMode.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val monthDropdownYear = remember { Calendar.getInstance().get(Calendar.YEAR) }
    val monthOptions = remember(monthDropdownYear) { getAllMonthOptionsForYear(monthDropdownYear) }
    var budgetMonths by remember { mutableStateOf<List<String>>(emptyList()) }
    var selectedMonth by remember { mutableStateOf<String?>(null) }
    var monthlyBudget by remember { mutableStateOf<MonthlyBudget?>(null) }
    var monthlySpentData by remember { mutableStateOf(MonthlySpentData(0.0, emptyMap())) }
    var menuExpanded by remember { mutableStateOf(false) }
    var showAddBudgetDialog by remember { mutableStateOf(false) }
    var monthInput by remember { mutableStateOf("") }
    var monthInputMenuExpanded by remember { mutableStateOf(false) }
    val categoryInputs = remember { mutableStateListOf(CategoryBudgetInput()) }
    var categoryOptions by remember { mutableStateOf(defaultBudgetCategories) }
    var isSavingBudget by remember { mutableStateOf(false) }
    var pendingDeleteCategory by remember { mutableStateOf<String?>(null) }
    var isDeletingCategory by remember { mutableStateOf(false) }

    val currentUserId = Firebase.auth.currentUser?.uid

    LaunchedEffect(currentUserId, isPreview) {
        if (isPreview) {
            budgetMonths = listOf("2026-09")
            selectedMonth = "2026-09"
            monthlyBudget = MonthlyBudget(
                totalBudget = 2550.0,
                categories = mapOf(
                    "Food" to 600.0,
                    "Travel" to 500.0,
                    "Shopping" to 250.0,
                    "Health" to 300.0
                )
            )
            categoryOptions = defaultBudgetCategories
            return@LaunchedEffect
        }

        if (currentUserId == null) {
            budgetMonths = emptyList()
            selectedMonth = null
            monthlyBudget = null
            monthlySpentData = MonthlySpentData(0.0, emptyMap())
            return@LaunchedEffect
        }

        categoryOptions = runCatching { getUserExpenseCategories(currentUserId) }
            .getOrDefault(defaultBudgetCategories)
        val months = runCatching { getBudgetMonths(currentUserId) }.getOrDefault(emptyList())
        budgetMonths = months
        selectedMonth = months.firstOrNull()
    }

    LaunchedEffect(currentUserId, selectedMonth, isPreview) {
        if (isPreview) {
            monthlySpentData = MonthlySpentData(
                totalSpent = 738.0,
                categorySpent = mapOf("Food" to 220.0, "Travel" to 180.0, "Shopping" to 120.0)
            )
            return@LaunchedEffect
        }
        if (currentUserId == null || selectedMonth == null) {
            monthlyBudget = null
            monthlySpentData = MonthlySpentData(0.0, emptyMap())
            return@LaunchedEffect
        }
        monthlyBudget = runCatching { getMonthlyBudget(currentUserId, selectedMonth!!) }.getOrNull()
        monthlySpentData = runCatching { getMonthlySpentData(currentUserId, selectedMonth!!) }
            .getOrElse { MonthlySpentData(0.0, emptyMap()) }
    }

    val categories = monthlyBudget?.categories?.toList().orEmpty()
    val spentAmount = monthlySpentData.totalSpent
    val leftToSpendAmount = ((monthlyBudget?.totalBudget ?: 0.0) - spentAmount).coerceAtLeast(0.0)

    fun resetBudgetDialog() {
        val defaultMonth = selectedMonth
            ?: monthOptions.firstOrNull()?.id
            ?: ""
        monthInput = defaultMonth
        categoryInputs.clear()
        categoryInputs.add(CategoryBudgetInput())
        monthInputMenuExpanded = false
        isSavingBudget = false
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = Color(0xFF0345FC)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier
                        .padding(horizontal = 14.dp, vertical = 10.dp)
                        .clickable { onBack() },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Image(
                        painter = painterResource(R.drawable.left),
                        contentDescription = "Back",
                        modifier = Modifier.size(24.dp)
                    )
                }

                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Your Budget",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 20.sp,
                        color = Color.White
                    )
                }

                Row(
                    modifier = Modifier
                        .padding(horizontal = 14.dp, vertical = 10.dp)
                        .clickable {
                            resetBudgetDialog()
                            if (!isPreview && currentUserId != null) {
                                scope.launch {
                                    categoryOptions = runCatching {
                                        getUserExpenseCategories(currentUserId)
                                    }.getOrDefault(defaultBudgetCategories)
                                }
                            }
                            showAddBudgetDialog = true
                        },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Image(
                        painter = painterResource(R.drawable.addbudget),
                        contentDescription = "Add budget",
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }

        if (showAddBudgetDialog) {
            AlertDialog(
                onDismissRequest = {
                    if (!isSavingBudget) showAddBudgetDialog = false
                },
                properties = DialogProperties(dismissOnBackPress = !isSavingBudget),
                title = { Text("Add Monthly Budget") },
                text = {
                    Column(
                        modifier = Modifier.verticalScroll(rememberScrollState())
                    ) {
                        Text(
                            text = "Choose month and add multiple category budgets.",
                            color = Color(0xFF6D6D7B),
                            fontSize = 14.sp
                        )
                        Spacer(modifier = Modifier.height(10.dp))

                        Box(modifier = Modifier.wrapContentSize()) {
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { monthInputMenuExpanded = true },
                                shape = RoundedCornerShape(10.dp),
                                color = Color(0xFFF4F4F8)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 14.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    val monthLabel = monthOptions.firstOrNull { it.id == monthInput }?.label
                                        ?: "Choose month"
                                    Text(
                                        text = monthLabel,
                                        color = if (monthLabel == "Choose month") Color(0xFF8D8D98) else Color(0xFF232342)
                                    )
                                    Icon(
                                        imageVector = Icons.Default.KeyboardArrowDown,
                                        contentDescription = "Open months",
                                        tint = Color(0xFF6D6D7B)
                                    )
                                }
                            }
                            DropdownMenu(
                                expanded = monthInputMenuExpanded,
                                onDismissRequest = { monthInputMenuExpanded = false }
                            ) {
                                monthOptions.forEach { option ->
                                    DropdownMenuItem(
                                        text = { Text(option.label) },
                                        onClick = {
                                            monthInput = option.id
                                            monthInputMenuExpanded = false
                                        }
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        val availableCategories =
                            if (categoryOptions.isEmpty()) defaultBudgetCategories else categoryOptions

                        categoryInputs.forEachIndexed { index, input ->
                            var categoryExpanded by remember(index) { mutableStateOf(false) }

                            Text(
                                text = "Category ${index + 1}",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 14.sp
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Box(modifier = Modifier.wrapContentSize()) {
                                Surface(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { categoryExpanded = true },
                                    shape = RoundedCornerShape(10.dp),
                                    color = Color(0xFFF4F4F8)
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 12.dp, vertical = 14.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = if (input.categoryName.isBlank()) "Choose category" else input.categoryName,
                                            color = if (input.categoryName.isBlank()) Color(0xFF8D8D98) else Color(0xFF232342)
                                        )
                                        Icon(
                                            imageVector = Icons.Default.KeyboardArrowDown,
                                            contentDescription = "Open categories",
                                            tint = Color(0xFF6D6D7B)
                                        )
                                    }
                                }
                                DropdownMenu(
                                    expanded = categoryExpanded,
                                    onDismissRequest = { categoryExpanded = false }
                                ) {
                                    availableCategories.forEach { category ->
                                        DropdownMenuItem(
                                            text = { Text(category) },
                                            onClick = {
                                                categoryInputs[index] = input.copy(categoryName = category)
                                                categoryExpanded = false
                                            }
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            OutlinedTextField(
                                value = input.amountText,
                                onValueChange = { value ->
                                    categoryInputs[index] = input.copy(amountText = value)
                                },
                                label = { Text("Amount") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                        }

                        TextButton(
                            enabled = !isSavingBudget,
                            onClick = { categoryInputs.add(CategoryBudgetInput()) }
                        ) {
                            Text("+ Add another category")
                        }
                    }
                },
                confirmButton = {
                    Button(
                        enabled = !isSavingBudget,
                        onClick = {
                            if (currentUserId == null) {
                                android.widget.Toast.makeText(
                                    context,
                                    "Please sign in first.",
                                    android.widget.Toast.LENGTH_SHORT
                                ).show()
                                return@Button
                            }
                            val monthId = monthInput.trim()
                            if (!BudgetFirestoreSchema.MONTH_ID_REGEX.matches(monthId)) {
                                android.widget.Toast.makeText(
                                    context,
                                    "Month must be in YYYY-MM format.",
                                    android.widget.Toast.LENGTH_SHORT
                                ).show()
                                return@Button
                            }

                            val parsedEntries = categoryInputs.mapNotNull { row ->
                                val categoryName = row.categoryName.trim()
                                val amount = row.amountText.trim().toDoubleOrNull()
                                if (categoryName.isBlank() || amount == null) {
                                    null
                                } else {
                                    categoryName to amount
                                }
                            }

                            if (parsedEntries.isEmpty()) {
                                android.widget.Toast.makeText(
                                    context,
                                    "Add at least one category with valid amount.",
                                    android.widget.Toast.LENGTH_SHORT
                                ).show()
                                return@Button
                            }

                            val categoryBudgetMap = parsedEntries.associate { it.first to it.second }
                            isSavingBudget = true
                            scope.launch {
                                val result = runCatching {
                                    upsertMonthlyCategoryBudgets(
                                        userId = currentUserId,
                                        monthId = monthId,
                                        categoryBudgets = categoryBudgetMap
                                    )
                                }
                                isSavingBudget = false
                                result.onSuccess {
                                    val months = runCatching { getBudgetMonths(currentUserId) }
                                        .getOrDefault(emptyList())
                                    budgetMonths = months
                                    selectedMonth = monthId
                                    monthlyBudget = runCatching {
                                        getMonthlyBudget(currentUserId, monthId)
                                    }.getOrNull()
                                    monthlySpentData = runCatching {
                                        getMonthlySpentData(currentUserId, monthId)
                                    }.getOrElse { MonthlySpentData(0.0, emptyMap()) }
                                    showAddBudgetDialog = false
                                    android.widget.Toast.makeText(
                                        context,
                                        "Budget saved for $monthId",
                                        android.widget.Toast.LENGTH_SHORT
                                    ).show()
                                }.onFailure { error ->
                                    android.widget.Toast.makeText(
                                        context,
                                        error.message ?: "Failed to save budget",
                                        android.widget.Toast.LENGTH_SHORT
                                    ).show()
                                }
                            }
                        }
                    ) {
                        Text(if (isSavingBudget) "Saving..." else "Save")
                    }
                },
                dismissButton = {
                    TextButton(
                        enabled = !isSavingBudget,
                        onClick = { showAddBudgetDialog = false }
                    ) {
                        Text("Cancel")
                    }
                }
            )
        }

        if (pendingDeleteCategory != null) {
            AlertDialog(
                onDismissRequest = { if (!isDeletingCategory) pendingDeleteCategory = null },
                title = { Text("Delete Category") },
                text = {
                    Text("Delete '${pendingDeleteCategory.orEmpty()}' from this month budget?")
                },
                confirmButton = {
                    Button(
                        enabled = !isDeletingCategory,
                        onClick = {
                            val uid = currentUserId
                            val monthId = selectedMonth
                            val categoryName = pendingDeleteCategory
                            if (uid == null || monthId == null || categoryName.isNullOrBlank()) {
                                pendingDeleteCategory = null
                                return@Button
                            }
                            isDeletingCategory = true
                            scope.launch {
                                runCatching { removeCategoryBudget(uid, monthId, categoryName) }
                                    .onSuccess {
                                        monthlyBudget = getMonthlyBudget(uid, monthId)
                                        monthlySpentData = getMonthlySpentData(uid, monthId)
                                        android.widget.Toast.makeText(
                                            context,
                                            "Category deleted",
                                            android.widget.Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                    .onFailure {
                                        android.widget.Toast.makeText(
                                            context,
                                            "Failed to delete category",
                                            android.widget.Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                isDeletingCategory = false
                                pendingDeleteCategory = null
                            }
                        }
                    ) {
                        Text(if (isDeletingCategory) "Deleting..." else "Delete")
                    }
                },
                dismissButton = {
                    TextButton(
                        enabled = !isDeletingCategory,
                        onClick = { pendingDeleteCategory = null }
                    ) {
                        Text("Cancel")
                    }
                }
            )
        }

        if (budgetMonths.isEmpty()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No budget months found",
                    fontWeight = FontWeight.Bold,
                    color = Color.Gray,
                    fontSize = 16.sp
                )
            }
            return@Column
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(4.dp))

            Box(modifier = Modifier.wrapContentSize()) {
                Row(
                    modifier = Modifier
                        .clickable { menuExpanded = true }
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = selectedMonth.orEmpty(),
                        fontSize = 18.sp,
                        color = Color(0xFF4A4A4A),
                        fontWeight = FontWeight.Medium
                    )
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowDown,
                        contentDescription = "Choose month",
                        tint = Color(0xFF4A4A4A)
                    )
                }

                DropdownMenu(
                    expanded = menuExpanded,
                    onDismissRequest = { menuExpanded = false }
                ) {
                    budgetMonths.forEach { monthId ->
                        DropdownMenuItem(
                            text = { Text(monthId) },
                            onClick = {
                                selectedMonth = monthId
                                menuExpanded = false
                            }
                        )
                    }
                }
            }

            Text(
                text = formatMoney(monthlyBudget?.totalBudget ?: 0.0),
                fontWeight = FontWeight.ExtraBold,
                fontSize = 48.sp,
                color = Color(0xFF232342),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(14.dp))

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                color = Color(0xFFF7F7FB)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 18.dp, vertical = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = "Spent",
                            color = Color(0xFF6D6D7B),
                            fontSize = 15.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = formatMoney(spentAmount),
                            fontWeight = FontWeight.Bold,
                            fontSize = 24.sp,
                            color = Color(0xFF232342)
                        )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "Left to spend",
                            color = Color(0xFF6D6D7B),
                            fontSize = 15.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = formatMoney(leftToSpendAmount),
                            fontWeight = FontWeight.Bold,
                            fontSize = 24.sp,
                            color = Color(0xFF232342),
                            textAlign = TextAlign.End
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (categories.isEmpty()) {
                Text(
                    text = "No category budgets for this month",
                    color = Color.Gray,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(vertical = 16.dp)
                )
            } else {
                categories.forEach { (categoryName, categoryBudget) ->
                    val categorySpent = spentForCategory(monthlySpentData.categorySpent, categoryName)
                    val categoryLeft = (categoryBudget - categorySpent).coerceAtLeast(0.0)

                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp)
                            .clickable {
                                if (!isDeletingCategory) pendingDeleteCategory = categoryName
                            },
                        shape = RoundedCornerShape(18.dp),
                        color = Color(0xFFF7F7FB)
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = categoryName,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 20.sp,
                                    color = Color(0xFF232342)
                                )
                                Text(
                                    text = formatMoney(categoryBudget),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 22.sp,
                                    color = Color(0xFF232342)
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Spent ${formatMoney(categorySpent)}",
                                    color = Color(0xFF6D6D7B),
                                    fontSize = 14.sp
                                )
                                Text(
                                    text = "Left to spend ${formatMoney(categoryLeft)}",
                                    color = Color(0xFF6D6D7B),
                                    fontSize = 14.sp
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Tap card to delete",
                                color = Color(0xFF9A9AAC),
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Preview(showBackground = true)
@Composable
fun Preview() {
    BudgetScreen(onBack = {})
}