package com.example.flogem.util

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.util.ArrayList

/**
 * 음성 인식 (Speech-to-Text) 매니저
 * - Google STT API 사용 (온라인)
 * - partial results 실시간 전달
 * - 한국어(KR) 기본
 */
class VoiceInputManager(
    private val context: Context,
    private val onResults: (String) -> Unit,
    private val onError: (String) -> Unit
) {

    private var speechRecognizer: SpeechRecognizer? = null
    private var isRecording = false

    init {
        if (SpeechRecognizer.isRecognitionAvailable(context)) {
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context)
        }
    }

    /**
     * 음성 녹음 시작
     * CALLER가 RECORD_AUDIO 권한을 확인해야 합니다
     */
    fun startListening() {
        if (speechRecognizer == null) {
            onError("음성 인식을 사용할 수 없습니다")
            return
        }

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "ko-KR")
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
        }

        speechRecognizer?.setRecognitionListener(object : RecognitionListener {
            override fun onResults(results: Bundle?) {
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                val text = matches?.firstOrNull() ?: ""
                if (text.isNotEmpty()) {
                    onResults(text)
                }
                isRecording = false
            }

            override fun onPartialResults(partialResults: Bundle?) {
                val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                val text = matches?.firstOrNull() ?: ""
                if (text.isNotEmpty()) {
                    onResults(text)
                }
            }

            override fun onError(errorCode: Int) {
                val errorMsg = getErrorText(errorCode)
                Log.e(TAG, "STT error: $errorMsg")
                onError(errorMsg)
                isRecording = false
            }

            override fun onReadyForSpeech(params: Bundle?) { isRecording = true }
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() {}
            override fun onEvent(eventType: Int, params: Bundle?) {}
        })

        try {
            speechRecognizer?.startListening(intent)
        } catch (e: Exception) {
            onError("음성 인식 시작 실패: ${e.message}")
        }
    }

    /**
     * 녹음 중지
     */
    fun stopListening() {
        if (isRecording) {
            speechRecognizer?.stopListening()
            isRecording = false
        }
    }

    /**
     * 리소스 해제
     */
    fun destroy() {
        speechRecognizer?.destroy()
        speechRecognizer = null
    }

    /**
     * 권한 확인 (Activity에서 호출)
     */
    fun checkPermission(activity: Activity): Boolean {
        return ContextCompat.checkSelfPermission(
            activity, Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun getErrorText(errorCode: Int): String {
        return when (errorCode) {
            SpeechRecognizer.ERROR_AUDIO -> "오디오 에러"
            SpeechRecognizer.ERROR_CLIENT -> "클라이언트 에러"
            SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "권한 부족"
            SpeechRecognizer.ERROR_NETWORK -> "네트워크 에러"
            SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "네트워크 타임아웃"
            SpeechRecognizer.ERROR_NO_MATCH -> "인식 결과 없음"
            SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "음성 인식기 사용 중"
            SpeechRecognizer.ERROR_SERVER -> "서버 에러"
            SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "음성 입력 없음"
            else -> "알 수 없는 에러 ($errorCode)"
        }
    }

    companion object {
        private const val TAG = "VoiceInputManager"
        const val REQUEST_RECORD_AUDIO = 1001
    }
}
