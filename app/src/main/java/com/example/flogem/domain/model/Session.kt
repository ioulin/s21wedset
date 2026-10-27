package com.example.flogem.domain.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 대화 세션 엔티티
 */
@Entity(tableName = "sessions")
data class Session(
    @PrimaryKey val id: String,
    val title: String,
    val createdAt: Long,
    val updatedAt: Long,
    val messageCount: Int
)
