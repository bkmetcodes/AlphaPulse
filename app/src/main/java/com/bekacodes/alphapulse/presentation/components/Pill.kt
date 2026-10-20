package com.bekacodes.alphapulse.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bekacodes.alphapulse.ui.theme.Negative
import com.bekacodes.alphapulse.ui.theme.Positive

@Composable
fun Pill(text: String, positive: Boolean) {
    Text(
        text = text,
        modifier = Modifier
            .background(if (positive) Color(0xFF102C27) else Color(0xFF321C2A), CircleShape)
            .padding(horizontal = 8.dp, vertical = 4.dp),
        color = if (positive) Positive else Negative,
        fontSize = if (text.length > 8) 12.sp else 11.sp,
        fontWeight = FontWeight.SemiBold
    )
}