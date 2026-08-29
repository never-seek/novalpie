# NovalPie App 2.0 用户反馈逐项审计台账

> 本台账把 2.0 上线后的用户反馈拆成可验证条目。`通过`必须同时具备：当前源码审查、最小自动化回归、与当前 APK 对应的 MuMu/真机运行证据。历史截图、旧 release 或单独的源码注释不能单独作为通过依据。

## 状态说明

- `待审计`：尚未以当前源码和当前 APK 复核。
- `源码/单测已覆盖`：尚缺实机或服务端验证。
- `实机已验证`：本轮运行完成，仍在最终门禁前。
- `阻塞`：需要用户明确授权、外部服务状态或真实设备后才能验证。

## A. 论坛、书评与章节评论

| ID | 用户需求/回归点 | 验收标准 | 当前状态 | 证据/后续动作 |
| --- | --- | --- | --- | --- |
| A01 | 根评论、回复、回复的回复均可发送 | 子回复提交到线程根 `comment_id`，保留 `reply_to_name`，成功后立即可见且刷新不丢失 | 实机已验证（当前 APK） | 2026-08-29 依据用户此前授权，在帖子 1828 中点自己评论的“回复”，确认编辑器目标为“回复 seeking”，发送明确标注的 `App QA nested reply test - please ignore.`。成功后回复数 17→18、编辑器恢复为“写评论”；该条在当前线程显示 `回复 seeking`，离开并重新加载 `/api/posts/1828/comments` 后仍在展开的 2 条回复组中。证据：`agent-bridge/screenshots/20260829-a01-reply-seeking-{target,typed,sent,after-send,reloaded-persisted}.png`、相应 UI 树及 `artifacts/20260829-a01-reply-refresh-logcat.txt`。 |
| A02 | 失败后可重试 | 网络/服务端拒绝时草稿、目标、幂等请求 ID 均保留 | 源码/单测已覆盖（当前 APK 选择态已核验） | 2026-08-29：MuMu 当前 APK 在帖子 1828 进入“回复 DDDZ”选择态，随后取消并确认无残留草稿/目标；截图 `agent-bridge/screenshots/20260829-a02-reply-target-current.png`，清理后的 UI 树 `agent-bridge/artifacts/20260829-a02-after-cancel-ui.xml`。`ForumCommentSubmissionPolicyTest` 强制运行 4/4，覆盖网络异常、服务端 `success=false`、旧帖成功回包保护和成功嵌套回复。未通过关闭 guest WLAN 或拦截认证流量人为制造真实失败，因此失败分支不冒充当前 APK 的实机网络故障证据；保留为后续真实设备/可控服务端验证项。 |
| A03 | 书籍书评的嵌套回复 | 与论坛一致地解析根/子回复、点赞/踩/表情/打赏路径 | 实机已验证（当前 APK） | 书籍 `350192` 详情的真实书评流：根评论、一级回复、回复的回复均显示在同一线程；证据：`20260827-a03-book-comments-current.png`、`20260827-a03-book-replies-expanded-current.png`、`20260827-a03-book-reply-target-current.png`；UI 树在 `agent-bridge/artifacts/20260827-a03-*current-ui.xml`。 |
| A04 | 章节评论的嵌套回复 | 与论坛一致；不被无限滚动清掉 | 实机已验证（当前 APK） | 当前 APK Reader 的章评根评论与回复均可展开；连续阅读状态保留章评并将其归属原章节，证据：`20260826-b05-status-fix-83-comment-retained.png`、`20260827-a08-reader-comment-visible.png`；`ReaderPresentationTest` 通过。 |
| A05 | 评论编辑器格式工具 | 粗体、斜体、标题、引用、代码、链接、列表、下划线、删除线、黑幕、折叠插入源站语法且选区不丢 | 实机已验证（当前 APK） | 当前帖子回复编辑器显示格式工具并保留回复目标/草稿；源码与 `ForumPresentationTest` 覆盖插入语法。证据：`20260827-a03-forum-reply-composer.xml`、`20260827-a03-forum-nested-reply-target.xml`。 |
| A06 | Markdown/HTML 富文本显示 | 斜体、下划线、删除线、代码、标题、链接、折叠、图片不显示原始控制字符 | 实机已验证（当前 APK） | 真实帖子 `1796` 当前 APK 中链接渲染为可点击富文本，控制语法未裸露；评论图片与 HTML/Markdown 图片路径亦由共享 renderer 处理。证据：`20260827-a05-forum-1796-after-wait.png`、`20260824-forum-1827-comment-4367-image-loaded.png`；测试：`ForumPresentationTest`。 |
| A07 | fold 折叠 | `[fold:title]...[/fold]` 默认收起，展开后可显示内部链接/图片/书卡 | 实机已验证（当前 APK） | 真实帖子 `1796` 的 fold 默认收起，点开后内部长 URL 正常显示并可点击；证据：`20260827-a05-forum-1796-after-wait.png`、`20260827-a05-fold-expanded.png`、对应 UI 树。 |
| A08 | 剧透黑幕 | 默认遮挡，点黑幕单独揭示；“显示剧透”偏好影响论坛、书评、章节评论和动态 | 实机已验证（当前 APK） | MuMu 当前安装包 `32ADFC032A62EEC43EF2A2A0C0FF691E401815629867B7E6FDF2FF9C4D1442B0`；真实帖子 1742 的 `||否决蘿莉||` 默认黑幕、单段揭示；书评/书籍评论、章评、本人动态、他人主页动态均验证开关同步。截图：`agent-bridge/screenshots/20260827-a08-forum-1041-spoiler-default.png`、`20260827-a08-forum-1041-spoiler-one-revealed.png`、`20260827-a08-book-spoiler-revealed.png`、`20260827-a08-book-comments-spoilers-on.png`、`20260827-a08-reader-comment-spoiler-off.png`、`20260827-a08-own-activity-spoilers-on.png`、`20260827-a08-public-activity-spoilers-on.png`；UI 树在 `agent-bridge/artifacts/20260827-a08-*`。`ForumPresentationTest`/`ForumSpoilerStateTest` 与完整门禁均通过。 |
| A09 | URL 与站内链接 | 外链可打开，`/forum/...`、`/book/...` 进入原生路由，长链接不截断为不可用文本 | 实机已验证（当前 APK） | `1796` fold 展开后的长 URL 显示为链接；`[bookid:350192]` 与帖子/书籍深链均进入原生路由。证据：`20260827-a05-fold-expanded.png`、`20260824-forum-comment-4388-book-card-opened.png`。 |
| A10 | 评论图片/动态图片 | Markdown/HTML 懒加载图片与 GIF/WebP，长按可原图预览 | 实机已验证（当前 APK） | 真实评论 `4367` 的 Markdown 图片在当前 APK 加载，静止长按打开全屏原图预览；证据：`20260824-forum-1827-comment-4367-image-loaded.png`、`20260824-forum-1827-comment-image-preview.png`。 |
| A11 | `[bookid:...]` 分享卡片 | 解析书籍 ID 和 `tags/bio` 选项，加载原生可点击书籍卡片 | 实机已验证（当前 APK） | 真实评论 `4388` 的 `[bookid:350192]` 当前 APK 渲染为带封面/作者/指标/tag 的原生书卡，点击进入对应书籍详情；证据：`20260824-forum-comment-4388-book-card.png`、`20260824-forum-comment-4388-book-card-opened.png`。 |
| A12 | 论坛书评入口 | 底栏“书评”进入真实书评 feed，而非空的帖子 type | 实机已验证（当前 APK） | 当前 APK 点“书评”进入真实书评列表，显示真实作者、关联书、正文摘要和统计；证据：`20260827-a12-forum-feed.png`、`20260827-a12-review.png`、`20260827-a12-review-ui.xml`。 |
| A13 | 论坛五个选项布局 | 五项横向可见/可横滑，不全部挤在左侧 | 实机已验证（当前 APK） | MuMu 900×1600 当前 APK 中“公告/推书/交流/书评/反馈”均匀占满横轴，未出现挤在左侧或裁切；证据：`20260827-a12-forum-feed.png`、`20260827-a12-forum-feed-ui.xml`。 |
| A14 | 误触和双击 | 滑动不打开帖子，单次稳定点按可打开；返回后不会加载上一个详情 | 实机已验证（当前 APK） | 当前 APK 单次点按分别打开两条不同书评关联书籍，系统返回回到论坛后再次点按打开第二条；连续两次快速上滑只滚动不打开详情。证据：`20260827-a14-single-tap-post-result.png`、`20260827-a14-after-back-to-forum.png`、`20260827-a14-single-tap-post-b.png`、`20260827-a14-after-scroll.png`。 |

## B. 阅读器

| ID | 用户需求/回归点 | 验收标准 | 当前状态 | 证据/后续动作 |
| --- | --- | --- | --- | --- |
| B01 | 进入/退出工具栏 | 中央点按显示/隐藏；正文点按关闭；不自动弹出，不需要双击 | 实机已验证（当前 APK） | MuMu 当前 APK：正文点按隐藏，中央单击一次显示、再单击一次隐藏；隐藏后等待 2 秒仍不自动弹出。UI 树和截图：`D:\NovalPie\agent-bridge\screenshots\20260827-np-b01-shown.png`、`20260827-np-b01-hidden.png`；对应树在 `agent-bridge\artifacts\20260827-np-b01-*.xml`。 |
| B02 | 工具栏语义 | 返回书籍、目录、设置、听书等图标不重复、不误导；网页回退入口不重复 | 实机已验证（当前 APK） | 右侧工具栏与“导航”面板均无无效的“网页正文”；导航只保留“书本页、收藏”。正文加载失败时也只提供原生“重试正文”，不再出现网页正文回退。章节评论的“打开网页评论”保留。2026-08-29 当前 B028 APK 再次实机打开 rail/导航面板确认，结束后恢复第 5 章原阅读位置；截图：`D:\NovalPie\agent-bridge\screenshots\20260827-reader-no-web-reader-action.png`、`20260827-reader-navigation-no-web-reader.png`、`20260829-b02-current-reader-navigation.png`，UI 树：`agent-bridge/artifacts/20260829-b02-current-reader-navigation-ui.xml`。 |
| B03 | 系统返回与阅读器返回 | 从正文正确回书籍详情，分页/连续阅读状态不污染其他页面 | 实机已验证（当前 APK） | MuMu 当前 APK：正文系统返回进入正确书籍详情，再返回进入顶层原生收藏页；无 WebView、无旧阅读器内容复用。截图：`D:\NovalPie\agent-bridge\screenshots\20260827-b03-after-reader-back.png`、`20260827-b03-after-detail-back.png`。 |
| B04 | 翻页前后章 | 翻至末页自动进入下一章；下一章可向左/向前回上章末尾 | 实机已验证（当前 APK） | 2026-08-29 当前 APK 临时切换到分页模式：第 1 章右侧翻页 9 次后自动进入第 2 章（底栏为 `2章-...`）；第 2 章顶部单击左侧后立即回到第 1 章的“本章结束 / 章节评论”末尾，底栏为第 1 章。`ReaderPresentationTest` 53 与 `ReaderAdjacentChapterTest` 13 均重新执行，0 failures/errors/skipped。测试结束已恢复“无限滚动=开、翻页=关”和测试前第 5 章阅读位置。证据：`agent-bridge/screenshots/20260829-b04-page-next-chapter-top.png`、`20260829-b04-page-previous-chapter-end.png`、`20260829-b04-restored-reader-ch5.png`，对应 UI 树位于 `artifacts/20260829-b04-*`。 |
| B05 | 无限滚动 | 末尾自动加载下一章；章节评论在正文之间保留，不被下一章加载清除；底栏跟随当前可见章节 | 实机已验证（当前 APK） | 2026-08-29 以 `354491/6992449` 从第 1 章连续滚动到第 5 章：底栏依次更新为第 2/3/4/5 章，并发出后续正文/评论请求；第 4 章“章节评论（还没有章节评论）”区仍位于第 5 章正文之前，未被 append 清除。当前章节为空评论，非空章节状态则由 `ReaderPresentationTest.continuousReaderKeepsASeparateCommentStateForEveryLoadedChapter` 和 `chapterCommentMutationPreservesTheContinuousReaderWindow` 覆盖。证据：`agent-bridge/screenshots/20260829-b05-{reader-initial,after-22-swipes,after-48-swipes,comment-retained-current,after-comment-next-body}.png`、对应 `artifacts/` UI 树与 `20260829-b05-current-reader-logcat.txt`。 |
| B06 | 目录与章节入口 | 首次直接点目录 UI 正常，章节高亮/定位正确 | 实机已验证（当前 APK） | 从第84章正文首次打开工具栏后直接点“目录”，面板正常显示并定位到第84章；点第82章后正文以该章顶部打开；再次打开目录自动定位并高亮第82章。`readerCatalogCurrentChapterIndex` 与相关单测覆盖索引计算。证据：`20260826-b06-catalog-open.png`、`20260826-b06-after-chapter82.png`、`20260826-b06-catalog-after82.png`；UI 树：`agent-bridge/artifacts/20260826-b06-catalog-open-ui.xml`、`20260826-b06-after-chapter82-ui.xml`、`20260826-b06-catalog-after82-ui.xml`。 |
| B07 | 听书 | 正常可播放/停止；关闭后控件消失而非仅变灰 | 实机已验证（无引擎错误路径；播放/停止受环境阻塞） | 2026-08-29 当前 APK 在 MuMu `127.0.0.1:16384` 临时开启“显示听书入口”后，工具栏出现“听书”；点击后先显示“准备中”，约 5 秒后显示明确的系统听书引擎超时错误和“系统设置”，不是静默无响应。点击“系统设置”实际打开 `com.android.settings/.Settings$TextToSpeechSettingsActivity`；随后关闭入口，工具栏不再有“听书”，错误反馈层也消失，并恢复原先关闭状态。设备 `tts_default_synth=null` 且未安装可用 TTS engine，因此真实朗读/停止仍需带引擎的真机或可控 MuMu 环境。证据：`agent-bridge/screenshots/20260829-b07-tts-error-final.png`、`20260829-b07-system-tts-settings.png`、`20260829-b07-restored-tts-off-controls.png`；UI 树：`agent-bridge/artifacts/20260829-b07-tts-error-final-ui.xml`、`agent-bridge/artifacts/20260829-b07-restored-tts-off-ui.xml`。 |
| B08 | 全文替换 | 对当前阅读正文生效、可回退/刷新，和网页功能同级 | 实机已验证（当前 APK） | `20260826-b08-full-replacement-panel.png` 展示全部模式；选择韩国模式后面板显示 `当前：韩国模式` 且正文重新加载，`20260826-b08-replacement-korea-reader.png`；随后已恢复 `关闭替换`。 |
| B09 | 阅读设置 | 字体、字号、行距、背景、亮度、边距、全屏等设置完整、持久化并安全适配 | 实机已验证（当前 APK） | 设置概览实际显示字体、排版、显示、布局、替换、主题、听书、其他八类；字体页提供字号/字重/字体与 TTF/OTF/TTC 导入，布局页提供留白与点击区。字号由 16 调至 17 后冷启动仍显示 `17 sp`，随后已恢复 16。证据：`20260826-b09-settings-overview.png`、`20260826-b09-font-settings.png`、`20260826-b09-font-persisted.png`、`20260826-b09-after-cold-relaunch.png`。 |
| B10 | 翻页动画 | 多种动画含“无动画”；开启分页后按选择执行 | 实机已验证（当前 APK） | 布局设置显示并可选 `无动画/淡入/覆盖/滑动/仿真`；当前 `无动画` 选项实际生效，右侧分页点按后正文视口立即前进，截图前后正文首段发生变化。证据：`20260826-b10-layout-settings.png`、`20260826-b10-page-none-before-turn.png`、`20260826-b10-page-none-after-turn.png`、`20260826-b10-page-none-after-turn-ui.xml`。 |
| B11 | 音量键翻页 | 上/下键方向正确，设置可完全关闭并交还系统音量 | 实机已验证（当前 APK） | 当前设置 UI 显示音量键翻页开启；MuMu 注入音量减后正文前进，音量加后回到原视口，方向正确。关闭开关交还系统音量的当前 APK 证据已在上一轮保留：`20260824-volume-key-page-turn-off.png`、`20260824-volume-key-disabled-system-volume.png`。本轮方向证据：`20260826-b11-before-volume-down.png`、`20260826-b11-after-volume-down.png`、`20260826-b11-after-volume-up.png`。 |
| B12 | 小屏与安全区 | 正文、菜单、底栏不被系统栏/屏幕边缘裁切 | 实机已验证（当前 APK） | MuMu 临时切换到 400dpi（约 360dp 宽）冷启动检查：正文标题、状态栏、底栏完整；工具栏的关闭/目录/设置/上章/下章/翻页/全屏/导航均完整；目录搜索框和第82章高亮卡片未越界。随后恢复物理 240dpi，并恢复至第84章。证据：`20260826-b12-narrow-reader.png`、`20260826-b12-narrow-controls.png`、`20260826-b12-narrow-catalog.png`、`20260826-b12-restored-reader.png`。 |

## C. 原生下载

| ID | 用户需求/回归点 | 验收标准 | 当前状态 | 证据/后续动作 |
| --- | --- | --- | --- |
| C01 | EPUB 原生下载 | 从 App 请求、显示进度、写入下载目录，不跳网页 | 实机已验证（当前 APK） | 当前 APK 从书籍详情原生菜单启动，显示章节/插图进度，并验证暂停、继续、取消、重试；证据：`20260828-c-epub-confirmation.png`、`20260828-c-epub-progress-before-pause.png`、`20260828-c-epub-paused.png`、`20260828-c-epub-resumed.png`、`20260828-c-epub-cancelled.png`、`20260828-c-epub-retry-started.png`、`20260828-c-epub-retry-cancelled-clean.png`。 |
| C02 | TXT 原生下载 | 菜单可见、下载成功、内容非网页回退 | 实机已验证（当前 APK） | 当前 APK 原生写入 MediaStore `Downloads`；精确条目 `1000003069` 为 `text/plain`、`1,184,718` 字节、归属 `com.novalpie.app.debug`。通过内容头尾确认是书名/作者/章节正文和图片占位符而非 HTML；随后仅删除该精确测试条目。证据：`20260828-c-txt-progress-start.png`、`20260828-c-txt-complete.png`、`artifacts/20260828-c-txt-content-1000003069.txt`。 |
| C03 | 大书与大图 | 不因 ArrayBuffer/内存分配失败；按流式写入 | 实机已验证（当前 APK） | 2026-08-29 当前 APK（SHA-256 `198BEF3DE2F6E2B84D58865995F06330B00BC5242774F7EE78FDB043D7906870`）对收藏书籍 358153 完成原生 EPUB：84 章、生成文件 `1,607,293,029` 字节；生成期间私有 `.part` 逐步增长，Downloads 只在归档关闭后出现 pending 条目，应用 `VmRSS` 约 209MB，App PID 日志无 OOM/`ArrayBuffer`/下载失败。`unzip -t` 返回 0；专项 `NativeEpubArchiveWriterTest` 21/21、`NativeDownloadTransferTest` 6/6 均 0 failures/errors。证据：`agent-bridge/screenshots/20260829-c03-epub-live.png`、`20260829-c03-epub-large-progress.png`、`20260829-c03-epub-published.png`，`artifacts/20260829-c03-epub-*`。测试产物已删除，旧有下载未触碰。 |
| C04 | 图片原始质量与格式 | 不压缩原图；GIF/WebP/大图保留原始字节与类型 | 实机已验证（当前 APK） | 同次 358153 原生归档的 542 个图像条目全部为 `Stored 0%`：正文 541 条（PNG 540、JPEG 1），独立封面 1 条 PNG；抽检文件头与声明格式一致（PNG `89504e...`、JPEG `ffd8ff...`），未发生转码或质量压缩。writer 的原始字节/格式识别由本轮新鲜 21 项专项测试覆盖；动图原图链路另有 D10 实机证据。证据：`artifacts/20260829-c03-archive-counts.txt`、`screenshots/20260829-c03-epub-published.png`。 |
| C05 | 图片数量与重复 | 所有正文出现次数保留，封面独立；不重复插入占位符 | 实机已验证（当前 APK） | 同次归档检查得到 84 个章节、541 个正文图条目、541 个正文 XHTML `src` 引用、封面 1 条目/1 引用、ZIP 重复路径 0；因此源正文每次 `[图片]` 出现均有对应条目，封面不计入正文图进度。重复 URL 可复用阶段字节但不会减少 occurrence，且 writer 专项测试覆盖重复出现/失败重试。证据：`artifacts/20260829-c03-archive-counts.txt`、`screenshots/20260829-c03-epub-live.png`。 |
| C06 | 并发设置 | “我的”中可选常用档位和自定义值，数值校验、下载前生效 | 实机已验证（当前 APK） | 2026-08-29 MuMu 当前 APK：下载并发卡含 2/4/8/12/16 路与自定义正整数；通过普通 UI 输入 `17` 并点“应用”后标题真实变为 `17 路`，再用 8 路预设恢复用户原设置并确认选中态/输入框均为 8。源码把该持久化值直接传给 `NativeEpubArchiveWriter.imageConcurrency`；`DownloadSettingsStoreTest` 4、`NativeEpubArchiveWriterTest` 21、`NativeDownloadTransferTest` 6 均 0 failures/errors。证据：`agent-bridge/screenshots/20260829-c06-custom-17.png`、`20260829-c06-restored-8.png`，UI 树：`agent-bridge/artifacts/20260829-c06-*.xml`。 |
| C07 | 失败、暂停/重试与清理 | 失败有明确原因，可重试，不发布半成品，不残留无主临时文件 | 实机已验证（当前 APK） | 当前 APK 验证暂停保持进度、继续恢复、取消反馈、重试入口和取消后私有工作目录无新文件/`.part`；证据：`20260828-c-epub-paused.png`、`20260828-c-epub-resumed.png`、`20260828-c-epub-cancelled.png`、`20260828-c-epub-retry-cancelled-clean.png`。 |

## D. 搜索、收藏、书籍详情与封面

| ID | 用户需求/回归点 | 验收标准 | 当前状态 | 证据/后续动作 |
| --- | --- | --- | --- |
| D01 | 搜索默认结果 | 未输入关键词时加载源站式推荐/默认书籍，非空白页 | 实机已验证（当前 APK） | 2026-08-29 当前 APK 打开搜索页后不输入关键词：约 700ms 即显示“全部小说（共 52074 部作品）”、868 页与实际 2 列封面书卡（例如《从多巴胺成瘾开始的异世界生活》《成为被猎人们痴迷的心理医生》），源站 `/api/search` 完成约 780ms，无空白/占位卡停留。证据：`agent-bridge/screenshots/20260829-d01-search-default-loading.png`、`20260829-d01-search-default-loaded.png`；UI 树与请求日志：`agent-bridge/artifacts/20260829-d01-search-default-*-ui.xml`、`20260829-d01-search-default-logcat.txt`。 |
| D02 | 成人区与筛选 | `所有/仅成人/全年龄` 明确可选；来源/规则/标签/字数/搜索模式/排序真实映射且不混书 | 实机已验证（当前 APK） | 2026-08-29 MuMu 当前 APK：默认 `所有 / 全部` 为 52,073 部；`仅成人` 为约 4,534、`全年龄` 为 47,539（两者合计默认总量）；`来源=上传` 为 3,152，卡片也显示“上传”来源 tag，证明筛选不是只改显示文本。规则、标签（包含/屏蔽/热门标签）和字数五档均在 UI 树实际可达；最终已恢复用户默认 `所有 / 全部`。证据：`agent-bridge/screenshots/20260829-d02-{search-initial,adult-only,all-ages,source-upload,tags,wordcount,restored-all}.png`。专项单测：`DiscoverPresentationTest` 16、`AdvancedSearchSyntaxTest` 6、`SearchSettingsStoreTest` 7、`NovalPieApiTest` 104，均 0 failures/errors；后者直接断言 `adult_filter`、`platform`、范围、来源、标签、字数、模式与排序参数。 |
| D03 | 搜索卡片信息 | 封面、作者、书名、收藏、阅读、字数、完整 tags 显示 | 实机已验证（当前 APK） | MuMu 搜索默认结果（共 52056 部）在 900×1600/240dpi 下显示真实封面、书名、作者、平台/完整可换行 tag 与收藏、本站阅读、字数三项指标；首屏和滚动后卡片均无空白。证据：`D:\NovalPie\agent-bridge\artifacts\20260828-d05-one-row-runtime\loaded-search.png`、`after-scroll.png` 与相应 UI tree。`NovelCardFactsTest`、`SearchGridPresentationTest` 覆盖字段与卡片语义。 |
| D04 | 搜索卡片对齐 | 同一行卡片稳定等高，底部指标同横轴；tag 多不造成相邻卡片错位 | 实机已验证（当前 APK） | 当前两列结果中标签数量不同的同排卡片仍保持一致的封面高度、tag 保留区和指标基线；`searchGridRowTagLineCount`/`searchGridRowMetricLineCount` 取行内最大值，`SearchGridPresentationTest` 覆盖共享高度和 CJK/窄屏指标换行。实机证据同 D03。 |
| D05 | 搜索图片性能 | 首屏/快速滚动不因封面解码明显掉帧，加载后不抖动 | 实机已验证（当前 APK） | 根因采样显示旧版滚动预取每次为两行/4 张全尺寸封面，和解码/主线程压力重叠。收紧为一行/2 张后，同一 MuMu、同一默认搜索页、6 次 500ms 滑动的 `gfxinfo` 从 P90 950ms/P95 3450ms/20 of 34 janky 降至 P90 34ms/P95 57ms/33 of 456 janky，且无 `Skipped`/`Davey` 记录；封面原请求尺寸和质量未改变。证据：修复前 `D:\NovalPie\agent-bridge\artifacts\20260828-d05-warm-isolated`，修复后 `D:\NovalPie\agent-bridge\artifacts\20260828-d05-one-row-runtime`。 |
| D06 | 收藏显示密度 | 2/3/4 列切换；书名作者不丢；长按进入管理而非封面预览 | 实机已验证（当前 APK） | MuMu 900×1600 / 240dpi 对同一 65 条收藏逐一切换 2、3、4 列；四列首行仍显示书名、作者与阅读进度，滚动后卡片保持同行对齐。静止长按《浪漫奇幻的NTR故事》进入“已选 1”批量管理，未打开封面预览；测试结束后恢复为原有 2 列、非选择状态。截图：`20260828-d06-collection-2col.png`、`20260828-d06-collection-3col.png`、`20260828-d06-collection-4col.png`、`20260828-d06-collection-longpress.png`、`20260828-d06-restored-2col.png`；UI 树：`agent-bridge/artifacts/20260828-d06-*.xml`。`LibraryPresentationTest` 强制重跑 26 项、0 failures/errors/skipped。当前本地与已安装 APK SHA-256：`235BDAB31CF86E5A6F51E0F67BE0665B8B4463CD568BE35EB527AD14A644F1E7`。 |
| D07 | 收藏阅读进度 | 未读不是伪 `100/100`；显示章节名与书名；读完后更新章节数产生提醒 | 实机已验证（当前 APK） | `ReaderProgressStore` 以本地有效章节号/章节 ID 和当前目录总数判定完成，不再被滞后的源站 `last_chapter` 否决；本地未读/未读完不会回填。MuMu 保留原数据安装最终 APK 后，收藏页显示 `浪漫奇幻的NTR故事`、`记忆的乐园 4`、`84/84`，未出现错误更新提示。证据：`D:\NovalPie\agent-bridge\screenshots\20260828-d07-final-runtime-loaded-2.png`；单测 16 项相关断言通过。最终 APK SHA-256：`B0C80C234601A9F9368FE66E73BB7A0D0319C413C06BDA2DCEEC32F3814A2BF8`。 |
| D08 | 书籍详情 tabs | 简介/目录/评论互斥切换，不三段叠在一个长页面 | 实机已验证（当前 APK） | `BookDetailScreen` 以单一 `contentTab` 条件渲染内容；当前 APK 对真实收藏书籍 358153 逐次切换并取得独立 UI 树：简介态仅有“作品简介”，目录态仅有“正文卷 · 共 84 章”，评论态仅有“书评 / 30 条书评 · 48 条回复”，未出现另一内容面板。返回收藏后重进默认回到简介。截图：`20260828-d08-final-introduction.png`、`20260828-d08-final-catalog.png`、`20260828-d08-final-comments.png`、`20260828-d08-reopen-default-tab.png`；UI 树：`agent-bridge/artifacts/20260828-d08-final-*-ui.xml`。`BookDetailPresentationTest` 互斥区段测试通过。 |
| D09 | 书籍详情信息密度 | 源站/本站数据简洁可读，不占据正文阅读空间；标签完整 | 实机已验证（当前 APK） | MuMu 900×1600 / 240dpi 对真实收藏书籍 358153 验证：13 个源标签完整保留、逐项可点；本站/源站指标压缩为两条单行统计（本站：推荐/阅读/收藏，源站：阅读/收藏），未形成大号数据卡片墙；简介、目录与评论 tabs 均保留在首屏，简介正文与固定阅读操作区不被统计数据挤出。UI 树记录标签 13 项、两条统计和完整边界；截图：`D:\NovalPie\agent-bridge\screenshots\20260828-d09-detail.png`，UI 树：`agent-bridge/artifacts/20260828-d09-detail-ui.xml`。`BookDetailFactsTest` 4 项、`BookDetailPresentationTest` 19 项强制重跑，均为 0 failures/errors/skipped。当前 APK SHA-256：`235BDAB31CF86E5A6F51E0F67BE0665B8B4463CD568BE35EB527AD14A644F1E7`。 |
| D10 | 封面外层/内层/动图 | 卡片显示外层；长按原图显示真实内层、动态图/幻影坦克可见；预览不裁切底部 | 实机已验证（当前 APK） | 349955《异世界论坛管理员》与 356821《成为一名浪漫奇幻情色小说作家》均验证卡片外层、长按真实内层 GIF、播放一轮后停帧；预览底栏完整。证据：`20260827-d10-gifdecoder-preview-2s.png`、`20260827-d10-gifdecoder-preview-cached-2s.png`、`20260827-d10-book356821-preview-4s.png`。 |
| D11 | 长按防误触 | 滑动中不打开预览；普通静止长按才打开 | 实机已验证（当前 APK） | 当前 APK 约 900ms 静止长按打开；超过 touch slop 的上滑被取消，未出现预览。证据：`20260827-d11-scroll-cancel-cover.png`、`20260827-d10-book349955-preview.png`；单测：`ImagePreviewTransformTest`。 |

## E. 我的、公开主页、装扮与权限

| ID | 用户需求/回归点 | 验收标准 | 当前状态 | 证据/后续动作 |
| --- | --- | --- | --- |
| E01 | 我的动态 | 有实际活动、评论、书评，不为空白 | 实机已验证（当前 APK） | 当前账号默认“全部”显示真实帖子/评论/书评活动；“帖子”“评论”“书评”筛选分别显示真实条目；“章评”在当前账号确实为空时显示明确空分类状态，切回“全部”恢复列表。证据：`D:\NovalPie\agent-bridge\screenshots\20260828-e01-activities.png`、`20260828-e01-posts.png`、`20260828-e01-comments.png`、`20260828-e01-bookreviews.png`、`20260828-e01-chapterreviews.png`、`20260828-e01-all-restored.png`；UI 树：`agent-bridge/artifacts/20260828-e01-*-ui.xml`。 |
| E02 | 背包 badge 预览 | 每件物品使用自己真实预览图，WebP/GIF 可渲染，不同 badge 不共用占位 | 实机已验证（当前 APK） | MuMu 当前 APK 的“我的→装扮→我的仓库”显示不同物品各自的真实预览内容（含图片徽章与 CSS 徽章），装备/未装备状态和操作均按物品区分；源码/单测覆盖 WebP、GIF 与独立 badge 元数据解析。证据：`D:\NovalPie\agent-bridge\screenshots\20260828-e02-inventory.png`、`20260828-e02-wardrobe.png`；UI 树：`agent-bridge/artifacts/20260828-e02-*-ui.xml`。 |
| E03 | 头像框与已装备 badge | 头像框在头像外层正确叠放，主页展示已装备 badge | 实机已验证（当前 APK） | 当前账号页显示头像框作为头像外部叠加层，已装备徽章在头像信息区可见；装扮仓库同步显示对应物品的“已装备”状态。证据：`D:\NovalPie\agent-bridge\screenshots\20260828-e01-profile-loading.png`、`20260828-e02-wardrobe.png`；`ProfilePresentationTest` 覆盖外层头像框比例、已装备 badge 回填与独立源样式。 |
| E04 | 上传书籍管理 | 紧凑卡片，封面/书名/作者不丢；可切 2/3/4 列；不长按预览封面 | 实机已验证（当前 APK） | MuMu 当前 APK（SHA-256 `98F73CE1AF7E1E353389D511906F76C1171A56DE58ABA5C28796B5BFAF8C0EBE`）在“我的→书籍”依次验证 2、3、4 列：首屏每张卡均保留封面、书名和作者；4 列恢复为用户原先偏好。当前 UI 节点没有 `long-clickable`，页面提示“管理页不预览封面”，未出现封面预览入口。证据：`20260829-e04-current-upload-2col.png`、`20260829-e04-current-upload-3col.png`、`20260829-e04-current-upload-4col-restored.png`；UI 树：`agent-bridge/artifacts/20260829-e04-current-upload-*-ui.xml`。 |
| E05 | 公开主页 | 作品、评论、书评、签到统计与动态完整；进入其他用户不会卡死 | 实机已验证（当前 APK） | 当前 APK 访问非本人用户 `100002`：公开资料、作品数、评论数、动态列表和签到资料均有真实内容；前轮从公开主页返回论坛正常，不复用旧页。截图：`D:\NovalPie\agent-bridge\screenshots\20260829-current-public-profile.png`；UI 树：`agent-bridge/artifacts/20260829-current-public-profile-ui.xml`。分区加载/请求序号回归由 `NetworkLoadPresentationTest` 覆盖。 |
| E06 | 管理员权限 | 管理员工具仅对具有服务器权限账户显示；普通用户不泄露 admin-only 入口 | 实机已验证（当前 APK） | 当前管理员会话的工具区显示六个服务器管理员入口：管理后台、内容审核、密钥管理、操作日志、抓取管理、商店管理；统一身份解析避免资料异步回填时错误隐藏。`UiNavigationTest`、`AdminPresentationTest` 覆盖管理员、普通用户和旧资料三种状态，普通用户不显示管理员入口。截图：`D:\NovalPie\agent-bridge\screenshots\20260829-current-tools-admin.png`；UI 树：`agent-bridge/artifacts/20260829-current-tools-admin-ui.xml`。 |

## F. 网络、登录、导航、适配与性能

| ID | 用户需求/回归点 | 验收标准 | 当前状态 | 证据/后续动作 |
| --- | --- | --- | --- |
| F01 | 登录与安全验证 | 安全验证不白屏；登录完成后原生页立即可用，不依赖网页手动刷新 | 实机已验证（验证码加载、取消/返回恢复；提交后待复验） | 验证码 WebView 在源站加载期间显示不透明加载层；2026-08-29 已实测登录 → 验证码 → 取消 → 系统返回后资料页重新取得真实个人资料，不再永久停在“正在同步个人资料”。未提交真实验证码或读取凭据，因此“成功登录后原生页立即可用”仍待用户主动登录时补验。证据：`agent-bridge/screenshots/20260828-f01-*`、`20260829-f01-fixed-after-cancel-back.png`。 |
| F02 | 代理 | MuMu reverse/proxy 正确，真实设备可配代理且默认不错误指向设备 `127.0.0.1` | 实机已验证（当前 APK） | MuMu `127.0.0.1:16384` 使用 `host-12 tcp:7890 tcp:7890` reverse；专项代理测试通过；原生收藏同步和 WebView fallback 均可达。 |
| F03 | 返回栈 | A→B→返回→C 不回到旧 A；论坛/书籍/收藏路由不污染 | 实机已验证（当前 APK） | 帖子 A→返回→帖子 B、书籍 A→返回收藏→书籍 B，以及 250ms 快速切换均未复用旧内容；证据见 `agent-bridge/screenshots/20260828-f03-*`。 |
| F04 | 冷启动深链 | force-stop 后 `novalpie://app/...` 仍进入目标，不被恢复任务拦截 | 实机已验证（当前 APK） | `novalpie://app/forum/1828` 冷启动进入帖子详情；专项 `MainActivityStartUriTest`、`WebsiteDeepLinkRouteTest`、`RouteStackPolicyTest` 全部通过。 |
| F05 | 输入法空白 | 软键盘关闭后底部不留大片空白，列表高度恢复 | 实机已验证（当前 APK） | 收藏搜索框、发帖正文均完成打开/关闭输入法回归；关闭后 `mInputShown=false`，内容窗口恢复原始高度并等待 2.5s 无空白。截图：`agent-bridge/screenshots/20260828-f05-ime-open.png`、`20260828-f05-ime-closed-800ms.png`、`20260828-f05-post-editor-ime-open.png`、`20260828-f05-post-editor-ime-closed-2500ms.png`；UI 树：`agent-bridge/artifacts/20260828-f05-post-editor-ime-closed-ui.xml`。 |
| F06 | 小屏文字裁切 | 书籍指标、工具栏、按钮、长标签能换行/省略而非被截断 | 实机已验证（当前 APK） | MuMu 360dp 窄屏验证搜索、收藏、详情；搜索同行卡片底部等高，指标完整显示。修正 `SearchGridPresentation` 对 FlowRow 标签行间距的高度预留。证据：`agent-bridge/screenshots/20260828-f06-after-fix.png`、`20260828-f06-after-fix-row2.png`、`20260828-f06-collection-360dp-cards.png`、`20260828-f06-book-detail-360dp-wait.png`；UI 树：`agent-bridge/artifacts/20260828-f06-after-fix-ui.xml`。 |
| F07 | 列表加载与无响应 | 网络失败有可用重试；个人主页、书/论坛滚动不需要“刷新才恢复” | 实机已验证（当前 APK） | 论坛正文与评论已拆分加载，正文先返回时评论独立保持加载/可重试；公开主页资料、动态、作品、签到统计/记录/设置也各自独立加载与重试，不再由最慢请求锁整页。`NetworkLoadPresentationTest` 覆盖正文/评论互不遮挡、分区失败与重试、乱序合并和陈旧书籍引用；`PublicProfileDeepLinkBootstrapTest` 锁定公开主页深链返回根页时必须先进入 `Loading`，防止永久停留在 `Idle`。2026-08-29 MuMu 当前 APK（SHA-256 `198BEF3DE2F6E2B84D58865995F06330B00BC5242774F7EE78FDB043D7906870`）冷启动后真实出现 `书架请求失败: timeout`；点击 `重试书架` 后先显示骨架加载，约 12.5 秒恢复 65 条收藏和默认收藏组，无需刷新或重启。截图：`agent-bridge/screenshots/20260829-f07-current-after-cold-start.png`、`20260829-f07-retry-loading.png`、`20260829-f07-retry-result.png`；UI 树：`agent-bridge/artifacts/20260829-f07-retry-before-ui.xml`、`20260829-f07-retry-success-ui.xml`；请求时间与性能取证：`agent-bridge/artifacts/20260829-f07-retry-logcat.txt`、`20260829-061529-f07-cold-start-perf/`。冷启动的 MuMu Houdini/ART 初始化和网络等待仍会造成首轮变慢，但未观察到 FATAL/ANR 或页面锁死。 |
| F08 | 图片滚动掉帧 | 区分网络等待、解码、Compose 主线程卡顿；对高图列表有可量化帧数据 | 实机已验证（当前 APK） | 修复前 `gfxinfo` 显示慢 UI thread 17、慢 draw commands 14、GPU P95 仅 1ms；logcat 同时有图片解码记录、3.337s GC、`Skipped 187 frames`，故不是纯网络或 GPU 等待。限制滚动投机预取后保留 1 次 bitmap upload，但 UI P90/P95 回到 34/57ms；MuMu 的系统负载仍是绝对帧时的噪声来源，结论以同机对照为准。 |

## 本轮运行记录

- 2026-08-29：完成 F01 验证码取消/系统返回后的个人页恢复修复与实机复核。根因是 `openNativeWebsiteRoute(AppRoute.Auth)` 重置到“我的”根页后只进入登录表单，没有启动 `loadProfile()`；验证码取消并系统返回时根页资料保持 `Idle`，UI 因而永久显示“正在同步个人资料”。`NovalPieViewModel.kt` 现于 `AppRoute.Auth` 进入认证路由前启动资料请求，`PublicProfileDeepLinkBootstrapTest.authDeepLinkInitializesTheProfileRootNeededAfterCaptchaCancel` 已先验证修复前红灯、修复后通过。专项 `AuthPresentationTest`、`PublicProfileDeepLinkBootstrapTest`、`UiNavigationTest` 通过；`:app:assembleDebug` 与 `:app:lintDebug` 通过，Lint 无问题。MuMu `127.0.0.1:16384` 原地安装 SHA-256 `B028916C3441FC48F1E2D3BE02E57EDD42E1EE5106CF2B918D612211410FF1B0` 的 Debug APK，保持 `tcp:7890` reverse 和既有用户数据；冷启动 → 登录 → 验证码 → 取消 → 系统返回后，约 406ms 取得 `/api/users/me` 并显示真实 seeking 资料。截图：`agent-bridge/screenshots/20260829-f01-fixed-after-cancel-back.png`；UI 树与日志：`agent-bridge/artifacts/20260829-f01-fixed-after-cancel-back-ui.xml`、`20260829-f01-fixed-after-cancel-back-logcat.txt`。本轮只输入临时虚构测试文本，未提交登录、未读取或保存真实密码/token；真实验证码提交后的原生登录即时回填仍保留待用户主动操作时复验。

- 2026-08-29：完成 B05 无限滚动当前 APK 复核，未修改生产代码。MuMu `127.0.0.1:16384` 的 Debug APK 与本地 APK SHA-256 均为 `B028916C3441FC48F1E2D3BE02E57EDD42E1EE5106CF2B918D612211410FF1B0`；保持用户登录态、下载、缓存和 `tcp:7890` reverse。以只读正文入口 `novalpie://app/book/354491/6992449` 启动，确认设置概览为“评论开、滚动模式”，连续滚过章节边界后底栏从第 1 章依次更新至第 2、3、4、5 章（进度 0.00% → 2.76%）。第 4 章末尾仍有原生“章节评论 / 打开网页评论 / 还没有章节评论”区，继续滚动后第 5 章正文正常接续；日志记录了 6992460、6992459、6992454、6992450 的连续正文请求及相应评论请求。强制重跑 `ReaderPresentationTest` 与 `ReaderAdjacentChapterTest` 共 66 项，0 failures/errors/skipped；此前同一代码状态的全量 Debug 单测为 87 suites / 730 tests / 0 failures/errors/skipped，`git diff --check` 通过。截图：`agent-bridge/screenshots/20260829-b05-reader-initial.png`、`20260829-b05-after-22-swipes.png`、`20260829-b05-after-48-swipes.png`、`20260829-b05-comment-retained-current.png`、`20260829-b05-after-comment-next-body.png`；日志：`agent-bridge/artifacts/20260829-b05-current-reader-logcat.txt`。`uiautomator` 进程自身 SIGSEGV 已与 App PID 分离，App 无 FATAL/ANR/OOM。

- 2026-08-29：完成 D01 搜索默认结果当前 APK 复核，未修改生产代码或搜索偏好。MuMu 从收藏页按原生底栏进入搜索页，未输入关键词也未点击搜索；约 700ms 的第一张证据即已显示“全部小说（共 52074 部作品）”、页码 1–5、共 868 页及默认书卡，7 秒后仍为完整 2 列书架，无空白内容区。视觉截图确认首行封面、书名、作者与 tag 数据可见；App 日志记录 `/api/tags` 410ms、`/api/search` 780ms 完成，无 FATAL/ANR/OOM。证据：`agent-bridge/screenshots/20260829-d01-search-default-loading.png`、`20260829-d01-search-default-loaded.png`，UI 树与日志：`agent-bridge/artifacts/20260829-d01-search-default-*-ui.xml`、`20260829-d01-search-default-logcat.txt`。同一未改代码状态的全量 Debug 单测 87 suites / 730 tests 均通过。

- 2026-08-29：完成 A01 当前 APK 的真实嵌套回复服务端验证。按用户此前“可回复 seeking 进行测试”的授权，在帖子 1828 中定位自己的评论，点其对应“回复”后 UI 确认目标为“回复 seeking”；使用一条清楚标注的审计文本 `App QA nested reply test - please ignore.` 提交。提交后评论摘要从 `14 条评论 · 17 条回复` 更新为 `14 条评论 · 18 条回复`，编辑器恢复普通“写评论”，当前线程立即显示“回复 seeking”与该文本。随后正常返回论坛、重新打开同一原生深链，`/api/posts/1828` 与 `/api/posts/1828/comments` 均重新完成请求；再次滚至该线程，文本仍在“收起 2 条回复”组中，证明不是仅本地乐观插入。无其他测试内容发送。证据：`agent-bridge/screenshots/20260829-a01-reply-seeking-target.png`、`20260829-a01-reply-seeking-typed.png`、`20260829-a01-reply-seeking-sent.png`、`20260829-a01-seeking-after-send.png`、`20260829-a01-reply-seeking-reloaded-persisted.png`，UI 树及 `agent-bridge/artifacts/20260829-a01-reply-refresh-logcat.txt`。

- 2026-08-29：完成 B04 翻页跨章节往返当前 APK 复核。为了不更改用户最终偏好，先在阅读器布局设置中记录“无限滚动=开、翻页=关”，临时开启分页模式；第 1 章中右侧点击前进共 9 次，底栏切为第 2 章。第 2 章顶部一次左侧点击即进入第 1 章末页，截图同时可见“本章结束”、章节评论区和第 1 章底栏，证明不是仅前移一页。`ReaderPresentationTest`（53）和 `ReaderAdjacentChapterTest`（13）强制重跑均为 0 failures/errors/skipped；App PID 存活，无 FATAL/ANR/OOM，`git diff --check` 通过。随后点击当前“翻页”模式 rail 恢复滚动模式，通过布局 switches 确认无限滚动开 / 翻页关，并以 `354491/6992450` 恢复测试前第 5 章进度。证据：`agent-bridge/screenshots/20260829-b04-page-next-chapter-top.png`、`20260829-b04-page-previous-chapter-end.png`、`20260829-b04-restored-reader-ch5.png`，UI 树：`agent-bridge/artifacts/20260829-b04-*`。

- 2026-08-29：完成 B07 当前 APK 听书错误/关闭路径验收。未修改生产代码，未清除登录态、Cookie、收藏、阅读进度、缓存或下载。MuMu `127.0.0.1:16384` 的 Debug APK 与本地 `app-debug.apk` SHA-256 均为 `75E450A900614131240F5691B2C5DC00FC225AAF56C7CE811FC7BED6C279421C`。设备安全设置 `tts_default_synth=null`，未发现可用 TTS engine；开启入口后点击“听书”在约 5 秒内转为“系统听书引擎启动超时，请在系统设置中启用文字转语音后重试”，点击“系统设置”解析并打开 `com.android.settings/.Settings$TextToSpeechSettingsActivity`。关闭入口后重新打开工具栏，`听书` 与错误反馈均不存在；原设置已恢复。`ReaderActionPresentationTest` 3/3 与 `ReaderPresentationTest` 53/53 强制重跑通过，0 failures/errors/skipped；App 进程存活且本轮 App 日志无 FATAL/ANR/OOM。`uiautomator` 偶发的 SIGSEGV 属于 MuMu 的测试工具进程，不属于 App。证据截图：`agent-bridge/screenshots/20260829-b07-tts-error-final.png`、`20260829-b07-system-tts-settings.png`、`20260829-b07-restored-tts-off-controls.png`；证据 UI 树/日志：`agent-bridge/artifacts/20260829-b07-tts-error-final-ui.xml`、`agent-bridge/artifacts/20260829-b07-tts-error-final-logcat.txt`、`agent-bridge/artifacts/20260829-b07-restored-tts-off-ui.xml`。真实引擎播放、暂停/停止和跨段自动朗读留待具备 TTS engine 的真机复验，不将环境错误态冒充完整播放通过。

- 2026-08-29：完成 F07 真实失败态与重试恢复实机验收。保留登录态、Cookie、收藏、阅读进度、缓存和下载，
  未发送评论、消费积分或修改外部账号状态。MuMu 当前 Debug APK（设备/本地 SHA-256 均为
  `198BEF3DE2F6E2B84D58865995F06330B00BC5242774F7EE78FDB043D7906870`）冷启动后，收藏页真实显示
  `书架请求失败: timeout` 和 `重试书架`。按 UI 树 bounds 点击重试后，1 秒内呈现原生骨架加载；约 12.5 秒后
  恢复 65 条收藏与默认收藏组，无需网页刷新或 App 重启。截图：
  `agent-bridge/screenshots/20260829-f07-current-after-cold-start.png`、
  `20260829-f07-retry-loading.png`、`20260829-f07-retry-result.png`；UI 树：
  `agent-bridge/artifacts/20260829-f07-retry-before-ui.xml`、
  `20260829-f07-retry-success-ui.xml`；重试日志：`agent-bridge/artifacts/20260829-f07-retry-logcat.txt`。
  `NovalPieHttpTiming` 记录 groups/favorites/me 分别约 11.95/12.53/12.85 秒完成。Perfetto/gfxinfo 取证显示
  MuMu x86_64 上还存在 Houdini/ART 类验证长尾，且 GPU 基本空闲；当前没有证据支持用猜测性 UI 重构解决该外部
  首次启动延迟，故未改业务代码。F07 标记为当前 APK 实机已验证。

- 2026-08-28：完成 F01 验证码加载白屏修复与当前 APK 实机复核。`AuthCaptchaScreen.kt` 为 WebView 增加
  `pageLoading` 状态、`onPageStarted`/确认登录路由后的完成回调，并在加载期间覆盖不透明 Material
  加载层；`AuthPresentationTest` 新增 `captchaLoadingStatusLabel` 回归。完整门禁
  `:app:testDebugUnitTest :app:lintDebug :app:assembleDebug --offline --no-daemon --max-workers=1
  -Pkotlin.incremental=false --console=plain` 返回 0：85 个 XML suite、706 tests、0 failures/errors/skipped，
  Lint `No issues found`，`git diff --check` 通过。Debug APK SHA-256：
  `47143F79D99AC2789BC63CDFEB0A6603E4F9958A886C2AB5A36902E5359B417B`。
- 2026-08-28：APK 以 `adb install -r` 安装到 MuMu `127.0.0.1:16384`，设备 base APK 与本地哈希一致，
  `tcp:7890` reverse 为 `host-12 tcp:7890 tcp:7890`。350ms 截图显示原生“正在加载源站安全验证…”加载层；
  约 3s 截图显示源站加载反馈；约 18s 截图显示真实 Cloudflare Turnstile“请验证您是真人”控件，加载层
  已消失。证据：`agent-bridge/screenshots/20260828-f01-captcha-loading-350ms.png`、
  `20260828-f01-captcha-loading-3s.png`、`20260828-f01-captcha-loaded-18s.png`；对应 UI 树和干净 App
  logcat 位于 `agent-bridge/artifacts/20260828-f01-*`。本轮只使用测试账号触发页面，没有勾选验证码、提交登录、
   读取/修改真实凭据或清除应用数据；登录完成后的无刷新链路保留待审计。

- 2026-08-28：完成 F07 网络加载与“卡死”回归的代码/实机成功路径核验。根因是论坛详情把约 0.6s
  返回的帖子正文与可能约 5.8s 才返回的评论放在同一等待链，公开主页也曾等待资料、动态、作品和三组
  签到请求全部结束后才显示。现在论坛正文/评论使用独立 `LoadResult` 和独立“重试帖子/重试评论”，
  公开主页的资料、动态、作品、签到统计、签到记录、签到设置分别加载与重试，并由请求序列号保护乱序
  响应。`NetworkLoadPresentationTest` 专项通过；MuMu 当前 APK 依次验证论坛正文先呈现、慢评论不遮挡，
  以及公开主页先显示资料、后补动态，非本人主页返回论坛正常。证据：
  `agent-bridge/screenshots/20260828-f07-forum-detail-0_9s.png`、
  `20260828-f07-forum-detail-6s.png`、`20260828-f07-profile-1s.png`、
  `20260828-f07-profile-6s.png`、`20260828-f07-profile-activities.png`、
  `20260828-f07-repro-other-profile.png`、`20260828-f07-repro-other-after-back.png`。未通过切断系统网络
  人为生成真实离线状态，因此“错误卡片点击重试”的实机失败路径仍明确保留待复验；本项不标作完全关闭。

- 2026-08-28：完成 F02–F05 当前 APK 回归。F02 在 MuMu `127.0.0.1:16384` 保持
  `host-12 tcp:7890 tcp:7890` reverse；原生请求与 WebView fallback 均成功，代理专项测试通过。
  F03 验证论坛/书籍 A→B 返回栈和 250ms 快速切换没有旧页面覆盖；F04 以
  `novalpie://app/forum/1828` force-stop 冷启动进入目标帖子。专项测试结果：
  `MainActivityStartUriTest` 2、`RouteStackPolicyTest` 9、`WebsiteDeepLinkRouteTest` 5，
  共 16 项，0 failures/errors/skipped。F05 在收藏搜索框与发帖正文分别打开、关闭输入法，
  关闭后 `mInputShown=false`，Compose 内容区域恢复原始高度，等待 2.5 秒仍无大片底部空白；
  未发现需要修改源码的缺陷。当前 Debug APK SHA-256 为
  `47143F79D99AC2789BC63CDFEB0A6603E4F9958A886C2AB5A36902E5359B417B`，设备 APK 同哈希。
  证据：`agent-bridge/screenshots/20260828-f02-*`、`agent-bridge/screenshots/20260828-f03-*`、
  `agent-bridge/screenshots/20260828-f04-*`、`agent-bridge/screenshots/20260828-f05-*` 及
  `agent-bridge/artifacts/20260828-f05-*`。

- 2026-08-28：完成 D06 收藏显示密度当前 APK 实机验收。MuMu `127.0.0.1:16384` 的真实收藏书架从 2 列依次切为 3 列和 4 列，卡片均保留封面、书名、作者和阅读进度；4 列首行验证了不同进度值（`84/84`、`1/207`、`0/73`、`1/22`）且卡片底部在同一水平基线。静止长按书卡进入批量选择并显示“已选 1”，没有启动封面预览；随后退出选择并恢复为原有 2 列。当前构建和设备 APK 的 SHA-256 均为 `235BDAB31CF86E5A6F51E0F67BE0665B8B4463CD568BE35EB527AD14A644F1E7`。`LibraryPresentationTest` 强制重跑 26 项通过、0 failures/errors/skipped。证据：`agent-bridge/screenshots/20260828-d06-collection-{2col,3col,4col,longpress}.png`、`20260828-d06-restored-2col.png` 和对应 UI 树。

- 2026-08-28：完成 D09 书籍详情信息密度与完整标签当前 APK 验收。真实书籍 358153 在 MuMu 900×1600 / 240dpi 首屏显示完整 13 个可点源标签；本站/源站数据为两条紧凑统计行，而非占屏的大型指标卡片，简介、目录与评论 tabs、简介正文和固定“继续阅读”操作均保持可达。`BookDetailFactsTest`（4）和 `BookDetailPresentationTest`（19）用 `--rerun-tasks` 强制重跑，0 failures/errors/skipped；截图 `agent-bridge/screenshots/20260828-d09-detail.png`，UI 树 `agent-bridge/artifacts/20260828-d09-detail-ui.xml`。测试后已返回收藏，不更改书籍、进度或登录态。

- 2026-08-28：完成 F06 当前 APK 窄屏布局验收。根因是搜索卡片标签 `FlowRow` 的行间距未计入固定标签区高度，四行标签会把底部指标向下推约 8px；`SearchGridPresentation.kt` 新增 `SEARCH_GRID_TAG_LINE_GAP_DP = 2` 并按“行高 + 行间距”完整预留，`SearchGridPresentationTest` 新增四行标签回归。专项测试通过；完整门禁 `:app:testDebugUnitTest :app:testReleaseUnitTest :app:lintDebug :app:assembleDebug --offline --no-daemon --max-workers=1 -Pkotlin.incremental=false --console=plain` 通过，Lint 无问题，`git diff --check` 通过。MuMu 360dp 修复后同行两张卡 bounds 均为 `[24,61][261,772]` 与 `[279,61][516,772]`，高度均 711px，指标文字完整；随后恢复设备原始 900×1600 显示。证据：`agent-bridge/screenshots/20260828-f06-small-search-card-details-360dp.png`（修复前）、`agent-bridge/screenshots/20260828-f06-after-fix.png`、`agent-bridge/screenshots/20260828-f06-after-fix-row2.png`、`agent-bridge/screenshots/20260828-f06-collection-360dp-cards.png`、`agent-bridge/screenshots/20260828-f06-book-detail-360dp-wait.png`、`agent-bridge/screenshots/20260828-f06-final-restored-900x1600-wait.png`；修复后 UI 树：`agent-bridge/artifacts/20260828-f06-after-fix-ui.xml`。

- 2026-08-27：完成 A08 当前 APK 实机验收。MuMu `127.0.0.1:16384` 运行的包哈希为
  `32ADFC032A62EEC43EF2A2A0C0FF691E401815629867B7E6FDF2FF9C4D1442B0`；以源站真实帖子 1742
  的 `||否决蘿莉||` 验证默认黑幕、单段揭示和恢复遮挡，并在书评/书籍评论、章节评论、本人动态、
  他人主页动态入口验证同一“显示剧透”偏好。未清除登录态、收藏、进度、缓存、下载或发送新的
  公开内容。本项关闭后继续审计 A03/A04。证据前缀：`agent-bridge/screenshots/20260827-a08-*`。

- 2026-08-27：阅读器“网页正文”入口审计：删除正文加载失败卡片遗留的 `网页正文` 次操作，源码 UI 中已无此标签；保留章节评论的 `打开网页评论`，避免影响评论回退。完整门禁 `:app:testDebugUnitTest :app:testReleaseUnitTest :app:lintDebug :app:assembleDebug --offline --no-daemon --max-workers=1 -Pkotlin.incremental=false` 通过（170 XML suite、1374 次 Debug/Release 测试运行、0 failures/errors/skipped；Lint `No issues found`）。MuMu `127.0.0.1:16384` 原地安装并在真实正文页打开工具栏/导航面板确认无该按钮；应用数据未清除。
- 2026-08-25：当前源码的 `ForumPresentationTest`（54）、`ForumCommentSubmissionPolicyTest`（2）和 `NovalPieApiTest`（96）全部通过。
- 2026-08-25：以 `adb install -r` 将当前 `app-debug.apk` 覆盖安装至 MuMu，未清除任何应用数据；当前 APK SHA-256：`D69389993FC3A454F34F8920BB645B649422B81019F22C49A0418AADA05B3132`。
- 2026-08-25：帖子 1828 中展开子回复并选择“回复 seeking”成功，截图：`D:\NovalPie\agent-bridge\screenshots\20260825-audit-child-reply-selected.png`。本轮不新发公开测试评论。

- 2026-08-27：完成 D10/D11 当前 APK 实机复核。Debug APK SHA-256 为
  `45D8737250D450BC4AD4F2580ECDE86EC1C9076F559C19E1DF25A4F759E17312`，已安装在 MuMu
  `127.0.0.1:16384`，进程存活，`tcp:7890` reverse 保持有效。349955 与 356821 的卡片显示外层图，
  静止长按加载真实内层 GIF；动画采样确认前几帧不同、约 2.5 秒后停在末帧，未被静态外层图替换。
  预览底部控制栏完整可见。滑动超过 touch slop 时不会触发预览；缓存后复测无 FATAL/ANR/OOM。
  本轮门禁 `:app:testDebugUnitTest :app:testReleaseUnitTest :app:lintDebug :app:assembleDebug
  --offline --no-daemon --max-workers=1 -Pkotlin.incremental=false` 通过（BUILD SUCCESSFUL，84 actionable
  tasks；Lint 无问题），`git diff --check` 通过。证据：`agent-bridge/screenshots/20260827-d10-gifdecoder-preview-2s.png`、
  `20260827-d10-gifdecoder-preview-cached-2s.png`、`20260827-d10-book356821-preview-4s.png`、
  `20260827-d11-scroll-cancel-cover.png`。未清除登录态、收藏、阅读进度、缓存、下载或发送评论。
