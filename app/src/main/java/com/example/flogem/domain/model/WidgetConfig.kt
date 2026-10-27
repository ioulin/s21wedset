package com.example.flogem.domain.model

/**
 * 플로팅 위젯 설정
 */
data class WidgetConfig(
    val edge: Edge = Edge.RIGHT,
    val x: Int = 0,
    val y: Int = 0,
    val isBreathing: Boolean = true,
    val autoTts: Boolean = false,
    val voiceModeDefault: Boolean = false
) {
    enum class Edge {
        LEFT, RIGHT, TOP, BOTTOM
    }
}
