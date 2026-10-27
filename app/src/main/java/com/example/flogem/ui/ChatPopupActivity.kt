package com.example.flogem.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognizerIntent
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.view.input.method.EditorInfo
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.flogem.R
import com.example.flogem.databinding.ActivityChatPopupBinding
import com.example.flogem.ui.adapter.ChatAdapter
import com.example.flogem.util.TtsManager
import com.example.flogem.util.VoiceInputManager
import com.example.flogem.viewmodel.ChatViewModel
import dagger.hilt.android.AndroidEntryPoint

/**
 * 채팅 팝업 액티비티
 * - 플로팅 위젯 탭 시 표시
 * - 텍스트 입력 또는 음성 인식 (마이크)
 * - 제미나이 스트리밍 응답 실시간 표시
 * - TTS 음성 출력 (선택)
 */
@AndroidEntryPoint
class ChatPopupActivity : AppCompatActivity() {

    private lateinit var binding: ActivityChatPopupBinding
    private val viewModel: ChatViewModel by viewModels()
    private lateinit var adapter: ChatAdapter
    private var ttsManager: TtsManager? = null
    private var voiceInputManager: VoiceInputManager? = null
    private var isVoiceMode = false

    private val speechRecognizerLauncher: ActivityResultLauncher<Intent> =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                val matches = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
                val text = matches?.firstOrNull() ?: ""
                if (text.isNotEmpty()) {
                    sendMessage(text)
                }
            }
        }

    private val requestAudioPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) {
                startVoiceInput()
            } else {
                Toast.makeText(this, "음성 인식 권한이 필요합니다", Toast.LENGTH_SHORT).show()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 레이아웃 인플레이션 (ViewBinding)
        binding = ActivityChatPopupBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 팝업 애니메이션
        setupPopupAnimation()

        // Intent에서 음성 모드 플래그 확인
        isVoiceMode = intent.getBooleanExtra("voice_mode", false)

        setupRecyclerView()
        setupClickListeners()
        setupViewModel()
        initTts()

        // 음성 모드 자동 전환
        if (isVoiceMode) {
            switchToVoiceMode()
        }
    }

    private fun setupPopupAnimation() {
        // 창 옵션: 팝업 스타일
        window?.apply {
            setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.WRAP_CONTENT)
            setBackgroundDrawableResource(android.R.color.transparent)
            setDimAmount(0.4f)
            addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
        }
        // 진입 애니메이션
        binding.root.alpha = 0f
        binding.root.translationY = 100f
        binding.root.animate()
            .alpha(1f)
            .translationY(0f)
            .setDuration(300)
            .start()
    }

    private fun setupRecyclerView() {
        adapter = ChatAdapter()
        binding.recyclerViewMessages.apply {
            layoutManager = LinearLayoutManager(this@ChatPopupActivity)
            adapter = this@ChatPopupActivity.adapter
        }
    }

    private fun setupClickListeners() {
        // 전송 버튼
        binding.buttonSend.setOnClickListener {
            val text = binding.editTextInput.text.toString().trim()
            if (text.isNotEmpty()) {
                sendMessage(text)
                binding.editTextInput.text?.clear()
            }
        }

        // 텍스트 입력 엔터 키 (전송)
        binding.editTextInput.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEND) {
                val text = binding.editTextInput.text.toString().trim()
                if (text.isNotEmpty()) {
                    sendMessage(text)
                    binding.editTextInput.text?.clear()
                }
                true
            } else false
        }

        // 마이크 버튼 (음성 인식)
        binding.buttonVoice.setOnClickListener {
            toggleVoiceMode()
        }

        // 닫기 버튼
        binding.buttonClose.setOnClickListener {
            finishWithAnimation()
        }

        // TTS 버튼 (현재 응답을 음성으로 들리기)
        binding.buttonTts.setOnClickListener {
            val latestMessage = viewModel.getLatestModelMessage()
            if (latestMessage != null) {
                ttsManager?.speak(latestMessage) {
                    Toast.makeText(this, "읽기 완료", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(this, "들려줄 메시지가 없습니다", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun setupViewModel() {
        // 메시지 관찰
        viewModel.messages.observe(this) { messages ->
            if (messages.isNotEmpty()) {
                adapter.submitList(messages)
                binding.recyclerViewMessages.scrollToPosition(messages.size - 1)
            }
        }

        // 타이핑 상태 관찰
        viewModel.isTyping.observe(this) { typing ->
            if (typing) {
                binding.typingIndicator.visibility = View.VISIBLE
            } else {
                binding.typingIndicator.visibility = View.GONE
            }
        }

        // 에러 메시지 관찰
        viewModel.errorMessage.observe(this) { error ->
            error?.let {
                Toast.makeText(this, it, Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun sendMessage(text: String) {
        viewModel.sendMessage(text, isVoiceMode)
        binding.editTextInput.text?.clear()
    }

    private fun initTts() {
        ttsManager = TtsManager(this)
    }

    private fun toggleVoiceMode() {
        if (isVoiceMode) {
            switchToTextMode()
        } else {
            switchToVoiceMode()
        }
    }

    private fun switchToVoiceMode() {
        isVoiceMode = true
        binding.buttonVoice.setImageResource(R.drawable.ic_keyboard)
        binding.inputContainer.visibility = View.GONE
        binding.voiceModeHint.visibility = View.VISIBLE

        // 음성 인식 권한 확인
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
            == PackageManager.PERMISSION_GRANTED) {
            startVoiceInput()
        } else {
            requestAudioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    private fun switchToTextMode() {
        isVoiceMode = false
        binding.buttonVoice.setImageResource(R.drawable.ic_mic)
        binding.inputContainer.visibility = View.VISIBLE
        binding.voiceModeHint.visibility = View.GONE
        voiceInputManager?.stopListening()
    }

    private fun startVoiceInput() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "ko-KR")
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
        }
        try {
            speechRecognizerLauncher.launch(intent)
        } catch (e: Exception) {
            Toast.makeText(this, "음성 인식을 사용할 수 없습니다", Toast.LENGTH_SHORT).show()
        }
    }

    private fun finishWithAnimation() {
        binding.root.animate()
            .alpha(0f)
            .translationY(100f)
            .setDuration(200)
            .withEndAction {
                ttsManager?.shutdown()
                voiceInputManager?.destroy()
                finish()
                overridePendingTransition(0, 0)
            }
            .start()
    }

    override fun onDestroy() {
        super.onDestroy()
        ttsManager?.shutdown()
        voiceInputManager?.destroy()
    }
}
