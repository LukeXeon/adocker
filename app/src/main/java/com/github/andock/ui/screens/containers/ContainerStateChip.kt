package com.github.andock.ui.screens.containers

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.github.andock.daemon.containers.ContainerState

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
    Text(
        state::class.simpleName ?: "",
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.surface,
        maxLines = 1,
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(color)
            .padding(horizontal = 4.dp)
    )
}