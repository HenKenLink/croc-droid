#!/bin/bash
set -e

# 设置默认值
FORCE_REBUILD=false
BUILD_TYPE="minified"  # 默认构建minified版本（最小化）

# 解析参数
while [[ $# -gt 0 ]]; do
    case $1 in
        --force)
            FORCE_REBUILD=true
            shift
            ;;
        --release)
            BUILD_TYPE="release"
            shift
            ;;
        --debug)
            BUILD_TYPE="debug"
            shift
            ;;
        --minified)
            BUILD_TYPE="minified"
            shift
            ;;
        *)
            echo "Unknown option: $1"
            echo "Usage: $0 [--force] [--release|--debug|--minified]"
            echo "  --force     Force rebuild Go bridge"
            echo "  --release   Build release APK (signed, optimized)"
            echo "  --debug     Build debug APK (full size, with debugging)"
            echo "  --minified  Build minified APK (optimized for size, default)"
            exit 1
            ;;
    esac
done

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
    /go/bin/gomobile init
    /go/bin/gomobile bind -target=android/arm64 -androidapi 26 ./
    mkdir -p ../../app/libs
    cp crocbridge.aar ../../app/libs/
    cd ../../
fi

echo "--------------------------------------"
echo "Step 2: Building Android App (APK)..."
echo "--------------------------------------"

# 根据构建类型设置任务和APK路径
if [ "$BUILD_TYPE" = "release" ]; then
    GRADLE_TASK="assembleRelease"
    APK_PATH="app/build/outputs/apk/release/app-release.apk"
    echo "Building RELEASE APK (signed, minified, optimized)..."
elif [ "$BUILD_TYPE" = "minified" ]; then
    GRADLE_TASK="assembleMinified"
    APK_PATH="app/build/outputs/apk/minified/app-minified.apk"
    echo "Building MINIFIED APK (optimized for size, no debugging)..."
else
    GRADLE_TASK="assembleDebug"
    APK_PATH="app/build/outputs/apk/debug/app-debug.apk"
    echo "Building DEBUG APK (full size, with debugging)..."
fi

# 始终运行 Gradle，因为它有自带的高效缓存机制 (Incremental Build)
echo "Running Gradle build..."
chmod +x ./gradlew
./gradlew $GRADLE_TASK

# 显示APK大小
if [ -f "$APK_PATH" ]; then
    APK_SIZE=$(du -h "$APK_PATH" | cut -f1)
    echo "--------------------------------------"
    echo "Build Successful!"
    echo "APK Path: $APK_PATH"
    echo "APK Size: $APK_SIZE"
    echo "--------------------------------------"
else
    echo "Error: APK not found at $APK_PATH"
    exit 1
fi
