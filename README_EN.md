# AudioPeel

[![License](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)
[![Version](https://img.shields.io/badge/Version-1.2-green)](https://github.com/tianxing-ovo/AudioPeel/releases/latest)
[![Downloads](https://img.shields.io/github/downloads/tianxing-ovo/AudioPeel/total?cacheSeconds=86400)](https://github.com/tianxing-ovo/AudioPeel/releases)
[![Latest Downloads](https://img.shields.io/github/downloads/tianxing-ovo/AudioPeel/latest/total?cacheSeconds=86400)](https://github.com/tianxing-ovo/AudioPeel/releases/latest)

[简体中文](README.md) | [English](README_EN.md)

A lightweight Android video-to-audio tool. Export to MP3 / M4A / WAV / FLAC / OGG — ad-free and free to use.

## Features

- **Video picker** — Uses Android Photo Picker, so selecting a video needs no storage read/write permission
- **Multiple formats** — Supports MP3 / M4A / WAV / FLAC / OGG
- **Smart suggestion** — Detects embedded audio codec and suggests a format such as AAC→M4A, preferring stream copy when compatible
- **Fast extraction** — SAF (Storage Access Framework) direct read reduces copies, stream-copies the audio track when codecs match, and bounds FFmpeg thread count
- **Progress & cancel** — Circular progress with percentage, and cancel support
- **Preview** — Media3 ExoPlayer preview with more stable seeking and less jump-back
- **File management** — Rename and save under `/Music/AudioPeel`, works across Android storage models, and clears old private-output files before each extraction
- **Share** — FileProvider sharing to apps such as WeChat, QQ, and Telegram

## Quick Start

### Requirements

- Android 7.0 or later (API 24)

### Install

1. Download the latest APK from [Releases](https://github.com/tianxing-ovo/AudioPeel/releases/latest)
2. Install and open the app
3. Pick a video, confirm the output format, then extract

## Screenshots

![](art/screenshot.png)

## Tech Stack

|       Area       |                            Choice                             |
|:----------------:|:-------------------------------------------------------------:|
|     Language     |                          100% Kotlin                          |
|        UI        |      Jetpack Compose + Material Design 3 + Edge-to-Edge       |
| Media processing |                           FFmpegKit                           |
|  Audio playback  |                   AndroidX Media3 ExoPlayer                   |
|  Media probing   |          `MediaExtractor` + `MediaMetadataRetriever`          |
|   Architecture   |                      MVVM + `StateFlow`                       |
|       Size       | R8 + resource shrinking + ABI filters (`arm64-v8a`, `x86_64`) |

## Build

### Prerequisites

1. **Android Studio** (a recent stable release recommended)
2. **Java 11+** (compatible with the project Gradle version)
3. **Android SDK** (`minSdk` 24, `compileSdk` / `targetSdk` 37)

### Steps

1. Clone the repository:
   ```bash
   git clone https://github.com/tianxing-ovo/AudioPeel.git
   ```
2. Open the project root in Android Studio
3. Wait for Gradle sync
4. Connect a device or emulator
5. Run `Run 'app'`, or `./gradlew assembleDebug`

## Permissions

Least-privilege by design; no sensitive permissions requested:

|                 Note                  |                                        Detail                                         |
|:-------------------------------------:|:-------------------------------------------------------------------------------------:|
|         No storage permission         | Video picking uses Photo Picker and `READ_EXTERNAL_STORAGE` is generally not required |
|         No network permission         |                     Fully offline processing with no data upload                      |
| No background/notification permission |                                     Not requested                                     |

## Contributing

Issues and Pull Requests are welcome:

1. Fork this repository
2. Create a branch: `git checkout -b feature/your-feature`
3. Commit: `git commit -m "feat: add your feature"`
4. Push to your fork remote and open a Pull Request

## License

This project is released under the [MIT License](LICENSE)
