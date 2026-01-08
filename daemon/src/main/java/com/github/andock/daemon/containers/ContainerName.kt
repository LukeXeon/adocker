package com.github.andock.daemon.containers

import android.app.Application
import com.github.andock.daemon.R
import com.github.andock.daemon.database.dao.ContainerDao
import kotlinx.coroutines.yield
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
            if (!containerDao.hasName(name)) {
                return name
            } else {
                yield()
            }
        } while (true)
    }

}