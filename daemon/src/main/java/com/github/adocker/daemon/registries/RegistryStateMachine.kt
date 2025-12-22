package com.github.adocker.daemon.registries

import android.os.SystemClock
import com.freeletics.flowredux2.ChangeableState
import com.freeletics.flowredux2.ChangedState
import com.freeletics.flowredux2.FlowReduxStateMachineFactory
import com.freeletics.flowredux2.initializeWith
import com.github.adocker.daemon.database.dao.RegistryDao
import com.github.adocker.daemon.database.model.RegistryType
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.ExperimentalCoroutinesApi
import timber.log.Timber
import javax.inject.Singleton

@OptIn(ExperimentalCoroutinesApi::class)
class RegistryStateMachine @AssistedInject constructor(
    @Assisted
    id: String,
    private val registryDao: RegistryDao,
    private val client: HttpClient,
    private val registryManager: RegistryManager,
) : FlowReduxStateMachineFactory<RegistryState, RegistryOperation>() {

    init {
        initializeWith { RegistryState.Checking(id, 0) }
        spec {
            inState<RegistryState.Unhealthy> {
                on<RegistryOperation.Check> {
                    override {
                        RegistryState.Checking(id, UNHEALTHY_THRESHOLD)
                    }
                }
                on<RegistryOperation.Remove> {
                    removeServer()
                }
            }
            inState<RegistryState.Checking> {
                onEnter {
                    checkServer()
                }
            }
            inState<RegistryState.Healthy> {
                on<RegistryOperation.Check> {
                    override {
                        RegistryState.Checking(id, failures)
                    }
                }
                on<RegistryOperation.Remove> {
                    removeServer()
                }
            }
            inState<RegistryState.Removing> {
                onEnter {
                    removingServer()
                }
            }
        }
    }

    private suspend fun ChangeableState<RegistryState.Checking>.checkServer(): ChangedState<RegistryState> {
        val server = registryDao.getRegistryById(snapshot.id)
        if (server != null) {
            try {
                Timber.d("Checking health of server: ${server.name} (${server.url})")
                val start = SystemClock.uptimeMillis()
                val response = client.get("${server.url}/v2/")
                // Accept both OK and Unauthorized (401) as healthy
                // 401 means the registry is responding but requires auth
                if (response.status != HttpStatusCode.OK && response.status != HttpStatusCode.Unauthorized) {
                    throw Exception("Unexpected status code: ${response.status}")
                }
                val latency = SystemClock.uptimeMillis() - start
                Timber.i("Server ${server.name} is healthy (latency: ${latency}ms)")
                return override {
                    RegistryState.Healthy(
                        id = id,
                        latencyMs = latency,
                        failures = 0
                    )
                }
            } catch (e: Exception) {
                Timber.w(e, "Server ${server.name} health check failed: ${e.message}")
                return override {
                    if (failures >= UNHEALTHY_THRESHOLD) {
                        Timber.w("Server ${server.name} marked as unhealthy after $failures failures")
                        RegistryState.Unhealthy(id)
                    } else {
                        RegistryState.Healthy(id, Long.MAX_VALUE, failures + 1)
                    }
                }
            }
        } else {
            return override {
                RegistryState.Removed(id)
            }
        }
    }

    private suspend fun <S : RegistryState> ChangeableState<S>.removeServer(): ChangedState<RegistryState> {
        return when (registryDao.getRegistryById(snapshot.id)?.type) {
            null -> {
                override {
                    RegistryState.Removed(id)
                }
            }

            RegistryType.CustomMirror -> {
                override {
                    RegistryState.Removing(id)
                }
            }

            else -> {
                noChange()
            }
        }
    }

    private suspend fun ChangeableState<RegistryState.Removing>.removingServer(): ChangedState<RegistryState> {
        registryManager.removeServer(snapshot.id)
        return override {
            RegistryState.Removed(id)
        }
    }

    companion object {
        private const val UNHEALTHY_THRESHOLD = 3 // Mark as unhealthy after 3 consecutive failures
    }

    @Singleton
    @AssistedFactory
    interface Factory {
        fun create(
            @Assisted
            id: String,
        ): RegistryStateMachine
    }
}