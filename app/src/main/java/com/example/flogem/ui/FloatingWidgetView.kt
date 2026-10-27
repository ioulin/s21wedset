package com.example.flogem.ui

import android.content.Context
import android.graphics.drawable.GradientDrawable
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.FrameLayout
import android.widget.ImageView
import androidx.annotation.DrawableRes
import androidx.core.content.ContextCompat
import com.example.flogem.R
import com.example.flogem.databinding.WidgetFloatingBinding

/**
 * 플로팅 위젯 뷰
 * 화면 모서리에 떠다니며 '숨쉬는' 애니메이션을 실행
 * - scale 1.0 → 1.15 → 1.0 (3초 주기, ease-in-out)
 * - alpha 0.8 → 1.0 → 0.8
 * - position: 약간의 상하 oscillation (±10dp)
 */
class FloatingWidgetView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    private val binding: WidgetFloatingBinding
    private var isBreathing = true
    private var baseY = 0f

    init {
        LayoutInflater.from(context).inflate(R.layout.widget_floating, this, true)
        binding = WidgetFloatingBinding.bind(this)
        initView()
    }

    private fun initView() {
        // 위젯 배경: 흰색 라운드 + 반투명 + 그림자
        val background = GradientDrawable().apply {
            setColor(ContextCompat.getColor(context, R.color.widget_bg))
            setCornerRadius(dpToPx(28f)) // 56dp / 2
            setShape(GradientDrawable.OVAL)
        }
        binding.widgetImage.background = background
        setShadow()
    }

    /**
     * 숨쉴 때 애니메이션 시작/중지
     */
    fun setBreathing(enabled: Boolean) {
        isBreathing = enabled
        if (enabled) startBreathingAnimation() else stopBreathingAnimation()
    }

    private fun startBreathingAnimation() {
        val animatorSet = android.animation.AnimatorSet()

        // Scale 애니메이션 (1.0 → 1.15 → 1.0)
        val scaleUpX = android.animation.ObjectAnimator.ofFloat(this, "scaleX", 1.0f, 1.15f)
        val scaleUpY = android.animation.ObjectAnimator.ofFloat(this, "scaleY", 1.0f, 1.15f)
        val scaleDownX = android.animation.ObjectAnimator.ofFloat(this, "scaleX", 1.15f, 1.0f)
        val scaleDownY = android.animation.ObjectAnimator.ofFloat(this, "scaleY", 1.15f, 1.0f)

        // Position Oscillation (±10dp 상하)
        val moveUp = android.animation.ObjectAnimator.ofFloat(this, "translationY", baseY, baseY - dpToPx(10f))
        val moveDown = android.animation.ObjectAnimator.ofFloat(this, "translationY", baseY - dpToPx(10f), baseY)

        // Alpha 변화 (0.8 → 1.0 → 0.8)
        val alphaUp = android.animation.ObjectAnimator.ofFloat(this, "alpha", 0.8f, 1.0f)
        val alphaDown = android.animation.ObjectAnimator.ofFloat(this, "alpha", 1.0f, 0.8f)

        // 첫 번째 절반 (확대 + 위로 이동 + 불투명도 증가)
        animatorSet.playTogether(scaleUpX, scaleUpY, moveUp, alphaUp)
        animatorSet.duration = 1500
        animatorSet.interpolator = AccelerateDecelerateInterpolator()

        // 두 번째 절반 (축소 + 아래로 이동 + 불투명도 감소)
        val reverseSet = android.animation.AnimatorSet()
        reverseSet.playTogether(scaleDownX, scaleDownY, moveDown, alphaDown)
        reverseSet.duration = 1500
        reverseSet.interpolator = AccelerateDecelerateInterpolator()

        // 전체 반복 (무한 루프, REVERSE 모드)
        val fullSet = android.animation.AnimatorSet()
        fullSet.playSequentially(animatorSet, reverseSet)
        fullSet.duration = 3000
        fullSet.addListener(object : android.animation.AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: android.animation.Animator) {
                if (isBreathing) {
                    animation.start()
                }
            }
        })

        fullSet.start()
    }

    private fun stopBreathingAnimation() {
        // Running animations on this view
        val animators = listOf("scaleX", "scaleY", "translationY", "alpha")
        // AnimatorSet 자동 종료
    }

    /**
     * 드래그로 위치 이동 시 호출
     */
    fun setPosition(x: Float, y: Float) {
        this.x = x
        this.y = y
        baseY = y
    }

    /**
     * 탭 시 눌림 효과
     */
    fun performClickAnimation() {
        animate()
            .scaleX(0.9f)
            .scaleY(0.9f)
            .setDuration(100)
            .withEndAction {
                animate()
                    .scaleX(1.0f)
                    .scaleY(1.0f)
                    .setDuration(100)
                    .start()
            }
            .start()
    }

    private fun setShadow() {
        // API 21+ 그림자 (elevation)
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            elevation = dpToPx(8f)
            translationZ = dpToPx(2f)
        }
    }

    private fun dpToPx(dp: Float): Float {
        return dp * context.resources.displayMetrics.density
    }
}
