# Builds build\Lumina.apk with the plain Android SDK command-line tools (no Gradle).
$ErrorActionPreference = "Stop"
$SDK = if ($env:ANDROID_HOME) { $env:ANDROID_HOME } else { "$env:LOCALAPPDATA\Android\Sdk" }
$BT = "$SDK\build-tools\35.0.0"
$JAR = "$SDK\platforms\android-35\android.jar"
$app = $PSScriptRoot
Set-Location $app; [Environment]::CurrentDirectory = $app
Remove-Item -Recurse -Force build -ErrorAction SilentlyContinue
New-Item -ItemType Directory -Force build\obj, build\dex | Out-Null
# merge public assets + (optional, never published) private_assets: Noise templates + your local_config.json
New-Item -ItemType Directory -Force build\assets_merged | Out-Null
if (Test-Path assets) { Copy-Item -Recurse -Force assets\* build\assets_merged\ -ErrorAction SilentlyContinue }
if (Test-Path private_assets) { Copy-Item -Recurse -Force private_assets\* build\assets_merged\ }
& "$BT\aapt2.exe" compile --dir res -o build\res.zip
& "$BT\aapt2.exe" link --manifest AndroidManifest.xml -I $JAR -A build\assets_merged -o build\base.apk build\res.zip
$ErrorActionPreference = "Continue"
javac --release 11 -classpath $JAR -d build\obj (Get-ChildItem src -Recurse -Filter *.java | ForEach-Object { $_.FullName }) 2>&1 | Where-Object { $_ -notmatch "deprecated API|Recompile with" }
if ($LASTEXITCODE -ne 0) { throw "javac failed" }
$ErrorActionPreference = "Stop"
$classes = Get-ChildItem -Recurse build\obj -Filter *.class | ForEach-Object { $_.FullName }
& "$BT\d8.bat" --min-api 33 --lib $JAR --output build\dex @classes
python -I -c "import zipfile,shutil; shutil.copy('build/base.apk','build/unsigned.apk'); z=zipfile.ZipFile('build/unsigned.apk','a'); z.write('build/dex/classes.dex','classes.dex'); z.close()"
& "$BT\zipalign.exe" -f 4 build\unsigned.apk build\aligned.apk
& "$BT\apksigner.bat" sign --ks "$env:USERPROFILE\.android\debug.keystore" --ks-pass pass:android --key-pass pass:android --ks-key-alias androiddebugkey --out build\Lumina.apk build\aligned.apk
& "$BT\apksigner.bat" verify build\Lumina.apk
"BUILD OK: $((Get-Item build\Lumina.apk).Length) bytes"
