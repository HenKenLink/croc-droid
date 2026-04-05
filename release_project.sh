#!/bin/bash
set -e

# --- Configuration ---
# You can override these in a local 'keystore.properties' file
KEYSTORE_PROPERTIES="app/keystore.properties"

# 1. SDK/NDK Environment
export ANDROID_HOME=$HOME/android-sdk
export ANDROID_NDK_HOME=$ANDROID_HOME/ndk/26.3.11579264
export PATH=$ANDROID_HOME/cmdline-tools/latest/bin:$ANDROID_HOME/platform-tools:$PATH

echo "--------------------------------------"
echo "Step 1: Building Multi-Arch Go Bridge"
echo "--------------------------------------"

cd go/crocbridge
rm -f crocbridge.aar
# Build for all common Android architectures:
# arm: 32-bit (armeabi-v7a)
# arm64: 64-bit (arm64-v8a)
# 386: x86
# amd64: x86_64
/go/bin/gomobile bind -target=android/arm,android/arm64,android/386,android/amd64 -androidapi 26 ./

mkdir -p ../../app/libs
cp crocbridge.aar ../../app/libs/
cd ../../

echo "--------------------------------------"
echo "Step 2: Checking for Signing Keys"
echo "--------------------------------------"

# Template if keystore.properties doesn't exist
if [ ! -f "$KEYSTORE_PROPERTIES" ]; then
    echo "⚠️  $KEYSTORE_PROPERTIES not found!"
    echo "To sign the release build, please create $KEYSTORE_PROPERTIES with:"
    echo "RELEASE_STORE_FILE=/path/to/keystore.jks"
    echo "RELEASE_STORE_PASSWORD=..."
    echo "RELEASE_KEY_ALIAS=..."
    echo "RELEASE_KEY_PASSWORD=..."
fi

echo "--------------------------------------"
echo "Step 3: Building Production Artifacts"
echo "--------------------------------------"

chmod +x ./gradlew
# Build both APK (for sideloading) and AAB (for Play Store)
./gradlew assembleRelease bundleRelease

echo "--------------------------------------"
echo "✅ Release Build Successful!"
echo "APK Path: app/build/outputs/apk/release/app-release.apk"
echo "AAB Path: app/build/outputs/bundle/release/app-release.aab"
echo "--------------------------------------"
