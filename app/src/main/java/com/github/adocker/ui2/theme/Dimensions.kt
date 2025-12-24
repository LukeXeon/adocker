package com.github.adocker.ui2.theme

import androidx.compose.ui.unit.dp

/**
 * Material Design 3 统一间距和尺寸规范
 * 遵循Google官方推荐的8dp网格系统
 */
object Spacing {
    // 基础间距 - 8dp增量
    val ExtraSmall = 4.dp
    val Small = 8.dp
    val Medium = 16.dp
    val Large = 24.dp
    val ExtraLarge = 32.dp
    val Huge = 48.dp

    // 特殊用途间距
    val CardPadding = 16.dp
    val ScreenPadding = 16.dp
    val ListItemSpacing = 12.dp
    val SectionSpacing = 24.dp
    val ContentSpacing = 16.dp
}

object Elevation {
    val None = 0.dp
    val Level1 = 1.dp
    val Level2 = 3.dp
    val Level3 = 6.dp
    val Level4 = 8.dp
    val Level5 = 12.dp
}

object BorderRadius {
    val ExtraSmall = 4.dp
    val Small = 8.dp
    val Medium = 12.dp
    val Large = 16.dp
    val ExtraLarge = 28.dp
    val Full = 999.dp  // Fully rounded
}

object IconSize {
    val Small = 16.dp
    val Medium = 24.dp
    val Large = 32.dp
    val ExtraLarge = 48.dp
    val Huge = 64.dp
}
