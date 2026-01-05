package com.github.andock.daemon.search

import io.ktor.http.Url

data class SearchKey(
    val url: Url,
    val names: Set<String>
)