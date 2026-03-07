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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
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
import androidx.compose.ui.window.Dialog
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.height
import com.example.linkforge.R
import com.example.linkforge.data.ExpenseAndIncome
import com.example.linkforge.data.UserPreferences
import com.example.linkforge.data.Wallet
import com.example.linkforge.walletmanage.AddWalletCard
import com.example.linkforge.walletmanage.WalletCard
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import java.util.UUID

@Composable
fun HomeScreen() {
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
    val uid = Firebase.auth.currentUser?.uid

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
                val income = (doc.get("income") as? Number)?.toDouble() ?: 0.0
                val expense = (doc.get("expenses") as? Number)?.toDouble() ?: 0.0
                val lend = (doc.get("lend") as? Number)?.toDouble() ?: 0.0
                val borrow = (doc.get("borrow") as? Number)?.toDouble() ?: 0.0
                val journey = ExpenseAndIncome(income = income, expense = expense, lend = lend, borrow = borrow)
                expenseAndIncome = journey
                userPrefs.saveJourney(journey)
            }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(top = 0.dp)
    ) {
        HeaderCompoable(displayName = displayName)
        SearchbarUi()

        Text("Your Wallets", fontWeight = FontWeight.ExtraBold, color=Color.Blue, modifier = Modifier.padding(start=15.dp, top = 14.dp), fontSize = 16.sp)
        UserCardSection(
            walletList = walletList,
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
        JourneyLazyRow(expenseAndIncome = expenseAndIncome)
        ServicesMoneyAddonSection()
        Row(Modifier.fillMaxWidth().padding(horizontal = 15.dp, vertical = 10.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("Your Transactions", fontWeight = FontWeight.ExtraBold, color=Color.Blue, modifier = Modifier.padding(), fontSize = 18.sp)
            Image(painter = painterResource(R.drawable.plus),contentDescription = null,modifier=Modifier.size(40.dp))
        }

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
    val label: String,
    val amount: Double,
    val iconResId: Int,
    val backgroundColor: Color,
    val textColor: Color
)

@Composable
fun JourneyLazyRow(expenseAndIncome: ExpenseAndIncome) {
    val items = listOf(
        JourneyItem(
            "Income",
            expenseAndIncome.income,
            R.drawable.income,
            JourneyIncomeColor,
            JourneyTextOnLight
        ),
        JourneyItem(
            "Expense",
            expenseAndIncome.expense,
            R.drawable.expenses,
            JourneyExpenseColor,
            JourneyTextOnLight
        ),
        JourneyItem(
            "Lend",
            expenseAndIncome.lend,
            R.drawable.lend,
            JourneyLendColor,
            JourneyTextOnLight
        ),
        JourneyItem(
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
        items(items, key = { it.label }) { item ->
            Surface(
                modifier = Modifier.width(160.dp),
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
fun ServicesMoneyAddonSection() {
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
                .clickable(onClick = { }),
            shape = ServicesPillShape,
            color =  Color.White
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
                .clickable(onClick = { }),
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
                .clickable(onClick = { }),
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
            WalletCard(wallet = wallet)
        }
        item {
            AddWalletCard(onClick = { showDialog = true })
        }
    }


}



@Composable
fun HeaderCompoable(displayName: String = "User") {
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
        }
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(
                    color = Color.Blue,
                    shape = CircleShape
                ), contentAlignment = Alignment.Center
        )
        {
            Text(
                displayName.trim().take(2).uppercase(),
                fontSize = 18.sp,
                color = Color.White
            )
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
