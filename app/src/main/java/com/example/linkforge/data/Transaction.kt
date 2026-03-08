package com.example.linkforge.data

/**
 * Represents a single transaction (stored in Firestore subcollection users/{uid}/transactions).
 * For income/expense: walletName, oldWalletMoney, newWalletMoney, category are set.
 * For lend/borrow: personName is set; wallet fields are empty/zero.
 */
data class Transaction(
    val transactionId: String = "",
    val title: String = "",
    val category: String = "",
    val type: String = "", // "income", "expense", "lend", "borrow"
    val amount: Double = 0.0,
    val walletName: String = "",
    val personName: String = "",
    val date: String = "",
    val oldWalletMoney: Double = 0.0,
    val newWalletMoney: Double = 0.0,
    val note: String = "",
    val createdAt: Long = 0L
)
