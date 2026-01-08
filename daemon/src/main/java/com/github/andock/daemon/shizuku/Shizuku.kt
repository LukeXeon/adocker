package com.github.andock.daemon.shizuku

import android.content.pm.PackageManager
import androidx.collection.MutableIntObjectMap
import kotlinx.coroutines.CompletableDeferred
import rikka.shizuku.Shizuku
import timber.log.Timber
import kotlin.random.Random


/**
 * Check if Shizuku is available
 */
val isAvailable: Boolean
    get() {
        if (Shizuku.isPreV11()) {
            Timber.w("Shizuku is pre v11")
            return false
        }
        return try {
            Shizuku.pingBinder()
        } catch (e: Exception) {
            Timber.w(e, "Shizuku is not available")
            false
        }
    }

/**
 * Check if we have Shizuku permission
 */
val hasPermission: Boolean
    get() {
        return if (isAvailable) {
            Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
        } else {
            false
        }
    }

private val requests = run {
    val map = MutableIntObjectMap<CompletableDeferred<Boolean>>()
    Shizuku.addRequestPermissionResultListener { requestCode, grantResult ->
        synchronized(map) {
            map.remove(requestCode)
        }?.complete(
            grantResult == PackageManager.PERMISSION_GRANTED
        )
    }
    return@run map
}

private fun nextRequestCode(): Pair<CompletableDeferred<Boolean>, Int> {
    val deferred = CompletableDeferred<Boolean>()
    synchronized(requests) {
        while (true) {
            val code = Random.nextInt(1, UShort.MAX_VALUE.toInt())
            if (!requests.containsKey(code)) {
                requests[code] = deferred
                return deferred to code
            }
        }
    }
}

/**
 * Request Shizuku permission
 */
suspend fun requestPermission(): Boolean {
    when {
        !isAvailable -> {
            return false
        }

        !hasPermission -> {
            val (deferred, code) = nextRequestCode()
            Shizuku.requestPermission(code)
            return deferred.await()
        }

        else -> {
            return true
        }
    }
}