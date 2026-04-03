# Croc-Droid Codespace Setup & Recovery Guide

This document records the steps taken to initialize the environment and project. Use this to restore the development environment if the Codespace is reset.

## 1. Android SDK Environment
The SDK is installed in `~/android-sdk`.

### Manual SDK Installation
Run the included `setup_sdk.sh` script to automate this:
```bash
chmod +x setup_sdk.sh
./setup_sdk.sh
source ~/.bashrc
```

### Components Installed
- **Command Line Tools**: 14.0 (latest)
- **Platforms**: `android-35`, `android-36`
- **Build-Tools**: `35.0.0`, `36.0.0`
- **NDK**: `26.3.11579264` (Required for GoMobile/CGO)
- **CMake**: `3.22.1`

## 2. Go & GoMobile Integration
The project uses `gomobile` to bridge the Go-based `croc` engine to Android.

### Setup Go Tools
```bash
go install golang.org/x/mobile/cmd/gomobile@latest
go install golang.org/x/mobile/cmd/gobind@latest
# Note: Ensure /usr/local/bin or ~/go/bin is in PATH
```

### Rebuilding the AAR Bridge
If you modify `go/crocbridge/bridge.go`, you must rebuild the AAR:
```bash
cd go/crocbridge
export ANDROID_HOME=$HOME/android-sdk
export ANDROID_NDK_HOME=$ANDROID_HOME/ndk/26.3.11579264
export PATH=$PATH:/usr/local/bin # or wherever gomobile is
gomobile bind -target=android -androidapi 26 -o ../../app/libs/crocbridge.aar ./
```

## 3. Project Structure Verification
### Important Files
- `setup_sdk.sh`: Main environment setup script.
- `app/libs/crocbridge.aar`: The compiled Go engine.
- `app/build.gradle.kts`: Configured for AGP 9.1 and JVM 17.
- `go/crocbridge/bridge.go`: The Go-side wrapper for `croc`.

### Known Fixes Applied
- **JVM Target**: Explicitly set to 17 in `app/build.gradle.kts` for both Java and Kotlin.
- **Icon Placeholders**: Added XML adaptive icons in `app/src/main/res/mipmap-anydpi-v26/` to prevent AAPT errors.

## 4. Build Commands
To compile the debug APK:
```bash
export ANDROID_HOME=$HOME/android-sdk
./gradlew assembleDebug
```
