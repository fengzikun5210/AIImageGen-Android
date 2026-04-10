# AI Image Generator - Android App

An Android app that generates images using AI, supporting two modes:

## Features

### Mode 1: Pollinations AI (Free, No API Key)
- Uses [Pollinations.ai](https://pollinations.ai) free API
- Supports models: **flux** (recommended), **turbo**
- Various aspect ratios: 1024x1024, 1024x2048, 2048x1024, 768x1344, 1344x768
- No registration required

### Mode 2: ModelScope API
- Uses [ModelScope](https://modelscope.cn) AI image generation API
- 6 models available
- Requires an **Access Token** from ModelScope

## How to Get ModelScope Access Token
1. Visit [modelscope.cn](https://modelscope.cn)
2. Register/Login
3. Go to Settings → Access Token
4. Generate a token and paste it into the app

## APK Download
Download the latest APK from the **Actions** tab of this repository, or from the Releases section.

## Building from Source
Open the project in Android Studio (Arctic Fox or newer) and run.

Alternatively, use the provided GitHub Actions workflow:
- Push to `main` branch → APK is automatically built
- Download from the Actions tab → Artifacts

## Technical Details
- Min SDK: 24 (Android 7.0)
- Target SDK: 34 (Android 14)
- Language: Java (Android)
- Build: Gradle with GitHub Actions
