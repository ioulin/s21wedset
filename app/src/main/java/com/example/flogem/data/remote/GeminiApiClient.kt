package com.example.flogem.data.remote

import android.util.Log
import com.example.flogem.data.model.GeminiRequest
import com.example.flogem.data.model.GeminiResponse
import com.google.gson.GsonBuilder
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.sse.EventSource
import okhttp3.sse.EventSourceListener
import okhttp3.sse.EventSources
import okhttp3.MediaType.Companion.toMediaType
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 제미나이 API 클라이언트
 * - 비스트리밍 (Retrofit) 및 스트리밍 (SSE) 응답 지원
 * - 요청/응답 로깅
 */
@Singleton
class GeminiApiClient @Inject constructor() {

    private val baseUrl = "https://generativelanguage.googleapis.com/"

    private val okHttpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    private val gson = GsonBuilder().create()

    private val retrofit: Retrofit = Retrofit.Builder()
        .baseUrl(baseUrl)
        .client(okHttpClient)
        .addConverterFactory(GsonConverterFactory.create(gson))
        .build()

    private val api: GeminiApi = retrofit.create(GeminiApi::class.java)

    /**
     * 비스트리밍 API 호출 (전체 응답을 한 번에 받음)
     */
    suspend fun generateContent(
        apiKey: String,
        model: String,
        request: GeminiRequest
    ): GeminiResponse {
        return api.generateContent(model, apiKey, request)
    }

    /**
     * 스트리밍 API 호출 (SSE)
     * 각 청크의 텍스트를 onChunk 콜백으로 전달
     */
    fun generateContentStream(
        apiKey: String,
        model: String,
        request: GeminiRequest,
        onChunk: (String) -> Unit,
        onComplete: () -> Unit,
        onError: (Throwable) -> Unit
    ) {
        val mediaType = "application/json; charset=UTF-8".toMediaType()
        val jsonBody = gson.toJson(request)
        val requestBody = RequestBody.create(mediaType, jsonBody)

        val httpRequest = Request.Builder()
            .url("$baseUrl/v1beta/models/$model:generateContent?key=$apiKey&alt=sse")
            .post(requestBody)
            .build()

        val eventSource = EventSources.createFactory(okHttpClient).newEventSource(httpRequest, object : EventSourceListener() {
            override fun onEvent(source: EventSource, id: String?, type: String?, data: String) {
                val eventData = data ?: return
                if (eventData == "[DONE]") {
                    onComplete()
                    return
                }
                try {
                    val json = if (eventData.startsWith("data: ")) {
                        eventData.substring(6)
                    } else {
                        eventData
                    }
                    val response = gson.fromJson(json, GeminiResponse::class.java)
                    response.getResponseText().let { text ->
                        if (text.isNotEmpty()) {
                            onChunk(text)
                        }
                    }
                    if (response.isFinished()) {
                        onComplete()
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "SSE parsing error: ${e.message}")
                    onError(e)
                }
            }

            override fun onFailure(source: EventSource, t: Throwable?, response: okhttp3.Response?) {
                Log.e(TAG, "SSE error: ${t?.message}")
                onError(t ?: Exception("Unknown SSE error"))
            }
        })

    }

    companion object {
        private const val TAG = "GeminiApiClient"
    }
}
