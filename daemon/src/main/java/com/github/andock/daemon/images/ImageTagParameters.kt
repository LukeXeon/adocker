package com.github.andock.daemon.images

data class ImageTagParameters(
    val registry: String,
    val repository: String,
    val pageSize: Int,
    val last: String?
)