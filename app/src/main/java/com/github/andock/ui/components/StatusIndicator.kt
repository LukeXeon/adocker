package com.github.andock.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.github.andock.daemon.containers.ContainerState

@Composable
fun StatusIndicator(
    state: ContainerState,
    modifier: Modifier = Modifier
) {
    val color = when (state) {
        is ContainerState.Running -> Color(0xFF4CAF50)  // Green for running
        is ContainerState.Created,
        is ContainerState.Starting -> Color(0xFF2196F3)  // Blue for created/starting
        is ContainerState.Exited,
        is ContainerState.Stopping,
        is ContainerState.Dead,
        is ContainerState.Removing,
        is ContainerState.Removed -> Color(0xFF9E9E9E)  // Gray for stopped states
    }

    Box(
        modifier = modifier
            .size(12.dp)
            .clip(CircleShape)
            .background(color)
    )
}
