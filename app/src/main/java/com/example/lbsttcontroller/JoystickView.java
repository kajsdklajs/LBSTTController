package com.example.lbsttcontroller;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

public class JoystickView extends View {
    private Paint bgPaint, stickPaint;
    private float cx, cy, radius, stickX, stickY;
    private OnJoystickMoveListener listener;

    public interface OnJoystickMoveListener {
        void onMove(float x, float y); // -1..1
    }

    public JoystickView(Context c) { super(c); init(); }
    public JoystickView(Context c, AttributeSet a) { super(c, a); init(); }

    private void init() {
        bgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        bgPaint.setColor(Color.parseColor("#1A1A3A"));
        bgPaint.setStyle(Paint.Style.STROKE);
        bgPaint.setStrokeWidth(4f);
        stickPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        stickPaint.setColor(Color.parseColor("#00FF88"));
    }

    public void setOnJoystickMoveListener(OnJoystickMoveListener l) { listener = l; }

    @Override
    protected void onSizeChanged(int w, int h, int ow, int oh) {
        cx = w / 2f; cy = h / 2f;
        radius = Math.min(w, h) / 2f - 20;
        stickX = cx; stickY = cy;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        canvas.drawCircle(cx, cy, radius, bgPaint);
        canvas.drawCircle(stickX, stickY, radius / 4f, stickPaint);
    }

    @Override
    public boolean onTouchEvent(MotionEvent e) {
        float dx = e.getX() - cx, dy = e.getY() - cy;
        float dist = (float) Math.hypot(dx, dy);
        if (dist > radius) { dx = dx / dist * radius; dy = dy / dist * radius; }
        stickX = cx + dx; stickY = cy + dy;
        float nx = dx / radius, ny = -dy / radius;
        if (listener != null) listener.onMove(nx, ny);
        invalidate();
        return true;
    }
}