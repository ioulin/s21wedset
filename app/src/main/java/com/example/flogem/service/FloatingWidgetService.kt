package com.example.flogem.service

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.DecelerateInterpolator
import androidx.core.app.NotificationCompat
import com.example.flogem.R
import com.example.flogem.ui.FloatingWidgetView
import com.example.flogem.ui.ChatPopupActivity
import com.example.flogem.util.WidgetConfigManager

/**
 * 플로팅 위젯 서비스 (Foreground Service)
 * - 화면 가장자리에 위젯 떠다니게 함 (숨쉴 때 애니메이션)
 * - 드래그로 위치 이동 (edge snap)
 * - 탭 → 채팅 팝업 또는 음성 인식
 */
class FloatingWidgetService : Service() {

    private lateinit var windowManager: WindowManager
    private lateinit var floatingWidgetView: FloatingWidgetView
    private lateinit var widgetParams: WindowManager.LayoutParams
    private lateinit var configManager: WidgetConfigManager

    private var initialX = 0
    private var initialY = 0
    private var initialTouchX = 0f
    private var initialTouchY = 0f
    private var isDragging = false

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        configManager = WidgetConfigManager(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, createNotification())

        if (!Settings.canDrawOverlays(this)) {
            stopSelf()
            return START_NOT_STICKY
        }

        setupFloatingWidget()
        return START_STICKY
    }

    @SuppressLint("InflateParams", "WrongViewHierarchyViolation")
    private fun setupFloatingWidget() {
        val savedConfig = configManager.loadConfig()

        floatingWidgetView = FloatingWidgetView(this).apply {
            setBreathing(savedConfig.isBreathing)
            setOnClickListener {
                performClickAnimation()
                openChatPopup()
            }
            setOnLongClickListener {
                openVoiceMode()
                true
            }
        }

        widgetParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else
                WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                    or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                    or WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH,
            PixelFormat.TRANSLUCENT
        )

        widgetParams.gravity = Gravity.START or Gravity.TOP
        widgetParams.x = savedConfig.x
        widgetParams.y = savedConfig.y

        // 화면 크기 기준으로 초기 위치 설정 (edge snap)
        val display = windowManager.defaultDisplay
        val screenWidth = display.width
        val screenHeight = display.height

        when (savedConfig.edge) {
            WidgetConfigManager.Edge.RIGHT -> {
                widgetParams.gravity = Gravity.END or Gravity.TOP
                widgetParams.x = 0
                widgetParams.y = screenHeight / 2 - 100
            }
            WidgetConfigManager.Edge.LEFT -> {
                widgetParams.gravity = Gravity.START or Gravity.TOP
                widgetParams.x = 0
                widgetParams.y = screenHeight / 2 - 100
            }
            WidgetConfigManager.Edge.TOP -> {
                widgetParams.gravity = Gravity.START or Gravity.TOP
                widgetParams.x = screenWidth / 2 - 100
                widgetParams.y = 0
            }
            WidgetConfigManager.Edge.BOTTOM -> {
                widgetParams.gravity = Gravity.START or Gravity.BOTTOM
                widgetParams.x = screenWidth / 2 - 100
                widgetParams.y = 0
            }
        }

        // 드래그 처리
        floatingWidgetView.setOnTouchListener(object : View.OnTouchListener {
            override fun onTouch(v: View, event: MotionEvent): Boolean {
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        isDragging = false
                        initialX = widgetParams.x
                        initialY = widgetParams.y
                        initialTouchX = event.rawX
                        initialTouchY = event.rawY
                        return true
                    }
                    MotionEvent.ACTION_MOVE -> {
                        val deltaX = (event.rawX - initialTouchX).toInt()
                        val deltaY = (event.rawY - initialTouchY).toInt()
                        widgetParams.x = initialX + deltaX
                        widgetParams.y = initialY + deltaY

                        // 드래그 중에 애니메이션 일시정지
                        floatingWidgetView.setBreathing(false)

                        // edge snap 시각적 피드백
                        updateDragFeedback(deltaX, deltaY, screenWidth, screenHeight)
                        windowManager.updateViewLayout(floatingWidgetView, widgetParams)
                        isDragging = true
                        return true
                    }
                    MotionEvent.ACTION_UP -> {
                        val snapEdge = findClosestEdge(widgetParams.x, widgetParams.y, screenWidth, screenHeight)
                        animateToEdge(snapEdge, screenWidth, screenHeight)
                        floatingWidgetView.setBreathing(true)

                        // 탭(드래그 아님) 감지
                        if (!isDragging) {
                            v.performClick()
                        }
                        return true
                    }
                    MotionEvent.ACTION_CANCEL -> {
                        floatingWidgetView.setBreathing(true)
                        return true
                    }
                }
                return false
            }
        })

        if (floatingWidgetView.isAttachedToWindow) {
            windowManager.removeView(floatingWidgetView)
        }
        windowManager.addView(floatingWidgetView, widgetParams)
    }

    private fun updateDragFeedback(deltaX: Int, deltaY: Int, screenW: Int, screenH: Int) {
        // 드래그 중 가장자리 근처일 때 시각적 피드백 (알파 변화)
        val threshold = 100
        val isNearEdge = kotlin.math.abs(deltaX) < threshold || kotlin.math.abs(deltaY) < threshold
        floatingWidgetView.alpha = if (isNearEdge) 0.6f else 1.0f
    }

    private fun findClosestEdge(x: Int, y: Int, screenW: Int, screenH: Int): WidgetConfigManager.Edge {
        val distances = listOf(
            WidgetConfigManager.Edge.LEFT to x.toFloat(),
            WidgetConfigManager.Edge.TOP to y.toFloat(),
            WidgetConfigManager.Edge.RIGHT to (screenW - x).toFloat(),
            WidgetConfigManager.Edge.BOTTOM to (screenH - y).toFloat()
        )
        return distances.minByOrNull { it.second }?.first
            ?: WidgetConfigManager.Edge.RIGHT
    }

    private fun animateToEdge(edge: WidgetConfigManager.Edge, screenW: Int, screenH: Int) {
        val targetX: Int
        val targetY: Int
        val widgetSize = 200 // approx widget size in pixels

        when (edge) {
            WidgetConfigManager.Edge.RIGHT -> {
                widgetParams.gravity = Gravity.END or Gravity.TOP
                targetX = 0
                targetY = widgetParams.y
            }
            WidgetConfigManager.Edge.LEFT -> {
                widgetParams.gravity = Gravity.START or Gravity.TOP
                targetX = 0
                targetY = widgetParams.y
            }
            WidgetConfigManager.Edge.TOP -> {
                widgetParams.gravity = Gravity.START or Gravity.TOP
                targetX = screenW / 2 - widgetSize / 2
                targetY = 0
            }
            WidgetConfigManager.Edge.BOTTOM -> {
                widgetParams.gravity = Gravity.START or Gravity.BOTTOM
                targetX = screenW / 2 - widgetSize / 2
                targetY = 0
            }
        }

        widgetParams.x = targetX
        widgetParams.y = targetY

        // 부드러운 이동 애니메이션
        floatingWidgetView.animate()
            .x(targetX.toFloat())
            .y(targetY.toFloat())
            .setDuration(500)
            .setInterpolator(DecelerateInterpolator())
            .start()

        // 위치 저장
        configManager.saveConfig(edge, targetX, targetY)
    }

    private fun openChatPopup() {
        val intent = Intent(this, ChatPopupActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        startActivity(intent)
    }

    private fun openVoiceMode() {
        // 길게 탭 → 채팅 팝업을 음성 모드로 열기
        val intent = Intent(this, ChatPopupActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("voice_mode", true)
        }
        startActivity(intent)
    }

    private fun createNotification(): Notification {
        val notificationText = "화면에 떠다니는 위젯이 활성화되었습니다"
        val channelId = "floating_widget_channel"

        return NotificationCompat.Builder(this, channelId)
            .setContentTitle(getString(R.string.app_name))
            .setContentText(notificationText)
            .setSmallIcon(R.drawable.ic_widget_logo)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "floating_widget_channel",
                "플로팅 위젯 서비스",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "화면에 떠다니는 위젯을 관리합니다"
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    override fun onDestroy() {
        if (::floatingWidgetView.isInitialized) {
            try {
                windowManager.removeView(floatingWidgetView)
            } catch (e: IllegalArgumentException) {
                // already removed
            }
        }
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        private const val NOTIFICATION_ID = 1001
    }
}
