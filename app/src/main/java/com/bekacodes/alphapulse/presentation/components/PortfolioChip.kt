package com.bekacodes.alphapulse.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.bekacodes.alphapulse.ui.theme.Border
import com.bekacodes.alphapulse.ui.theme.Primary
import com.bekacodes.alphapulse.ui.theme.TextSecondary

@Composable
fun PortfolioChip(label: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        Modifier
            .height(32.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(if (selected) Primary else Background)
            .border(1.dp, if (selected) Primary else Border, RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 13.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = if (selected) Color.White else TextSecondary,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium
        )
    }
}