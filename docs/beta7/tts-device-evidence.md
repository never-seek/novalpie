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

## 2026-09-06 真实源章节跨章、后台断点

开发APK `1c5cc984670ad0d3c536f7393b1765d0279612e427adec7d5720a0201bb166eb`，MuMu Android15/540×1098/density240/font1.0，无损install-r。

- 慢网回归`SpeechProgressLatencyTest`先失败：进度POST阻塞时，后续段落本地锚点1200ms内无法更新。将本地小状态apply与远端写分离后通过；`ReadingProgressSynchronizerTest`3项覆盖按书合并、顺序写、账号隔离、失败不按每句重发。
- 真实书`353686《透明龙》`，当前目录9章。最初指定6072568为最后一章，测试明确失败“无下一章”，未开始播放，不算App跨章缺陷。
- 改用6072567（第8章）尾部第47片段开始，实际系统TTS发声并HOME后台；自动加载6072568（第9章），首个onStart片段索引**0**，实际派生内容440片段。
- 本地ReaderProgressStore第9章与源目录一致，熄屏仍Speaking，通知暂停→Paused、继续→同一章节/片段0 Speaking。
- `NativeTtsLiveDeviceTest` **OK (1 test)，14.806s**；流程耗时13719ms。机器证据`beta7-device/20260906-live-tts-353686-ch8.{json,log}`和`20260906-live-tts-353686-report.json`，只含ID/计数/状态，不含正文/凭据。
- 结束停止播放、移除服务，未删除章节或修改替换规则。该真实书本地/网站进度由测试推进到第9章（末章），不影响其他书。

仍待：最终优化包重测、真实公共规则变更后的TTS、音频焦点竞争和耳机事件、OEM/长时间背景。网站远端进度此次仅发送，尚未独立读取回验；不把本地第9章当远端已读回证明。本次MuMu冷启动83s、App17.9s，其他较快启动不能抹掉这个性能样本。

## 2026-09-07 当前节点管线复验

c04ee1e3...8436cee4开发包，同MuMu Android15/360dp/font1.0，`20260907-workspace-live-tts.{json,log}`通过。真实353686/6072567尾片段47→6072568首片段0、440句群、本地第9章、HOME/熄屏/通知暂停继续均成立，核心13401ms。报告`20260907-workspace-live-tts-report.json`；无旧包混记。声音仍系统中文引擎，没有新增云语音API。原样保持上段尚待的R8/焦点/OEM等范围。
