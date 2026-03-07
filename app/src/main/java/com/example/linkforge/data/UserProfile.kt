package com.example.linkforge.data

/**
 * Cached user profile (name, email) so we don't need to call Firebase repeatedly.
 */
data class UserProfile(
    val displayName: String,
    val email: String,
    val wallets: List<Wallet> = emptyList()
)

data class Wallet(
    val id: String = "",
    val name: String = "",
    val balance: Double = 0.0,
    val isDefault: Boolean = false
)
