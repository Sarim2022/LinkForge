package com.example.linkforge.component


import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.FontWeight

@Composable
fun YourButton(
    text: String,
    onClick: () -> Unit,
    textColor: Color = Color.White,
    backgroundColor: Color = Color(0xFF1976D2) // Default Blue
) {
    Button(
        onClick = onClick,
        shape = RoundedCornerShape(30.dp), // Creates the pill shape
        colors = ButtonDefaults.buttonColors(
            containerColor = backgroundColor,
            contentColor = textColor
        ),
        // Adding internal padding to give the text breathing room
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
    ) {
        Text(
            text = text,
            fontSize = 10.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewHOME() {
    // Now you can call it with just the required parameters
    YourButton(
        text = "Sign In",
        onClick = { /* Handle click */ }
    )
}

fun processQuestion(question: String): String {

    return when {
        question.contains("today transaction") ->
            "Today you spent ₹220 and earned ₹1010."

        question.contains("profit this month") ->
            "Yes, you made a profit of ₹790 this month."

        question.contains("saving") ->
            "You saved ₹500 this month."

        else ->
            "I couldn't understand. Try asking about transactions or savings."
    }
}