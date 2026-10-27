package com.example.flogem.data.model

import com.google.gson.annotations.SerializedName

/**
 * 제미나이 API 응답 모델 (스트리밍 및 일반 응답 모두 처리)
 */
data class GeminiResponse(
    @SerializedName("candidates") val candidates: List<Candidate>? = null,
    @SerializedName("usageMetadata") val usageMetadata: UsageMetadata? = null,
    @SerializedName("promptFeedback") val promptFeedback: PromptFeedback? = null
) {
    data class Candidate(
        @SerializedName("content") val content: Content? = null,
        @SerializedName("finishReason") val finishReason: String? = null,
        @SerializedName("index") val index: Int? = null
    ) {
        data class Content(
            @SerializedName("parts") val parts: List<Part>? = null,
            @SerializedName("role") val role: String? = null
        ) {
            data class Part(
                @SerializedName("text") val text: String? = null
            )
        }
    }

    data class UsageMetadata(
        @SerializedName("promptTokenCount") val promptTokenCount: Int? = null,
        @SerializedName("candidatesTokenCount") val candidatesTokenCount: Int? = null,
        @SerializedName("totalTokenCount") val totalTokenCount: Int? = null
    )

    data class PromptFeedback(
        @SerializedName("blockReason") val blockReason: String? = null,
        @SerializedName("safetyRatings") val safetyRatings: List<SafetyRating>? = null
    ) {
        data class SafetyRating(
            @SerializedName("category") val category: String,
            @SerializedName("probability") val probability: String
        )
    }

    /**
     * 스트리밍 응답에서 텍스트 추출
     */
    fun getResponseText(): String {
        return candidates?.firstOrNull()
            ?.content
            ?.parts
            ?.firstOrNull()
            ?.text
            ?: ""
    }

    /**
     * 응답이 끝났는지 (finishReason == "STOP")
     */
    fun isFinished(): Boolean {
        return candidates?.firstOrNull()?.finishReason == "STOP"
                || candidates?.firstOrNull()?.finishReason == "MAX_TOKENS"
    }
}
