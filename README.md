# NovalPie 2.0 原生 Android 客户端

基于 Kotlin + Jetpack Compose，连接 [novalpie.cc](https://novalpie.cc) 的账号、书库、论坛和阅读服务。

> 2026-09-12：Beta 7 仍在重构与验收中，**尚未发布**。当前源码有 1,194 项单测通过，优化 APK 已生成；最新包尚因主机资源不足未完成 MuMu 安装复验。测试通过不等于全功能验收通过。
>
> 当前进度：[状态摘要](docs/beta7/STATUS.md) · [剩余交付清单](docs/beta7/REMAINING.md) · [完整需求](docs/beta7/requirements.md)。旧 README 的历史版本保留在 Git 与 `docs/history/` 中，不作为当前功能证明。

## 与网页不同的部分

- 原生按行分页、段落断点、音量键翻页开关及无动画选项；连续阅读和分页共享章节身份，字体变化重新布局。
- 使用 Android 系统 TTS 和播放服务；声音取决于设备安装的引擎，不捆绑付费云 TTS。
- 公共替换规则可按书启停、逐条屏蔽；个人规则与公共规则的共享关系明确，屏蔽不删除他人贡献。
- 原生 EPUB/TXT 可选原文或冻结的替换规则快照，保留原图和动态图字节，提供有界并发、暂停/继续、取消与下载记录。
- 收藏与上传书籍页面提供 2/3/4 列；封面预览、书籍管理长按与正文交互按不同入口处理。
- 新增原生网页阅读配置管理：主动保存、加载、更新、设置默认和删除；不会自动覆盖本机设置。

这些功能有分片测试和运行证据，但 Beta 7 全部组合、所有角色、不同屏幕与后台分支仍按验收清单关闭，不能把这份功能介绍当作最终通过报告。

## 保留的边界

账号、内容、积分、权限和可下载范围仍由网站决定。App 不会伪造服务器缺失的章节、图片或翻译。

正文不提供复制/全选；不恢复轮盘或无效“网页正文”按钮；不引入外部书源、爬虫、付费云 TTS 或横屏双页。安全验证和部分 HTML/CSS 装扮素材仍需要 WebView，主业务不以整页 WebView 代替。

界面与正文已有简繁转换实现；此前“仅简体、无 UI 测试”的说明已过时。具体覆盖情况以当前 APK 证据为准。

## 构建与测试

需要 JDK 17、Android SDK platform 35 / build-tools 35.0.0。本项目当前锁定 Gradle 8.9 与 AGP 8.7.3。

Windows PowerShell 示例：

```powershell
cd D:\NovalPie\native-android
$env:GRADLE_USER_HOME = 'D:\NovalPie\.gradle-user-home'
.\gradlew.bat :app:testDebugUnitTest --no-daemon --max-workers=1
.\gradlew.bat :app:assembleBeta :app:lintDebug --no-daemon --max-workers=1
```

缓存齐全后可加 `--offline`。低内存主机应顺序执行测试、优化编译、R8、模拟器验收，不并发启动 Gradle 和 MuMu。此前遇到的主机内存耗尽不通过删除用户数据或修改服务端协议解决。

- `Debug` 用于开发及组件设备测试。
- `Beta` 开启 R8/资源优化，关闭调试，保持 Beta 6 的 `com.novalpie.app.debug` 包身份和同一签名以便覆盖安装。
- `Release` 是独立构建配置，不能不经签名与包名校验就当作 Beta 6 的兼容升级。
- Beta APK 输出：`app/build/outputs/apk/beta/app-beta.apk`。
- 验包工具：`tools/beta7-audit/verify-beta-artifact.ps1`。公开证书指纹与 APK SHA-256 可以记录，私钥及凭据不进仓库。

## 当前架构

仍为单个 `:app` 模块：

- `core/`：AppContainer、会话/代理环境、公共依赖和任务组合。
- `feature/`：已拆出的搜索、收藏、书籍详情读取、论坛、消息、个人页、管理、工作区、上传、编辑器、阅读/TTS/下载等业务组件。
- `data/`：API 兼容字段、存储、章节会话/解密、EPUB 和文件处理。
- `ui/`：Compose 页面、主题与导航入口。

**拆分尚未全部完成。** 阅读加载/部分进度与替换动作、书籍管理和部分账号/考试逻辑仍在根 ViewModel；集中 API 和页面文件也仍较大。不要把所有文件已迁到 `feature/` 当成事实。详见[架构合同](docs/beta7/architecture-and-migration.md)。

章节签名、会话、AES-GCM 和响应兼容别名由协议测试保护，整理代码不能擅自更换这些协议。

## 验证与发布

测试包含行为单测、MockWebServer 协议测试、Robolectric/Compose 测量与交互测试，以及独立的 MuMu/原生 UI 证据。字符串清单只是辅助，不能代替真实按钮、分页、TTS 发声或导出校验。

Beta 7 交付必须满足：

1. 所有计划中的功能缺口与阻断缺陷关闭，并关联当前源码和 APK。
2. 无损覆盖、当前优化包实际安装、核心流程和适配/性能验收通过。
3. 源码提交、`v2.0.0-native-beta7` 标签、验收 APK 与 GitHub 资产哈希一致。
4. 新建 Beta 7 Release，保留 Beta 6；中文说明列出 App 特点、网页差异和未认证的设备边界。

旧包成功、只跑单测、模拟器无法启动时的静态验包都不能替代最终安装验收。当前发布状态始终以[状态摘要](docs/beta7/STATUS.md)为准。
