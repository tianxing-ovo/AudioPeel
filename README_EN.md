# 🎵 AudioPeel

**[中文](README.md) | English**

A lightweight Android video-to-audio extraction tool, supporting MP3 / M4A / WAV / FLAC / OGG output — ad-free and completely free

![Version](https://img.shields.io/badge/Version-1.2-blue?style=for-the-badge)
![Min SDK](https://img.shields.io/badge/Min_SDK-24_(Android_7.0)-orange?style=for-the-badge)
[![Download APK](https://img.shields.io/badge/Download-APK-brightgreen?style=for-the-badge&logo=android&logoColor=white)](https://github.com/tianxing-ovo/AudioPeel/releases/latest)

![Android](https://img.shields.io/badge/Android-3DDC84?style=for-the-badge&logo=android&logoColor=white)
![Kotlin](https://img.shields.io/badge/Kotlin-0095D5?&style=for-the-badge&logo=kotlin&logoColor=white)
![Jetpack Compose](https://img.shields.io/badge/Jetpack_Compose-4285F4?style=for-the-badge&logo=jetpackcompose&logoColor=white)
![FFmpeg](https://img.shields.io/badge/FFmpeg-007808?style=for-the-badge&logo=ffmpeg&logoColor=white)

## 📸 Screenshots

![AudioPeel Screenshot](art/screenshot.png)

---

## 📋 Table of Contents

- [Features](#-features)
- [Tech Stack](#-tech-stack--architecture)
- [Build Guide](#-build-guide)
- [Development Notes](#-development-notes)
- [Permissions](#-permissions)
- [Contributing](#-contributing)
- [License](#-license)

---

## 🌟 Features

- **Simple Video Selection** — Native Android Photo Picker support. No storage permissions required.

- **Multiple Output Formats** — Export to MP3, M4A, WAV, FLAC, or OGG, covering everything from high-compatibility lossy to lossless formats.

- **Smart Format Detection** — Automatically detects the embedded audio codec and selects the optimal output format (e.g., AAC→M4A), prioritizing direct stream copy for instant extraction.

- **Blazing Fast Extraction** — SAF direct read skips file copying, smart stream copy avoids unnecessary transcoding, and multi-threaded processing leverages multi-core performance. An 18-minute video takes just seconds.

- **Real-time Progress** — Circular progress bar with percentage readout, with cancel support at any time.

- **Built-in Audio Preview** — Smooth playback powered by Media3 ExoPlayer with silky seek bar interaction.

- **File Management** — Custom renaming, auto-save to `/Music/AudioPeel`, and one-tap access via system file manager.

- **Quick Sharing** — Secure file sharing via FileProvider to WeChat, Telegram, and other apps.

- **Minimal APK Size** — R8 code shrinking + resource shrinking + ABI filtering for the smallest possible install size.

---

## 🚀 Tech Stack & Architecture

Built following Google's recommended modern architecture practices, entirely with native solutions:

| Module            | Technology                                                             |
|-------------------|------------------------------------------------------------------------|
| Language          | 100% Kotlin                                                            |
| UI Framework      | Jetpack Compose + Material Design 3 + Edge-to-Edge                     |
| Media Processing  | FFmpegKit (`com.mrljdx:ffmpeg-kit-full`)                               |
| Audio Engine      | AndroidX Media3 ExoPlayer                                              |
| Media Probing     | Native `MediaExtractor` + `MediaMetadataRetriever`                     |
| Architecture      | MVVM + `StateFlow` unidirectional data flow                            |
| File Storage      | Scoped Storage + MediaStore API (Android 10+ compatible)               |
| Size Optimization | R8 shrinking + resource shrinking + ABI filter (`arm64-v8a`, `x86_64`) |

---

## 💻 Build Guide

### Prerequisites

1. **Android Studio** (Iguana | 2023.2.1 or later recommended)
2. **Java 11+** (must be compatible with Gradle version)
3. **Android SDK Level 36+** (Target SDK API 36 recommended)

> **Compatibility:** Minimum Android 7.0 (API 24), Target SDK API 36.

### Build Steps

1. Clone the repository:
   ```bash
   git clone https://github.com/tianxing-ovo/AudioPeel.git
   ```
2. Open the project root directory in Android Studio.
3. Wait for Gradle to sync and download dependencies.
4. Connect a physical device or configure an Android emulator.
5. Click `Run 'app'` or execute `./gradlew assembleDebug` in the terminal.

---

## 💡 Development Notes

Throughout the development of this project, we tackled several common challenges in Android's fragmented ecosystem. We hope these insights help other developers facing similar issues:

- **SAF Direct Read** — Using `FFmpegKitConfig.getSafParameterForRead()` to read videos directly from `content://` URIs, completely eliminating the time-consuming step of copying files to a cache directory. Separate SAF file descriptors are allocated for FFprobe and FFmpeg to resolve the `SAF id not found` issue.

- **Smart Stream Copy** — Detecting the source audio codec via native `MediaExtractor`. When the codec is compatible with the target format, `-c:a copy` is used to directly copy the audio stream, avoiding unnecessary transcoding for near-instant extraction.

- **Cross-version Storage Compatibility** — Unified support across multiple Android generations: Android 10+ uses `MediaStore.Audio.Media.EXTERNAL_CONTENT_URI` for secure writes; transitional versions use `Documents Provider` to locate the Music folder; legacy devices use traditional file creation.

- **MD3 Interaction Polish** — Redesigned Slider and ExoPlayer controls to resolve the progress bar snap-back jitter caused by concurrent background media progress updates and user drag events in Jetpack Compose, achieving smooth, damped iOS-like interaction.

- **APK Size Reduction** — R8 code shrinking + resource shrinking + ABI architecture filtering. The private directory is automatically cleaned before each extraction to prevent unbounded data growth.

---

## 🔒 Permissions

This app follows the principle of least privilege and requires no sensitive permissions:

| Permission               | Description                                                                       |
|--------------------------|-----------------------------------------------------------------------------------|
| No Storage Permission    | Uses Android Photo Picker for video selection — no `READ_EXTERNAL_STORAGE` needed |
| No Network Permission    | All processing is done locally — no data is uploaded                              |
| No Background Permission | No background execution or notification permissions requested                     |

---

## 🤝 Contributing

Issues and Pull Requests are welcome!

1. Fork this repository
2. Create a feature branch: `git checkout -b feature/your-feature`
3. Commit your changes: `git commit -m "feat: add your feature"`
4. Push the branch: `git push origin feature/your-feature`
5. Open a Pull Request

---

## 📜 License

This project is open-sourced under the [MIT License](LICENSE). Feel free to clone, learn from, share, and build upon it.
