package com.example.flogem.domain.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

/**
 * 대화 메시지 엔티티
 * Room DB에 저장됨
 */
@Entity(tableName = "messages")
data class ChatMessage(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val sessionId: String,
    val role: Role,
    val content: String,
    val timestamp: Long = System.currentTimeMillis(),
    val isTyping: Boolean = false,
    val isError: Boolean = false
) {
    enum class Role {
        USER,       // 사용자 입력
        MODEL,      // 제미나이 응답
        SYSTEM,     // 시스템 메시지
        ERROR       // 오류 메시지
    }
}
