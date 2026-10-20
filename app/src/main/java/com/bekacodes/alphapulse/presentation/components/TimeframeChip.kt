package com.bekacodes.alphapulse.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bekacodes.alphapulse.ui.theme.Background
import com.bekacodes.alphapulse.ui.theme.Primary
import com.bekacodes.alphapulse.ui.theme.TextSecondary

@Composable
fun TimeframeChip(label: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        Modifier
            .size(width = 42.dp, height = 28.dp)
            .clip(CircleShape)
            .background(if (selected) Primary else Background)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = if (selected) Color.White else TextSecondary,
            fontSize = 12.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
        )
    }
}