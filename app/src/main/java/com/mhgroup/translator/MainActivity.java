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
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
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

public class MainActivity extends AppCompatActivity {

    // UI
    private TextView tvKoText, tvViText, tvStatus, tvSpeakLabel;
    private Button btnStart, btnHistory;
    private WaveformView waveformView;
    private LinearLayout layoutListening;

    // 음성인식 (Google SpeechRecognizer)
    private SpeechRecognizer speechRecognizer;
    private boolean isListening = false;
    private boolean isKorean = true; 

    // 번역
    private Translator koToViTranslator, viToKoTranslator;

    // 히스토리
    private HistoryManager historyManager;
    private StringBuilder currentSession = new StringBuilder();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initViews();
        initTranslators();
        initSpeechRecognizer();
        historyManager = new HistoryManager(this);
    }

    private void initViews() {
        tvKoText    = findViewById(R.id.tv_ko_text);
        tvViText    = findViewById(R.id.tv_vi_text);
        tvStatus    = findViewById(R.id.tv_status);
        tvSpeakLabel = findViewById(R.id.tv_speak_label);
        btnStart    = findViewById(R.id.btn_start);
        btnHistory  = findViewById(R.id.btn_history);
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
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        // 현재 언어 설정 (한국어: ko-KR, 베트남어: vi-VN)
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, isKorean ? "ko-KR" : "vi-VN");
        intent.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true);

        runOnUiThread(() -> {
            speechRecognizer.startListening(intent);
            isListening = true;
            updateListeningUI(true);
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
            // 소리 크기에 따라 파형 반응
            if (rmsdB > 2) waveformView.triggerPulse();
        }

        @Override
        public void onBufferReceived(byte[] buffer) {}

        @Override
        public void onEndOfSpeech() {
            tvStatus.setText("처리 중...");
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
            if (isListening) {
                // 짧은 대기 후 다시 리스닝 (연속 통역 모드 유지)
                new Handler(Looper.getMainLooper()).postDelayed(() -> {
                    if (isListening) startListening();
                }, 1000);
            }
        }

        @Override
        public void onResults(Bundle results) {
            ArrayList<String> matches = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
            if (matches != null && !matches.isEmpty()) {
                String text = matches.get(0);
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

    private void translate(String text) {
        if (text == null || text.trim().isEmpty()) return;

        tvStatus.setText("번역 중...");
        Translator translator = isKorean ? koToViTranslator : viToKoTranslator;

        translator.translate(text)
                .addOnSuccessListener(translatedText -> {
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

                    // 언어 자동 전환
                    isKorean = !isKorean;
                    updateLangLabel();

                    // 다음 인식을 위해 자동으로 리스닝 시작 (통역 모드 유지)
                    if (isListening) {
                        new Handler(Looper.getMainLooper()).postDelayed(this::startListening, 1500);
                    }
                })
                .addOnFailureListener(e -> {
                    tvStatus.setText("번역 실패");
                    if (isListening) startListening();
                });
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
        if (currentSession.length() > 0) {
            historyManager.saveSession(currentSession.toString());
            currentSession = new StringBuilder();
        }
        startActivity(new Intent(this, HistoryActivity.class));
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (speechRecognizer != null) {
            speechRecognizer.destroy();
        }
        if (koToViTranslator != null) koToViTranslator.close();
        if (viToKoTranslator != null) viToKoTranslator.close();
        if (currentSession.length() > 0) {
            historyManager.saveSession(currentSession.toString());
        }
    }
}
