package com.example.autotapper;

import android.app.Activity;
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

import java.io.InputStream;

public class HelpActivity extends Activity {
    private static final String[] TITLES = {
            "快速上手",
            "开启无障碍",
            "允许悬浮窗",
            "添加多个圆点",
            "调整点击顺序",
            "设置点击参数",
            "手势与高级玩法",
            "开始与停止",
            "玩法灵感 · 抽卡",
            "玩法灵感 · 签到",
            "玩法灵感 · 连击",
            "玩法灵感 · 日常",
            "玩法灵感 · 挂机",
            "玩法灵感 · 多开",
            "像素级微调",
            "方案保存与复用",
            "多点同时",
            "统计中心",
            "常见问题",
            "兼容性提示"
    };
    private static final String[] BODIES = {
            "安装后先完成无障碍与悬浮窗授权，再添加圆点，5 分钟即可开始自动连点。\n\n多圆点会按编号依次点击，支持顺序、随机、单点三种模式。",
            "进入系统「设置 → 无障碍」，找到「极速连点器」并打开服务。开启后，App 才有权限在其他应用里模拟真实手指点击。",
            "点击「设置 / 添加圆点」时允许悬浮窗权限。这样彩色圆点可以显示在其他应用上层，直接拖到目标按钮上。",
            "定位模式下点「＋」添加圆点，把圆点拖到目标位置。想要几个点就加几个点，每个点会自动编号。",
            "第 1 个圆点会先被点击，后面依次执行。长按圆点可前移、后移或删除；拖到屏幕左右边缘也能快速换位。",
            "间隔越小点击越快，随机抖动会让节奏更接近真人；次数填 0 表示无限循环。建议先从 150ms 间隔开始测试。",
            "普通点击用于点按目标，长按适合批量选择，滑动适合翻页或连击任务。配合顺序 / 随机模式可组合出多种玩法。",
            "打开目标 App 后回到主页面点「开始连点」，运行中可在通知栏查看状态，随时点「停止」结束任务。",
            "把圆点对准「开始」「十连」「确认」按钮，用顺序模式循环，即可自动完成一轮抽卡操作。",
            "用 2 个圆点对准「立即签到」和「领取奖励」，固定间隔，每天一键完成签到。",
            "给多个技能按钮各放一个圆点，按编号循环点击；开启随机抖动会更接近真人。",
            "把每块田 / 工坊的入口都放一个圆点，保存后每天按同一顺序批量执行。",
            "刷新、确认、开始三个按钮各放一个圆点，总数填 0 即可无限循环挂机。",
            "多开窗口排成一排时，给每个入口放一个圆点，顺序点击即可批量操作。",
            "长按圆点打开微调面板，方向键每次移动 10px，长按方向键每次移动 1px。先拖动大概位置，再用微调做像素级对齐，不依赖 x/y 坐标。",
            "在主页把圆点、间隔、次数和手势保存为方案；下次打开方案库一键恢复。不同玩法可各存一套，切换时无需重新定位。",
            "手势选择「多点同时」，一批圆点会在同一瞬间同时按下，适合多个入口同时触发、批量领取与多人同时操作。",
            "主页实时显示本次点击、今日点击、累计点击和点击速率。运行记录保留最近一次状态，通知栏也有一键停止按钮。",
            "连点前先确认无障碍与悬浮窗都已开启；部分国产系统需要在电池优化里允许后台运行。若手势被系统取消，请把间隔调大一些。",
            "支持 Android 7.0 及以上。若遇到安装失败，请检查是否允许安装未知来源，并卸载旧版本后重装本 APK。"
    };
    private static final String[] FILES = {
            "step01.jpg", "step02.jpg", "step03.jpg", "step04.jpg",
            "step05.jpg", "step06.jpg", "step07.jpg", "step08.jpg", "step09.jpg", "step10.jpg", "step11.jpg", "step12.jpg", "step13.jpg", "step14.jpg",
            "step15.jpg", "step16.jpg", "step17.jpg", "step18.jpg", "step19.jpg", "step20.jpg"
    };
    private int index = 0;
    private TextView stepView;
    private TextView titleView;
    private TextView bodyView;
    private ImageView imageView;
    private Button prevBtn;
    private Button nextBtn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        buildUi();
        update();
    }

    private void buildUi() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(0xFF0A0E1A);
        root.setPadding(dp(14), dp(12), dp(14), dp(14));

        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(4), dp(4), dp(4), dp(8));

        stepView = new TextView(this);
        stepView.setTextColor(0xFF55D7A8);
        stepView.setTextSize(15);
        stepView.setTypeface(null, Typeface.BOLD);

        titleView = new TextView(this);
        titleView.setTextColor(0xFFF2F6FF);
        titleView.setTextSize(24);
        titleView.setTypeface(null, Typeface.BOLD);
        LinearLayout.LayoutParams titleLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        titleLp.topMargin = dp(10);
        content.addView(stepView);
        content.addView(titleView, titleLp);

        imageView = new ImageView(this);
        imageView.setAdjustViewBounds(true);
        LinearLayout.LayoutParams imageLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        imageLp.topMargin = dp(12);
        content.addView(imageView, imageLp);

        bodyView = new TextView(this);
        bodyView.setTextColor(0xFFB8C6DA);
        bodyView.setTextSize(15);
        bodyView.setLineSpacing(dp(2), 1.08f);
        LinearLayout.LayoutParams bodyLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        bodyLp.topMargin = dp(12);
        content.addView(bodyView, bodyLp);

        scroll.addView(content, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        root.addView(scroll, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1));

        LinearLayout bottom = new LinearLayout(this);
        bottom.setOrientation(LinearLayout.HORIZONTAL);
        bottom.setGravity(Gravity.CENTER_VERTICAL);
        bottom.setPadding(0, dp(10), 0, 0);

        prevBtn = new Button(this);
        prevBtn.setText("上一页");
        prevBtn.setTextColor(0xFFF2F6FF);
        prevBtn.setTextSize(14);
        prevBtn.setTypeface(null, Typeface.BOLD);
        prevBtn.setBackgroundResource(R.drawable.bg_neutral);
        prevBtn.setOnClickListener(v -> {
            if (index > 0) {
                index--;
                update();
            }
        });
        bottom.addView(prevBtn, new LinearLayout.LayoutParams(0, dp(48), 1));

        nextBtn = new Button(this);
        nextBtn.setText("下一页");
        nextBtn.setTextColor(0xFF08140F);
        nextBtn.setTextSize(15);
        nextBtn.setTypeface(null, Typeface.BOLD);
        nextBtn.setBackgroundResource(R.drawable.bg_primary);
        LinearLayout.LayoutParams nextLp = new LinearLayout.LayoutParams(0, dp(48), 1);
        nextLp.leftMargin = dp(10);
        nextBtn.setOnClickListener(v -> {
            if (index >= TITLES.length - 1) {
                finish();
            } else {
                index++;
                update();
            }
        });
        bottom.addView(nextBtn, nextLp);

        root.addView(bottom, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        setContentView(root);
    }

    private void update() {
        stepView.setText("图文教程  ·  " + (index + 1) + " / " + TITLES.length);
        titleView.setText(TITLES[index]);
        bodyView.setText(BODIES[index]);
        imageView.setImageBitmap(loadImage(FILES[index]));
        prevBtn.setEnabled(index > 0);
        nextBtn.setText(index >= TITLES.length - 1 ? "完成" : "下一页");
    }

    private Bitmap loadImage(String file) {
        try {
            AssetManager am = getAssets();
            BitmapFactory.Options bounds = new BitmapFactory.Options();
            bounds.inJustDecodeBounds = true;
            InputStream in = am.open("tutorial/" + file);
            BitmapFactory.decodeStream(in, null, bounds);
            in.close();

            int targetW = Math.max(1, getResources().getDisplayMetrics().widthPixels);
            int targetH = Math.max(1, getResources().getDisplayMetrics().heightPixels / 2);
            int sample = 1;
            while (bounds.outWidth / (sample * 2) >= targetW
                    && bounds.outHeight / (sample * 2) >= targetH) {
                sample *= 2;
            }
            BitmapFactory.Options opts = new BitmapFactory.Options();
            opts.inSampleSize = sample;
            opts.inPreferredConfig = Bitmap.Config.RGB_565;
            InputStream in2 = am.open("tutorial/" + file);
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