# Beta 7 专项校验（2026-09-17 夜间至 09-18）

## 结论

**没有通过完整验收。此次实际运行了测试，不是重述旧报告，也不是工具不可用。**

- 09-17 22:00 左右，关闭 Gradle build cache 后重新执行已提交基线：1238 tests / 174 suites，0 failures/errors/skipped。
- 09-18 对中断期间新增的工作区代码重新执行：**1243 tests / 174 suites，0 failures/errors/skipped**。这是该轮开始时工作区快照的结果，不能覆盖之后其他会话继续修改的代码。
- 为旧测试没有覆盖的场景增加**外置、显式启用的专项验证**，调用真实生产函数：**10 项，4 通过、6 失败**。失败均为需求断言失败，不是测试无法启动。
- 透明 PNG、动态 WebP 压缩问题已从“源码风险”升级为**可重复失败测试和真实 EPUB 内部文件证据**。
- 新工作区的下载容错还存在“缺图后标记完成而无法原任务补图”“插图校对失败仅写内部记录而不反馈用户”的问题。
- 本轮没有修改生产代码、没有提交/push/tag、没有安装或发布 APK。外置测试和文档是本轮新增内容。

## 一、版本与并行修改边界

HEAD 仍为 `978ca5573177767b71d6180b21a3429f6ec6ed5a`，但另一个 Antigravity 会话正在同一工作区修改下载/正文解析代码并安装包。

| 对象 | SHA-256 / 说明 |
|---|---|
| GitHub 当前资产 | `0c9041ede19a3a88a2d83df8d5a2dcdcb7f38dfb6ebe4b35ec7ebfd113a687ab`，24,340,321 bytes，`NovalPie-native-2.0.0-beta7-debug.apk` |
| 00:10 观察到的本地/已安装 APK | `0e6557166e13df36c4a648d046e4a88a625e63370f8ae954094813fcc893efb2` |
| 随后本地/已安装 APK 又更新为 | `2aa0bf0082c77365e75bd83c479dc58125ea9eddb75a40e3ae1b2d8e5bb86b52` |
| 1243 项测试开始时工作区 patch | `de23d06dc294491700289027a59e145dcef7992272d485e4eba796fea5b7d4dc` |
| 后续专项测试期间观察到的 patch | `41c8798b9dcb454c63cf649b97a21c8bd746d4c50db1d6e2c66ee81dba80c752` |

00:21 源码继续出现下载取消/校对容错修改；本轮没有覆盖或回退这些修改。**最终发布前仍需以固定源码、固定 APK 做一次完整门禁，不能把这里不同快照的结果合并成“最新版全通过”。**

版本证据：
- `D:\NovalPie\agent-bridge\artifacts\beta7-device\20260918-release-latest.json`
- `D:\NovalPie\agent-bridge\artifacts\beta7-device\20260918-verification-working-tree.patch`
- `D:\NovalPie\agent-bridge\artifacts\beta7-device\20260918-probe-working-tree.patch`

## 二、专项失败及影响

### P1：开启图片压缩会丢透明度、动画帧

测试使用自行生成的非用户素材：64×64 RGBA PNG、红蓝两帧 WebP，以及普通不透明 JPEG 对照。Robolectric API 35 使用原生图形后端，直接执行 `stageNativeEpubFile` 和 `NativeEpubArchiveWriter.write`。

结果：
- 原图模式：PNG 字节一致、alpha=80；WebP 字节一致、两帧保留，均通过。
- 压缩模式：透明 PNG 变成 RGB JPEG，alpha=255；两帧 WebP 变成单帧 JPEG。
- 完整生成两个两章 EPUB 后解包确认：原图包内为 PNG/WebP；压缩包内两张均为 JPEG。两包 ZIP CRC/XML 都正常，但**压缩包丢失原图性质**。
- 普通不透明 JPEG 的压缩对照通过，1200×600 缩为 1080×540，证明测试确实执行了转换而非模拟空实现。

失败测试：
1. `optionalCompressionMustNotDestroyAlpha`
2. `optionalCompressionMustKeepAnimation`
3. `fullEpubArchiveMustNotFlattenTransparentOrAnimatedIllustrations`

应保留用户需要的压缩开关，但对透明/多帧素材保留原格式或明确采用不会破坏内容的策略。这不是要撤掉 Antigravity 的压缩功能。尚未宣称本轮完成真实大书下载/安装阅读器的端到端压缩验收。

### P1：缺图导出后完成状态关闭了原任务补图能力

使用本机 MockWebServer 进行真实下载任务执行：书籍/授权/正文返回正常，仅一张插图返回 404；没有访问真实用户账号，也没有消耗积分。

当前结果：
```text
returnedSuccess = true
phase = Completed
failedImages = 1
totalImages = 1
canResume = false
authorizationCalls = 1
workDirectoryRetained = false
displayMessage = 已保存到下载目录；1张插图获取失败已占位
```

提示有明确失败计数，所以不是“完全没有警告”。但是缺图任务被标记完成、检查点被删除，下载记录的 `downloadCanResume` 返回 false。用户不能在同一个任务补齐失败图片，只能重新开始，存在重复下载/重新授权风险。

失败测试：`missingImageMustRemainRetryableWithoutAuthorizingTheWholeBookAgain`。

如果需要允许用户先拿到部分 EPUB，应区分“部分完成”和“完整完成”，保留失败清单及复用原授权/资源的重试入口，而不是把任务彻底终结。

### P1/P2：插图校对不一致被降级为无状态成功

测试提供“导出为 A、A，而阅读正文要求 B”的明确不一致输入。`TaskExportImageReconciler` 写入 `1-mismatch.json` 后仍返回原正文字符串，不将不一致返回给任务/UI；调用方可以继续正常完成。后续工作区修改还会在部分正文读取失败时以空列表继续校对。

失败测试：`knownSourceMismatchMustNotReturnAnUnqualifiedSuccessfulReconciliation`。

这不等于 App 应伪造缺少的 B 或一律删除 A：源正文与导出文件可能本来不同。问题是**用户看不到“未校对通过”的状态**，无法区分完整验证与容错保留。应保留原图并明确告警/可重试，不能用内部日志替代用户反馈。

### P2：图片徽章常态仍错误使用展开态 contain

最小 CSS：
```css
.badge { width:125px; height:35px; background-size:cover; }
.badge:hover,.badge:active,.badge:focus { height:180px; background-size:contain; }
```

真实 `adminShopBadgePreviewContentScale` 返回 Fit，而常态应是 Crop；对照组只有常态 contain 时返回 Fit 正确。

失败测试：`hoverContainMustNotOverrideBaseCover`。

这与前轮个人栏小缩略图的实机现象一致；仓库外框等高不代表图片样式已正确。

### 仍未关闭的其他事项

- 360dp 搜索卡片底部字数错行：09-17 已复现，相关卡片排版未改。本轮在另一会话同时操作 MuMu 的阶段不新增无效“通过”证据。
- GitHub 仍为 Debug，尚未满足最初非调试优化 Beta 的发布要求。
- 全套 TTS 跨章/耳机/音频焦点、连续后退分页、所有尺寸字体组合、真实多图大书以及下载断网/空间不足矩阵，仍未全部验收。
- 未完成专项安全审计，不能声明没有安全漏洞。

## 三、MuMu 补充运行观察

- 在 09-17 已发布 APK 上进入此前阅读的书，正文可显示；目录、章评等请求一度出现 20 秒左右超时，显示明确错误和重试入口。
- 点击目录重试，第一次仍失败；之后再次重试恢复目录，定位第 8 章，菜单和进度恢复。保留了失败及恢复控件树。不能将暂时网络超时直接定性为 App 永久断网。
- 无限滚动章末仍存在“本章结束 → 章节评论”区域，章评失败显示重试，不是静默删掉章评。跨章完整矩阵本轮没有做完。
- 没有新增真实下载/评论、没有清用户数据。00:11 后设备中另一次真实下载和新包安装来自其他正在工作的会话，不算本轮执行证据。
- 临时尺寸已恢复 900×1600，density 240、font_scale 1.0。搜索曾临时改成网格；由于另一会话持续导航/安装，未确认恢复列表成功，不能写成已恢复。
- 发现共享设备/源码并发操作后，停止继续操控 MuMu，避免互相干扰。

## 四、构建与静态检查边界

`testDebugUnitTest` 已明确使用 `cleanTestDebugUnitTest --no-build-cache`，不是 FROM-CACHE。`assembleDebug` 完成，当前源码编译/包装任务没有报语法错误。

组合命令的最终状态仍是失败：`lintAnalyzeDebug` 在 512MiB Gradle heap 下发生 `OutOfMemoryError: Java heap space`。这是校验进程资源不足，不是 App 运行时 OOM，也不是 Lint 已通过。本轮没有通过改 lint 规则、屏蔽问题或提高断言宽容度来制造全绿。需在代码交接后用足够堆内存单独重跑 Lint。

## 五、复现材料

- 既有测试结果：`D:\NovalPie\agent-bridge\artifacts\beta7-device\20260918-current-baseline-results.json`
- 基线门禁日志：`D:\NovalPie\agent-bridge\artifacts\beta7-device\20260918-current-gate.log`
- 专项日志：`D:\NovalPie\agent-bridge\artifacts\beta7-device\20260918-boundary-tests.log`
- 专项 JUnit XML：`D:\NovalPie\agent-bridge\artifacts\beta7-device\20260918-boundary-probes\results`
- 原图/压缩 EPUB、缺图状态证据：`D:\NovalPie\agent-bridge\artifacts\beta7-device\20260918-boundary-probes\outputs`
- 外置 Gradle init：`D:\NovalPie\agent-bridge\artifacts\beta7-device\20260918-boundary-probes\probes.gradle`

外置测试默认不进入项目 test source set，仅显式 `-I` 时加入；没有把失败用例改成“期待丢图/期待错误样式”以通过测试。

**本轮结果：找到可重复问题，未修复生产代码，不宣布 Beta7 全部通过。**
