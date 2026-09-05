# Beta 7 原生深度重构执行入口

状态：执行中；当前阶段为 **B7-01 需求、网站与反馈基线**。未开始业务模块替换，未发布 Beta 7。

最新续接进展见 [执行记录](execution-log.md) 和 [全站控件映射](site-control-map.md)。已有未接线的搜索/导航/按行分页/文本节点/持久下载核心与定向测试，业务界面仍是旧实现；这些不算 Beta 7 完成。当前安卓网站核对改用 App 已登录 WebView，未导出凭据。TTS引擎包可见性缺陷已有[实机修复前后证据](tts-device-evidence.md)。

## 不可变交付条件

- Kotlin + Compose，单 Android 应用模块，按业务拆分；保留现有外观，只微调功能缺陷、对齐、密度和适配。
- 全部业务纳入：论坛/书评/章评、阅读/TTS/替换、EPUB/TXT、搜索/收藏/详情、账号/公开主页/装扮/消息、工作区/上传/编辑器/管理员。
- Beta 6 无损覆盖升级：`com.novalpie.app.debug`，沿用既有证书，版本号递增。保留会话、阅读断点、规则、屏蔽项、设置、离线内容及下载。
- 阅读原文不可变；无系统复制/全选、无轮盘、无无效“网页正文”按钮；不增加外部书源、爬虫或付费云 TTS。
- 保留按书公共规则总开关、逐条屏蔽、个人优先、导出可选应用规则、原图、并发设置、2/3/4 列及完整书名作者、音量键开关和无动画。
- 最终优化 Beta APK 必须在 MuMu 完成真实安装、覆盖升级与全量业务验收。仅构建成功、旧截图或错误提示都不代表通过。
- 源码提交、Beta 7 标签、已安装验收包和 GitHub APK 必须一致。保留 Beta 6，不覆盖它；最终中文说明必须交代 App 特点、网站区别和未认证设备边界。

## 资料与执行顺序

| ID | 交付 | 状态 |
|---|---|---|
| B7-00 | 保存当前发布源码基线，确认编译与升级身份 | 已保存 `a137f899b133fb2022abd8a88a7048144da17f12`；Debug gate 通过；签名来自 Beta 6 公开证书 |
| B7-01 | 会话要求、冲突决策、当前网站路由/控件/API、全量反馈分类 | 执行中；参见 [需求清单](requirements.md)、[网站与反馈基线](source-baseline.md) |
| B7-02 | AppContainer、导航器、独立业务状态/Repository/API | 等待 B7-01 完成 |
| B7-03 | 真实文本行分页、稳定锚点与连续阅读 | 等待基线与公共接口 |
| B7-04 | 应用级 TTS/前台播放、共享文本替换管线 | 等待基线与公共接口 |
| B7-05 | 前台原生下载、持久任务/检查点与完整性 | 等待基线与公共接口 |
| B7-06 | 论坛/搜索/收藏/详情各项回归及功能补齐 | 等待基线；按需求 ID 验收 |
| B7-07 | 账号/消息/装扮/工作区/上传/管理员原生完整性 | 等待基线；按权限/控件 ID 验收 |
| B7-08 | 优化 Beta 构建、数据迁移、MuMu 多尺寸/性能/TTS/下载验收 | 未开始 |
| B7-09 | 同步源码，发布 `v2.0.0-native-beta7`、APK、中文说明并回读 | 未开始；验收失败禁止发布 |

## 证据规则

`historical`、`source-discovered`、`read-verified`、`write-verified`、`runtime-verified` 必须分开记录；每个 runtime 结论必须关联 APK SHA-256、设备配置、操作及结果。缺失证据记为 pending，不自动升级状态。

当前原生仓库是 `D:\NovalPie\native-android`。根目录七月的 README 指向旧 WebView/Capacitor 项目，只作历史出处；`docs/inventory/` 也是旧时点盘点，不作当前通过凭据。

研究证据保存在 `D:\NovalPie\agent-bridge\artifacts\beta7-baseline\`，不将原始公开帖子正文、站点完整 bundle、会话数据或大媒体文件提交进 Git。文档和生成工具保留 Git 版本；工具只请求公开 GET，不登录、不执行写入端点。

## 当前环境

- 当前源码基线通过 `:app:testDebugUnitTest :app:assembleDebug :app:lintDebug --offline --no-daemon --max-workers=1 -Pkotlin.incremental=false`（2026-09-05，本轮 exit 0）。历史测试报告 92 suites / 842 tests，不代表 Beta 7 验收。
- Beta 6 APK SHA-256：`30F07AC07FBAA0D79AA0FF8EBD126FF82B55456D392825B32D62E7BD9707D6C3`。
- 覆盖升级签名证书 SHA-256：`cc7d6fe6844d4a4ee510d65b185ac5e32a642faa9bfb819df75d51be4c607047`；只记录公开证书，不读取或记录私钥密码。
- MuMu 新安装路径由卸载注册信息确认：`D:\Program Files\Netease\MuMu\nx_main\MuMuManager.exe`；实例 0 为 Android 15，已正常启动，ADB 为 `127.0.0.1:16384`，900×1600/240dpi。旧 `MuMu\emulator\MuMuPlayer-12.0` 路径已失效。初始旧 APK `1d1dcc...` 已以 `install -r` 覆盖到当前公开 Beta 6 `30f07a...`，设备哈希匹配、数据保留。
- 旧 Edge 扩展接入失败；命名会话 `novalpie-beta7-guest` 在初始访客页之后实际成为 seeking 登录会话，已通过 65 条收藏及本人/管理员控件确认；该会话名不代表现在的访客状态。未由代理输入凭据或导出 storage。后续复用该窗口调查登录后状态，单独访客会话用于登录前验证。

## 下一项

公开源码和论坛分页采集完成，人工逐页核对进行中：259 资源、29 路由；反馈区 456 帖/10 页，交流区 853 帖/18 页，选取 752 相关主题并获取 2,140 条根/嵌套回复。获取完成不等于所有反馈已人工审查。新版源码分析生成 192 个规范化 API 候选（含经手工证明的 v2 wrapper 基址）和 2,824 个含兼容重复的控件候选，均未冒充运行验证。参见 [发现清单](findings.md)。

继续登录前后及管理员页面/子面板实测和反馈人工分类，建立每个控件的来源/权限/API/App 映射。B7-01 完成前不开始替换业务模块。
