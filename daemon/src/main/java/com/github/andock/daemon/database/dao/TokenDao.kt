package com.github.andock.daemon.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.github.andock.daemon.database.model.TokenEntity

@Dao
interface TokenDao {
    @Query("SELECT * FROM tokens WHERE url = :url")
    suspend fun getTokenByUrl(url: String): TokenEntity?

    @Insert
    suspend fun insertToken(token: TokenEntity)

    @Query("DELETE FROM tokens WHERE expiry < (strftime('%s', 'now') * 1000)")
    suspend fun deleteExpiredTokens()
}