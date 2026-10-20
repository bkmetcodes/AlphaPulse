package com.bekacodes.alphapulse.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bekacodes.alphapulse.presentation.models.NewsArticleUiModel
import com.bekacodes.alphapulse.ui.theme.Border
import com.bekacodes.alphapulse.ui.theme.Negative
import com.bekacodes.alphapulse.ui.theme.Positive
import com.bekacodes.alphapulse.ui.theme.SurfaceColor
import com.bekacodes.alphapulse.ui.theme.TextPrimary
import com.bekacodes.alphapulse.ui.theme.TextSecondary

@Composable
fun NewsCard(
    article: NewsArticleUiModel,
    onClick: () -> Unit
) {
    val isPositive = article.symbolChangePercent >= 0
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(104.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(SurfaceColor)
            .border(1.dp, Border, RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Text(
                "${article.source} · ${article.publishedAgo}",
                color = TextSecondary,
                fontSize = 10.sp
            )
            Text(
                text = article.title,
                color = TextPrimary,
                fontSize = 13.sp,
                lineHeight = 16.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            val sign = if (isPositive) "+" else ""
            Text(
                text = "[${article.relatedSymbol} $sign${
                    String.format(
                        "%.1f%%",
                        article.symbolChangePercent
                    )
                }]",
                modifier = Modifier
                    .background(
                        if (isPositive) Color(0xFF102C27) else Color(0xFF321C2A),
                        RoundedCornerShape(4.dp)
                    )
                    .padding(horizontal = 6.dp, vertical = 2.dp),
                color = if (isPositive) Positive else Negative,
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
        // Ticker Monogram Thumbnail (replaces missing R.drawable resource so it compiles cleanly)
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(
                    Brush.linearGradient(
                        listOf(Color(0xFF1E293B), Color(0xFF312E81))
                    )
                )
                .border(1.dp, Border, RoundedCornerShape(10.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = article.relatedSymbol,
                color = Color(0xFFA5B4FC),
                fontSize = 13.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.ExtraBold
            )
        }
    }
}