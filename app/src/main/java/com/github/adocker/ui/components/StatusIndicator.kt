package com.github.adocker.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.github.adocker.daemon.database.model.ContainerStatus

@Composable
fun StatusIndicator(
    status: ContainerStatus,
    modifier: Modifier = Modifier
) {
    val color = when (status) {
        ContainerStatus.RUNNING -> Color(0xFF4CAF50)
        ContainerStatus.STOPPED -> Color(0xFFF44336)
        ContainerStatus.PAUSED -> Color(0xFFFF9800)
        ContainerStatus.CREATED -> Color(0xFF2196F3)
        ContainerStatus.EXITED -> Color(0xFF9E9E9E)
    }

    Box(
        modifier = modifier
            .size(12.dp)
            .clip(CircleShape)
            .background(color)
    )
}
