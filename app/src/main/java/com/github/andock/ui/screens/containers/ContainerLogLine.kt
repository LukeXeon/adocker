package com.github.andock.ui.screens.containers

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.github.andock.ui.theme.Spacing
import com.github.andock.ui.utils.formatDate

@Composable
fun ContainerLogLine(
    timestamp: Long,
    isError: Boolean,
    message: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = if (isError) {
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.error
            )
        } else {
            CardDefaults.cardColors()
        },
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier.padding(Spacing.Medium),
            verticalArrangement = Arrangement.spacedBy(Spacing.ExtraSmall)
        ) {
            Text(
                text = formatDate(timestamp),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Normal
            )
        }
    }
}