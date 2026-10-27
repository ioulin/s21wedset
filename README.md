# FloGem - Floating Widget with Gemini API

Android 앱: 화면 가장자리에 떠다니는 위젯 플로팅 + 제미나이 API 대화 기능

## 주요 기능

- **플로팅 위젯**: 화면 가장자리 숨쉬듯이 부드럽게 떠다니는 움직임
- **제미나이 API**: 위젯 탭 → 대화창 팝업 또는 음성인식 → 제미나이와 대화
- **STT/TTS**: 음성인식 마이크 + 텍스트-음성 변환
- **보안**: API 키는 EncryptedSharedPreferences로 암호화 저장

## 빌드 방법

### GitHub Actions (권장)
푸시하거나 PR을 생성하면 자동으로 Debug APK가 빌드됩니다.
- Artifacts에서 `app-debug.apk` 다운로드

### 로컬 빌드 (Linux x86_64)
```bash
./gradlew assembleDebug
```

## 프로젝트 구조

```
app/src/main/
├── java/com/example/flogem/
│   ├── domain/model/      # 도메인 모델 (ChatMessage, Session, WidgetConfig, ApiKeyConfig)
│   ├── data/
│   │   ├── model/         # API 요청/응답 모델 (GeminiRequest, GeminiResponse)
│   │   ├── remote/        # 네트워크 계층 (GeminiApi, GeminiApiClient)
│   │   ├── local/         # Room DB (AppDatabase, MessageDao, SessionDao)
│   │   └── repository/     # Repository (ConversationRepository)
│   ├── di/                # Hilt DI (AppModule)
│   ├── ui/                # UI (ChatPopupActivity, MainActivity, SettingsActivity, FloatingWidgetView)
│   ├── ui/adapter/        # RecyclerView 어댑터 (ChatAdapter)
│   ├── util/              # 유틸리티 (ApiKeyManager, VoiceInputManager, TtsManager, NetworkUtils, WidgetConfigManager)
│   ├── viewmodel/         # ViewModel (ChatViewModel)
│   └── service/           # 원격 서비스 (FloatingWidgetService, TtsInitReceiver)
└── res/
    ├── layout/            # 레이아웃 파일
    ├── drawable/          # 드로어블
    ├── values/            # 문자열, 색상, 테마, 디멘션
    ├── anim/              # 애니메이션
    └── xml/               # 네트워크 보안 설정
```

## 설정

### 제미나이 API 키
앱 시작 시 Settings 화면에서 API 키를 입력하면 EncryptedSharedPreferences에 안전하게 저장됩니다.

### 모델
기본 모델: `gemini-1.5-flash`

## 라이선스
MIT
