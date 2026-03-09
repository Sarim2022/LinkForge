package com.example.linkforge.Screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.linkforge.R
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

data class SavingsMonthOption(
    val id: String,
    val label: String
)

data class SavingsMonthData(
    val totalSavings: Double,
    val walletSavings: Map<String, Double>
)

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

private fun formatSavingsMoney(amount: Double): String =
    NumberFormat.getCurrencyInstance(Locale.US).format(amount)

private fun currentMonthId(): String =
    SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(Date())

private fun currentMonthLabel(): String =
    SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(Date())

private fun getMonthOptionsForYear(year: Int): List<SavingsMonthOption> {
    val names = listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")
    return (1..12).map { month ->
        val id = "%04d-%02d".format(year, month)
        SavingsMonthOption(id = id, label = "${names[month - 1]}$year")
    }
}

private suspend fun getUserWalletBalances(uid: String): Map<String, Double> {
    val userDoc = Firebase.firestore.collection("users").document(uid).get().awaitTask()
    @Suppress("UNCHECKED_CAST")
    val walletsData = userDoc.get("wallets") as? List<Map<String, Any>> ?: emptyList()
    return walletsData.associate { map ->
        val name = (map["name"] as? String).orEmpty().ifBlank { "Wallet" }
        val balance = (map["balance"] as? Number)?.toDouble() ?: 0.0
        name to balance
    }
}

private suspend fun getSavingsForMonth(uid: String, monthId: String): SavingsMonthData {
    val doc = Firebase.firestore
        .collection("users").document(uid)
        .collection("savings").document(monthId)
        .get()
        .awaitTask()
    if (!doc.exists()) return SavingsMonthData(0.0, emptyMap())

    val total = (doc.get("totalSavings") as? Number)?.toDouble() ?: 0.0
    val walletMap = (doc.get("wallets") as? Map<*, *>)
        ?.entries
        ?.associate { it.key.toString() to (it.value as? Number)?.toDouble().orEmpty() }
        ?: emptyMap()
    return SavingsMonthData(totalSavings = total, walletSavings = walletMap)
}

private suspend fun addSavingsForWallet(
    uid: String,
    monthId: String,
    walletName: String,
    amount: Double
) {
    val safeAmount = amount.coerceAtLeast(0.0)
    val db: FirebaseFirestore = Firebase.firestore
    val ref = db.collection("users").document(uid).collection("savings").document(monthId)
    db.runTransaction { tx ->
        val existing = tx.get(ref)
        val current = (existing.get("wallets") as? Map<*, *>)
            ?.entries
            ?.associate { it.key.toString() to (it.value as? Number)?.toDouble().orEmpty() }
            ?.toMutableMap()
            ?: mutableMapOf()

        current[walletName] = (current[walletName] ?: 0.0) + safeAmount
        val total = current.values.sum()
        tx.set(
            ref,
            mapOf(
                "totalSavings" to total,
                "wallets" to current
            ),
            SetOptions.merge()
        )
        null
    }.awaitTask()
}

private fun Double?.orEmpty(): Double = this ?: 0.0

@Composable
fun SavingsScreen(onBack: () -> Unit) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val uid = Firebase.auth.currentUser?.uid
    val year = remember { Calendar.getInstance().get(Calendar.YEAR) }
    val monthOptions = remember(year) { getMonthOptionsForYear(year) }

    var walletBalances by remember { mutableStateOf<Map<String, Double>>(emptyMap()) }
    var selectedMonthId by remember { mutableStateOf(currentMonthId()) }
    var selectedMonthSavings by remember { mutableStateOf(SavingsMonthData(0.0, emptyMap())) }
    var showAddDialog by remember { mutableStateOf(false) }
    var isSaving by remember { mutableStateOf(false) }

    LaunchedEffect(uid) {
        if (uid == null) return@LaunchedEffect
        walletBalances = runCatching { getUserWalletBalances(uid) }.getOrDefault(emptyMap())
    }

    LaunchedEffect(uid, selectedMonthId) {
        if (uid == null) return@LaunchedEffect
        selectedMonthSavings = runCatching {
            getSavingsForMonth(uid, selectedMonthId)
        }.getOrDefault(SavingsMonthData(0.0, emptyMap()))
    }

    val totalWalletBalance = walletBalances.values.sum()
    val selectedMonthLabel = monthOptions.firstOrNull { it.id == selectedMonthId }?.label ?: selectedMonthId

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
                Text(
                    text = "Savings",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White
                )
                Row(
                    modifier = Modifier
                        .padding(horizontal = 14.dp, vertical = 10.dp)
                        .clickable { showAddDialog = true },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Image(
                        painter = painterResource(R.drawable.addbudget),
                        contentDescription = "Add savings",
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFF5F7FF))
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Total Balance",
                color = Color(0xFF6D6D7B),
                fontWeight = FontWeight.SemiBold,
                fontSize = 16.sp
            )
            Text(
                text = formatSavingsMoney(totalWalletBalance),
                fontWeight = FontWeight.ExtraBold,
                fontSize = 42.sp,
                color = Color(0xFF232342)
            )

            Spacer(modifier = Modifier.height(16.dp))
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                color = Color.White
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "This Month Savings",
                        color = Color(0xFF6D6D7B),
                        fontSize = 15.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "$selectedMonthLabel (${selectedMonthId})",
                        color = Color(0xFF4A4A4A),
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = formatSavingsMoney(selectedMonthSavings.totalSavings),
                        fontWeight = FontWeight.Bold,
                        fontSize = 30.sp,
                        color = Color(0xFF232342)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))
            if (selectedMonthSavings.walletSavings.isEmpty()) {
                Text(
                    text = "No savings added yet",
                    color = Color.Gray,
                    fontWeight = FontWeight.Medium
                )
            } else {
                selectedMonthSavings.walletSavings.forEach { (walletName, amount) ->
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 10.dp),
                        shape = RoundedCornerShape(14.dp),
                        color = Color.White
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = walletName, fontWeight = FontWeight.SemiBold, color = Color(0xFF232342))
                            Text(text = formatSavingsMoney(amount), fontWeight = FontWeight.Bold, color = Color(0xFF232342))
                        }
                    }
                }
            }
        }

        if (showAddDialog) {
            var monthMenuExpanded by remember { mutableStateOf(false) }
            var walletMenuExpanded by remember { mutableStateOf(false) }
            var dialogMonthId by remember { mutableStateOf(selectedMonthId.ifBlank { currentMonthId() }) }
            var selectedWalletName by remember { mutableStateOf(walletBalances.keys.firstOrNull().orEmpty()) }
            var amountText by remember { mutableStateOf("") }

            AlertDialog(
                onDismissRequest = { if (!isSaving) showAddDialog = false },
                title = { Text("Add Savings") },
                text = {
                    Column {
                        Box(modifier = Modifier.wrapContentSize()) {
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { monthMenuExpanded = true },
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
                                        text = monthOptions.firstOrNull { it.id == dialogMonthId }?.label ?: "Choose month",
                                        color = Color(0xFF232342)
                                    )
                                    Icon(
                                        imageVector = Icons.Default.KeyboardArrowDown,
                                        contentDescription = "Open month"
                                    )
                                }
                            }
                            DropdownMenu(
                                expanded = monthMenuExpanded,
                                onDismissRequest = { monthMenuExpanded = false }
                            ) {
                                monthOptions.forEach { option ->
                                    DropdownMenuItem(
                                        text = { Text(option.label) },
                                        onClick = {
                                            dialogMonthId = option.id
                                            monthMenuExpanded = false
                                        }
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Box(modifier = Modifier.wrapContentSize()) {
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { walletMenuExpanded = true },
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
                                        text = selectedWalletName.ifBlank { "Choose wallet" },
                                        color = Color(0xFF232342)
                                    )
                                    Icon(
                                        imageVector = Icons.Default.KeyboardArrowDown,
                                        contentDescription = "Open wallet"
                                    )
                                }
                            }
                            DropdownMenu(
                                expanded = walletMenuExpanded,
                                onDismissRequest = { walletMenuExpanded = false }
                            ) {
                                walletBalances.keys.forEach { walletName ->
                                    DropdownMenuItem(
                                        text = { Text(walletName) },
                                        onClick = {
                                            selectedWalletName = walletName
                                            walletMenuExpanded = false
                                        }
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        OutlinedTextField(
                            value = amountText,
                            onValueChange = { amountText = it },
                            label = { Text("Amount") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                },
                confirmButton = {
                    Button(
                        enabled = !isSaving,
                        onClick = {
                            if (uid == null) return@Button
                            val amount = amountText.trim().toDoubleOrNull()
                            if (selectedWalletName.isBlank() || amount == null || amount <= 0.0) {
                                android.widget.Toast.makeText(
                                    context,
                                    "Select wallet and valid amount.",
                                    android.widget.Toast.LENGTH_SHORT
                                ).show()
                                return@Button
                            }
                            isSaving = true
                            scope.launch {
                                runCatching {
                                    addSavingsForWallet(
                                        uid = uid,
                                        monthId = dialogMonthId,
                                        walletName = selectedWalletName,
                                        amount = amount
                                    )
                                }.onSuccess {
                                    selectedMonthId = dialogMonthId
                                    selectedMonthSavings = getSavingsForMonth(uid, dialogMonthId)
                                    walletBalances = getUserWalletBalances(uid)
                                    showAddDialog = false
                                    android.widget.Toast.makeText(
                                        context,
                                        "Savings added",
                                        android.widget.Toast.LENGTH_SHORT
                                    ).show()
                                }.onFailure {
                                    android.widget.Toast.makeText(
                                        context,
                                        it.message ?: "Failed to add savings",
                                        android.widget.Toast.LENGTH_SHORT
                                    ).show()
                                }
                                isSaving = false
                            }
                        }
                    ) {
                        Text(if (isSaving) "Saving..." else "Save")
                    }
                },
                dismissButton = {
                    Button(
                        enabled = !isSaving,
                        onClick = { showAddDialog = false }
                    ) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}
