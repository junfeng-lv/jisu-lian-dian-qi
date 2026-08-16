$sdk = "$env:LOCALAPPDATA\Android\Sdk"
$aapt2 = "$sdk\build-tools\36.0.0\aapt2.exe"
$d8 = "$sdk\build-tools\36.0.0\d8.bat"
$androidJar = "$sdk\platforms\android-35\android.jar"
$jarExe = "C:\Program Files\Java\jdk-21.0.11\bin\jar.exe"
$apksigner = "$sdk\build-tools\36.0.0\apksigner.bat"
$keytool = "C:\Program Files\Java\jdk-21.0.11\bin\keytool.exe"
$zipalign = "$sdk\build-tools\36.0.0\zipalign.exe"

$scriptPath = $MyInvocation.MyCommand.Path
if (-not [System.IO.Path]::IsPathRooted($scriptPath)) {
    $scriptPath = Join-Path (Get-Location) $scriptPath
}
$root = [System.IO.Path]::GetFullPath((Split-Path $scriptPath))

$stage = [System.IO.Path]::GetFullPath((Join-Path $env:SystemDrive "codex_auto_tapper_build"))
$expectedStage = [System.IO.Path]::GetFullPath((Join-Path $env:SystemDrive "codex_auto_tapper_build"))
if (Test-Path -LiteralPath $stage) {
    $resolved = [System.IO.Path]::GetFullPath($stage)
    if ($resolved -ne $expectedStage) { throw "Refusing to delete unexpected stage path: $resolved" }
    Remove-Item -LiteralPath $resolved -Recurse -Force
}
New-Item -ItemType Directory -Force $stage | Out-Null
Copy-Item -LiteralPath "$root\app" -Destination $stage -Recurse -Force

$srcMain = "$stage\app\src\main"
$build = "$stage\build"
New-Item -ItemType Directory -Force "$build\res","$build\classes","$build\dex","$build\gen" | Out-Null

Write-Host "=== 1/8 Compile resources ==="
& $aapt2 compile -o "$build\res" --dir "$srcMain\res" --no-crunch
if ($LASTEXITCODE -ne 0) { Write-Host "FAIL aapt2 compile"; exit 1 }

Write-Host "=== 2/8 Link base APK ==="
$flatFiles = Get-ChildItem "$build\res" -Filter *.flat | Select-Object -ExpandProperty FullName
& $aapt2 link -o "$build\base.apk" --manifest "$srcMain\AndroidManifest.xml" -I $androidJar --auto-add-overlay --java "$build\gen" $flatFiles
if ($LASTEXITCODE -ne 0) { Write-Host "FAIL aapt2 link"; exit 1 }

Write-Host "=== 3/8 Compile Java ==="
$javaFiles = Get-ChildItem "$srcMain\java" -Recurse -Filter *.java | Select-Object -ExpandProperty FullName
$genFiles = Get-ChildItem "$build\gen" -Recurse -Filter *.java -ErrorAction SilentlyContinue | Select-Object -ExpandProperty FullName
$allFiles = $javaFiles + $genFiles
& javac -encoding UTF-8 -cp "$androidJar" -d "$build\classes" $allFiles 2>&1
if ($LASTEXITCODE -ne 0) { Write-Host "FAIL javac"; exit 1 }

Write-Host "=== 4/8 Create classes jar ==="
& $jarExe --create --file "$build\classes.jar" -C "$build\classes" .
if ($LASTEXITCODE -ne 0) { Write-Host "FAIL jar"; exit 1 }

Write-Host "=== 5/8 Convert to dex ==="
& cmd /c "$d8 --release --min-api 24 --output $build\dex $build\classes.jar"
if ($LASTEXITCODE -ne 0) { Write-Host "FAIL d8"; exit 1 }

Write-Host "=== 6/8 Add dex + assets to APK ==="
& $jarExe --update --file "$build\base.apk" -C "$build\dex" classes.dex
if ($LASTEXITCODE -ne 0) { Write-Host "FAIL jar update (dex)"; exit 1 }
$addRoot = "$stage\apkroot"
New-Item -ItemType Directory -Force "$addRoot" | Out-Null
Copy-Item -LiteralPath "$srcMain\assets" -Destination "$addRoot\assets" -Recurse -Force
& $jarExe --update --file "$build\base.apk" -C "$addRoot" .
if ($LASTEXITCODE -ne 0) { Write-Host "FAIL jar update (assets)"; exit 1 }

Write-Host "=== 7/8 Zipalign ==="
& $zipalign -f 4 "$build\base.apk" "$build\aligned.apk"
if ($LASTEXITCODE -ne 0) { Write-Host "FAIL zipalign"; exit 1 }

Write-Host "=== 8/8 Sign APK ==="
$keystoreStage = "$stage\keystore.jks"
$keystoreRoot = "$root\.android_keystore.jks"
if (-not (Test-Path $keystoreStage)) {
    if (Test-Path $keystoreRoot) {
        Copy-Item -LiteralPath $keystoreRoot -Destination $keystoreStage
    } else {
        & "$keytool" -genkey -v -keystore $keystoreStage -storepass changeit -alias androiddebugkey -keypass changeit -keyalg RSA -keysize 2048 -validity 10000 -dname "CN=Developer,OU=Dev,O=Example,L=Shanghai,ST=Beijing,C=CN" 2>&1 | Out-Null
        if (Test-Path $keystoreStage) {
            Copy-Item -LiteralPath $keystoreStage -Destination $keystoreRoot
        }
    }
}
Remove-Item "$build\signed.apk" -Force -ErrorAction SilentlyContinue
& cmd /c "$apksigner sign --ks $keystoreStage --ks-pass pass:changeit --key-pass pass:changeit --v1-signing-enabled true --v2-signing-enabled true --v3-signing-enabled true --out $build\signed.apk $build\aligned.apk"
if ($LASTEXITCODE -ne 0) { Write-Host "FAIL sign"; exit 1 }

Write-Host ""
Write-Host "=== Signature verify ==="
& cmd /c "$apksigner verify --verbose $build\signed.apk"
Write-Host ""
Write-Host "=== Badging ==="
& $aapt2 dump badging "$build\signed.apk" | Select-Object -First 8
Write-Host ""
Copy-Item -LiteralPath "$build\signed.apk" -Destination "$root\auto_tapper.apk" -Force
$info = Get-Item "$root\auto_tapper.apk"
Write-Host "BUILD OK: $($info.FullName) ($($info.Length) bytes)"

if (Test-Path -LiteralPath $stage) {
    $r = [System.IO.Path]::GetFullPath($stage)
    if ($r -eq $expectedStage) {
        Remove-Item -LiteralPath $r -Recurse -Force -ErrorAction SilentlyContinue
    }
}