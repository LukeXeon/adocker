package com.github.adocker.daemon.mirrors

import com.freeletics.flowredux2.ChangeableState
import com.freeletics.flowredux2.ChangedState
import com.freeletics.flowredux2.FlowReduxStateMachineFactory
import com.freeletics.flowredux2.initializeWith
import com.github.adocker.daemon.database.dao.MirrorDao
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
class MirrorStateMachine @AssistedInject constructor(
    @Assisted
    initialState: MirrorState,
    private val mirrorDao: MirrorDao,
    private val client: HttpClient,
    private val mirrorManager: MirrorManager,
) : FlowReduxStateMachineFactory<MirrorState, MirrorOperation>() {
    init {
        initializeWith { initialState }
        spec {
            inState<MirrorState.Unhealthy> {
                on<MirrorOperation.Check> {
                    override {
                        MirrorState.Checking(id, UNHEALTHY_THRESHOLD)
                    }
                }
                on<MirrorOperation.Delete> {
                    override {
                        MirrorState.Deleting(id)
                    }
                }
            }
            inState<MirrorState.Checking> {
                onEnter {
                    checkMirror()
                }
            }
            inState<MirrorState.Healthy> {
                on<MirrorOperation.Check> {
                    override {
                        MirrorState.Checking(id, failures)
                    }
                }
                on<MirrorOperation.Delete> {
                    override {
                        MirrorState.Deleting(id)
                    }
                }
            }
            inState<MirrorState.Deleting> {
                onEnter {
                    mirrorManager.removeMirror(snapshot.id)
                    override {
                        MirrorState.Deleted(id)
                    }
                }
            }
        }
    }

    private suspend fun ChangeableState<MirrorState.Checking>.checkMirror(): ChangedState<MirrorState> {
        val mirror = mirrorDao.getMirrorById(snapshot.id)
        if (mirror != null) {
            try {
                Timber.d("Checking health of mirror: ${mirror.name} (${mirror.url})")
                val latency = measureTimeMillis {
                    val response = client.get("${mirror.url}/v2/")
                    // Accept both OK and Unauthorized (401) as healthy
                    // 401 means the registry is responding but requires auth
                    if (response.status != HttpStatusCode.OK && response.status != HttpStatusCode.Unauthorized) {
                        throw Exception("Unexpected status code: ${response.status}")
                    }
                }
                Timber.i("Mirror ${mirror.name} is healthy (latency: ${latency}ms)")
                return override {
                    MirrorState.Healthy(
                        id = id,
                        latencyMs = latency,
                        failures = 0
                    )
                }
            } catch (e: Exception) {
                Timber.w(e, "Mirror ${mirror.name} health check failed: ${e.message}")
                return override {
                    if (failures >= UNHEALTHY_THRESHOLD) {
                        Timber.w("Mirror ${mirror.name} marked as unhealthy after $failures failures")
                        MirrorState.Unhealthy(id)
                    } else {
                        MirrorState.Healthy(id, -1, failures + 1)
                    }
                }
            }
        } else {
            return override {
                MirrorState.Deleted(id)
            }
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
            initialState: MirrorState
        ): MirrorStateMachine
    }
}