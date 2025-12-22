package com.github.adocker.daemon.registries

import com.freeletics.flowredux2.ChangeableState
import com.freeletics.flowredux2.ChangedState
import com.freeletics.flowredux2.FlowReduxStateMachineFactory
import com.freeletics.flowredux2.initializeWith
import com.github.adocker.daemon.database.dao.RegistryDao
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.ExperimentalCoroutinesApi
import timber.log.Timber
import javax.inject.Singleton
import kotlin.system.measureTimeMillis

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
                on<RegistryOperation.Delete> {
                    override {
                        RegistryState.Deleting(id)
                    }
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
                on<RegistryOperation.Delete> {
                    override {
                        RegistryState.Deleting(id)
                    }
                }
            }
            inState<RegistryState.Deleting> {
                onEnter {
                    removeServer()
                }
            }
        }
    }

    private suspend fun ChangeableState<RegistryState.Checking>.checkServer(): ChangedState<RegistryState> {
        val server = registryDao.getRegistryById(snapshot.id)
        if (server != null) {
            try {
                Timber.d("Checking health of server: ${server.name} (${server.url})")
                val latency = measureTimeMillis {
                    val response = client.get("${server.url}/v2/")
                    // Accept both OK and Unauthorized (401) as healthy
                    // 401 means the registry is responding but requires auth
                    if (response.status != HttpStatusCode.OK && response.status != HttpStatusCode.Unauthorized) {
                        throw Exception("Unexpected status code: ${response.status}")
                    }
                }
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
                RegistryState.Deleted(id)
            }
        }
    }

    private suspend fun ChangeableState<RegistryState.Deleting>.removeServer(): ChangedState<RegistryState> {
        registryManager.removeServer(snapshot.id)
        return override {
            RegistryState.Deleted(id)
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