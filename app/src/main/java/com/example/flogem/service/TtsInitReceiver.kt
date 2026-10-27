package com.example.flogem.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.speech.tts.TextToSpeech
import android.util.Log
import java.util.Locale

/**
 * TTS 초기화 브로드캐스트 리시버
 * 앱 시작 시 TTS 엔진을 미리 초기화하여 랙 감소
 */
class TtsInitReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action == ACTION_INIT_TTS) {
            val tts = TextToSpeech(context) { status ->
                if (status == TextToSpeech.SUCCESS) {
                    tts.language = Locale.KOREAN
                    Log.d(TAG, "TTS 엔진 초기화 성공")
                } else {
                    Log.e(TAG, "TTS 엔진 초기화 실패")
                }
            }
        }
    }

    companion object {
        private const val TAG = "TtsInitReceiver"
        const val ACTION_INIT_TTS = "com.example.flogem.ACTION_INIT_TTS"
    }
}
