# 系统TTS实机前后对照（开发构建）

日期：2026-09-05；MuMu Android15，`127.0.0.1:16384`，540×1098、density240（360×732dp）。不是最终Beta7认证；现有ReaderTtsController尚待迁移到前台播放协调器。

## 失败复现

测试 `ReaderTtsDeviceTest.installedChineseEngineSynthesizesPcmAndActuallyCompletesPlayback`：系统已选择eSpeak，但NovalPie初始化返回 `TextToSpeech.ERROR(-1)`。日志：`AppsFilter: com.novalpie.app.debug -> com.reecedunn.espeak BLOCKED`。系统 `query-services android.intent.action.TTS_SERVICE` 可找到该服务；说明不是没安装引擎。

## 修复

`app/src/main/AndroidManifest.xml` 增加狭义 `queries/intent/android.intent.action.TTS_SERVICE`，满足Android11+包可见性。不增加QUERY_ALL_PACKAGES，不改变系统安全策略。

## 当前运行结果

APK SHA256：`2E3D244F48276D10051DDE9DEFA038ED47C7C50C15E637D10258711A846EAA6F`（Debug，版本仍Beta6，开发核对包）。保留包名/证书，`adb install -r`，没有清数据。

1. 安装中文引擎测试：`OK (1 test)`，7.943秒，工具exec71074。
2. 含原生Controller的测试组：`OK (2 tests)`，13.627秒，工具exec89791。
   - 系统选择离线zh/cmn音色。
   - 中文合成WAV，RIFF头正确，非静音数据，323,394 bytes；设备位置仅本任务生成的 `cache/beta7-tts-chinese-smoke.wav`。
   - 实际`speak`接到onStart/onDone，非仅synthesize。
   - `ReaderTtsController`连续播放“第一段：中文听书正常。”与“第二段：连续朗读没有丢掉。”，进度回调0、1均收到，结束Stopped。

eSpeak来自F-Droid官方源，版本1.52.0/22；包SHA256 `0a7822fec54d7f7ae759ffedc9afeaa77f809587b171fb2101998787821ec790`，已检查F-Droid签名。它是MuMu验收依赖，不嵌入App、不代表推荐用户必须换引擎。

仍待：前台服务后台/锁屏/音频焦点/耳机拔出/暂停恢复/跨章/改规则后旧回调失效；R8 Beta产物再次完整运行。以上两项通过不能代替全部TTS门禁。

## 前台服务原型实机通过

后续开发APK `c1fe369fa096eb5c05c0d9a0f2e9dea6c779396284d91a544fa20ca4bcc2eea8`，`ReaderPlaybackServiceDeviceTest`通过（2026-09-05 19:14，5.175秒）：

- 由前台Activity启动mediaPlayback服务，中文合成章节进入Speaking。
- HOME退桌面仍Speaking；SLEEP熄屏仍Speaking；WAKE后媒体通知存在。
- 使用媒体通知PendingIntent执行暂停→Paused、继续→Speaking、停止→Stopped且通知移除。
- 记录：`agent-bridge/artifacts/beta7-device/20260905-tts-background-service.json`和同名log，包含已装包/本地包hash一致检查。

这是服务原型验证，使用合成测试文字；接入真实阅读器按钮、真实源章节、焦点竞争/耳机事件、跨章首句与进度还需继续验收。随后已将ReaderScreen入口改接服务，完整构建/新按钮设备测试正在进行。
