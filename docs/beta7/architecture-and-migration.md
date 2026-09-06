# Beta 7 架构与迁移合同

这是用户批准的实现合同，正在分阶段实施。TTS应用级服务、文本节点管线、搜索/统一导航/新分页/持久下载/下载记录/屏蔽/论坛feed/收藏/详情读取/消息各子模块已接现有入口，并有分轮开发包实机证据。456条反馈基线全文分类已完成，B7-01细部控件/角色映射及相关交流主题仍未闭环；个人/管理/创作及reader数据层仍需继续迁移。不把全局状态类简单移动到新文件就算架构重构。以[执行记录](execution-log.md)为当前状态。

## 依赖方向

`App shell → Feature UI + FeatureViewModel → Repository → Domain API/Store → shared transport`

- 仍是单 `:app` 模块。显式 `AppContainer` 构建并注入依赖，不引入 Hilt/KSP 作为本轮前提。
- 外壳只负责主题、系统窗口、根导航和生命周期组合；`NovalPieApp` 不持有业务任务，`NovalPieViewModel` 不继续汇总全应用 mutableState。
- 领域：auth、library/search/books、reader、forum/comments、profile/wardrobe、messages、workspace/upload/editor、admin。每项独立 immutable state、事件、request identity 和 repository。
- API 保留现有 `org.json` 兼容字段，按业务拆开 transport 与 normalization；协议样本证明签名/会话/AES-GCM/下载授权不变。
- Navigator 保留 `AppRoute`/深链语义，按 entry 保存搜索条件、列表滚动与详情 ID。请求结果同时校验 route ID、账号代次与请求代次。
- GET 的有限重试只处理可重试网络状态；非幂等写入不盲重发。错误分类至少区分未登录/权限/网络/上游拒绝/解析失败/内容缺失。

## 阅读器新接口

- `ChapterDocument`：不可变解析文档，保留原始内容身份、text span、图片资源引用和章节评论边界。
- `ReaderAnchor`：书/章节 ID、稳定 block ID、文本 offset 或图片位置；不以短暂 LazyColumn index 作为唯一持久断点。
- `PageLayoutKey`：正文/规则修订、字体与排版参数、可读区域尺寸、图片尺寸信息。
- `PagePlan`：页面片段及锚点范围的完整布局结果。长段落以真实行边界分割；可整段容纳的优先保持段落完整；不能整段装下一屏时必须可继续阅读。
- 单一翻页控制器接受 Next/Previous/Chapter/Anchor 操作。点击、音量和工具按钮只转换成这些操作；一次物理输入至多一次翻页/跨章。
- 页眉/页脚/安全区是布局预算；打开工具栏和迟到图片都保持 semantic anchor，不能通过涂白遮罩或额外一屏尾 padding 修饰假分页。
- 重排任务按 generation 发布，旧结果丢弃；连续模式复用文档和锚点，按章节拥有评论状态，预取不更新已读进度。

## 应用级任务

- TTS：application-scoped playback coordinator + foreground playback service，页面仅订阅状态/发命令；处理 audio focus、耳机断开、锁屏/后台、旧 utterance 回调、跨章首句、暂停恢复与关闭入口。
- 替换：一条派生文本管线用于正文/TTS/导出，只改 text node，不改 HTML attributes/URL/图片标记；公共规则本书策略和单条屏蔽保留。个人本机停用不得无提示变成远端 DELETE。
- 下载：download coordinator + foreground data-transfer service；新增持久化 `DownloadTask`/检查点，授权 ticket 与任务身份绑定，任务执行与 Activity/路由分离。进程恢复可继续或明确说明需重试。
- `DownloadTask` 至少记录任务 ID、书籍 ID、格式、源/替换模式、冻结规则修订、阶段、已完成章节/图资源、失败项、私有工作目录及最终 URI。敏感票据使用既有安全保存边界，不写进公开日志。
- 流式原图，按出现位置写正文引用、按资源身份复用字节；并发用户值与实际有界调度分开。成功发布前必须核验目录/章节/图片引用及全部必要任务完成。

## 无损覆盖升级

- 公开 Beta 6 基线源码提交：`a137f899b133fb2022abd8a88a7048144da17f12`；发布 APK `30f07ac0...9707d6c3`。
- 新 optimized beta variant 使用 `applicationId=com.novalpie.app.debug`，`debuggable=false`，R8/resources shrinking 开启；证书 SHA-256 必须等于 `cc7d6fe6844d4a4ee510d65b185ac5e32a642faa9bfb819df75d51be4c607047`。
- `versionName=2.0.0-native-beta7`，versionCode 必须高于 Beta 6 的 `2026090101`，不得以卸载/清数据解决不兼容。
- 先兼容读取现有 SharedPreferences/cache/MediaStore，按 store 小步迁移。迁移必须可重跑、检测失败后保留旧记录，不以空默认覆写有效数据。
- 会话、阅读锚点、个人规则/公共屏蔽、字体主题、搜索筛选、网格列数、离线缓存和已有 Downloads 文件逐项验收。更换账号不得混用上一账号数据。
- 规则store保留原prefs文件与legacy keys，首次Beta7保存原登录账号id归属。每账号个人规则/公共单条屏蔽/本书策略/revision隔离；数据与迁移标记原子写，恢复默认不再次重放旧值。升级无原身份时不自动归给后来登录者，提供明确确认后恢复本书旧规则为停用的本机副本，移除远端ID、不发布；旧副本始终保留。定向测试先红后绿，MuMu实际旧状态迁移仍按独立证据验收。
- 2026-09-06本项实际迁移已通过：41ff74ed...6ebdfc1b开发包install-r，设备原1本/1规则逐字段及本书开关保留，另一合成账号为空，旧键保留；`20260906-replacement-account-upgrade.{json,log}`与report只含计数，不导出个人规则。不是最终优化包或所有store迁移通过。
- 基线保存、每个模块改造和修复单独可审查提交；不能同时大规模替换 JSON 协议、导航和所有 UI。

## 发布

最终只在 optimized beta APK 验收通过后推送源码/标签并创建新 Beta 7 Release；不覆盖 Beta 6。实际构建 commit、安装包哈希、Release digest 一致。中文说明包含各板块自己的原生特点、网页差异、必要网页依赖和未认证的真机/OEM/高刷边界。
