package com.adocker.runner.ui.components

import android.os.Build
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.text.font.FontWeight

@Composable
fun PhantomProcessWarningDialog(
    onDismiss: () -> Unit,
    onNavigateToSettings: () -> Unit
) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
        // No warning needed for Android 11 and below
        return
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error
            )
        },
        title = {
            Text(
                text = "Process Restrictions Detected",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold
                )
            )
        },
        text = {
            Text(
                text = "Android 12+ limits background processes to 32. This may cause containers to terminate unexpectedly.\n\n" +
                        "We recommend disabling this restriction for optimal container performance.",
                style = MaterialTheme.typography.bodyMedium
            )
        },
        confirmButton = {
            Button(
                onClick = {
                    onDismiss()
                    onNavigateToSettings()
                }
            ) {
                Text("Open Settings")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Later")
            }
        }
    )
}
