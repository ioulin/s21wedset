# 플로팅 위젯 + 제미나이 API 대화 앱 기획서

> **프로젝트명**: FloGem (Floating Gemini)
> **버전**: 1.0.0
> **플랫폼**: Android (aarch64 / ARM64)
> **작성일**: 2026-10-27

---

## 1. 프로젝트 개요

### 1.1 비전
화면을 떠다니는 플로팅 위젯(플로팅)이 화면 모서리를 따라 숨쉬듯이 움직이며, 사용자가 언제든 탭 한 번으로 제미나이(Gemini) API와 대화할 수 있도록 한다. 채팅 창 팝업과 음성 인식(마이크) 두 가지 입력 방식을 제공한다.

### 1.2 핵심 기능
| 기능 | 설명 |
|---|---|
| 플로팅 위젯 | 화면 가장자리에 떠다니는 원형/아바타 위젯 (항상 위층, 투명 배경) |
| 숨코 피는 듯한 애니메이션 | scale + position oscillation 으로 부드러운 호흡감 |
| 탭 → 채팅 팝업 | 위젯 탭 시 대화창 다이얼로그/액티비티 표시 |
| 탭 → 음성 인식 | 위젯 길게 탭 또는 버튼 전환 시 마이크 활성화 |
| 제미나이 API 연동 | Gemini API 실시간 스트리밍 응답 |
| TTS 음성 출력 | 제미나이 응답을 음성으로 출력 (선택) |

---

## 2. 사용자 사용 시나리오

```
[1] 앱 첫 실행 → SYSTEM_ALERT_WINDOW 권한 요청
[2] 플로팅 위젯이 화면 오른쪽 가장자리에 자동 등장
    → 부드럽게 확대/축소 & 약간 상하 oscillation (숨쉬는 느낌)
[3] 사용자가 위젯 탭
    → 대화창 팝업 (하단에서 슬라이드 업)
[4] 텍스트 입력 또는 마이크 버튼 → 음성 인식
[5] 제미나이 API에 전송 → 스트리밍 응답 실시간 표시
[6] 응답 완료 → TTS로 듣기 / 텍스트로 보기 선택
[7] 창 닫기 → 위젯 다시 떠다님
[8] 위젯 드래그 → 위치 고정 (좌/우/상/하 가장자리 스냅)
```

---

## 3. 시스템 아키텍처

### 3.1 컴포넌트 구조

```
┌─────────────────────────────────────────────────────┐
│              Android System (Window Manager)         │
├─────────────────────────────────────────────────────┤
│  FloatingWidgetService (Foreground Service)          │
│  ├─ FloatingWidgetView (breathing animation)       │
│  ├─ WidgetDragHandler (edge snap positioning)      │
│  ├─ PermissionChecker (SYSTEM_ALERT_WINDOW)          │
├─────────────────────────────────────────────────────┤
│  GeminiApiClient (Retrofit + OkHttp + Gson)        │
│  ├─ API Key Manager (Encrypted SharedPreferences)   │
│  ├─ Streaming SSE Client (OkHttp EventSource)      │
│  └─ ConversationManager (message history buffer)   │
├─────────────────────────────────────────────────────┤
│  ChatPopup (Activity 또는 DialogFragment)            │
│  ├─ ChatAdapter (RecyclerView)                     │
│  ├─ VoiceInputButton → SpeechToText                │
│  └─ TTS Button → TextToSpeech                     │
├─────────────────────────────────────────────────────┤
│  AudioManager                                        │
│  ├─ STTRecorder (음성 → 텍스트)                     │
│  └─ TTSSpeaker (텍스트 → 음성)                      │
└─────────────────────────────────────────────────────┘
```

### 3.2 계층 구조 (Clean Architecture)

```
Presentation Layer
  ├── FloatingWidgetView
  ├── ChatPopupActivity
  └── ChatAdapter

Domain Layer
  ├── ConversationUseCase
  ├── SendQueryUseCase
  └── ManageWidgetStateUseCase

Data Layer
  ├── GeminiApiDataSource (Retrofit)
  ├── ConversationDao (Room)
  ├── ApiKeyRepository
  └── AudioRepository (STT/TTS)
```

---

## 4. 상세 설계

### 4.1 플로팅 위젯 (Floating Widget)

#### 4.1.1 윈도우 매니저 설정
```kotlin
val params = WindowManager.LayoutParams(
    WindowManager.LayoutParams.WRAP_CONTENT,
    WindowManager.LayoutParams.WRAP_CONTENT,
    // Android 8+ : TYPE_APPLICATION_OVERLAY
    // Android 6~7 : TYPE_PHONE
    WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
        or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
        or WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH
        or WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON,
    PixelFormat.TRANSLUCENT
)
```

#### 4.1.2 숨쉬는 애니메이션 (Breathing Animation)
- **Scale Animation**: 1.0f → 1.15f → 1.0f (주기 3초, ease-in-out)
- **Position Oscillation**: 화면 가장자리 기준 ±10dp 상하 흔들림
- **Alpha 변화**: 0.8f → 1.0f → 0.8f (함께 동작)

```kotlin
val animatorSet = AnimatorSet()
val scaleUpX = ObjectAnimator.ofFloat(widget, "scaleX", 1.0f, 1.15f)
val scaleDownX = ObjectAnimator.ofFloat(widget, "scaleX", 1.15f, 1.0f)
val scaleUpY = ObjectAnimator.ofFloat(widget, "scaleY", 1.0f, 1.15f)
val scaleDownY = ObjectAnimator.ofFloat(widget, "scaleY", 1.15f, 1.0f)
val moveUp = ObjectAnimator.ofFloat(widget, "y", baseY, baseY - 10f)
val moveDown = ObjectAnimator.ofFloat(widget, "y", baseY - 10f, baseY)
val alphaAnim = ObjectAnimator.ofFloat(widget, "alpha", 0.8f, 1.0f)

animatorSet.playTogether(scaleUpX, scaleUpY, moveUp, alphaAnim)
animatorSet.duration = 1500
animatorSet.interpolator = AccelerateDecelerateInterpolator()
animatorSet.repeatCount = ValueAnimator.INFINITE
animatorSet.repeatMode = ValueAnimator.REVERSE
```

#### 4.1.3 위치 스냅 (Edge Snapping)
- 4개의 가장자리 (좌/우/상/하) 중 하나에 스냅
- 드래그 완료 시 가장 가까운 가장자리로 부드럽게 이동 (Spring Animation)
- 사용자가 위치를 고정하면 SharedPreferences에 저장

### 4.2 제미나이 API 클라이언트

#### 4.2.1 API 엔드포인트
```
POST https://generativelanguage.googleapis.com/v1beta/models/{model}:generateContent?key={API_KEY}
WebSocket: wss://generativelanguage.googleapis.com/ws/google.ai... (실시간 스트리밍)
```

#### 4.2.2 주요 모델
| 모델 | 용도 | 특징 |
|---|---|---|
| gemini-1.5-flash | 기본 대화 | 빠른 응답, 효율적 |
| gemini-1.5-pro | 복잡한 추론 | 높은 정확도, 비용↑ |
| gemini-2.0-flash | 최신 | 이미지+텍스트 멀티모달 |

#### 4.2.3 요청/응답 스키마 (JSON)

**요청:**
```json
{
  "contents": [{
    "role": "user",
    "parts": [{"text": "안녕, 오늘 날씨 어때?"}]
  }],
  "generationConfig": {
    "temperature": 0.7,
    "topK": 40,
    "topP": 0.95,
    "maxOutputTokens": 2048
  },
  "safetySettings": [
    {"category": "HARM_CATEGORY_HARASSMENT", "threshold": "BLOCK_MEDIUM_AND_ABOVE"}
  ]
}
```

**응답 (스트리밍):**
```json
{
  "candidates": [{
    "content": {
      "parts": [{"text": "안녕하세요!"}],
      "role": "model"
    },
    "finishReason": "STOP",
    "index": 0
  },
  "usageMetadata": {
    "promptTokenCount": 10,
    "candidatesTokenCount": 2048,
    "totalTokenCount": 2058
  }]
}
```

#### 4.2.4 대화 컨텍스트 관리
- 최근 N개 메시지 (기본 10turn) 유지
- 토큰 제한 (최대 200K 토큰) → 오래된 메시지 LRU 삭제
- Room DB에 대화 저장 (로컬 히스토리)
- 세션별 구분 (Session ID)

### 4.3 채팅 팝업 UI

#### 4.3.1 레이아웃
```
┌──────────────────────────────────┐
│  ┌─ 헤더 (제목 + 닫기 버튼) ─────┐ │
│  │ FloGem · Gemini 채팅       ✕ │ │
│  └─────────────────────────────┘ │
│  ┌─ 채팅 영역 (RecyclerView) ───┐ │
│  │                               │ │
│  │  [말풍선] 안녕하세요!         │ │
│  │  [입력] 오늘은 어때?          │ │
│  │  [말풍선] ...                 │ │
│  └─────────────────────────────┘ │
│  ┌─ 입력 영역 ──────────────────┐ │
│  │ [마이크] [텍스트 입력] [전송]  │ │
│  └─────────────────────────────┘ │
└──────────────────────────────────┘
```

#### 4.3.2 채팅 말풍선
| 구분 | 스타일 |
|---|---|
| 사용자 | 오른쪽 정렬, 파란 원형 말풍선, 하얀 텍스트 |
| 제미나이 | 왼쪽 정렬, 회색 원형 말풍선, 검정 텍스트, 타이핑 애니메이션 (점 3개) |

#### 4.3.3 입력 방식 전환
- **텍스트 모드**: 에딧텍스트 + 전송 버튼
- **음성 모드**: 마이크 버튼 → 녹음 → STT → 전송
- 버튼 토글로 모드 전환 (마이크 ↔ 키보드 아이콘)

### 4.4 음성 인식 (STT)

#### 4.4.1 녹음 흐름
```
1. 마이크 버튼 탭 → 녹음 시작 (RecordButton 애니메이션: 빨간 원 + 파형)
2. SpeechRecognizer 또는 직접 녹음 (AudioRecord)
   - Google STT API (온라인)
   - 또는 로컬 STT (TensorFlow Lite 모델) — 오프라인 지원
3. 인식된 텍스트를 채팅창에 삽입
4. 자동 전송 or 전송 버튼 탭
```

#### 4.4.2 주요 API
```kotlin
val speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context)
val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
    putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
    putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)  // 실시간 스트리밍 인식
}
speechRecognizer.setRecognitionListener(object : RecognitionListener {
    override fun onResults(results: Bundle) { /* 최종 결과 */ }
    override fun onPartialResults(partial: Bundle) { /* 중간 결과 실시간 표시 */ }
})
```

### 4.5 TTS (텍스트 → 음성)

#### 4.5.1 음성 출력
```kotlin
val tts = TextToSpeech(context) { status ->
    if (status == TextToSpeech.SUCCESS) {
        tts.language = Locale.KOREAN  // 한국어 음성
        tts.setPitch(1.0f)            // 기본 음높이
        tts.setSpeechRate(1.0f)       // 기본 속도
        tts.speak(responseText, TextToSpeech.QUEUE_FLUSH, null, "uttId")
    }
}
```

#### 4.5.2 자동 TTS 재생
- 설정에서 "자동 음성 출력" 토글 ON
- 제미나이 응답 스트리밍이 끝날 때까지 대기 → TTS 시작
- 중간에 멈추기/일시정지 버튼 제공

---

## 5. UI/UX 세부 디자인

### 5.1 플로팅 위젯 디자인
- **기본 상태**: 56dp 원형, 아바타 또는 로고
  - 테두리: 2dp, 반짝이는 효과 (GradientDrawable)
  - 배경: 반투명 (alpha 0.85)
  - 그림자: 부드러운 외곽선 (elevation 8dp)
- **숨 쉬는 상태**: scale 1.0~1.15, alpha 0.8~1.0
- **탭 시**: 살짝 축소 애니메이션 (눌림 효과)

### 5.2 채팅 팝업 디자인
- **진입 애니메이션**: 하단에서 슬라이드 업 + fade in
- **배경**: 반투명 다크 (dimened 0.4)
- **채팅 영역**: 흰색 라운드 상단, 최대 높이 60% 화면
- **입력 영역**: 라운드 하단, 고정-bottom

### 5.3 색상 팔레트
```
Primary:   #4285F4 (Google Blue)
Secondary: #34A853 (Google Green)  
Accent:    #FBBC05 (Google Yellow)
Error:     #EA4335 (Google Red)
Widget BG: #FFFFFF (white, alpha 0.9)
Chat Bubble (User):     #4285F4
Chat Bubble (Gemini):   #F1F3F4 (light gray)
```

### 5.4 애니메이션 지속 시간
| 동작 | 지속 시간 |
|---|---|
| 숨쉬기 (한 사이클) | 3000ms |
| 채팅 팝업 등장 | 300ms (ease-out) |
| 채팅 팝업 닫기 | 200ms (ease-in) |
| 타이핑 인디케이터 | 1000ms (무한 반복) |
| 위젯 위치 스냅 | 500ms (spring) |

---

## 6. 기술 스택

| 계층 | 기술 | 버전/사양 |
|---|---|---|
| 언어 | Kotlin | 1.9+ |
| 최소 SDK | Android API | 24 (Android 7.0) |
| 타겟 SDK | | 34 (Android 14) |
| UI | Jetpack Compose / View | 혼용 (플로팅 위젯은 View, 채팅은 Compose) |
| API 클라이언트 | Retrofit + OkHttp | 최신 |
| JSON | Gson / Kotlinx.serialization | |
| 스트리밍 | OkHttp SSE / WebSocket | |
| 로컬 DB | Room | 2.5+ |
| DI | Hilt | 2.48+ |
| STT | Android SpeechRecognizer / Google STT API | |
| TTS | Android TextToSpeech | |
| 권한 | SYSTEM_ALERT_WINDOW | Settings.ACTION_MANAGE_OVERLAY_PERMISSION |

### 6.1 외부 의존성 (build.gradle)
```gradle
dependencies {
    // AndroidX
    implementation "androidx.core:core-ktx:1.13.1"
    implementation "androidx.appcompat:appcompat:1.7.0"
    implementation "com.google.android.material:material:1.12.0"
    implementation "androidx.room:room-ktx:2.6.1"
    implementation "androidx.room:room-runtime:2.6.1"
    kapt "androidx.room:room-compiler:2.6.1"

    // Hilt (DI)
    implementation "com.google.dagger:hilt-android:2.51.1"
    kapt "com.google.dagger:hilt-android-compiler:2.51.1"

    // 네트워킹
    implementation "com.squareup.retrofit2:retrofit:2.11.0"
    implementation "com.squareup.retrofit2:converter-gson:2.11.0"
    implementation "com.squareup.okhttp3:okhttp:4.12.4"
    implementation "com.squareup.okhttp3:logging-interceptor:4.12.4"

    // 스트리밍 SSE
    implementation "com.squareup.okhttp3:okhttp-sse:4.12.4"

    // 음성
    implementation "com.google.code.gson:gson:2.11.0"
}
```

### 6.2 필수 권한 (AndroidManifest.xml)
```xml
<uses-permission android:name="android.permission.SYSTEM_ALERT_WINDOW" />
<uses-permission android:name="android.permission.RECORD_AUDIO" />
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />

<application
    android:usesCleartextTraffic="false"
    ... >
    <service
        android:name=".service.FloatingWidgetService"
        android:permission="android.permission.BIND_ACCESSIBILITY_SERVICE"
        android:exported="false" />
</application>
```

---

## 7. API 흐름도

### 7.1 채팅 API 호출 흐름
```
사용자 입력
    │
    ▼
[ConversationManager]
    │ 1. 입력 텍스트 + 대화 기록 수집
    ▼
[GeminiApiClient]
    │ 2. POST /v1beta/models/gemini-1.5-flash:generateContent
    │    + 헤더: x-goog-api-key: {API_KEY}
    ▼
[Gemini API Server]
    │ 3. 스트리밍 응답 (SSE/Chunked)
    ▼
[ChatPopup]
    │ 4. 응답 청크 실시간 UI 업데이트 (점진적 텍스트 표시)
    ▼
사용자에게 표시
```

### 7.2 음성 인식 → 대화 흐름
```
1. 마이크 버튼 탭
    ▼
2. SpeechRecognizer 시작 (실시간 partial results)
    │   - onPartialResults: "안녕하" (실시간 표시)
    │   - onResults: "안녕하세요" (최종)
    ▼
3. 인식된 텍스트 → 채팅창 삽입 → 자동 전송
    ▼
4. Gemini API 호출 (7.1 흐름)
    ▼
5. 응답 텍스트 → TTS 재생
```

---

## 8. 데이터 모델

### 8.1 Message (대화 메시지)
```kotlin
@Entity(tableName = "messages")
data class Message(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val sessionId: String,
    val role: Role,            // USER, MODEL, SYSTEM
    val content: String,
    val timestamp: Long = System.currentTimeMillis(),
    val isTyping: Boolean = false,   // 타이핑 중인지 여부
    val isError: Boolean = false    // 전송 실패 여부
)

enum class Role { USER, MODEL, SYSTEM }
```

### 8.2 Session (대화 세션)
```kotlin
@Entity(tableName = "sessions")
data class Session(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val title: String,           // "2026-10-27 대화" (자동 생성)
    val createdAt: Long,
    val updatedAt: Long,
    val messageCount: Int
)
```

### 8.3 WidgetConfig (위젯 설정)
```kotlin
data class WidgetConfig(
    val edge: Edge = Edge.RIGHT,          // 현재 위치한 가장자리
    val x: Int = 0,                       // X 좌표
    val y: Int = 0,                       // Y 좌표
    val isBreathing: Boolean = true,      // 숨쉬기 애니메이션 on/off
    val autoTts: Boolean = false,         // 자동 TTS on/off
    val voiceModeDefault: Boolean = false // 기본 입력 방식 (음성)
)

enum class Edge { LEFT, RIGHT, TOP, BOTTOM }
```

---

## 9. 에러 처리 및 예외 상황

| 상황 | 처리 방안 |
|---|---|
| API 키 없음 / 유효하지 않음 | 설정 화면 → "API 키 입력" → 직접 입력 또는 QR 코드 스캔 |
| 네트워크 오프라인 | 채팅창에 "오프라인 상태입니다" 메시지 + 재시도 버튼 |
| API 할당량 초과 | "이번 달 할당량을 초과했습니다" + 설정에서 할당량 확인 링크 |
| 음성 인식 실패 | "음성을 인식하지 못했습니다. 다시 시도해주세요" + 텍스트 모드 전환 |
| TTS 실행 불가 | "TTS 엔진을 사용할 수 없습니다" + 설정 → TTS 엔진 선택 |
| 권한 거부 (SYSTEM_ALERT_WINDOW) | "플로팅 위젯을 위해서는 권한이 필요합니다" → 설정 화면 이동 버튼 |
| 앱 충돌 / ANR | 자동 재시작 (Foreground Service) + 로컬 로그 기록 |

---

## 10. 설정 화면

### 10.1 설정 항목
```
[FloGem 설정]

┌─────────────────────────────┐
│ API 설정                    ▼ │
│ ├─ 제미나이 API 키 [••••••]   │
│ ├─ 기본 모델 [gemini-1.5-flash ▼] │
│ ├─ Temperature [0.7]          │
│ └─ Max Output Tokens [2048]   │
└─────────────────────────────┘

┌─────────────────────────────┐
│ 플로팅 위젯                 ▼ │
│ ├─ 숨쉴 때 애니메이션 [ON]     │
│ ├─ 위치 고정 [오른쪽]            │
│ ├─ 투명도 [85%]               │
│ └─ 크기 [56dp]                │
└─────────────────────────────┘

┌─────────────────────────────┐
│ 음성/대화                  ▼ │
│ ├─ 자동 음성 출력 [OFF]       │
│ ├─ 기본 입력 방식 [텍스트]      │
│ ├─ 음성 속도 [1.0x]           │
│ └─ 언어 [한국어]              │
└─────────────────────────────┘

┌─────────────────────────────┐
│ 데이터 관리                 ▼ │
│ ├─ 대화 기록 보기             │
│ ├─ 캐시 삭제                  │
│ └─ 설정 초기화                │
└─────────────────────────────┘
```

---

## 11. 성능 최적화 전략

### 11.1 배터리 절약
- **숨쉴 때 애니메이션**: ValueAnimator 대신 `AnimatedVectorDrawable` 사용 (GPU 최적화)
- **위젯 숨김**: 사용자가 5분간 상호작용 없으면 투명도 낮추기 (0.3f)
- **백그라운드에서 API 호출**: 최소화 (Foreground Service + wake lock)

### 11.2 메모리 최적화
- 스트리밍 응답 → 청크 단위로 UI 업데이트 (전체 버퍼링 금지)
- 대화 기록 → Room 페이징 (20개 이하 메모리 유지)
- 이미지 응답 (제미나이 멀티모달) → Glaser로 다운샘플링

### 11.3 네트워크 최적화
- OkHttp `connection pooling` 활성화
- 응답 캐싱 (동일 질문 → 10분간 캐시)
- 재시도 전략: 네트워크 연결 복구 시 자동 재전송

---

## 12. 배포 및 출시 계획

### 12.1 개발 로드맵

| 단계 | 기능 | 목표일 |
|---|---|---|
| 1단계 | 플로팅 위젯 + 숨쉴 때 애니메이션 | 2026.11.10 |
| 2단계 | 제미나이 API 기본 연동 + 채팅 | 2026.11.20 |
| 3단계 | 스트리밍 응답 + TTS | 2026.11.30 |
| 4단계 | 음성 인식 (STT) | 2026.12.10 |
| 5단계 | 설정 화면 + 로컬 저장 | 2026.12.15 |
| 6단계 | 베타 테스트 + 출시 | 2026.12.25 |

### 12.2 테스트 전략
- **단위 테스트**: GeminiApiClient, ConversationManager (JUnit + MockK)
- **UI 테스트**: 채팅 팝업, 위젯 탭 동작 (Espresso)
- **통합 테스트**: STT → API → TTS 전체 흐름

---

## 13. 위험 요인 및 대응

| 위험 | 대응 방안 |
|---|---|
| 제미나이 API 비용 증가 | 토큰 사용량 실시간 표시 + 월간 한도 설정 |
| Android 12+ 제한 강화 | `MANAGE_EXTERNAL_STORAGE` 대신 `Storage Access Framework` |
| 배터리 최적화로 인한 서비스 종료 | "배터리 최적화 제외" 안내 다이얼로그 제공 |
| 음성 인식 정확도 | fallback: 직접 텍스트 입력 + 자동 수정 제안 |
| API 키 유출 | 암호화 (EncryptedSharedPreferences) + 코드 난독화 (ProGuard) |

---

## 14. 향후 확장 가능성

### 14.1 멀티모달 지원
- 제미나이 2.0 이미지 입력: 화면 캡처 → 위젯에 이미지 전송 → "이거 뭐야?" 분석
- 카메라 권한으로 실시간 이미지 스트리밍

### 14.2 멀티 에이전트
- 위젯을 떠다니면서 여러 컨텍스트의 작업을 수행
- 예: "메일 확인해줘", "날씨 알려줘" → 각각의 에이전트에게 라우팅

### 14.3 에지 컴퓨팅
- 자주 쓰는 질문은 로컬 LLM (Phi-3 Mini)으로 캐싱
- 오프라인 모드 지원

### 14.4 크로스플랫폼
- Flutter/Dart로 포팅 → iOS 위젯 (iOS는 제한적)
- Windows/Mac 위젯 (Electron)

---

## 15. 부록

### 15.1 제미나이 API 키 발급 방법
1. https://aistudio.google.com/ 접속
2. Google 계정 로그인
3. "Get API Key" → 새 프로젝트 생성 또는 선택
4. API 키 복사 → 앱 설정에 입력

### 15.2 제미나이 API 가격정책 (2026년 기준)
| 모델 | 1M 토큰당 가격 |
|---|---|
| gemini-1.5-flash | $0.075 / $0.30 |
| gemini-1.5-pro | $1.60 / $4.00 |
| gemini-2.0-flash | $0.035 / $0.14 |

> 무료 할당량: 월 1,500회 (프로모션 기간 동안)

---

*본 기획서는 AndroidTermux 환경 (cline-termux 프로젝트 기반)에서 작성되었습니다.*
*마지막 업데이트: 2026-10-27*
