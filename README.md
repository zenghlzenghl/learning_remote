# 📱 Learning Remote - 学习型遥控器 APP

[![Build APK](https://github.com/YOUR_USERNAME/learning_remote/actions/workflows/build-apk.yml/badge.svg)](https://github.com/YOUR_USERNAME/learning_remote/actions/workflows/build-apk.yml)

Android 手机应用，用于控制 ESP32-S3 学习型遥控器。

## ✨ 功能特性

- 📺 **红外遥控 (IR)** - 学习和发射红外遥控信号
- 📡 **无线遥控 (RF)** - 学习和发射 433MHz 射频信号
- 💾 **信号存储** - 按遥控器 ID 分类保存信号
- 🔄 **一键重放** - 点击即可发射已保存的信号
- 📊 **实时显示** - 解码结果实时推送到手机

## 🚀 快速开始

### 方法 1：下载预编译 APK（推荐）

1. 访问 [Releases 页面](../../releases)
2. 下载最新的 `app-debug.apk`
3. 传输到手机并安装

### 方法 2：手动编译

**前置要求**：
- Android Studio Hedgehog 或更新版本
- JDK 17+
- Android SDK API 33+

**编译步骤**：
```bash
# 克隆项目
git clone https://github.com/YOUR_USERNAME/learning_remote.git
cd learning_remote/android_app

# 编译 Debug 版本
./gradlew assembleDebug

# APK 位置
# app/build/outputs/apk/debug/app-debug.apk
```

## 📱 使用说明

### 1. 连接 ESP32-S3

1. 确保 ESP32-S3 已烧录固件并与手机在同一 WiFi 网络
2. 查看 ESP32-S3 串口日志获取 IP 地址
3. 打开 APP，输入：
   - **IP 地址**: ESP32-S3 的 IP（如 192.168.1.100）
   - **端口**: 8888
4. 点击 **Connect**

### 2. 学习遥控信号（Decode 标签）

1. 切换到 **🔍 Decode** 标签页
2. 将原遥控器对准 ESP32-S3 的接收模块
3. 按下遥控器按键
4. APP 显示解码结果
5. 填写信息后点击 **Save**
   - 选择类型：IR / RF
   - 输入遥控器 ID（如 "TV_SONY"、"AC_GREE"）
   - 输入按键名称（如 "Power"、"Volume+"）

### 3. 发射信号（IR Remote / RF Remote 标签）

1. 切换到 **📺 IR Remote** 或 **📡 RF Remote** 标签
2. 查看按遥控器 ID 分组的信号列表
3. 点击 **▶ Send** 发射对应信号
4. 被控设备响应！

### 4. 管理信号

- **删除信号**: 长按或点击删除按钮
- **查看详情**: 点击信号项查看详细信息

## 🔌 硬件连接

| 功能 | ESP32-S3 GPIO | 模块 |
|------|--------------|------|
| IR 接收 | GPIO 4 | VS1838B |
| IR 发射 | GPIO 5 | 红外 LED |
| RF 接收 | GPIO 6 | 433MHz 接收模块 |
| RF 发射 | GPIO 7 | 433MHz 发射模块 |

## 🛠️ 技术栈

- **语言**: Kotlin
- **最低 SDK**: API 24 (Android 7.0)
- **目标 SDK**: API 34 (Android 14)
- **架构**: MVVM + ViewBinding
- **网络**: TCP Socket
- **异步**: Coroutines + Flow

## 📦 项目结构

```
android_app/
├── app/
│   └── src/main/
│       ├── java/com/example/learningremote/
│       │   ├── MainActivity.kt      # 主界面
│       │   ├── network/
│       │   │   └── TcpClient.kt    # TCP 通信
│       │   └── adapter/
│       │       └── SignalAdapter.kt # 列表适配器
│       └── res/
│           ├── layout/              # UI 布局
│           └── values/              # 资源文件
├── .github/workflows/
│   └── build-apk.yml               # CI/CD 配置
├── build.gradle                    # 项目级构建配置
├── settings.gradle                 # 项目设置
└── README.md                       # 本文档
```

## 🤝 贡献

欢迎提交 Issue 和 Pull Request！

## 📄 许可证

MIT License

## 👨‍💻 作者

Your Name

---

**⭐ 如果这个项目对你有帮助，请给一个 Star！⭐**