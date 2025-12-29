package com.github.andock.ui.screens.search

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.github.andock.ui.theme.Spacing

@Composable
fun SearchFilterPanel(
    showOnlyOfficial: Boolean,
    minStars: Int,
    onToggleOfficial: () -> Unit,
    onMinStarsChange: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(
            modifier = Modifier.padding(Spacing.Medium),
            verticalArrangement = Arrangement.spacedBy(Spacing.Small)
        ) {
            Text(
                text = "Filters",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Official images only", style = MaterialTheme.typography.bodyMedium)
                Switch(
                    checked = showOnlyOfficial,
                    onCheckedChange = { onToggleOfficial() }
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Minimum stars: $minStars", style = MaterialTheme.typography.bodyMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(Spacing.Small)) {
                    FilledTonalButton(
                        onClick = { onMinStarsChange(0) },
                        enabled = minStars != 0
                    ) {
                        Text("Reset")
                    }
                    FilledTonalButton(onClick = { onMinStarsChange(10) }) { Text("10+") }
                    FilledTonalButton(onClick = { onMinStarsChange(100) }) { Text("100+") }
                    FilledTonalButton(onClick = { onMinStarsChange(1000) }) { Text("1000+") }
                }
            }
        }
    }
}