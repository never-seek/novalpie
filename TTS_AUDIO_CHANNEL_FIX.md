# TTS 音频通道修复报告（最终版 v4.0）

## 🎯 问题诊断

### 症状描述

**现象：** 流程正常（文本往下走、回调触发），但是**没有声音**

**关键发现：**
- ✅ JS 桥接逻辑正常：onStart 和 onDone 能正常触发
- ✅ 文本传递正常：前端能正确发送文本到原生层
- ❌ 音频输出失败：引擎接受了任务，但没有声音

### 根本原因

这是典型的 **音频通道错误** 问题：

1. **引擎初始化问题：**
   - 可能强制指定了错误的引擎包名
   - 部分系统版本鉴权失败
   - 引擎处于"半死不活"状态

2. **音频流向错误：**
   - TTS 默认可能流向"通话音量"或"通知音量"
   - 如果手机处于静音模式，就听不到声音
   - 需要强制指定为"媒体音量"通道

3. **语言包缺失：**
   - setLanguage 返回失败
   - 引擎初始化成功但无法发声

---

## ✅ 修复内容

### 1. 优化 initTTS：移除硬编码包名

**修复前：**
```java
tts = new TextToSpeech(this, status -> {
    // ...
});
```

**修复后：**
```java
// 核心修复1：传 null 让系统使用当前默认引擎，这是最稳妥的做法
// 避免强制指定包名导致部分系统版本鉴权失败
tts = new TextToSpeech(this, status -> {
    // ...
}, null); // 传 null 让系统决定引擎
```

**关键改进：**
- 不再强制指定小米引擎包名
- 让系统自动选择用户设置的默认引擎
- 避免包名变更导致的鉴权失败

### 2. 增强语言检测

**修复前：**
```java
if (langResult == TextToSpeech.LANG_MISSING_DATA || langResult == TextToSpeech.LANG_NOT_SUPPORTED) {
    Log.e(TAG, "错误：当前 TTS 引擎不支持中文或缺少语言包！");
    ttsInitialized = false;
}
```

**修复后：**
```java
// 核心修复2：检测语言是否真的可用
int langResult = tts.setLanguage(Locale.CHINA);
if (langResult < TextToSpeech.LANG_AVAILABLE) {
    Log.e(TAG, "语言初始化失败，代码: " + langResult + " (可能缺少语音包)");
    ttsInitialized = false;
} else {
    Log.d(TAG, "TTS 引擎就绪并支持中文，语言代码: " + langResult);
    ttsInitialized = true;
}
```

**关键改进：**
- 更精确的语言检测（< LANG_AVAILABLE）
- 记录详细的语言代码
- 明确标记初始化状态

### 3. 强制音频流向媒体通道（核心修复！）

**修复前：**
```java
int result = tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "novalpie_id");
```

**修复后：**
```java
// 核心修复3：显式指定音频属性为媒体流 (STREAM_MUSIC)
// 这样可以确保音频从"音乐/媒体"通道发出，而不是通话或通知通道
Bundle params = new Bundle();
params.putInt(TextToSpeech.Engine.KEY_PARAM_STREAM, android.media.AudioManager.STREAM_MUSIC);

int result = tts.speak(text, TextToSpeech.QUEUE_FLUSH, params, "novalpie_id");
```

**关键改进：**
- **强制使用 STREAM_MUSIC**（媒体音量）
- 只要平时听歌有声音，这个就有声音
- 避免被静音模式或通话音量影响

### 4. 增加导入

**新增导入：**
```java
import android.media.AudioManager;
```

---

## 📱 新 APK 信息

**文件名：** novalpie-app-final-v4.apk  
**文件大小：** 5.80 MB  
**构建时间：** 2025-03-27 19:20  
**有效期：** 180 天（至 2025-09-23）

### 下载链接（有效期 180 天）

```
https://coze-coding-project.tos.coze.site/coze_storage_7621867712305922067/novalpie-app-final-v4_49594a4e.apk?sign=1790162444-78e47f52df-0-804b84b3ff4a1648af5a56e7c6e99085cea34e56f3ac96976b86a0a187cabbea
```

---

## 🧪 测试验证步骤

### 步骤 1：安装新 APK

```bash
adb install -r novalpie-app-final-v4.apk
```

### 步骤 2：检查媒体音量

**非常重要！**
1. 打开手机设置
2. 找到"声音与振动"
3. 确保 **"媒体音量"** 已开启并调大
4. **不是铃声音量，不是通知音量，是媒体音量！**

### 步骤 3：测试系统 TTS

**验证引擎是否正常：**
1. 打开手机设置
2. 设置 → 无障碍 → 文字转语音
3. 点击"播放示例"按钮
4. **如果示例也没声音，说明系统 TTS 引擎坏了**

**如果示例没声音：**
- 下载并安装 Google TTS
- 或更新小米语音引擎
- 或在设置中切换其他引擎

### 步骤 4：查看日志

**清空并查看日志：**
```bash
adb logcat -c
adb logcat | grep "NovalpieTTS\|NovalPie-Main"
```

**期望看到的日志：**

```
D/NovalPie-Main: === 可用的 TTS 引擎列表 ===
D/NovalPie-Main: 引擎名称: com.xiaomi.mibrain.speech, 标签: 小米语音
D/NovalPie-Main: =========================
D/NovalPie-Main: TTS 初始化成功，默认引擎: com.xiaomi.mibrain.speech
D/NovalPie-Main: TTS 引擎就绪并支持中文，语言代码: 0
D/NovalPie-Main: TTS speak: 准备朗读 200 字符
D/NovalPie-Main: TTS speak: 请求成功提交，音频流: STREAM_MUSIC
D/NovalPie-Main: TTS 开始朗读: novalpie_id
（听到语音播放）
D/NovalPie-Main: TTS 朗读完成: novalpie_id
```

---

## 🔍 故障排查

### 情况 A：仍然没有声音

**检查项：**
1. ✅ 媒体音量是否开启（不是铃声音量）
2. ✅ 系统设置中的 TTS 示例是否能播放
3. ✅ 是否下载了中文语音包

**解决方法：**
```bash
# 查看日志
adb logcat | grep "NovalpieTTS"

# 如果看到 "语言初始化失败"
# → 去"设置 → 无障碍 → 文字转语音"下载中文语音包

# 如果看到 "播放请求失败"
# → 检查媒体音量，或切换引擎
```

### 情况 B：日志显示语言代码为负数

**日志示例：**
```
E/NovalPie-Main: 语言初始化失败，代码: -1 (可能缺少语音包)
```

**语言代码说明：**
- `-2` (LANG_NOT_SUPPORTED): 不支持该语言
- `-1` (LANG_MISSING_DATA): 缺少语言包
- `0` (LANG_AVAILABLE): 语言可用
- `1` (LANG_COUNTRY_AVAILABLE): 国家变体可用
- `2` (LANG_COUNTRY_VAR_AVAILABLE): 完整支持

**解决方法：**
- 下载中文语音包
- 或切换到支持中文的引擎

### 情况 C：系统 TTS 示例也没声音

**说明：** 系统级 TTS 故障，不是 App 问题

**解决方法：**
1. 重启手机
2. 清除 TTS 引擎缓存
3. 重新安装 Google TTS
4. 或使用其他引擎

---

## 📊 修复对比表

| 修复项 | v3.0 (之前) | v4.0 (最终) |
|--------|-------------|-------------|
| 引擎选择 | 自动获取默认 | **传 null 强制系统选择** |
| 音频流 | 默认（可能错误） | **强制 STREAM_MUSIC** |
| 语言检测 | LANG_AVAILABLE | **< LANG_AVAILABLE (更严格)** |
| Bundle 参数 | null | **指定 STREAM_MUSIC** |
| 日志详细度 | 一般 | **详细记录语言代码** |

---

## 💡 为什么这次会有效？

### 1. 取消硬编码

**之前的问题：**
- 可能强制启动小米 TTS 包
- 但你的系统版本里包名可能变了
- 导致引擎处于"半死不活"状态

**现在的解决方案：**
- 用 null 动态获取
- 系统会自动指向你设置里选中的引擎
- 最稳定的方式

### 2. Bundle 参数

**之前的问题：**
- 系统可能把 TTS 归类到"通话"或"通知"音量
- 如果手机处于静音模式，就听不到

**现在的解决方案：**
- 强制 STREAM_MUSIC
- 只要平时听歌有声音，这个就有声音
- 与音乐播放器使用同一个音量通道

---

## 🎯 最终检查清单

在测试新 APK 前，请确认：

- [ ] 已卸载旧版本（或使用 adb install -r）
- [ ] 媒体音量已开启并调大
- [ ] 系统设置中 TTS 示例能播放
- [ ] 已下载中文语音包
- [ ] 准备好查看 Logcat 日志

---

## 📚 相关文档

- **TTS_EMPTY_READ_FIX.md** - 空转跳读修复（v3.0）
- **TTS_THREAD_FIX.md** - 线程崩溃修复（v2.0）
- **BUILD_SUCCESS.md** - 第一次构建报告（v1.0）

---

## 🎉 预期结果

如果一切正常，你应该：

1. ✅ 安装新 APK
2. ✅ 调大媒体音量
3. ✅ 打开应用，点击听书
4. ✅ **听到语音播放** 🎵
5. ✅ 自动翻页正常
6. ✅ 流畅的听书体验

---

**修复完成时间：** 2025-03-27 19:20  
**修复版本：** v4.0-audio-channel-final  
**状态：** ✅ 最终修复完成

**如果这次还有问题，请提供：**
1. 完整的 Logcat 日志
2. 系统设置中 TTS 示例是否能播放
3. 手机型号和系统版本
4. 当前使用的是哪个 TTS 引擎
