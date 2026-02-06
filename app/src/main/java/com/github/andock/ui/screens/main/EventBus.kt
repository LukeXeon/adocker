package com.github.andock.ui.screens.main

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateMapOf
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.Channel.Factory.BUFFERED
import kotlinx.coroutines.flow.consumeAsFlow
import kotlinx.coroutines.withContext
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty
import kotlin.uuid.ExperimentalUuidApi

class EventBus {
    private val _channels = mutableStateMapOf<Key<*>, Channel<Any?>>()

    val channels: Map<Key<*>, Channel<Any?>>
        get() = _channels

    @Composable
    inline fun <reified T> subscribe(
        key: Key<T>,
        crossinline onResult: suspend (T) -> Unit
    ): Boolean {
        val channel = channels[key] ?: return false
        LaunchedEffect(channel) {
            channel.consumeAsFlow().collect { result ->
                onResult(result as T)
            }
        }
        return true
    }

    suspend fun <T> send(key: Key<T>, result: T) {
        withContext(Dispatchers.Main.immediate) {
            _channels.getOrPut(key) {
                Channel(
                    capacity = BUFFERED,
                    onBufferOverflow = BufferOverflow.SUSPEND
                )
            }
        }.send(result)
    }

    data class Key<T>(val name: String, val default: T)

    companion object {

        @OptIn(ExperimentalUuidApi::class)
        fun <T> key(initialValue: T): ReadOnlyProperty<Any?, Key<T>> {
            return object : ReadOnlyProperty<Any?, Key<T>> {
                @Volatile
                private var value: Key<T>? = null

                override fun getValue(thisRef: Any?, property: KProperty<*>): Key<T> {
                    val v = value
                    if (v == null) {
                        synchronized(this) {
                            var v2 = value
                            if (v2 == null) {
                                v2 = Key(property.name, initialValue)
                                value = v2
                            }
                            return v2
                        }
                    } else {
                        return v
                    }
                }
            }
        }
    }
}