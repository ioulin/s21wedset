package com.example.flogem.viewmodel

import android.app.Application
import android.speech.RecognizerIntent
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.example.flogem.data.model.GeminiRequest
import com.example.flogem.data.model.GeminiResponse
import com.example.flogem.data.remote.GeminiApiClient
import com.example.flogem.domain.model.ApiKeyConfig
import com.example.flogem.domain.model.ChatMessage
import com.example.flogem.util.ApiKeyManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

/**
 * 채팅 ViewModel
 * - 제미나이 스트리밍 응답 처리
 * - 메시지 리스트 관리
 * - 타이핑 상태, 에러 처리
 */
@HiltViewModel
class ChatViewModel @Inject constructor(
    application: Application,
    private val geminiApiClient: GeminiApiClient,
    private val apiKeyManager: ApiKeyManager
) : AndroidViewModel(application) {

    private val _messages = MutableLiveData<List<ChatMessage>>(emptyList())
    val messages: LiveData<List<ChatMessage>> = _messages

    private val _isTyping = MutableLiveData(false)
    val isTyping: LiveData<Boolean> = _isTyping

    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?> = _errorMessage

    private val currentSessionId = UUID.randomUUID().toString()
    private val viewModelScope = CoroutineScope(Dispatchers.IO + Job())

    /**
     * 메시지 전송 (스트리밑)
     */
    fun sendMessage(text: String, isVoiceMode: Boolean = false) {
        val apiKey = apiKeyManager.getApiKey()
        if (apiKey.isNullOrEmpty()) {
            _errorMessage.postValue("API 키가 설정되지 않았습니다. 설정에서 입력해주세요.")
            return
        }

        val config = apiKeyManager.getApiConfig()

        val userMessage = ChatMessage(
            id = UUID.randomUUID().toString(),
            sessionId = currentSessionId,
            role = ChatMessage.Role.USER,
            content = text
        )

        // 사용자 메시지 즉시 추가
        val currentList = _messages.value?.toMutableList() ?: mutableListOf()
        currentList.add(userMessage)
        _messages.postValue(currentList)

        // 타이핑 상태 시작
        _isTyping.postValue(true)

        // 타이핑 인디케이터용 가상 메시지
        val typingMessage = ChatMessage(
            id = "typing",
            sessionId = currentSessionId,
            role = ChatMessage.Role.MODEL,
            content = "",
            isTyping = true
        )
        currentList.add(typingMessage)
        _messages.postValue(currentList)

        // 스트리밍 요청 구성
        val contents = currentList.filter { it.id != "typing" }.map { msg ->
            GeminiRequest.Content(
                role = if (msg.role == ChatMessage.Role.USER) "user" else "model",
                parts = listOf(GeminiRequest.Content.Part(text = msg.content))
            )
        }

        val request = GeminiRequest(
            contents = contents,
            generationConfig = GeminiRequest.GenerationConfig(
                temperature = config.temperature,
                topK = config.topK,
                topP = config.topP,
                maxOutputTokens = config.maxOutputTokens
            ),
            safetySettings = listOf(
                GeminiRequest.SafetySetting("HARM_CATEGORY_HARASSMENT", "BLOCK_MEDIUM_AND_ABOVE"),
                GeminiRequest.SafetySetting("HARM_CATEGORY_HATE_SPEECH", "BLOCK_MEDIUM_AND_ABOVE"),
                GeminiRequest.SafetySetting("HARM_CATEGORY_SEXUALLY_EXPLICIT", "BLOCK_MEDIUM_AND_ABOVE"),
                GeminiRequest.SafetySetting("HARM_CATEGORY_DANGEROUS_CONTENT", "BLOCK_MEDIUM_AND_ABOVE")
            )
        )

        val fullResponse = StringBuilder()

        geminiApiClient.generateContentStream(
            apiKey = config.apiKey,
            model = config.model,
            request = request,
            onChunk = { chunk ->
                fullResponse.append(chunk)
                val updatedList = _messages.value?.toMutableList() ?: return@generateContentStream
                val typingIdx = updatedList.indexOfFirst { it.id == "typing" }
                if (typingIdx >= 0) {
                    updatedList[typingIdx] = updatedList[typingIdx].copy(
                        content = fullResponse.toString(),
                        isTyping = false
                    )
                    _messages.postValue(updatedList)
                }
            },
            onComplete = {
                val finalList = _messages.value?.toMutableList() ?: return@generateContentStream
                val typingIdx = finalList.indexOfFirst { it.id == "typing" }
                if (typingIdx >= 0) {
                    finalList[typingIdx] = ChatMessage(
                        id = UUID.randomUUID().toString(),
                        sessionId = currentSessionId,
                        role = ChatMessage.Role.MODEL,
                        content = fullResponse.toString()
                    )
                }
                _messages.postValue(finalList)
                _isTyping.postValue(false)
            },
            onError = { e ->
                // 에러 메시지 추가
                val errorList = _messages.value?.toMutableList() ?: return@generateContentStream
                val typingIdx = errorList.indexOfFirst { it.id == "typing" }
                if (typingIdx >= 0) {
                    errorList[typingIdx] = ChatMessage(
                        id = UUID.randomUUID().toString(),
                        sessionId = currentSessionId,
                        role = ChatMessage.Role.ERROR,
                        content = "오류: ${e.message}"
                    )
                }
                _messages.postValue(errorList)
                _isTyping.postValue(false)
                _errorMessage.postValue("전송 실패: ${e.message}")
            }
        )
    }

    /**
     * 최근 모델 메시지 가져오기 (TTS용)
     */
    fun getLatestModelMessage(): String? {
        return _messages.value
            ?.filter { it.role == ChatMessage.Role.MODEL && !it.isTyping }
            ?.lastOrNull()
            ?.content
    }

    override fun onCleared() {
        super.onCleared()
        viewModelScope.cancel()
    }
}
