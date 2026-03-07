package com.example.linkforge.Screens

import android.app.Activity
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.systemBars

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.linkforge.R
import com.example.linkforge.data.UserPreferences
import com.example.linkforge.data.UserProfile
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.auth.api.signin.GoogleSignInStatusCodes
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import java.util.UUID

private val ButtonShape = RoundedCornerShape(40.dp)

@Composable
fun AuthScreen(onNavigateToHome: () -> Unit = {}) {
    val context = LocalContext.current
    var isSigningIn by remember { mutableStateOf(false) }

    val webClientId = stringResource(R.string.default_web_client_id)
    val signInClient = remember(webClientId) {
        if (webClientId.isNotEmpty() && webClientId != "YOUR_WEB_CLIENT_ID") {
            val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(webClientId)
                .requestEmail()
                .build()
            GoogleSignIn.getClient(context, gso)
        } else null
    }

    val signInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        isSigningIn = false
        if (result.resultCode != Activity.RESULT_OK || result.data == null) {
            Toast.makeText(context, "Sign-in cancelled or failed", Toast.LENGTH_SHORT).show()
            return@rememberLauncherForActivityResult
        }
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        try {
            val account = task.getResult(ApiException::class.java)
            val idToken = account?.idToken
            if (idToken == null) {
                Toast.makeText(context, "Could not get account info", Toast.LENGTH_SHORT).show()
                return@rememberLauncherForActivityResult
            }
            val credential = GoogleAuthProvider.getCredential(idToken, null)
            Firebase.auth.signInWithCredential(credential)
                .addOnSuccessListener { authResult ->
                    val user = authResult.user
                    if (user != null) {
                        val profile = UserProfile(
                            displayName = user.displayName?.takeIf { it.isNotBlank() }
                                ?: user.email?.substringBefore('@').orEmpty().ifBlank { "User" },
                            email = user.email.orEmpty()
                        )
                        UserPreferences(context).saveUser(profile)
                        handleSignUpSuccess(user, onNavigateToHome) { msg ->
                            Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                        }
                    } else {
                        onNavigateToHome()
                    }
                }
                .addOnFailureListener { e ->
                    Toast.makeText(
                        context,
                        "Sign-in failed: ${e.message ?: "Unknown error"}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
        } catch (e: ApiException) {
            val msg = when (e.statusCode) {
                GoogleSignInStatusCodes.SIGN_IN_CANCELLED -> "Sign-in cancelled"
                GoogleSignInStatusCodes.SIGN_IN_FAILED -> "Sign-in failed"
                else -> "Sign-in error: ${e.message ?: "Unknown error"}"
            }
            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
        }
    }

    val config = LocalConfiguration.current
    val screenWidthDp = config.screenWidthDp
    val screenHeightDp = config.screenHeightDp

    val horizontalPadding = maxOf(16.dp, (screenWidthDp * 0.05f).toInt().dp)
    val topPadding = maxOf(24.dp, (screenHeightDp * 0.03f).toInt().dp)
    // Extra bottom padding so "Connect with Google" stays above media/player overlays
    val bottomPadding = maxOf(72.dp, (screenHeightDp * 0.06f).toInt().dp)
    // Example for titleFontSize
    val titleFontSize = (screenWidthDp / 18f).coerceIn(16f, 24f).sp

// Example for descriptionFontSize
    val descriptionFontSize = (screenWidthDp / 7f).coerceIn(18f, 52f).sp

// Example for buttonFontSize
    val buttonFontSize = (screenWidthDp / 18f).coerceIn(16f, 22f).sp

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0345FC))
            .windowInsetsPadding(WindowInsets.systemBars)
            .padding(
                start = horizontalPadding,
                end = horizontalPadding,
                top = topPadding,
                bottom = bottomPadding
            )
    ) {
        Text(
            "LinkForge",
            fontSize = titleFontSize,
            fontWeight = FontWeight.ExtraBold,
            color = Color.White,
            modifier = Modifier.clickable {
                onNavigateToHome() // This triggers the redirect
            }
        )
        Spacer(Modifier.height(16.dp))

        Text(
            "Linking your daily spending to a stronger, more secure financial future.",
            fontSize = descriptionFontSize,
            lineHeight = (descriptionFontSize.value * 1.3f).sp,
            fontWeight = FontWeight.ExtraBold,
            color = Color.White,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.weight(1f))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .clip(ButtonShape)
                .background(Color.White)
                .clickable(enabled = !isSigningIn) {
                    if (signInClient != null) {
                        isSigningIn = true
                        signInLauncher.launch(signInClient.signInIntent)
                    } else {
                        Toast.makeText(
                            context,
                            "Google Sign-In not configured. Add Web Client ID in app settings.",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            if (isSigningIn) {
                CircularProgressIndicator(
                    modifier = Modifier.size(32.dp),
                    color = Color(0xFF0345FC),
                    strokeWidth = 2.dp
                )
            } else {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.googlelogo),
                        contentDescription = null,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(Modifier.width(12.dp))
                    Text(
                        text = "Connect with Google",
                        fontSize = buttonFontSize,
                        color = Color(0xFF1F1F1F), // Professional Dark Gray
                        fontFamily = FontFamily.SansSerif, // Lowercase 'f'
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

private fun handleSignUpSuccess(
    firebaseUser: FirebaseUser,
    navigateToHome: () -> Unit,
    onError: (String) -> Unit = {}
) {
    val db = Firebase.firestore
    val userRef = db.collection("users").document(firebaseUser.uid)

    userRef.get().addOnSuccessListener { document ->
        if (!document.exists()) {
            val initialData = hashMapOf(
                "uid" to firebaseUser.uid,
                "username" to (firebaseUser.displayName ?: "New User"),
                "wallets" to listOf(
                    hashMapOf(
                        "id" to UUID.randomUUID().toString(),
                        "name" to "Default Wallet",
                        "balance" to 0.0,
                        "isDefault" to true
                    )
                ),
                "income" to 0.0,
                "expenses" to 0.0,
                "lend" to 0.0,
                "borrow" to 0.0,
                "savings" to 0.0,
                "budget" to emptyList<Map<String, Any>>(),
                "reminders" to emptyList<Map<String, Any>>()
            )
            userRef.set(initialData)
                .addOnSuccessListener { navigateToHome() }
                .addOnFailureListener { e ->
                    Log.e("Firestore", "Error creating user document", e)
                    onError("Could not save profile. Check Firestore rules.")
                    navigateToHome()
                }
        } else {
            navigateToHome()
        }
    }.addOnFailureListener { e ->
        Log.e("Firestore", "Error checking user document", e)
        onError("Could not load profile. Check Firestore rules.")
        navigateToHome()
    }
}

@Composable
@Preview(showBackground = true)
fun PreviewAuth() {
    AuthScreen()
}
