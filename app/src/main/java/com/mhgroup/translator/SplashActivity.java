package com.mhgroup.translator;

import android.Manifest;
import android.app.Dialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.mlkit.common.model.DownloadConditions;
import com.google.mlkit.nl.translate.TranslateLanguage;
import com.google.mlkit.nl.translate.Translation;
import com.google.mlkit.nl.translate.Translator;
import com.google.mlkit.nl.translate.TranslatorOptions;

public class SplashActivity extends AppCompatActivity {

    private static final int MIC_PERMISSION_CODE = 100;
    private SharedPreferences prefs;
    private Dialog loadingDialog;
    private TextView tvProgress;
    private ProgressBar progressBar;
    private TextView tvStatus;
    private int downloadStep = 0; // 0=KO팩, 1=VI팩, 2=완료

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        prefs = getSharedPreferences("mh_prefs", MODE_PRIVATE);

        // 마이크 권한 먼저 요청
        checkMicPermission();
    }

    private void checkMicPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {
            showMicPermissionDialog();
        } else {
            checkLanguagePacks();
        }
    }

    private void showMicPermissionDialog() {
        Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_mic_permission);
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        dialog.setCancelable(false);

        Button btnAllow = dialog.findViewById(R.id.btn_allow_mic);
        btnAllow.setOnClickListener(v -> {
            dialog.dismiss();
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.RECORD_AUDIO}, MIC_PERMISSION_CODE);
        });
        dialog.show();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == MIC_PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                checkLanguagePacks();
            } else {
                Toast.makeText(this, "마이크 권한이 필요합니다. 설정에서 허용해주세요.", Toast.LENGTH_LONG).show();
                // 권한 없어도 다운로드는 진행 (나중에 재요청)
                checkLanguagePacks();
            }
        }
    }

    private void checkLanguagePacks() {
        boolean packsDownloaded = prefs.getBoolean("packs_downloaded", false);
        if (packsDownloaded) {
            goToMain();
            return;
        }
        showDownloadDialog();
    }

    private void showDownloadDialog() {
        Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_download);
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        dialog.setCancelable(false);

        Button btnDownload = dialog.findViewById(R.id.btn_download);
        btnDownload.setOnClickListener(v -> {
            dialog.dismiss();
            startDownload();
        });
        dialog.show();
    }

    private void startDownload() {
        setContentView(R.layout.activity_downloading);
        progressBar = findViewById(R.id.progress_download);
        tvProgress = findViewById(R.id.tv_progress);
        tvStatus = findViewById(R.id.tv_status);

        downloadKoreanPack();
    }

    private void downloadKoreanPack() {
        tvStatus.setText("한국어 언어팩 다운로드 중...");
        tvProgress.setText("1 / 2");
        progressBar.setProgress(0);
        progressBar.setIndeterminate(true);

        TranslatorOptions options = new TranslatorOptions.Builder()
                .setSourceLanguage(TranslateLanguage.KOREAN)
                .setTargetLanguage(TranslateLanguage.VIETNAMESE)
                .build();

        Translator translator = Translation.getClient(options);

        DownloadConditions conditions = new DownloadConditions.Builder().build();

        translator.downloadModelIfNeeded(conditions)
                .addOnSuccessListener(v -> {
                    progressBar.setIndeterminate(false);
                    progressBar.setProgress(50);
                    downloadVietnamesePack();
                })
                .addOnFailureListener(e -> {
                    progressBar.setIndeterminate(false);
                    tvStatus.setText("다운로드 실패. 재시도 중...");
                    new Handler(Looper.getMainLooper()).postDelayed(this::downloadKoreanPack, 2000);
                });
    }

    private void downloadVietnamesePack() {
        tvStatus.setText("베트남어 언어팩 다운로드 중...");
        tvProgress.setText("2 / 2");
        progressBar.setProgress(50);

        TranslatorOptions options = new TranslatorOptions.Builder()
                .setSourceLanguage(TranslateLanguage.VIETNAMESE)
                .setTargetLanguage(TranslateLanguage.KOREAN)
                .build();

        Translator translator = Translation.getClient(options);
        DownloadConditions conditions = new DownloadConditions.Builder().build();

        translator.downloadModelIfNeeded(conditions)
                .addOnSuccessListener(v -> {
                    progressBar.setProgress(100);
                    tvStatus.setText("준비 완료!");
                    tvProgress.setText("완료");
                    prefs.edit().putBoolean("packs_downloaded", true).apply();
                    new Handler(Looper.getMainLooper()).postDelayed(this::goToMain, 800);
                })
                .addOnFailureListener(e -> {
                    tvStatus.setText("다운로드 실패. 재시도 중...");
                    new Handler(Looper.getMainLooper()).postDelayed(this::downloadVietnamesePack, 2000);
                });
    }

    private void goToMain() {
        startActivity(new Intent(this, MainActivity.class));
        finish();
    }
}
