package com.github.adocker.ui.screens.settings

data class PhantomProcessUiState(
    val shizukuAvailable: Boolean = false,
    val shizukuPermissionGranted: Boolean = false,
    val phantomKillerDisabled: Boolean = false,
    val currentLimit: Int? = null,
    val isChecking: Boolean = false,
    val isProcessing: Boolean = false,
    val error: String? = null,
    val successMessage: String? = null
)