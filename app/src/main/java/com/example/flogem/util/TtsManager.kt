package com.example.flogem.util

import android.content.Context
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import java.util.Locale
import java.util.UUID

/**
 * 텍스트 → 음성 (TTS) 매니저
 * - 제미나이 응답을 음성으로 출력
 * - 한국어(KR) 기본, 속도/조성 조절
 */
class TtsManager(private val context: Context) : TextToSpeech.OnInitListener {

    private var tts: TextToSpeech? = null
    private var isReady = false
    private val utteranceId: String = UUID.randomUUID().toString()

    init {
        tts = TextToSpeech(context, this)
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val result = tts?.setLanguage(Locale.KOREAN)
            if (result == TextToSpeech.LANG_MISSING_DATA ||
                result == TextToSpeech.LANG_NOT_SUPPORTED) {
                Log.e(TAG, "TTS: 한국어 언어 데이터 없음")
                isReady = false
            } else {
                // 기본 설정
                tts?.setPitch(1.0f)     // 기본 음높이
                tts?.setSpeechRate(1.0f) // 기본 속도
                isReady = true
            }
        } else {
            Log.e(TAG, "TTS 초기화 실패")
            isReady = false
        }
    }

    /**
     * 텍스트를 음성으로 출력
     */
    fun speak(text: String, onComplete: (() -> Unit)? = null) {
        if (!isReady) {
            Log.w(TAG, "TTS가 준비되지 않음")
            return
        }

        if (onComplete != null) {
            tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) {}
                override fun onDone(utteranceId: String?) = onComplete()
                override fun onError(utteranceId: String?) = onComplete()
            })
        }

        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, utteranceId)
    }

    /**
     * 현재 말하는 중지
     */
    fun stop() {
        tts?.stop()
    }

    /**
     * 음성 속도 설정 (1.0 = 기본, 0.5 = 절반속도, 2.0 = 2배속도)
     */
    fun setSpeechRate(rate: Float) {
        tts?.setSpeechRate(rate)
    }

    /**
     * 음높이 설정 (1.0 = 기본)
     */
    fun setPitch(pitch: Float) {
        tts?.setPitch(pitch)
    }

    /**
     * TTS가 준비됬는지 확인
     */
    fun isReady(): Boolean = isReady

    /**
     * 리소스 해제
     */
    fun shutdown() {
        tts?.stop()
        tts?.shutdown()
        tts = null
    }

    companion object {
        private const val TAG = "TtsManager"
    }
}
