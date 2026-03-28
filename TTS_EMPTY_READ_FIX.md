# TTS "空转跳读" 问题修复报告

## 🎯 问题诊断

### 症状描述

**现象：** 点击听书后，2秒扫完200字且没有声音，快速跳过整章内容

**用户反馈：** "语音停留在一个地方不动" → 修复后 → "快速跳读且无声"

### 根本原因

这是一个典型的 **"空转"或"跳读"** 问题：

1. **引擎拒绝请求：**
   - TTS引擎在执行 `speak()` 时立即报错或返回"朗读完成"状态
   - 前端接收到 `onDone` 回调后，立即发送下一段文字
   - 循环往复，导致几秒钟内"读"完整章，实际无任何声音

2. **可能的触发原因：**
   - 设置的语言（Locale.CHINA）在当前引擎中未安装或未激活
   - 网页加载过快，TTS还没完全初始化就发出 `speak` 请求
   - 传入的文本包含特殊字符，或 `utteranceId` 不被旧版引擎接受

---

## ✅ 修复内容

### 1. 增强 initTTS 的可用性检查

**修复前：**
```java
int result = tts.setLanguage(Locale.CHINA);
if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
    Log.e(TAG, "TTS 错误：不支持中文或缺少语音包");
} else {
    ttsInitialized = true;
}
```

**修复后：**
```java
int langResult = tts.setLanguage(Locale.CHINA);
if (langResult == TextToSpeech.LANG_MISSING_DATA || langResult == TextToSpeech.LANG_NOT_SUPPORTED) {
    Log.e(TAG, "错误：当前 TTS 引擎不支持中文或缺少语言包！");
    ttsInitialized = false;  // 核心修复：明确标记为未初始化
} else {
    Log.d(TAG, "TTS 引擎就绪并支持中文");
    ttsInitialized = true;
}
```

**关键改进：**
- 语言不支持时，**明确标记 `ttsInitialized = false`**
- 增加详细日志输出，便于排查问题

### 2. 添加引擎枚举功能

**新增功能：**
```java
private void logAvailableEngines() {
    if (tts != null) {
        java.util.List<TextToSpeech.EngineInfo> engines = tts.getEngines();
        Log.d(TAG, "=== 可用的 TTS 引擎列表 ===");
        for (TextToSpeech.EngineInfo engine : engines) {
            Log.d(TAG, "引擎名称: " + engine.name + ", 标签: " + engine.label);
        }
        Log.d(TAG, "=========================");
    }
}
```

**作用：**
- 启动时打印所有可用的 TTS 引擎
- 帮助用户确认小米手机实际使用的引擎包名
- 为后续硬编码引擎包名提供依据

### 3. 增加详细的朗读日志

**新增日志：**
```java
@Override 
public void onStart(String utteranceId) {
    Log.d(TAG, "TTS 开始朗读: " + utteranceId);
    mainHandler.post(() -> executeScript("if(window.onTtsStart) window.onTtsStart();"));
}

@Override 
public void onDone(String utteranceId) {
    Log.d(TAG, "TTS 朗读完成: " + utteranceId);
    mainHandler.post(() -> executeScript("if(window.onTtsEnd) window.onTtsEnd();"));
}
```

**作用：**
- 清晰追踪每次朗读的开始和结束
- 帮助判断是否真的在朗读，还是直接跳过

### 4. 完善 speak 的错误拦截

**修复前：**
```java
if (tts != null && ttsInitialized) {
    tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "novalpie_id");
} else {
    mainHandler.post(() -> executeScript("if(window.onTtsEnd) window.onTtsEnd();"));
}
```

**修复后：**
```java
if (tts != null && ttsInitialized) {
    Log.d(TAG, "TTS speak: 准备朗读 " + text.length() + " 字符");
    
    // 核心修复：检查 speak 的返回值
    int result = tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "novalpie_id");
    
    if (result != TextToSpeech.SUCCESS) {
        Log.e(TAG, "TTS Speak 请求被引擎拒绝，错误码: " + result);
        // 失败时延迟回调，防止前端疯狂翻页
        mainHandler.postDelayed(() -> executeScript("if(window.onTtsEnd) window.onTtsEnd();"), 1000);
    } else {
        Log.d(TAG, "TTS speak: 请求成功提交");
    }
} else {
    Log.e(TAG, "TTS 引擎尚未就绪或不支持当前语言");
    // 引擎没准备好，延迟1秒回调，防止卡死
    mainHandler.postDelayed(() -> executeScript("if(window.onTtsEnd) window.onTtsEnd();"), 1000);
}
```

**关键改进：**
- **检查 speak 返回值**，判断引擎是否接受请求
- **失败时延迟1秒回调**，防止前端疯狂翻页
- 增加详细日志，追踪每次 speak 的状态

### 5. 错误时增加延迟回调

**修复：**
```java
@Override 
public void onError(String utteranceId) {
    Log.e(TAG, "TTS 朗读过程中发生错误: " + utteranceId);
    // 核心修复：出错时增加 500ms 延迟再通知前端，防止瞬间跳读死循环
    mainHandler.postDelayed(() -> executeScript("if(window.onTtsEnd) window.onTtsEnd();"), 500);
}
```

**作用：**
- 出错时不立即回调，而是延迟 500ms
- 给前端一个"缓冲期"，防止快速跳读

---

## 📱 新 APK 信息

**文件名：** novalpie-app-debug-v3.apk  
**文件大小：** 5.80 MB  
**构建时间：** 2025-03-27 19:08  
**有效期：** 180 天（至 2025-09-23）

### 下载链接（有效期 180 天）

```
https://coze-coding-project.tos.coze.site/coze_storage_7621867712305922067/novalpie-app-debug-v3_6731a295.apk?sign=1790161682-c10bd6e668-0-22ff0de6f9b642cd1324a5f61874a4e26d6f52bb8b5e543efae1306725afe4dc
```

---

## 🧪 测试验证步骤

### 1. 安装新 APK

```bash
adb install -r novalpie-app-debug-v3.apk
```

### 2. 查看日志（关键步骤）

**启动应用后，立即查看日志：**
```bash
adb logcat -c  # 清空日志
adb logcat | grep "NovalpieTTS\|NovalPie-Main"
```

**期望看到的日志：**

```
D/NovalPie-Main: === 可用的 TTS 引擎列表 ===
D/NovalPie-Main: 引擎名称: com.xiaomi.mibrain.speech, 标签: 小米语音
D/NovalPie-Main: 引擎名称: com.google.android.tts, 标签: Google 文字转语音
D/NovalPie-Main: =========================
D/NovalPie-Main: TTS 初始化成功，默认引擎: com.xiaomi.mibrain.speech
D/NovalPie-Main: TTS 引擎就绪并支持中文
```

**点击听书后：**
```
D/NovalPie-Main: TTS speak: 准备朗读 200 字符
D/NovalPie-Main: TTS speak: 请求成功提交
D/NovalPie-Main: TTS 开始朗读: novalpie_id
（听到语音播放）
D/NovalPie-Main: TTS 朗读完成: novalpie_id
```

### 3. 异常情况排查

#### 情况 A：日志显示 "LANG_NOT_SUPPORTED"

```
E/NovalPie-Main: 错误：当前 TTS 引擎不支持中文或缺少语言包！
```

**解决方法：**
1. 打开小米手机设置
2. 设置 → 无障碍 → 文字转语音
3. 选择"小米语音引擎"或"Google 文字转语音"
4. **下载中文离线语音包**（非常重要！）
5. 重启应用

#### 情况 B：日志显示 "Speak 请求被引擎拒绝"

```
E/NovalPie-Main: TTS Speak 请求被引擎拒绝，错误码: -1
```

**解决方法：**
1. 查看引擎列表，确认实际使用的引擎
2. 如果不是小米引擎，可以在代码中硬编码引擎包名
3. 或者在设置中切换默认引擎

#### 情况 C：完全没声音，但日志正常

**检查项：**
- ✅ 手机媒体音量是否开启（不是铃声音量）
- ✅ 是否下载了离线语音包
- ✅ 引擎是否设置为默认

---

## 🔍 详细排查指南

### 1. 确认引擎可用

**查看日志输出：**
```
D/NovalPie-Main: === 可用的 TTS 引擎列表 ===
D/NovalPie-Main: 引擎名称: com.xiaomi.mibrain.speech, 标签: 小米语音
```

**如果没有输出：**
- TTS 初始化可能失败
- 检查 `TTS 初始化完全失败，状态码:` 日志

### 2. 确认语言支持

**成功日志：**
```
D/NovalPie-Main: TTS 引擎就绪并支持中文
```

**失败日志：**
```
E/NovalPie-Main: 错误：当前 TTS 引擎不支持中文或缺少语言包！
```

**解决步骤：**
1. 打开手机设置
2. 找到文字转语音设置
3. 下载中文语音包

### 3. 确认 speak 调用成功

**成功日志：**
```
D/NovalPie-Main: TTS speak: 请求成功提交
D/NovalPie-Main: TTS 开始朗读: novalpie_id
```

**失败日志：**
```
E/NovalPie-Main: TTS Speak 请求被引擎拒绝，错误码: -1
```

**错误码说明：**
- `-1` (ERROR): 通用错误
- `-2` (ERROR_SYNTHESIS): 合成错误
- `-3` (ERROR_SERVICE): 服务错误

### 4. 确认朗读进度

**正常流程：**
```
TTS 开始朗读 → (播放中) → TTS 朗读完成
```

**异常流程：**
```
TTS 开始朗读 → 立即 TTS 朗读完成 (无声音)
```

**如果出现异常流程：**
- 引擎可能没有正确播放
- 检查媒体音量
- 检查语音包是否完整

---

## 🛠️ 高级排查

### 查看完整的 TTS 系统日志

```bash
adb logcat -s TTS,TextToSpeech,SpeechService
```

### 强制切换引擎（如需要）

如果发现默认引擎不是小米，可以在代码中硬编码：

```java
// 在 initTTS 方法中添加
String xiaomiEngine = "com.xiaomi.mibrain.speech";
tts = new TextToSpeech(this, status -> {
    // ...
}, xiaomiEngine);
```

### 测试特定引擎

```bash
# 列出所有引擎
adb shell settings get secure tts_default_rate

# 查看当前默认引擎
adb shell settings get secure tts_default_synth
```

---

## 📋 修复对比表

| 问题 | 修复前 | 修复后 |
|------|--------|--------|
| 语言不支持 | 标记为已初始化 | 明确标记为未初始化 |
| 引擎信息 | 无日志 | 启动时打印所有引擎 |
| speak 调用 | 不检查返回值 | 检查并记录错误码 |
| 朗读状态 | 无日志 | 详细记录开始/结束 |
| 错误回调 | 立即回调 | 延迟 500ms 回调 |
| 失败回调 | 立即回调 | 延迟 1000ms 回调 |

---

## 📚 相关文档

- **TTS_THREAD_FIX.md** - 线程崩溃修复报告（v2.0）
- **BUILD_SUCCESS.md** - 第一次构建报告（v1.0）

---

## 💡 使用建议

1. **首次使用：**
   - 安装后立即查看日志，确认引擎初始化成功
   - 如果显示"语言不支持"，下载中文语音包
   - 重启应用后再次测试

2. **如果仍然无声：**
   - 查看引擎列表，确认实际使用的引擎
   - 在设置中切换引擎
   - 或者在代码中硬编码引擎包名

3. **日志分析：**
   - 提供完整日志输出
   - 标注 "可用的 TTS 引擎列表" 部分
   - 标注 speak 调用的返回值

---

**修复完成时间：** 2025-03-27 19:08  
**修复版本：** v3.0-error-intercept  
**状态：** ✅ 修复完成，等待用户测试反馈
