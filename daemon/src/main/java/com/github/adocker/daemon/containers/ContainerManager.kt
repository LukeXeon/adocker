package com.github.adocker.daemon.containers

import com.github.adocker.daemon.app.AppContext
import com.github.adocker.daemon.database.dao.ContainerDao
import com.github.adocker.daemon.database.dao.ImageDao
import com.github.adocker.daemon.registry.model.ContainerConfig
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ContainerManager @Inject constructor(
    private val containerDao: ContainerDao,
    private val imageDao: ImageDao,
    private val appContext: AppContext,
    private val contextFactory: ContainerContext.Factory,
) {
    suspend fun getAllContainers(): Flow<List<Container>> {
        TODO()
    }

    suspend fun startContainer(
        containerId: String,
    ) {
        TODO()
    }

    suspend fun stopContainer(
        containerId: String,
    ) {
        TODO()
    }

    suspend fun deleteContainer(
        containerId: String,
    ) {
        TODO()
    }

    suspend fun crateContainer(
        imageId: String,
        name: String,
        config: ContainerConfig,
    ) {
        TODO()
    }

    companion object {
        /**
         * Generate a random container name
         */
        private fun generateContainerName(): String {
            val adj = ADJECTIVES.random()
            val noun = NOUNS.random()
            val num = (1000..9999).random()
            return "${adj}_${noun}_$num"
        }

        private val NOUNS = listOf(
            "panda", "tiger", "eagle", "dolphin", "falcon", "wolf", "bear", "lion"
        )

        private val ADJECTIVES = listOf(
            "happy", "sleepy", "brave", "clever", "swift", "calm", "eager", "fancy"
        )
    }
}