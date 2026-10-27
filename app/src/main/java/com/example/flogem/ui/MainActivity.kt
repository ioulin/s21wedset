package com.example.flogem.ui

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.flogem.R
import com.example.flogem.databinding.ActivityMainBinding
import com.example.flogem.service.FloatingWidgetService
import com.example.flogem.util.ApiKeyManager
import dagger.hilt.android.AndroidEntryPoint

/**
 * 메인 액티비티
 * - 앱 최초 실행 시 권한 요청 (SYSTEM_ALERT_WINDOW, RECORD_AUDIO)
 * - API 키 설정
 * - 플로팅 위젯 서비스 시작/중지 토글
 */
@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var apiKeyManager: ApiKeyManager

    private val OVERLAY_PERMISSION_REQUEST = 1001
    private val AUDIO_PERMISSION_REQUEST = 1002

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        apiKeyManager = ApiKeyManager(this)

        setupClickListeners()
        checkPermissions()
    }

    private fun setupClickListeners() {
        // 플로팅 위젯 시작 버튼
        binding.buttonStartWidget.setOnClickListener {
            if (!apiKeyManager.hasApiKey()) {
                showApiKeyDialog()
            } else {
                startFloatingWidget()
            }
        }

        // 설정 버튼
        binding.buttonSettings.setOnClickListener {
            val intent = Intent(this, SettingsActivity::class.java)
            startActivity(intent)
        }

        // API 키 관리 버튼
        binding.buttonManageApiKey.setOnClickListener {
            showApiKeyDialog()
        }
    }

    private fun showApiKeyDialog() {
        val dialog = androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("제미나이 API 키")
            .setMessage("https://aistudio.google.com/ 에서 API 키를 발급받아 입력하세요")
            .setView(R.layout.dialog_api_key_input)
            .setPositiveButton("저장") { _, _ ->
                // 다이얼로그에서 입력한 API 키 저장
            }
            .setNegativeButton("취소", null)
            .create()

        dialog.show()

        // 다이얼로그의 EditText 가져오기
        val editText = dialog.findViewById<android.widget.EditText>(R.id.editApiKey)
        editText?.setText(apiKeyManager.getApiKey() ?: "")

        dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE)
            .setOnClickListener {
                val apiKey = editText?.text.toString().trim()
                if (apiKey.isNotEmpty()) {
                    apiKeyManager.saveApiKey(apiKey)
                    Toast.makeText(this, "API 키가 저장되었습니다", Toast.LENGTH_SHORT).show()
                    startFloatingWidget()
                    dialog.dismiss()
                } else {
                    Toast.makeText(this, "API 키를 입력해주세요", Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun checkPermissions() {
        // SYSTEM_ALERT_WINDOW 권한
        if (!Settings.canDrawOverlays(this)) {
            binding.permissionStatusOverlay.text = "권한 필요"
            binding.buttonRequestOverlay.setOnClickListener {
                requestOverlayPermission()
            }
        } else {
            binding.permissionStatusOverlay.text = "허용됨"
            binding.buttonRequestOverlay.isEnabled = false
        }

        // RECORD_AUDIO 권한
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.RECORD_AUDIO)
            == android.content.pm.PackageManager.PERMISSION_GRANTED) {
            binding.permissionStatusAudio.text = "허용됨"
            binding.buttonRequestAudio.isEnabled = false
        } else {
            binding.permissionStatusAudio.text = "권한 필요"
            binding.buttonRequestAudio.setOnClickListener {
                requestAudioPermission()
            }
        }
    }

    private fun requestOverlayPermission() {
        val intent = Intent(
            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
            Uri.parse("package:$packageName")
        )
        startActivityForResult(intent, OVERLAY_PERMISSION_REQUEST)
    }

    private fun requestAudioPermission() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            requestPermissions(
                arrayOf(android.Manifest.permission.RECORD_AUDIO),
                AUDIO_PERMISSION_REQUEST
            )
        }
    }

    private fun startFloatingWidget() {
        if (!Settings.canDrawOverlays(this)) {
            Toast.makeText(this, "플로팅 권한이 필요합니다", Toast.LENGTH_SHORT).show()
            requestOverlayPermission()
            return
        }

        val intent = Intent(this, FloatingWidgetService::class.java)
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }
        Toast.makeText(this, "플로팅 위젯이 시작되었습니다", Toast.LENGTH_SHORT).show()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == OVERLAY_PERMISSION_REQUEST) {
            if (Settings.canDrawOverlays(this)) {
                binding.permissionStatusOverlay.text = "허용됨"
                binding.buttonRequestOverlay.isEnabled = false
                startFloatingWidget()
            } else {
                Toast.makeText(this, "플로팅 권한이 거부되었습니다", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
