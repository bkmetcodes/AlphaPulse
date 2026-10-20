package com.bekacodes.alphapulse.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bekacodes.alphapulse.ui.theme.Background
import com.bekacodes.alphapulse.ui.theme.Border
import com.bekacodes.alphapulse.ui.theme.Primary
import com.bekacodes.alphapulse.ui.theme.TextPrimary
import com.bekacodes.alphapulse.ui.theme.TextSecondary

@Composable
fun CommandBar(
    inputText: String,
    isProcessing: Boolean,
    onInputChanged: (String) -> Unit,
    onSend: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(Background)
            .navigationBarsPadding()
            .imePadding()
            .padding(horizontal = 16.dp, vertical = 10.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(58.dp)
                .clip(CircleShape)
                .background(Color(0xFF191D38))
                .border(1.dp, Primary, CircleShape)
                .padding(start = 16.dp, end = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text("✦", color = Color(0xFFA5B4FC), fontSize = 20.sp)

            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                if (inputText.isEmpty()) {
                    Text(
                        text = if (isProcessing) "Updating portfolio..." else "e.g. Add 5 shares of SNDK at $1400",
                        color = TextSecondary,
                        fontSize = 13.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                BasicTextField(
                    value = inputText,
                    onValueChange = onInputChanged,
                    enabled = !isProcessing,
                    singleLine = true,
                    textStyle = TextStyle(color = TextPrimary, fontSize = 13.sp),
                    cursorBrush = SolidColor(Primary),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(if (inputText.isNotBlank() && !isProcessing) Primary else Border)
                    .clickable(enabled = inputText.isNotBlank() && !isProcessing, onClick = onSend),
                contentAlignment = Alignment.Center
            ) {
                Text("↑", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}