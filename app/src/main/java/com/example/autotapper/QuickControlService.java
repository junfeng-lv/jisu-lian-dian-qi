package com.example.autotapper;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.graphics.RadialGradient;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.os.Build;
import android.os.IBinder;
import android.os.SystemClock;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;

public class QuickControlService extends Service {

    private static final String CHANNEL_ID = "quick_control";
    private static final int NOTIFICATION_ID = 3;
    private static final int BALL_SIZE = 52;
    private static final int PANEL_W = 220;
    private static final int PANEL_H = 400;
    private static final int COLLAPSED_W = 148;
    private static final int COLLAPSED_H = 76;

    private static QuickControlService instance;

    private WindowManager wm;
    private WindowManager.LayoutParams params;
    private FrameLayout root;
    private GlassBallView ball;
    private GlassSliderView topSlider;
    private GlassSliderView bottomSlider;
    private Button exitBtn;
    private boolean expanded = false;
    private boolean running = false;
    private int screenW = 1080;
    private int screenH = 1920;

    public static boolean isActive() {
        return instance != null;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (instance == null || root == null) {
            instance = this;
            wm = (WindowManager) getSystemService(WINDOW_SERVICE);
            android.graphics.Point real = new android.graphics.Point();
            wm.getDefaultDisplay().getRealSize(real);
            screenW = real.x;
            screenH = real.y;
            running = getSharedPreferences("tap_prefs", MODE_PRIVATE).getBoolean("running", false);
            buildOverlay();
            startForeground(NOTIFICATION_ID, buildNotification());
        }
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        if (root != null && wm != null) {
            try {
                wm.removeView(root);
            } catch (Exception ignored) {
            }
        }
        root = null;
        instance = null;
        try {
            NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
            if (nm != null) nm.cancel(NOTIFICATION_ID);
        } catch (Exception ignored) {
        }
        super.onDestroy();
    }

    private void buildOverlay() {
        int type = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                : WindowManager.LayoutParams.TYPE_PHONE;
        params = new WindowManager.LayoutParams(
                dp(COLLAPSED_W), dp(COLLAPSED_H),
                type,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                        | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                        | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
                PixelFormat.TRANSLUCENT);
        params.gravity = Gravity.TOP | Gravity.START;
        params.x = Math.max(dp(8), screenW - dp(COLLAPSED_W + 8));
        params.y = Math.max(dp(100), screenH / 2 - dp(COLLAPSED_H / 2));
        clampWindow();

        root = new FrameLayout(this);
        root.setClipChildren(false);

        ball = new GlassBallView(this);
        ball.setListener(new GlassBallView.Listener() {
            @Override
            public void onTap() {
                toggleExpanded();
            }

            @Override
            public void onDrag(float dx, float dy) {
                moveWindow(dx, dy);
            }
        });
        ball.setRunning(running);

        topSlider = new GlassSliderView(this, true);
        topSlider.setListener(() -> startTapping());
        bottomSlider = new GlassSliderView(this, false);
        bottomSlider.setListener(() -> stopTapping());

        exitBtn = new Button(this);
        exitBtn.setText("退出连点器");
        exitBtn.setTextSize(11);
        exitBtn.setTextColor(Color.WHITE);
        exitBtn.setTypeface(null, Typeface.BOLD);
        exitBtn.setBackgroundResource(R.drawable.bg_danger);
        exitBtn.setAllCaps(false);
        exitBtn.setSingleLine(true);
        exitBtn.setMaxLines(1);
        exitBtn.setGravity(Gravity.CENTER);
        exitBtn.setPadding(0, 0, 0, 0);
        exitBtn.setOnClickListener(v -> exitCurrentApp());

        layoutExpanded(false);
        wm.addView(root, params);
    }

    private void layoutExpanded(boolean expandedState) {
        root.removeView(ball);
        root.removeView(topSlider);
        root.removeView(bottomSlider);
        root.removeView(exitBtn);
        if (expandedState) {
            FrameLayout.LayoutParams topLp = new FrameLayout.LayoutParams(dp(64), dp(148));
            topLp.gravity = Gravity.TOP | Gravity.CENTER_HORIZONTAL;
            topLp.topMargin = dp(16);
            root.addView(topSlider, topLp);

            FrameLayout.LayoutParams bottomLp = new FrameLayout.LayoutParams(dp(64), dp(148));
            bottomLp.gravity = Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL;
            bottomLp.bottomMargin = dp(16);
            root.addView(bottomSlider, bottomLp);

            FrameLayout.LayoutParams exitLp = new FrameLayout.LayoutParams(dp(66), dp(48));
            exitLp.gravity = Gravity.LEFT | Gravity.CENTER_VERTICAL;
            exitLp.leftMargin = dp(6);
            root.addView(exitBtn, exitLp);

            FrameLayout.LayoutParams ballLp = new FrameLayout.LayoutParams(dp(BALL_SIZE), dp(BALL_SIZE));
            ballLp.gravity = Gravity.CENTER;
            root.addView(ball, ballLp);
        } else {
            FrameLayout.LayoutParams exitLp = new FrameLayout.LayoutParams(dp(58), dp(44));
            exitLp.gravity = Gravity.LEFT | Gravity.CENTER_VERTICAL;
            exitLp.leftMargin = dp(5);
            root.addView(exitBtn, exitLp);

            FrameLayout.LayoutParams ballLp = new FrameLayout.LayoutParams(dp(BALL_SIZE), dp(BALL_SIZE));
            ballLp.gravity = Gravity.LEFT | Gravity.CENTER_VERTICAL;
            ballLp.leftMargin = dp(74);
            root.addView(ball, ballLp);
        }
    }
    private void toggleExpanded() {
        expanded = !expanded;
        int oldW = params.width;
        int oldH = params.height;
        int cx = params.x + oldW / 2;
        int cy = params.y + oldH / 2;
        layoutExpanded(expanded);
        params.width = expanded ? dp(PANEL_W) : dp(COLLAPSED_W);
        params.height = expanded ? dp(PANEL_H) : dp(COLLAPSED_H);
        params.x = Math.max(0, Math.min(screenW - params.width, cx - params.width / 2));
        params.y = Math.max(0, Math.min(screenH - params.height, cy - params.height / 2));
        clampWindow();
        try {
            wm.updateViewLayout(root, params);
        } catch (Exception ignored) {
        }
        root.requestLayout();
        root.invalidate();
    }

    private void moveWindow(float dx, float dy) {
        if (params == null || wm == null || root == null) return;
        params.x += Math.round(dx);
        params.y += Math.round(dy);
        clampWindow();
        try {
            wm.updateViewLayout(root, params);
        } catch (Exception ignored) {
        }
    }

    private void clampWindow() {
        params.x = Math.max(0, Math.min(screenW - params.width, params.x));
        params.y = Math.max(0, Math.min(screenH - params.height, params.y));
    }

    private void startTapping() {
        SharedPreferences p = getSharedPreferences("tap_prefs", MODE_PRIVATE);
        String posStr = p.getString("positions", "[]");
        try {
            if (new JSONArray(posStr).length() == 0) {
                toast("请先在主页设置圆点");
                return;
            }
        } catch (Exception ignored) {
            toast("请先在主页设置圆点");
            return;
        }
        if (!TapAccessibilityService.isConnected()) {
            toast("请先开启无障碍服务");
            return;
        }
        p.edit()
                .putBoolean("running", true)
                .putInt("current", 0)
                .putLong("startTime", System.currentTimeMillis())
                .putLong("elapsedMs", 0)
                .putString("status", "运行中")
                .putString("lastLog", "已通过悬浮控制开始")
                .apply();
        TapAccessibilityService.startOrRefresh();
        running = true;
        if (ball != null) ball.setRunning(true);
        toast("已开始连点");
    }

    private void stopTapping() {
        TapAccessibilityService.stop();
        running = false;
        if (ball != null) ball.setRunning(false);
        toast("已停止连点");
    }

    private void exitCurrentApp() {
        TapAccessibilityService.stop();
        stopService(new Intent(this, TapOverlayService.class));
        stopSelf();
        android.os.Process.killProcess(android.os.Process.myPid());
    }

    private void toast(String msg) {
        try {
            Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
        } catch (Exception ignored) {
        }
    }

    private Notification buildNotification() {
        try {
            if (Build.VERSION.SDK_INT >= 26) {
                NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
                NotificationChannel channel = new NotificationChannel(
                        CHANNEL_ID, "悬浮控制球", NotificationManager.IMPORTANCE_LOW);
                channel.setShowBadge(false);
                nm.createNotificationChannel(channel);
            }
            Intent open = new Intent(this, MainActivity.class);
            open.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            PendingIntent pi = PendingIntent.getActivity(this, 3, open,
                    PendingIntent.FLAG_UPDATE_CURRENT | (Build.VERSION.SDK_INT >= 23 ? PendingIntent.FLAG_IMMUTABLE : 0));
            Notification.Builder b;
            if (Build.VERSION.SDK_INT >= 26) {
                b = new Notification.Builder(this, CHANNEL_ID);
            } else {
                b = new Notification.Builder(this);
            }
            return b.setSmallIcon(R.drawable.ic_notification)
                    .setContentTitle("极速连点器")
                    .setContentText("悬浮控制运行中 · 点击圆球展开")
                    .setContentIntent(pi)
                    .setPriority(Notification.PRIORITY_LOW)
                    .setOngoing(true)
                    .build();
        } catch (Exception ignored) {
            return new Notification.Builder(this).build();
        }
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private class GlassBallView extends View {
        interface Listener {
            void onTap();
            void onDrag(float dx, float dy);
        }

        private Listener listener;
        private boolean dragging = false;
        private boolean isRunning = false;
        private float downX, downY, lastRawX, lastRawY, moved;

        GlassBallView(android.content.Context context) {
            super(context);
            setClickable(true);
        }

        void setListener(Listener l) {
            listener = l;
        }

        void setRunning(boolean value) {
            isRunning = value;
            postInvalidate();
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            float cx = getWidth() / 2f;
            float cy = getHeight() / 2f;
            float r = Math.min(cx, cy) - dp(3);
            long t = SystemClock.uptimeMillis();

            Paint glow = new Paint(Paint.ANTI_ALIAS_FLAG);
            glow.setStyle(Paint.Style.FILL);
            glow.setColor(0x2EFFFFFF);
            canvas.drawCircle(cx, cy, r + dp(5), glow);

            Paint ring = new Paint(Paint.ANTI_ALIAS_FLAG);
            ring.setStyle(Paint.Style.STROKE);
            ring.setStrokeWidth(dp(2));
            ring.setColor(0x66FFFFFF);
            canvas.drawCircle(cx, cy, r + dp(3), ring);

            Paint pulse = new Paint(Paint.ANTI_ALIAS_FLAG);
            pulse.setStyle(Paint.Style.STROKE);
            pulse.setStrokeWidth(dp(2));
            float pr = r + dp(4) + (t % 2600L) / 2600f * dp(12);
            pulse.setColor(0x3380F0FF);
            canvas.drawCircle(cx, cy, pr, pulse);

            Paint fill = new Paint(Paint.ANTI_ALIAS_FLAG);
            fill.setShader(new RadialGradient(cx - dp(7), cy - dp(9), r * 1.55f,
                    new int[]{0xFFE8FDFF, 0xFF37C8E0, 0xFF1C6FA8, 0xFF12437A},
                    new float[]{0f, 0.42f, 0.78f, 1f}, Shader.TileMode.CLAMP));
            canvas.drawCircle(cx, cy, r, fill);

            Paint edge = new Paint(Paint.ANTI_ALIAS_FLAG);
            edge.setStyle(Paint.Style.STROKE);
            edge.setStrokeWidth(dp(2));
            edge.setColor(0xCCFFFFFF);
            canvas.drawCircle(cx, cy, r, edge);

            Paint gloss = new Paint(Paint.ANTI_ALIAS_FLAG);
            gloss.setColor(0x99FFFFFF);
            canvas.drawOval(new RectF(cx - r * 0.58f, cy - r * 0.72f, cx + r * 0.08f, cy - r * 0.22f), gloss);

            Paint play = new Paint(Paint.ANTI_ALIAS_FLAG);
            play.setColor(Color.WHITE);
            play.setStyle(Paint.Style.FILL);
            Path bolt = new Path();
            bolt.moveTo(cx + dp(2), cy - dp(10));
            bolt.lineTo(cx - dp(9), cy + dp(4));
            bolt.lineTo(cx - dp(2), cy + dp(4));
            bolt.lineTo(cx - dp(6), cy + dp(11));
            bolt.lineTo(cx + dp(9), cy - dp(4));
            bolt.lineTo(cx + dp(2), cy - dp(4));
            bolt.close();
            canvas.drawPath(bolt, play);

            Paint dot = new Paint(Paint.ANTI_ALIAS_FLAG);
            dot.setColor(isRunning ? 0xFF37E6B8 : 0xFF9AA7BD);
            canvas.drawCircle(cx + r * 0.72f, cy + r * 0.72f, dp(4), dot);
            dot.setStyle(Paint.Style.STROKE);
            dot.setStrokeWidth(dp(2));
            dot.setColor(0xFFFFFFFF);
            canvas.drawCircle(cx + r * 0.72f, cy + r * 0.72f, dp(4), dot);
            postInvalidateDelayed(50);
        }

        @Override
        public boolean onTouchEvent(MotionEvent event) {
            switch (event.getActionMasked()) {
                case MotionEvent.ACTION_DOWN:
                    downX = event.getX();
                    downY = event.getY();
                    lastRawX = event.getRawX();
                    lastRawY = event.getRawY();
                    moved = 0;
                    dragging = false;
                    return true;
                case MotionEvent.ACTION_MOVE:
                    float dx = event.getRawX() - lastRawX;
                    float dy = event.getRawY() - lastRawY;
                    lastRawX = event.getRawX();
                    lastRawY = event.getRawY();
                    moved += Math.abs(dx) + Math.abs(dy);
                    if (moved > dp(4)) {
                        dragging = true;
                        if (listener != null) listener.onDrag(dx, dy);
                    }
                    return true;
                case MotionEvent.ACTION_UP:
                    if (!dragging) {
                        performHapticFeedback(android.view.HapticFeedbackConstants.KEYBOARD_TAP);
                        if (listener != null) listener.onTap();
                    }
                    dragging = false;
                    return true;
                case MotionEvent.ACTION_CANCEL:
                    dragging = false;
                    return true;
            }
            return false;
        }
    }

    private class GlassSliderView extends View {
        interface Listener {
            void onTrigger();
        }

        private final boolean upMode;
        private Listener listener;
        private float downY, dragOffset;
        private boolean touched = false;

        GlassSliderView(android.content.Context context, boolean up) {
            super(context);
            upMode = up;
            setClickable(true);
        }

        void setListener(Listener l) {
            listener = l;
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            float w = getWidth();
            float h = getHeight();
            RectF r = new RectF(dp(3), dp(3), w - dp(3), h - dp(3));
            long t = SystemClock.uptimeMillis();
            float phase = t / 50f;

            Paint fill = new Paint(Paint.ANTI_ALIAS_FLAG);
            fill.setShader(new LinearGradient(r.left, r.top, r.right, r.bottom,
                    new int[]{0xEEFFFFFF, 0x66BFEFFF, 0x88A5E8FF, 0xEEFFFFFF},
                    new float[]{0f, 0.25f, 0.72f, 1f}, Shader.TileMode.CLAMP));
            canvas.drawRoundRect(r, dp(30), dp(30), fill);

            Path clip = new Path();
            RectF hair = new RectF(r.left + dp(1), r.top + dp(1), r.right - dp(1), r.bottom - dp(1));
            clip.addRoundRect(hair, dp(29), dp(29), Path.Direction.CW);

            canvas.save();
            canvas.clipPath(clip);
            Paint wave = new Paint(Paint.ANTI_ALIAS_FLAG);
            wave.setStyle(Paint.Style.STROKE);
            wave.setStrokeWidth(dp(2));
            for (int i = 0; i < 3; i++) {
                float xBase = r.centerX() + (i - 1) * dp(6) + dragOffset * 0.10f;
                Path path = new Path();
                boolean first = true;
                for (float y = r.top; y <= r.bottom; y += dp(3)) {
                    float x = xBase + (float) Math.sin((y - r.top) / dp(16) + phase + i * 1.7f) * dp(3);
                    if (first) {
                        path.moveTo(x, y);
                        first = false;
                    } else {
                        path.lineTo(x, y);
                    }
                }
                wave.setColor(i == 1 ? 0x80C9F6FF : 0x55FFFFFF);
                wave.setStrokeWidth(dp(i == 1 ? 3 : 2));
                canvas.drawPath(path, wave);
            }
            RectF gloss = new RectF(r.left + dp(2), r.top + dp(8), r.right - dp(2), r.top + dp(18));
            Paint glossPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            glossPaint.setColor(0x66FFFFFF);
            canvas.drawOval(gloss, glossPaint);
            canvas.restore();

            Paint stroke = new Paint(Paint.ANTI_ALIAS_FLAG);
            stroke.setStyle(Paint.Style.STROKE);
            stroke.setStrokeWidth(dp(2));
            stroke.setColor(0xCCFFFFFF);
            canvas.drawRoundRect(r, dp(30), dp(30), stroke);

            Paint text = new Paint(Paint.ANTI_ALIAS_FLAG);
            text.setColor(Color.WHITE);
            text.setTypeface(Typeface.DEFAULT_BOLD);
            text.setTextAlign(Paint.Align.CENTER);
            text.setTextSize(dp(12));
            float cy = r.centerY() + dp(1);
            String firstLine = upMode ? "上滑" : "下滑";
            String secondLine = upMode ? "开始" : "停止";
            canvas.drawText(firstLine, r.centerX(), baselineFor(text, cy - dp(12)), text);
            canvas.drawText(secondLine, r.centerX(), baselineFor(text, cy + dp(15)), text);

            if (touchProgress()) {
                Paint thumb = new Paint(Paint.ANTI_ALIAS_FLAG);
                thumb.setColor(dragOffset >= dp(2) ? 0xCCFFFFFF : 0x66FFFFFF);
                float ty = r.centerY() + (upMode ? -dragOffset : dragOffset);
                ty = Math.max(r.top + dp(18), Math.min(r.bottom - dp(18), ty));
                canvas.drawCircle(r.centerX(), ty, dp(9), thumb);
                Paint thumbRing = new Paint(Paint.ANTI_ALIAS_FLAG);
                thumbRing.setStyle(Paint.Style.STROKE);
                thumbRing.setStrokeWidth(dp(2));
                thumbRing.setColor(0xBFFFFFFF);
                canvas.drawCircle(r.centerX(), ty, dp(12), thumbRing);
            }
            postInvalidateDelayed(33);
        }

        private boolean touchProgress() {
            return touched && Math.abs(dragOffset) > dp(1);
        }

        private float baselineFor(Paint p, float midY) {
            Paint.FontMetrics fm = p.getFontMetrics();
            return midY - (fm.ascent + fm.descent) / 2f;
        }

        @Override
        public boolean onTouchEvent(MotionEvent event) {
            boolean upGesture;
            switch (event.getActionMasked()) {
                case MotionEvent.ACTION_DOWN:
                    downY = event.getY();
                    dragOffset = 0;
                    touched = true;
                    getParent().requestDisallowInterceptTouchEvent(true);
                    return true;
                case MotionEvent.ACTION_MOVE:
                    float distance = upMode
                            ? (downY - event.getY())
                            : (event.getY() - downY);
                    dragOffset = Math.max(0, Math.min(dp(32), distance));
                    invalidate();
                    return true;
                case MotionEvent.ACTION_UP:
                    float swipe = upMode
                            ? (downY - event.getY())
                            : (event.getY() - downY);
                    upGesture = swipe >= dp(22);
                    touched = false;
                    dragOffset = 0;
                    invalidate();
                    performHapticFeedback(android.view.HapticFeedbackConstants.KEYBOARD_TAP);
                    if (upGesture && listener != null) {
                        listener.onTrigger();
                    }
                    return true;
                case MotionEvent.ACTION_CANCEL:
                    touched = false;
                    dragOffset = 0;
                    invalidate();
                    return true;
            }
            return false;
        }
    }
}
