package com.example.autotapper;

import android.app.Activity;
import android.content.SharedPreferences;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.InputStream;

public class TemplateActivity extends Activity {
    private static final String PREFS = "tap_prefs";
    private static final int[][][] PRESETS = {
            {{50, 50}},
            {{35, 58}, {65, 58}},
            {{50, 35}, {30, 60}, {70, 60}},
            {{50, 30}, {25, 55}, {75, 55}, {50, 80}, {50, 55}},
            {{42, 60}, {58, 72}, {50, 84}},
            {{30, 40}, {70, 40}, {30, 65}, {70, 65}},
            {{50, 30}, {20, 55}, {80, 55}, {50, 80}, {35, 42}, {65, 42}},
            {{50, 55}, {25, 42}, {75, 42}, {25, 72}, {75, 72}, {50, 30}}
    };
    private static final String[] NAMES = {
            "单点连点", "中心连锁", "三角循环", "多点连击",
            "签到点赞", "日常任务", "挂机巡逻", "全屏环绕"
    };
    private static final String[] DESCS = {
            "1 个圆点 · 高频连续点击，适合长按体力、任务按钮或单点刷券。",
            "2 个圆点 · 中心两点互换，适合两点轮流触发、交替领取的玩法。",
            "3 个圆点 · 三角循环走位，适合三点轮转的技能、道具与抽卡连点。",
            "5 个圆点 · 多点高速轮点，适合连击、批量技能与多点快速点击。",
            "3 个圆点 · 签到与领奖串联，适合每天固定路线的一键签到。",
            "4 个圆点 · 日常入口轮巡，适合每天重复执行的批量任务。",
            "6 个圆点 · 刷新、确认、开始循环巡逻，适合挂机与自动助手。",
            "6 个圆点 · 环绕屏幕九宫位，适合多开窗口与全屏批量操作。"
    };
    private static final String[] FILES = {
            "template01.jpg", "template02.jpg", "template03.jpg", "template04.jpg",
            "template05.jpg", "template06.jpg", "template07.jpg", "template08.jpg"
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        buildUi();
    }

    private void buildUi() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(0xFF0A0E1A);
        root.setPadding(dp(14), dp(12), dp(14), dp(14));

        TextView header = new TextView(this);
        header.setText("模板图鉴 · 一键载入");
        header.setTextColor(0xFFF2F6FF);
        header.setTextSize(22);
        header.setTypeface(null, Typeface.BOLD);
        root.addView(header, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));

        TextView sub = new TextView(this);
        sub.setText("选择玩法模板，载入后回到主页微调圆点即可使用。");
        sub.setTextColor(0xFF8FA3BD);
        sub.setTextSize(13);
        LinearLayout.LayoutParams subLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        subLp.topMargin = dp(6);
        root.addView(sub, subLp);

        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(false);
        LinearLayout list = new LinearLayout(this);
        list.setOrientation(LinearLayout.VERTICAL);
        list.setPadding(dp(4), dp(12), dp(4), dp(8));
        for (int i = 0; i < NAMES.length; i++) {
            list.addView(buildCard(i));
        }
        scroll.addView(list, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        root.addView(scroll, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1));
        setContentView(root);
    }

    private LinearLayout buildCard(final int index) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(12), dp(12), dp(12), dp(14));
        card.setBackgroundResource(R.drawable.bg_card);
        LinearLayout.LayoutParams cardLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        cardLp.bottomMargin = dp(14);
        card.setLayoutParams(cardLp);

        ImageView image = new ImageView(this);
        image.setAdjustViewBounds(true);
        image.setImageBitmap(loadImage(FILES[index]));
        card.addView(image, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));

        TextView title = new TextView(this);
        title.setText(NAMES[index]);
        title.setTextColor(0xFFF2F6FF);
        title.setTextSize(19);
        title.setTypeface(null, Typeface.BOLD);
        LinearLayout.LayoutParams titleLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        titleLp.topMargin = dp(10);
        card.addView(title, titleLp);

        TextView desc = new TextView(this);
        desc.setText(DESCS[index]);
        desc.setTextColor(0xFFB8C6DA);
        desc.setTextSize(14);
        desc.setLineSpacing(dp(2), 1.08f);
        LinearLayout.LayoutParams descLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        descLp.topMargin = dp(6);
        card.addView(desc, descLp);

        Button use = new Button(this);
        use.setText("使用此模板");
        use.setTextColor(0xFF08140F);
        use.setTextSize(15);
        use.setTypeface(null, Typeface.BOLD);
        use.setBackgroundResource(R.drawable.bg_primary);
        use.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                applyTemplate(index);
            }
        });
        LinearLayout.LayoutParams useLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(50));
        useLp.topMargin = dp(12);
        card.addView(use, useLp);
        return card;
    }

    private void applyTemplate(int index) {
        try {
            if (index < 0 || index >= PRESETS.length) return;
            int w = Math.max(1, getResources().getDisplayMetrics().widthPixels);
            int h = Math.max(1, getResources().getDisplayMetrics().heightPixels);
            JSONArray arr = new JSONArray();
            for (int[] pct : PRESETS[index]) {
                JSONObject o = new JSONObject();
                o.put("x", w * pct[0] / 100);
                o.put("y", h * pct[1] / 100);
                arr.put(o);
            }
            SharedPreferences.Editor e = getSharedPreferences(PREFS, MODE_PRIVATE).edit();
            e.putString("positions", arr.toString()).apply();
            Toast.makeText(this, "已载入 " + arr.length() + " 个圆点，回主页可微调", Toast.LENGTH_LONG).show();
            finish();
        } catch (Exception ignored) {
        }
    }

    private Bitmap loadImage(String file) {
        try {
            AssetManager am = getAssets();
            BitmapFactory.Options bounds = new BitmapFactory.Options();
            bounds.inJustDecodeBounds = true;
            InputStream in = am.open("templates/" + file);
            BitmapFactory.decodeStream(in, null, bounds);
            in.close();
            int targetW = Math.max(1, getResources().getDisplayMetrics().widthPixels);
            int targetH = Math.max(1, getResources().getDisplayMetrics().heightPixels);
            int sample = 1;
            while (bounds.outWidth / (sample * 2) >= targetW
                    && bounds.outHeight / (sample * 2) >= targetH) {
                sample *= 2;
            }
            BitmapFactory.Options opts = new BitmapFactory.Options();
            opts.inSampleSize = sample;
            opts.inPreferredConfig = Bitmap.Config.RGB_565;
            InputStream in2 = am.open("templates/" + file);
            Bitmap bmp = BitmapFactory.decodeStream(in2, null, opts);
            in2.close();
            return bmp != null ? bmp : Bitmap.createBitmap(1, 1, Bitmap.Config.RGB_565);
        } catch (Exception ignored) {
            return Bitmap.createBitmap(1, 1, Bitmap.Config.RGB_565);
        }
    }

    private int dp(int value) {
        return Math.round(getResources().getDisplayMetrics().density * value);
    }
}
