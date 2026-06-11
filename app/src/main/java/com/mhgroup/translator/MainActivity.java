package com.mhgroup.translator;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.mlkit.nl.translate.TranslateLanguage;
import com.google.mlkit.nl.translate.Translation;
import com.google.mlkit.nl.translate.Translator;
import com.google.mlkit.nl.translate.TranslatorOptions;

import java.util.ArrayList;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    // UI
    private TextView tvKoText, tvViText, tvStatus, tvSpeakLabel;
    private Button btnStart, btnHistory;
    private WaveformView waveformView;
    private LinearLayout layoutListening;

    // 음성인식
    private SpeechRecognizer speechRecognizer;
    private boolean isListening = false;
    private boolean isKorean = true;
    private boolean isTranslating = false; // 연속 발화 제어용 플래그

    // 번역
    private Translator koToViTranslator, viToKoTranslator;

    // TTS
    private TextToSpeech tts;
    private boolean isTtsReady = false;

    // 히스토리 및 자동 저장
    private HistoryManager historyManager;
    private StringBuilder currentSession = new StringBuilder();

    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initViews();
        initTranslators();
        initSpeechRecognizer();
        initTTS();
        historyManager = new HistoryManager(this);
    }

    private void initViews() {
        tvKoText = findViewById(R.id.tv_ko_text);
        tvViText = findViewById(R.id.tv_vi_text);
        tvStatus = findViewById(R.id.tv_status);
        tvSpeakLabel = findViewById(R.id.tv_speak_label);
        btnStart = findViewById(R.id.btn_start);
        btnHistory = findViewById(R.id.btn_history);
        waveformView = findViewById(R.id.waveform_view);
        layoutListening = findViewById(R.id.layout_listening);

        btnStart.setOnClickListener(v -> toggleListening());
        btnHistory.setOnClickListener(v -> openHistory());

        layoutListening.setVisibility(View.GONE);
        updateLangLabel();
    }

    private void initTranslators() {
        TranslatorOptions optKoVi = new TranslatorOptions.Builder()
                .setSourceLanguage(TranslateLanguage.KOREAN)
                .setTargetLanguage(TranslateLanguage.VIETNAMESE)
                .build();
        koToViTranslator = Translation.getClient(optKoVi);

        TranslatorOptions optViKo = new TranslatorOptions.Builder()
                .setSourceLanguage(TranslateLanguage.VIETNAMESE)
                .setTargetLanguage(TranslateLanguage.KOREAN)
                .build();
        viToKoTranslator = Translation.getClient(optViKo);
    }

    private void initSpeechRecognizer() {
        if (speechRecognizer != null) {
            speechRecognizer.destroy();
        }
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this);
        speechRecognizer.setRecognitionListener(recognitionListener);
    }

    private void initTTS() {
        tts = new TextToSpeech(this, status -> {
            if (status == TextToSpeech.SUCCESS) {
                isTtsReady = true;
            } else {
                Log.e("TTS", "초기화 실패");
            }
        });
    }

    private void toggleListening() {
        if (isListening) {
            stopListening();
        } else {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
                    != PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "마이크 권한이 필요합니다", Toast.LENGTH_SHORT).show();
                return;
            }
            startListening();
        }
    }

    private void startListening() {
        if (speechRecognizer == null) initSpeechRecognizer();

        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        
        // 핵심 수정: 현재 언어를 기본으로 하되, 한국어와 베트남어 모두에 대해 인식을 시도하도록 설정
        String primaryLang = isKorean ? "ko-KR" : "vi-VN";
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, primaryLang);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, primaryLang);
        intent.putExtra(RecognizerIntent.EXTRA_ONLY_RETURN_LANGUAGE_PREFERENCE, false);
        
        // 다중 언어 지원 힌트 추가
        String[] languages = {"ko-KR", "vi-VN"};
        intent.putExtra(RecognizerIntent.EXTRA_SUPPORTED_LANGUAGES, languages);

        intent.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true);

        // 연속 발화 지원 설정
        intent.putExtra("android.speech.extra.SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS", 3000L);
        intent.putExtra("android.speech.extra.SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS", 2000L);
        intent.putExtra("android.speech.extra.SPEECH_INPUT_MINIMUM_LENGTH_MILLIS", 2000L);

        runOnUiThread(() -> {
            try {
                speechRecognizer.startListening(intent);
                isListening = true;
                updateListeningUI(true);
            } catch (Exception e) {
                Log.e("Speech", "시작 오류: " + e.getMessage());
            }
        });
    }

    private void stopListening() {
        if (speechRecognizer != null) {
            speechRecognizer.stopListening();
        }
        isListening = false;
        updateListeningUI(false);
    }

    private final RecognitionListener recognitionListener = new RecognitionListener() {
        @Override
        public void onReadyForSpeech(Bundle params) {
            tvStatus.setText("말씀하세요...");
        }

        @Override
        public void onBeginningOfSpeech() {
            waveformView.startAnimation();
        }

        @Override
        public void onRmsChanged(float rmsdB) {
            waveformView.setVolume(rmsdB);
        }

        @Override
        public void onBufferReceived(byte[] buffer) {}

        @Override
        public void onEndOfSpeech() {
            tvStatus.setText("처리 중...");
            // onEndOfSpeech 이후 결과가 안 나올 경우를 대비한 자동 재시작은 onError에서 처리
        }

        @Override
        public void onError(int error) {
            String message;
            switch (error) {
                case SpeechRecognizer.ERROR_AUDIO: message = "오디오 에러"; break;
                case SpeechRecognizer.ERROR_SPEECH_TIMEOUT: message = "시간 초과"; break;
                case SpeechRecognizer.ERROR_NO_MATCH: message = "인식 결과 없음"; break;
                default: message = "인식 오류: " + error; break;
            }
            Log.e("Speech", message);

            // 연속 발화 재시작 로직
            if (isListening && !isTranslating) {
                if (error == SpeechRecognizer.ERROR_NO_MATCH || error == SpeechRecognizer.ERROR_SPEECH_TIMEOUT) {
                    mainHandler.postDelayed(() -> {
                        if (isListening && !isTranslating) startListening();
                    }, 300);
                }
            }
        }

        @Override
        public void onResults(Bundle results) {
            ArrayList<String> matches = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
            if (matches != null && !matches.isEmpty()) {
                String text = matches.get(0);
                detectAndSetLanguage(text);
                
                if (isKorean) tvKoText.setText(text);
                else tvViText.setText(text);
                
                translate(text);
            }
        }

        @Override
        public void onPartialResults(Bundle partialResults) {
            ArrayList<String> matches = partialResults.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
            if (matches != null && !matches.isEmpty()) {
                String partial = matches.get(0);
                if (isKorean) tvKoText.setText(partial);
                else tvViText.setText(partial);
            }
        }

        @Override
        public void onEvent(int eventType, Bundle params) {}
    };

    private void detectAndSetLanguage(String text) {
        boolean hasKorean = text.matches(".*[\\uAC00-\\uD7AF\\u1100-\\u11FF]+.*");
        boolean hasVietnamese = text.matches(".*[àáâãèéêìíòóôõùúăđĩũơưạảấầẩẫậắằẳẵặẹẻẽếềểễệỉịọỏốồổỗộớờởỡợụủứừửữựỳỵỷỹ]+.*");

        if (hasKorean && !hasVietnamese) {
            isKorean = true;
        } else if (hasVietnamese && !hasKorean) {
            isKorean = false;
        }
        // 둘 다 없거나 둘 다 있는 경우 현재 언어 유지
        runOnUiThread(this::updateLangLabel);
    }

    private void translate(String text) {
        if (text == null || text.trim().isEmpty()) return;

        isTranslating = true;
        tvStatus.setText("번역 중...");
        Translator translator = isKorean ? koToViTranslator : viToKoTranslator;

        translator.translate(text)
                .addOnSuccessListener(translatedText -> {
                    runOnUiThread(() -> {
                        if (isKorean) {
                            tvViText.setText(translatedText);
                        } else {
                            tvKoText.setText(translatedText);
                        }
                        tvStatus.setText("완료");

                        // 히스토리 추가
                        String entry = (isKorean ? "[KO] " : "[VI] ") + text
                                + "\n→ " + (isKorean ? "[VI] " : "[KO] ") + translatedText + "\n\n";
                        currentSession.append(entry);

                        // TTS 출력
                        speak(translatedText, isKorean ? new Locale("vi") : Locale.KOREAN);

                        isTranslating = false;
                        // 번역 완료 후 다음 인식을 위해 자동 재시작
                        if (isListening) {
                            mainHandler.postDelayed(this::startListening, 1500);
                        }
                    });
                })
                .addOnFailureListener(e -> {
                    runOnUiThread(() -> {
                        tvStatus.setText("번역 실패");
                        isTranslating = false;
                        if (isListening) startListening();
                    });
                });
    }

    private void speak(String text, Locale locale) {
        if (isTtsReady && tts != null) {
            int result = tts.setLanguage(locale);
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                Toast.makeText(this, "지원되지 않는 언어입니다", Toast.LENGTH_SHORT).show();
            } else {
                tts.stop();
                tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "TranslationTTS");
            }
        }
    }

    private void updateListeningUI(boolean listening) {
        if (listening) {
            btnStart.setText("■ 중지");
            btnStart.setBackgroundColor(Color.parseColor("#DA3633"));
            layoutListening.setVisibility(View.VISIBLE);
            waveformView.startAnimation();
        } else {
            btnStart.setText("🎙 통역 시작");
            btnStart.setBackgroundColor(Color.parseColor("#3FB950"));
            layoutListening.setVisibility(View.GONE);
            tvStatus.setText("대기 중");
            waveformView.stopAnimation();
        }
    }

    private void updateLangLabel() {
        tvSpeakLabel.setText(isKorean ? "한국어로 말씀하세요" : "Hãy nói tiếng Việt");
    }

    private void openHistory() {
        autoSaveSession();
        startActivity(new Intent(this, HistoryActivity.class));
    }

    private void autoSaveSession() {
        if (currentSession.length() > 0) {
            boolean success = historyManager.saveSession(currentSession.toString());
            if (!success) {
                Toast.makeText(this, "기록 저장에 실패했습니다", Toast.LENGTH_SHORT).show();
            }
            currentSession = new StringBuilder();
        }
    }

    @Override
    protected void onDestroy() {
        autoSaveSession();
        mainHandler.removeCallbacksAndMessages(null);
        
        if (speechRecognizer != null) {
            speechRecognizer.destroy();
        }
        if (tts != null) {
            tts.stop();
            tts.shutdown();
        }
        if (koToViTranslator != null) koToViTranslator.close();
        if (viToKoTranslator != null) viToKoTranslator.close();
        
        super.onDestroy();
    }
}
