$ErrorActionPreference = 'Stop'
$root = (Get-Item $PSScriptRoot).Parent.FullName
$build = Join-Path $root 'build'
$classes = Join-Path $build 'classes'
$res = Join-Path $build 'res'
$src = Join-Path $build 'src'
Remove-Item $build -Recurse -Force -ErrorAction SilentlyContinue | Out-Null
New-Item -ItemType Directory -Force $classes,$res,$src | Out-Null

Copy-Item "$root\app\src\main" -Destination "$src\main" -Recurse -Force
Copy-Item "$root\public" -Destination "$src\main\assets" -Recurse -Force

$aapt = 'C:\Users\吕俊锋\AppData\Local\Android\Sdk\build-tools\36.0.0\aapt2.exe'
& $aapt compile --dir "$src\main\res" -o $res
& $aapt link -I "C:\Users\吕俊锋\AppData\Local\Android\Sdk\platforms\android-36\android.jar" --manifest "$src\main\AndroidManifest.xml" --java $res -A "$src\main\assets" -o "$res\resources.apk"

$javaFiles = Get-ChildItem "$res\**\*.java" -Recurse | Select-Object -ExpandProperty FullName
& 'javac.exe' -bootclasspath "C:\Users\吕俊锋\AppData\Local\Android\Sdk\platforms\android-36\android.jar" -d $classes @javaFiles
& 'C:\Users\吕俊锋\AppData\Local\Android\Sdk\build-tools\36.0.0\d8.bat' --release --output "$classes.dex" $classes

Add-Type -AssemblyName System.IO.Compression.FileSystem
$outZip = Join-Path $root 'auto_tapper.apk'
if (Test-Path $outZip) { Remove-Item $outZip -Force }
[IO.Compression.ZipFile]::CreateFromDirectory((Join-Path $root 'build\res'), $outZip)
Write-Host "APK built: $outZip"
