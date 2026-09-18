# Beta 7 状态摘要

## 2026-09-18 原生下载监控面板、重试决策与 GitHub 发布更新（当前最新）

- **全量单测通过**：1,244 tests / 174 suites / 0 failures/errors/skipped；代码架构与稳定性验证通过。
- **全新原生下载监控面板落地**：
  - 1:1 复刻原生下载面板，提供大进度条、实时百分比动画，以及章节与插图计数的双维度进度展示。
  - 底部集成深色等宽控制台（Console），动态平滑滚屏输出彩色语法高亮日志（`[DOWNLOAD]`、`[RETRY]`、`[PACK]`、`[SUCCESS]`、`[ERROR]`），全透明呈现网络拉取与打包过程。
- **网络资源指数退避重试**：
  - 单张图片/资源最多 5 次重试，退避区间 400ms ~ 6s 并加入随机抖动，极大增强弱网与 CDN 波动下的容错能力。
- **失败决策二选一卡片（全成功免打扰）**：
  - 任务全成功免打扰直接打包；若重试 5 次后仍有部分网络图源失效，挂起任务弹出二选一卡片（【换个网络后重试】/【直接打包】），支持断点恢复重试或直接容错出包生成完整 EPUB。
- **GitHub Release 资产与说明更新已完成**：
  - GitHub 标签：`v2.0.0-native-beta7`
  - 资产包：`NovalPie-native-2.0.0-beta7-debug.apk`（24,346,611 字节）
  - SHA-256：`F3ADB8D9C7ADCE3F10C53ECF673511ACBFA9BD25AEF7AEBD6260D78B2E772060`
  - 中文说明文档已同步更新至 GitHub Release 页面及 `docs/RELEASE_NOTES_2.0_BETA7.md`。

## 2026-09-18 专项校验（历史参考）

- 实际执行而非重述历史：新工作区基线 **1243 tests / 174 suites，0失败/错误/跳过**；追加的外置专项 **10项，6项失败**。详细边界见 [专项校验报告](20260918-deep-verification.md)。
- 已复现：开启压缩后透明 PNG 变成无透明度 JPEG、两帧 WebP 变成单帧 JPEG；完整 EPUB 解包也如此。原图模式字节保留通过。
- 新下载容错问题：缺图任务返回 Completed、无法同任务重试、工作检查点被清除；插图校对不一致只写内部记录，用户侧无校对失败状态。
- 图片徽章 cover/contain 状态混用的真实函数回归失败；搜索指标错行仍未关闭。
- 校验期间其他会话继续修改并安装：本地/设备由 `0e655716…` 又变成 `2aa0bf00…`；GitHub仍是 `0c9041ed…`。不同快照不能混算为“最新版本全部通过”。
- Lint 进程在512MiB堆上OOM，静态门禁未完成；这不是App运行时崩溃。需稳定交接后单独增加构建堆再验。
- 本轮只写外置验证材料/文档，没有修改或回滚生产代码、没有发布。MuMu尺寸恢复900×1600；为避免与其他会话冲突，停止继续并发操作。

## 2026-09-17 当前校验结论（优先于下面的历史记录）

- 已核对用户指定 Antigravity 会话 `251a77ab-7dba-46f2-8deb-6a642652e3ff`：下载压缩、长书超时、装扮卡片、高刷新率与连续章节修复均已包含在当前版本，并非漏合并。留存 2922 章 EPUB 的 CRC/XML/结构复核通过；详见 [Antigravity 修改对账](antigravity-251a77ab-reconciliation.md)。
- **自动化通过，但未完整验收通过。** 当前源码 `978ca55`，GitHub 已发布 `v2.0.0-native-beta7`；资产是 **Debug APK**，不是下面历史记录中的优化 Beta 包。
- 本地、MuMu 已安装包、GitHub digest 一致：`0c9041ede19a3a88a2d83df8d5a2dcdcb7f38dfb6ebe4b35ec7ebfd113a687ab`，24,340,321 bytes，versionCode 2026091601。
- 清除测试结果后重新执行：**1238 tests / 174 suites / 0 failures/errors/skipped**；`assembleDebug` 成功；本次 `lintDebug` 为 **0 errors / 16 warnings / 1 information**。
- MuMu 确认章节 8→9→8、随滚动进度更新、TTS 播放/暂停/后台恢复/停止、登录态/收藏/默认搜索正常；不能据此覆盖全部业务矩阵。
- 尚需修复：360dp 搜索书卡指标错行；图片徽章混用 hover 的 contain 样式；可选图片压缩缺少透明/动态图保护。发布包仍需按计划改为当前源码的非调试优化构建并重新验收。
- 本轮没有业务代码修改、覆盖安装或 GitHub 替换。完整证据与边界：[20260917-verification.md](20260917-verification.md)。
- **以下内容为历史记录，其中“当前有效”“尚未发布”和旧 APK hash 不再代表现在的发布状态。**

## 2026-09-15 徽章布局与图片预览最终适配（当前有效）

- 已按用户提供的两张网页截图复现：论坛普通徽章使用内容宽度，个人资料 Hero 徽章按网页头像栏规则可伸展；不再把三枚徽章全部拉成等宽条。
- 具有 CSS 背景图或 `image_url` 的徽章保留源宽高、内边距和 `contain/cover` 行为，窄卡按整体比例缩放；`#0000`、透明颜色和 `.badge__text{display:none}` 生效，图片上的可见文字使用暗色阴影。
- 本人资料页徽章保留在用户名栏，不再被头像框或刷新/换头像按钮挤到错误位置；论坛徽章仍保持紧凑排列。
- 当前测试：**1237 tests / 174 suites / 0 failures/errors/skipped**；`assembleBeta` 成功；`lintDebug` 0 errors / 5 warnings；`git diff --check` clean。
- 当前 APK：3,276,210 bytes，SHA-256 `839b1a59748b4a3b96ce9087e740723960573d91e96a15aefb7af7f21bb85910`；已覆盖安装 MuMu `127.0.0.1:16384`，本地/设备 hash 一致。
- 实机截图：`D:\NovalPie\agent-bridge\screenshots\beta7-20260915-profile-badges-final.png`、`D:\NovalPie\agent-bridge\screenshots\beta7-20260915-forum-badges-beautified.png`；报告：`D:\NovalPie\agent-bridge\artifacts\beta7-device\20260915-badge-layout-runtime.json`。
- Beta7 仍未创建 GitHub Release；本轮只处理徽章布局与预览，不把其他审计项提前标为完成。

## 2026-09-15 徽章布局与图片预览二次适配（当前有效）

- 普通徽章按内容宽度测量并使用 FlowRow 自然排列，不再把同一栏徽章拉成等宽长条；带背景图的徽章保留网页声明的宽高与内边距，并整体缩放。
- 透明图片徽章不再叠加错误的蓝紫渐变；`color:#0000`、`.badge__text{display:none}` 等网页规则会正确隐藏文字，带文字的图片徽章增加暗色文字阴影。
- 本人资料页徽章移到身份卡完整宽度下方；交流区保持紧凑自然排列。新回归覆盖多徽章同排、160×60 图片比例、100dp 窄卡整体缩放和三值内边距。
- 当前测试：**1236 tests / 174 suites / 0 failures/errors/skipped**；`assembleBeta` 成功；`lintDebug` 0 errors / 5 warnings；`git diff --check` clean。
- 当前 APK：`app/build/outputs/apk/beta/app-beta.apk`，3,276,213 bytes，SHA-256 `969921c732981b7ea2467d75cc49da9d87052ce63867f3003bd5a35025245028`。已覆盖安装 MuMu `127.0.0.1:16384`，本地/设备 hash 一致。
- 截图：`D:\NovalPie\agent-bridge\screenshots\beta7-20260915-profile-badges-beautified.png`、`D:\NovalPie\agent-bridge\screenshots\beta7-20260915-forum-badges-beautified.png`；报告：`D:\NovalPie\agent-bridge\artifacts\beta7-device\20260915-badge-layout-runtime.json`。
- Beta7 仍未创建 GitHub Release；本轮只处理徽章布局/预览，不把其他剩余审计项标为完成。

## 2026-09-15 交流区徽章网页样式适配（当前有效）

- 从当前公开交流区接口重新读取帖子 `1742` 的徽章 `111` CSS：这是 `data:image/svg+xml` 铭牌背景加 `mask-image` 荆棘装饰，不再使用旧的六边形/星星占位效果。
- 复杂徽章使用隔离的无脚本、禁止外网、禁止导航和无 Cookie 依赖的局部 WebView 渲染；普通徽章继续走 Compose 原生渲染，避免所有徽章无脑套网页。
- 按父卡片可用宽度整体等比缩放，修复小屏右侧装饰被裁切；装扮背包中的宽徽章使用卡片宽度，不再塞进 96dp 小方框。补充外部资源、恶意标记和缩放回归测试。
- 当前测试：**1231 tests / 173 suites / 0 failures/errors/skipped**；`assembleBeta` 成功；`lintDebug` 0 errors / 5 warnings；`git diff --check` clean。
- 当前 APK：`app/build/outputs/apk/beta/app-beta.apk`，3,276,223 bytes，SHA-256 `961af3a31c7d8d0ae67c9d120424578ad4dcd10b17a1418eeabc1aadd7919dd2`。已覆盖安装 MuMu `127.0.0.1:16384`，本地/设备 hash 一致，版本 `2.0.0-native-beta7`。
- 实机截图：`D:\NovalPie\agent-bridge\screenshots\beta7-20260915-forum-badge-adaptive-final.png`；运行报告：`D:\NovalPie\agent-bridge\artifacts\beta7-device\20260915-forum-badge-adaptive-runtime.json`；接口样本：`D:\NovalPie\agent-bridge\artifacts\beta7-device\20260915-forum-badge-source.json`。
- Beta 7 仍未创建 GitHub Release；本轮只关闭交流区徽章样式/适配问题，不把剩余业务矩阵标记为全部完成。

## 2026-09-14 重启声明

用户已要求作废2026-09-13 08:30之后的过程结论并重新开始。新的审计分界和证据规则见[RESTART-20260914.md](RESTART-20260914.md)。以下旧段落全部降级为历史线索，不能作为当前Beta7通过依据；当前dirty源码和APK必须重新审查，发布状态保持未发布。

## 2026-09-14 重启后有效门禁

- 当前源码重新执行 `testDebugUnitTest`：1226 tests / 173 suites，0 failures/errors/skipped；本轮新增阅读器章节标题与 badge 素材回归。
- `assembleBeta`（R8、资源压缩）和 `lintDebug` 成功；Lint 0 errors / 5 warnings，`git diff --check` clean。
- 当前 APK `app/build/outputs/apk/beta/app-beta.apk`：3,276,209 bytes，SHA-256 `71edd3d55bc69a77fe334f25fcf3bae1aca7643ed4c7f4b6e6ac31eca3ff0b75`；MuMu `127.0.0.1:16384` 已覆盖安装，本地/设备哈希一致，冷启动与阅读器/装扮页检查无 App `AndroidRuntime` 异常。证据：`agent-bridge/artifacts/beta7-device/20260914-title-badge-fix-runtime.json`、`agent-bridge/screenshots/beta7-20260914-reader-title-check.png`、`beta7-20260914-badge-inventory-owned.png`。
- 本轮 UI 修复：滚动阅读器章节标题改为占满正文列后居中；装扮 badge 优先读取 CSS 背景素材、无 CSS 素材时读取 `image_url`，WebP 兜底使用 Fit 避免裁切；API 兼容常见 `html/css` 字段别名。ProfilePresentation 定向测试 28 项通过。
- EPUB 文本/目录类条目采用 Deflate 无损压缩；PNG/JPEG/GIF/WebP 仍按原始字节保存，不降画质、不转码、不抽帧。该验证不等于所有下载中断/替换组合已完成；Beta7 仍未发布。

更新：2026-09-13 19:52。结论：**未全部完成，尚未发布。**

新一轮：离线未知进度、EPUB文本无损压缩、书籍元信息独立状态/账号隔离/草稿、确认框与封面回传、异步管理深链导航均已实现；追加2处审查问题已修并复审。完整1225项测试/173套、Beta编译、R8打包、lint0错误5警告均通过；521be94e已覆盖安装，实书EPUB和离线进度已复验。章节列表管理及其余计划仍未全部完成。

## 本轮最终候选

- `app/build/outputs/apk/beta/app-beta.apk`，3,276,217bytes，SHA256 `521be94ee0c92c9673c0ae11dc5b87438d99c0db1aa2c3c6653b145f33a423d6`，设备与本地一致，原Beta6签名/包名、非debug/audit组件无均通过。
- 44章原文EPUB含6正文图+封面，ZIP/CRC和独立Librera正文/插图通过；7图SHA/字节数与旧原图包完全一致，文件约减5.07%。这是文本无损压缩，不是图片降画质，也没有可选图片质量档位。
- 离线实机截图显示“目录未加载 / 离线缓存 / —”，不再伪0.00%。断网测试临时影响ADB，已通过MuMu原生shell恢复并验证飞机模式0、Wi-Fi1、移动数据1、ADB reverse正常，联网重启进度回2.27%。
- 测试EPUB清理命令被工具策略拒绝，两份约5.85MB文件仍保留；未绕行删除，未操作其他文件。管理编辑深链也在同一被拒绝命令里，未换入口重试；元信息写入仅有合成协议/真实Compose测试，不能说已在线改书成功。
- 最终报告：`agent-bridge/artifacts/beta7-device/20260913-lossless-metadata-runtime.json`。下面保留旧候选证据，不覆盖本节。

本轮新增：编辑器删除期间误打开其他章节的实际Compose回归已修，27项定向测试及独立复核通过；书籍/章节管理的空回执、明确拒绝、部分字段失败处理已修，完整1203项单测通过。见[本轮记录](20260912-management-receipts.md)。本轮新优化APK已完成MuMu覆盖安装和核心运行检查。

## 源码与安装包（分别记录）

- 已完成的近期修复：编辑器草稿与章节一致性、异步结果隔离、后台分章/序列化、临时EPUB取消清理、跳章后滚动进度、云配置保留无动画、系统大字号页眉页脚高度、书籍当前读写门槛读取与缺失时禁保存。
- 当前源码全量测试：1,203项、169个测试集、0失败/错误/跳过；Beta编译通过。R8第一次因系统本地内存不足崩溃，61157改用低内存/2逻辑核预算后assembleBeta成功，R8和资源压缩保持开启；19408独立lintDebug通过，0错误/5警告。
- 最新验证APK：`app/build/outputs/apk/beta/app-beta.apk`，3,259,834bytes，SHA-256 `bfeddab2d81f542baf4c8eaf92bc7a9cca681306410987b1ea64df138de911af`。包身份/签名沿用Beta6，非debug；静态验包报告`agent-bridge/artifacts/beta7-device/beta7-management-receipts-20260912.json`。
- 新包已完成MuMu安装复验；旧包`5b58ddbb...dbadade`仅用于历史证据，不能与本轮运行结果混用。

## 2026-09-13 最新包安装验收

- 新优化 APK `bfeddab2d81f542baf4c8eaf92bc7a9cca681306410987b1ea64df138de911af` 已通过 `adb install -r` 覆盖安装到 MuMu `127.0.0.1:16384`；设备读取哈希与本地完全一致，包名 `com.novalpie.app.debug`、版本 `2.0.0-native-beta7`、Android 15、540×1098、density240。
- 冷启动、收藏（65条目）、搜索、论坛五分区、个人页、应用设置、正文阅读器均可达；清空 logcat 后冷启动没有 App crash，ADB reverse 为 `host-13 tcp:7890 tcp:7890`。
- TTS 实机：开始/暂停/继续/停止可用；退到后台时 `ReaderPlaybackService` 为 foreground，音频 Frames written 持续增长；停止后服务退出。
- 大字号适配：320dp/字号2.0 下页脚时间、章节标题、百分比都在可视区域内，截图 `agent-bridge/screenshots/beta7-20260913-reader-320dp-font2-online.png`；12组阅读器尺寸/字号截图及汇总在 `agent-bridge/artifacts/beta7-device/20260913-reader-layout-matrix.json`。
- 本轮未执行任何真实转让、删除章节、改权限、公共规则发布或积分写入；AndroidX 分页 instrumentation 因测试 APK 缺少 `androidx.tracing.Trace` 未通过，生产 App 本身已单独启动并验证，不能把该测试环境失败计为分页通过。
- 设备安装初查：`agent-bridge/artifacts/beta7-device/20260913-beta7-install-runtime.json`（其中crashBufferEmpty=false来自uiautomator进程退出SIGSEGV，不是App业务崩溃）。最终正常冷启动与完整本轮报告：`agent-bridge/artifacts/beta7-device/20260913-beta7-core-runtime.json`；完整中文变更说明：[BETA7-CHANGES-ZH.md](BETA7-CHANGES-ZH.md)。

## 历史实机证据（不同包不混用）

- 真实Beta6覆盖到首个优化Beta候选：259项偏好、11个离线文件、登录身份保留。
- 5b58ddbb包：原生网页云配置创建、读取应用、更新、设默认、删除后刷新为空；唯一测试配置已清理。章节内百分比会变化并在重启后恢复。
- 同包纯文字热缓存三轮jank：0.72%、1.10%、0.34%，都低于5%。不是完整冷热/图列表/真机高刷认证。
- a7fdae0e包：44章原生EPUB，6正文图片+封面、全部ZIP/XML/外部阅读器打开；强调样式修复前后7张图字节哈希一致。
- 大书12.2GB完整导出及其他TTS/规则组合有更早分片证据；最终同包关键路径仍需复验。

## 仍未完成

1. 阅读数据层、部分书籍管理/评论/账号/考试及领域API的剩余拆分；不是所有根业务都已迁出。
2. 编辑器和上传批次完整实机流程、进程恢复；云配置网站自定义资源等兼容边界仍需补齐/确认。
3. 最新优化包上的连续章评、TTS焦点/耳机/跨章与规则组合、原文/替换×EPUB/TXT等最终组合验收。
4. 320/360/412/600dp×系统字号1/1.3/2、明暗/输入法/断网/返回、冷热图文性能及长期内存矩阵。
5. 剩余网站控件/接口证据、普通用户与管理员权限差异和受控管理操作；原六项代码问题及新管理回执分片已通过独立复核，但不是完整业务验收。
6. 整理最终提交、标签、中文说明、安装包与GitHub资产一致性；验收通过后新建Beta7 Release，保留Beta6。

## 当前环境阻塞

09-13主机资源恢复后MuMu已经启动，内存阻塞已解除。模拟器内旧手动代理10.0.2.2可造成22秒请求超时；本轮已通过App设置改成127.0.0.1:7890并保持ADB reverse，随后真实目录/详情请求完成。未改OS网络或真机默认策略。旧Debug AndroidX测试依赖和调试辅助活动不能直接用于R8包，需独立SDK/UI黑盒驱动；本轮没有为了通过而降低R8。

新增待修：目录请求失败、只读离线正文时，全书百分比仍显示0.00%。已有实机截图和源码定位，应改为未知状态而不是伪造零进度；与正常联网后2.27%→2.31%的进度更新分开记录。

## 反馈与历史

截至2026-09-13采集：App帖1871有39条根/嵌套回复；最近两条2453/2455仅询问旧WebView下载位置与指路，无新增Bug描述。反馈区472帖；新1951报告357637原文获取卡住，仍需核对源数据。缺图/作者换图不伪造补图。发布前需再次检查增量。

逐项需求见[requirements.md](requirements.md)，执行历史见[CURRENT_STATE.md](CURRENT_STATE.md)和[execution-log.md](execution-log.md)。历史文档中的“当前”只代表其记录时间，本摘要优先。
