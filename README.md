# 音频提取器 (AudioPeel)

**音频提取器** 是一款基于 Android 的轻量级、无广告且完全免费的视频转音频提取工具。
只需一小会儿，你就可以从手机相册的任意视频中完美抽离出所需的音频片段，并保存为你喜欢的格式！

## 📸 截图展示
<div>
  <img src="art/screenshot.png" alt="AudioPeel 截图" />
</div>

---

## 🌟 核心功能特性 (Features)

- **📸 极简视频选择器**  
  原生支持 Android 新版照片选择器 (`Photo Picker`)，极简安全地读取和加载设备里的视频文件。
  
- **🎵 多格式高保真无损输出**  
  内置强大的 `FFmpegKit` 作为转换引擎，可自由随心选择你需要的音频格式：
  - **MP3**: 主流首选的高兼容格式
  - **AAC**: 相对占用小且高品质
  - **WAV**: 无损原声格式，提供最佳后续剪辑质量
  - **FLAC**: 高品质无损压缩格式
  - **OGG**: 高效压缩格式
  
- **⏱️ 实时进度展示**  
  在转换或提取的进程中，直观显示解析与输出进度的环形进度条和真实的百分比读数。
  
- **✏️ 自定义命名 & 📂 专属文件管理**  
  - 提取后可以直接重命名音频文件，避免千篇一律的数字乱码名字。
  - 所提取出的录音/音乐均会自动保存在你手机目录深处的专属文件夹 `/Music/AudioPeel` 里。
  - 支持一键调起 Android 系统文件管理器跳转至专属音频抽屉。

- **🔗 应用内快捷分享**  
  通过 Android `FileProvider` 实现全方位文件共享，提取成功后能立刻以文件的形式安全地分享至微信、QQ、Telegram 甚至 AirDrop/互传 等任何常用通讯工具中。

---

## 🚀 技术栈与架构 (Tech Stack)

该应用遵循 Google 推荐的最佳架构实践，完全使用原生且现代化的解决方案构建：
- **开发语言**: 100% Kotlin
- **UI 框架**: Jetpack Compose (采用 Material Design 3 现代视效、全屏留白兼容输入法自适应)
- **多媒体处理架构**: FFmpegKit (`com.mrljdx:ffmpeg-kit-full:6.1.4`) 用于异步媒体指令操作。
- **架构模式**: MVVM (Model-View-ViewModel) 和基于 `StateFlow` 的可观察响应式单向数据流机制。
- **文件存储规范**: 采用了完全兼容 Android 10+ 的 Scoped Storage（分区存储）的最新 Android SDK MediaStore 媒体插入法则。

---

## 💻 本地编译指南 (Build Settings)

如果您想对本作进行二开，可以按照下述环境要求进行配置编译。

### 环境依赖
1. **Android Studio** (推荐版本 Iguana | 2023.2.1 或更新)
2. **Java 17+** 
3. **Android SDK Level 34+** (建议 Target SDK API 34+ 以确保完全兼容 SAF 文件选择器及通知等新系统特效)

### 编译步骤
1. 克隆本项目：
   ```bash
   git clone https://github.com/tianxing-ovo/AudioPeel.git
   ```
2. 使用 Android Studio 打开项目根目录。
3. 等待 Gradle 同步拉取诸如 Compose Material3、FFmpegKit 等核心库。
4. 插入实体机或配置好环境的 Android 模拟器。
5. 点击上方工具栏的运行 (`Run 'app'`) 或键入指令 `./gradlew assembleDebug`。

---

## 💡 开发历程与已知优化项
在这个工程的创建过程中，已自动解决并修复了诸多 Android 碎片化开发会碰到的历史遗留难题。如果你也是 Android 开发者，希望能为你提供到微小的思路帮助：

- 修复了 `SAF (Storage Access Framework)` 返回给 `ffmpeg` 时报错的不可识别路径问题（采取自动写入同沙盒环境 cache 再丢弃的策略）。
- 梳理了新老旧几代 Android 的保存机制；对于 Android O 等过渡版本通过 Documents Provider 精确锚定 Music 文件夹实现快速路径定位，对更古董的设备则执行传统创建方法。
- 对于 Android 10+ 以上系统采用安全的 `MediaStore.Audio.Media.EXTERNAL_CONTENT_URI` 指绝对相对路径写入，而不再申请全盘外置存储强读权限，让 APP 变成一款绝对干净绿色的纯净工具。

---

## 📜 许可证 (License)

本项目基于 [MIT License](LICENSE) 协议开源。欢迎任何出于学习、分享和二次创作的克隆使用。
