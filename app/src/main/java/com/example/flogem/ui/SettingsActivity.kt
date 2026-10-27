package com.example.flogem.ui

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.flogem.databinding.ActivitySettingsBinding
import com.example.flogem.util.ApiKeyManager
import dagger.hilt.android.AndroidEntryPoint

/**
 * 설정 액티비티
 * - API 키 관리
 * - 모델 선택
 * - 위젯 설정 (숨쉴 때 애니메이션, 자동 TTS, 음성 모드 기본)
 * - 앱 정보
 */
@AndroidEntryPoint
class SettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySettingsBinding
    private lateinit var apiKeyManager: ApiKeyManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        apiKeyManager = ApiKeyManager(this)

        setupToolbar()
        setupApiKeySection()
        setupWidgetSettings()
        setupAppInfo()
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setHomeActionContentDescription("뒤로")
        toolbarNavigationOnClickListener()
    }

    private fun toolbarNavigationOnClickListener() {
        binding.toolbar.setNavigationOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }
    }

    private fun setupApiKeySection() {
        // 현재 저장된 API 키 표시 (마스킹)
        val savedKey = apiKeyManager.getApiKey()
        val displayKey = if (savedKey.isNullOrEmpty()) {
            "API 키가 설정되지 않았습니다"
        } else {
            savedKey.take(4) + "*" + "*".repeat(savedKey.length - 8) + savedKey.takeLast(4)
        }
        binding.textApiKeyStatus.text = displayKey

        binding.buttonEditApiKey.setOnClickListener {
            showEditApiKeyDialog()
        }

        // 모델명 표시
        val config = apiKeyManager.getApiConfig()
        binding.textModelName.text = config.model
        binding.buttonChangeModel.setOnClickListener {
            showModelSelectionDialog(config.model)
        }
    }

    private fun showEditApiKeyDialog() {
        val dialog = androidx.appcompat.app.AlertDialog.Builder(this).apply {
            setTitle("제미나이 API 키 수정")
            val input = android.widget.EditText(this@SettingsActivity).apply {
                hint = "API 키 입력"
                setText(apiKeyManager.getApiKey() ?: "")
                inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD
            }
            setView(input, 50, 30, 50, 30)
            setPositiveButton("저장") { _, _ ->
                val key = input.text.toString().trim()
                if (key.isNotEmpty()) {
                    apiKeyManager.saveApiKey(key)
                    Toast.makeText(this@SettingsActivity, "저장되었습니다", Toast.LENGTH_SHORT).show()
                    recreate()
                } else {
                    Toast.makeText(this@SettingsActivity, "API 키를 입력해주세요", Toast.LENGTH_SHORT).show()
                }
            }
            setNegativeButton("취소", null)
        }
        dialog.show()
    }

    private fun showModelSelectionDialog(currentModel: String) {
        val models = arrayOf(
            "gemini-2.5-flash-lite",
            "gemini-2.0-flash",
            "gemini-2.0-flash-lite",
            "gemini-1.5-flash",
            "gemini-1.5-pro",
            "gemini-1.5-flash-8b"
        )
        val currentIndex = models.indexOfFirst { it.contains(currentModel, ignoreCase = true) }
        val safeIndex = if (currentIndex >= 0) currentIndex else 0

        val dialog = androidx.appcompat.app.AlertDialog.Builder(this).apply {
            setTitle("사용할 모델 선택")
            setSingleChoiceItems(models, safeIndex) { _, which ->
                val selected = models[which]
                val config = apiKeyManager.getApiConfig()
                apiKeyManager.saveApiKey(config.apiKey) // 재저장
                // 모델 저장을 위해 config 업데이트
                val newConfig = config.copy(model = selected)
                apiKeyManager.saveApiConfig(newConfig)
                binding.textModelName.text = selected
                Toast.makeText(this@SettingsActivity, "모델: $selected", Toast.LENGTH_SHORT).show()
            }
            setNegativeButton("취소", null)
        }
        dialog.show()
    }

    private fun setupWidgetSettings() {
        val config = apiKeyManager.getApiConfig()
        // Auto TTS 토글
        // Voice Mode Default 토글
        // 위젯 위치 설정
    }

    private fun setupAppInfo() {
        val versionInfo = try {
            val pInfo = packageManager.getPackageInfo(packageName, 0)
            "FloGem v${pInfo.versionName}"
        } catch (e: Exception) {
            "FloGem"
        }
        binding.textAppVersion.text = versionInfo
    }
}
