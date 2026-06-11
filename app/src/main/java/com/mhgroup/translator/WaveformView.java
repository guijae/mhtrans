package com.mhgroup.translator;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.os.Handler;
import android.os.Looper;
import android.util.AttributeSet;
import android.view.View;

import java.util.Random;

public class WaveformView extends View {

    private Paint paint;
    private float[] barHeights;
    private static final int BAR_COUNT = 20;
    private static final int BAR_COLOR = Color.parseColor("#3FB950");
    private static final int BAR_COLOR_PULSE = Color.parseColor("#58D68D");
    private boolean isAnimating = false;
    private boolean isPulsing = false;
    private Handler handler = new Handler(Looper.getMainLooper());
    private Random random = new Random();
    private Runnable animRunnable;
    private Runnable pulseRunnable;

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
        barHeights = new float[BAR_COUNT];
        for (int i = 0; i < BAR_COUNT; i++) barHeights[i] = 8f;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        int w = getWidth();
        int h = getHeight();
        if (w == 0 || h == 0) return;

        float barW = (float) w / (BAR_COUNT * 2 - 1);
        float gap = barW;

        for (int i = 0; i < BAR_COUNT; i++) {
            float x = i * (barW + gap);
            float barH = Math.max(8f, barHeights[i]);
            float top = (h - barH) / 2f;
            float bottom = top + barH;

            paint.setColor(isPulsing ? BAR_COLOR_PULSE : BAR_COLOR);
            paint.setAlpha(isPulsing ? 255 : 200);

            RectF rect = new RectF(x, top, x + barW, bottom);
            canvas.drawRoundRect(rect, barW / 2f, barW / 2f, paint);
        }
    }

    public void startAnimation() {
        isAnimating = true;
        animRunnable = new Runnable() {
            @Override
            public void run() {
                if (!isAnimating) return;
                // 잔잔한 idle 애니메이션
                for (int i = 0; i < BAR_COUNT; i++) {
                    barHeights[i] = 8f + random.nextFloat() * 16f;
                }
                invalidate();
                handler.postDelayed(this, 150);
            }
        };
        handler.post(animRunnable);
    }

    public void stopAnimation() {
        isAnimating = false;
        isPulsing = false;
        if (animRunnable != null) handler.removeCallbacks(animRunnable);
        if (pulseRunnable != null) handler.removeCallbacks(pulseRunnable);
        for (int i = 0; i < BAR_COUNT; i++) barHeights[i] = 8f;
        invalidate();
    }

    // 음성 감지 시 강한 파형
    public void triggerPulse() {
        isPulsing = true;
        handler.removeCallbacks(pulseRunnable != null ? pulseRunnable : () -> {});

        int[] steps = {0};
        pulseRunnable = new Runnable() {
            @Override
            public void run() {
                if (steps[0] >= 8) {
                    isPulsing = false;
                    invalidate();
                    return;
                }
                for (int i = 0; i < BAR_COUNT; i++) {
                    barHeights[i] = 10f + random.nextFloat() * 60f;
                }
                steps[0]++;
                invalidate();
                handler.postDelayed(this, 80);
            }
        };
        handler.post(pulseRunnable);
    }
}
