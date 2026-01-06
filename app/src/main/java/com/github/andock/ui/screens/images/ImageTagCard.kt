package com.github.andock.ui.screens.images

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LocalMinimumInteractiveComponentSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.github.andock.ui.theme.Spacing

@Composable
fun ImageTagCard(
    name: String,
    onClick: () -> Unit
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val isActive = lifecycleOwner.lifecycle.currentStateFlow
        .collectAsState().value.isAtLeast(Lifecycle.State.RESUMED)
    Card(
        enabled = isActive,
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(
                    LocalMinimumInteractiveComponentSize.current
                )
                .padding(
                    horizontal = Spacing.Medium,
                    vertical = Spacing.Small
                ),
            contentAlignment = Alignment.CenterStart
        ) {
            Text(
                text = name,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold
            )
        }
    }
}