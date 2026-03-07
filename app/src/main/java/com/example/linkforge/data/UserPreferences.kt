package com.example.linkforge.data

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

private const val PREFS_NAME = "linkforge_user_prefs"
private const val KEY_DISPLAY_NAME = "display_name"
private const val KEY_EMAIL = "email"
private const val KEY_WALLETS = "wallets"
private const val KEY_JOURNEY = "journey"

private val gson = Gson()
private val walletListType = object : TypeToken<List<Wallet>>() {}.type

class UserPreferences(context: Context) {
    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun saveUser(profile: UserProfile) {
        prefs.edit()
            .putString(KEY_DISPLAY_NAME, profile.displayName)
            .putString(KEY_EMAIL, profile.email)
            .apply()
    }

    fun getUser(): UserProfile? {
        val name = prefs.getString(KEY_DISPLAY_NAME, null) ?: return null
        val email = prefs.getString(KEY_EMAIL, "") ?: ""
        return UserProfile(displayName = name, email = email)
    }

    fun saveWallets(wallets: List<Wallet>) {
        prefs.edit()
            .putString(KEY_WALLETS, gson.toJson(wallets))
            .apply()
    }

    fun getWallets(): List<Wallet> {
        val json = prefs.getString(KEY_WALLETS, null) ?: return emptyList()
        return try {
            gson.fromJson(json, walletListType) ?: emptyList()
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun saveJourney(data: ExpenseAndIncome) {
        prefs.edit()
            .putString(KEY_JOURNEY, gson.toJson(data))
            .apply()
    }

    fun getJourney(): ExpenseAndIncome? {
        val json = prefs.getString(KEY_JOURNEY, null) ?: return null
        return try {
            gson.fromJson(json, ExpenseAndIncome::class.java)
        } catch (_: Exception) {
            null
        }
    }

    fun clearUser() {
        prefs.edit()
            .remove(KEY_DISPLAY_NAME)
            .remove(KEY_EMAIL)
            .remove(KEY_WALLETS)
            .remove(KEY_JOURNEY)
            .apply()
    }
}
