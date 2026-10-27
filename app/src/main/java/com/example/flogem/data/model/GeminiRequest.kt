package com.example.flogem.data.model

import com.google.gson.annotations.SerializedName

/**
 * 제미나이 API 요청 모델
 */
data class GeminiRequest(
    @SerializedName("contents") val contents: List<Content>,
    @SerializedName("generationConfig") val generationConfig: GenerationConfig,
    @SerializedName("safetySettings") val safetySettings: List<SafetySetting>? = null,
    @SerializedName("systemInstruction") val systemInstruction: SystemInstruction? = null
) {
    data class Content(
        @SerializedName("role") val role: String, // "user", "model"
        @SerializedName("parts") val parts: List<Part>
    ) {
        data class Part(
            @SerializedName("text") val text: String? = null,
            @SerializedName("inlineData") val inlineData: InlineData? = null
        )

        data class InlineData(
            @SerializedName("mimeType") val mimeType: String,
            @SerializedName("data") val data: String // base64
        )
    }

    data class GenerationConfig(
        @SerializedName("temperature") val temperature: Float,
        @SerializedName("topK") val topK: Int,
        @SerializedName("topP") val topP: Float,
        @SerializedName("maxOutputTokens") val maxOutputTokens: Int
    )

    data class SafetySetting(
        @SerializedName("category") val category: String,
        @SerializedName("threshold") val threshold: String
    )

    data class SystemInstruction(
        @SerializedName("parts") val parts: List<Content.Part>
    )
}
