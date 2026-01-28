package com.github.andock.startup.coroutines

import kotlin.coroutines.CoroutineContext

internal data class RootContext(
    val name: String
) : AbstractCoroutineContext(), CoroutineContext.Element {

    override val key: CoroutineContext.Key<*>
        get() = Key

    companion object Key : CoroutineContext.Key<RootContext>
}