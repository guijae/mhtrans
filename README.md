# MH그룹 AI통역 - Android APK 빌드 가이드

## 기능
- 한국어 ↔ 베트남어 실시간 통역
- MLKit 오프라인 번역 (언어팩 최초 1회 다운로드)
- 음성 파형 시각화
- 대화 TXT 저장 / 삭제
- 카카오톡 공유
- 큰 글씨 UI

---

## 빌드 방법 (5단계)

### 1단계. Android Studio 설치
- https://developer.android.com/studio 에서 다운로드
- 설치 후 실행

### 2단계. 프로젝트 열기
- Android Studio 실행
- `File > Open` 선택
- 이 폴더(MHTranslator) 선택

### 3단계. Vosk 모델 추가 (중요!)
Vosk 오프라인 음성인식 모델을 다운로드해서 추가해야 합니다.

1. https://alphacephei.com/vosk/models 접속
2. `vosk-model-small-ko-0.22` 다운로드 (한국어, 약 82MB)
3. `vosk-model-small-vi-0.4` 다운로드 (베트남어, 약 37MB)
4. 압축 해제 후 폴더명 변경:
   - `vosk-model-small-ko` (한국어)
   - `vosk-model-small-vi` (베트남어)
5. `app/src/main/assets/` 폴더 안에 두 폴더 복사

### 4단계. 빌드
- Android Studio 상단 `▶ Run` 버튼 클릭
- 또는 `Build > Build Bundle(s)/APK(s) > Build APK(s)`

### 5단계. APK 파일 위치
```
MHTranslator/app/build/outputs/apk/debug/app-debug.apk
```

---

## 저장 폴더
대화 기록: `/내부저장소/Documents/MH그룹통역기록/`

---

## 주의사항
- 최초 실행 시 MLKit 번역 팩 다운로드 필요 (약 50~100MB)
- Vosk 모델 없이는 음성인식 안 됨 (3단계 필수)
- Android 7.0 (API 24) 이상 필요
