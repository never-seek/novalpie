# NovalPie TTS 修复指南

## 修复内容概览

本次修复解决了 Android WebView 应用无法连接手机 TTS 引擎（小米语音/谷歌语音）的问题。

### 核心修复点

#### 1. **TTS Polyfill 增强** (`MainActivity.java` 第 110-135 行)
   
**问题：** 原有 Polyfill 过于简单，缺少状态回调机制，前端无法得知 TTS 播放状态。

**修复：**
```javascript
// 新增功能：
- window.currentUtterance：跟踪当前播放的语音对象
- window.onTtsStart()：原生安卓调用，通知前端 TTS 开始
- window.onTtsEnd()：原生安卓调用，通知前端 TTS 结束
- getVoices()：返回 mock 数据，兼容前端框架校验
```

**效果：** 前端可以通过 `utterance.onstart` 和 `utterance.onend` 回调监听播放状态。

---

#### 2. **TTS 引擎自动选择** (`MainActivity.java` 第 375-430 行)

**问题：** 未指定具体 TTS 引擎，可能使用不合适的引擎导致无声音。

**修复：**
- 优先尝试 **小米系统语音引擎** (`com.xiaomi.mibrain.speech`)
- 备选小米引擎 (`com.miui.tts`，部分 MIUI 版本)
- 再次尝试 **谷歌语音引擎** (`com.google.android.tts`)
- 最后使用系统默认引擎

**新增方法：**
- `logAvailableEngines()`：枚举并打印所有可用引擎
- `setBestTTSEngine()`：自动选择最佳引擎
- `switchTTSEngine()`：安全切换引擎

---

#### 3. **双向回调机制** (`MainActivity.java` 第 322-348 行)

**问题：** 原生 TTS 的进度监听器未回调给前端，导致前端卡死。

**修复：**
```java
tts.setOnUtteranceProgressListener(new UtteranceProgressListener() {
    @Override
    public void onStart(String utteranceId) {
        executeScript("if(window.onTtsStart) window.onTtsStart();");
    }

    @Override
    public void onDone(String utteranceId) {
        executeScript("if(window.onTtsEnd) window.onTtsEnd();");
    }

    @Override
    public void onError(String utteranceId) {
        executeScript("if(window.onTtsEnd) window.onTtsEnd();");
    }
});
```

---

#### 4. **speak 方法优化** (`MainActivity.java` 第 450-456 行)

**问题：** 未传入 `utteranceId`，导致进度监听器不生效。

**修复：**
```java
// 修改前：
tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "novalpie_tts");

// 修改后：
tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "novalpie_id");
// 必须传入固定的 utteranceId，监听器才能触发
```

---

#### 5. **新增调试接口** (`MainActivity.java` 第 470-483 行)

**新增方法：** `getEngines()`
```java
@JavascriptInterface
public String getEngines() {
    // 返回 JSON 格式的引擎列表
    // 示例：[{"name":"com.xiaomi.mibrain.speech","label":"小米语音"}]
}
```

**前端调用：**
```javascript
console.log(AndroidTTS.getEngines()); // 查看所有可用引擎
```

---

## 使用方法

### 1. 手机端配置（重要！）

**小米手机：**
1. 打开 **设置 → 更多设置 → 无障碍 → 文字转语音**
2. 选择引擎：
   - **小米语音引擎**（推荐）
   - **Google 文字转语音**
3. 点击引擎 → **下载离线语音包**（否则可能无法播放）
4. 调整 **媒体音量**（非铃声音量）

**其他手机：**
- 确保已安装并启用 TTS 引擎
- 下载对应语言的离线语音包

---

### 2. 编译并安装 APK

```bash
# 进入项目目录
cd novalpie-app

# 安装依赖
npm install

# 编译前端
npm run build

# 同步到 Android
npx cap sync android

# 打开 Android Studio 编译
npx cap open android
```

或在 Android Studio 中：
- Build → Build Bundle(s) / APK(s) → Build APK(s)
- 安装到手机

---

### 3. 调试方法

#### 查看日志（adb logcat）
```bash
# 过滤 NovalPie 相关日志
adb logcat | grep "NovalPie-Main"

# 查看关键信息：
# - 可用的 TTS 引擎列表
# - 已切换到哪个引擎
# - TTS 播放开始/结束状态
```

#### 浏览器控制台测试
在 Chrome DevTools 中（需启用 WebView 调试）：
```javascript
// 1. 查看可用引擎
console.log(AndroidTTS.getEngines());

// 2. 测试 TTS
const utterance = new SpeechSynthesisUtterance("你好，这是测试语音");
utterance.onstart = () => console.log("开始播放");
utterance.onend = () => console.log("播放结束");
speechSynthesis.speak(utterance);

// 3. 停止播放
speechSynthesis.cancel();
```

---

## 常见问题排查

### 1. 没有声音

**检查项：**
- ✅ 手机媒体音量是否打开
- ✅ 对应引擎的离线语音包是否已下载
- ✅ 查看日志是否显示 "TTS 初始化成功"
- ✅ 查看日志是否显示 "TTS 开始朗读"

**解决方法：**
- 小米手机：设置 → 无障碍 → 文字转语音 → 下载语音包
- 调大媒体音量（不是铃声音量）
- 查看日志确认引擎切换成功

---

### 2. 引擎切换失败

**查看日志：**
```
adb logcat | grep "TTS 引擎"
```

**可能原因：**
- 引擎包名写错（不同 MIUI 版本可能不同）
- 引擎未安装或被禁用

**解决方法：**
- 查看日志中的 "可用的 TTS 引擎" 列表
- 根据实际可用引擎调整代码（`setBestTTSEngine` 方法）

---

### 3. 前端卡死或无反应

**检查项：**
- ✅ 查看日志是否有 "TTS 开始朗读" 和 "TTS 朗读完成"
- ✅ 浏览器控制台是否有 JS 错误
- ✅ `window.onTtsStart` 和 `window.onTtsEnd` 是否正确注入

**调试方法：**
```javascript
// 在控制台测试注入是否成功
console.log(typeof window.speechSynthesis); // 应该输出 "object"
console.log(typeof window.onTtsStart); // 应该输出 "function"
```

---

## 技术原理

### 架构流程图

```
┌─────────────────┐
│  前端 JavaScript │
│  speechSynthesis │
└────────┬────────┘
         │ speak(text)
         ↓
┌─────────────────┐
│ WebView Polyfill │ ← window.AndroidTTS.speak(text)
└────────┬────────┘
         │ JavascriptInterface
         ↓
┌─────────────────┐
│  Android Java   │
│  WebAppInterface│
└────────┬────────┘
         │ tts.speak()
         ↓
┌─────────────────┐
│  TextToSpeech   │ ← 小米/谷歌引擎
│  (原生 API)      │
└────────┬────────┘
         │ UtteranceProgressListener
         ↓
┌─────────────────┐
│  回调 WebView    │
│  onTtsStart/End │
└─────────────────┘
```

### 关键技术点

1. **Polyfill 欺骗：** 前端以为有 Web Speech API，实际调用原生 AndroidTTS
2. **双向绑定：** 原生 TTS 状态通过 JS 回调通知前端
3. **引擎选择：** 自动检测并切换到最佳引擎
4. **utteranceId：** 必须传入固定 ID，进度监听器才能触发

---

## 文件修改列表

| 文件路径 | 修改内容 | 行号 |
|---------|---------|------|
| `novalpie-app/android/app/src/main/java/com/novalpie/app/MainActivity.java` | TTS Polyfill 注入 | 110-135 |
| `novalpie-app/android/app/src/main/java/com/novalpie/app/MainActivity.java` | initTTS 方法 | 311-358 |
| `novalpie-app/android/app/src/main/java/com/novalpie/app/MainActivity.java` | 引擎枚举和选择 | 375-430 |
| `novalpie-app/android/app/src/main/java/com/novalpie/app/MainActivity.java` | speak 方法优化 | 450-456 |
| `novalpie-app/android/app/src/main/java/com/novalpie/app/MainActivity.java` | getEngines 调试接口 | 470-483 |

---

## 下一步优化建议

1. **前端增强：**
   - 添加音量调节接口（`tts.setVolume()`）
   - 添加语速调节 UI（`tts.setSpeechRate()`）
   - 添加暂停/恢复功能

2. **引擎管理：**
   - 提供前端选择引擎的接口
   - 记住用户选择的引擎偏好

3. **错误处理：**
   - 前端显示更友好的错误提示
   - 引擎不可用时提供下载引导

4. **性能优化：**
   - 长文本分段播放
   - 缓存已合成的语音

---

## 联系与反馈

如有问题，请查看日志并提交 Issue：
- 标题格式：[TTS 问题] 简要描述
- 必须附上 `adb logcat | grep "NovalPie"` 日志
- 说明手机型号和系统版本

---

**修复完成时间：** 2025-03-27  
**修复版本：** v1.0.0-tts-fix
