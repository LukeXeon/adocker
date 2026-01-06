package com.github.andock.ui.route


@Retention(AnnotationRetention.BINARY)
annotation class DeepLink(
    val uriPattern: String = "",
    val action: String = "",
    val mimeType: String = "",
)
