package com.example.flogem.domain.model

/**
 * 제미나이 API 키 정보
 */
data class ApiKeyConfig(
    val apiKey: String = "",
    val model: String = "gemini-1.5-flash",
    val temperature: Float = 0.7f,
    val maxOutputTokens: Int = 2048,
    val topK: Int = 40,
    val topP: Float = 0.95f
)
