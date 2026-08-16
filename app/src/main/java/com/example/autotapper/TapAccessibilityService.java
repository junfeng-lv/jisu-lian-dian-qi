package com.example.autotapper;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.accessibilityservice.GestureDescription;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Path;
import android.graphics.Point;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.view.WindowManager;
import android.view.accessibility.AccessibilityEvent;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.Random;
import java.util.Locale;
import java.util.Date;
import java.text.SimpleDateFormat;

public class TapAccessibilityService extends AccessibilityService {

    private static final String CHANNEL_ID = "auto_tapper_status";
    private static final int NOTIFICATION_ID = 2;
    private static final String ACTION_STOP = "ACTION_STOP";
    private static TapAccessibilityService instance = null;
    private static volatile boolean running = false;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Random random = new Random();
    private Runnable task;
    private JSONArray positions = new JSONArray();
    private long intervalMs = 150;
    private long jitterMs = 0;
    private int total = 0;
    private int modeIndex = 0;
    private int gestureIndex = 0;
    private int current = 0;
    private long startTimeWall = 0;
    private int screenW = 1080;
    private int screenH = 1920;

    public static boolean isConnected() {
        return instance != null;
    }

    public static boolean isRunning() {
        return running;
    }

    public static boolean performBack() {
        TapAccessibilityService s = instance;
        if (s == null) return false;
        return s.performGlobalAction(GLOBAL_ACTION_BACK);
    }

    public static boolean startOrRefresh() {
        TapAccessibilityService s = instance;
        if (s == null) return false;
        s.runOnMainThread(s::startInternal);
        return true;
    }

    public static void stop() {
        running = false;
        TapAccessibilityService s = instance;
        if (s != null) s.stopInternal();
    }

    private void runOnMainThread(Runnable r) {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            r.run();
        } else {
            handler.post(r);
        }
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
    }

    @Override
    public void onInterrupt() {
    }

    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();
        instance = this;
        try {
            Point real = new Point();
            ((WindowManager) getSystemService(WINDOW_SERVICE)).getDefaultDisplay().getRealSize(real);
            screenW = real.x;
            screenH = real.y;
        } catch (Exception ignored) {
        }
        AccessibilityServiceInfo info = getServiceInfo();
        if (info == null) {
            info = new AccessibilityServiceInfo();
        }
        info.eventTypes = AccessibilityEvent.TYPES_ALL_MASK;
        info.feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC;
        info.notificationTimeout = 100;
        info.flags |= AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS
                | AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS;

        setServiceInfo(info);
        showStatusNotification("无障碍服务已就绪");
        if (prefs().getBoolean("running", false)) {
            runOnMainThread(this::startInternal);
        }
    }
    @Override
    public void onDestroy() {
        running = false;
        handler.removeCallbacksAndMessages(null);
        cancelStatusNotification();
        instance = null;
        super.onDestroy();
    }

    private SharedPreferences prefs() {
        return getSharedPreferences("tap_prefs", MODE_PRIVATE);
    }

    private void startInternal() {
        SharedPreferences p = prefs();
        positions = readPositions(p);
        intervalMs = Math.max(30, p.getLong("interval", 150));
        jitterMs = Math.max(0, p.getLong("jitter", 0));
        total = Math.max(0, p.getInt("total", 0));
        modeIndex = p.getInt("modeIndex", 0);
        gestureIndex = p.getInt("gestureIndex", 0);
        current = Math.max(0, p.getInt("current", 0));
        startTimeWall = System.currentTimeMillis();
        handler.removeCallbacks(task);
        if (positions.length() == 0) {
            running = false;
            p.edit().putBoolean("running", false).putString("status", "未设置位置")
                    .putString("lastLog", "请先设置点击位置").apply();
            cancelStatusNotification();
            return;
        }
        running = true;
        p.edit().putBoolean("running", true).putInt("current", current)
                .putLong("startTime", startTimeWall).putLong("elapsedMs", 0)
                .putString("status", "运行中").putString("lastLog", "已开始连点").apply();
        showStatusNotification("自动连点运行中");
        task = this::nextAction;
        handler.postDelayed(task, 120);
    }

    private void stopInternal() {
        running = false;
        handler.removeCallbacks(task);
        long elapsed = Math.max(0, System.currentTimeMillis() - startTimeWall);
        prefs().edit().putBoolean("running", false).putString("status", "已停止")
                .putLong("elapsedMs", elapsed).putString("lastLog", "已停止连点").apply();
        showStatusNotification("连点已停止");
    }

    private void nextAction() {
        if (!running || instance == null || positions.length() == 0) {
            return;
        }
        try {
            int duration;
            GestureDescription gesture;
            String actionName;
            if (gestureIndex == 3) {
                GestureDescription.Builder gb = new GestureDescription.Builder();
                for (int i = 0; i < positions.length(); i++) {
                    JSONObject o = positions.getJSONObject(i);
                    int px = clampCoord(o.getInt("x"), screenW);
                    int py = clampCoord(o.getInt("y"), screenH);
                    Path multi = new Path();
                    multi.moveTo(px, py);
                    gb.addStroke(new GestureDescription.StrokeDescription(multi, 0, 80));
                }
                gesture = gb.build();
                duration = 80;
                actionName = "多点同时 x" + positions.length();
            } else {
                int posIndex = pickIndex();
                JSONObject obj = positions.getJSONObject(posIndex);
                int x = clampCoord(obj.getInt("x"), screenW);
                int y = clampCoord(obj.getInt("y"), screenH);
                Path path = new Path();
                if (gestureIndex == 2) {
                    int tx;
                    int ty;
                    if (positions.length() > 1) {
                        int next = (posIndex + 1) % positions.length();
                        JSONObject to = positions.getJSONObject(next);
                        tx = clampCoord(to.getInt("x"), screenW);
                        ty = clampCoord(to.getInt("y"), screenH);
                    } else {
                        tx = clampCoord(x + (screenW - x > 160 ? 120 : -120), screenW);
                        ty = clampCoord(y + (screenH - y > 160 ? 120 : -120), screenH);
                    }
                    path.moveTo(x, y);
                    path.lineTo(tx, ty);
                    duration = 320;
                } else {
                    path.moveTo(x, y);
                    duration = gestureIndex == 1 ? 520 : 50;
                }
                gesture = new GestureDescription.Builder()
                        .addStroke(new GestureDescription.StrokeDescription(path, 0, duration))
                        .build();
                actionName = gestureLabel();
            }
            int step = gestureIndex == 3 ? Math.max(1, positions.length()) : 1;
            current += step;
            final int delta = step;
            String label = "第 " + current + " 次";
            boolean dispatched = dispatchGesture(gesture, new GestureResultCallback() {
                @Override
                public void onCompleted(GestureDescription gestureDescription) {
                }
                @Override
                public void onCancelled(GestureDescription gestureDescription) {
                    if (running && instance != null) {
                        prefs().edit().putString("lastLog", "一次手势被系统取消")
                                .putString("status", "运行中").apply();
                    }
                }
            }, handler);
            if (dispatched) {
                recordStats(delta);
                prefs().edit().putInt("current", current)
                        .putString("status", "运行中")
                        .putString("lastLog", label + " " + actionName)
                        .apply();
            } else {
                prefs().edit().putInt("current", current)
                        .putString("status", "手势失败")
                        .putString("lastLog", "无法派发手势，请检查无障碍权限").apply();
            }
            if (total > 0 && current >= total) {
                finish("已完成");
                return;
            }
            long extra = jitterMs > 0 ? random.nextInt((int) jitterMs + 1) : 0;
            long delay = Math.max(duration + 40, intervalMs + extra);
            handler.postDelayed(task, delay);
        } catch (Exception e) {
            finish("发生错误");
        }
    }
    private int clampCoord(int v, int max) {
        return Math.max(0, Math.min(max - 1, v));
    }
    private void recordStats(int delta) {
        try {
            SharedPreferences p = prefs();
            String key = new SimpleDateFormat("yyyyMMdd", Locale.US).format(new Date());
            boolean sameDay = key.equals(p.getString("todayKey", ""));
            int today = sameDay ? p.getInt("today", 0) : 0;
            int allTime = p.getInt("allTime", 0);
            p.edit().putInt("today", today + delta)
                    .putInt("allTime", allTime + delta)
                    .putString("todayKey", key)
                    .apply();
        } catch (Exception ignored) {
        }
    }

    private void finish(String status) {
        running = false;
        handler.removeCallbacks(task);
        long elapsed = Math.max(0, System.currentTimeMillis() - startTimeWall);
        prefs().edit().putBoolean("running", false).putString("status", status)
                .putLong("elapsedMs", elapsed).putString("lastLog", status).apply();
        showStatusNotification("连点任务结束：" + status);
    }

    private int pickIndex() {
        int n = positions.length();
        if (n == 0) return 0;
        if (modeIndex == 1) return random.nextInt(n);
        if (modeIndex == 2) return 0;
        return current % n;
    }

    private JSONArray readPositions(SharedPreferences p) {
        try {
            return new JSONArray(p.getString("positions", "[]"));
        } catch (Exception e) {
            return new JSONArray();
        }
    }

    private String gestureLabel() {
        if (gestureIndex == 1) return "长按";
        if (gestureIndex == 2) return "滑动";
        if (gestureIndex == 3) return "多点同时";
        return "点击";
    }

    private void showStatusNotification(String text) {
        try {
            if (Build.VERSION.SDK_INT >= 26) {
                NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
                if (nm != null) {
                    NotificationChannel channel = new NotificationChannel(
                            CHANNEL_ID, "连点状态", NotificationManager.IMPORTANCE_LOW);
                    channel.setShowBadge(false);
                    nm.createNotificationChannel(channel);
                }
            }
            Intent open = new Intent(this, MainActivity.class);
            open.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            PendingIntent pi = PendingIntent.getActivity(this, 0, open,
                    PendingIntent.FLAG_UPDATE_CURRENT | (Build.VERSION.SDK_INT >= 23 ? PendingIntent.FLAG_IMMUTABLE : 0));
            Notification.Builder b;
            if (Build.VERSION.SDK_INT >= 26) {
                b = new Notification.Builder(this, CHANNEL_ID);
            } else {
                b = new Notification.Builder(this);
            }
            b.setSmallIcon(R.drawable.ic_notification)
                    .setContentTitle("极速连点器")
                    .setContentText(text)
                    .setPriority(Notification.PRIORITY_LOW)
                    .setContentIntent(pi)
                    .setOngoing(false);
            Intent stopUi = new Intent(this, MainActivity.class);
            stopUi.setAction(ACTION_STOP);
            stopUi.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            PendingIntent stopPi = PendingIntent.getActivity(this, 2, stopUi,
                    PendingIntent.FLAG_UPDATE_CURRENT | (Build.VERSION.SDK_INT >= 23 ? PendingIntent.FLAG_IMMUTABLE : 0));
            b.addAction(new Notification.Action.Builder(null, "停止连点", stopPi).build());
            NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
            if (nm != null) nm.notify(NOTIFICATION_ID, b.build());
        } catch (Exception ignored) {
        }
    }

    private void cancelStatusNotification() {
        try {
            NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
            if (nm != null) nm.cancel(NOTIFICATION_ID);
        } catch (Exception ignored) {
        }
    }
}