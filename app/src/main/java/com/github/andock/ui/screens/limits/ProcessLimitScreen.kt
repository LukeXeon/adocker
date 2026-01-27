package com.github.andock.ui.screens.limits

import android.os.Build
import androidx.compose.foundation.layout.Column
import com.github.andock.ui.route.Route
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.github.andock.R
import com.github.andock.ui.screens.main.LocalNavController
import com.github.andock.ui.theme.Spacing
import com.github.andock.ui.utils.debounceClick

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProcessLimitScreen() {
    val navController = LocalNavController.current
    val isLimited = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.phantom_title)) },
                navigationIcon = {
                    IconButton(
                        onClick = debounceClick {
                            navController.popBackStack()
                        }
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.action_back)
                        )
                    }
                }
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(Spacing.Medium)
        ) {
            if (isLimited) {
                ProcessLimited()
            } else {
                ProcessUnlimit()
            }
        }
    }
}