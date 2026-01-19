#!/bin/bash

# Deploy Script
# Usage: ./deploy_app.sh

echo "🚀 Starting Build & Deploy..."

# 0. Force Java 17 (SDKMAN)
# Because Android Gradle doesn't support Java 25 yet.
if [ -d "$HOME/.sdkman/candidates/java/17.0.10-tem" ]; then
    export JAVA_HOME="$HOME/.sdkman/candidates/java/17.0.10-tem"
    echo "☕ Set JAVA_HOME to $JAVA_HOME"
fi

# Detect ADB Location
ADB="adb"
if ! command -v adb &> /dev/null; then
    if [ -f "local.properties" ]; then
        SDK_DIR=$(grep "^sdk.dir" local.properties | cut -d'=' -f2)
        if [ -n "$SDK_DIR" ]; then
            ADB="$SDK_DIR/platform-tools/adb"
        fi
    fi
fi

# 1. Build & Install Debug APK
./gradlew installDebug

if [ $? -eq 0 ]; then
    echo "✅ Build Successful. Launching App..."
    
    # 2. Launch Main Activity
    $ADB shell am start -n com.vaultguard.app/.MainActivity
    
    echo "📲 App Launched on Emulator!"
else
    echo "❌ Build Failed."
    exit 1
fi
