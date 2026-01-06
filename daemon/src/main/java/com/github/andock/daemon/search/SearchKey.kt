package com.github.andock.daemon.search

import com.google.common.hash.BloomFilter
import io.ktor.http.Url

@Suppress("UnstableApiUsage")
data class SearchKey(
    val url: Url,
    val names: BloomFilter<CharSequence>?
)