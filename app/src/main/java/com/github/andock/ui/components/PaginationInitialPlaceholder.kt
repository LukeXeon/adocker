package com.github.andock.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.github.andock.ui.theme.Spacing


@Composable
fun PaginationInitialPlaceholder() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(bottom = Spacing.BottomSpacing),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator()
    }
}