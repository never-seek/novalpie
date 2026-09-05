# Beta 7 执行记录

## 2026-09-05 续接：核心原型与安卓网站核对

本轮不是成品验收，尚未发布。当前业务界面仍使用 Beta 6 的业务 ViewModel；新增的搜索/导航/分页类是未接线的垂直切片原型，不能据此把 B7-02/03 标为完成。B7-01 的控件/反馈基线仍须补全。

- 新增 `core/AppContainer` / `RequestEnvironment`：依赖单例、沿用原偏好存储、账号/代理代次；Cookie 回退仅由原 API 惰性触发。原根 ViewModel 还未迁移，正式接线前不得认为全站请求已隔离。
- 新增 `core/AppNavigator`：不可变栈原子替换，根不能被弹空，连续章节替换保留书籍返回链；未接入现有 UI。
- 新增 `feature/search/SearchRepository` / `SearchViewModel`：查询与视图偏好分离、请求代次/取消、默认结果、指定页失败重试、搜索返回位置；发现旧逻辑“请求期间切换列表/网格使结果丢弃并一直 Loading”。7 个新增测试通过，尚无接线运行证据。
- 新增 `feature/reader/pagination/PagePlan` / `ReaderPageNavigator`：真实测量行片段、不拆半行，长段跨页，大图整页适配，语义锚点索引，跨章请求门禁，旧布局 generation 丢弃。10 个新增纯核心测试通过，尚未完成 Compose 文本测量/绘制和正式 reader 接入。
- 核心依赖/导航测试 4 项通过；上述定向 Gradle 测试先红（缺失类）再绿。定向 build `75519` 返回 0，不代表全量 Beta 7 gate。
- 审计工具发现并修复两个证据缺陷：同路径 GET 状态不能关联到写入接口；原生 `fetch(urlAlias)`、无 options 的 fetch、`/search` 路径曾漏抓。15 项 Node 测试通过，新 API 候选 203 条，29 路由、2824 静态控件候选，27 条 GET status 关联；均不冒充完整 schema/行为通过。
- 反馈索引全部 752 帖已做标题/正文摘要层复查，并继续完整复核 1871/1828/1124/929/240/1413/325/907/794/952/1730/1619/1797/193/342/454/426/466/482 等重点线程。长文学示例和无关讨论不代表 App 缺陷；未读全的长回复、批量输出裁切仍不能标 full-reviewed。

## 环境和证据身份

- MuMu 0 重新启动：`main launch` 后 `control --vmindex 0 launch` 成功，ADB `127.0.0.1:16384`、`reverse tcp:7890 tcp:7890` 恢复。未卸载或清数据。
- 起始原生页：`agent-bridge/screenshots/20260905-beta7-resume.png`，旧阅读历史、四列设置、继续阅读仍在；这是 Beta 6 对照，不是 Beta 7 通过证据。
- 原登录 Edge 自动化会话已不在，当前 Edge 没有 NovalPie tab；不复建/复制登录态。按项目 `BROWSER_SESSION_POLICY.md` 使用 App 现有认证 WebView 继续源站核对，收藏实际 65 本。
- 新增仅 `src/debug/` 存在的 `SourceAuditActivity`（`android.permission.DUMP` 保护），复用已有 App WebView 的正常认证路径，不向主机导出 Cookie/Token/密码。Beta/release source set 不包含它，不作为任何原生功能替代。通过 Playwright Android WebView 通道连接，普通 browser CDP 因 context 不支持失败后已改用专用通道。
- 开发核对 APK：23,424,485 bytes，SHA-256 `C0BC40CBEF32EB012E988B2618F6BA88F070520BD27D760EE192A0CB10095582`，`assembleDebug` 返回0，`install -r` 成功。版本仍标 Beta 6：这是开发包，不能提供为 Beta 7 成品或发布。
- MuMu 分辨率从900×1600改成540×1098，240dpi不变，得到安卓网页360×732用于窄屏基线；最终验收需恢复/明确记录设备配置。
- 最新安卓网页证据：`agent-bridge/artifacts/beta7-baseline/20260905-mumu-web/`，每份 JSON 仅控件、字段类型、公开响应 schema 和请求状态，无账户/私信/配置秘密值。
- TTS 默认引擎仍为 null，尚未通过实际发声门禁。

## 新增需处理项目

| ID | 证据 | 原生改进/验收 |
|---|---|---|
| GAP-005 | 网页 `Bf41DnD2.js` 每1000标签按offset取全，安卓当前真实连续 `/api/tags?sort&limit&offset` 200；App只取前100标签 | 完整标签、分页/缓存/搜索，不能把100个建议当全量tag |
| GAP-006 | `DTGWrGaF.js:L()` 高级模式基础scope/source/tag/字数/排序回到独立默认，语法参数生效；旧 App 仍叠加基础设置 | 按网页高级语法与基础筛选隔离，不误混书；基础模式内语法行为也须核对 |
| GAP-007 | 安卓搜索设置实际含作者、类型、状态、收藏/推荐、排序字段提示、字数、标签与筛选提示显示开关；App仅缓存开关 | 保留用户要求默认完整信息，提供显示开关并持久化；不强制隐藏书名/作者 |
| FB-929-4336 | 最新公开回复明确“机器时间错误可能造成 session-key Invalid Signature” | 验证服务器时间差与签名错误提示/可安全恢复；不改OS时间或伪造权限 |
| FB-240 | 手机字数滑杆难精确输入，繁体标签不能匹配简体标签 | 字数双端+可输入，标签检索繁简兼容；记录真实查询结果 |
| FB-1124 | 公共正则示例需要跳过插图引号；目前 reader 在原HTML上替换，export只保护一种图片标记 | 共用文本节点派生管线，保护HTML属性、链接地址、图片/占位；不支持的lookahead需清楚说明 |
| FB-1413 | 用户利用替换加入换行 | 支持纯文本换行及安全br语义，不以任意HTML注入实现 |

下一步：补齐原站控件与原生功能映射，逐项完成模块接线、服务化和回归；不提前发布。

## 同日继续：真实字体测量、文本节点、下载持久化、TTS实机根因

- 增加 `ChapterDocument`/`measureChapterDocument`/`PagedChapterCanvas`。使用完整 `TextLayoutResult` 分行片段绘制，不重排substring；保留span/缩进/行边界。`ChapterMeasurementTest` 的Robolectric默认legacy graphics没有真实字体宽度，切到Native Graphics后2项真实字体测量用例通过（不放宽断言）。还未替换正式ReaderScreen。
- `DerivedTextPipeline` 9项测试通过：只替换文本，HTML属性/链接/图片/占位保留，实体解码、br换行兼容、禁止替换注入任意HTML；尚未接入reader/TTS/export。
- `DownloadTaskStore` 6项通过：AtomicFile、账号隔离、规则快照/授权保留、中断转NeedsRetry、授权不明禁止自动重复扣费、坏记录保留报告、并发有界。还未接到独立协调器/前台服务。
- 新feature/core定向结果：7 suites，38 tests，0 failures/errors。该结果是新核心单测，不是全量应用验收。
- 旧androidTest被发现长期失效（仍引用已删轮盘、缺替换/锚点参数），已修接线；改为正文长按不出现复制/全选/轮盘的验收。MuMu章评/手势/禁复制/繁体4项通过；宽度设置一项因测试未包生产滚动容器在360dp无法显示，已按生产ReaderSettingsSheet的滚动容器修测试，待再跑。
- TTS **实机确认根因**：eSpeak已安装/系统可见，但App缺少Android11+ `queries -> android.intent.action.TTS_SERVICE`，设备日志AppsFilter明确BLOCKED，初始化返回-1。先在MuMu跑失败，补Manifest后同一测试通过。
- eSpeak来自F-Droid `com.reecedunn.espeak_22.apk` v1.52.0，10,446,659 bytes，SHA256 `0A7822FEC54D7F7AE759FFEDC9AFEAA77F809587B171FB2101998787821EC790`；apksigner验证通过，F-Droid证书SHA256 `acdee0a53d94886b9824ad748ceae1d7c8d3e15e1de694dc684dfaae62231ea7`。仅安装于MuMu，不内置App/不修改主机语音。
- 当前开发APK SHA256 `2E3D244F48276D10051DDE9DEFA038ED47C7C50C15E637D10258711A846EAA6F`。`install -r`无损覆盖；TTS device instrumentation **2项通过**（session89791）：实际中文系统音色生成323,394-byte非静音RIFF WAV并开始/完成真实播放；App现有ReaderTtsController连续朗读两段、逐段回调0/1、结束Stopped。不是只测试无引擎错误。后台/焦点/锁屏/跨章等完整前台服务门禁仍未完成。
- 网站取证新增：安卓原站搜索规则/字数/设置/完整标签分页；本人作品 `GET /api/v2/users/100164/novels`、屏蔽200、装扮/仓库200；编辑器六工具分区；工作区各tab/状态200。仍无真实业务写入。
- MuMu WebView110的viewport问题不仅reader，还影响整屏editor：根/父容器高度0导致按钮命中HTML。只在临时页面加明确height用于控件调查，证据名称包含`forced-root`/`local-compat`；不能当未修饰网站可用证据。导航会移除这些诊断style，没有改站点服务器。
- Node证据匹配再加固定query action隔离，16测试通过；203静态API候选、25个旧浏览器GET status匹配，外加新的MuMu网页记录（尚未合并总索引）。读成功不代表写成功。

## 已接现有入口的缺陷修复（不冒充模块重构完成）

- 阅读显示及原生EPUB/TXT的现有替换入口现共用`DerivedTextPipeline`。先以实际旧入口测试确认HTML属性/图片URL遭替换失败，再接线；相关替换49项定向通过，搜索freshness新增断言按预期红灯。后续已修freshness，等待全量gate。
- `RequestFreshness`比较resolved query而不是包含view/cache的整个SearchOptions，保留serial/keyword/page校验；搜索中切换网格或缓存设置不会丢掉正确回包。
- 个人规则本机停用/章节范围/标题设置不再触发服务器DELETE；有效规则汇总按服务器ID排除自身共享副本，避免停用后又以公共行生效；全部规则列表开关遵守自己的`isEnabled`。新回归先红（缺row策略/旧删除期望）再修，等待全量gate。
- TTS新独立协调器6项通过：真实下一章首句、停止后迟到回调无效、暂停从当前句重入、耳机/音频中断暂停不抢播、改规则重队列、下一章失败可重试且不读占位。未与系统前台服务接线，不能据此说已支持后台听书。
- 所有新核心合计44项通过；固定开发APK的MuMu交互最终5项通过（exec50052）。宽度控件属于Layout，旧测试仍选Typography，已改到正确类别并使用生产相同可滚动容器。
- 新增`run-device-gate.ps1`核验已装APK与本地hash、设备SDK/尺寸/字体、test APK hash，解析JUnit结果（ADB exit0不算通过）。已保存`beta7-device/20260905-tts-base.*`和`20260905-reader-interactions.*`，均passed=true；现有开发包2e3d...，不含后来接线修复。

## 19时后续检查点

- 共用替换/规则本机控制/search freshness修改的完整Debug gate已成功：100 suites/888 tests/0失败，lint0错误27依赖提醒，assemble成功（exec18583）。开发APK SHA256 `B2B6D664E4A283B16F85D1EE3AC7E6A99AEC4B276DA5279F7B96559503BB06C9`，install-r后TTS2项/阅读交互5项再验均通过，记录`beta7-device/20260905-tts-after-rules.*`和`20260905-reader-after-rules.*`。
- 读取到App帖最新4620（18:50:29）实体文字露码：`&lt;例子1&gt;`。`ReaderTextTest.plainReaderTextDecodesEntitiesWithoutEatingAngleBracketContent`实测红灯；纯文本快速路径改为一次实体解码，不把解码后的尖括号再当HTML解析。新测试/构建执行中，未进入上述b2b6包。
- 增加`ReaderPlaybackService`原型、`AndroidSpeechEngine`及`WebsiteSpeechChapterSource`：应用级coordinator、系统MediaSession/通知控制、mediaPlayback前台服务、音频焦点/耳机断开处理。音频焦点拒绝不会先发声，只准备Paused。原生ReaderScreen仍用旧controller，尚未切换到服务。
- 新增服务设备测试`ReaderPlaybackServiceDeviceTest`：合成测试章节，退桌面/熄屏/通知栏暂停继续停止，待构建后实际运行。没有用真实书籍正文作为测试音频，不需要扣积分。
- 新增静态`native-map.cjs`/测试，17项Node工具测试通过；生成api-source-map.md，203网页候选与122原生直接调用引用中的101项路径方法对应。余项是待人工解析（含间接调用），不直接判缺功能，不算协议/运行通过。
- 注意构建期间曾改动待编译的prototype，造成一次`preparePaused`旧classes与新test不一致。已重新串行全量compile，不把该构建状态当产品运行缺陷；后续避免同一源码集编译时编辑。

## 听书服务接线继续

- `ReaderPlaybackServiceDeviceTest`实际通过，开发APK`c1fe369f...c2eea8`；退桌面和熄屏保持Speaking，通知Pause/Resume/Stop及移除通过。详见tts-device-evidence。
- `ReaderScreen`已改接AppContainer.playback，去掉页面dispose时shutdown；从可见章节开始听，不重新朗读所有累积章节，服务负责加载下一章；前台视图跟随服务chapter/segment。保留旧界面/按钮/音色选项，关闭TTS停止服务。
- 已新增直接在完整ReaderScreen点击“听书”、离开到桌面仍朗读的设备回归测试。当前全量unit/lint/build/AndroidTest构建在exec91178运行，等待结果，尚未跑接线后的实机按钮用例。此阶段尚未做最终R8或Beta7发布。
- exec91178全量通过：100 suites/890 tests，lint0错误27提醒，Debug/AndroidTest均打包。已装开发APK`bcfc03c7ff40d1c771d5d016f3038d03f3cf3b68606fdc6ea0ef1b31c082dd4a`。
- 接线后设备UI测试发现2项失败：中央段落tap后未找到听书label，展开章评后写评论不在当前视口。正在增加截图/语义树定位真实触控路径，不用服务独立测试通过代替按钮可用。失败日志`beta7-device/20260905-reader-service-ui.log`。服务后台测试已经通过，但页面接线未验收。
