package com.github.andock.daemon.database

import com.github.andock.daemon.app.AppTask
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Provider


@AppTask("database")
suspend fun database(database: Provider<AppDatabase>): AppDatabase {
    return withContext(Dispatchers.IO) {
        database.get()
    }
}