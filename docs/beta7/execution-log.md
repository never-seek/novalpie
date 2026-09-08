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
- 失败定位补充：测试的普通ComponentActivity没有主App的手机竖屏锁定，实际1098×540横屏，“短段落中心”在左翻页区域；不是屏幕中央。测试现与MainActivity一样设置手机竖屏，真实点屏幕中央，章评滚动到达。`20260905-reader-ui-portrait.log`最终6项通过，包括点击听书→应用服务Speaking→退桌面仍Speaking；对应bcfc...开发APK。这些证据已包含运行朝向记录，旧wm size不能独自证明竖屏。
- Root账号/代理变更现通知AppContainer刷新环境，停止旧会话播放防串号；服务销毁释放语音引擎，Error通知“继续”走retry。上述后续改动重新跑unit/build/device，不用bcfc证据替代。
- exec94443包含上述会话保护的unit/Debug/AndroidTest gate通过。额外把TTS进度跟随改为读取ReaderBodyLayout的预建textLocations，不再每句话主线程重新解析全部章节HTML；该改动尚待重新build。
- 最新分页原型`NativePagedReader`已写：真实分页画布、章节尾评论页、分页注册事件、锚点重排、图片尺寸更新与原图长按。仍未替换ReaderScreen的旧分页分支。`ReaderAnchorStore`独立schema保存book/chapter/block/offset；新的13项分页/真实字形/锚点测试通过；exec26106的assemble仍在跑。
- 新增`NativePaginationDeviceTest`，合成长段落、连续前后翻、章末请求一次；待AndroidTest构建和实机。不要把PagePlan单测代替最终EP49/EP50实机覆盖。
- `NativePaginationDeviceTest`已在MuMu通过（2.655秒，开发包7cd6193e...59556d0，日志`beta7-device/20260905-native-page-prototype.*`）：实际长段落多页前进、返回同页、末页仅跨章一次。
- NativePagedReader已接ReaderScreen的pageTurnMode分支；普通连续滚动暂保持旧LazyColumn。页眉/页脚为分页预留真实预算，音量/点击都调用同一page table，末尾单独章评页有返回末页/阅读菜单/下一章。隐藏插图/去重设置补齐，TTS followText定位实测页；仍在build，未拿固定EP49/EP50验收，不宣称最终通过。
- 当前running build为exec27871（unit/assembleDebug/assembleDebugAndroidTest），上一exec92274因新增设置回归缺参数按预期红灯后已修。新增showImages/removeDuplicateLines策略先红后修，仍待结果。
- exec27871完成：101 suites/892 tests/0失败，Debug/AndroidTest打包成功。新包SHA256`82EF9F779584FAAB28831C0A0F94BE85C9189E4AEC8E60CD01AAA09BC2B0B49E`，无损install-r。
- 正式ReaderScreen新分页接线实机测试通过（`beta7-device/20260905-reader-paged-integrated.*`）：7项，含真实左右屏幕点击→下一页/上一页→同锚点、章评展开/滚动、长按不复制不轮盘、繁体、布局宽度可达、听书按钮启动应用服务并退桌面继续。
- 新分页仍需完成：固定真实EP49/EP50含图前后循环、迟到图片/多设置组合/字体迁移、全部动画（当前simulated为缩放+横移还需纸张效果提升）、按页TTS关闭highlight仍跟随、分页背景图、精确锚点与滚动切换。不能以当前7项测试宣告Beta7完成。
- 搜索状态已从全局VM迁入feature/search/SearchViewModel：根只暴露兼容getter/action，query/page/history/tags/scroll/request generation属于feature，切账号/代理invalidate，返回保留搜索。旧重复performSearch/goToPage逻辑已移除，根onCleared关闭子scope。exec25790搜索相关测试+assemble通过。下一步补全量tag/字数输入/高级语法隔离和运行证据。
- 搜索全量tag offset分页/繁简检索与双端字数+直接输入已实现，先红SearchCatalogTest后修，构建exec9968运行；尚未安装验证。标签默认先显示24个，输入过滤全量再逐页展开，避免一次Compose全部。
- SearchCatalog的tag分页/字数输入/繁简测试已通过；高级模式继承基础来源/标签导致混书的新测试红灯，已按当前网页源函数L()修为独立默认+语法参数，等待全量。exec60284漏import ChineseVariant编译失败已补；当前running全量unit+assemble为exec54879。未新发布。
- 360dp原生截图`20260905-beta7-progress-current.png`暴露收藏列数说明挤成单行，列数文案与说明相贴；属于布局微调待修（不影响书名/作者保留要求）。当前处于历史tab，收藏数0不代表真实65本被删除，需切收藏进行最新数量验收。
- 当前源码已把下载执行从root VM迁到DownloadCoordinator/NativeDownloadTaskRunner/NativeDownloadService：AppScope、dataSync FGS通知、AtomicFile任务、冻结规则序列化、source.txt及assets检查点、失败拒绝发布缺图文件。原root约700行下载实现与危险全局孤儿清理已移除，UI按钮兼容委派并恢复未完成任务；正等待全量编译/实机测试，不能称已验收。
- 下载原型Node/核心单测曾通过，但后续runner/service新实现尚未完整build；当前running exec32689（unit/Debug/AndroidTest）。源码表明早前native tags分页offset使用过滤后条目数不稳，新增空标签TDD已修；实机曾出现分页不继续，仍需再验证当前包。
- 仅下载/听书/阅读等任务拥有的临时资源可以清理；没有执行新的D盘大清理，也没有动GitHub资产。
- 公共GET核对标签目录18351条，offset0/1000/2000与尾18000均正常，原生实机错误不是源站整体分页坏。当前修复按原始数组长度前移offset（不能按过滤后数量），去重不等于没有下一页；仍需最新包复验。
- 下载新增并发同URL锁与SHA256检查点校验，复用字节不得只比文件大小；中途坏图会重取。账号/代理切换会取消旧下载防混会话；服务pending启动去重。还没实测真实EPUB/TXT与扣分/恢复，发布门禁仍未通过。
- 标签实机已成功，全量列表显示12834；再次完整公开GET对照18351原始行/12834独立ID/5517重复，原生按原始offset遍历去重完全对应；来源不是缺失5517标签。
- 原生下载首次真实小书（353686透明龙，9章，allowDownload=true）测试失败：TXT流已落私有文件，但MediaStore未发布行的SIZE数据库列滞后返回0，被新完整性检查误判。已改用实际文件描述符statSize/读字节校验，不放宽完整性；重测将复用持久授权和source检查点，不重新扣这一步积分。
- 当前最新APK已装`15c384c6...691573b`，不是Beta7成品。当前exec41452正在重新build下载publisher修订与live test。真实下载测试只删本次新生成URI，现有用户下载未动；失败私有检查点保留。
- `NativeDownloadTaskRunnerTest`通过：模拟最终磁盘保存失败后重跑，只调用1次授权POST、1次正文GET，输出字节完整。当前live TXT第一次失败没有生成公开成功文件；私有缓存可恢复，下一次live测试明确选同book/format/mode的既有授权任务。
- 小书353686真实原文/替换×TXT/EPUB四组合全部通过，43.605秒。开发APK187e04d9...24a2f6b1，EPUB两包均9个chapter文件、1封面、无重复zip entry。规则仅任务快照，没有发布公共规则。4个本次公开下载URI已删，私有验收副本保留，见download-device-evidence.md；大书/插图/OEM/任务恢复仍未全验收。
- 最新下载修订build通过；现在exec11924运行最新全量unit+lint，完成后保存模块可回滚提交。尚无Beta7版本号/R8/Release，不能上传当前开发包。
- 最新187e04d9包原生收藏切换验证65条，与网页65一致，记录`beta7-device/collection-count.xml`；当前历史tab的0收藏仅未加载分区计数，仍需修为未加载不显示误导0，冷启动全矩阵待做。窄屏列数说明已分行，截图20260906-home-retry.png。
- exec11924最终完整unit/lint通过：104 suites/899 tests/0 failures/errors，lint0错误27依赖提示。已保存app/src模块重构为本地中间提交（不push、不tag、不发布），用于后续继续拆分和修复；当前只是开发基线，不是Beta7完成。
- 中间源码提交为9297dd4；后续正在补新版原站的用户/作品屏蔽功能：v2列表/状态/设置接口、ProfileTab.BlockedUsers、他人主页屏蔽按钮、书籍菜单屏蔽按钮。blockedUsers分页/DELETE协议先红后绿（exec99013），新增界面仍待build/设备。没有在真实网站屏蔽任何人或书。
- 屏蔽列表原生已可打开，显示已屏蔽0位/暂无用户，与源站一致，截图20260906-native-blocked-list.png；未操作他人账号资产。全局返回栈改用AppNavigator原子不可变列表，AppContainer统一API实例；readerSessionKey绑定environment revision，换账号/代理不能复用旧密钥。新增protocol test先红后绿，exec90084全量unit+assemble通过。
- 外部Readest虽然提示WebView过旧，但取消提示后可走正常本地文件选择器导入本任务215265字节替换EPUB，书架显示《透明龙》；正在点开正文/目录验证，不先认定失败或成功。
- 统一容器后APK350173e7...2ade32dd已覆盖安装。后台测试失败仅在Paused后300ms通知仍旧label，改成bounded wait观察实际通知状态（保持必须变为继续/停止后消失断言）。之后MuMu退出，ADBoffline，info is_android_started=false；当前已请求launch，等待恢复。Readest仅证导入封面/书架，还未证正文打开。
- 当前build为exec63952（AndroidTest）。本轮新增block API/UI、AppNavigator接线、requestRevision隔离、统一AppContainer尚未commit。最近完整unit+assemble exec90084已通过，门禁不得混用不同hash。

## 2026-09-06 继续至成品：恢复协议与分页设置回归

- 上轮exec39294 lint通过，105suites/901tests是此前完整unit基线；后台TTS通知bounded-wait复验`20260906-container-tts-retest`通过，不是通知行为放宽。
- 下载新增两条实测红灯（exec87437）：AtomicFile仅剩`.json.bak`漏恢复；授权期间暂停再取消丢失Uncertain状态。修为逻辑base恢复、持久写串行/读取最新状态、写入未知不重扣，Android15 `onTimeout(startId,fgsType)`保留NeedsRetry检查点。
- exec30398下载11项通过/Debug打包，AndroidTest因多余assertExists import失败（测试接口成员不是扩展），移除import后exec2129通过。开发包25cab2...76c818安装成功。
- MuMu对25cab2包新增分页繁体/富文本词距用例实测失败：未找到“龍書”；旧连续前后翻测试仍通过，证据`20260906-pagination-settings-before.*`。修复真实分页转换/词距映射、原样span、背景图、高亮独立跟页，新增替换拆段/插段锚点映射。接入ReaderPageNavigator统一普通页/边界移动。
- exec79908全量106suites/908tests/0失败，Debug/AndroidTest成功；APK`BBD994E5BF6047454889DEEAD00965BAF97BEB0B449F49E3A3D5C54ECAB181E1`无损install-r，MuMu分页3项通过，`20260906-pagination-settings-after.*`：前后同页/边界一次、繁体+粗体词距、无高亮听书跟页+字号重排。不是固定EP49/50最终门禁。
- 后续2条新红灯（exec39013）：同长度坏source.txt被复用；只改高亮/自动滚动设置竟暂停TTS。修为正文SHA256检查点、任务内URL锁释放范围、仅声音参数变化暂停，exec26039相关14项通过。尚未装入bbd994开发包。
- 屏蔽功能迁入独立BlockingRepository/BlockingViewModel/BlockTargetViewModel；界面只读不可变state、账号/环境变更失效，写入结果不明先刷新。个人屏蔽列表用户名路由已接onOpenUser。当前全量unit/assemble/AndroidTest/lint执行于exec10307，等待完成。
- 独立Librera已验证本轮小书替换EPUB实际正文及目录跳章；官方F-Droid证书与文件hash记录于download-device-evidence。设备验收副本hash一致，已精确删除本次公开测试EPUB；用户旧下载未动。Readest旧WebView问题不当成功证据。
- 新community-review.md逐帖保存115个反馈帖完整正文/嵌套回复分类（剩余仍未完成），构建间持续核对，不以关键词索引代替人工意见审查。
- 未发布/未推送。还需固定真书EP49/50、分页锚点/动画/布局/音量综合、TTS后台进度、大下载/任务记录/恢复、其余feature拆分、全站控件/反馈/角色矩阵及优化包升级验收。当前所有开发APK仍Beta6版本字段，不作为Beta7成品。
- exec10307最终全量107suites/912tests/0失败，unit/Debug/AndroidTest/lint成功（11m34s）。当前已装开发APK`6F685654539C56205EE936D0191CE5CFF86C8F70133E5D82A87755EC8DE7D8AD`；ReaderChapterCommentsInteractionTest7项通过、TTS后台锁屏通知服务1项通过，证据`20260906-reader-all-followfix.*`和`20260906-tts-after-display.*`。下一切片为用户可见下载任务记录/完成文件打开分享/进程恢复继续。
- 后续社区基线456/456全部逐帖阅读分类完成，community-review.md机器比对无遗漏/额外/重复；不是仅关键词分类。网页App帖最新读取root14条，新增4621（2026-09-05 19:02:45）附图内容为实体露码，与此前4620同类，源图341×50已查看并保存comment-4621.png；没有自动回帖或修改投票。
- 下载历史已接独立DownloadHistoryViewModel/Panel，“我的→下载”真实显示旧任务（APKbd1ca2...3b7a49），文件已删时打开明确提示不崩溃。FileProvider仅暴露应用外部Download目录，内容URI正常只读分享；账号不符不能恢复。
- 两下载新回归exec24707红灯：封面失败仍生成成功包；末次进度被节流丢total。修复后exec71492定向27项通过。不是大书完整验收。
- 新分页横滑实机红灯20260906-page-swipe-before；加入横向拖动阈值共用navigator后通过对应项。新增图失败空白页红灯20260906-swipe-image-before，改为加载/失败/重试插图；20260906-swipe-image-after该项通过，但繁体case首次锚点等待超时（同机器重编译+Lint负载，未放宽断言），正隔离复测session7078。
- 账号读回包过期/失败写入自动cookie重发两测试exec64821红灯；网络层绑定environment revision，GET/HEAD才可fallback、写入禁OkHttp自动重试，签名请求和章节取数间隔检查同一revision。exec51766全量108suites/919tests/0失败及unit/Debug/AndroidTest/lint通过（16m52s）。APK9e5320...1a77f4已安装，不是Beta7发布身份。
- 正在新增forum feed独立ViewModel、分区page/query/scroll缓存与环境清理；尚未接root。新“成功GET确认网页会话后，后续写入直接用已确认会话且不自动重发”测试尚待实现，避免禁重试导致续期后写入长期不可用。
- forum feed4项测试已过，接root移除约150行旧逻辑，AppContainer提供Repository，分区各自页码/搜索/滚动，账号变更清缓存。合法GET确认Cookie后以opaque revision标记后续直接使用，不让写入自动重发；新回归先红后修，exec90127unit/Debug/AndroidTest通过（11m15s）。
- 分页4项复测出现No compose hierarchy/启动ANR，最新exit-info为process failed to complete startup、尚未进Activity；主机可用物理内存约1GB。测试换debug-only固定竖屏ReaderTestActivity并保持测试屏幕常亮，未改正式UI。MuMu实例0已正常shutdown，未卸载或清数据；可用内存回到3.5GB，构建完成后再launch独占QA。不要把这些失败当通过或未经验证归为产品分页缺陷。
- 流下载新测试exec21905确证两失败：原图外站请求携带本站token（仅fixture验证，没有泄露真实登录值）；取消卡住的stream要等completion无法及时关socket。已修同源会话限制/重定向剥除站点会话，onCancelling关闭socket并保留CancellationException。当前全量gate正在下一工具session执行。
- 论坛反应/书评提交写入补ContentMutationIdentity路由/代次/环境检查，旧操作返回不再覆盖另一本书/另一帖当前状态；尚待本轮完整build/实机。
- TTS后台进度切片新增SpeechDocument（段落/UTF16偏移、可见图开关对应item索引）与SpeechProgressRecorder：只有实际utterance onStart写本地进度/锚点，按章节同步网站；根观察进度store变化更新收藏和继续阅读。来源与UI共用buildSpeechChapter，移除从全局LazyColumn索引直接比较单章索引的起读偏差。新用例先编译红灯后接实现，当前完整build34016执行中，尚无实机后台进度验收。
- 实机最新startup ANR细节：exit-info `failed to complete startup`，MainActivity am start -W也超时115738ms，MuMu下uiautomator自身还存在SIGSEGV记录。主机内存压力存在但不能单凭此断言是模拟器问题；实例0已安全shutdown等待构建后重启、不清数据。保持发布blocked-by-verification，其他代码工作仍继续。
- build34016全量113suites/930tests/0失败，Debug/AndroidTest成功（9m59s），stream取消/外站无会话header与TTS真实位置存储等新用例均绿。开发APK`265F63D23F7DAB9897F02BF37112F0CE12613CD4BF7FB512EAD4C5DC70CE61B4`；MuMu正在launch，接下来无Gradle并发下安装/回归。保存本轮本地中间源码提交，不push不发布；全部门禁仍未关闭。
- 本地中间提交c18ad1d完成（55files），未push。MuMu重启后最新265f包无损install-r，MainActivity冷启动成功5001ms；分页4项全部通过（20260906-final-paging-restarted.*），包括横滑前后、字体/繁体/词距/无高亮跟页、插图失败可重试。旧startup超时证据保留，不能断言所有设备已无ANR，继续整体运行验收。
- 265f包TTS后台通知/锁屏1项、Reader交互7项均通过；新增后台进度接线暴露合成UI测试也会写假book7的进度，已添加显式测试播放不记录进度开关，并在设备测试结束只清“测试书籍/测试章节”匹配的旧合成记录，不动真实书记录。73722相关TTS10项及Debug/AndroidTest build通过；再安装运行带清理的测试。该测试隔离修改不影响正常书籍进度。
- 测试隔离提交5f0542f（未push），APK`AB1B96FD1647093B7232801F62B1A4D1C80E90B09721C8A02442FE32397C47C8`。首次Reader TTS启动15s超时，其余6项通过；同包中文系统引擎真实PCM/两段播放2项通过（tts-engine-recheck），随后完整Reader7项通过（reader-after-engine-ready），未放宽断言。冷引擎/设备启动稳定性仍需最终矩阵，不把一次热成功抹掉冷超时。
- ab1b96包真实论坛书评流显示总25037、最新正文与书名；滚动到Gzs《难得变成TS那就改过自新吧》，点击进入原生详情（简介/目录76/评论tabs可见），系统返回后首个可见卡及像素位置均恢复[99,85][498,157]。切收藏再论坛同样位置保持，截图20260906-forum-return.png，设备XML `beta7-forum-return-{start,after}.xml`。没有发帖/投票/反应写入。
- 准备大包真实测试NativeLargeDownloadLiveDeviceTest：必须显式bookId/minimumBytes，独立task前缀仅恢复测试自己的失败任务；真实服务授权/后台打包，源图出现数对照ZIP引用数、原始缓存SHA256对照每个ZIP资源，64KB流式核验，输出JSON无正文/凭据。测试最终只删除自身公开URI；测试不等于通过，尚未执行。计划使用已在社区1828明确提及的350192大图书，目标至少1GB，不拿合成文件冒充真实书。
- 大包实测已启动：exec87377，book350192/minimumBytes1073741824，APKab1b96...397c47c8，androidTest hash a956ff11...b93396b。源允许下载、服务foreground=true，任务工作目录已增长493MB；仍执行中，禁止重启MuMu/安装App/并发Gradle或改掉运行数据。仅可代码/文档编辑并记录后续未build。当前新增收藏未加载计数显示待加载/—，不再误报0/已同步，尚未build。
- 大包后台至10:23工作目录12GB（包含源资源检查点/打包文件，非最终EPUB大小），PSS多次约51–54MB，HOME后FGS仍运行，等待最终图引用/原始SHA核对。期间离线代码新增ReaderServerClock：仅本站HTTPS Date校正内存签名时间、不改OS时钟，防设备时差InvalidSignature；新单测待大包结束后build，未进入正在测的ab1b96包。
- 离线待build另补替换编辑器内容区可滚动、替换后多行输入；不支持的安全正则lookahead/backreference在规则行明确提示“未应用/可复制改写”，避免共享行开着却悄悄忽略。仍需真实公共规则写入/默认策略/逐条开关/导出验证，不能以错误提示替代兼容实现。
- 真实350192大包exec87377最终FAILED（1828.76秒/约30m29s）：1365/1365章、19573/19573图、failedAssets=0，私有EPUB22,965,254,625 bytes已打包，但发布MediaStore时`write failed: EPROTO`。源TXT15,917,976 bytes，19574marker含1空、10403原始URL（含空）、同章重复URL9171；因此包大于唯一资源缓存，不能先自行删除合法引用。未生成公开成功文件，任务`beta7-large-350192-ee4db75a-71fa-4958-bd0a-8c3ca391e142`的source/assets/result与合法票据均留在MuMu。待修发布小chunk和已完成包检查点复用，再续同一task而非重授权。
- 为安全build MuMu已shutdown实例0；D盘剩37.8GB，原私有task约32GB保留待恢复，不做广泛清理。最新开始发布buffer回归工具session见exec；禁止删除该失败任务资源。
- 旧WebView保存用128KB块，新publisher曾用1MB块。待验证修订改为64KB发布块；新增DownloadPackageCheckpoint按源/规则/文件SHA复用已完成包，旧失败大包允许一次完整ZIP CRC+目录/图数量检查后采纳检查点。这样后续保存失败只重试保存，不重授权/重下载/重打包。新用例/全量build正在执行，EPROTO根因与最终恢复尚未实机通过。
- 32710全量115suites/938tests/0失败、Debug/AndroidTest成功，保存本地22b6699（未push）。新包`6A6069B663DF2D96D839ADFD7916CC7B42B87C77DDB5B2D94DFE142B821792AD`，测试APK`0A660510BB1F1973DA60853B502702CE87A6D9F19CCFA842C46581E9E531E269`；已无损安装开始exec69606续同一350192任务。先校验原23GB包CRC和SHA再保存，不重新扣积分/下载资源。当前原任务result.epub不要删除；运行期间禁止安装或重启MuMu。
- exec69606恢复失败（278.768秒），同包CRC已通过/建立package.complete，64KB写到公共pending约7.7GB后仍EPROTO；小chunk不是完整解决。新增明确本机保底：公共MediaStore IOException时把已验证成品移动至files/native-downloads，经仅该目录FileProvider可打开分享，界面明确系统目录失败/本机保存/卸载会移除，不假报公共目录成功。当前74124全量build中、MuMu正常shutdown，任务原包与缓存保留供下一次恢复；不重新下载/授权。
- 本机保底单测因AndroidX FileProvider在Windows Robolectric路径'/'比较失败，不是Android路径行为；改注入仅URI工厂的单测，保持文件移动/范围/显示原断言，并新增真实Device FileProvider打开/删除测试。53203单测+Debug/AndroidTest通过（2m47s），APKAFA983EC...28C4D161，测试0C595FB8...A0483288，安装实机先验证真实FileProvider后再恢复大包。所有未push。
- 本机FileProvider device test通过。exec27498第三次大包恢复最终OK(1test)，331.238秒；相同任务免重授权，23GB完成包移入本机区并读回完整验证，1365章/19573源占位/19573图引用/19573正文图片+1封面，各原图SHA一致。机器报告在beta7-device/20260906-large-350192-report.json。测试成品已删，成功任务工作目录已清；不是整个Beta7验收通过，当前只大包完整性/本机兼容路径通过。
- 本轮全量939tests/0failures、lint通过（10817，6m50s），本机fallback保存为本地提交63af1f7（未push）。公共存储缺陷以明确兼容路径解决，不能直接宣传所有系统公共Downloads成功。后续开始CommunityQuoteRuleTest原站1124常用引号负向前瞻的窄范围安全兼容，当前测试14241运行；不引入任意回溯正则执行。
- CommunityQuoteRuleTest首次2红灯；新增仅识别论坛1124四种引号固定模式的线性扫描+RE2替换适配。与Java原模式在合成小文本做精确对照时发现跳过匹配后的引号边界差异，新增case继续红灯后修；当前正在重新跑47项替换相关测试。仍未安装这一适配，不宣称任意lookahead支持。
- 9330替换相关47项测试已全绿，CommunityQuoteRule4项包含四引号类型/禁止跨行/跳过与相邻边界/危险规则拒绝，Debug assemble仍执行。适配只覆盖已核对的1124固定模式，其他前后瞻仍错误提示；任意脚本/回溯正则未启用。
- 9330最终Debug打包成功（5m8s），本地提交f87a7be（未push）。最新开发APK76067A86ABC40CE8FA4988FEE0D39DF3867E6A0730E60EA1E25B7EF484A382EE已无损安装，MainActivity冷启动3360ms成功。仍是Beta6开发版本字段，不能当Beta7成品上传。截图20260906-beta7-dev-current.png；尚未实机输入1124规则或公共写入验收。
- 真实353686/6072568小书阅读器打开正常、中央点菜单可用，进入设置低部文本替换时复现分类滚动位置继承导致顶部条目被卷走。修为selectedCategory变化滚到顶部，当前build51217全量unit/Debug/AndroidTest运行。MuMu实例0为了构建资源已正常shutdown，未清用户数据；原规则未做任何真实修改。下午页面状态已变更，后续一律刷新UI证据再操作，不复用上午坐标。
- 51217因新LibraryContentViewModelTest未实现类型先红；随后新增feature/library的Query/Repository/ViewModel（尚未接root），首个编译类型默认lambda缺空格导致解析失败已修。当前定向79556运行；4个新用例覆盖冷启动page1/源总数、分组迟到回包、失败追加保留页和重试、列数切换不干扰请求。不要把该未接线切片算收藏重构完成。
- 79556定向4项通过，收藏Query/Repository/ViewModel已接AppContainer/root，旧loadHome/loadMore约200行迁出；根仍暂留管理动作/展示偏好委派，后续还需完整迁移。当前88227全量unit/Debug/AndroidTest运行，未安装本切片；设置分类重置滚动修订同批。MuMu仍shutdown不清数据。
- 88227全量unit/Debug/AndroidTest通过（7m14s），新增多页返回刷新不丢前页用例后29413定向5项通过。补齐当前页回传与刷新加载1..已加载页范围，收藏mutation执行归feature、generation防旧回包。下一全量build执行中，仍未安装当前library切片；此前76067仍是设备最新包。
- 54202全量948tests/0failures，Debug/AndroidTest完成（5m58s），收藏接线与设置类别滚动修订准备MuMu安装。包含新领域逻辑不代表收藏所有管理/原文进度/升级状态已证，待实机。
- CB2AE692A4242CC03A6ACA62D08AB4752A2A8670095786FC1B7F3586F9433B64包已无损安装，冷启动2903ms成功；真实65收藏、历史加载、全部卡片、详情返回同位置正常，截图20260906-library-split.png。UI另发现overview把“—”过滤成空再当0，已修不再错误显示0；history总数新增单独字段，避免第一页20当总数。当前下一全量unit/assemble/AndroidTest/lint运行，MuMu已shutdown让资源，需新包复验。
- 81885上述计数修订全量unit/Debug/AndroidTest/lint通过（9m16s）。后续核对用户“书名作者不能略”发现2/3/4列仍固定2行/1行Ellipsis，新增CompactLibraryRowMeasurement按每排真实宽度测量短身份文字，保留全名并按排最大行数对齐；原主题/卡片结构不改。当前新测量用例/Library定向+Debug/AndroidTest正在build，尚未新安装。
- 97213短卡片真实测量/Library6项定向与Debug/AndroidTest通过（5m15s），新APK752417DE410ABB6C5F6A4FF82F3CD0E6A61AE3E0E42D0B44B830184E4A09AFD2准备覆盖安装；此前全量948绿色不含这1个新增字体测量测试，需后续最终全量。
- 752417包安装冷启动2914ms成功，MuMu四列真实长书名/韩文长作者完整显示，同行作者/进度同基线；截图20260906-library-fullnames.png已查看，短文名不会把同行进度抬高。未加载历史概览显示“最近—”，不是0。收藏总65仍正确，原登录/进度保留。上传页同组件待单独实机。
- 上传书页2列→4列实机：`再次踏入光芒之中`及`시요,유야,티카티카`、同排长标题均完整显示，作者同基线；4列截图20260906-upload-four-fullnames.png，恢复原2列。当前新一轮全量unit/lint启动，MuMu正常shutdown，不清数据。收藏module/卡片修改仍待本地commit，不push。
- 全量unit/lint70563通过（6m14s），收藏及完整名称本地提交7e7affb（未push）。下一片BookDetailViewModelTest3项先红（16833缺类），新增独立BookDetailRepository/ViewModel：书/目录/评论/原封面/权限独立加载、代次隔离、草稿保留、访客不请求管理权限；当前定向build运行，**尚未接root**。不要拿此prototype当详情迁移完成。
- 4874详情3项定向通过（2m22s），BookDetailViewModel/Repository接root/AppContainer，移除旧loadBookDetail约130行；书籍/目录/评论等互不等待、source cover和身份代次保留，环境切换清旧详情再加载。新全量unit/assemble/AndroidTest正在跑，未实机安装；剩余详情管理/评论动作仍暂在root委派逐项迁移。
- 1343全量120 suites/952 tests/0 failures，Debug与AndroidTest打包成功（5m13s）。APK `8E29E1356DB079E884FF0EE66286DA671F4C01EF1C97CC889229E68A82AAC217` 已install-r，MuMu冷启动2941ms，原登录/65收藏/4列保留；从收藏进入《成为了S级的青梅竹马》，简介/207章目录/4条书评互斥切换正确。返回收藏原卡位置 `[151,202][260,502]` 不变，再开《老师不是恋爱对象》显示其22章目录与本书封面、作者、简介，无旧书串页。截图 `20260906-book-split-{catalog,comments,second}.png` 已逐张查看。仅本切片运行通过，不代表详情所有写入或Beta7发布门禁完成；MuMu随后正常shutdown为下一编译让资源。
- 消息迁移先保留旧Conversation/Settings算法，38802合成回归6项中5失败，明确复现刷新丢草稿、A发送回包覆盖B、发消息过程中输入丢失、success:false误报成功、设置保存取错快照。随后拆MessagesRepository/Inbox/Detail/Conversation/Settings各自state/jobs/environment，root移除400余行旧数据任务；新增收件箱统计与列表独立、已提交query与输入分离、分页/返回草稿/旧账号回包测试。4647首次修订因Kotlin中文插值缺大括号编译失败，修正后62908定向11项正在运行；尚未安装消息切片，合成设备UI用例已添加，绝不读取真实私信正文取证。
- 62908消息11项通过；随后59617协议4项全红，修复外层success:false被data对象遮蔽、拒绝读取误归空、错消息id、HTTP拒绝丢原错误。72294全量122 suites/967tests/0失败，Debug/AndroidTest通过（6m45s）。新开发APK `3E3C4528CFD630082BE0F015E81A4EE10EDCA269942A22A066A81850536FA960` 已无损安装，MuMu冷启动3450ms，接下来运行MessageFeatureDeviceTest两项合成控件；还不是Beta7最终包。另完整阅读22个相关交流帖并逐条分类，详community-review。
- 新包MessageFeatureDeviceTest两项MuMu通过（3.316s），截图20260906-message-conversation-draft.png已查看；生产界面实际经“工具→打开完整消息中心→消息设置”进入，过滤轨/统计/开关/免打扰字段正常，截图20260906-message-settings-native.png。没有打开真实私信或执行已读/删除/保存，UI dump仅在内存过滤控件标签，不留消息正文。源码又发现消息设置“编辑→刷新失败→重试”会把旧草稿恢复默认，已加特定回归，下一测试执行中。MuMu为build正常shutdown，未清数据。
- 77707消息设置失败重试丢稿用例确认红灯，改为独立dirty标记后84433消息12项通过；同批新SpeechProgressLatencyTest确认慢网站进度请求会阻塞后续本地锚点（1200ms超时）。修订为onStart立即SharedPreferences.apply保存小型本地状态、独立ReadingProgressSynchronizer串行/按书合并远端写，根阅读与后台TTS共用，失败不按每句盲重发。14887相关测试+Debug/AndroidTest build正在执行，新增真实源跨章后台TTS用例NativeTtsLiveDeviceTest待显式bookId/chapterId运行，尚未实机验证该进度改动。
- 14887慢网进度/顺序sync/消息12项共17定向全绿，Debug/AndroidTest build通过（7m49s）。最新APK `1C5CC984670AD0D3C536F7393B1765D0279612E427ADEC7D5720A0201BB166EB` 已install-r；MuMu本次开机83s，App冷启17.9s（启动慢证据保留，不把此前3s代表全部）。NativeTtsLiveDeviceTest开始用真实353686/6072568验证章末后台→次章首句/进度/熄屏/通知暂停恢复；禁止运行中安装/重启抢占。尚无最终Beta7发布。
- 真书TTS第一次选6072568是全书末章，测试明确无next失败，未播。公开v2目录核对9章后改6072567→6072568，49921 **OK1/14.806s**：起读第47段→下章第0段，440段真实派生内容，本地第9章、HOME/熄屏/通知暂停恢复正常。报告20260906-live-tts-353686-report.json保留；测试结束停止服务。当前87502同包Reader7项回归运行，尚无最终优化包/发布。
- 87502 Reader交互7项、67145分页4项同1c5cc984包全通过；随后MuMu正常shutdown，全量32735正在lint（unit124suites/972tests/0失败已返回）。规则同步风险已列findings，下一片继续；消息/进度这批未push，Beta7无远端发布。
- 32735最终全量972tests/0失败、lint通过（9m52s）。消息/后台进度本轮实现及设备证据保存本地可回滚节点，未push；MuMu当前shutdown、最后已装1c5cc984包。下一片替换规则Repository/同步/回归以及真实公共规则读写导出验收，仍不发布Beta7。
- 本轮节点4675917已保存（未push）。替换Identity4项先红65994；新基线字段websiteSource/websiteReplacement仅在确认同步后记录，旧无baseline规则不猜测覆盖。字面/正则身份、公共去重、用户order及安全跨设备更新后24862替换46项通过，尚未打新包。源网页8L_S-edK.js复核合并按created_at/id最新、个人优先，字面最长优先后正则。新增ReplacementRemoteWriter旧序列3项回归23839执行中，尚未接root。
- 23839旧write三项全红；先create/checkpoint再delete、同值read-reconcile、切书仍绑定store已接root，16758替换50项+Debug/AndroidTest通过（未安装）。额外legacy首次改source缺baseline新增用例71262先缺方法编译红；helper保护previous基线后通过93258其中writer5项。协议93258两项仍红：规则DELETE/读取success:false误判、forum/progress同类ack；已修并在4724全量unit执行中。待新包/真实公共创建及下载，不提前发布。
- 4724全量984tests通过；补公共规则POST/PUT明确拒绝校验，删除改为确认远端完成才删本机（失败原规则仍在），当前69179全量/Debug/AndroidTest打包中。新增显式opt-in ReplacementLiveDeviceTest仅允许353686，临时创建带Beta7验收标记的单条规则；测试记录自有id后，public开关/单条屏蔽/TTS派生文本/原文及替换TXT与EPUB四组合后删除规则/仅本次下载，尚未执行。
- 22:45执行环境恢复，旧unified进程handle已失；从daemon-25988日志确认69179在20:05结束：全量984tests/Debug成功，但AndroidTest第78行多写一个.content导致编译失败。没有启动公共规则真实测试/没有线上临时规则。已修该测试语法，当前重新assembleDebug/AndroidTest；MuMu仍未运行，最后已装包仍1c5cc984，不能以20:05新应用包代替未编译的新测试包证据。
- 48180配套build通过，APK830903DDF5A3EB4F371CC7B77CE92053EEBF4F83694085488597DFBA6482291E、测试8FFEB087...0BA8553B安装成功。MuMu冷启本次84s、App15.6s。64727真实公共规则+4导出 **OK1/39.306s**：公共回读/按书关/单条屏蔽/TTS派生文本/原章节不变、TXT与EPUB原文替换分开均通过；临时公共规则和4下载URI清除、GET确认ruleRemoved=true，pending文件已删除。报告20260906-public-replacement-report.json，详情见replacement-audit。当前无线上测试规则和活跃下载，尚无Beta7发布。
- 5659 Reader7项同830903dd包回归全通过；MuMu正常shutdown，正在lint，替换本批准备本地commit（未push）。公共规则live临时内容清理已闭环，不得重复清理其他用户规则。
- 24358 lint通过（5m51s）；此前69179全量984项绿色，48180测试包配套完成，真实公共规则四导出/Reader7已通过。保存本批同步和优先级修复本地节点，未push/未发布；后续账号隔离与正文节点结构仍继续。
- 本批节点8279cf4本地提交完成，未push。下一规则账号隔离14278两项先红；每账号key+原登录归属+保留legacy副本迁移实现，无原身份时不自动迁给新登录者，增加确认恢复本书旧规则为停用/无远端ID副本的入口。30853三项隔离/六项store定向正在跑，尚未安装；新增ReplacementUpgradeDeviceTest在设备内部核对原账号旧规则/开关，不导出实际规则或登录材料。MuMu仍shutdown、最后830903dd包。
- 30853规则隔离+store9项通过，94223全量128 suites/987 tests/0失败、Debug/AndroidTest通过（3m7s）。APK41FF74ED99D3E09E053E711FE0ABAE5DC69BD1C287882B9D250968106EBDFC1B已无损install-r，App冷启8.565s；ReplacementUpgradeDeviceTest实机通过：legacyBook1/legacyRule1/loaded1，逐字段比对/原开关保留、外账号为空、备份仍在。报告20260906-replacement-upgrade-report.json。79176同包Reader回归进行；尚不是整个Beta6→最终优化Beta7升级验收。
- 79176同包Reader7项通过，87186 lint通过（6m55s），规则账号迁移切片保存本地提交；未push/未发布。MuMu当前shutdown、最后41ff74ed包，下一片派生正文结构保护的实测回归及剩余领域继续。
- 节点a5b1eb4已保存。正文结构21963四项2红：替换新Markdown图被下载、星号变格式。新增ReaderTextDerivation（仅内存、不入原文cache）先解析原图/格式再逐文字节点应用，阅读与后台TTS共享；67261结构/替换/句群定向通过。下载同问题87890单项先红，EPUB改原图匹配优先、文字callback后替换，当前73820相关回归执行，尚未打包/安装这一切片。又全文读15相关交流并分类，公共规则旧样本清理保持完成。
- 73820节点/EPUB/替换定向通过；TXT纯文本标签输出58838先红，分开原图标记与纯文本callback后74945全量130suites/993tests/0失败、Debug/AndroidTest通过（4m55s）。新APK1D29E7940885331FB6CFD04F1EB3BDCE06A3D9F21D7CF9527AB9D3B08CC0EB83，MuMu正在launch待安装，再跑新增节点分页/真实TTS/公共规则4导出，尚未实机此包。
- 1d29e794节点版已install-r，冷启4.243s；NativePaginationDeviceTest新增替换输出保留文本等共5项MuMu通过（20260906-text-nodes-paging）。现同包真实353686第8→9章TTS回归进行，再做公共规则导出；不能运行中安装或重启。
- 76012真书TTS复测目录GET SocketTimeout；97531只读重试源准备60s超时，均未开始TTS。电脑同代理目录15s超时，直连403/cloudflare；现有Edge连接不存在且未确认前台挑战页，cf-human-cursor按技能限制未盲点运行。没有发布新QA规则；已有成功830903公共规则报告不能冒充新节点包验证。25572离线Reader7全通过，系统TTS同包测试继续；网络问题单独记录，其他工程推进。
- 51693同包中文系统TTS2项真实发声通过；44301 lint通过（5m4s），与993全量/5分页/7Reader构成节点保护分片证据。节点新包联网TTS/公共规则导出仍受代理源读取超时影响未闭环；本地保存切片不发布，MuMu当前shutdown、最后1d29e794包。下一性能片将连续阅读解析/替换移到后台，避免追加整章在Main上执行。
- 节点保护本地提交98262f7（未push）。ReaderDocumentPreparerTest先缺类型68190编译红；新增Dispatchers.Default解析/规则派生、每书8条LRU缓存、复用追加前的章节，root从remember同步解析改LaunchedEffect后台发布。63570少ensureActive import编译红已修，13404定向3项+Debug/AndroidTest正在执行；当前未装此性能切片，完整长滚动内存窗口/性能3轮仍待。
- 13404后台解析3测试/Debug/AndroidTest通过（4m32s），APK8A00D82EECCADA19C0B7A7F25822448B83B3B9244665FB96CBF95265E23ED7DA已install-r，冷启6.463s，3172 Reader7项通过。电脑目录GET恢复200，但64294真书TTS正文获取超时（未播）。现有登录WebView Playwright对照：/book/353686/6072567根html/body/nuxt高度0、reader-content文本0，无CF挑战；证据20260907-mumu-web/reader-live-network-compare.json，不冒充源正文已成功。MuMu现在shutdown让全量unit/lint，继续其他模块。
- 74143全量131suites/996tests/0失败、lint0错误3警告通过（7m43s），后台解析切片本地保存，不push。下一个人页拆独立ViewModel，修正共享profile序号导致分区卡Loading/保存草稿覆盖/写后读失败误报等问题；当前MuMu仍shutdown，无活跃任务或公共临时规则。
- 节点860395b已本地保存。个人页ProfileRepository/ViewModel先沿旧逻辑做78483三回归全红，修为独立hero/inventory读代次、草稿dirty、account取消，24130三项通过；已接root/AppContainer并移除约360行业务load/mutation代码。61710全量unit/Debug/AndroidTest正在执行，未安装。追加失败写使相关panel卡Loading、写后读失败/重复购买风险仍需针对性覆盖，不能把3项通过当个人全页完成。
- 61710全量999tests/Debug/AndroidTest通过（未安装）。85175追加失败装备卡初始加载1红，移除失败前无谓失效后Profile5项绿；14988资料保存/装备购买外层success:false两协议红，已改通用ack/明确拒绝校验，全量下一build执行中。MuMu仍shutdown，个人页新切片尚未设备验证。
- 22233全量133suites/1003tests/0失败、Debug/AndroidTest通过（2m11s）。Profile切片APK495845B314A5ACBE2064D9A5E758548E09241E8028F68FEA8A178E2DC75B6D42准备安装；MuMu正在launch。现无活跃下载/线上QA规则，GitHub仍未push/未发Beta7。
- 495845b包已install-r/cold2.762s，实机头像框/badge、本人动态、上传书、仓库、签到日期记录均正常；截图20260907-profile-{feature,activities,books,inventory}.png已查看。未保存的用户名追加_B7_QA→刷新仍在→精确删除6字还原seeking，截图profile-draft-retained；没有资料写入/购买/签到。MuMu现shutdown让18487 lint，Profile切片未commit/未push，详情见profile-audit.md。
- 18487 lint通过（7m25s），本人个人页切片保存本地提交，未push。最后已装495845b包，1003全量unit/实际分区与草稿通过；MuMu仍shutdown、无活跃Gradle或下载。后续公开主页/创作管理/完整矩阵继续，不是Beta7成品。
- 本人页节点8be86e8已保存。PublicProfileVM三项先缺类型26511编译红，新增每user独立6-entry缓存与分区任务、返回同人tabs/旧回包不污染当前人、会话清缓存，6816三项通过。已接root/AppContainer移除旧约220行请求逻辑、返回可取原cache；43635全量/Debug/AndroidTest正在跑，未安装。动态分页仍第一窗口，下一步补合并不截漏与更多入口；MuMu仍shutdown。
- 43635全量1006tests/Debug/AndroidTest通过；88541复现多个来源分页并集合并后仅take(limit)导致漏动态。增加真实hasMore/partialFailure、本人及公开主页更多/同页重试/多页刷新保持；91582定向10项通过。43090再复现posts/bookReviews明确其他author仍混本人动态，补同作者检查；当前新全量build运行。此批仍未实机安装，后续网络恢复才验本人/他人页与多页；MuMu关机，没有线上测试写入。
- 43921全量135suites/1010tests/0失败、Debug/AndroidTest通过。公开主页按用户+tab/filter scroll缓存，5454新6项/Debug成功；修正旧tab dispose必须写旧身份key，新增第7用例，35053全量再次执行。最后已装仍495845b本人页包，公共主页/动态分页/scroll新批尚未安装，MuMushutdown。新增功能源码不能代替当前设备证据。
- 35053全量1012tests/Debug/AndroidTest通过，DA559464...AD7D83F7已install-r/cold4.782s，实机榛名全色100002作品46/签到215/动态正常，书→详情→返回以及动态→帖→返回坐标保持。截图public-profile/books/checkins/activity-back已查看。公开卡缺指标仍空白长标题截断，已改紧凑完整名称卡；91819全量/build含只读PublicProfilePagingLiveDeviceTest新更多按钮验证，未安装当前改动，MuMu已shutdown。
- 91819全量1012tests/Debug/AndroidTest通过（4m10s），公开紧凑卡包12D11E9598693A2867B74C24D4632FDC04CBA3AA4AF5A02B28731ED3BB338A70，MuMu正在launch待install-r与公开动态真实page2按钮验证。本切片尚未commit/发布。
- 12d11e95包已安装/cold3.392s，21187公开分页live首轮在performScrollTo找屏外lazy节点失败（数据hasMore已证），不是已完成分页验收。增加public-profile-list testTag改用performScrollToNode真实滚动，60199重build通过，84DB5453...EAD9AAE1准备重装复测。MuMu正在launch。
- 84db5453包已install-r，54168公开分页实机**OK1/3.97s**：page2，6→17条、旧条目全保留，报告20260907-public-activity-paging-report.json。原生公开作品紧凑卡长名作者全显示，截图20260907-public-books-compact已查看。MuMu现shutdown让lint，无额外写入，准备本地保存切片；尚无Beta7发布。
- 9829 lint通过（6m13s），公开主页/动态分页本批保存本地节点，未push。当前最后已装84db5453，1012全量unit+公开分页真实按钮/完整作品/返回通过，MuMu仍shutdown；下一工作区真实队列+配置业务迁移，不把旧“只改状态”计为已实现。
- 6960db6已保存公开主页节点。工作区初始Repository/ViewModel尚未接root，三项旧算法回归准备；43355/94958 adapter错误函数/缺参编译失败已校正为既有setWorkspaceCookieActive，当前定向build进行中。没有读用户实际key/cookie或执行远端配置写入。网站dSlFh-Ca源码已核对真实prepare/raw/submit和模型调用，旧队列只有状态变更必须实现真实执行器。
- 49677工作区旧逻辑3红、48186修后3绿，接root后50051全量1015tests/Debug/AndroidTest通过（未装）。95223账号切换串配置/外层拒绝两红，82679工作区相关测试修后绿色：按账号隔离key/jobs保留legacy、未归属确认恢复、不自动共享/运行；API/Cookie明确拒绝不伪空、不伪成功，手动重试create先read避免同值重复，UI失败草稿可继续编辑。新全量build执行中，尚未实机；真实翻译队列仍未实现。详细协议见workspace-audit.md。
- 8728全量1017/Debug/AndroidTest通过后发现工作区13请求漏/api前缀（源HTML apiBase及dSlFh-Ca拼接确证），81739新协议用例先红；修正确路径并纠正旧WorkspaceApiTest保留参数断言，35691全量138suites/1018tests/0失败、Debug/AndroidTest通过。当前MuMu launch准备安装工作区配置切片，未读用户secret/未远端配置写入。
- 配置APK9862376C...17089275已install-r/cold3.076s，95971合成WorkspaceDraftDeviceTest通过（保存失败草稿重新编辑字段保留，不调用实际API）。实际workspace读取出现错误全0，公开源stats显示data.apiStatus是7个翻译数量数组，不是旧API汇总object；36304新样本先红，修translationCounts及从translators计算API数。58004缺totalRequests模型字段编译失败，已补当前下一全量build，MuMu已shutdown。真实配置值未记录，源统计仅记录状态/数量。
- 22708全量138suites/1019tests/0失败、Debug/AndroidTest通过（11m31s，本机资源慢），现在MuMu launch安装统计数组兼容包；配置模块真实页待复验，真实任务执行器尚未实现。
- EAC6038CED0D84192F86627991B49E80A8123DE318F8ABF9D0C35B17B04CD598统计包已install-r/cold3.727s，真实工作区API383/健康18及7类翻译数量正常（待翻8397、失败54、自助7330、上传暂不可57977等本次值）；截图20260907-workspace-status.png已查看，无secret。35075同包草稿UI再次通过；MuMu现在shutdown让lint，配置本批未commit，真执行器仍下一项。
- 21522 lint通过（8m19s），工作区配置切片保存本地节点，未push。最后已装eac6038c，1019全量unit/草稿UI/当前源统计实机通过；真实翻译执行器仍下一阶段，MuMu关机无任务运行。
- 0dbe284后真翻译队列切片：新增Runner/Coordinator/Service/TaskStore/模型协议/UI。46263三红（列表缺项误判成功、停止掩盖不确定、配置删除启动异常）→86037九绿；60263/59858磁盘旧Queued红灯定位Windows AtomicFile覆盖rename差异，生产读回检查点后才继续，JVM旧版备份算法+MuMu15实测分开。13558定向19绿，67679畸形术语新增红灯修后91994全量143suites/1040tests/0failures，Debug/AndroidTest成功。
- 真队列开发APK0E63206D5A61238C5D0D11862B4BBE78B5F3D0176FEE4167E6D957F0A4AC442E已无损安装，cold6225ms。53410设备测试首先因POST_NOTIFICATIONS原本拒绝而无通知超时；临时授权后83239已走完真实后台暂停/继续/完整checkpoint读回，第二任务因ActivityScenario无法从HOME仅moveToState拉回前台失败，测试改真实启动新Activity。失败原始日志保留，不能当全测试通过。
- 真实工作区新队列空态截图20260907-native-translation-queue.png已看，用户无本人API，未执行外部模型/源站译文写入；旧配置/登录保留。通知权限已恢复原拒绝状态，MuMu shutdown。新增首次后台任务通知请求（下载/TTS/翻译）+可拒绝降级说明、selected任务tab自动滚入视野、通知弹窗真实测试；77506全量/Debug/AndroidTest正在构建，尚未安装该改动。
- 77506全量1040tests/Debug/AndroidTest成功（10m9s），C04EE1E3...8436CEE4已无损安装。1007通知真实授权弹窗通过、51375真FGS队列完整测试通过：后台暂停/继续/读回完成，提交中停止保留不确定，普通resume不重发。MuMu在线，临时通知已授权供回归后恢复；无真实模型/站点译文写入，源码未commit/push；继续源GET与Reader回归。
- 用户继续后16:22 MuMu已关闭、ADB连接消失；正常launch重连，未清数据。当前c04ee1e3包真实工作区raw GET353686返回0候选（prepared=false，不冒充prepare完成）；11933分页5项、29468Reader7项通过。31290真书TTS353686第8尾47→第9首0/440段、后台熄屏/暂停恢复/本地章9通过13.401s，report已导出。
- 55839同c04ee1e3包公共规则真实创建/共享GET/按书停用/逐条屏蔽/派生TTS/原文未变/原文替换×TXT/EPUB4导出全通过10.376s；临时规则与4URI已删，report.ruleRemoved=true。截图20260907-native-translation-queue-final已看，选中tab完整显示。通知授权已恢复原拒绝状态，MuMu shutdown，85404 lint执行中；无活跃下载/翻译或临时公共规则，无GitHub写入。
- 工作区真队列保存本地b899f2b；85404 lint最终通过5m29s。下一切片ContinuousReaderWindow回归先按旧无限保留算法建立测试；当前未接UI/VM，MuMu仍shutdown，无真实写入或运行服务。
- 连续窗口14131四红→40151/38673定向绿色，接8章窗口、稳定key断点、保留草稿、上一章取回；84152全量1045里TranslationCoordinatorTest暴露1路抢块乱序。临时仅一块确认问题后恢复原2块断言，生产改固定worker/原序领块；4647定向通过。67694全量单测和Debug成功，但编译期间新加semantics定义未进入本轮main classes导致AndroidTest unresolved；96741重复任务已取消，当前新完整build接下工具session。尚未装连续窗口包。
- 76221全量1045/Debug/AndroidTest通过，7261F912...E5917107已无损装。65384初次滚动测试仅ScrollToNode保证可见而不保证首段为第6章，改exact anchor目标后43546仅AndroidTest成功、31419真窗口位置/回载第3章通过；NativePagination5、Reader7和权限回归同包通过。通知恢复拒绝，MuMu shutdown。56697发现前插缓存逐条驱逐导致8章重解析，延至当前窗口命中后再trim→97262新4项通过，尚未装此最后preparer变化。
- 登录79111三红（整屏无限等待/错误不结束/共享auth清理）→93816七项绿，node guard合成真实执行1绿。55628全量145suites/1049/Debug/AndroidTest通过，E3C83B7B...00BED314无损安装。87763检测闭合Shadow DOM iframe为0超时；Playwright仅连接现AppWebView确认真实三provider与CFframe，普通ADB点已见框后“安全验证已完成”回填截图，未登录POST。返回误用/profile实为收藏，文档已纠正，不冒充UID证据。
- 18785测试固定竖屏/closed-shadow伴随输入节点修订构建成功，A6A571E4...BDC23D32已装。26103源验证UI+取消前后currentUser ID一致通过，报告captchaVisible1/providers3/loginSubmitted=false；真实回填另见上条人工证据。当前继续window/pagination回归，MuMu在线，无临时规则/下载；尚未发布Beta7。
- 17645当前a6a571e4窗口/分页5回归通过。新DownloadImageOccurrenceLiveDeviceTest只读授权TXT图引用+4章JSON，不取图；23727读取旧350192下载票据明确410已过期，未新授权。27767测试限定410且无本轮授权时只申请1次新票据后继续（用户允许积分验证），已AndroidTest build成功；MuMu正在launch重测，无20GB图下载任务。
- 双图已真实确认：41527旧票据404（前次410后清除）后45540允许404/410明确过期；11282对350192完成1次新授权、读取1365章导出+4章正文、0图片下载。源导出19573次图/36相邻重复；第2/3/7/9章导出各2同URL、正文各1，证明此前仅对齐导出基线不足。报告20260907-download-image-occurrence-report，临时TXT已清。新增按正文权威出现数/顺序校对，仅核重复章；合法同图多次保留，缺数据或顺序不符失败不假成功。79703三归一化红修colon，88791定向中，尚未装此校图改动。
- 88791图校对11项绿；89548编译中追加normalizer导致新单测未见旧main失败，83644最新全量/Debug/AndroidTest通过。51AB4A94...AC113438已装，76181真实350192四章片段EPUB4图/4引用/4,395,806bytes、原图SHA全一致（原各2变各1）；report已取。68356同包公共替换与4导出全过，临时规则/URI清理。登录保存2c7f743本地未push；MuMu现shutdown让lint，无活跃下载/翻译。完整大书新校对性能/终包仍待。
- 47809 lint通过10m56s，49498下载定向绿。双图修复保存63e11c9本地；后续15202增加4路有界预检/重试复用6项通过。新4681下载完成打开反馈：书籍详情新增打开/分享、重进读本账号最近完成记录。65588完整unit绿但lint因编辑中的文件行号偏移报告2处缩进；最新59689完整unit/Debug/AndroidTest通过，E78B3F01...1C5F8C3D已装。54579真实详情→打开合成TXT→重进恢复入口通过4.305s，仅测试TXT和自身记录已删除，不花积分。尚需当前lint与最新完整production下载性能。
- 09-08中断续接确认无遗留Gradle/下载，89003全量/Debug/AndroidTest成功、607b6d4e已装；89835整书预检86章之后第148导出多独有1图而失败。46447诊断后44696确认该1图并非重复；新增只去掉已确认重复、保留export-only图并显示计数，46809通过、3224a710已装。80981整书908校对/6633重复移除后第1035无效URL失败；48630诊断、27788075已装，99049只确认第一个URL无效（不是data）。58097补细部shape测试未装。19410独立空src用例红→72601修后ReaderText/图校对通过。当前整书未完成，不发布。
- 89008完整unit/Debug/AndroidTest过，e295fc4e包49699实际1035章emptySrcTags1/validImages19已证，完整预检另遇源500而失败。34625增加仅安全GET有限500重试test/build过，8F545321...95B86358已装；73131完整1365章校图通过（1133有重复章GET，移除9171额外引用，保留23处源导出独有图，76.593s），report已导出。MuMu已shutdown，无活跃校图/下载/临时规则，尚无Beta7优化包发布。整书预检≠最终11GB包已生成。
- ForumPostRepository/ViewModel最初3定向41709通过，拆root约450行详情/写入；返回缓存/每帖scroll、分页接口及4测试已接。80230全量旧slice绿但lint新空src那行缩进报错，已纠正。32924 public暴露internal scroll编译报错改internal，98871完整unit/Debug/AndroidTest通过但尚未安装。22686更严格分页顺序断言红，改真正append；新增根/回复同ID防御性回归（752旧帖索引未发现现网碰撞，不宣称线上已复现）76589通过。当前最新完整gate继续，MuMu仍shutdown，forum新片未实机。
- 40386完整150suites/1069/Debug/AndroidTest通过，cb071e6b已装；78268切旧1422源已删除而超时，网页native错误明确不存在。换现存1934后20463真实App帖草稿/目标切帖返回通过。70117新发送echo滞后GET丢回复回归红，82770修后论坛相关绿/Debug/AndroidTest过；27437180...8C706121已装。嵌套回复测试初始非Unit未执行；修测试重建77484，48212本人1871/2391真实嵌套发送1条created2439/root4549，回读确认为1，标记729d0bbc，测试回复保留。无其他发帖/投票/反应写入。
- 27437180同包69599文件打开/连续window/分页5通过，5525通知授权/Reader7/验证码展示并cancel前后账号一致通过。通知测试临时授权已恢复拒绝，MuMu已shutdown让当前unit/lint跑；本轮创建的合成TXT/下载记录已精确删除。没有待清公共规则，无活跃下载，测试回复2439明确保留在本人App帖。下一核心是修双图后的整书生成/大小/性能，不用旧23GB证据代替。
- 30507最终完整unit/lint通过5m17s。新增加空data-src不应掩盖非空src回归正在定向验证（不属于此前全量），MuMu保持shutdown，无活跃下载或额外评论写入。
- 2026-09-08 11:11 exec62934修正后真实350192整书终于通过：1365章/10402正文图文件与引用/独立封面，12,197,807,311bytes，24分32秒含全ZIP/原图SHA检查。source19573减9171额外重复、保留23导出独有图，首次在完整包验证而非只预检。PSS约200–214MB；最终为明确本机FileProvider兼容保存。只删除测试自己成品，work/native-downloads各4KB，报告f5b59a5d...eaa56839已取。MuMu正常shutdown，开始13152翻译重试范围/缺候选/损坏块三个新红灯；未改对应生产实现。GitHub未动。
- 12:15本地保存d306708（未push），翻译13152三红→64923全绿，89054全量150suites/1077tests及Debug/AndroidTest/lint通过9m14s。985EE739...8A40BF47无损install-r，80453真FGS翻译合成源/模型后台/暂停恢复/未知不重发通过，72348分页5+Reader7通过。通知权限恢复原拒绝，MuMu已正常shutdown。752帖全文分类已全部登记、与thread-index核对missing=[]。下一上传3个协议红灯7779进行中；未真实上传/修改作品，Beta7未发布。
- 16:21上传独立状态/固定写入/源status/未知ack：7779三红→10314 XML16绿，63955 envelope两红→修后49846全量151suites1088、Debug/AndroidTest/lint全过21m19s。6EEB11E4...FA61713E已无损装，43991自动化误找未绘制提交item失败→test改父列表scrollToNode、36226只重建AndroidTest→36089合成上传表单取消/核对/成功ID全部过；10577同包Reader7和生产论坛切帖过。直接生产/upload URI被工具策略拦截，未绕行，不声称该入口实测。1936source只读81/82/83图2/0/3，82源仍缺补图；报告已取，无外站抓图/读进度写。新反馈总461及新增回复已全文核对；当前无运行下载/翻译，未GitHub发布。
- 16:54编辑器存档5691三红（缺正文空稿/不校验/备份不恢复）→11412六绿，后台加载错误保留/新编辑不覆盖接线；52982完整1093/build/lint通过11m40s。随后74292未知指针误删/65413相似ID正文误删两红，修为保留未知与精确generation，51225全量1095/Debug/AndroidTest通过2m52s。尚未装新包，MuMu正在launch做EditorArchiveDeviceTest；这最后两处小改未重新lint，不把前轮lint算最新。用户存档/下载未删。
- 17:24存档697d7f91已install-r并实际AtomicFile恢复/损坏拒绝、Upload/Reader7通过，2b2645f保存本地，8554lint通过7m50s。后EPUB解析39633四红→2782五绿，34125扩展9项属性注释一红→42340十项绿，18751完整1103unit/Debug/AndroidTest过4m26s。18D5330D...96D47943无损装、21622真实Android XML两章往返/同文件两锚点/特殊文本保留、上传合成表单全过，report339293fb...edcef80已取。parser最新lint仍待；未真实上传、未GitHub写入。
