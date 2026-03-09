package com.example.linkforge.Screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.linkforge.R
import com.example.linkforge.data.UserPreferences

private data class SettingsItem(
    val label: String
)

@Composable
fun ProfileScreen() {
    val context = LocalContext.current
    val user = UserPreferences(context).getUser()
    val displayName = user?.displayName?.ifBlank { "User Name" } ?: "User Name"
    val email = user?.email?.ifBlank { "email@example.com" } ?: "email@example.com"

    val settingsItems = listOf(
        SettingsItem("User Profile"),
        SettingsItem("Categories Management"),
        SettingsItem("My Wallets"),
        SettingsItem("My Journeys"),
        SettingsItem("My reminders"),
        SettingsItem("See transactions"),
        SettingsItem("Export data"),
        SettingsItem("Clear All data"),
        SettingsItem("Delete Account"),
        SettingsItem("Account security"),
        SettingsItem("About")
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .background(Color(0xFFF5F5F5))
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 18.dp, vertical = 18.dp)
    ) {
        Text(
            text = "Profile",
            fontSize = 30.sp,
            color = Color.Black,
            fontWeight = FontWeight.ExtraBold,
            modifier = Modifier.padding(top = 4.dp, bottom = 15.dp)
        )

        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            color = Color.White
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { }
                    .padding(horizontal = 14.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Image(
                    painter = painterResource(R.drawable.user),
                    contentDescription = "User",
                    modifier = Modifier.size(48.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = displayName,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF222222)
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = email,
                        fontSize = 13.sp,
                        color = Color(0xFF7A7A7A)
                    )
                }
                Image(
                    painter = painterResource(R.drawable.rightarrow),
                    contentDescription = "Open profile",
                    modifier = Modifier.size(16.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        Text(
            text = "Account Settings",
            fontSize = 22.sp,
            fontWeight = FontWeight.ExtraBold,
            color = Color(0xFF1D1D1D),
            modifier = Modifier.padding(bottom = 10.dp)
        )

        Surface(
            modifier = Modifier
                .fillMaxWidth(),
            shape = RoundedCornerShape(13.dp),
            color = Color.White,
            shadowElevation = 1.dp,
            tonalElevation = 0.dp
        ) {
            Column(modifier = Modifier.padding(vertical = 4.dp)) {
                settingsItems.forEachIndexed { index, item ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(onClick = { })
                            .padding(horizontal = 16.dp, vertical = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = item.label,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color.Black,
                            modifier = Modifier.weight(1f)
                        )
                        Box(contentAlignment = Alignment.Center) {
                            Image(
                                painter = painterResource(R.drawable.rightarrow),
                                contentDescription = "Open setting",
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                    if (index < settingsItems.lastIndex) {
                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = 12.dp),
                            color = Color(0xFFEEEEEE),
                            thickness = 1.dp
                        )
                    }
                }
            }
        }
    }
}
