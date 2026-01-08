package com.github.andock.daemon.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.github.andock.daemon.database.model.ContainerEntity
import com.github.andock.daemon.database.model.ContainerLastRun
import com.github.andock.daemon.images.models.ContainerConfig
import kotlinx.coroutines.flow.Flow

@Dao
interface ContainerDao {
    @Query("SELECT id,lastRunAt FROM containers")
    suspend fun getAllLastRun(): List<ContainerLastRun>

    @Query("SELECT id FROM containers")
    suspend fun getAllIds(): List<String>

    @Query("SELECT config FROM containers WHERE id = :id")
    suspend fun findConfigById(id: String): ContainerConfig?

    @Query("SELECT * FROM containers WHERE id = :id")
    fun findByIdAsFlow(id: String): Flow<ContainerEntity?>

    @Query("SELECT COUNT(*) FROM containers WHERE name = :name")
    suspend fun hasName(name: String): Boolean

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(container: ContainerEntity)

    @Query("DELETE FROM containers WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("UPDATE containers SET lastRunAt = :timestamp WHERE id = :id")
    suspend fun setLastRun(id: String, timestamp: Long)

    @Query(
        """ 
WITH RECURSIVE
-- 步骤1：复刻Android资源中的形容词/名词数组（需替换为你strings.xml中的实际内容）
adjectives(adj) AS (
    VALUES 
        ('happy'), ('brave'), ('calm'), ('fast'), ('smart'),  -- 替换为你R.array.adjectives的实际值
        ('bright'), ('gentle'), ('strong'), ('lucky'), ('proud')
),
nouns(noun) AS (
    VALUES 
        ('dolphin'), ('eagle'), ('tiger'), ('fox'), ('bear'),  -- 替换为你R.array.nouns的实际值
        ('wolf'), ('rabbit'), ('deer'), ('lion'), ('hawk')
),
-- 步骤2：复刻randomContainerName()逻辑：形容词_名词_4位随机数（1000-9999）
candidate AS (
    SELECT 
        -- 随机选形容词
        (SELECT adj FROM adjectives ORDER BY random() LIMIT 1) || '_' ||
        -- 随机选名词
        (SELECT noun FROM nouns ORDER BY random() LIMIT 1) || '_' ||
        -- 生成1000-9999的随机数（复刻(1000..9999).random()）
        (abs(random()) % 9000 + 1000) AS name
),
-- 步骤3：复刻generateName()的do-while校验逻辑
unique_name AS (
    -- 初始生成一个候选名称
    SELECT name FROM candidate
    UNION ALL
    -- 如果名称已存在，重新生成（对应代码中的yield()循环）
    SELECT (SELECT name FROM candidate) FROM unique_name
    WHERE (SELECT name FROM candidate) IN (SELECT name FROM containers)
    LIMIT 1  -- 只返回第一个唯一名称
)
-- 最终返回不存在于containers表的名称
SELECT name FROM unique_name 
WHERE name NOT IN (SELECT name FROM containers)
LIMIT 1;
    """
    )
    suspend fun generateName(): String
}