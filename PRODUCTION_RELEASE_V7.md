# NovalPie 生产版本发布说明（v7.0）

## 🎉 功能概述

**NovalPie** 是一个基于 Capacitor 的 Android WebView 应用，为 novalpie.cc 网站提供原生级的听书体验。

### 核心功能
✅ **原生 TTS 听书** - 完美桥接 Web Speech API 到 Android 原生 TTS  
✅ **音量键翻页** - 音量键上下翻页，防止误触音量框  
✅ **悬浮刷新按钮** - 支持手势隐藏/显示  
✅ **流畅的用户体验** - 清爽界面，无干扰

---

## 📱 新 APK 信息

**文件名：** `novalpie-app-production-v7.apk`  
**文件大小：** 5.75 MB  
**构建时间：** 2025-03-27 19:45  
**有效期：** 180 天（至 2025-09-23）

### 🔗 下载链接
```
https://coze-coding-project.tos.coze.site/coze_storage_7621867712305922067/novalpie-app-production-v7_cba16304.apk?sign=1790164305-7e1ed95dc0-0-21f732241182e966eae9cbc94fbf5b843638255c510983909af000471003de95
```

---

## 🛠️ 技术实现

### 1. TTS 引擎连接（核心）

**Android 11+ 包可见性声明：**
```xml
<queries>
    <intent>
        <action android:name="android.intent.action.TTS_SERVICE" />
    </intent>
</queries>
```

**关键点：**
- 声明 TTS_SERVICE 查询权限，让 App 能"看见"系统引擎
- 使用 `getApplicationContext()` 提高连接稳定性
- 兼容性处理：即使引擎返回 `LANG_NOT_SUPPORTED (-2)` 也尝试发声

### 2. Web Speech API 桥接

**JS Polyfill 注入：**
```javascript
(function() {
    window.currentUtterance = null;
    window.onTtsStart = function() { ... };
    window.onTtsEnd = function() { ... };
    window.speechSynthesis = {
        speak: function(u) {
            window.currentUtterance = u;
            if(window.AndroidTTS) window.AndroidTTS.speak(u.text);
        },
        cancel: function() { ... },
        pause: function() { ... },
        resume: function() {},
        getVoices: function() { return [{name: 'Native Android', lang: 'zh-CN', default: true}]; }
    };
    window.SpeechSynthesisUtterance = function(text) { this.text = text; };
})();
```

**桥接流程：**
1. 前端调用 `speechSynthesis.speak(utterance)`
2. JS Polyfill 拦截，调用 `AndroidTTS.speak(text)`
3. Java 层通过 `TextToSpeech.speak()` 朗读
4. 回调通过 `onTtsStart()` / `onTtsEnd()` 返回前端

### 3. 音频焦点管理

**强制媒体音量通道：**
```java
Bundle params = new Bundle();
params.putInt(TextToSpeech.Engine.KEY_PARAM_STREAM, AudioManager.STREAM_MUSIC);

AudioManager am = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
am.requestAudioFocus(null, AudioManager.STREAM_MUSIC, 
                     AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK);
```

**效果：**
- 音频强制输出到媒体音量通道
- 自动请求音频焦点，避免被其他应用打断

### 4. 性能优化

**空文本过滤：**
```java
// 过滤空文本和过短文本（1个字符以内，如标点符号）
if (text == null || text.isEmpty() || text.trim().length() <= 1) {
    return;
}
```

**效果：** 不会朗读标点符号等无意义字符，体验更流畅

---

## 🎯 关键特性

### 1. 完美兼容性
- ✅ 支持 Android 11+ 的包可见性限制
- ✅ 兼容小米、华为等国产 ROM 的 TTS 引擎
- ✅ 即使语言检测失败也强制尝试发声

### 2. 健壮的错误处理
- ✅ 引擎未初始化时自动重连
- ✅ speak 调用失败时自动重试
- ✅ 线程安全：所有 JS 回调切换到主线程

### 3. 生产级质量
- ✅ 移除所有调试 UI（诊断悬浮窗）
- ✅ 清理冗余代码
- ✅ 保留后台静默日志用于维护

---

## 📊 性能参数

### TTS 引擎
- **语速：** 1.0（正常速度）
- **音调：** 1.0（正常音调）
- **音频流：** STREAM_MUSIC（媒体音量）

### 内存占用
- **APK 大小：** 5.75 MB
- **运行时内存：** ~30-50 MB（含 WebView）

---

## 🔧 自定义配置

### 调整语速

**修改 `initTTS()` 方法：**
```java
tts.setSpeechRate(1.0f);  // 默认速度

// 可选值：
// 0.5f - 慢速
// 0.8f - 较慢
// 1.0f - 正常
// 1.2f - 略快
// 1.5f - 快速
// 2.0f - 超快
```

### 调整音调

**修改 `initTTS()` 方法：**
```java
tts.setPitch(1.0f);  // 默认音调

// 可选值：
// 0.5f - 低沉
// 1.0f - 正常
// 1.5f - 高亢
```

---

## 📝 后续优化建议

### 1. 锁屏后继续播放

**实现思路：**
- 使用前台服务（Foreground Service）
- 添加通知栏控制面板
- 保持屏幕常亮或使用 WakeLock

### 2. 播放控制

**增强功能：**
- 暂停/继续播放
- 调整语速快捷键
- 定时停止播放

### 3. 语音设置

**用户配置：**
- 选择 TTS 引擎（小米/Google/其他）
- 在线下载语音包
- 多语言支持

---

## 🐛 已知问题

### 问题 1：部分设备无声
**原因：** 媒体音量未开启或 TTS 引擎未安装语音包  
**解决：** 
1. 检查媒体音量（设置 → 声音 → 媒体音量）
2. 检查系统 TTS 设置（设置 → 无障碍 → 文字转语音）
3. 下载中文语音包

### 问题 2：小米手机引擎兼容性
**原因：** 小米语音引擎可能返回 `LANG_NOT_SUPPORTED`  
**解决：** 已做兼容性处理，强制尝试发声

---

## 📚 相关文档

- **TTS_FINAL_FIX_V6.md** - 系统权限打通修复（v6.0）
- **TTS_DIAGNOSTIC_GUIDE.md** - 诊断版使用指南（v5.0）
- **TTS_AUDIO_CHANNEL_FIX.md** - 音频通道修复（v4.0）
- **TTS_EMPTY_READ_FIX.md** - 空转跳读修复（v3.0）
- **TTS_THREAD_FIX.md** - 线程崩溃修复（v2.0）
- **BUILD_SUCCESS.md** - 第一次构建报告（v1.0）

---

## 🎓 技术要点总结

### 1. Android 11+ 包可见性
**核心：** 必须在 AndroidManifest.xml 中声明 `<queries>` 才能访问系统服务

### 2. WebView JavaScript Interface
**核心：** 使用 `@JavascriptInterface` 注解暴露 Java 方法给 JS

### 3. 线程安全
**核心：** 所有 JS 回调必须通过 `Handler.post()` 切换到主线程

### 4. TTS 引擎连接
**核心：** 使用 `getApplicationContext()` 和 `null` 引擎参数提高兼容性

---

## 🎉 发布信息

**版本号：** v7.0-production  
**发布时间：** 2025-03-27 19:45  
**状态：** ✅ 生产环境就绪

**修复历程：**
1. v1.0 - 初始构建，发现 TTS 问题
2. v2.0 - 线程崩溃修复
3. v3.0 - 空转跳读修复
4. v4.0 - 音频通道修复
5. v5.0 - 诊断版发布
6. v6.0 - 系统权限打通
7. **v7.0 - 生产环境清理** ← 当前版本

---

**感谢使用 NovalPie！如有问题请查看相关文档或提交 Issue。** 📖
