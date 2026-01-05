package com.github.andock.ui.components

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp


val LocalBottomSpacerHeight = staticCompositionLocalOf {
    0.dp
}

@Composable
fun BottomSpacer(add: Dp = 0.dp) {
    Spacer(Modifier.height(LocalBottomSpacerHeight.current + add))
}