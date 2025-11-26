package com.github.adocker.core.container

import com.github.adocker.core.database.model.ContainerEntity
import com.github.adocker.core.engine.PRootEngine
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.CoroutineScope
import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.InputStreamReader
import java.io.OutputStreamWriter

data class RunningContainer @AssistedInject constructor(
    private val entity: ContainerEntity,
    private val engine: PRootEngine,
    private val scope: CoroutineScope
) {
    val id: String get() = entity.id
    val name: String get() = entity.name
    val imageId: String get() = entity.imageId
    val imageName: String get() = entity.imageName
    val created: Long get() = entity.created
    val config get() = entity.config
    val stdin = BufferedWriter(OutputStreamWriter(process.outputStream))
    val stdout = BufferedReader(InputStreamReader(process.inputStream))

    @AssistedFactory
    interface Factory {

    }
}