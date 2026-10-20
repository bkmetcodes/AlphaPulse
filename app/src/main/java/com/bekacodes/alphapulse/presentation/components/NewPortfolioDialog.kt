package com.bekacodes.alphapulse.presentation.components

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.bekacodes.alphapulse.ui.theme.Border
import com.bekacodes.alphapulse.ui.theme.Primary
import com.bekacodes.alphapulse.ui.theme.SurfaceColor
import com.bekacodes.alphapulse.ui.theme.TextPrimary
import com.bekacodes.alphapulse.ui.theme.TextSecondary

@Composable
fun NewPortfolioDialog(
    onDismiss: () -> Unit,
    onCreate: (String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SurfaceColor,
        titleContentColor = TextPrimary,
        textContentColor = TextSecondary,
        title = { Text("Create New Portfolio", fontWeight = FontWeight.Bold, fontSize = 18.sp) },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                placeholder = { Text("e.g., Aerospace & Defense", color = TextSecondary) },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary,
                    focusedBorderColor = Primary,
                    unfocusedBorderColor = Border
                )
            )
        },
        confirmButton = {
            Button(
                onClick = { if (name.isNotBlank()) onCreate(name.trim()) },
                colors = ButtonDefaults.buttonColors(containerColor = Primary)
            ) {
                Text("Create", color = Color.White)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = TextSecondary)
            }
        }
    )
}