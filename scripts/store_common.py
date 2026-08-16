from pathlib import Path
from PIL import Image, ImageDraw, ImageFont

HOME = Path.home()
ROOT = HOME / 'Documents/Codex/2026-08-15/z'
OUT = ROOT / '上架资料' / '应用图标与截图'
FONT_PATH = ROOT / 'outputs/auto_tapper/app/src/main/assets/fonts/NotoSansCJKsc-Regular.otf'
S = 3

BG = (10, 14, 26)
CARD = (18, 31, 53)
CARD2 = (13, 24, 40)
ROW = (13, 23, 39)
BORDER = (44, 64, 94)
TEXT = (238, 246, 255)
MUTED = (145, 161, 188)
ACCENT = (55, 230, 184)
WARN = (255, 180, 84)
DANGER = (255, 107, 129)
INFO = (100, 169, 255)
PURPLE = (138, 85, 232)
DOT_COLORS = [
    (255, 107, 129),
    (100, 169, 255),
    (55, 230, 184),
    (255, 180, 84),
    (176, 124, 255),
    (255, 154, 196),
    (62, 230, 161),
    (106, 197, 255),
]

_CACHE = {}


def font(px):
    if px not in _CACHE:
        _CACHE[px] = ImageFont.truetype(str(FONT_PATH), px)
    return _CACHE[px]


def dp(value):
    return int(round(value * S))


def box(rect):
    return (dp(rect[0]), dp(rect[1]), dp(rect[2]), dp(rect[3]))


def vgrad(size, top, bottom):
    width, height = size
    base = Image.new('RGB', (1, height))
    for y in range(height):
        k = y / max(1, height - 1)
        base.putpixel((0, y), tuple(int(top[i] + (bottom[i] - top[i]) * k) for i in range(3)))
    return base.resize((width, height))


def rrect(d, rect, r, fill=None, outline=None, width=1):
    d.rounded_rectangle(box(rect), radius=dp(r), fill=fill, outline=outline, width=width)


def card(d, rect, r=18, fill=CARD, outline=BORDER):
    rrect(d, rect, r, fill=fill, outline=outline, width=1)


def text_w(d, text, sp):
    return d.textlength(text, font=font(sp * S)) / S


def wrap(d, text, sp, max_width):
    lines = []
    current = ''
    for ch in text:
        if ch == '\n':
            lines.append(current)
            current = ''
            continue
        test = current + ch
        if d.textlength(test, font=font(sp * S)) <= max_width * S:
            current = test
        else:
            lines.append(current)
            current = ch
    if current:
        lines.append(current)
    return lines


def dtext(d, xy, text, sp, fill=TEXT, stroke=0, stroke_fill=None, anchor=None):
    d.text((dp(xy[0]), dp(xy[1])), text, font=font(sp * S), fill=fill,
           stroke_width=stroke, stroke_fill=stroke_fill, anchor=anchor)


def dtext_multi(d, xy, text, sp, max_width, fill=TEXT, gap=4):
    x = dp(xy[0])
    y = dp(xy[1])
    fnt = font(sp * S)
    for line in wrap(d, text, sp, max_width):
        d.text((x, y), line, font=fnt, fill=fill)
        y += fnt.size + dp(gap)


def pill(d, xy, width, height, text, sp, bg, fg, radius=None):
    r = height // 2 if radius is None else radius
    rrect(d, (xy[0], xy[1], xy[0] + width, xy[1] + height), r, fill=bg)
    d.text((dp(xy[0] + width / 2), dp(xy[1] + height / 2)), text,
           font=font(sp * S), fill=fg, anchor='mm')


def button(d, rect, text, sp, bg, fg, radius=14, stroke=0):
    rrect(d, rect, radius, fill=bg)
    if stroke:
        rrect(d, rect, radius, outline=fg, width=stroke)
    cx = (rect[0] + rect[2]) / 2
    cy = (rect[1] + rect[3]) / 2
    d.text((dp(cx), dp(cy)), text, font=font(sp * S), fill=fg, anchor='mm')


def status_bar(d):
    dtext(d, (30, 13), '9:41', 15, fill=TEXT)
    for i, hh in enumerate((9, 15, 21, 27)):
        x0 = 852 + i * 18
        rrect(d, (x0, 27 - hh, x0 + 11, 27), 2, fill=TEXT)
    rrect(d, (942, 16, 982, 27), 3, outline=TEXT, width=2)
    rrect(d, (988, 19, 994, 24), 2, fill=TEXT)
    rrect(d, (947, 19, 972, 24), 2, fill=ACCENT)


def dot(d, cx, cy, r, color, number=None, ring_color=(255, 255, 255)):
    rrect(d, (cx - r, cy - r, cx + r, cy + r), r, fill=color)
    if ring_color:
        rrect(d, (cx - r, cy - r, cx + r, cy + r), r, outline=ring_color, width=2)
    if number is not None:
        d.text((dp(cx), dp(cy)), str(number), font=font(int(r * 0.62 * S)),
               fill=(255, 255, 255), anchor='mm')


def icon_image(size):
    big = size * 4
    scale = big / 108.0
    img = Image.new('RGB', (big, big), '#0D1B2E')
    d = ImageDraw.Draw(img)
    d.polygon([
        (58 * scale, 10 * scale),
        (30 * scale, 58 * scale),
        (50 * scale, 58 * scale),
        (43 * scale, 96 * scale),
        (80 * scale, 45 * scale),
        (59 * scale, 45 * scale),
    ], fill='#22D3A6')
    circles = [
        ((51, 46), 7, '#FFFFFF'),
        ((67, 44), 7, '#FFC45E'),
        ((45, 66), 7, '#64A9FF'),
        ((65, 64), 7, '#FF6B81'),
    ]
    for (cx, cy), r, color in circles:
        d.ellipse([(cx - r) * scale, (cy - r) * scale,
                   (cx + r) * scale, (cy + r) * scale], fill=color)
    return img.resize((size, size), Image.LANCZOS)
