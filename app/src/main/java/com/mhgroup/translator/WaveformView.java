package com.mhgroup.translator;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.view.View;

public class WaveformView extends View {

    private Paint paint;
    private Path path;
    private float amplitude = 0f;
    private float targetAmplitude = 0f;
    private float phase = 0f;
    
    // 설정 값
    private static final int WAVE_COLOR = Color.parseColor("#3FB950");
    private static final float FREQUENCY = 1.5f;
    private static final float SPEED = 0.15f;
    private static final float SMOOTHING = 0.2f; // 값의 부드러운 변화 정도

    public WaveformView(Context context) {
        super(context);
        init();
    }

    public WaveformView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setColor(WAVE_COLOR);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(5f);
        path = new Path();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        
        int w = getWidth();
        int h = getHeight();
        if (w == 0 || h == 0) return;

        // 진폭 부드럽게 보정 (Interpolation)
        amplitude += (targetAmplitude - amplitude) * SMOOTHING;
        
        path.reset();
        float midY = h / 2f;
        
        // 부드러운 사인파 그리기
        for (float x = 0; x <= w; x += 5) {
            // 중앙은 크게, 양 끝은 0으로 수렴하게 하는 가우시안 윈도우 적용
            float scaling = (float) (Math.exp(-Math.pow(1.5 * (x - w / 2f) / (w / 2f), 2)));
            
            float y = midY + (float) (amplitude * scaling * Math.sin(x * 0.02f * FREQUENCY + phase));
            
            if (x == 0) path.moveTo(x, y);
            else path.lineTo(x, y);
        }

        canvas.drawPath(path, paint);

        // 페이즈 이동으로 애니메이션 효과
        phase -= SPEED;
        invalidate();
    }

    public void startAnimation() {
        targetAmplitude = 20f; // 기본 진동
        invalidate();
    }

    public void stopAnimation() {
        targetAmplitude = 0f;
        invalidate();
    }

    // 소리 크기에 따라 목표 진폭 설정 (부드러운 반응)
    public void triggerPulse() {
        targetAmplitude = 80f; // 소리 감지 시 크게
        // 짧은 시간 후 다시 기본 진동으로 복귀하도록 처리하고 싶다면 별도 로직 추가 가능
    }
    
    // MainActivity에서 rmsdB 값을 직접 전달받을 수 있도록 추가
    public void setVolume(float rmsdB) {
        // rmsdB는 보통 -2에서 10 사이의 값
        float level = Math.max(0, rmsdB + 2); 
        targetAmplitude = 20f + (level * 8f); // 기본 20 + 소리 크기에 따른 추가 진폭
    }
}
