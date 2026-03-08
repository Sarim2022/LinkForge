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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.linkforge.R
import com.example.linkforge.data.Transaction
import com.google.firebase.Timestamp
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

private val TransactionRowBackground = Color(0xFFE3F2FD)
private val IncomeAmountColor = Color(0xFF2E7D32)
private val ExpenseAmountColor = Color(0xFFC62828)

@Composable
fun JourneyDescScreen(
    journeyType: String,
    totalAmount: Double,
    onBack: () -> Unit,
    uid: String?
) {
    var journeyTransactions by remember { mutableStateOf<List<Transaction>>(emptyList()) }
    val title = when (journeyType) {
        "income" -> "Income"
        "expense" -> "Expense"
        "lend" -> "Lend"
        "borrow" -> "Borrow"
        else -> journeyType.replaceFirstChar { it.uppercase() }
    }

    LaunchedEffect(uid, journeyType) {
        if (uid == null) return@LaunchedEffect
        Firebase.firestore
            .collection("users").document(uid).collection("transactions")
            .get()
            .addOnSuccessListener { snapshot ->
                val list = snapshot.documents.mapNotNull { doc ->
                    val type = doc.getString("type") ?: return@mapNotNull null
                    if (type != journeyType) return@mapNotNull null
                    val titleDoc = doc.getString("title") ?: ""
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
                        title = titleDoc,
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
                journeyTransactions = list
            }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
    ) {
        // Top bar with back button
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier
                    .padding(4.dp)
                    .clickable { onBack() },
                shape = RoundedCornerShape(12.dp),
                color = Color(0xFFF5F5F5)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("← Back", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF0345FC))
                }
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
        ) {
            // Journey type name
            Text(
                text = title,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black
            )
            Spacer(Modifier.height(8.dp))

            // Total amount
            Text(
                text = "Total: ₹${if (totalAmount == totalAmount.toLong().toDouble()) totalAmount.toLong() else totalAmount}",
                fontSize = 20.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF0345FC)
            )
            Spacer(Modifier.height(24.dp))

            Text(
                text = "Transactions",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Blue
            )
            Spacer(Modifier.height(12.dp))
        }

        // Transactions list
        if (journeyTransactions.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No $title transactions yet.",
                    fontSize = 15.sp,
                    color = Color.Gray
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(
                    items = journeyTransactions,
                    key = { it.transactionId }
                ) { transaction ->
                    JourneyDescTransactionRow(transaction = transaction)
                }
            }
        }
    }
}

@Composable
private fun JourneyDescTransactionRow(transaction: Transaction) {
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
        modifier = Modifier.fillMaxWidth(),
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
                    .size(44.dp)
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
