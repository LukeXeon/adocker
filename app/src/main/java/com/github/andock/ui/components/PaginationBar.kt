package com.github.andock.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.github.andock.R

@Composable
fun PaginationBar(
    currentPage: Int,
    totalPages: Int,
    onPageChange: (Int) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Previous button
        TextButton(
            onClick = { if (currentPage > 1) onPageChange(currentPage - 1) },
            enabled = currentPage > 1
        ) {
            Text(stringResource(R.string.pagination_prev))
        }

        // Page numbers
        val pageRange = when {
            totalPages <= 5 -> 1..totalPages
            currentPage <= 3 -> 1..5
            currentPage >= totalPages - 2 -> (totalPages - 4)..totalPages
            else -> (currentPage - 2)..(currentPage + 2)
        }

        pageRange.forEach { page ->
            TextButton(
                onClick = { onPageChange(page) },
                colors = if (page == currentPage) {
                    ButtonDefaults.textButtonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    ButtonDefaults.textButtonColors()
                },
                modifier = Modifier.padding(horizontal = 2.dp)
            ) {
                Text("$page")
            }
        }

        // Next button
        TextButton(
            onClick = { if (currentPage < totalPages) onPageChange(currentPage + 1) },
            enabled = currentPage < totalPages
        ) {
            Text(stringResource(R.string.pagination_next))
        }
    }
}