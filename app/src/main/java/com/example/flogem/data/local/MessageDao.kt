package com.example.flogem.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import androidx.room.Delete
import com.example.flogem.domain.model.ChatMessage
import kotlinx.coroutines.flow.Flow

/**
 * 메시지 DAO (Data Access Object)
 */
@Dao
interface MessageDao {

    @Insert
    suspend fun insert(message: ChatMessage)

    @Insert
    suspend fun insertAll(messages: List<ChatMessage>)

    @Update
    suspend fun update(message: ChatMessage)

    @Query("SELECT * FROM messages WHERE sessionId = :sessionId ORDER BY timestamp ASC")
    fun getMessages(sessionId: String): Flow<List<ChatMessage>>

    @Query("SELECT * FROM messages WHERE sessionId = :sessionId ORDER BY timestamp DESC LIMIT 1")
    suspend fun getLatestMessage(sessionId: String): ChatMessage?

    @Query("DELETE FROM messages WHERE sessionId = :sessionId")
    suspend fun deleteSessionMessages(sessionId: String)

    @Query("SELECT COUNT(*) FROM messages WHERE sessionId = :sessionId")
    suspend fun getMessageCount(sessionId: String): Int

    @Query("SELECT * FROM messages WHERE sessionId = :sessionId ORDER BY timestamp DESC LIMIT :limit")
    suspend fun getRecentMessages(sessionId: String, limit: Int): List<ChatMessage>
}
