package com.example.flogem.data.repository

import android.util.Log
import com.example.flogem.data.local.MessageDao
import com.example.flogem.data.local.SessionDao
import com.example.flogem.data.model.GeminiRequest
import com.example.flogem.data.model.GeminiResponse
import com.example.flogem.data.remote.GeminiApiClient
import com.example.flogem.domain.model.ApiKeyConfig
import com.example.flogem.domain.model.ChatMessage
import com.example.flogem.domain.model.Session
import com.example.flogem.domain.model.WidgetConfig
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.runBlocking
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 데이터 리포지토리: Gemini API + Room DB 통합 관리
 */
@Singleton
class ConversationRepository @Inject constructor(
    private val messageDao: MessageDao,
    private val sessionDao: SessionDao,
    private val geminiApiClient: GeminiApiClient
) {

    /**
     * 모든 세션 가져오기
     */
    fun getAllSessions(): Flow<List<Session>> = sessionDao.getAllSessions()

    /**
     * 특정 세션의 메시지 가져오기
     */
    fun getMessages(sessionId: String): Flow<List<ChatMessage>> =
        messageDao.getMessages(sessionId)

    /**
     * 새 세션 생성
     */
    suspend fun createSession(): Session {
        val sessionId = UUID.randomUUID().toString()
        val session = Session(
            id = sessionId,
            title = "대화 ${System.currentTimeMillis() / 1000}",
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis(),
            messageCount = 0
        )
        sessionDao.insert(session)
        return session
    }

    /**
     * 메시지 저장
     */
    suspend fun saveMessage(message: ChatMessage) {
        messageDao.insert(message)
    }

    /**
     * 세션 업데이트
     */
    suspend fun updateSession(sessionId: String, messageCount: Int) {
        sessionDao.getSession(sessionId)?.let { session ->
            sessionDao.updateSession(
                sessionId = sessionId,
                title = session.title,
                updatedAt = System.currentTimeMillis(),
                messageCount = messageCount
            )
        }
    }

    /**
     * 세션 삭제
     */
    suspend fun deleteSession(sessionId: String) {
        sessionDao.deleteSession(sessionId)
        messageDao.deleteSessionMessages(sessionId)
    }

    /**
     * 제미나이 API에 질문 전송 (비스트리밍)
     */
    suspend fun sendMessage(
        sessionId: String,
        userMessage: ChatMessage,
        apiKeyConfig: ApiKeyConfig
    ): ChatMessage {
        // 1. 사용자 메시지 저장
        runBlocking { saveMessage(userMessage) }

        // 2. 대화 기록 가져와서 요청 구성
        val history = messageDao.getRecentMessages(sessionId, limit = 20)
        val contents = history.map { msg ->
            GeminiRequest.Content(
                role = if (msg.role == ChatMessage.Role.USER) "user" else "model",
                parts = listOf(GeminiRequest.Content.Part(text = msg.content))
            )
        } + GeminiRequest.Content(
            role = "user",
            parts = listOf(GeminiRequest.Content.Part(text = userMessage.content))
        )

        val request = GeminiRequest(
            contents = contents,
            generationConfig = GeminiRequest.GenerationConfig(
                temperature = apiKeyConfig.temperature,
                topK = apiKeyConfig.topK,
                topP = apiKeyConfig.topP,
                maxOutputTokens = apiKeyConfig.maxOutputTokens
            ),
            safetySettings = listOf(
                GeminiRequest.SafetySetting("HARM_CATEGORY_HARASSMENT", "BLOCK_MEDIUM_AND_ABOVE"),
                GeminiRequest.SafetySetting("HARM_CATEGORY_HATE_SPEECH", "BLOCK_MEDIUM_AND_ABOVE"),
                GeminiRequest.SafetySetting("HARM_CATEGORY_SEXUALLY_EXPLICIT", "BLOCK_MEDIUM_AND_ABOVE"),
                GeminiRequest.SafetySetting("HARM_CATEGORY_DANGEROUS_CONTENT", "BLOCK_MEDIUM_AND_ABOVE")
            )
        )

        // 3. API 호출
        val response: GeminiResponse = geminiApiClient.generateContent(
            apiKey = apiKeyConfig.apiKey,
            model = apiKeyConfig.model,
            request = request
        )

        val responseText = response.getResponseText()
        val modelMessage = ChatMessage(
            id = UUID.randomUUID().toString(),
            sessionId = sessionId,
            role = ChatMessage.Role.MODEL,
            content = responseText
        )

        // 4. 응답 저장
        saveMessage(modelMessage)
        return modelMessage
    }

    /**
     * 제미나이 API에 질문 전송 (스트리밍)
     * 각 청크마다 onProgress 콜백 호출
     */
    fun sendMessageStream(
        sessionId: String,
        userMessage: ChatMessage,
        apiKeyConfig: ApiKeyConfig,
        onProgress: (String) -> Unit,
        onComplete: (String) -> Unit,
        onError: (Throwable) -> Unit
    ) {
        // 1. 사용자 메시지 저장
        runBlocking { saveMessage(userMessage) }

        // 2. 대화 기록으로부터 요청 구성
        val contents = emptyList<GeminiRequest.Content>()
        val request = GeminiRequest(
            contents = contents + GeminiRequest.Content(
                role = "user",
                parts = listOf(GeminiRequest.Content.Part(text = userMessage.content))
            ),
            generationConfig = GeminiRequest.GenerationConfig(
                temperature = apiKeyConfig.temperature,
                topK = apiKeyConfig.topK,
                topP = apiKeyConfig.topP,
                maxOutputTokens = apiKeyConfig.maxOutputTokens
            ),
            safetySettings = listOf(
                GeminiRequest.SafetySetting("HARM_CATEGORY_HARASSMENT", "BLOCK_MEDIUM_AND_ABOVE")
            )
        )

        val fullResponse = StringBuilder()

        geminiApiClient.generateContentStream(
            apiKey = apiKeyConfig.apiKey,
            model = apiKeyConfig.model,
            request = request,
            onChunk = { chunk ->
                fullResponse.append(chunk)
                onProgress(chunk)
            },
            onComplete = {
                val modelMessage = ChatMessage(
                    id = UUID.randomUUID().toString(),
                    sessionId = sessionId,
                    role = ChatMessage.Role.MODEL,
                    content = fullResponse.toString()
                )
                runBlocking {
                    saveMessage(modelMessage)
                    updateSession(sessionId, messageDao.getMessageCount(sessionId))
                }
                onComplete(fullResponse.toString())
            },
            onError = { e ->
                Log.e("ConversationRepository", "Stream error", e)
                onError(e)
            }
        )
    }

    companion object {
        private const val TAG = "ConversationRepository"
    }
}
