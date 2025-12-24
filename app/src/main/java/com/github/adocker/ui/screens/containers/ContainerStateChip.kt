package com.github.adocker.ui.screens.containers

import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import com.github.adocker.daemon.containers.ContainerState

@Composable
fun ContainerStateChip(state: ContainerState) {
    val color = when (state) {
        is ContainerState.Created -> MaterialTheme.colorScheme.secondary
        is ContainerState.Starting -> MaterialTheme.colorScheme.secondary
        is ContainerState.Running -> MaterialTheme.colorScheme.primary
        is ContainerState.Stopping -> MaterialTheme.colorScheme.error
        is ContainerState.Exited -> MaterialTheme.colorScheme.error
        is ContainerState.Dead -> MaterialTheme.colorScheme.error
        is ContainerState.Removing -> MaterialTheme.colorScheme.error
        is ContainerState.Removed -> MaterialTheme.colorScheme.error
    }
    AssistChip(
        onClick = { },
        label = { Text(state::class.simpleName ?: "") },
        colors = AssistChipDefaults.assistChipColors(
            labelColor = color
        )
    )
}