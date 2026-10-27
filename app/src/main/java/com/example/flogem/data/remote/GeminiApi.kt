package com.example.flogem.data.remote

import com.example.flogem.data.model.GeminiRequest
import com.example.flogem.data.model.GeminiResponse
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Path
import okhttp3.ResponseBody
import retrofit2.http.Query

/**
 * 제미나이 API 인터페이스 (Retrofit)
 * 참고: https://ai.google.dev/gemini-api/docs
 */
interface GeminiApi {

    /**
     * 일반 (비스트리밍) 응답 요청
     * POST https://generativelanguage.googleapis.com/v1beta/models/{model}:generateContent?key={apiKey}
     */
    @POST("v1beta/models/{model}:generateContent")
    suspend fun generateContent(
        @Path("model") model: String,
        @Query("key") apiKey: String,
        @Body request: GeminiRequest
    ): GeminiResponse

    /**
     * 스트리밍 응답 요청 (Server-Sent Events)
     * 동일 엔드포인트지만 stream 파라미터를 사용
     */
    @POST("v1beta/models/{model}:generateContent")
    suspend fun generateContentStream(
        @Path("model") model: String,
        @Query("key") apiKey: String,
        @Query("alt") alt: String = "sse",
        @Body request: GeminiRequest
    ): retrofit2.Response<ResponseBody>
}
