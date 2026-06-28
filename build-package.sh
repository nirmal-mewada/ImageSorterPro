#!/bin/bash
# ============================================================================
# ImageSorterPro - Build & Package Script
# Creates platform-specific app bundles using jpackage
# Supports: macOS (.app/.dmg), Windows (.exe/.msi)
# ============================================================================

set -e

PROJECT_DIR="$(cd "$(dirname "$0")" && pwd)"
TARGET_DIR="$PROJECT_DIR/target"
INSTALLER_DIR="$TARGET_DIR/installer"
RESOURCES_DIR="$PROJECT_DIR/src/main/resources/com/imagesorter/images"
APP_NAME="ImageSorterPro"
APP_VERSION="1.0.0"
MAIN_CLASS="com.imagesorter.Main"
VENDOR="Nirmal"
DESCRIPTION="Quick-sort and organize images and videos using hotkeys"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

echo -e "${BLUE}╔══════════════════════════════════════════════════╗${NC}"
echo -e "${BLUE}║     ImageSorterPro - Build & Package Tool        ║${NC}"
echo -e "${BLUE}╚══════════════════════════════════════════════════╝${NC}"
echo ""

# --- Step 1: Check prerequisites ---
echo -e "${YELLOW}[1/4] Checking prerequisites...${NC}"

if ! command -v java &> /dev/null; then
    echo -e "${RED}ERROR: Java not found. Please install JDK 21+.${NC}"
    exit 1
fi

JAVA_VERSION=$(java -version 2>&1 | head -1 | awk -F '"' '{print $2}' | cut -d'.' -f1)
echo "  Java version: $JAVA_VERSION"

if ! command -v jpackage &> /dev/null; then
    echo -e "${RED}ERROR: jpackage not found. Requires JDK 14+.${NC}"
    exit 1
fi
echo "  jpackage: $(jpackage --version)"

if ! command -v mvn &> /dev/null; then
    echo -e "${RED}ERROR: Maven not found.${NC}"
    exit 1
fi
echo "  Maven: $(mvn --version | head -1 | awk '{print $3}')"

echo -e "${GREEN}  ✓ All prerequisites met${NC}"
echo ""

# --- Step 2: Build the fat JAR ---
echo -e "${YELLOW}[2/4] Building fat JAR with Maven...${NC}"
mvn clean package -DskipTests -q
echo -e "${GREEN}  ✓ JAR built successfully${NC}"

# Find the shaded JAR (it's the one without 'original-' prefix)
SHADE_JAR=$(find "$TARGET_DIR" -maxdepth 1 -name "ImageSorterPro-*.jar" ! -name "original-*" | head -1)
if [ -z "$SHADE_JAR" ]; then
    echo -e "${RED}ERROR: Cannot find shaded JAR in $TARGET_DIR${NC}"
    exit 1
fi
JAR_NAME=$(basename "$SHADE_JAR")
JAR_SIZE=$(du -h "$SHADE_JAR" | awk '{print $1}')
echo "  JAR: $JAR_NAME ($JAR_SIZE)"
echo ""

# --- Step 3: Detect platform and set options ---
echo -e "${YELLOW}[3/4] Detecting platform...${NC}"

PLATFORM_ARGS=()
OS_TYPE="unknown"

case "$(uname -s)" in
    Darwin*)
        OS_TYPE="macOS"
        echo "  Platform: macOS ($(uname -m))"
        
        # Use .icns icon for macOS
        if [ -f "$RESOURCES_DIR/app_icon.icns" ]; then
            PLATFORM_ARGS+=("--icon" "$RESOURCES_DIR/app_icon.icns")
            echo "  Icon: app_icon.icns"
        fi
        
        # macOS-specific options
        PLATFORM_ARGS+=("--type" "app-image")
        PLATFORM_ARGS+=("--mac-package-name" "$APP_NAME")
        ;;
    MINGW*|MSYS*|CYGWIN*|Windows*)
        OS_TYPE="Windows"
        echo "  Platform: Windows"
        
        # Use .ico icon for Windows
        if [ -f "$RESOURCES_DIR/app_icon.ico" ]; then
            PLATFORM_ARGS+=("--icon" "$RESOURCES_DIR/app_icon.ico")
            echo "  Icon: app_icon.ico"
        fi
        
        # Windows-specific options
        PLATFORM_ARGS+=("--type" "app-image")
        PLATFORM_ARGS+=("--win-console")
        ;;
    *)
        OS_TYPE="Linux"
        echo "  Platform: Linux"
        PLATFORM_ARGS+=("--type" "app-image")
        ;;
esac
echo ""

# --- Step 4: Create app bundle with jpackage ---
echo -e "${YELLOW}[4/4] Creating app bundle with jpackage...${NC}"

# Clean previous installer output
rm -rf "$INSTALLER_DIR"
mkdir -p "$INSTALLER_DIR"

# Create a clean input directory with only the JAR we need
INPUT_DIR="$TARGET_DIR/jpackage-input"
rm -rf "$INPUT_DIR"
mkdir -p "$INPUT_DIR"
cp "$SHADE_JAR" "$INPUT_DIR/"

# --- Download and bundle FFmpeg binary for packaging ---
echo -e "${YELLOW}  Downloading and bundling FFmpeg...${NC}"
FFMPEG_VERSION="8.1"
FFMPEG_URL=""
FFMPEG_EXE=""

case "$OS_TYPE" in
    macOS)
        ARCH_TYPE=$(uname -m)
        if [ "$ARCH_TYPE" = "arm64" ]; then
            FFMPEG_URL="https://github.com/Tyrrrz/FFmpegBin/releases/download/${FFMPEG_VERSION}/ffmpeg-osx-arm64.zip"
        else
            FFMPEG_URL="https://github.com/Tyrrrz/FFmpegBin/releases/download/${FFMPEG_VERSION}/ffmpeg-osx-x64.zip"
        fi
        FFMPEG_EXE="ffmpeg"
        ;;
    Windows)
        FFMPEG_URL="https://github.com/Tyrrrz/FFmpegBin/releases/download/${FFMPEG_VERSION}/ffmpeg-windows-x64.zip"
        FFMPEG_EXE="ffmpeg.exe"
        ;;
esac

if [ -n "$FFMPEG_URL" ]; then
    echo "    Downloading: $FFMPEG_URL"
    if curl -L -s -o "$TARGET_DIR/ffmpeg.zip" "$FFMPEG_URL"; then
        mkdir -p "$TARGET_DIR/ffmpeg-extracted"
        unzip -q -o "$TARGET_DIR/ffmpeg.zip" -d "$TARGET_DIR/ffmpeg-extracted"
        cp "$TARGET_DIR/ffmpeg-extracted/$FFMPEG_EXE" "$INPUT_DIR/"
        if [ "$OS_TYPE" = "macOS" ]; then
            chmod +x "$INPUT_DIR/$FFMPEG_EXE"
        fi
        echo -e "${GREEN}    ✓ Bundled $FFMPEG_EXE successfully${NC}"
    else
        echo -e "${RED}    ERROR: Failed to download FFmpeg. Continuing packaging without it.${NC}"
    fi
else
    echo -e "${YELLOW}    ⚠ Skipping FFmpeg download for this platform${NC}"
fi

echo "  Running jpackage..."
jpackage \
    --input "$INPUT_DIR" \
    --name "$APP_NAME" \
    --main-jar "$JAR_NAME" \
    --main-class "$MAIN_CLASS" \
    --dest "$INSTALLER_DIR" \
    --app-version "$APP_VERSION" \
    --vendor "$VENDOR" \
    --description "$DESCRIPTION" \
    --java-options "-Xmx512m" \
    --java-options "-Xms128m" \
    --java-options "--add-opens=javafx.graphics/com.sun.javafx.application=ALL-UNNAMED" \
    "${PLATFORM_ARGS[@]}"

echo -e "${GREEN}  ✓ App bundle created successfully${NC}"
echo ""

# --- Summary ---
echo -e "${BLUE}╔══════════════════════════════════════════════════╗${NC}"
echo -e "${BLUE}║                  BUILD COMPLETE                  ║${NC}"
echo -e "${BLUE}╚══════════════════════════════════════════════════╝${NC}"
echo ""

if [ "$OS_TYPE" = "macOS" ]; then
    APP_BUNDLE="$INSTALLER_DIR/$APP_NAME.app"
    if [ -d "$APP_BUNDLE" ]; then
        BUNDLE_SIZE=$(du -sh "$APP_BUNDLE" | awk '{print $1}')
        echo -e "  ${GREEN}✓${NC} App bundle: $APP_BUNDLE"
        echo -e "  ${GREEN}✓${NC} Bundle size: $BUNDLE_SIZE"
        echo ""
        echo -e "  ${YELLOW}To run:${NC}"
        echo "    open \"$APP_BUNDLE\""
        echo "  or:"
        echo "    \"$APP_BUNDLE/Contents/MacOS/$APP_NAME\""
        echo ""
        echo -e "  ${YELLOW}To create a DMG installer:${NC}"
        echo "    Re-run with --type dmg in jpackage args"
    fi
elif [ "$OS_TYPE" = "Windows" ]; then
    echo -e "  ${GREEN}✓${NC} App bundle: $INSTALLER_DIR/$APP_NAME/"
    echo ""
    echo -e "  ${YELLOW}To run:${NC}"
    echo "    $INSTALLER_DIR/$APP_NAME/$APP_NAME.exe"
fi
echo ""
