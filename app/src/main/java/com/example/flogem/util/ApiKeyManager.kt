package com.example.flogem.util

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.example.flogem.domain.model.ApiKeyConfig
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 제미나이 API 키의 안전한 저장 및 관리
 * EncryptedSharedPreferences를 사용하여 암호화 저장
 */
@Singleton
class ApiKeyManager @Inject constructor(
    private val context: Context
) {
    private val appContext = context.applicationContext

    private val prefs: SharedPreferences by lazy {
        try {
            val masterKey = MasterKey.Builder(appContext, MasterKey.DEFAULT_MASTER_KEY_ALIAS)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()
            EncryptedSharedPreferences.create(
                appContext,
                PREFS_NAME,
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SKEIN,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        } catch (e: Exception) {
            Log.e("ApiKeyManager", "Failed to init EncryptedSharedPreferences, falling back to plain prefs", e)
            appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        }
    }

    /**
     * API 키 저장
     */
    fun saveApiKey(apiKey: String) {
        prefs.edit()
            .putString(KEY_API_KEY, apiKey)
            .apply()
    }

    /**
     * API 키 불러오기
     */
    fun getApiKey(): String? = prefs.getString(KEY_API_KEY, null)

    /**
     * API 설정 저장
     */
    fun saveApiConfig(config: ApiKeyConfig) {
        prefs.edit()
            .putString(KEY_API_KEY, config.apiKey)
            .putString(KEY_MODEL, config.model)
            .putFloat(KEY_TEMPERATURE, config.temperature)
            .putInt(KEY_MAX_TOKENS, config.maxOutputTokens)
            .putInt(KEY_TOP_K, config.topK)
            .putFloat(KEY_TOP_P, config.topP)
            .apply()
    }

    /**
     * API 설정 불러오기
     */
    fun getApiConfig(): ApiKeyConfig {
        return ApiKeyConfig(
            apiKey = getApiKey() ?: "",
            model = prefs.getString(KEY_MODEL, "gemini-2.0-flash") ?: "gemini-2.0-flash",
            temperature = prefs.getFloat(KEY_TEMPERATURE, 0.7f),
            maxOutputTokens = prefs.getInt(KEY_MAX_TOKENS, 2048),
            topK = prefs.getInt(KEY_TOP_K, 40),
            topP = prefs.getFloat(KEY_TOP_P, 0.95f)
        )
    }

    /**
     * API 키가 설정되어 있는지 확인
     */
    fun hasApiKey(): Boolean = !getApiKey().isNullOrEmpty()

    /**
     * API 키 삭제
     */
    fun clearApiKey() {
        prefs.edit()
            .remove(KEY_API_KEY)
            .apply()
    }

    companion object {
        private const val PREFS_NAME = "flogem_api_prefs"
        private const val KEY_API_KEY = "api_key"
        private const val KEY_MODEL = "model"
        private const val KEY_TEMPERATURE = "temperature"
        private const val KEY_MAX_TOKENS = "max_output_tokens"
        private const val KEY_TOP_K = "top_k"
        private const val KEY_TOP_P = "top_p"
    }
}
