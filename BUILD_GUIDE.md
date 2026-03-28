# NovalPie Android APK 构建指南

## ✅ TTS 修复已完成

Java 源码已按照以下要求修复：
- ✅ 简化了 initTTS() 方法，使用系统默认引擎
- ✅ 强制设置中文语言环境 (Locale.CHINA)
- ✅ 实现了 UtteranceProgressListener 双向回调
- ✅ 简化了 speak() 和 stop() 方法
- ✅ 移除了多余的引擎选择和调试代码

---

## 📦 构建 APK 步骤

### 前置要求

确保你的开发环境已安装：
- ✅ Node.js (v14+)
- ✅ Java JDK (v11+)
- ✅ Android SDK
- ✅ Gradle (通过 Android Studio 或命令行)

### 构建步骤

#### 1. 安装项目依赖

```bash
cd /workspace/projects/novelapp/novalpie-app
npm install
```

#### 2. 构建前端资源

```bash
npm run build
```

#### 3. 同步到 Android 项目

```bash
npx cap sync android
```

#### 4. 构建 Debug APK

**方法 A：使用命令行**

```bash
cd android
./gradlew clean assembleDebug
```

**方法 B：使用 Android Studio**

```bash
npx cap open android
```

然后在 Android Studio 中：
- Build → Build Bundle(s) / APK(s) → Build APK(s)

---

## 📱 APK 输出位置

构建成功后，APK 文件位于：

```
/workspace/projects/novelapp/novalpie-app/android/app/build/outputs/apk/debug/app-debug.apk
```

---

## 🔧 安装到手机

### 使用 adb 安装

```bash
adb install /workspace/projects/novelapp/novalpie-app/android/app/build/outputs/apk/debug/app-debug.apk
```

### 或直接传输

1. 将 APK 文件传输到手机
2. 在手机上打开文件管理器
3. 点击 APK 文件安装
4. 如需权限，开启"允许安装未知来源应用"

---

## 🎯 验证修复效果

### 1. 查看日志

```bash
adb logcat | grep "NovalpieTTS"
```

期望输出：
```
D/NovalpieTTS: TTS 初始化成功，当前引擎: com.xiaomi.mibrain.speech
D/NovalpieTTS: 开始执行 JS 回调: window.onTtsStart()
D/NovalpieTTS: 开始执行 JS 回调: window.onTtsEnd()
```

### 2. 测试 TTS 功能

打开应用，点击听书按钮：
- ✅ 应该能听到语音播放
- ✅ 播放开始/结束状态正常
- ✅ 无卡死现象

---

## ⚠️ 常见问题

### 1. JAVA_HOME 未设置

**错误信息：**
```
ERROR: JAVA_HOME is not set and no 'java' command could be found
```

**解决方法：**

**macOS/Linux:**
```bash
# 安装 Java (macOS)
brew install openjdk@11

# 设置环境变量
export JAVA_HOME=/usr/local/opt/openjdk@11
export PATH=$JAVA_HOME/bin:$PATH

# 添加到 ~/.bashrc 或 ~/.zshrc
echo 'export JAVA_HOME=/usr/local/opt/openjdk@11' >> ~/.zshrc
echo 'export PATH=$JAVA_HOME/bin:$PATH' >> ~/.zshrc
```

**Windows:**
1. 下载并安装 JDK 11
2. 设置环境变量：
   - JAVA_HOME: `C:\Program Files\Java\jdk-11`
   - Path 添加: `%JAVA_HOME%\bin`

---

### 2. Android SDK 未找到

**错误信息：**
```
SDK location not found
```

**解决方法：**

创建 `local.properties` 文件：
```bash
cd /workspace/projects/novelapp/novalpie-app/android
echo "sdk.dir=/Users/你的用户名/Library/Android/sdk" > local.properties
```

或设置环境变量：
```bash
export ANDROID_HOME=/Users/你的用户名/Library/Android/sdk
export PATH=$PATH:$ANDROID_HOME/emulator
export PATH=$PATH:$ANDROID_HOME/platform-tools
```

---

### 3. Gradle 权限问题

**错误信息：**
```
./gradlew: Permission denied
```

**解决方法：**
```bash
cd /workspace/projects/novelapp/novalpie-app/android
chmod +x gradlew
```

---

### 4. 编译错误：找不到符号

**可能原因：**
- Java 代码有语法错误
- 依赖包缺失

**解决方法：**
```bash
# 清理并重新构建
./gradlew clean
./gradlew assembleDebug --stacktrace
```

查看详细错误日志，修复对应的 Java 代码。

---

## 📝 修改的文件清单

| 文件路径 | 修改内容 |
|---------|---------|
| `novalpie-app/android/app/src/main/java/com/novalpie/app/MainActivity.java` | 简化 initTTS() 方法，移除引擎选择逻辑，优化回调机制 |

---

## 🎉 修复完成

所有 Java 代码已修复完成！按照上述步骤构建并安装 APK，即可验证 TTS 功能是否正常工作。

如有问题，请查看 `adb logcat` 日志并参考故障排查部分。
