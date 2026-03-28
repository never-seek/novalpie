# TTS 最终修复报告（v6.0 - 系统权限打通版）

## 🎯 问题根源定位

### 核心问题
**App 无法"看见"系统中的 TTS 引擎！**

### 现象
- ✅ 系统设置显示：小米语音引擎和 Google TTS 都已安装
- ✅ 系统 TTS 示例可以正常播放
- ❌ App 诊断悬浮窗显示："❌ 引擎未就绪" 或引擎列表为空

### 根本原因

**Android 11+ 的包可见性限制！**

从 Android 11（API 30）开始，系统引入了"包可见性"机制。默认情况下，**App 无法看到系统中安装的其他应用和服务**。

如果 App 想要访问特定的系统服务（如 TTS），必须在 `AndroidManifest.xml` 中声明 `<queries>` 标签，明确告诉系统"我要查询这个服务"。

**之前的代码问题：**
```xml
<!-- AndroidManifest.xml 缺少 TTS 服务声明 -->
<manifest>
    <application>...</application>
    <!-- ❌ 没有声明要查询 TTS_SERVICE -->
</manifest>
```

**结果：** App 调用 `TextToSpeech` 时，系统会说"抱歉，我没看到任何 TTS 引擎"，即使引擎已经安装在系统中。

---

## ✅ 最终修复方案

### 修复 1：添加 TTS 服务查询权限（最关键！）

**位置：** `android/app/src/main/AndroidManifest.xml`

```xml
<manifest xmlns:android="http://schemas.android.com/apk/res/android"
    package="com.novalpie.app">

    <application>
        <!-- ... -->
    </application>

    <!-- ✅ 核心：声明 App 需要查询 TTS 服务 -->
    <!-- Android 11+ 包可见性：允许查询 TTS 引擎 -->
    <queries>
        <intent>
            <action android:name="android.intent.action.TTS_SERVICE" />
        </intent>
    </queries>

    <!-- 其他权限 -->
    <uses-permission android:name="android.permission.INTERNET" />
</manifest>
```

**关键点：**
- `<queries>` 必须放在 `<application>` 标签**外面**
- `<action android:name="android.intent.action.TTS_SERVICE" />` 声明要查询 TTS 服务
- 这一步是**最关键的修复**，没有它，App 就像在"真空"中，完全看不到系统引擎

### 修复 2：使用全局上下文初始化

**位置：** `MainActivity.java` 的 `initTTS()` 方法

**修复前：**
```java
tts = new TextToSpeech(this, status -> { ... });
```

**修复后：**
```java
// 使用 getApplicationContext() 提高连接稳定性
tts = new TextToSpeech(getApplicationContext(), status -> { ... });
```

**原因：**
- 在 WebView 环境中，`this` 可能指向 Activity 的代理对象
- 使用 `getApplicationContext()` 可以确保获得真正的系统上下文
- 提高 Service 绑定的成功率

### 修复 3：兼容性语言检测

**位置：** `MainActivity.java` 的 `initTTS()` 方法

**修复前：**
```java
if (langResult < TextToSpeech.LANG_AVAILABLE) {
    ttsInitialized = false; // 严格判定失败
}
```

**修复后：**
```java
// 兼容性处理：即使返回 -2 (不支持)，在某些国产系统上也能出声
if (langResult >= TextToSpeech.LANG_AVAILABLE || 
    langResult == TextToSpeech.LANG_NOT_SUPPORTED) {
    
    if (langResult == TextToSpeech.LANG_NOT_SUPPORTED) {
        report("⚠️ 语言返回不支持(-2)，但仍将尝试发声");
    }
    ttsInitialized = true; // 强制给 true
}
```

**原因：**
- 某些国产 ROM 的 TTS 引擎（如小米语音）会返回 `LANG_NOT_SUPPORTED (-2)`
- 但实际上引擎是可用的（可能依赖云端语音合成）
- 强制尝试发声，给用户一个机会

### 修复 4：移除严格校验，强制尝试发声

**位置：** `MainActivity.java` 的 `speak()` 方法

**修复前：**
```java
if (tts != null && ttsInitialized) {
    // 只有初始化成功才发声
} else {
    // 直接失败
}
```

**修复后：**
```java
// 即使 ttsInitialized 是 false，也强行尝试一次
if (tts != null) {
    int res = tts.speak(text, ...);
    if (res != TextToSpeech.SUCCESS) {
        // 失败时自动重连
        initTTS();
    }
}
```

**原因：**
- 某些引擎明明能用，却返回"不支持语言"
- 强制尝试发声，如果失败再自动重连
- 提高容错性，避免"误判"导致无声

---

## 📱 新 APK 信息

**文件名：** `novalpie-app-final-v6.apk`  
**文件大小：** 5.75 MB  
**构建时间：** 2025-03-27 19:35  
**有效期：** 180 天（至 2025-09-23）

### 🔗 下载链接
```
https://coze-coding-project.tos.coze.site/coze_storage_7621867712305922067/novalpie-app-final-v6_5734944c.apk?sign=1790163714-78e230ef8e-0-c3eb90af8ece3ae07ee497df045ca8fe7ce1f4d74f7b7daaac4d1a9c9b61ceb7
```

---

## 🧪 预期效果

### 安装新 APK 后，应该看到：

**诊断悬浮窗日志（正常流程）：**
```
[19:35:10] ✅ 诊断窗口已就绪
[19:35:12] 📋 系统可用引擎:
[19:35:12]   • com.xiaomi.mibrain.speech (小米语音)
[19:35:12]   • com.google.android.tts (Google 文字转语音)
[19:35:12] 📡 使用全局上下文连接引擎...
[19:35:12] ✅ 引擎实例连接成功: com.xiaomi.mibrain.speech
[19:35:12] ✅ 语言检查通过: LANG_AVAILABLE (0)
[19:35:15] 📤 前端调用 speak: 第一章 风起云涌...
[19:35:15] 🎤 强制尝试朗读: 第一... (200字)
[19:35:15] 🔊 音频焦点获取成功
[19:35:15] ✅ Speak 调用成功，等待引擎发声...
[19:35:15] ▶️ 引擎开始发声...
（听到语音播放）🎵
[19:35:20] 🏁 引擎发声结束
[19:35:20] 🏁 前端收到 onEnd
```

### 关键改进：

1. **引擎列表不再为空**
   - 之前：显示 "⚠️ 未检测到任何 TTS 引擎"
   - 现在：显示小米语音和 Google TTS

2. **引擎连接成功**
   - 之前：显示 "❌ 引擎连接失败"
   - 现在：显示 "✅ 引擎实例连接成功"

3. **语言检测通过**
   - 之前：可能显示 "❌ 语言不支持"
   - 现在：显示 "✅ 语言检查通过" 或兼容性提示

4. **真正发声**
   - 之前：流程正常但无声音
   - 现在：**能听到语音播放** 🎵

---

## 🔍 如果仍然没声音

### 检查项 1：媒体音量
**设置 → 声音与振动 → 媒体音量**

**重要：** 是媒体音量，不是铃声音量！

### 检查项 2：系统 TTS 示例
**设置 → 无障碍 → 文字转语音 → 播放示例**

如果示例也没声音，说明：
- 引擎语音包未下载
- 引擎本身故障
- 需要切换到其他引擎

### 检查项 3：查看悬浮窗日志
如果悬浮窗显示：
- "❌ 语言返回不支持(-2)，但仍将尝试发声" → 应该能听到声音
- "✅ Speak 调用成功，等待引擎发声..." → 如果还是没声音，检查音量
- "▶️ 引擎开始发声..." 后立刻 "🏁 引擎发声结束" → 引擎在假读，尝试切换引擎

### 尝试切换引擎
1. 设置 → 无障碍 → 文字转语音
2. 切换到 "Google 文字转语音服务"
3. 点击"播放示例"验证
4. 重试 NovalPie 听书

---

## 📊 修复对比表

| 修复项 | v5.0 (诊断版) | v6.0 (最终版) |
|--------|---------------|---------------|
| Manifest 权限 | ❌ 未声明 | ✅ 已声明 TTS_SERVICE |
| 上下文 | `this` | ✅ `getApplicationContext()` |
| 语言检测 | 严格判定 | ✅ 兼容性处理（-2 也尝试） |
| speak 校验 | 需要 `ttsInitialized=true` | ✅ 强制尝试，失败重连 |
| 引擎可见性 | ❌ 无法看到引擎 | ✅ 可以枚举引擎列表 |

---

## 💡 技术要点总结

### Android 11+ 包可见性机制

**背景：**
- Android 11（API 30）引入包可见性限制
- 默认情况下，App 无法看到系统中安装的其他应用
- 这是为了提高隐私和安全性

**影响：**
- 如果不声明 `<queries>`，`TextToSpeech.getEngines()` 会返回空列表
- `TextToSpeech` 初始化可能失败或连接到错误的引擎
- App 像在"真空"中，无法访问系统服务

**解决方案：**
```xml
<queries>
    <intent>
        <action android:name="android.intent.action.TTS_SERVICE" />
    </intent>
</queries>
```

### 全局上下文 vs Activity 上下文

**Activity 上下文（this）：**
- 可能指向代理对象
- 在 WebView 环境中可能不稳定
- Service 绑定可能失败

**全局上下文（getApplicationContext()）：**
- 始终指向真正的系统上下文
- 生命周期独立于 Activity
- 更适合 Service 绑定

### 国产 ROM 兼容性

**问题：**
- 小米、华为等国产 ROM 的 TTS 引擎可能返回 `LANG_NOT_SUPPORTED (-2)`
- 但实际上引擎是可用的（可能依赖云端合成）

**解决：**
- 不严格依赖 `setLanguage()` 的返回值
- 强制尝试发声
- 给用户一个机会

---

## 🎉 最终状态

**修复完成时间：** 2025-03-27 19:35  
**版本号：** v6.0-system-visibility-final  
**状态：** ✅ 系统权限打通完成

**核心修复：**
1. ✅ AndroidManifest.xml 添加 `<queries>` 声明 TTS_SERVICE
2. ✅ initTTS() 使用 `getApplicationContext()`
3. ✅ 语言检测兼容性处理（-2 也尝试）
4. ✅ speak() 移除严格校验，强制尝试发声
5. ✅ 失败时自动重连

---

## 📚 相关文档

- **TTS_DIAGNOSTIC_GUIDE.md** - 诊断版使用指南（v5.0）
- **TTS_AUDIO_CHANNEL_FIX.md** - 音频通道修复（v4.0）
- **TTS_EMPTY_READ_FIX.md** - 空转跳读修复（v3.0）
- **TTS_THREAD_FIX.md** - 线程崩溃修复（v2.0）
- **BUILD_SUCCESS.md** - 第一次构建报告（v1.0）

---

**如果这次还有问题，请提供：**
1. 悬浮窗截图（特别是引擎列表和初始化状态）
2. 系统设置中的 TTS 引擎配置截图
3. 系统 TTS 示例是否能播放
4. 手机型号和系统版本
