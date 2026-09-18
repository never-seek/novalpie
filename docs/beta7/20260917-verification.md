# Beta 7 校验报告（2026-09-17）

## 结论

**本次编译、1238 项自动化测试通过，但 Beta 7 不能判定为完整验收通过。**

已在 MuMu 检查与 GitHub 当前资产完全一致的 APK。阅读器章节/进度更新和 TTS 基础播放正常；另发现书卡指标错行、徽章样式状态混用、可选图片压缩缺少透明/动画保护，以及发布包仍为 Debug 的问题。本轮只校验和记录，**没有修改业务代码、覆盖安装、清除数据或替换 GitHub 资产**。

本报告取代旧状态文档中有关“当前 APK”“尚未发布”及当前测试数量的结论；不将历史验证自动计入本次通过项。

后续补充：已按用户提供的 Antigravity 会话 `251a77ab-7dba-46f2-8deb-6a642652e3ff` 完成[修改对账](antigravity-251a77ab-reconciliation.md)。确认这些修改全部位于本报告校验版本内，并对该会话留存的 2922 章 EPUB 完成只读 CRC/XML/条目复核。压缩属于应保留的可选功能；风险指透明/动画保护不足，不是要求取消压缩。历史大书成功证据与当前包本轮下载验收仍分别记录。

## 1. 实际核对的版本

| 项目 | 结果 |
|---|---|
| 源码 | `978ca5573177767b71d6180b21a3429f6ec6ed5a` |
| 分支 / 标签 | `codex/native-app2-current-20260821` / `v2.0.0-native-beta7` |
| GitHub APK | `NovalPie-native-2.0.0-beta7-debug.apk` |
| 大小 | 24,340,321 bytes |
| SHA-256 | `0c9041ede19a3a88a2d83df8d5a2dcdcb7f38dfb6ebe4b35ec7ebfd113a687ab` |
| 包名 / 版本 | `com.novalpie.app.debug` / `2.0.0-native-beta7-debug` / 2026091601 |
| 一致性 | 本地构建产物、MuMu 已安装 APK、GitHub 资产 digest 一致 |
| 签名 | 验证成功；证书 SHA-256 `cc7d6fe6844d4a4ee510d65b185ac5e32a642faa9bfb819df75d51be4c607047` |
| 调试开关 | `DEBUGGABLE`：开启，**不满足原计划的非调试优化 Beta 包要求** |

`app/build/outputs/apk/beta/app-beta.apk` 是旧优化包，SHA-256 为 `839b1a59…`，不是当前 GitHub/设备版本。不能用该旧包的运行记录替代本次包的验证。

## 2. 自动化校验

- 先执行 `testDebugUnitTest + lintDebug`，发现测试任务使用已有结果后，补执行 `cleanTestDebugUnitTest + testDebugUnitTest + assembleDebug`。
- **重新执行的测试：174 suites / 1238 tests / 0 failures / 0 errors / 0 skipped。**
- `ReaderChapterProgressIdentityTest.scrollProgressFollowsChapterBWhenReaderScreenSurvivesNavigation` 通过。
- `assembleDebug` 成功；复核 APK hash 未变化。
- 当前 `lintDebug`：**0 errors、16 warnings、1 information**。不是旧记录中的 5 warnings。警告涉及固定方向、Modifier 参数顺序、已过时 SDK 判断；信息项为整数 State 装箱。
- 原图字节、流式 EPUB、重复图片引用、文本替换导出等已有单测通过；**没有压缩开关开启后的透明 PNG / 动态 WebP 保真回归**，测试全绿不代表该场景安全。

日志：
- `D:\NovalPie\agent-bridge\artifacts\beta7-device\20260917-tests-lint.log`
- `D:\NovalPie\agent-bridge\artifacts\beta7-device\20260917-fresh-tests-build.log`
- `D:\NovalPie\agent-bridge\artifacts\beta7-device\20260917-verification-summary.json`

## 3. MuMu 实际运行结果

设备：Android 15，`127.0.0.1:16384`；原始尺寸 900×1600、density 240、font_scale 1.0。期间临时改为 540×1098（360dp）检查适配，结束已恢复原始尺寸。

### 已验证通过（仅限下列流程）

1. **章节与进度**：滚动模式第 8 章 → 下一章第 9 章；底栏显示 9，滚动后进度从 0.27% 更新为 0.28%；返回上一章后显示 8 / 0.24%。不再复现旧构建的底栏冻结。点击下一章后控制器隐藏，中心点击可再次展开。
2. **TTS 基础生命周期**：启动后音频输出帧数实际增长；暂停显示已暂停，恢复后切到 HOME，音频帧从 1,060,864 增长到 1,217,536；返回应用能停止，反馈层消失，未遗留播放服务。不是只看按钮文字判成功。
3. **收藏 / 搜索**：收藏显示 65 本，继续阅读含书名与章节；搜索在没有关键词时返回现有筛选条件下的 403 项结果；封面、标签及上传书绿色角标可见。
4. **登录 / 我的**：登录态保留；资料加载后头像、徽章及统计正常返回。徽章视觉仍存在下述问题。
5. **替换入口**：读取到“我的 0 条 / 公共 222 条可选 / 公共未应用”。只证明入口和数据可用，不代表本轮完成替换后的四种导出验收。

源码检查：滚动章节标题使用满宽、居中；分页标题也设置居中。本轮未将所有长标题/大字体组合记为实机通过。

### 本轮未完成

- TTS 自动跨章首句、音频焦点、耳机断开、规则实时改变、不同 OEM 后台行为。
- 短章节无限续读、章评保留、五种动画与连续向后分页截断的完整矩阵。
- 原文/替换 × EPUB/TXT 的当前包完整下载；开启压缩、1GB+ 大书、下载中断恢复、空间不足。
- 320/360/412/600dp × 字体 1.0/1.3/2.0、所有主题和权限角色组合。
- 所有论坛格式、嵌套回复、管理员和工作区写入流程。
- 冷/热缓存性能三轮及持续内存测试；本轮没有据此声称所有手机达到高刷新率。

## 4. 必须跟进的问题

### V17-01：搜索卡片等高但指标不在同一行【实机已复现】

360dp 两列搜索中，左右卡片整体同高，但左侧字数 `674.7w` 换到第二行，右侧 `218.5w` 与收藏/阅读留在第一行。控件树中左侧字数 top=891，右侧 top=862。说明逐卡片自行换行仍违反“相同指标同横轴”的要求。

应使用一致的指标轨道/行策略，不能只让外框等高，也不能省略数据。

证据：`D:\NovalPie\agent-bridge\screenshots\beta7-20260917-search-grid-360.png`

### V17-02：图片徽章把展开态 contain 混进常态【实机现象 + 源码根因】

当前公开徽章 113 的 `.badge` 常态为 125×35、`background-size: cover`；`:hover/:active/:focus` 展开态才是 `contain`。`adminShopBadgePreviewContentScale` 搜索整份 CSS，只要任何位置出现 contain 就选择 Fit，没有区分选择器状态。实机个人栏因此显示很小的完整缩略图，而不是常态裁切横幅。

还需检查个人身份栏的可用宽度与点击预览适配；不能把网页展开的 90vw 直接塞进列表。按常态/展开态分别解析，再采用适合原生小屏的交互。

证据：
- `D:\NovalPie\agent-bridge\screenshots\beta7-20260917-profile-loaded-360.png`
- `D:\NovalPie\agent-bridge\artifacts\beta7-device\20260917-public-badge-style.json`（仅公开徽章样式，无 Cookie、Token 或私人信息）
- `ui/AdminRedesign.kt` 的 `adminShopBadgePreviewContentScale`；`ui/ProfileScreens.kt` 的 `ProfileArtworkBadge`。

### V17-03：图片压缩开启后缺少透明/动画保护【源码确认，尚未端到端复现】

默认 `compressImages=false`，原图路径保留字节；ZIP 文本压缩不等于有损压图。

但开启“图片压缩”后，`NativeEpubArchiveWriter.stageNativeEpubFile` 对 JPEG/PNG/WebP 调用 `compressStagedImageIfPossible`，使用 BitmapFactory 解码、最大 1080×1920 缩放并转 JPEG。代码未检查透明通道或 WebP 动画：透明 PNG/WebP、幻影坦克图及动态 WebP 会面临丢失透明/帧信息的问题。GIF 不进入这个分支。

应增加透明/多帧保留策略和相应回归；明确区分原图、仅 ZIP 压缩和静态有损压缩。**本轮没有重新下载书籍，不把源码判断说成真实导出已复现。**

### V17-04：发布配置与文档不一致【资产/源码已核实】

GitHub 当前为 Debug APK，不是原计划要求的非调试 R8 Beta。需用当前提交重新构建 `assembleBeta`，完成该产物安装验证后才可替换；不能直接上传旧优化包。

`docs/RELEASE_NOTES_2.0_BETA7.md` 的 APK hash 与当前资产不一致；`STATUS.md` 的旧“未发布”记录也已失效。应依据实际交付包同步，不沿用“彻底解决”而无当前证据的表述。

## 5. 测试副作用与环境恢复

- 未登出、未清数据、未覆盖安装、未发送评论、未启动下载或删除既有下载。
- 临时搜索网格已恢复原先列表模式；尺寸已 reset；字体/密度未变；TTS 已停止。
- 测试沿用了当前正在阅读的书，阅读位置在第 8 章内发生变化；没有伪称完全没有状态变化。
- UIAutomator 个别转储发生的是 `uiautomator` 工具进程崩溃，不是 App 进程；只使用实际生成且与截图一致的控件树。
- 一个指定书籍深链被执行策略阻止后未重试或绕过；改用原本打开的其他书检查阅读器。

**下一步：分别为 V17-01/02/03 建立失败回归并修复；重新生成优化 Beta 包并对同一产物验收。当前不能标记“Beta 7 所有要求已完成”。**
