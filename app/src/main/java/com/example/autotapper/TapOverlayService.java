package com.example.autotapper;

import android.app.AlertDialog;
import android.app.Service;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Point;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.view.Gravity;
import android.view.HapticFeedbackConstants;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;

public class TapOverlayService extends Service {

    private static TapOverlayService active;
    private WindowManager wm;
    private WindowManager.LayoutParams params;
    private FrameLayout overlay;
    private LinearLayout topBar;
    private LinearLayout bottomBar;
    private TextView hintView;
    private TextView statusView;
    private TextView countView;
    private ArrayList<DotView> dots = new ArrayList<>();
    private int screenW = 1080;
    private int screenH = 1920;
    private int statusBarH = 60;
    private int navBarH = 60;
    private final Handler main = new Handler(Looper.getMainLooper());

    public static boolean isActive() {
        return active != null;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (overlay != null) return START_NOT_STICKY;
        active = this;
        wm = (WindowManager) getSystemService(WINDOW_SERVICE);
        Point real = new Point();
        wm.getDefaultDisplay().getRealSize(real);
        screenW = real.x;
        screenH = real.y;
        statusBarH = systemDimen("status_bar_height", 60);
        navBarH = systemDimen("navigation_bar_height", 60);

        int type = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                : WindowManager.LayoutParams.TYPE_PHONE;

        params = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT,
                type,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                        | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                        | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
                PixelFormat.TRANSLUCENT);
        params.gravity = Gravity.TOP;

        overlay = new FrameLayout(this) {
            @Override
            public boolean onTouchEvent(MotionEvent event) {
                return false;
            }
        };
        overlay.setBackgroundColor(0x66000000);
        overlay.setClipToPadding(false);

        topBar = buildTopBar();
        bottomBar = buildBottomBar();
        FrameLayout.LayoutParams topLp = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.WRAP_CONTENT,
                Gravity.TOP);
        topLp.topMargin = statusBarH + dp(8);
        topLp.leftMargin = dp(10);
        topLp.rightMargin = dp(10);
        overlay.addView(topBar, topLp);

        FrameLayout.LayoutParams bottomLp = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.WRAP_CONTENT,
                Gravity.BOTTOM);
        bottomLp.bottomMargin = navBarH + dp(8);
        bottomLp.leftMargin = dp(10);
        bottomLp.rightMargin = dp(10);
        overlay.addView(bottomBar, bottomLp);

        wm.addView(overlay, params);
        overlay.post(this::loadSavedDots);
        return START_STICKY;
    }

    private void loadSavedDots() {
        dots.clear();
        SharedPreferences p = getSharedPreferences("tap_prefs", MODE_PRIVATE);
        try {
            JSONArray arr = new JSONArray(p.getString("positions", "[]"));
            for (int i = 0; i < arr.length(); i++) {
                JSONObject o = arr.getJSONObject(i);
                addDot(o.getInt("x"), o.getInt("y"));
            }
        } catch (Exception ignored) {
        }
        if (dots.isEmpty()) {
            addDot(screenW / 2, clampY(screenH * 42 / 100));
        }
        updateNumbers();
        updateStatus();
    }

    private LinearLayout buildTopBar() {
        LinearLayout bar = new LinearLayout(this);
        bar.setOrientation(LinearLayout.HORIZONTAL);
        bar.setGravity(Gravity.CENTER_VERTICAL);
        bar.setPadding(dp(14), dp(10), dp(10), dp(10));
        bar.setBackgroundResource(R.drawable.bg_overlay_bar);

        LinearLayout textWrap = new LinearLayout(this);
        textWrap.setOrientation(LinearLayout.VERTICAL);

        LinearLayout titleRow = new LinearLayout(this);
        titleRow.setOrientation(LinearLayout.HORIZONTAL);
        titleRow.setGravity(Gravity.CENTER_VERTICAL);
        TextView title = new TextView(this);
        title.setText("圆点定位");
        title.setTextColor(Color.WHITE);
        title.setTextSize(17);
        title.setTypeface(null, Typeface.BOLD);
        titleRow.addView(title);

        countView = new TextView(this);
        countView.setTextColor(0xFF37E6B8);
        countView.setTextSize(12);
        countView.setPadding(dp(8), dp(2), dp(8), dp(2));
        countView.setBackgroundResource(R.drawable.bg_neutral);
        LinearLayout.LayoutParams countLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        countLp.leftMargin = dp(8);
        titleRow.addView(countView, countLp);
        textWrap.addView(titleRow);

        hintView = new TextView(this);
        hintView.setText("拖动圆点对齐目标 · 长按弹出菜单 · 拖到左右边缘换顺序");
        hintView.setTextColor(0xFFB9C6DC);
        hintView.setTextSize(11);
        LinearLayout.LayoutParams hintLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        hintLp.topMargin = dp(3);
        textWrap.addView(hintView, hintLp);

        bar.addView(textWrap, new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));

        Button addBtn = makeTopButton("＋", 0xFF1ED39E, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int bx = screenW / 2;
                int by = clampY(screenH * 45 / 100);
                if (!dots.isEmpty()) {
                    DotView last = dots.get(dots.size() - 1);
                    bx = last.x + dp(16);
                    by = last.y + dp(16);
                }
                addDot(bx, by);
                updateNumbers();
                updateStatus();
                Toast.makeText(TapOverlayService.this, "已添加圆点", Toast.LENGTH_SHORT).show();
            }
        });
        LinearLayout.LayoutParams addLp = new LinearLayout.LayoutParams(dp(52), dp(46));
        addLp.leftMargin = dp(8);
        bar.addView(addBtn, addLp);
        return bar;
    }

    private Button makeTopButton(final String text, final int color, final View.OnClickListener listener) {
        Button b = new Button(this);
        b.setText(text);
        b.setTextSize(15);
        b.setTextColor(0xFFFFFFFF);
        b.setTypeface(null, Typeface.BOLD);
        b.setBackgroundColor(color);
        b.setOnClickListener(listener);
        return b;
    }

    private LinearLayout buildBottomBar() {
        LinearLayout bar = new LinearLayout(this);
        bar.setOrientation(LinearLayout.HORIZONTAL);
        bar.setGravity(Gravity.CENTER_VERTICAL);
        bar.setPadding(dp(12), dp(8), dp(12), dp(8));
        bar.setBackgroundResource(R.drawable.bg_overlay_bar);

        statusView = new TextView(this);
        statusView.setTextColor(0xFFDCE6F5);
        statusView.setTextSize(12);
        bar.addView(statusView, new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));
        Button clear = makeTopButton("清空", 0xFF8A55E8, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                confirmClearDots();
            }
        });
        LinearLayout.LayoutParams clearLp = new LinearLayout.LayoutParams(dp(72), dp(44));
        clearLp.leftMargin = dp(8);
        bar.addView(clear, clearLp);

        Button cancel = makeTopButton("取消", 0xFF51627E, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finishAndSave(false);
            }
        });
        LinearLayout.LayoutParams cancelLp = new LinearLayout.LayoutParams(dp(88), dp(44));
        cancelLp.leftMargin = dp(8);
        bar.addView(cancel, cancelLp);

        Button save = makeTopButton("保存", 0xFF1ED39E, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finishAndSave(true);
            }
        });
        LinearLayout.LayoutParams saveLp = new LinearLayout.LayoutParams(dp(88), dp(44));
        saveLp.leftMargin = dp(8);
        bar.addView(save, saveLp);
        return bar;
    }

    private void addDot(int x, int y) {
        DotView dot = new DotView(this, dots.size(), clampX(x), clampY(y));
        dots.add(dot);
        overlay.addView(dot);
        dot.updateLayout();
    }

    private void updateNumbers() {
        for (int i = 0; i < dots.size(); i++) {
            dots.get(i).setNumber(i + 1);
        }
        if (countView != null) {
            countView.setText(dots.size() + " 个");
        }
        if (hintView != null && dots.size() > 1) {
            hintView.setText("顺序：" + buildOrderText() + "  ·  长按圆点可调整");
        } else if (hintView != null) {
            hintView.setText("拖动圆点对齐目标 · 长按弹出菜单 · 拖到左右边缘换顺序");
        }
    }

    private String buildOrderText() {
        if (dots.isEmpty()) return "无";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < dots.size(); i++) {
            if (i > 0) sb.append(" → ");
            sb.append(i + 1);
        }
        return sb.toString();
    }

    void onEdgeHint(final boolean left, final boolean right) {
        if (hintView == null) return;
        if (left) {
            hintView.setText("已到左边缘：松手后此圆点向前移动一位");
        } else if (right) {
            hintView.setText("已到右边缘：松手后此圆点向后移动一位");
        } else if (dots.size() > 1) {
            hintView.setText("顺序：" + buildOrderText() + "  ·  长按圆点可调整");
        } else {
            hintView.setText("拖动圆点对齐目标 · 长按弹出菜单 · 拖到左右边缘换顺序");
        }
    }

    void moveDot(int from, int delta) {
        int to = from + delta;
        if (from < 0 || to < 0 || to >= dots.size()) return;
        DotView d = dots.remove(from);
        dots.add(to, d);
        overlay.removeView(d);
        overlay.addView(d);
        d.updateLayout();
        updateNumbers();
        updateStatus();
        if (d != null) {
            d.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK);
        }
    }

    void checkReorder(DotView d) {
        if (dots.size() < 2) {
            d.edgeHandled = false;
            onEdgeHint(false, false);
            return;
        }
        int index = dots.indexOf(d);
        if (index < 0) return;
        boolean inLeft = d.x < Math.min(dp(56), screenW / 7);
        boolean inRight = d.x > screenW - Math.min(dp(56), screenW / 7);
        if (inLeft || inRight) {
            d.inEdgeZone = true;
            if (!d.edgeHandled) {
                d.edgeHandled = true;
                if (inLeft) {
                    onEdgeHint(true, false);
                    if (index > 0) moveDot(index, -1);
                } else if (inRight) {
                    onEdgeHint(false, true);
                    if (index < dots.size() - 1) moveDot(index, 1);
                }
            }
        } else {
            if (d.inEdgeZone) {
                d.inEdgeZone = false;
                d.edgeHandled = false;
                onEdgeHint(false, false);
            }
        }
    }

    private void showDotMenu(final DotView dot) {
        int index = dots.indexOf(dot);
        if (index < 0) return;
        final AlertDialog dialog = new AlertDialog.Builder(this).create();
        dialog.setTitle("圆点 " + (index + 1) + "  (" + dot.x + ", " + dot.y + ")");
        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(14), dp(6), dp(14), dp(10));
        TextView hint = new TextView(this);
        hint.setText("方向键微调 10px · 长按方向键微调 1px");
        hint.setTextColor(0xFFB8C6DA);
        hint.setTextSize(12);
        content.addView(hint, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        String[] arrows = {"↖", "↑", "↗", "←", "●", "→", "↙", "↓", "↘"};
        for (int r = 0; r < 3; r++) {
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            for (int c = 0; c < 3; c++) {
                final int dx = (c - 1) * 10;
                final int dy = (r - 1) * 10;
                Button key = new Button(this);
                key.setText(arrows[r * 3 + c]);
                key.setTextSize(14);
                key.setAllCaps(false);
                key.setTextColor(Color.WHITE);
                key.setBackgroundResource(R.drawable.bg_input);
                key.setOnClickListener(v -> {
                    nudgeDot(dot, dx / 10, dy / 10, 10);
                    int cur = dots.indexOf(dot);
                    if (cur >= 0) dialog.setTitle("圆点 " + (cur + 1) + "  (" + dot.x + ", " + dot.y + ")");
                });
                key.setOnLongClickListener(v -> {
                    nudgeDot(dot, Integer.signum(dx), Integer.signum(dy), 1);
                    int cur = dots.indexOf(dot);
                    if (cur >= 0) dialog.setTitle("圆点 " + (cur + 1) + "  (" + dot.x + ", " + dot.y + ")");
                    return true;
                });
                LinearLayout.LayoutParams keyLp = new LinearLayout.LayoutParams(0, dp(42), 1);
                if (c > 0) keyLp.leftMargin = dp(6);
                row.addView(key, keyLp);
            }
            LinearLayout.LayoutParams rowLp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            if (r > 0) rowLp.topMargin = dp(6);
            content.addView(row, rowLp);
        }
        LinearLayout actions = new LinearLayout(this);
        actions.setOrientation(LinearLayout.HORIZONTAL);
        addActionButton(actions, "前移", 0xFF51627E, v -> {
            int cur = dots.indexOf(dot);
            if (cur > 0) {
                moveDot(cur, -1);
                dialog.setTitle("圆点 " + cur + "  (" + dot.x + ", " + dot.y + ")");
            }
        });
        addActionButton(actions, "后移", 0xFF51627E, v -> {
            int cur = dots.indexOf(dot);
            if (cur >= 0 && cur < dots.size() - 1) {
                moveDot(cur, 1);
                dialog.setTitle("圆点 " + (cur + 2) + "  (" + dot.x + ", " + dot.y + ")");
            }
        });
        addActionButton(actions, "删除", 0xFFFF6B81, v -> {
            int cur = dots.indexOf(dot);
            if (cur >= 0) removeDot(cur);
            dialog.dismiss();
        });
        addActionButton(actions, "关闭", 0xFF51627E, v -> dialog.dismiss());
        LinearLayout.LayoutParams actionsLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        actionsLp.topMargin = dp(12);
        content.addView(actions, actionsLp);
        dialog.setView(content);
        try {
            if (dialog.getWindow() != null && Build.VERSION.SDK_INT >= 26) {
                dialog.getWindow().setType(WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY);
            }
            dialog.show();
        } catch (Exception ignored) {
        }
    }
    private Button makeMenuButton(String text, int color, View.OnClickListener listener) {
        Button b = new Button(this);
        b.setText(text);
        b.setTextSize(13);
        b.setAllCaps(false);
        b.setTextColor(Color.WHITE);
        b.setBackgroundColor(color);
        b.setOnClickListener(listener);
        return b;
    }
    private void addActionButton(LinearLayout container, String text, int color, View.OnClickListener listener) {
        Button b = makeMenuButton(text, color, listener);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, dp(42), 1);
        lp.leftMargin = dp(6);
        container.addView(b, lp);
    }
    private void nudgeDot(DotView dot, int dx, int dy, int step) {
        dot.x = clampX(dot.x + dx * step);
        dot.y = clampY(dot.y + dy * step);
        dot.updateLayout();
        dot.invalidate();
        dot.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK);
        updateStatus();
    }
    private void confirmClearDots() {
        AlertDialog.Builder b = new AlertDialog.Builder(this)
                .setTitle("清空圆点")
                .setMessage("确定删除全部 " + dots.size() + " 个圆点？")
                .setPositiveButton("清空", (d, w) -> {
                    while (!dots.isEmpty()) removeDot(0);
                    updateStatus();
                })
                .setNegativeButton("取消", null);
        try {
            AlertDialog d = b.create();
            if (d.getWindow() != null && Build.VERSION.SDK_INT >= 26) {
                d.getWindow().setType(WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY);
            }
            d.show();
        } catch (Exception ignored) {
        }
    }

    private void removeDot(int index) {
        if (index < 0 || index >= dots.size()) return;
        DotView d = dots.remove(index);
        overlay.removeView(d);
        updateNumbers();
        updateStatus();
    }

    private void updateStatus() {
        if (statusView != null) {
            statusView.setText("已定位 " + dots.size() + " 个圆点，顺序 " + buildOrderText());
        }
    }

    private void finishAndSave(final boolean save) {
        if (save) {
            try {
                JSONArray arr = new JSONArray();
                for (DotView d : dots) {
                    JSONObject o = new JSONObject();
                    o.put("x", d.x);
                    o.put("y", d.y);
                    arr.put(o);
                }
                getSharedPreferences("tap_prefs", MODE_PRIVATE).edit()
                        .putString("positions", arr.toString())
                        .putBoolean("overlay_saved", true)
                        .apply();
                Toast.makeText(this, "已保存 " + dots.size() + " 个位置", Toast.LENGTH_SHORT).show();
            } catch (Exception ignored) {
            }
        } else {
            Toast.makeText(this, "已取消定位", Toast.LENGTH_SHORT).show();
        }
        stopSelf();
    }

    @Override
    public void onDestroy() {
        active = null;
        main.removeCallbacksAndMessages(null);
        if (overlay != null && wm != null) {
            try {
                wm.removeView(overlay);
            } catch (Exception ignored) {
            }
        }
        overlay = null;
        super.onDestroy();
    }

    private int clampX(int x) {
        return Math.max(dp(32), Math.min(screenW - dp(32), x));
    }

    private int clampY(int y) {
        int top = statusBarH + dp(92);
        int bottom = screenH - navBarH - dp(112);
        return Math.max(top, Math.min(bottom, y));
    }

    private int systemDimen(String key, int def) {
        try {
            int id = getResources().getIdentifier(key, "dimen", "android");
            if (id > 0) {
                return getResources().getDimensionPixelSize(id);
            }
        } catch (Exception ignored) {
        }
        return (int) (def * getResources().getDisplayMetrics().density);
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private class DotView extends View {
        static final int DOT_SIZE_DP = 58;
        static final int[] COLORS = {0xFFFF6B81, 0xFF64A9FF, 0xFF37E6B8, 0xFFFFB454,
                0xFFB07CFF, 0xFFFF9AC4, 0xFF3EE6A1, 0xFF6AC5FF};

        private int x;
        private int y;
        private int number;
        private final int baseColor;
        private final Paint fill;
        private final Paint ring;
        private final Paint text;
        private final Paint glow;
        private boolean dragging = false;
        private boolean longPressReady = false;
        boolean edgeHandled = false;
        boolean inEdgeZone = false;
        private float downX;
        private float downY;
        private float dx;
        private float dy;
        private long downTime;

        DotView(final TapOverlayService svc, final int index, final int px, final int py) {
            super(svc);
            x = px;
            y = py;
            number = index + 1;
            baseColor = COLORS[index % COLORS.length];
            setLayoutParams(new FrameLayout.LayoutParams(dp(DOT_SIZE_DP), dp(DOT_SIZE_DP)));
            fill = new Paint(Paint.ANTI_ALIAS_FLAG);
            fill.setStyle(Paint.Style.FILL);
            ring = new Paint(Paint.ANTI_ALIAS_FLAG);
            ring.setStyle(Paint.Style.STROKE);
            ring.setStrokeWidth(dp(3));
            ring.setColor(Color.WHITE);
            text = new Paint(Paint.ANTI_ALIAS_FLAG);
            text.setColor(Color.WHITE);
            text.setTextSize(dp(22));
            text.setTextAlign(Paint.Align.CENTER);
            text.setTypeface(Typeface.DEFAULT_BOLD);
            glow = new Paint(Paint.ANTI_ALIAS_FLAG);
            glow.setStyle(Paint.Style.STROKE);
            glow.setStrokeWidth(dp(6));
            glow.setColor(baseColor);
            updateLayout();
        }

        @Override
        protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
            int size = dp(DOT_SIZE_DP);
            setMeasuredDimension(size, size);
        }

        void setNumber(int n) {
            number = n;
            postInvalidate();
        }

        boolean containsRaw(float rawX, float rawY) {
            return rawX >= getLeft() - dp(12) && rawX <= getRight() + dp(12)
                    && rawY >= getTop() - dp(12) && rawY <= getBottom() + dp(12);
        }

        void updateLayout() {
            int size = dp(DOT_SIZE_DP);
            int l = x - size / 2;
            int t = y - size / 2;
            FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(size, size);
            lp.gravity = Gravity.TOP | Gravity.START;
            lp.leftMargin = l;
            lp.topMargin = t;
            setLayoutParams(lp);
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            float cx = getWidth() / 2f;
            float cy = getHeight() / 2f;
            float r = Math.min(cx, cy) - dp(2);
            if (dragging) {
                canvas.drawCircle(cx, cy, r + dp(6), glow);
                ring.setColor(0xFFFFE082);
            } else {
                ring.setColor(Color.WHITE);
            }
            fill.setColor(dragging ? 0xFFFFC25E : baseColor);
            canvas.drawCircle(cx, cy, r, fill);
            canvas.drawCircle(cx, cy, r, ring);
            Paint.FontMetrics fm = text.getFontMetrics();
            float baseline = cy - (fm.ascent + fm.descent) / 2f;
            canvas.drawText(String.valueOf(number), cx, baseline, text);
            if (longPressReady) {
                Paint marker = new Paint(Paint.ANTI_ALIAS_FLAG);
                marker.setColor(Color.WHITE);
                marker.setStrokeWidth(dp(4));
                canvas.drawLine(cx - dp(8), cy + r + dp(4), cx + dp(8), cy + r + dp(4), marker);
            }
        }

        @Override
        public boolean onTouchEvent(MotionEvent event) {
            int action = event.getActionMasked();
            switch (action) {
                case MotionEvent.ACTION_DOWN:
                    downX = event.getRawX();
                    downY = event.getRawY();
                    dx = downX - x;
                    dy = downY - y;
                    downTime = event.getEventTime();
                    dragging = false;
                    longPressReady = false;
                    edgeHandled = false;
                    performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP);
                    if (getParent() instanceof android.view.ViewGroup) {
                        ((android.view.ViewGroup) getParent()).bringChildToFront(this);
                    }
                    invalidate();
                    return true;

                case MotionEvent.ACTION_MOVE:
                    if (longPressReady) return true;
                    float rawX = event.getRawX();
                    float rawY = event.getRawY();
                    float distance = (float) Math.hypot(rawX - downX, rawY - downY);
                    if (!dragging && distance < dp(16)) {
                        long pressed = event.getEventTime() - downTime;
                        if (pressed > 500) {
                            longPressReady = true;
                            performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);
                            invalidate();
                            return true;
                        }
                    }
                    if (distance > dp(4)) dragging = true;
                    if (dragging) {
                        x = clampX((int) (rawX - dx));
                        y = clampY((int) (rawY - dy));
                        updateLayout();
                        checkReorder(this);
                        invalidate();
                    }
                    return true;

                case MotionEvent.ACTION_UP:
                    if (longPressReady) {
                        longPressReady = false;
                        invalidate();
                        showDotMenu(this);
                    } else if (!dragging) {
                        showDotMenu(this);
                    } else {
                        dragging = false;
                        edgeHandled = false;
                        inEdgeZone = false;
                        checkReorder(this);
                        onEdgeHint(false, false);
                        invalidate();
                    }
                    return true;

                case MotionEvent.ACTION_CANCEL:
                    dragging = false;
                    longPressReady = false;
                    edgeHandled = false;
                    inEdgeZone = false;
                    invalidate();
                    return true;
            }
            return false;
        }
    }
}