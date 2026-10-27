package com.example.flogem.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.example.flogem.domain.model.Session
import kotlinx.coroutines.flow.Flow

/**
 * 세션 DAO
 */
@Dao
interface SessionDao {

    @Insert
    suspend fun insert(session: Session)

    @Query("SELECT * FROM sessions ORDER BY updatedAt DESC")
    fun getAllSessions(): Flow<List<Session>>

    @Query("SELECT * FROM sessions WHERE id = :sessionId LIMIT 1")
    suspend fun getSession(sessionId: String): Session?

    @Query("UPDATE sessions SET title = :title, updatedAt = :updatedAt, messageCount = :messageCount WHERE id = :sessionId")
    suspend fun updateSession(
        sessionId: String,
        title: String,
        updatedAt: Long,
        messageCount: Int
    )

    @Query("DELETE FROM sessions WHERE id = :sessionId")
    suspend fun deleteSession(sessionId: String)

    @Query("SELECT MAX(id) FROM sessions")
    suspend fun getLatestSessionId(): String?
}
