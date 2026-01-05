package com.github.andock.daemon.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.github.andock.daemon.database.model.AuthTokenEntity

@Dao
interface AuthTokenDao {
    @Query("SELECT * FROM auth_tokens WHERE url = :url")
    suspend fun getTokenByUrl(url: String): AuthTokenEntity?

    @Insert
    suspend fun insertToken(token: AuthTokenEntity)

    @Query("DELETE FROM auth_tokens WHERE expiry < (strftime('%s', 'now') * 1000)")
    suspend fun deleteExpiredTokens()

    @Query("DELETE FROM auth_tokens WHERE token = :token")
    suspend fun deleteToken(token: String)
}