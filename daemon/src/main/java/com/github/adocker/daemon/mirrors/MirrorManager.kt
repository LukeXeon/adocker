package com.github.adocker.daemon.mirrors

import com.github.adocker.daemon.database.dao.MirrorDao
import com.github.adocker.daemon.database.model.MirrorEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.Duration

@Singleton
class MirrorManager @Inject constructor(
    private val mirrorDao: MirrorDao,
    private val factory: Mirror.Factory,
    scope: CoroutineScope
) {
    companion object {
        // Docker Hub as hardcoded fallback
        const val DOCKER_HUB_URL = "https://registry-1.docker.io"

        // Built-in mirrors for Docker Hub (excluding Docker Hub itself)
        private val BUILT_IN_MIRRORS = listOf(
            MirrorEntity(
                id = "3f8e7d6c-5b4a-4876-80fe-dcba98765432",
                name = "DaoCloud (China)",
                url = "https://docker.m.daocloud.io",
                priority = 100,
                isBuiltIn = true
            ),
            MirrorEntity(
                id = "789abcde-1234-4678-90ab-cdef12345678",
                name = "Xuanyuan (China)",
                url = "https://docker.xuanyuan.me",
                priority = 90,
                isBuiltIn = true
            ),
            MirrorEntity(
                id = "1a2b3c4d-5e6f-4890-abcd-ef1234567890",
                name = "Aliyun (China)",
                url = "https://registry.cn-hangzhou.aliyuncs.com",
                priority = 80,
                isBuiltIn = true
            ),
            MirrorEntity(
                id = "87654321-abcd-4f12-3456-7890abcdef12",
                name = "Huawei Cloud (China)",
                url = "https://mirrors.huaweicloud.com",
                priority = 70,
                isBuiltIn = true
            )
        )
    }

    private val mutex = Mutex()
    private val _mirrors = MutableStateFlow<Map<String, Mirror>>(emptyMap())
    val mirrors = _mirrors.asStateFlow()

    init {
        scope.launch {
            mutex.withLock {
                mirrorDao.insertMirrors(BUILT_IN_MIRRORS)
                _mirrors.value = mirrorDao.getAllMirrorIds().map { id ->
                    factory.create(MirrorState.Checking(id, 0))
                }.associateBy { it.id }
            }
            while (isActive) {
                delay(with(Duration) {
                    5.minutes
                })
                checkAllMirrors()
            }
        }
    }

    suspend fun getBestMirror(): Mirror {
//        ensureInitialized()
//
//        val bestMirror = mirrorDao.getBestMirror()
//
//        return if (bestMirror != null) {
//            Timber.d("Using best mirror: ${bestMirror.name} (latency: ${bestMirror.latencyMs}ms)")
//            bestMirror.url
//        } else {
//            Timber.w("No healthy mirrors available, falling back to Docker Hub")
//            DOCKER_HUB_URL
//        }
        TODO()
    }

    suspend fun getHealthyMirrors(): List<Mirror> {
        mutex.withLock {
            return _mirrors.value.values.asSequence().filter {
                it.state.value is MirrorState.Healthy
            }.sortedBy {
                TODO()
            }.toList()
        }
    }

    suspend fun addCustomMirror(
        name: String,
        url: String,
        bearerToken: String? = null,
        priority: Int = 50
    ): Result<Mirror> {
        val newMirror = MirrorEntity(
            id = UUID.randomUUID().toString(),
            url = url.removeSuffix("/"),
            name = name,
            bearerToken = bearerToken,
            priority = priority,
            isBuiltIn = false,
        )
        mutex.withLock {
            mirrorDao.insertMirror(newMirror)
            val mirror = factory.create(MirrorState.Checking(newMirror.id, 0))
            _mirrors.value += newMirror.id to mirror
            return Result.success(mirror)
        }
    }

    /**
     * Check health of all mirrors concurrently
     */
    suspend fun checkAllMirrors() {
        mutex.withLock {
            _mirrors.value.values.forEach {
                it.check()
            }
        }
    }

    internal suspend fun removeMirror(id: String) {
        mutex.withLock {
            mirrorDao.deleteMirrorById(id)
            _mirrors.value -= id
        }
    }
}