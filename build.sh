#!/usr/bin/env bash
# 「媒体同步」APK 一键构建脚本（无需 Gradle / Android Studio）
# 依赖：JDK 11+、Android SDK platform-35 与 build-tools 35.0.0、zip
set -euo pipefail

ROOT="$(cd "$(dirname "$0")" && pwd)"
SDK="${ANDROID_SDK_ROOT:-${ANDROID_HOME:-/opt/android-sdk}}"
BT="$SDK/build-tools/35.0.0"
PLATFORM="$SDK/platforms/android-35/android.jar"
BUILD="$ROOT/build"
OUT="$ROOT/releases/媒体同步.apk"
KS="$BUILD/mediasync-release.jks"
STOREPASS="mediasync2026"

if [ ! -f "$PLATFORM" ] || [ ! -x "$BT/aapt2" ]; then
  echo "错误：未找到 Android SDK platform-35 / build-tools 35.0.0" >&2
  echo "请先安装：sdkmanager \"platforms;android-35\" \"build-tools;35.0.0\" \"platform-tools\"" >&2
  echo "或设置 ANDROID_SDK_ROOT 指向已安装的 SDK 目录。" >&2
  exit 1
fi

echo "==> 清理构建目录"
rm -rf "$BUILD"
mkdir -p "$BUILD/gen" "$BUILD/classes" "$BUILD/dex" "$ROOT/releases"

echo "==> 1/7 编译资源 (aapt2 compile)"
"$BT/aapt2" compile --dir "$ROOT/app/src/main/res" -o "$BUILD/res.zip"

echo "==> 2/7 链接资源并生成 R.java (aapt2 link)"
"$BT/aapt2" link -o "$BUILD/base.apk" \
  -I "$PLATFORM" \
  --manifest "$ROOT/app/src/main/AndroidManifest.xml" \
  -R "$BUILD/res.zip" \
  --java "$BUILD/gen" \
  --min-sdk-version 24 --target-sdk-version 35 \
  --version-code 1 --version-name 1.0.0 \
  --auto-add-overlay

echo "==> 3/7 编译 Java 源码 (javac)"
find "$ROOT/app/src/main/java" "$BUILD/gen" -name '*.java' > "$BUILD/sources.txt"
javac -encoding UTF-8 -source 11 -target 11 -Xlint:-options \
  -classpath "$PLATFORM" \
  -d "$BUILD/classes" \
  @"$BUILD/sources.txt"

echo "==> 4/7 转换为 Dalvik 字节码 (d8)"
mapfile -t CLASSES < <(find "$BUILD/classes" -name '*.class' | sort)
"$BT/d8" --lib "$PLATFORM" --min-api 24 --output "$BUILD/dex" "${CLASSES[@]}"

echo "==> 5/7 合并 classes.dex 到 APK"
(cd "$BUILD/dex" && zip -q -u "$BUILD/base.apk" classes.dex)
"$BT/zipalign" -f 4 "$BUILD/base.apk" "$BUILD/aligned.apk"

echo "==> 6/7 生成签名密钥并签名"
if [ ! -f "$KS" ]; then
  keytool -genkeypair -v -keystore "$KS" \
    -alias mediasync -keyalg RSA -keysize 2048 -validity 10950 \
    -storepass "$STOREPASS" -keypass "$STOREPASS" \
    -dname "CN=MediaSync, OU=OpenSource, O=MediaSync, L=CN, ST=CN, C=CN" > /dev/null 2>&1
fi
"$BT/apksigner" sign \
  --ks "$KS" --ks-key-alias mediasync \
  --ks-pass "pass:$STOREPASS" --key-pass "pass:$STOREPASS" \
  --v1-signing-enabled true --v2-signing-enabled true --v3-signing-enabled true \
  --out "$OUT" "$BUILD/aligned.apk"

echo "==> 7/7 校验"
"$BT/apksigner" verify --verbose "$OUT" | head -12
echo
"$BT/aapt2" dump badging "$OUT" | head -6
echo
echo "构建完成： $OUT"
ls -lh "$OUT"
