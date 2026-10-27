package com.example.flogem.util

import android.content.Context
import android.content.SharedPreferences

/**
 * 플로팅 위젯 위치 및 설정 저장/로드
 * SharedPreferences를 사용하여 영착 데이터 관리
 */
class WidgetConfigManager(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    enum class Edge {
        LEFT, RIGHT, TOP, BOTTOM
    }

    data class Config(
        val edge: Edge = Edge.RIGHT,
        val x: Int = 0,
        val y: Int = 0,
        val isBreathing: Boolean = true,
        val autoTts: Boolean = false,
        val voiceModeDefault: Boolean = false
    )

    fun loadConfig(): Config {
        return Config(
            edge = Edge.valueOf(prefs.getString(KEY_EDGE, Edge.RIGHT.name) ?: Edge.RIGHT.name),
            x = prefs.getInt(KEY_X, 0),
            y = prefs.getInt(KEY_Y, 0),
            isBreathing = prefs.getBoolean(KEY_BREATHING, true),
            autoTts = prefs.getBoolean(KEY_AUTO_TTS, false),
            voiceModeDefault = prefs.getBoolean(KEY_VOICE_MODE, false)
        )
    }

    fun saveConfig(edge: Edge, x: Int, y: Int) {
        prefs.edit()
            .putString(KEY_EDGE, edge.name)
            .putInt(KEY_X, x)
            .putInt(KEY_Y, y)
            .apply()
    }

    fun saveConfig(config: Config) {
        prefs.edit()
            .putString(KEY_EDGE, config.edge.name)
            .putInt(KEY_X, config.x)
            .putInt(KEY_Y, config.y)
            .putBoolean(KEY_BREATHING, config.isBreathing)
            .putBoolean(KEY_AUTO_TTS, config.autoTts)
            .putBoolean(KEY_VOICE_MODE, config.voiceModeDefault)
            .apply()
    }

    fun saveBreathing(isBreathing: Boolean) {
        prefs.edit().putBoolean(KEY_BREATHING, isBreathing).apply()
    }

    fun isBreathing(): Boolean = prefs.getBoolean(KEY_BREATHING, true)

    companion object {
        private const val PREFS_NAME = "flogem_widget_prefs"
        private const val KEY_EDGE = "edge"
        private const val KEY_X = "x"
        private const val KEY_Y = "y"
        private const val KEY_BREATHING = "is_breathing"
        private const val KEY_AUTO_TTS = "auto_tts"
        private const val KEY_VOICE_MODE = "voice_mode_default"
    }
}
