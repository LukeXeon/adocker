package com.github.adocker.ui.utils

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())

fun formatDate(timestamp: Long): String {
    return sdf.format(Date(timestamp))
}