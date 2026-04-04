#!/bin/bash

# 1. Define the SDK root directory
export ANDROID_HOME=$HOME/android-sdk
mkdir -p $ANDROID_HOME

# 2. Check if Command Line Tools already exist
if [ ! -f "$ANDROID_HOME/cmdline-tools/latest/bin/sdkmanager" ]; then
    echo "Android Command Line Tools not found. Downloading..."
    CMDLINE_URL="https://dl.google.com/android/repository/commandlinetools-linux-14742923_latest.zip"
    curl -L -o /tmp/cmdline-tools.zip $CMDLINE_URL
    mkdir -p $ANDROID_HOME/cmdline-tools/latest
    unzip -q /tmp/cmdline-tools.zip -d /tmp/
    # Move content and handle potential nested 'cmdline-tools' folder from zip
    mv /tmp/cmdline-tools/* $ANDROID_HOME/cmdline-tools/latest/ 2>/dev/null || true
    rm -rf /tmp/cmdline-tools /tmp/cmdline-tools.zip
else
    echo "Android Command Line Tools already present at $ANDROID_HOME/cmdline-tools/latest"
fi

# 4. Configure environment variables and persist into .bashrc
export PATH=$ANDROID_HOME/cmdline-tools/latest/bin:$ANDROID_HOME/platform-tools:$PATH

if ! grep -q "ANDROID_HOME" ~/.bashrc; then
    echo "Persisting environment variables into .bashrc..."
    echo "" >> ~/.bashrc
    echo "# Android SDK paths" >> ~/.bashrc
    echo "export ANDROID_HOME=$ANDROID_HOME" >> ~/.bashrc
    echo "export PATH=\$ANDROID_HOME/cmdline-tools/latest/bin:\$ANDROID_HOME/platform-tools:\$PATH" >> ~/.bashrc
fi

# 5. Install core Android components via sdkmanager
if [ ! -d "$ANDROID_HOME/platforms/android-36" ]; then
    echo "Installing Platform-Tools, SDK 35/36, and Build-Tools..."
    yes | sdkmanager --sdk_root=$ANDROID_HOME --licenses
    sdkmanager --sdk_root=$ANDROID_HOME \
        "platform-tools" \
        "platforms;android-36" \
        "platforms;android-35" \
        "build-tools;36.0.0" \
        "build-tools;35.0.0"
else
    echo "Core Android components already installed."
fi

# 5b. Install NDK (Required for gomobile)
NDK_VERSION="26.3.11579264"
if [ ! -d "$ANDROID_HOME/ndk/$NDK_VERSION" ]; then
    echo "Installing NDK $NDK_VERSION..."
    sdkmanager --sdk_root=$ANDROID_HOME "ndk;$NDK_VERSION"
else
    echo "NDK $NDK_VERSION already installed."
fi

# 6. Configure project local.properties
if [ -f "settings.gradle.kts" ]; then
    echo "Android project detected. Configuring local.properties..."
    echo "sdk.dir=$ANDROID_HOME" > local.properties
    echo "ndk.dir=$ANDROID_HOME/ndk/$NDK_VERSION" >> local.properties
fi

# 7. Setup Go and Gomobile
if ! command -v gomobile &> /dev/null; then
    echo "Gomobile not found. Installing..."
    go install golang.org/x/mobile/cmd/gomobile@latest
    # Assuming GOBIN is in PATH; if not, link it to /usr/local/bin or similar if needed
    # Codespace usually has $HOME/go/bin in PATH
    GO_BIN_PATH=$(go env GOPATH)/bin
    if [[ ":$PATH:" != *":$GO_BIN_PATH:"* ]]; then
        echo "Adding $GO_BIN_PATH to .bashrc PATH..."
        echo 'export PATH=$PATH:'"$GO_BIN_PATH" >> ~/.bashrc
        export PATH=$PATH:$GO_BIN_PATH
    fi
    gomobile init -ndk $ANDROID_HOME/ndk/$NDK_VERSION
else
    echo "Gomobile is already installed."
fi

echo "-----------------------------------------------"
echo "🎉 Android SDK installation completed successfully!"
echo "Please run 'source ~/.bashrc' or restart your terminal to activate the changes."
echo "You can now run './gradlew assembleDebug' to build your project."
