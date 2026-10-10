package com.jengacode.barcodescanner.scanner;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.*;
import android.util.AttributeSet;
import android.view.View;

public class ScanOverlay extends View {
  private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
  private float position;
  private ValueAnimator animator;

  public ScanOverlay(Context c, AttributeSet a) {
    super(c, a);
  }

  @Override
  protected void onAttachedToWindow() {
    super.onAttachedToWindow();
    animator = ValueAnimator.ofFloat(0, 1);
    animator.setDuration(2200);
    animator.setRepeatCount(ValueAnimator.INFINITE);
    animator.setRepeatMode(ValueAnimator.REVERSE);
    animator.addUpdateListener(
        a -> {
          position = (float) a.getAnimatedValue();
          invalidate();
        });
    animator.start();
  }

  @Override
  protected void onDetachedFromWindow() {
    if (animator != null) animator.cancel();
    super.onDetachedFromWindow();
  }

  @Override
  protected void onDraw(Canvas c) {
    float l = getWidth() * .12f,
        r = getWidth() * .88f,
        t = getHeight() * .22f,
        b = getHeight() * .78f;
    paint.setStyle(Paint.Style.STROKE);
    paint.setStrokeWidth(5);
    paint.setColor(Color.rgb(157, 187, 255));
    c.drawRoundRect(l, t, r, b, 16, 16, paint);
    paint.setStrokeWidth(2);
    paint.setColor(Color.rgb(114, 161, 255));
    c.drawLine(
        l + 8, t + 8 + (b - t - 16) * position, r - 8, t + 8 + (b - t - 16) * position, paint);
  }
}
