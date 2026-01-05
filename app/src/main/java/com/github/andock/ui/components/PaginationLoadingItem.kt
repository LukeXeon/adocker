package com.github.andock.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.github.andock.ui.theme.Spacing

@Composable
fun PaginationLoadingItem() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(Spacing.Medium),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator()
    }
}