package com.github.andock.daemon.images

data class ImageTagPagingKey(
    val registry: String,
    val repository: String,
    val pageSize: Int,
    val last: String?
)