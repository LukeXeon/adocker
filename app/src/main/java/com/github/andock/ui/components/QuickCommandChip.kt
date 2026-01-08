package com.github.andock.ui.components

import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.SuggestionChipDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.sp

@Composable
fun QuickCommandChip(
    command: String,
    onExecute: (String) -> Unit
) {
    SuggestionChip(
        onClick = { onExecute(command) },
        label = {
            Text(
                text = command,
                fontFamily = FontFamily.Monospace,
                fontSize = 10.sp
            )
        },
        colors = SuggestionChipDefaults.suggestionChipColors(
            containerColor = Color(0xFF3D3D3D),
            labelColor = Color(0xFFE0E0E0)
        ),
        border = null
    )
}