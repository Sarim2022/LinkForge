package com.example.linkforge.Screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.content.Context
import android.util.Log
import android.widget.Toast
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import com.example.linkforge.R
import com.example.linkforge.data.ExpenseAndIncome
import com.example.linkforge.data.Transaction
import com.example.linkforge.data.UserPreferences
import com.example.linkforge.data.Wallet
import com.example.linkforge.Screens.BudgetScreen
import com.example.linkforge.Screens.JourneyDescScreen
import com.example.linkforge.Screens.ReminderScreen
import com.example.linkforge.Screens.SavingsScreen
import com.example.linkforge.Screens.WalletDescScreen
import com.example.linkforge.walletmanage.AddWalletCard
import com.example.linkforge.walletmanage.WalletCard
import com.google.firebase.Timestamp
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.Date
import java.util.UUID
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults

@Composable
fun HomeScreen(onLogout: () -> Unit = {}, clearHomeSubScreen: Int = 0) {
    val context = LocalContext.current
    val userPrefs = remember(context) { UserPreferences(context) }
    val user = remember(context) { userPrefs.getUser() }
    val displayName = user?.displayName?.takeIf { it.isNotBlank() } ?: "User"

    var walletList by remember { mutableStateOf(userPrefs.getWallets()) }
    var expenseAndIncome by remember {
        mutableStateOf(
            userPrefs.getJourney() ?: ExpenseAndIncome(income = 0.0, expense = 0.0, lend = 0.0, borrow = 0.0)
        )
    }
    var transactionList by remember { mutableStateOf<List<Transaction>>(emptyList()) }
    val uid = Firebase.auth.currentUser?.uid

    fun fetchTransactions() {
        if (uid == null) return
        Firebase.firestore
            .collection("users").document(uid).collection("transactions")
            .get()
            .addOnSuccessListener { snapshot ->
                val list = snapshot.documents.mapNotNull { doc ->
                    val type = doc.getString("type") ?: return@mapNotNull null
                    val title = doc.getString("title") ?: ""
                    val category = doc.getString("category") ?: ""
                    val amount = (doc.get("amount") as? Number)?.toDouble() ?: 0.0
                    val walletName = doc.getString("walletName") ?: ""
                    val personName = doc.getString("personName") ?: ""
                    val date = doc.getString("date") ?: ""
                    val note = doc.getString("note") ?: ""
                    val oldWalletMoney = (doc.get("oldWalletMoney") as? Number)?.toDouble() ?: 0.0
                    val newWalletMoney = (doc.get("newWalletMoney") as? Number)?.toDouble() ?: 0.0
                    val createdAt = (doc.get("createdAt") as? Timestamp)?.toDate()?.time ?: 0L
                    Transaction(
                        transactionId = doc.id,
                        title = title,
                        category = category,
                        type = type,
                        amount = amount,
                        walletName = walletName,
                        personName = personName,
                        date = date,
                        oldWalletMoney = oldWalletMoney,
                        newWalletMoney = newWalletMoney,
                        note = note,
                        createdAt = createdAt
                    )
                }.sortedByDescending { it.createdAt }
                transactionList = list
            }
    }

    LaunchedEffect(uid) {
        if (uid == null) return@LaunchedEffect
        val db = Firebase.firestore
        db.collection("users").document(uid).get()
            .addOnSuccessListener { doc ->
                @Suppress("UNCHECKED_CAST")
                val walletsData = doc.get("wallets") as? List<Map<String, Any>> ?: emptyList()
                val wallets = walletsData.map { map ->
                    Wallet(
                        id = (map["id"] as? String).orEmpty(),
                        name = (map["name"] as? String).orEmpty(),
                        balance = (map["balance"] as? Number)?.toDouble() ?: 0.0,
                        isDefault = (map["isDefault"] as? Boolean) ?: false
                    )
                }
                walletList = wallets
                userPrefs.saveWallets(wallets)
                val income = (doc.get("totalIncome") as? Number)?.toDouble() ?: (doc.get("income") as? Number)?.toDouble() ?: 0.0
                val expense = (doc.get("totalExpense") as? Number)?.toDouble() ?: (doc.get("expenses") as? Number)?.toDouble() ?: 0.0
                val lend = (doc.get("lend") as? Number)?.toDouble() ?: 0.0
                val borrow = (doc.get("borrow") as? Number)?.toDouble() ?: 0.0
                val journey = ExpenseAndIncome(income = income, expense = expense, lend = lend, borrow = borrow)
                expenseAndIncome = journey
                userPrefs.saveJourney(journey)
                val txnCount = (doc.get("transactionCount") as? Number)?.toInt() ?: 0
                userPrefs.saveTransactionCount(txnCount)
            }
        fetchTransactions()
    }

    var selectedWallet by remember { mutableStateOf<Wallet?>(null) }
    var selectedJourneyType by remember { mutableStateOf<String?>(null) }
    var selectedJourneyAmount by remember { mutableStateOf(0.0) }
    var selectedServiceScreen by remember { mutableStateOf<String?>(null) }
    var selectedTransaction by remember { mutableStateOf<Transaction?>(null) }

    LaunchedEffect(clearHomeSubScreen) {
        if (clearHomeSubScreen > 0) {
            selectedWallet = null
            selectedJourneyType = null
            selectedServiceScreen = null
        }
    }

    if (selectedWallet != null) {
        WalletDescScreen(
            wallet = selectedWallet!!,
            onBack = { selectedWallet = null },
            uid = uid,
            onWalletUpdated = { updated ->
                walletList = walletList.map { if (it.name == updated.name && it.id == updated.id) updated else it }
                userPrefs.saveWallets(walletList)
                selectedWallet = updated
            }
        )
        return
    }

    if (selectedJourneyType != null) {
        JourneyDescScreen(
            journeyType = selectedJourneyType!!,
            totalAmount = selectedJourneyAmount,
            onBack = { selectedJourneyType = null },
            uid = uid
        )
        return
    }

    if (selectedTransaction != null) {
        TransactionDetailScreen(
            transaction = selectedTransaction!!,
            onBack = { selectedTransaction = null }
        )
        return
    }

    when (selectedServiceScreen) {
        "savings" -> {
            SavingsScreen(onBack = { selectedServiceScreen = null })
            return
        }
        "reminder" -> {
            ReminderScreen(onBack = { selectedServiceScreen = null })
            return
        }
        "budget" -> {
            BudgetScreen(onBack = { selectedServiceScreen = null })
            return
        }
        else -> { }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(top = 0.dp)
    ) {
        HeaderCompoable(displayName = displayName, onLogout = onLogout)

        Text("Your Wallets", fontWeight = FontWeight.ExtraBold, color=Color.Blue, modifier = Modifier.padding(start=15.dp, top = 14.dp), fontSize = 16.sp)
        UserCardSection(
            walletList = walletList,
            onWalletClick = { selectedWallet = it },
            onAddWallet = { name, balance, onSuccess ->
                if (uid == null) {
                    onSuccess()
                    return@UserCardSection
                }
                val newWallet = hashMapOf(
                    "id" to UUID.randomUUID().toString(),
                    "name" to name,
                    "balance" to balance,
                    "isDefault" to false
                )
                val updatedWallets = walletList.map { w ->
                    hashMapOf(
                        "id" to w.id,
                        "name" to w.name,
                        "balance" to w.balance,
                        "isDefault" to w.isDefault
                    )
                } + newWallet
                Firebase.firestore.collection("users").document(uid)
                    .update("wallets", updatedWallets)
                    .addOnSuccessListener {
                        val added = Wallet(
                            id = newWallet["id"] as String,
                            name = name,
                            balance = balance,
                            isDefault = false
                        )
                        walletList = walletList + added
                        userPrefs.saveWallets(walletList)
                        onSuccess()
                    }
                    .addOnFailureListener { onSuccess() }
            }
        )
        Text("Your Journey", fontWeight = FontWeight.ExtraBold, color=Color.Blue, modifier = Modifier.padding(start=15.dp), fontSize = 16.sp)
        JourneyLazyRow(
            expenseAndIncome = expenseAndIncome,
            onJourneyClick = { type, amount ->
                selectedJourneyType = type
                selectedJourneyAmount = amount
            }
        )
        ServicesMoneyAddonSection(
            onSavingsClick = { selectedServiceScreen = "savings" },
            onRemindClick = { selectedServiceScreen = "reminder" },
            onBudgetClick = { selectedServiceScreen = "budget" }
        )
        var showAddTransactionDialog by remember { mutableStateOf(false) }
        if (showAddTransactionDialog) {
            AddTransactionDialog(
                walletList = walletList,
                expenseCategories = userPrefs.getExpenseCategories().ifEmpty { listOf("Food", "Groceries", "Transport", "Rent", "Bills", "Shopping", "Entertainment", "Health", "Travel", "Other") },
                incomeCategories = userPrefs.getIncomeCategories().ifEmpty { listOf("Salary", "Freelance", "Business", "Investment", "Gift", "Bonus", "Cashback", "Interest", "Prize", "Other") },
                currentJourney = expenseAndIncome,
                onDismiss = { showAddTransactionDialog = false },
                onAdded = { updatedWallets, updatedJourney ->
                    walletList = updatedWallets
                    expenseAndIncome = updatedJourney
                    userPrefs.saveWallets(updatedWallets)
                    userPrefs.saveJourney(updatedJourney)
                    userPrefs.saveTransactionCount(userPrefs.getTransactionCount() + 1)
                    showAddTransactionDialog = false
                    fetchTransactions()
                },
                uid = uid
            )
        }
        Row(Modifier.fillMaxWidth().padding(horizontal = 15.dp, vertical = 10.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("Your Transactions", fontWeight = FontWeight.ExtraBold, color=Color.Blue, modifier = Modifier.padding(), fontSize = 18.sp)
            Image(
                painter = painterResource(R.drawable.plus),
                contentDescription = null,
                modifier = Modifier
                    .size(40.dp)
                    .clickable { showAddTransactionDialog = true }
            )
        }

        if (transactionList.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 32.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Do your first transaction using the + button.",
                    fontSize = 15.sp,
                    color = Color.Gray,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 24.dp)
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentPadding = PaddingValues(horizontal = 15.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                items(
                    items = transactionList,
                    key = { it.transactionId }
                ) { transaction ->
                    TransactionRow(
                        transaction = transaction,
                        onClick = { selectedTransaction = transaction }
                    )
                }
            }
        }
    }
}

private val TransactionRowIconSize = 44.dp
private val IncomeAmountColor = Color(0xFF2E7D32)
private val ExpenseAmountColor = Color(0xFFC62828)
private val TransactionRowBackground = Color(0xFFE3F2FD) // light blue

@Composable
private fun TransactionRow(
    transaction: Transaction,
    onClick: () -> Unit
) {
    val (iconResId, amountColor, amountPrefix) = when (transaction.type) {
        "income" -> Triple(R.drawable.income, IncomeAmountColor, "+")
        "expense" -> Triple(R.drawable.expenses, ExpenseAmountColor, "-")
        "lend", "borrow" -> Triple(R.drawable.lend, Color.Black, "")
        else -> Triple(R.drawable.lend, Color.Black, "")
    }
    val subtitle = when (transaction.type) {
        "income", "expense" -> transaction.category.ifEmpty { transaction.date }
        "lend", "borrow" -> transaction.personName.ifEmpty { transaction.date }
        else -> transaction.date
    }
    val amountText = buildString {
        append(amountPrefix)
        append("₹")
        val a = transaction.amount
        append(if (a == a.toLong().toDouble()) a.toLong() else a)
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        color = TransactionRowBackground
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
        Box(
            modifier = Modifier
                .size(TransactionRowIconSize)
                .clip(CircleShape)
                .background(
                    when (transaction.type) {
                        "income" -> Color(0xFFE8F5E9)
                        "expense" -> Color(0xFFFFEBEE)
                        else -> Color(0xFFF3E5F5)
                    }
                ),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(iconResId),
                contentDescription = null,
                modifier = Modifier.size(24.dp)
            )
        }
        Spacer(Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = transaction.title.ifEmpty { "Transaction" },
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = subtitle.ifEmpty { "—" },
                fontSize = 13.sp,
                color = Color.Gray
            )
        }
        Text(
            text = amountText,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = amountColor
        )
        }
    }
}

@Composable
private fun TransactionDetailScreen(
    transaction: Transaction,
    onBack: () -> Unit
) {
    val amountPrefix = when (transaction.type) {
        "income" -> "+"
        "expense" -> "-"
        else -> ""
    }
    val amountColor = when (transaction.type) {
        "income" -> Color(0xFF2E7D32)
        "expense" -> Color(0xFFC62828)
        else -> Color.Black
    }
    val amountText = buildString {
        append(amountPrefix)
        append("₹")
        val a = transaction.amount
        append(if (a == a.toLong().toDouble()) a.toLong() else a)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .background(Color(0xFFF6F8FF))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Image(
                painter = painterResource(R.drawable.left),
                contentDescription = "Back",
                modifier = Modifier
                    .size(24.dp)
                    .clickable { onBack() }
            )
            Text(
                text = "Transaction Details",
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 24.dp),
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 20.sp,
                color = Color(0xFF1B1F3B)
            )
        }

        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            shape = RoundedCornerShape(18.dp),
            color = Color.White,
            shadowElevation = 2.dp
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Text(
                    text = transaction.title.ifEmpty { "Transaction" },
                    fontSize = 24.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color(0xFF1B1F3B)
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = amountText,
                    fontSize = 30.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = amountColor
                )
                Spacer(modifier = Modifier.height(14.dp))

                TransactionDetailRow(label = "Type", value = transaction.type.ifEmpty { "—" })
                TransactionDetailRow(label = "Category", value = transaction.category.ifEmpty { "—" })
                TransactionDetailRow(label = "Person Name", value = transaction.personName.ifEmpty { "—" })
                TransactionDetailRow(label = "Wallet", value = transaction.walletName.ifEmpty { "—" })
                TransactionDetailRow(label = "Date", value = transaction.date.ifEmpty { "—" })
                TransactionDetailRow(label = "Old Wallet Amount", value = "₹${transaction.oldWalletMoney}")
                TransactionDetailRow(label = "New Wallet Amount", value = "₹${transaction.newWalletMoney}")
                TransactionDetailRow(label = "Note", value = transaction.note.ifEmpty { "—" })
                TransactionDetailRow(
                    label = "Transaction ID",
                    value = transaction.transactionId.ifEmpty { "—" }
                )
            }
        }
    }
}

@Composable
private fun TransactionDetailRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = label,
            fontWeight = FontWeight.SemiBold,
            color = Color(0xFF5C6078),
            fontSize = 14.sp
        )
        Text(
            text = value,
            color = Color(0xFF1B1F3B),
            fontSize = 14.sp,
            textAlign = TextAlign.End,
            modifier = Modifier.padding(start = 14.dp)
        )
    }
}

private fun todayDateString(): String =
    SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddTransactionDialog(
    walletList: List<Wallet>,
    expenseCategories: List<String>,
    incomeCategories: List<String>,
    currentJourney: ExpenseAndIncome,
    onDismiss: () -> Unit,
    onAdded: (List<Wallet>, ExpenseAndIncome) -> Unit,
    uid: String?
) {
    val context = LocalContext.current
    var title by remember { mutableStateOf("") }
    var type by remember { mutableStateOf("expense") }
    var category by remember { mutableStateOf("") }
    var amountStr by remember { mutableStateOf("") }
    var personName by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var dateStr by remember { mutableStateOf(todayDateString()) }
    var selectedWalletName by remember { mutableStateOf("") }
    var categoryExpanded by remember { mutableStateOf(false) }
    var walletExpanded by remember { mutableStateOf(false) }
    var isAdding by remember { mutableStateOf(false) }

    val isIncomeOrExpense = type == "income" || type == "expense"
    val categories = if (type == "income") incomeCategories else expenseCategories
    if (category !in categories) category = categories.firstOrNull().orEmpty()
    if (selectedWalletName.isEmpty() && walletList.isNotEmpty()) selectedWalletName = walletList.first().name

    val textFieldShape = RoundedCornerShape(12.dp)
    val textFieldHeight = 56.dp
    val textFieldColors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = Color(0xFF0345FC),
        unfocusedBorderColor = Color(0xFFE0E0E0),
        focusedContainerColor = Color(0xFFFAFAFA),
        unfocusedContainerColor = Color(0xFFFAFAFA),
        cursorColor = Color(0xFF0345FC),
        focusedLabelColor = Color(0xFF0345FC),
        unfocusedLabelColor = Color(0xFF757575),
        focusedTextColor = Color(0xFF1A1A1A),
        unfocusedTextColor = Color(0xFF1A1A1A)
    )

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .padding(horizontal = 16.dp)
                .heightIn(max = 640.dp),
            shape = RoundedCornerShape(24.dp),
            color = Color.White,
            shadowElevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Title
                Text(
                    text = "Add Transaction",
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center,
                    fontFamily = FontFamily.SansSerif,
                    fontWeight = FontWeight.Bold,
                    fontSize = 22.sp,
                    color = Color(0xFF1A1A1A)
                )
                Spacer(Modifier.height(20.dp))

                // Section: Title field
                Text("Title", fontSize = 14.sp, color = Color(0xFF757575), fontWeight = FontWeight.Medium)
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Enter title", fontSize = 14.sp) },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(textFieldHeight),
                    shape = textFieldShape,
                    colors = textFieldColors,
                    textStyle = TextStyle(fontSize = 16.sp)
                )
                Spacer(Modifier.height(20.dp))

                // Section: Type
                Text("Type", fontSize = 14.sp, color = Color(0xFF757575), fontWeight = FontWeight.Medium)
                Spacer(Modifier.height(12.dp))
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    listOf("income" to "Income", "expense" to "Expense", "lend" to "Lend", "borrow" to "Borrow").forEach { (value, label) ->
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp)
                                .clickable { type = value },
                            shape = RoundedCornerShape(10.dp),
                            color = if (type == value) Color(0xFF0345FC) else Color(0xFFF5F5F5)
                        ) {
                            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text(
                                    text = label,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (type == value) Color.White else Color(0xFF616161),
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }
                Spacer(Modifier.height(20.dp))

                // Section: Category (Income/Expense only)
                if (isIncomeOrExpense) {
                    Text("Category", fontSize = 14.sp, color = Color(0xFF757575), fontWeight = FontWeight.Medium)
                    Spacer(Modifier.height(12.dp))
                    ExposedDropdownMenuBox(expanded = categoryExpanded, onExpandedChange = { categoryExpanded = it }) {
                        OutlinedTextField(
                            value = category,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Select category", fontSize = 14.sp) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(textFieldHeight)
                                .menuAnchor(),
                            shape = textFieldShape,
                            colors = textFieldColors,
                            textStyle = TextStyle(fontSize = 16.sp),
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryExpanded) }
                        )
                        ExposedDropdownMenu(expanded = categoryExpanded, onDismissRequest = { categoryExpanded = false }) {
                            categories.forEach { cat ->
                                DropdownMenuItem(
                                    text = { Text(cat, fontSize = 15.sp) },
                                    onClick = {
                                        category = cat
                                        categoryExpanded = false
                                    }
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(20.dp))
                }

                // Section: Amount
                Text("Amount (₹)", fontSize = 14.sp, color = Color(0xFF757575), fontWeight = FontWeight.Medium)
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = amountStr,
                    onValueChange = { amountStr = it.filter { c -> c.isDigit() || c == '.' } },
                    label = { Text("0.00", fontSize = 14.sp) },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(textFieldHeight),
                    shape = textFieldShape,
                    colors = textFieldColors,
                    textStyle = TextStyle(fontSize = 16.sp)
                )
                Spacer(Modifier.height(20.dp))

                // Section: Person Name (Lend/Borrow only)
                if (type == "lend" || type == "borrow") {
                    Text("Person name", fontSize = 14.sp, color = Color(0xFF757575), fontWeight = FontWeight.Medium)
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = personName,
                        onValueChange = { personName = it },
                        label = { Text("Enter person name", fontSize = 14.sp) },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(textFieldHeight),
                        shape = textFieldShape,
                        colors = textFieldColors,
                        textStyle = TextStyle(fontSize = 16.sp)
                    )
                    Spacer(Modifier.height(20.dp))
                }

                // Section: Wallet (Income/Expense only)
                if (isIncomeOrExpense) {
                    Text("Wallet", fontSize = 14.sp, color = Color(0xFF757575), fontWeight = FontWeight.Medium)
                    Spacer(Modifier.height(12.dp))
                    ExposedDropdownMenuBox(expanded = walletExpanded, onExpandedChange = { walletExpanded = it }) {
                        OutlinedTextField(
                            value = selectedWalletName,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Select wallet", fontSize = 14.sp) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(textFieldHeight)
                                .menuAnchor(),
                            shape = textFieldShape,
                            colors = textFieldColors,
                            textStyle = TextStyle(fontSize = 16.sp),
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = walletExpanded) }
                        )
                        ExposedDropdownMenu(expanded = walletExpanded, onDismissRequest = { walletExpanded = false }) {
                            walletList.forEach { w ->
                                DropdownMenuItem(
                                    text = { Text(w.name, fontSize = 15.sp) },
                                    onClick = {
                                        selectedWalletName = w.name
                                        walletExpanded = false
                                    }
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(20.dp))
                }

                // Section: Date
                Text("Date", fontSize = 14.sp, color = Color(0xFF757575), fontWeight = FontWeight.Medium)
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = dateStr,
                    onValueChange = { dateStr = it },
                    label = { Text("yyyy-mm-dd", fontSize = 14.sp) },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(textFieldHeight),
                    shape = textFieldShape,
                    colors = textFieldColors,
                    textStyle = TextStyle(fontSize = 16.sp)
                )
                Spacer(Modifier.height(20.dp))

                // Section: Note
                Text("Note", fontSize = 14.sp, color = Color(0xFF757575), fontWeight = FontWeight.Medium)
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("Optional note", fontSize = 14.sp) },
                    singleLine = false,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = textFieldHeight, max = 120.dp),
                    shape = textFieldShape,
                    colors = textFieldColors,
                    textStyle = TextStyle(fontSize = 16.sp)
                )
                Spacer(Modifier.height(24.dp))

                // Cancel and Add buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .weight(1f)
                            .height(56.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF616161))
                    ) {
                        Text("Cancel", fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                    }
                    Button(
                        onClick = {
                        val amount = amountStr.toDoubleOrNull() ?: 0.0
                        if (isAdding) return@Button
                        if (uid == null) {
                            Toast.makeText(context, "Not signed in.", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        if (title.isBlank()) {
                            Toast.makeText(context, "Please enter a title.", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        if (amount <= 0) {
                            Toast.makeText(context, "Please enter an amount greater than 0.", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        if (isIncomeOrExpense) {
                            if (category.isBlank()) {
                                Toast.makeText(context, "Please select a category.", Toast.LENGTH_SHORT).show()
                                return@Button
                            }
                            val wallet = walletList.find { it.name == selectedWalletName }
                            if (wallet == null) {
                                Toast.makeText(context, "Please add and select a wallet first.", Toast.LENGTH_SHORT).show()
                                return@Button
                            }
                            val addsToWallet = type == "income"
                            val newWalletMoney = if (addsToWallet) wallet.balance + amount else wallet.balance - amount
                            if (!addsToWallet && newWalletMoney < 0) {
                                Toast.makeText(context, "Insufficient balance in wallet.", Toast.LENGTH_SHORT).show()
                                return@Button
                            }
                            isAdding = true
                            addIncomeOrExpenseTransaction(
                                context = context,
                                uid = uid,
                                title = title.trim(),
                                category = category,
                                type = type,
                                amount = amount,
                                walletName = wallet.name,
                                oldWalletMoney = wallet.balance,
                                newWalletMoney = newWalletMoney,
                                dateStr = dateStr.ifEmpty { todayDateString() },
                                note = note.trim(),
                                walletList = walletList,
                                currentJourney = currentJourney,
                                isAdding = { isAdding = it },
                                onAdded = onAdded
                            )
                        } else {
                            if (personName.isBlank()) {
                                Toast.makeText(context, "Please enter the person's name.", Toast.LENGTH_SHORT).show()
                                return@Button
                            }
                            isAdding = true
                            addLendOrBorrowTransaction(
                                context = context,
                                uid = uid,
                                title = title.trim(),
                                type = type,
                                amount = amount,
                                personName = personName.trim(),
                                dateStr = dateStr.ifEmpty { todayDateString() },
                                note = note.trim(),
                                walletList = walletList,
                                currentJourney = currentJourney,
                                isAdding = { isAdding = it },
                                onAdded = onAdded
                            )
                        }
                    },
                        enabled = !isAdding,
                        modifier = Modifier
                            .weight(1f)
                            .height(56.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0345FC))
                    ) {
                        Text(
                            text = if (isAdding) "Adding…" else "Add",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 16.sp
                        )
                    }
                }
            }
        }
    }
}

private fun addIncomeOrExpenseTransaction(
    context: Context,
    uid: String,
    title: String,
    category: String,
    type: String,
    amount: Double,
    walletName: String,
    oldWalletMoney: Double,
    newWalletMoney: Double,
    dateStr: String,
    note: String,
    walletList: List<Wallet>,
    currentJourney: ExpenseAndIncome,
    isAdding: (Boolean) -> Unit,
    onAdded: (List<Wallet>, ExpenseAndIncome) -> Unit
) {
    val userRef = Firebase.firestore.collection("users").document(uid)
    val txnData = hashMapOf<String, Any>(
        "title" to title,
        "category" to category,
        "type" to type,
        "amount" to amount,
        "walletName" to walletName,
        "date" to dateStr,
        "oldWalletMoney" to oldWalletMoney,
        "newWalletMoney" to newWalletMoney,
        "note" to note,
        "createdAt" to FieldValue.serverTimestamp()
    )
    userRef.collection("transactions").add(txnData)
        .addOnSuccessListener { docRef ->
            docRef.update("transactionId", docRef.id)
                .addOnSuccessListener {
                    val updatedWallets = walletList.map { w ->
                        if (w.name == walletName) w.copy(balance = newWalletMoney) else w
                    }
                    val updatedJourney = when (type) {
                        "income" -> currentJourney.copy(income = currentJourney.income + amount)
                        "expense" -> currentJourney.copy(expense = currentJourney.expense + amount)
                        else -> currentJourney
                    }
                    val walletsMap = updatedWallets.map { w ->
                        hashMapOf(
                            "id" to w.id,
                            "name" to w.name,
                            "balance" to w.balance,
                            "isDefault" to w.isDefault
                        )
                    }
                    userRef.update(
                        mapOf(
                            "wallets" to walletsMap,
                            "totalIncome" to updatedJourney.income,
                            "totalExpense" to updatedJourney.expense,
                            "transactionCount" to FieldValue.increment(1)
                        )
                    )
                        .addOnSuccessListener {
                            isAdding(false)
                            onAdded(updatedWallets, updatedJourney)
                            Toast.makeText(context, "Transaction added.", Toast.LENGTH_SHORT).show()
                        }
                        .addOnFailureListener { e ->
                            isAdding(false)
                            Log.e("AddTransaction", "Update user failed", e)
                            Toast.makeText(context, "Failed to update: ${e.message}", Toast.LENGTH_LONG).show()
                        }
                }
                .addOnFailureListener { e ->
                    isAdding(false)
                    Log.e("AddTransaction", "Update transactionId failed", e)
                    Toast.makeText(context, "Failed to save: ${e.message}", Toast.LENGTH_LONG).show()
                }
        }
        .addOnFailureListener { e ->
            isAdding(false)
            Log.e("AddTransaction", "Add transaction failed", e)
            Toast.makeText(context, "Failed to add transaction: ${e.message}", Toast.LENGTH_LONG).show()
        }
}

private fun addLendOrBorrowTransaction(
    context: Context,
    uid: String,
    title: String,
    type: String,
    amount: Double,
    personName: String,
    dateStr: String,
    note: String,
    walletList: List<Wallet>,
    currentJourney: ExpenseAndIncome,
    isAdding: (Boolean) -> Unit,
    onAdded: (List<Wallet>, ExpenseAndIncome) -> Unit
) {
    val userRef = Firebase.firestore.collection("users").document(uid)
    val txnData = hashMapOf<String, Any>(
        "title" to title,
        "type" to type,
        "amount" to amount,
        "personName" to personName,
        "date" to dateStr,
        "note" to note,
        "createdAt" to FieldValue.serverTimestamp()
    )
    userRef.collection("transactions").add(txnData)
        .addOnSuccessListener { docRef ->
            docRef.update("transactionId", docRef.id)
                .addOnSuccessListener {
                    val updatedJourney = when (type) {
                        "lend" -> currentJourney.copy(lend = currentJourney.lend + amount)
                        "borrow" -> currentJourney.copy(borrow = currentJourney.borrow + amount)
                        else -> currentJourney
                    }
                    userRef.update(
                        mapOf(
                            "lend" to updatedJourney.lend,
                            "borrow" to updatedJourney.borrow,
                            "transactionCount" to FieldValue.increment(1)
                        )
                    )
                        .addOnSuccessListener {
                            isAdding(false)
                            onAdded(walletList, updatedJourney)
                            Toast.makeText(context, "Transaction added.", Toast.LENGTH_SHORT).show()
                        }
                        .addOnFailureListener { e ->
                            isAdding(false)
                            Log.e("AddTransaction", "Update user failed", e)
                            Toast.makeText(context, "Failed to update: ${e.message}", Toast.LENGTH_LONG).show()
                        }
                }
                .addOnFailureListener { e ->
                    isAdding(false)
                    Log.e("AddTransaction", "Update transactionId failed", e)
                    Toast.makeText(context, "Failed to save: ${e.message}", Toast.LENGTH_LONG).show()
                }
        }
        .addOnFailureListener { e ->
            isAdding(false)
            Log.e("AddTransaction", "Add transaction failed", e)
            Toast.makeText(context, "Failed to add transaction: ${e.message}", Toast.LENGTH_LONG).show()
        }
}

private val JourneyPillShape = RoundedCornerShape(percent = 25)

// Tonal colors for immediate context: Income, Expense, Lend, Borrow
// Professional Deep Tones for Dark Mode
private val JourneyIncomeColor = Color(0xFFFFFFFF)   // Deep Emerald Green
private val JourneyExpenseColor = Color(0xFFFFFFFF)  // Deep Burnt Orange
private val JourneyLendColor = Color(0xFFFFFFFF)     // Deep Royal Purple
private val JourneyBorrowColor = Color(0xFFFFFFFF)   // Deep Amber/Gold
private val JourneyTextOnLight = Color(0xFF000000)
private val JourneyTextOnDark = Color.Black

private data class JourneyItem(
    val type: String,
    val label: String,
    val amount: Double,
    val iconResId: Int,
    val backgroundColor: Color,
    val textColor: Color
)

@Composable
fun JourneyLazyRow(
    expenseAndIncome: ExpenseAndIncome,
    onJourneyClick: (type: String, amount: Double) -> Unit = { _, _ -> }
) {
    val items = listOf(
        JourneyItem(
            "income",
            "Income",
            expenseAndIncome.income,
            R.drawable.income,
            JourneyIncomeColor,
            JourneyTextOnLight
        ),
        JourneyItem(
            "expense",
            "Expense",
            expenseAndIncome.expense,
            R.drawable.expenses,
            JourneyExpenseColor,
            JourneyTextOnLight
        ),
        JourneyItem(
            "lend",
            "Lend",
            expenseAndIncome.lend,
            R.drawable.lend,
            JourneyLendColor,
            JourneyTextOnLight
        ),
        JourneyItem(
            "borrow",
            "Borrow",
            expenseAndIncome.borrow,
            R.drawable.borrow,
            JourneyBorrowColor,
            JourneyTextOnLight
        )
    )
    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        contentPadding = PaddingValues(horizontal = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(items, key = { it.type }) { item ->
            Surface(
                modifier = Modifier
                    .width(160.dp)
                    .clickable { onJourneyClick(item.type, item.amount) },
                shape = JourneyPillShape,
                color = item.backgroundColor,
                shadowElevation = 1.dp,
                tonalElevation = 0.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Image(
                            painter = painterResource(id = item.iconResId),
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = item.label,
                            color = item.textColor,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    Text(
                        text = formatMoney(item.amount),
                        color = item.textColor,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            }
        }
    }
}

private val ServicesPillShape = RoundedCornerShape(percent = 50)

@Composable
fun ServicesMoneyAddonSection(
    onSavingsClick: () -> Unit = {},
    onRemindClick: () -> Unit = {},
    onBudgetClick: () -> Unit = {}
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 2.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Savings – blue pill, white icon & text
        Surface(
            modifier = Modifier
                .weight(1f)
                .clickable(onClick = onSavingsClick),
            shape = ServicesPillShape,
            color = Color.White
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Image(
                    painter = painterResource(R.drawable.savings),
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Savings",
                    color = Color.Black,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
        // Remind – white pill, black icon & text
        Surface(
            modifier = Modifier
                .weight(1f)
                .clickable(onClick = onRemindClick),
            shape = ServicesPillShape,
            color = Color.White,
            shadowElevation = 1.dp,
            tonalElevation = 0.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Image(
                    painter = painterResource(R.drawable.reminder),
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Remind",
                    color = Color.Black,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
        // Budget – white pill, black icon & text
        Surface(
            modifier = Modifier
                .weight(1f)
                .clickable(onClick = onBudgetClick),
            shape = ServicesPillShape,
            color = Color.White,
            shadowElevation = 1.dp,
            tonalElevation = 0.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Image(
                    painter = painterResource(R.drawable.budget),
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Budget",
                    color = Color.Black,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}



@Composable
fun UserCardSection(
    walletList: List<Wallet>,
    onWalletClick: (Wallet) -> Unit = {},
    onAddWallet: (name: String, balance: Double, onSuccess: () -> Unit) -> Unit
) {
    var showDialog by remember { mutableStateOf(false) }
    var walletName by remember { mutableStateOf("") }
    var walletAmountStr by remember { mutableStateOf("") }
    var isAdding by remember { mutableStateOf(false) }

    if (showDialog) {
        Dialog(onDismissRequest = { showDialog = false }) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth(0.95f)
                    .padding(horizontal = 10.dp),
                shape = RoundedCornerShape(24.dp),
                color = Color.White,
                shadowElevation = 4.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp)
                ) {
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.walletaddon),
                            contentDescription = null,
                            modifier = Modifier.size(56.dp)
                        )
                    }
                    Spacer(Modifier.height(16.dp))
                    Text(
                        text = "New Wallet",
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center,
                        fontFamily = FontFamily.SansSerif,
                        fontWeight = FontWeight.Bold,
                        fontSize = 22.sp,
                        color = Color(0xFF1A1A1A)
                    )
                    Spacer(Modifier.height(20.dp))
                    val textFieldShape = RoundedCornerShape(12.dp)
                    val textFieldColors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF0345FC),
                        unfocusedBorderColor = Color(0xFFE0E0E0),
                        focusedContainerColor = Color(0xFFFAFAFA),
                        unfocusedContainerColor = Color(0xFFFAFAFA),
                        cursorColor = Color(0xFF0345FC),
                        focusedLabelColor = Color(0xFF0345FC),
                        unfocusedLabelColor = Color(0xFF757575),
                        focusedTextColor = Color(0xFF1A1A1A),
                        unfocusedTextColor = Color(0xFF1A1A1A)
                    )
                    OutlinedTextField(
                        value = walletName,
                        onValueChange = { walletName = it },
                        label = { Text("Wallet name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = textFieldShape,
                        colors = textFieldColors
                    )
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = walletAmountStr,
                        onValueChange = { walletAmountStr = it.filter { c -> c.isDigit() || c == '.' } },
                        label = { Text("Amount (₹)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = textFieldShape,
                        colors = textFieldColors
                    )
                    Spacer(Modifier.height(24.dp))
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp)
                                .clickable { showDialog = false },
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFFF5F5F5),
                            border = BorderStroke(1.dp, Color(0xFFE0E0E0))
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    "Cancel",
                                    color = Color(0xFF616161),
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 16.sp
                                )
                            }
                        }
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp)
                                .clickable(enabled = !isAdding) {
                                    val name = walletName.trim()
                                    val amount = walletAmountStr.toDoubleOrNull() ?: 0.0
                                    if (name.isNotEmpty()) {
                                        isAdding = true
                                        onAddWallet(name, amount) {
                                            isAdding = false
                                            walletName = ""
                                            walletAmountStr = ""
                                            showDialog = false
                                        }
                                    }
                                },
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFF0345FC)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    if (isAdding) "Adding…" else "Add new wallet",
                                    color = Color.White,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 16.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    LazyRow(
        contentPadding = PaddingValues(start=10.dp,end=12.dp, top = 1.dp,bottom=0.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        items(walletList) { wallet ->
            WalletCard(wallet = wallet, onClick = { onWalletClick(wallet) })
        }
        item {
            AddWalletCard(onClick = { showDialog = true })
        }
    }


}



@Composable
fun HeaderCompoable(displayName: String = "User", onLogout: () -> Unit = {}) {
    var showLogoutDialog by remember { mutableStateOf(false) }
    val currentMonthLabel = remember {
        SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(Date())
    }

    if (showLogoutDialog) {
        LogoutDialog(
            onDismiss = { showLogoutDialog = false },
            onLogout = {
                showLogoutDialog = false
                onLogout()
            }
        )
    }

    Row(
        Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 15.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column() {
            Text(
                "Overview",
                fontSize = 18.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color.Black
            )
            Text(
                text = currentMonthLabel,
                fontSize = 13.sp,
                color = Color.Gray,
                fontWeight = FontWeight.Medium
            )
        }
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(
                    color = Color.Blue,
                    shape = CircleShape
                )
                .clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() }
                ) {
                    showLogoutDialog = true
                },
            contentAlignment = Alignment.Center
        ) {
            Text(
                displayName.trim().take(2).uppercase(),
                fontSize = 18.sp,
                color = Color.White
            )
        }
    }
}

@Composable
private fun LogoutDialog(onDismiss: () -> Unit, onLogout: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .padding(horizontal = 10.dp),
            shape = RoundedCornerShape(24.dp),
            color = Color.White,
            shadowElevation = 4.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.logout),
                        contentDescription = null,
                        modifier = Modifier.size(56.dp)
                    )
                }
                Spacer(Modifier.height(16.dp))
                Text(
                    text = "Are you sure you want to log out?",
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center,
                    fontFamily = FontFamily.SansSerif,
                    fontWeight = FontWeight.Bold,
                    fontSize = 22.sp,
                    color = Color(0xFF1A1A1A)
                )
                Spacer(Modifier.height(24.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .height(56.dp)
                            .clickable { onDismiss() },
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFFF5F5F5),
                        border = BorderStroke(1.dp, Color(0xFFE0E0E0))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "Cancel",
                                color = Color(0xFF616161),
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 16.sp
                            )
                        }
                    }
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .height(56.dp)
                            .clickable { onLogout() },
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFF0345FC)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "Logout",
                                color = Color.White,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 16.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun formatMoney(amount: Double): String =
    if (amount == amount.toLong().toDouble()) "${amount.toLong()}" else "$amount"

private val SearchBarShape = RoundedCornerShape(percent = 50)
private val SearchPlaceholderGray = Color(0xFF9E9E9E)
private val SearchTextColor = Color(0xFF1A1A1A)

@Composable
fun SearchbarUi() {
    var query by remember { mutableStateOf("") }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 15.dp, vertical = 0.dp)
            .shadow(2.dp, SearchBarShape, ambientColor = Color.Black.copy(alpha = 0.06f))
            .clip(SearchBarShape)
            .background(Color.White)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Image(
                painter = painterResource(id = R.drawable.search),
                contentDescription = "Search",
                modifier = Modifier.size(20.dp)
            )
            Spacer(Modifier.width(12.dp))
            BasicTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.weight(1f),
                singleLine = true,
                textStyle = TextStyle(
                    color = SearchTextColor,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Normal
                ),
                cursorBrush = SolidColor(Color(0xFF0345FC)),
                decorationBox = { innerTextField ->
                    Box {
                        if (query.isEmpty()) {
                            Text(
                                "Search",
                                color = SearchPlaceholderGray,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Normal
                            )
                        }
                        innerTextField()
                    }
                }
            )
        }
    }
}
