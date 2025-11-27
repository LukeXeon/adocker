package com.github.adocker.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.github.adocker.ui.theme.Spacing
import com.github.adocker.ui.theme.IconSize
import com.github.adocker.R
import com.github.adocker.daemon.registry.model.SearchResult

@Composable
fun SearchResultCard(
    result: SearchResult,
    onPull: () -> Unit,
    modifier: Modifier = Modifier,
    isPulling: Boolean = false
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Spacing.Medium),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Spacing.Small)
                ) {
                    Text(
                        text = result.repoName ?: "Unknown",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    if (result.isOfficial) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = MaterialTheme.colorScheme.primaryContainer
                        ) {
                            Text(
                                text = stringResource(R.string.images_official),
                                style = MaterialTheme.typography.labelSmall,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                result.shortDescription?.let { description ->
                    if (description.isNotBlank()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = description,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(Spacing.Small)
                ) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = null,
                        modifier = Modifier.size(IconSize.Small),
                        tint = Color(0xFFFFB300)
                    )
                    Text(
                        text = "${result.starCount}",
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }

            IconButton(
                onClick = onPull,
                enabled = !isPulling
            ) {
                Icon(
                    imageVector = Icons.Default.CloudDownload,
                    contentDescription = stringResource(R.string.action_pull),
                    tint = if (isPulling) MaterialTheme.colorScheme.onSurfaceVariant
                    else MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}
