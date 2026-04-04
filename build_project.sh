#!/bin/bash
set -e

# 设置强制重新编译标志
FORCE_REBUILD=false
if [[ "$1" == "--force" ]]; then
    FORCE_REBUILD=true
fi

# 1. 智能加载 SDK 环境变量
export ANDROID_HOME=$HOME/android-sdk
if [ ! -f "$ANDROID_HOME/cmdline-tools/latest/bin/sdkmanager" ]; then
    echo "SDK environment not detected or incomplete. Running setup_sdk.sh..."
    if [ -f "./setup_sdk.sh" ]; then
        chmod +x ./setup_sdk.sh
        ./setup_sdk.sh
    else
        echo "Error: setup_sdk.sh not found!"
        exit 1
    fi
else
    echo "✅ SDK environment is ready."
    export PATH=$ANDROID_HOME/cmdline-tools/latest/bin:$ANDROID_HOME/platform-tools:$PATH
fi

# 确保 NDK 路径设置正确
export ANDROID_NDK_HOME=$ANDROID_HOME/ndk/26.3.11579264

echo "--------------------------------------"
echo "Step 1: Checking Go Bridge (AAR)..."
echo "--------------------------------------"

AAR_PATH="app/libs/crocbridge.aar"
REBUILD_AAR=$FORCE_REBUILD

if [ ! -f "$AAR_PATH" ]; then
    REBUILD_AAR=true
    echo "AAR not found. Building..."
else
    # 检查 Go 源码是否有更新 (比 AAR 文件新)
    # 检查 go/crocbridge 目录和 external/croc 目录
    CHANGES=$(find go/crocbridge/ external/croc/ -type f -newer "$AAR_PATH" | wc -l)
    if [ "$CHANGES" -gt 0 ]; then
        REBUILD_AAR=true
        echo "Detected $CHANGES file changes in Go source. Rebuilding AAR..."
    else
        echo "✅ AAR is up to date. Skipping Go build. (Use --force to rebuild)"
    fi
fi

if [ "$REBUILD_AAR" = true ]; then
    cd go/crocbridge
    rm -f crocbridge.aar
    /go/bin/gomobile bind -target=android/arm64 -androidapi 26 ./
    mkdir -p ../../app/libs
    cp crocbridge.aar ../../app/libs/
    cd ../../
fi

echo "--------------------------------------"
echo "Step 2: Building Android App (APK)..."
echo "--------------------------------------"

# APK 路径
APK_PATH="app/build/outputs/apk/debug/app-debug.apk"

# 始终运行 Gradle，因为它有自带的高效缓存机制 (Incremental Build)
# 除非 Gradle 任务耗时太长，通常不需要手动跳过
echo "Running Gradle build..."
chmod +x ./gradlew
./gradlew assembleDebug

echo "--------------------------------------"
echo "Build Successful!"
echo "APK Path: $APK_PATH"
echo "--------------------------------------"
