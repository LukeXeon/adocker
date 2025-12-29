package com.github.andock.daemon.database.model

import androidx.room.Entity
import androidx.room.PrimaryKey


@Entity(tableName = "search_records")
data class SearchRecordEntity(
    @PrimaryKey
    val query: String,
    val updateAt: Long
)