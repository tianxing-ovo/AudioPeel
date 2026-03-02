# 🎵 音频提取器 (AudioPeel)

**中文 | [English](README_EN.md)**

一款轻量级 Android 视频转音频工具，支持快速提取并导出为 MP3 / M4A / WAV / FLAC / OGG 格式，无广告且完全免费

![Version](https://img.shields.io/badge/Version-1.2-blue?style=for-the-badge)
![Min SDK](https://img.shields.io/badge/Min_SDK-24_(Android_7.0)-orange?style=for-the-badge)
[![Download APK](https://img.shields.io/badge/Download-APK-brightgreen?style=for-the-badge&logo=android&logoColor=white)](https://github.com/tianxing-ovo/AudioPeel/releases/latest)

![Android](https://img.shields.io/badge/Android-3DDC84?style=for-the-badge&logo=android&logoColor=white)
![Kotlin](https://img.shields.io/badge/Kotlin-0095D5?&style=for-the-badge&logo=kotlin&logoColor=white)
![Jetpack Compose](https://img.shields.io/badge/Jetpack_Compose-4285F4?style=for-the-badge&logo=jetpackcompose&logoColor=white)
![FFmpeg](https://img.shields.io/badge/FFmpeg-007808?style=for-the-badge&logo=ffmpeg&logoColor=white)

## 📸 界面展示

![AudioPeel 截图](art/screenshot.png)

---

## 📋 目录

- [🎵 音频提取器 (AudioPeel)](#-音频提取器-audiopeel)
  - [📸 界面展示](#-界面展示)
  - [📋 目录](#-目录)
  - [🌟 核心功能特性](#-核心功能特性)
  - [🚀 技术栈与架构](#-技术栈与架构)
  - [💻 本地编译指南](#-本地编译指南)
    - [环境依赖](#环境依赖)
    - [编译步骤](#编译步骤)
  - [💡 开发历程与技术探索](#-开发历程与技术探索)
  - [🔒 权限说明](#-权限说明)
  - [🤝 参与贡献](#-参与贡献)
  - [📜 许可证 (License)](#-许可证-license)

---

## 🌟 核心功能特性

- **极简视频选择** — 原生支持 Android Photo Picker，无需额外申请读写权限，安全读取设备视频。

- **多格式灵活输出** — 支持 MP3、M4A、WAV、FLAC、OGG 五种音频格式，覆盖从高兼容到无损的全场景需求。

- **智能格式推荐** — 自动检测视频内嵌音频编码，智能选中最佳输出格式（如 AAC→M4A），优先直接复制实现秒级提取。

- **极速提取** — SAF 直读跳过文件拷贝、智能流复制避免不必要转码、多线程充分利用多核性能，18 分钟视频仅需数秒。

- **实时进度展示** — 环形进度条 + 百分比读数，实时呈现提取进度，支持随时取消。

- **内置播放预览** — 基于 Media3 ExoPlayer 的流畅试听体验，支持丝滑的进度拖拽。

- **文件管理** — 支持自定义重命名，自动保存至 `/Music/AudioPeel`，一键打开系统文件管理器查看。

- **快捷分享** — 基于 FileProvider 的安全文件共享，支持微信、QQ、Telegram 等主流应用。

- **极致包体** — R8 压缩混淆 + 资源压缩 + ABI 过滤，确保安装包体积最小化。

---

## 🚀 技术栈与架构

本项目遵循 Google 推荐的现代化架构实践，完全使用原生方案构建：

| 模块    | 技术选型                                                   |
|-------|--------------------------------------------------------|
| 开发语言  | 100% Kotlin                                            |
| UI 框架 | Jetpack Compose + Material Design 3 + Edge-to-Edge     |
| 多媒体处理 | FFmpegKit (`com.mrljdx:ffmpeg-kit-full`)               |
| 音频引擎  | AndroidX Media3 ExoPlayer                              |
| 媒体探测  | Android 原生 `MediaExtractor` + `MediaMetadataRetriever` |
| 架构模式  | MVVM + `StateFlow` 单向数据流                               |
| 文件存储  | Scoped Storage + MediaStore API（兼容 Android 10+）        |
| 包体优化  | R8 压缩 + 资源压缩 + ABI 过滤（`arm64-v8a`, `x86_64`）           |

---

## 💻 本地编译指南

### 环境依赖

1. **Android Studio**（推荐版本 Iguana | 2023.2.1 或更新）
2. **Java 11+**（注：编译需兼容 Gradle 版本）
3. **Android SDK Level 36+**（建议 Target SDK API 36）

> **兼容性：** 最低支持 Android 7.0（API 24），目标 SDK API 36。

### 编译步骤

1. 克隆本项目：
   ```bash
   git clone https://github.com/tianxing-ovo/AudioPeel.git
   ```
2. 使用 Android Studio 打开项目根目录。
3. 等待 Gradle 同步拉取依赖。
4. 连接实体手机或配置好的 Android 模拟器。
5. 点击 `Run 'app'` 或在 Terminal 执行 `./gradlew assembleDebug`。

---

## 💡 开发历程与技术探索

在工程演进过程中，我们解决了多个 Android 碎片化开发中的典型难题，希望能为遇到同类问题的开发者提供思路：

- **SAF 直读优化** — 使用 `FFmpegKitConfig.getSafParameterForRead()` 直接从 `content://` URI 读取视频，彻底消除文件复制到缓存目录的耗时步骤。同时为 FFprobe 和 FFmpeg 分配独立的 SAF 文件描述符，解决了 `SAF id not found` 问题。

- **智能流复制加速** — 通过原生 `MediaExtractor` 检测源音频编码，当编码与目标格式兼容时自动使用 `-c:a copy` 直接复制音频流，避免不必要的转码，实现秒级提取。

- **全版本媒体存储兼容** — 梳理了多代 Android 的路径保存机制：Android 10+ 采用 `MediaStore.Audio.Media.EXTERNAL_CONTENT_URI` 安全写入；过渡版本通过 `Documents Provider` 锚定 Music 文件夹；老版本执行传统物理创建。

- **MD3 交互精调** — 重新设计了 Slider 和 ExoPlayer 控制器，攻克了 Jetpack Compose 中后台进度更新与用户拖拽冲突导致的弹回抖动问题，实现细腻的阻尼级交互体验。

- **包体瘦身** — R8 压缩混淆 + 资源压缩 + ABI 架构过滤，私有目录在每次提取前自动清理历史文件，防止应用数据无限膨胀。

---

## 🔒 权限说明

本应用遵循最小权限原则，无需任何敏感权限：

| 权限    | 说明                                                      |
|-------|---------------------------------------------------------|
| 无存储权限 | 使用 Android Photo Picker 选取视频，无需 `READ_EXTERNAL_STORAGE` |
| 无网络权限 | 所有处理均在本地完成，不上传任何数据                                      |
| 无后台权限 | 不申请后台运行或通知权限                                            |

---

## 🤝 参与贡献

欢迎提交 Issue 和 Pull Request！

1. Fork 本仓库
2. 创建特性分支：`git checkout -b feature/your-feature`
3. 提交更改：`git commit -m "feat: add your feature"`
4. 推送分支：`git push origin feature/your-feature`
5. 发起 Pull Request

---

## 📜 许可证 (License)

本项目基于 [MIT License](LICENSE) 协议开源。欢迎任何出于学习、分享和二次创作的使用。