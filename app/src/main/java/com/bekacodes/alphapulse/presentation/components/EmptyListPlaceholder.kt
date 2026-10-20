package com.bekacodes.alphapulse.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bekacodes.alphapulse.ui.theme.Border
import com.bekacodes.alphapulse.ui.theme.SurfaceColor
import com.bekacodes.alphapulse.ui.theme.TextSecondary

@Composable
fun EmptyListPlaceholder(message: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 28.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(SurfaceColor)
            .border(1.dp, Border, RoundedCornerShape(12.dp))
            .padding(20.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = message,
            color = TextSecondary,
            fontSize = 12.sp,
            lineHeight = 18.sp
        )
    }
}