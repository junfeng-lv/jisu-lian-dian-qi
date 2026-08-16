package com.example.autotapper;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;

public class MainActivity extends Activity {
    private static final String PREFS = "tap_prefs";
    private static final int REQ_OVERLAY = 2001;
    private static final int REQ_QUICK = 2003;

    private TextView tvA11y, tvRun, tvClicks, tvElapsed, tvStatus, tvLog;
    private TextView tvToday, tvAll, tvRate;
    private TextView posInfo, posCount, a11yHint;
    private LinearLayout posList;
    private EditText intervalInput, jitterInput, totalInput;
    private CheckBox infiniteCheck;
    private Spinner modeSpinner, gestureSpinner;
    private Switch quickSwitch;
    private Button startBtn, stopBtn, setPosBtn, clearBtn, enableBtn, templateBtn, helpBtn, saveProfileBtn, profileBtn, galleryBtn;
    private JSONArray positions = new JSONArray();
    private Handler refreshHandler = new Handler(Looper.getMainLooper());
    private Runnable refreshRunnable = this::refreshStatus;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        requestNotificationPermission();

        tvA11y = findViewById(R.id.tvA11y);
        tvRun = findViewById(R.id.tvRun);
        tvClicks = findViewById(R.id.tvClicks);
        tvElapsed = findViewById(R.id.tvElapsed);
        tvStatus = findViewById(R.id.tvStatus);
        tvLog = findViewById(R.id.tvLog);
        posInfo = findViewById(R.id.posInfo);
        posCount = findViewById(R.id.posCount);
        a11yHint = findViewById(R.id.a11yHint);
        posList = findViewById(R.id.posList);
        intervalInput = findViewById(R.id.interval);
        jitterInput = findViewById(R.id.jitter);
        totalInput = findViewById(R.id.total);
        infiniteCheck = findViewById(R.id.infinite);
        modeSpinner = findViewById(R.id.modeSpinner);
        gestureSpinner = findViewById(R.id.gestureSpinner);
        startBtn = findViewById(R.id.startBtn);
        stopBtn = findViewById(R.id.stopBtn);
        setPosBtn = findViewById(R.id.setPos);
        clearBtn = findViewById(R.id.clearPos);
        enableBtn = findViewById(R.id.enableBtn);
        templateBtn = findViewById(R.id.templateBtn);
        helpBtn = findViewById(R.id.helpBtn);
        galleryBtn = findViewById(R.id.galleryBtn);
        saveProfileBtn = findViewById(R.id.saveProfileBtn);
        profileBtn = findViewById(R.id.profileBtn);
        tvToday = findViewById(R.id.tvToday);
        tvAll = findViewById(R.id.tvAll);
        tvRate = findViewById(R.id.tvRate);
        quickSwitch = findViewById(R.id.quickSwitch);

        SharedPreferences p = getSharedPreferences(PREFS, MODE_PRIVATE);
        intervalInput.setText(String.valueOf(p.getLong("interval", 150)));
        jitterInput.setText(String.valueOf(p.getLong("jitter", 0)));
        totalInput.setText(String.valueOf(p.getInt("total", 50)));
        modeSpinner.setSelection(Math.min(2, Math.max(0, p.getInt("modeIndex", 0))));
        gestureSpinner.setSelection(Math.min(3, Math.max(0, p.getInt("gestureIndex", 0))));
        boolean infinite = p.getInt("total", 50) == 0;
        infiniteCheck.setChecked(infinite);
        totalInput.setEnabled(!infinite);

        infiniteCheck.setOnCheckedChangeListener((b, checked) -> totalInput.setEnabled(!checked));
        startBtn.setOnClickListener(v -> startTap());
        stopBtn.setOnClickListener(v -> stopTap());
        setPosBtn.setOnClickListener(v -> openPositionOverlay());
        clearBtn.setOnClickListener(v -> confirmClear());
        templateBtn.setOnClickListener(v -> showTemplateMenu());
        helpBtn.setOnClickListener(v -> startActivity(new Intent(this, HelpActivity.class)));
        galleryBtn.setOnClickListener(v -> startActivity(new Intent(this, TemplateActivity.class)));
        enableBtn.setOnClickListener(v -> openAccessibilitySettings());
        saveProfileBtn.setOnClickListener(v -> showSaveProfileDialog());
        profileBtn.setOnClickListener(v -> showProfileMenu());
        quickSwitch.setOnCheckedChangeListener((b, checked) -> toggleQuickControl(checked));
        handleStopIntent(getIntent());
        refreshHandler.postDelayed(refreshRunnable, 250);
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadPositions();
        refreshStatus();
        if (quickSwitch != null) {
            quickSwitch.setChecked(QuickControlService.isActive());
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        handleStopIntent(intent);
    }

    private void handleStopIntent(Intent intent) {
        if (intent != null && "ACTION_STOP".equals(intent.getAction())) {
            stopTap();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        refreshHandler.removeCallbacks(refreshRunnable);
    }    private void requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= 33) {
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, 100);
        }
    }
    private void loadPositions() {
        try {
            positions = new JSONArray(getSharedPreferences(PREFS, MODE_PRIVATE).getString("positions", "[]"));
        } catch (Exception ignored) {
            positions = new JSONArray();
        }
        updatePosDisplay();
    }

    private void updatePosDisplay() {
        int len = positions.length();
        posCount.setText(len + " 个");
        posList.removeAllViews();
        if (len == 0) {
            posInfo.setVisibility(View.VISIBLE);
            clearBtn.setVisibility(View.GONE);
            return;
        }
        posInfo.setVisibility(View.GONE);
        clearBtn.setVisibility(View.VISIBLE);
        for (int i = 0; i < len; i++) {
            try {
                final int index = i;
                JSONObject obj = positions.getJSONObject(i);
                int x = obj.getInt("x");
                int y = obj.getInt("y");

                LinearLayout row = new LinearLayout(this);
                row.setOrientation(LinearLayout.HORIZONTAL);
                row.setGravity(Gravity.CENTER_VERTICAL);
                row.setPadding(dp(10), dp(6), dp(6), dp(6));
                row.setBackgroundResource(R.drawable.bg_row);
                LinearLayout.LayoutParams rowLp = new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                rowLp.bottomMargin = dp(8);
                row.setLayoutParams(rowLp);

                TextView badge = new TextView(this);
                badge.setText(String.valueOf(index + 1));
                badge.setTextColor(0xFFFFFFFF);
                badge.setTextSize(14);
                badge.setGravity(Gravity.CENTER);
                badge.setBackgroundResource(R.drawable.bg_badge);
                row.addView(badge, new LinearLayout.LayoutParams(dp(36), dp(36)));

                TextView label = new TextView(this);
                label.setText("(" + x + ", " + y + ")");
                label.setTextColor(0xFFD7E2F5);
                label.setTextSize(14);
                label.setPadding(dp(10), 0, dp(10), 0);
                row.addView(label, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));

                row.addView(makeSmallButton("↑", v -> movePos(index, -1)), smallLp());
                row.addView(makeSmallButton("↓", v -> movePos(index, 1)), smallLp());
                row.addView(makeSmallButton("✕", v -> deletePos(index)), smallLp());
                posList.addView(row);
            } catch (Exception ignored) {
            }
        }
    }

    private Button makeSmallButton(String text, View.OnClickListener listener) {
        Button b = new Button(this);
        b.setText(text);
        b.setTextColor(0xFFFFFFFF);
        b.setTextSize(15);
        b.setBackgroundResource(R.drawable.bg_neutral);
        b.setOnClickListener(listener);
        return b;
    }

    private LinearLayout.LayoutParams smallLp() {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(dp(44), dp(40));
        lp.leftMargin = dp(6);
        return lp;
    }

    private void savePositions() {
        getSharedPreferences(PREFS, MODE_PRIVATE).edit()
                .putString("positions", positions.toString()).apply();
    }

    private void movePos(int i, int delta) {
        int j = i + delta;
        if (i < 0 || j < 0 || j >= positions.length()) return;
        try {
            Object tmp = positions.get(i);
            positions.put(i, positions.get(j));
            positions.put(j, tmp);
            savePositions();
            updatePosDisplay();
        } catch (Exception ignored) {
        }
    }

    private void deletePos(int i) {
        if (i < 0 || i >= positions.length()) return;
        JSONArray next = new JSONArray();
        for (int k = 0; k < positions.length(); k++) {
            if (k == i) continue;
            try { next.put(positions.get(k)); } catch (Exception ignored) {}
        }
        positions = next;
        savePositions();
        updatePosDisplay();
    }

    private void confirmClear() {
        new AlertDialog.Builder(this)
                .setTitle("清空位置")
                .setMessage("确定清空全部点击位置？")
                .setPositiveButton("清空", (d, w) -> {
                    positions = new JSONArray();
                    savePositions();
                    updatePosDisplay();
                })
                .setNegativeButton("取消", null)
                .show();
    }

    private void showTemplateMenu() {
        String[] items = {
                "1 点 · 单点连点",
                "2 点 · 中心连锁",
                "3 点 · 三角循环",
                "5 点 · 多点连击",
                "3 点 · 签到点赞",
                "4 点 · 日常任务",
                "6 点 · 挂机巡逻",
                "6 点 · 全屏环绕",
                "清空当前位置"
        };
        new AlertDialog.Builder(this)
                .setTitle("快捷模板")
                .setItems(items, (d, which) -> applyTemplates(which))
                .setNegativeButton("取消", null)
                .show();
    }

    private void applyTemplates(int which) {
        if (which == 8) {
            positions = new JSONArray();
            savePositions();
            updatePosDisplay();
            Toast.makeText(this, "已清空位置", Toast.LENGTH_SHORT).show();
            return;
        }
        int w = Math.max(1, getResources().getDisplayMetrics().widthPixels);
        int h = Math.max(1, getResources().getDisplayMetrics().heightPixels);
        int[][] preset;
        switch (which) {
            case 1:
                preset = new int[][]{{50, 50}};
                break;
            case 2:
                preset = new int[][]{{35, 58}, {65, 58}};
                break;
            case 3:
                preset = new int[][]{{50, 35}, {30, 60}, {70, 60}};
                break;
            case 4:
                preset = new int[][]{{50, 30}, {25, 55}, {75, 55}, {50, 80}, {50, 55}};
                break;
            case 5:
                preset = new int[][]{{42, 60}, {58, 72}, {50, 84}};
                break;
            case 6:
                preset = new int[][]{{30, 40}, {70, 40}, {30, 65}, {70, 65}};
                break;
            case 7:
                preset = new int[][]{{50, 30}, {20, 55}, {80, 55}, {50, 80}, {35, 42}, {65, 42}};
                break;
            default:
                preset = new int[][]{{50, 55}, {25, 42}, {75, 42}, {25, 72}, {75, 72}, {50, 30}};
                break;
        }
        try {
            JSONArray next = new JSONArray();
            for (int[] pct : preset) {
                JSONObject o = new JSONObject();
                o.put("x", w * pct[0] / 100);
                o.put("y", h * pct[1] / 100);
                next.put(o);
            }
            positions = next;
            savePositions();
            updatePosDisplay();
            Toast.makeText(this, "已载入 " + preset.length + " 个模板圆点，可在悬浮定位里微调", Toast.LENGTH_LONG).show();
        } catch (Exception ignored) {
        }
    }
    private JSONArray profiles() {
        try {
            return new JSONArray(getSharedPreferences(PREFS, MODE_PRIVATE).getString("profiles", "[]"));
        } catch (Exception e) {
            return new JSONArray();
        }
    }
    private void showSaveProfileDialog() {
        final EditText input = new EditText(this);
        input.setText("方案 " + (profiles().length() + 1));
        input.setTextColor(0xFFEEF6FF);
        input.setHint("方案名称");
        input.setBackgroundResource(R.drawable.bg_input);
        input.setPadding(dp(14), dp(10), dp(14), dp(10));
        new AlertDialog.Builder(this)
                .setTitle("保存当前方案")
                .setMessage("保存圆点、参数与手势，之后可一键恢复。")
                .setView(input)
                .setPositiveButton("保存", (d, w) -> {
                    String name = input.getText().toString().trim();
                    if (name.isEmpty()) name = "方案 " + (profiles().length() + 1);
                    saveProfile(name);
                })
                .setNegativeButton("取消", null)
                .show();
    }
    private void saveProfile(String name) {
        try {
            JSONArray arr = profiles();
            JSONObject o = new JSONObject();
            o.put("name", name);
            o.put("positions", positions.toString());
            o.put("interval", Math.max(30, parseLong(intervalInput, 150)));
            o.put("jitter", Math.max(0, parseLong(jitterInput, 0)));
            o.put("total", infiniteCheck.isChecked() ? 0 : Math.max(1, parseInt(totalInput, 50)));
            o.put("modeIndex", modeSpinner.getSelectedItemPosition());
            o.put("gestureIndex", gestureSpinner.getSelectedItemPosition());
            arr.put(o);
            SharedPreferences.Editor e = getSharedPreferences(PREFS, MODE_PRIVATE).edit();
            e.putString("profiles", arr.toString()).apply();
            Toast.makeText(this, "已保存方案：" + name, Toast.LENGTH_SHORT).show();
        } catch (Exception ignored) {
        }
    }
    private void showProfileMenu() {
        JSONArray arr = profiles();
        int n = arr.length();
        String[] items = new String[n + 1];
        for (int i = 0; i < n; i++) {
            try {
                items[i] = (i + 1) + ". " + arr.getJSONObject(i).getString("name");
            } catch (Exception ignored) {
                items[i] = "方案 " + (i + 1);
            }
        }
        items[n] = "删除全部方案";
        new AlertDialog.Builder(this)
                .setTitle("方案库")
                .setItems(items, (d, which) -> {
                    if (which == n) {
                        confirmClearProfiles();
                        return;
                    }
                    loadProfile(which);
                })
                .setNegativeButton("取消", null)
                .show();
    }
    private void loadProfile(int index) {
        try {
            JSONArray arr = profiles();
            JSONObject o = arr.getJSONObject(index);
            positions = new JSONArray(o.getString("positions"));
            savePositions();
            intervalInput.setText(String.valueOf(o.getLong("interval")));
            jitterInput.setText(String.valueOf(o.getLong("jitter")));
            int total = o.getInt("total");
            totalInput.setText(String.valueOf(total));
            infiniteCheck.setChecked(total == 0);
            totalInput.setEnabled(total != 0);
            modeSpinner.setSelection(Math.min(2, Math.max(0, o.getInt("modeIndex"))));
            gestureSpinner.setSelection(Math.min(3, Math.max(0, o.getInt("gestureIndex"))));
            updatePosDisplay();
            Toast.makeText(this, "已载入方案 " + o.optString("name", "方案 " + (index + 1)), Toast.LENGTH_SHORT).show();
        } catch (Exception ignored) {
        }
    }
    private void confirmClearProfiles() {
        new AlertDialog.Builder(this)
                .setTitle("删除全部方案")
                .setMessage("确定删除全部保存方案？")
                .setPositiveButton("删除", (d, w) -> {
                    getSharedPreferences(PREFS, MODE_PRIVATE).edit().remove("profiles").apply();
                    Toast.makeText(this, "已删除全部方案", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("取消", null)
                .show();
    }
    private void startTap() {
        if (positions.length() == 0) {
            Toast.makeText(this, "请先设置点击位置", Toast.LENGTH_SHORT).show();
            return;
        }
        if (!TapAccessibilityService.isConnected()) {
            Toast.makeText(this, "请先开启无障碍服务", Toast.LENGTH_LONG).show();
            openAccessibilitySettings();
            return;
        }
        long interval = Math.max(30, parseLong(intervalInput, 150));
        long jitter = Math.max(0, parseLong(jitterInput, 0));
        int total = infiniteCheck.isChecked() ? 0 : Math.max(1, parseInt(totalInput, 50));
        getSharedPreferences(PREFS, MODE_PRIVATE).edit()
                .putString("positions", positions.toString())
                .putLong("interval", interval)
                .putLong("jitter", jitter)
                .putInt("total", total)
                .putInt("modeIndex", modeSpinner.getSelectedItemPosition())
                .putInt("gestureIndex", gestureSpinner.getSelectedItemPosition())
                .putBoolean("running", true)
                .putString("status", "启动中")
                .putInt("current", 0)
                .putLong("startTime", System.currentTimeMillis())
                .putLong("elapsedMs", 0)
                .putString("lastLog", "连点任务已启动")
                .apply();
        TapAccessibilityService.startOrRefresh();
        refreshStatus();
    }

    private void stopTap() {
        TapAccessibilityService.stop();
        SharedPreferences p = getSharedPreferences(PREFS, MODE_PRIVATE);
        long elapsed = Math.max(0, System.currentTimeMillis() - p.getLong("startTime", System.currentTimeMillis()));
        p.edit().putBoolean("running", false).putString("status", "已停止")
                .putLong("elapsedMs", elapsed).putString("lastLog", "已手动停止").apply();
        refreshStatus();
    }

    private void openPositionOverlay() {
        if (TapAccessibilityService.isRunning()) {
            TapAccessibilityService.stop();
            getSharedPreferences(PREFS, MODE_PRIVATE).edit()
                    .putBoolean("overlay_restore", true).apply();
            Toast.makeText(this, "定位期间自动连点已暂停，完成后自动恢复", Toast.LENGTH_SHORT).show();
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
            Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:" + getPackageName()));
            startActivityForResult(intent, REQ_OVERLAY);
            Toast.makeText(this, "请允许显示在其他应用上层", Toast.LENGTH_LONG).show();
            return;
        }
        startService(new Intent(this, TapOverlayService.class));
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQ_OVERLAY) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && Settings.canDrawOverlays(this)) {
                startService(new Intent(this, TapOverlayService.class));
            } else {
                Toast.makeText(this, "需要悬浮窗权限才能定位", Toast.LENGTH_LONG).show();
            }
        } else if (requestCode == REQ_QUICK) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && Settings.canDrawOverlays(this)) {
                startQuickControl();
            } else {
                quickSwitch.setChecked(false);
                Toast.makeText(this, "未开启悬浮窗权限，无法使用悬浮控制球", Toast.LENGTH_LONG).show();
            }
        }
    }

    private void openAccessibilitySettings() {
        startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS));
    }


    private void toggleQuickControl(boolean enabled) {
        if (enabled) {
            startQuickControl();
        } else {
            stopQuickControl();
        }
    }

    private void startQuickControl() {
        if (QuickControlService.isActive()) {
            quickSwitch.setChecked(true);
            return;
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
            quickSwitch.setChecked(false);
            Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:" + getPackageName()));
            startActivityForResult(intent, REQ_QUICK);
            Toast.makeText(this, "请允许显示在其他应用上层后才能开启悬浮控制球", Toast.LENGTH_LONG).show();
            return;
        }
        Intent i = new Intent(this, QuickControlService.class);
        if (Build.VERSION.SDK_INT >= 26) {
            startForegroundService(i);
        } else {
            startService(i);
        }
        quickSwitch.setChecked(true);
    }

    private void stopQuickControl() {
        stopService(new Intent(this, QuickControlService.class));
        quickSwitch.setChecked(false);
    }
    private void refreshStatus() {
        SharedPreferences p = getSharedPreferences(PREFS, MODE_PRIVATE);
        if (p.getBoolean("overlay_restore", false) && !TapOverlayService.isActive()) {
            p.edit().putBoolean("overlay_restore", false).apply();
            if (TapAccessibilityService.startOrRefresh()) {
                Toast.makeText(this, "已恢复连点", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "无障碍服务未连接，请重新开启后点开始", Toast.LENGTH_SHORT).show();
            }
        }
        boolean connected = TapAccessibilityService.isConnected();
        boolean running = p.getBoolean("running", false);
        long startTime = p.getLong("startTime", 0);
        long elapsed = p.getLong("elapsedMs", 0);
        if (running && startTime > 0) elapsed = System.currentTimeMillis() - startTime;
        tvA11y.setText(connected ? "已开启" : "未开启");
        tvA11y.setTextColor(connected ? 0xFF37E6B8 : 0xFFFFB454);
        a11yHint.setText(connected ? "无障碍已开启，可以开始连点。" : "无障碍未开启，先前往系统设置完成授权。");
        tvRun.setText(running ? "运行中" : "待机");
        tvRun.setTextColor(running ? 0xFF37E6B8 : 0xFF9AA7BD);
        int currentClicks = Math.max(0, p.getInt("current", 0));
        tvClicks.setText(String.valueOf(currentClicks));
        tvElapsed.setText(formatElapsed(elapsed));
        tvToday.setText(String.valueOf(Math.max(0, p.getInt("today", 0))));
        tvAll.setText(String.valueOf(Math.max(0, p.getInt("allTime", 0))));
        double seconds = elapsed / 1000.0;
        long rate = seconds >= 1.0 ? Math.round(currentClicks / seconds) : 0;
        tvRate.setText(rate + " 次/秒");
        tvStatus.setText(p.getString("status", "就绪"));
        String log = p.getString("lastLog", "");
        if (!log.isEmpty()) {
            tvLog.setText(log);
            tvLog.setTypeface(Typeface.MONOSPACE);
        }
        startBtn.setEnabled(!running && connected && positions.length() > 0);
        stopBtn.setEnabled(running);
    }

    private String formatElapsed(long ms) {
        long totalSec = Math.max(0, ms / 1000);
        return String.format(java.util.Locale.US, "%02d:%02d", totalSec / 60, totalSec % 60);
    }

    private long parseLong(EditText view, long def) {
        try {
            return Long.parseLong(view.getText().toString().trim());
        } catch (Exception ignored) {
            return def;
        }
    }

    private int parseInt(EditText view, int def) {
        try {
            return Integer.parseInt(view.getText().toString().trim());
        } catch (Exception ignored) {
            return def;
        }
    }

    private int dp(int value) {
        return Math.round(getResources().getDisplayMetrics().density * value);
    }
}