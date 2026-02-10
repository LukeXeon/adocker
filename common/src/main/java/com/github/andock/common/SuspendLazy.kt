package com.github.andock.common

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.atomic.AtomicReferenceFieldUpdater

private object UninitializedValue

private class SynchronizedSuspendLazyImpl<out T>(
    initializer: suspend () -> T,
    lock: Mutex? = null
) : SuspendLazy<T> {
    private var initializer: (suspend () -> T)? = initializer

    @Volatile
    private var value: Any? = UninitializedValue

    // final field to ensure safe publication of 'SynchronizedLazyImpl' itself through
    // var lazy = lazy() {}
    private val lock = lock ?: Mutex()

    override suspend fun getValue(): T {
        val v1 = value
        if (v1 !== UninitializedValue) {
            @Suppress("UNCHECKED_CAST")
            return v1 as T
        }
        return lock.withLock {
            val v2 = value
            if (v2 !== UninitializedValue) {
                @Suppress("UNCHECKED_CAST") (v2 as T)
            } else {
                val typedValue = initializer!!.invoke()
                value = typedValue
                initializer = null
                typedValue
            }
        }
    }

    override fun isInitialized(): Boolean = value !== UninitializedValue

    override fun toString() = if (isInitialized()) {
        value.toString()
    } else {
        "Lazy value not initialized yet."
    }
}


private class SafePublicationSuspendLazyImpl<out T>(initializer: suspend () -> T) : SuspendLazy<T> {
    @Volatile
    private var initializer: (suspend () -> T)? = initializer

    @Volatile
    private var value: Any? = UninitializedValue

    // Artificial final field to ensure safe publication of 'SafePublicationLazyImpl' itself through
    // var lazy = lazy() {}
    @Suppress("unused")
    private val final: Any = UninitializedValue

    override suspend fun getValue(): T {
        val value = value
        if (value !== UninitializedValue) {
            @Suppress("UNCHECKED_CAST")
            return value as T
        }

        val initializerValue = initializer
        // if we see null in initializer here, it means that the value is already set by another thread
        if (initializerValue != null) {
            val newValue = initializerValue()
            if (valueUpdater.compareAndSet(
                    this,
                    UninitializedValue,
                    newValue
                )
            ) {
                initializer = null
                return newValue
            }
        }
        @Suppress("UNCHECKED_CAST")
        return value as T
    }

    override fun isInitialized(): Boolean = value !== UninitializedValue

    override fun toString(): String =
        if (isInitialized()) value.toString() else "Lazy value not initialized yet."

    companion object {
        private val valueUpdater = AtomicReferenceFieldUpdater.newUpdater(
            SafePublicationSuspendLazyImpl::class.java,
            Any::class.java,
            "value"
        )
    }
}


private class UnsafeSuspendLazyImpl<out T>(initializer: suspend () -> T) : SuspendLazy<T> {
    private var initializer: (suspend () -> T)? = initializer
    private var value: Any? = UninitializedValue

    override suspend fun getValue(): T {
        if (value === UninitializedValue) {
            value = initializer!!()
            initializer = null
        }
        @Suppress("UNCHECKED_CAST")
        return value as T
    }

    override fun isInitialized(): Boolean = value !== UninitializedValue

    override fun toString(): String =
        if (isInitialized()) {
            value.toString()
        } else {
            "Lazy value not initialized yet."
        }

}

private class AdaptSuspendLazyImpl<T>(
    private val lazy: Lazy<T>
) : SuspendLazy<T> {

    override fun isInitialized() = lazy.isInitialized()

    override suspend fun getValue(): T = lazy.value
}

sealed interface SuspendLazy<out T> {
    fun isInitialized(): Boolean

    suspend fun getValue(): T
}

fun <T> suspendLazy(lock: Mutex?, initializer: suspend () -> T): SuspendLazy<T> =
    SynchronizedSuspendLazyImpl(initializer, lock)

fun <T> suspendLazy(initializer: suspend () -> T): SuspendLazy<T> =
    SynchronizedSuspendLazyImpl(initializer)

fun <T> suspendLazy(mode: LazyThreadSafetyMode, initializer: suspend () -> T): SuspendLazy<T> =
    when (mode) {
        LazyThreadSafetyMode.SYNCHRONIZED -> SynchronizedSuspendLazyImpl(initializer)
        LazyThreadSafetyMode.PUBLICATION -> SafePublicationSuspendLazyImpl(initializer)
        LazyThreadSafetyMode.NONE -> UnsafeSuspendLazyImpl(initializer)
    }

fun <T> Lazy<T>.asSuspendLazy(): SuspendLazy<T> {
    return AdaptSuspendLazyImpl(this)
}

