package com.github.adocker.daemon.containers

import android.app.Application
import com.github.adocker.daemon.R
import com.github.adocker.daemon.database.dao.ContainerDao
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.isActive
import javax.inject.Inject
import javax.inject.Singleton


@Singleton
class ContainerName @Inject constructor(
    private val containerDao: ContainerDao,
    application: Application,
) {
    private val adjectives = application.resources.getStringArray(R.array.adjectives).asList()
    private val nouns = application.resources.getStringArray(R.array.nouns).asList()

    /**
     * Generate a random container name
     */
    private fun randomContainerName(): String {
        val adj = adjectives.random()
        val noun = nouns.random()
        val num = (1000..9999).random()
        return "${adj}_${noun}_$num"
    }

    suspend fun generateName(): String {
        do {
            val name = randomContainerName()
            if (containerDao.getContainerByName(name) == null) {
                return name
            } else {
                currentCoroutineContext().ensureActive()
            }
        } while (true)
    }

}