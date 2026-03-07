package com.example.linkforge.Screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.linkforge.R

/** Splash (logo only). Shown 1.5s at launch; navigation is handled in MainActivity. */
@Composable
fun SplashScreen() {
    val config = LocalConfiguration.current
    val screenWidthDp = config.screenWidthDp

    val horizontalPadding = maxOf(16.dp, (screenWidthDp * 0.05f).toInt().dp)
    val titleFontSize = (screenWidthDp / 12f).coerceIn(20f, 36f).sp
    val logoSize = (screenWidthDp / 10f).coerceIn(24f, 48f).dp

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0345FC))
            .windowInsetsPadding(WindowInsets.systemBars)
            .padding(horizontal = horizontalPadding),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Image(
                painter = painterResource(id = R.drawable.pull),
                contentDescription = null,
                modifier = Modifier.size(logoSize)
            )
            Spacer(Modifier.width(8.dp))
            Text(
                "LinkForge",
                fontWeight = FontWeight.ExtraBold,
                color = Color.White,
                fontSize = titleFontSize
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewSplash() {
    SplashScreen()
}
