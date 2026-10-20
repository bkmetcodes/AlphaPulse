package com.bekacodes.alphapulse.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bekacodes.alphapulse.ui.theme.Primary
import com.bekacodes.alphapulse.ui.theme.TextPrimary
import com.bekacodes.alphapulse.ui.theme.TextSecondary

@Composable
fun TabHeaderItem(
    title: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier.clickable(onClick = onClick),
        horizontalAlignment = Alignment.Start
    ) {
        Text(
            text = title,
            color = if (selected) TextPrimary else TextSecondary,
            fontSize = 14.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
        )
        Spacer(Modifier.height(10.dp))
        Box(
            Modifier
                .width(88.dp)
                .height(3.dp)
                .background(
                    if (selected) Primary else Color.Transparent,
                    RoundedCornerShape(2.dp)
                )
        )
    }
}