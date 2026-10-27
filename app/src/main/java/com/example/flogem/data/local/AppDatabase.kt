package com.example.flogem.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.flogem.domain.model.ChatMessage
import com.example.flogem.domain.model.Session

/**
 * 앱 로컬 데이터베이스 (Room)
 * - 대화 메시지 저장
 * - 대화 세션 관리
 */
@Database(
    entities = [ChatMessage::class, Session::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun messageDao(): MessageDao
    abstract fun sessionDao(): SessionDao
}
