package com.example.linkforge.data

/**
 * User's total income, expense, lend and borrow amounts (stored in Firebase per user).
 */
data class ExpenseAndIncome(
    val income: Double = 0.0,
    val expense: Double = 0.0,
    val lend: Double = 0.0,
    val borrow: Double = 0.0
)
