package com.bekacodes.alphapulse.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bekacodes.alphapulse.presentation.models.WatchlistItemUiModel
import com.bekacodes.alphapulse.presentation.formatCurrency
import com.bekacodes.alphapulse.ui.theme.Border
import com.bekacodes.alphapulse.ui.theme.TextPrimary
import com.bekacodes.alphapulse.ui.theme.TextSecondary

@Composable
fun WatchlistRow(item: WatchlistItemUiModel) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(66.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Text(item.symbol, color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Text(
                item.companyName,
                color = TextSecondary,
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Sparkline(item.sparkline, item.isRising, Modifier.size(width = 66.dp, height = 30.dp))
        Column(
            modifier = Modifier.width(122.dp),
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Text(
                text = formatCurrency(item.currentPrice),
                color = TextPrimary,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold
            )
            val sign = if (item.isRising) "+" else ""
            Pill("$sign${String.format("%.2f%%", item.dayChangePercent)}", item.isRising)
        }
    }
    Box(
        Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(Border)
    )
}