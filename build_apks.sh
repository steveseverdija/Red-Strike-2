#!/bin/bash

# Extract version name from app/build.gradle.kts
VERSION_NAME=$(grep 'versionName =' app/build.gradle.kts | awk -F'"' '{print $2}')
if [ -z "$VERSION_NAME" ]; then
    VERSION_NAME="1.0"
fi

echo "Building APKs for version v$VERSION_NAME..."

# Run gradle build
gradle clean :app:assembleRelease :app:assembleDebug
if [ $? -ne 0 ]; then
    echo "Gradle build failed! Aborting export."
    exit 1
fi

# Remove previous github_build_* folders
echo "Cleaning up previous build folders..."
rm -rf github_build_*

# Create output directory
OUT_DIR="github_build_v$VERSION_NAME"
mkdir -p "$OUT_DIR"

# Copy and rename APKs
cp ./app/build/outputs/apk/release/app-release.apk "$OUT_DIR/redstrike-release-v$VERSION_NAME.apk"
cp ./app/build/outputs/apk/debug/app-debug.apk "$OUT_DIR/redstrike-debug-v$VERSION_NAME.apk"

echo "Build completed successfully. APKs exported to $OUT_DIR:"
ls -lh "$OUT_DIR"
