# -*- coding: utf-8 -*-
import sys
from pathlib import Path
sys.path.insert(0, str(Path(__file__).parent))
import store_common as C
from PIL import Image, ImageDraw, ImageFont

S = C.S
W, H = 360, 640
FONT = C.font

def base():
    img = C.vgrad((W*S, H*S), (16, 20, 38), (7, 10, 19))
    d = ImageDraw.Draw(img)
    return img, d

def dt(d, x, y, s, sp, fill=C.TEXT, anchor=None, stroke=0):
    C.dtext(d, (x, y), s, sp, fill=fill, anchor=anchor, stroke=stroke)

def dp(v):
    return int(round(v * S))

def rrect(d, rect, r=18, fill=C.CARD, outline=C.BORDER, width=1):
    C.rrect(d, rect, r, fill=fill, outline=outline, width=width)

def poly(d, pts, fill):
    d.polygon([(int(x*S), int(y*S)) for x,y in pts], fill=fill)

def pill(d, x, y, w, h, text, sp, bg, fg, r=None):
    C.pill(d, (x, y), w, h, text, sp, bg, fg, radius=r)

def btn(d, rect, text, sp, bg, fg, r=14):
    C.button(d, rect, text, sp, bg, fg, radius=r)

def paste_cover(img, path, x1, y1, x2, y2):
    bi = Image.open(path).convert('RGB')
    tw = (x2 - x1) * S
    th = (y2 - y1) * S
    scale = max(tw / bi.width, th / bi.height)
    nw = int(bi.width * scale)
    nh = int(bi.height * scale)
    bi = bi.resize((nw, nh), Image.LANCZOS)
    left = (nw - tw) // 2
    top = (nh - th) // 2
    img.paste(bi.crop((left, top, left+tw, top+th)), (x1*S, y1*S))

def scene_main():
    img, d = base()
    C.status_bar(d)
    # Header
    rrect(d, (18, 42, 342, 116), 20, fill=C.CARD, outline=C.BORDER)
    icon = C.icon_image(44*S)
    img.paste(icon, (28*S, 62*S))
    dt(d, 86, 68, '极速连点', 24, fill=C.TEXT)
    dt(d, 86, 100, '多位置顺序点击 · 长按 · 滑动 · 多点同时', 11, fill=C.MUTED)
    pill(d, 286, 50, 44, 22, 'v4.2', 11, C.ACCENT, (8, 16, 15))
    # Switch card
    rrect(d, (18, 128, 342, 204), 18, fill=C.CARD, outline=C.BORDER)
    dt(d, 32, 144, '悬浮控制球', 16, fill=C.TEXT)
    C.dtext_multi(d, (32, 170), '在其他应用里点开上滑开始、下滑停止，左侧可退出连点器。', 11, 278, fill=C.MUTED)
    rrect(d, (286, 148, 322, 174), 13, fill=C.ACCENT)
    dt(d, 304, 157, '开', 13, fill=(8, 20, 20), anchor='mm')
    # Stats 2x2
    stats = [
        ('无障碍', '已开启', C.ACCENT),
        ('运行状态', '连点中', C.ACCENT),
        ('已点击', '128', C.TEXT),
        ('已用时间', '00:32', C.INFO),
        ('今日点击', '1,286', C.ACCENT),
        ('累计点击', '82,406', C.INFO),
        ('点击速率', '7 次/秒', C.WARN),
    ]
    positions = [(18, 210, 170, 270), (190, 210, 342, 270),
                 (18, 278, 170, 338), (190, 278, 342, 338),
                 (18, 346, 170, 406), (190, 346, 342, 406),
                 (18, 414, 170, 474)]
    for i, (label, value, color) in enumerate(stats):
        x1, y1, x2, y2 = positions[i]
        rrect(d, (x1, y1, x2, y2), 14, fill=C.ROW, outline=C.BORDER)
        dt(d, x1+14, y1+12, label, 10, fill=C.MUTED)
        dt(d, x1+14, y1+30, value, 16, fill=color)
    # Position card
    y0 = 486
    rrect(d, (18, y0, 342, y0+136), 18, fill=C.CARD, outline=C.BORDER)
    dt(d, 32, y0+14, '点击位置', 16, fill=C.TEXT)
    pill(d, 286, y0+14, 44, 22, '6 个', 11, C.ACCENT, (8, 16, 15))
    dot_colors = [(255,107,129),(100,169,255),(55,230,184),(255,180,84),(176,124,255),(255,154,196)]
    for i, col in enumerate(dot_colors):
        cx = 46 + i * 50
        C.dot(d, cx, y0+58, 14, col, number=i+1, ring_color=(255,255,255))
    dt(d, 32, y0+82, '6 个彩色圆点按编号顺序自动点击，支持随机与单点模式。', 11, fill=C.MUTED)
    btn(d, (32, y0+104, 162, y0+132), '设置圆点', 12, C.WARN, (24, 15, 3))
    btn(d, (180, y0+104, 310, y0+132), '快捷模板', 12, C.ROW, C.TEXT)
    # Start card
    y1 = 612
    rrect(d, (18, y1, 342, 640), 18, fill=C.CARD, outline=C.BORDER)
    dt(d, 32, y1+12, '就绪', 14, fill=C.ACCENT)
    btn(d, (150, y1+8, 240, y1+36), '开始连点', 13, C.ACCENT, (8, 16, 15))
    btn(d, (248, y1+8, 330, y1+36), '停止', 13, C.DANGER, (255,255,255))
    return img

def scene_dots():
    img, d = base()
    C.status_bar(d)
    # Underlying app mock
    rrect(d, (0, 36, 360, 118), 0, fill=(18, 26, 58))
    dt(d, 18, 62, '‹ 返回', 18, fill=C.TEXT)
    dt(d, 180, 62, '任务大厅', 18, fill=C.TEXT, anchor='mm')
    dt(d, 180, 86, '每日签到 · 连续 12 天', 11, fill=C.MUTED, anchor='mm')
    # Task cards
    tasks = [('每日登录', '+10'), ('观看视频', '+20'), ('分享好友', '+15'), ('挑战副本', '+50')]
    cards = [(20, 140, 170, 320), (190, 140, 340, 320),
             (20, 340, 170, 520), (190, 340, 340, 520)]
    for (i, (label, reward)) in enumerate(tasks):
        x1, y1, x2, y2 = cards[i]
        rrect(d, (x1, y1, x2, y2), 18, fill=C.CARD2, outline=C.BORDER)
        dt(d, x1+14, y1+16, label, 15, fill=C.TEXT)
        dt(d, x1+14, y1+44, reward, 22, fill=C.ACCENT)
        btn(d, (x1+14, y1+80, x2-14, y1+110), '领取', 12, C.ACCENT, (8, 16, 15))
    # Bottom nav
    rrect(d, (0, 600, 360, 640), 0, fill=(10, 14, 30))
    # Dim overlay
    overlay = Image.new('RGBA', img.size, (5, 8, 16, 160))
    img = Image.alpha_composite(img.convert('RGBA'), overlay).convert('RGB')
    d = ImageDraw.Draw(img)
    # Top bar
    rrect(d, (18, 74, 342, 152), 18, fill=C.CARD, outline=C.ACCENT)
    dt(d, 32, 88, '圆点定位', 17, fill=C.TEXT)
    pill(d, 286, 88, 44, 22, '6 个', 11, C.ACCENT, (8, 16, 15))
    dt(d, 32, 118, '顺序：1→2→3→4→5→6 · 长按圆点可调整', 11, fill=C.MUTED)
    # Dots on tasks
    dot_positions = [(55, 260), (255, 270), (60, 430), (250, 430), (140, 530), (285, 530)]
    dot_colors = [(255,107,129),(100,169,255),(55,230,184),(255,180,84),(176,124,255),(255,154,196)]
    for i, ((cx, cy), col) in enumerate(zip(dot_positions, dot_colors)):
        C.dot(d, cx, cy, 16, col, number=i+1, ring_color=(255,255,255))
    # Bottom bar
    rrect(d, (18, 560, 342, 618), 18, fill=C.CARD, outline=C.ACCENT)
    dt(d, 32, 576, '已定位 6 个圆点，顺序 1→2→3→4→5→6', 11, fill=C.MUTED)
    btn(d, (242, 566, 276, 596), '清空', 11, C.PURPLE, (255,255,255))
    btn(d, (280, 566, 318, 596), '取消', 11, C.ROW, C.TEXT)
    btn(d, (318, 566, 356, 596), '保存', 11, C.ACCENT, (8, 16, 15))
    return img

def scene_quick():
    img, d = base()
    C.status_bar(d)
    # Underlying background
    rrect(d, (0, 36, 360, 118), 0, fill=(18, 26, 58))
    dt(d, 180, 62, '游戏大厅', 18, fill=C.TEXT, anchor='mm')
    dt(d, 180, 86, '在线 3,248', 11, fill=C.MUTED, anchor='mm')
    cards2 = [(20, 140, 170, 320), (190, 140, 340, 320),
              (20, 340, 170, 520), (190, 340, 340, 520)]
    for (i, (x1, y1, x2, y2)) in enumerate(cards2):
        rrect(d, (x1, y1, x2, y2), 18, fill=C.CARD2, outline=C.BORDER)
        dt(d, x1+14, y1+16, f'副本{i+1}', 15, fill=C.TEXT)
    rrect(d, (0, 600, 360, 640), 0, fill=(10, 14, 30))
    # Floating panel
    rrect(d, (86, 110, 306, 540), 36, fill=(12, 27, 48), outline=(100, 180, 220))
    # Exit button
    rrect(d, (94, 302, 156, 348), 14, fill=C.DANGER)
    dt(d, 125, 325, '退出\n连点器', 10, fill=(255,255,255), anchor='mm')
    # Top slider vertical
    def glass_slider(d, x, y_top, y_bottom, up):
        cx = x + 32
        rrect(d, (x, y_top, x+64, y_bottom), 32, fill=(210, 240, 255), outline=(255,255,255), width=2)
        # Draw wave lines
        for yy in range(y_top+8, y_bottom-8, 4):
            wave = 3 + (yy * 0.1) % 6
            d.line([(dp(cx - wave), dp(yy)), (dp(cx + wave), dp(yy))], fill=(100, 160, 200, 60), width=dp(2))
        # Gloss
        rrect(d, (x+8, y_top+8, x+56, y_top+24), 8, fill=(255,255,255, 80))
        # Text
        first = '上滑' if up else '下滑'
        second = '开始' if up else '停止'
        dt(d, cx, (y_top+y_bottom)//2 - 14, first, 12, fill=(255,255,255), anchor='mm')
        dt(d, cx, (y_top+y_bottom)//2 + 14, second, 12, fill=(255,255,255), anchor='mm')
        # Thumb
        if up:
            ty = y_top + 28
        else:
            ty = y_bottom - 28
        C.dot(d, cx, ty, 10, (255,255,255), ring_color=(200,220,240))
    glass_slider(d, 152, 136, 280, up=True)
    glass_slider(d, 152, 344, 488, up=False)
    # Center ball
    ball_cx, ball_cy = 184, 326
    C.dot(d, ball_cx, ball_cy, 22, (55, 230, 184), ring_color=None)
    # Draw glow ring
    rrect(d, (ball_cx-26, ball_cy-26, ball_cx+26, ball_cy+26), 26, outline=(80, 200, 230), width=2)
    # Small bolt
    poly(d, [(ball_cx-2, ball_cy-16), (ball_cx-12, ball_cy+2), (ball_cx-4, ball_cy+2),
             (ball_cx-8, ball_cy+16), (ball_cx+12, ball_cy-2), (ball_cx+4, ball_cy-2)],
         fill=(255,255,255))
    # Status dot
    C.dot(d, ball_cx+14, ball_cy+14, 4, C.ACCENT, ring_color=(255,255,255))
    # Callout text
    dt(d, 125, 556, '上滑  开始  ·  下滑  停止', 11, fill=C.ACCENT, anchor='mm')
    return img

def scene_templates():
    img, d = base()
    C.status_bar(d)
    dt(d, 24, 54, '模板图鉴 · 一键载入', 22, fill=C.TEXT)
    dt(d, 24, 84, '选择玩法模板，载入后回到主页微调圆点即可使用。', 12, fill=C.MUTED)
    pill(d, 286, 56, 60, 22, '8 个模板', 11, C.ACCENT, (8, 16, 15))
    # First card
    y0 = 102
    rrect(d, (18, y0, 342, y0+310), 18, fill=C.CARD, outline=C.BORDER)
    path1 = C.ROOT / 'outputs/auto_tapper/app/src/main/assets/templates/template01.jpg'
    if path1.exists():
        paste_cover(img, path1, 30, y0+14, 330, y0+174)
    dt(d, 30, y0+186, '单点连点', 19, fill=C.TEXT)
    C.dtext_multi(d, (30, y0+218), '1 个圆点 · 高频连续点击，适合长按体力、任务按钮或单点刷券。', 13, 294, fill=C.MUTED)
    btn(d, (30, y0+270, 330, y0+306), '使用此模板', 15, C.ACCENT, (8, 16, 15))
    # Second card partial
    y1 = y0 + 320
    rrect(d, (18, y1, 342, y1+210), 18, fill=C.CARD, outline=C.BORDER)
    path2 = C.ROOT / 'outputs/auto_tapper/app/src/main/assets/templates/template02.jpg'
    if path2.exists():
        paste_cover(img, path2, 30, y1+14, 330, y1+174)
    dt(d, 30, y1+186, '中心连锁', 19, fill=C.TEXT)
    C.dtext_multi(d, (30, y1+218), '2 个圆点 · 中心两点互换，适合两点轮流触发、交替领取的玩法。', 13, 294, fill=C.MUTED)
    # Scroll hint
    dt(d, 180, 620, '⋮ 下滑查看更多', 12, fill=C.MUTED, anchor='mm')
    return img

def scene_profiles():
    img, d = base()
    C.status_bar(d)
    # Underlay main
    rrect(d, (18, 42, 342, 116), 20, fill=C.CARD)
    dt(d, 86, 68, '极速连点', 24, fill=C.TEXT)
    dt(d, 86, 100, '多位置顺序点击', 11, fill=C.MUTED)
    # Dim overlay
    overlay = Image.new('RGBA', img.size, (5, 8, 16, 160))
    img = Image.alpha_composite(img.convert('RGBA'), overlay).convert('RGB')
    d = ImageDraw.Draw(img)
    # Dialog
    rrect(d, (18, 90, 342, 570), 24, fill=(14, 28, 50), outline=C.BORDER, width=2)
    dt(d, 32, 110, '方案库', 20, fill=C.TEXT)
    dt(d, 32, 136, '一键恢复圆点与参数', 12, fill=C.MUTED)
    # Close icon
    C.dot(d, 322, 116, 10, C.DANGER, ring_color=None)
    # List
    profiles = [
        ('今日签到', '3 个圆点 · 150ms · 顺序循环'),
        ('抽卡十连', '3 个圆点 · 220ms · 随机循环'),
        ('挂机巡逻', '6 个圆点 · 180ms · 顺序循环'),
        ('多开任务', '4 个圆点 · 300ms · 多点同时'),
    ]
    for i, (name, desc) in enumerate(profiles):
        y = 158 + i * 88
        rrect(d, (32, y, 328, y+76), 14, fill=C.ROW, outline=C.INFO)
        dt(d, 46, y+12, name, 15, fill=C.TEXT)
        dt(d, 46, y+36, desc, 11, fill=C.MUTED)
        btn(d, (252, y+16, 314, y+48), '载入', 12, C.ACCENT, (8, 16, 15))
    # Delete all
    btn(d, (32, 512, 328, 550), '删除全部方案', 14, C.DANGER, (255,255,255))
    # Cancel
    btn(d, (32, 558, 328, 594), '取消', 14, C.ROW, C.TEXT)
    return img

def scene_tutorial():
    img, d = base()
    C.status_bar(d)
    pill(d, 24, 50, 120, 24, '图文教程 · 1/20', 12, C.ACCENT, (8, 16, 15))
    dt(d, 24, 82, '快速上手', 24, fill=C.TEXT)
    dt(d, 24, 112, '5 分钟开始自动连点', 13, fill=C.MUTED)
    # Image area
    step_path = C.ROOT / 'outputs/auto_tapper/app/src/main/assets/tutorial/step01.jpg'
    if step_path.exists():
        paste_cover(img, step_path, 18, 140, 342, 430)
    # Body card
    rrect(d, (18, 446, 342, 546), 18, fill=C.CARD, outline=C.BORDER)
    C.dtext_multi(d, (32, 462), '安装后先完成无障碍与悬浮窗授权，再添加圆点，5 分钟即可开始自动连点。\n\n多圆点会按编号依次点击，支持顺序、随机、单点三种模式。', 14, 296, fill=C.MUTED)
    # Buttons
    rrect(d, (18, 564, 170, 604), 14, fill=C.ROW)
    dt(d, 94, 584, '上一页', 14, fill=C.MUTED, anchor='mm')
    btn(d, (190, 564, 342, 604), '下一页', 15, C.ACCENT, (8, 16, 15))
    return img

def save(img, name):
    out = C.OUT / f'{name}.png'
    out_720 = C.OUT / f'{name}_720.png'
    img.save(out)
    img.resize((720, 1280), Image.LANCZOS).save(out_720)
    print(f'  {out.name}  {img.size} -> {out_720.name} 720x1280')

if __name__ == '__main__':
    C.OUT.mkdir(parents=True, exist_ok=True)
    print('Generating screenshots...')
    scenes = [
        ('screen_01_main', scene_main),
        ('screen_02_dot_position', scene_dots),
        ('screen_03_quick_control', scene_quick),
        ('screen_04_templates', scene_templates),
        ('screen_05_profiles', scene_profiles),
        ('screen_06_tutorial', scene_tutorial),
    ]
    for name, func in scenes:
        img = func()
        save(img, name)
    print('Done.')