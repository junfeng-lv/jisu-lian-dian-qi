Add-Type -AssemblyName System.Drawing

$dir = 'C:\Users\吕俊锋\Documents\Codex\2026-08-15\z\outputs\auto_tapper\app\src\main\assets\tutorial'
New-Item -ItemType Directory -Force $dir | Out-Null

function Get-JpegCodec {
  [System.Drawing.Imaging.ImageCodecInfo[]]$codecs = [System.Drawing.Imaging.ImageCodecInfo]::GetImageEncoders()
  foreach ($c in $codecs) { if ($c.MimeType -eq 'image/jpeg') { return $c } }
  return $null
}

function New-Slide {
  param(
    [int]$num,
    [string]$title,
    [string]$body,
    [string]$tag,
    [int]$r = 55,
    [int]$g = 180,
    [int]$b = 255
  )
  $w = 1080
  $h = 1920
  $bmp = New-Object System.Drawing.Bitmap($w, $h, [System.Drawing.Imaging.PixelFormat]::Format24bppRgb)
  $g = [System.Drawing.Graphics]::FromImage($bmp)
  $g.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::AntiAlias
  $g.TextRenderingHint = [System.Drawing.Text.TextRenderingHint]::AntiAliasGridFit

  $rect = New-Object System.Drawing.Rectangle(0, 0, $w, $h)
  $bg = New-Object System.Drawing.Drawing2D.LinearGradientBrush($rect, [System.Drawing.Color]::FromArgb(255,10,18,32), [System.Drawing.Color]::FromArgb(255,18,32,58), 15)
  $g.FillRectangle($bg, $rect)

  $glow = New-Object System.Drawing.SolidBrush([System.Drawing.Color]::FromArgb(30, $r, $g, $b))
  $g.FillEllipse($glow, -150, -80, 620, 620)
  $g.FillEllipse($glow, 620, 1240, 760, 760)

  $penSoft = New-Object System.Drawing.Pen([System.Drawing.Color]::FromArgb(26,255,255,255), 2)
  for ($i = 0; $i -lt 8; $i++) {
    $y = 420 + ($i * 180)
    $g.DrawLine($penSoft, 120, $y, 960, $y)
  }

  $titleFont = New-Object System.Drawing.Font('Microsoft YaHei UI', 52, [System.Drawing.FontStyle]::Bold, [System.Drawing.GraphicsUnit]::Pixel)
  $bodyFont = New-Object System.Drawing.Font('Microsoft YaHei UI', 30, [System.Drawing.FontStyle]::Regular, [System.Drawing.GraphicsUnit]::Pixel)
  $tagFont = New-Object System.Drawing.Font('Microsoft YaHei UI', 24, [System.Drawing.FontStyle]::Bold, [System.Drawing.GraphicsUnit]::Pixel)
  $smallFont = New-Object System.Drawing.Font('Microsoft YaHei UI', 22, [System.Drawing.FontStyle]::Regular, [System.Drawing.GraphicsUnit]::Pixel)

  $tagBrush = New-Object System.Drawing.SolidBrush([System.Drawing.Color]::FromArgb(255,$r,255,$b))
  $g.DrawString($tag, $tagFont, $tagBrush, 80, 90)

  $titleBrush = New-Object System.Drawing.SolidBrush([System.Drawing.Color]::White)
  $g.DrawString($title, $titleFont, $titleBrush, 80, 170)

  $lineBrush = New-Object System.Drawing.SolidBrush([System.Drawing.Color]::FromArgb(255,190,208,230))
  $bodyRect = New-Object System.Drawing.RectangleF(80, 340, 920, 900)
  $g.DrawString($body, $bodyFont, $lineBrush, $bodyRect)

  $numFont = New-Object System.Drawing.Font('Microsoft YaHei UI', 150, [System.Drawing.FontStyle]::Bold, [System.Drawing.GraphicsUnit]::Pixel)
  $numBrush = New-Object System.Drawing.SolidBrush([System.Drawing.Color]::FromArgb(38,255,255,255))
  $g.DrawString([string]$num, $numFont, $numBrush, 820, 1350)

  $footBrush = New-Object System.Drawing.SolidBrush([System.Drawing.Color]::FromArgb(150,180,200,230))
  $g.DrawString('极速连点器  ·  图文教程  ' + $num + ' / 8', $smallFont, $footBrush, 80, 1790)

  $enc = New-Object System.Drawing.Imaging.EncoderParameters(1)
  $enc.Param[0] = New-Object System.Drawing.Imaging.EncoderParameter([System.Drawing.Imaging.Encoder]::Quality, [long]86)
  $file = Join-Path $dir ('step' + $num.ToString('00') + '.jpg')
  $bmp.Save($file, (Get-JpegCodec), $enc)
  $g.Dispose()
  $bmp.Dispose()
  Write-Host ("generated " + $file + " " + (Get-Item -LiteralPath $file).Length)
}

New-Slide 1 '快速上手' '1. 安装后打开「极速连点器」`n2. 先开启无障碍服务`n3. 再允许悬浮窗权限`n4. 添加圆点后即可开始连点' 'STEP 01 · 总览' 55 220 255
New-Slide 2 '开启无障碍' '进入手机系统设置，点「无障碍」`n找到「极速连点器 / 已下载的应用」`n打开服务开关并确认授权`n开启后状态卡会显示「已开启」' 'STEP 02 · 授权' 80 230 160
New-Slide 3 '允许悬浮窗' '主页面点「设置 / 添加圆点」`n系统会引导到悬浮窗权限页`n选择「允许显示在其他应用上层」`n返回 App 即可进入定位模式' 'STEP 03 · 权限' 255 200 90
New-Slide 4 '添加多个圆点' '进入定位模式后点「＋ 添加」`n圆点会自动出现在屏幕中间`n把圆点拖到目标按钮 / 位置上`n想加几个就加几个' 'STEP 04 · 定位' 255 110 140
New-Slide 5 '调整点击顺序' '第 1 个圆点会先被点击`n长按圆点可「前移 / 后移 / 删除」`n把圆点拖到屏幕左右边缘也可换序`n确认顺序后点「保存」' 'STEP 05 · 顺序' 190 120 255
New-Slide 6 '设置点击参数' '间隔：每次点击相隔的时间`n随机抖动：让间隔更接近真人`n次数：0 表示无限循环`n开始前先选择「顺序 / 随机 / 单点」' 'STEP 06 · 参数' 60 200 160
New-Slide 7 '手势与高级玩法' '普通点击：单次点按`n长按：持续按住目标`n滑动：从圆点滑到下一个点`n多点同时：一批圆点同时点击' 'STEP 07 · 手势' 80 160 255
New-Slide 8 '开始与停止' '打开目标 App，保持页面在前台`n返回连点器点「开始连点」`n运行时通知栏会显示状态`n随时回 App 或通知栏停止' 'STEP 08 · 完成' 120 220 120