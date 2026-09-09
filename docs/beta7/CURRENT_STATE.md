# Beta 7 当前工作点

更新：2026-09-09 08:42。完整经过见 execution-log.md，本文只保留最新事实，避免把旧包证据误认成终验。

## 发布状态

**尚未完成，不可发布。** 未升级Beta7版本号、未新增optimized beta variant、未tag/push/创建Release。Beta6保持原样。必须同包完成R8/无损升级/运行矩阵后再交付。

## 工程与设备

- 分支`codex/native-app2-current-20260821`，最新本地提交`5914fb9`（未push），管理/图片类型/上传/存档/EPUB导入切片已保存。UploadDraftStore持久草稿变化未提交；没有远端发布或版本号变更。
- 最后已安装开发APK：`BB1B1EEC9716BD8560D3E0FE97A397F059F35AA40268D0B10D8D8638BEE83D59`，1129全量unit/Debug/AndroidTest/lint通过；MuMu草稿真实文件恢复/账号书隔离/未知不重发、上传表单、管理表单过。前71d99895 Reader7/生产Forum过。版本仍Beta6开发字段，不是Beta7成品。
- **exec62934整书350192下载/成品验证通过并结束，测试自有12GB成品已删除，工作目录均4KB。** 报告20260908-corrected-full-350192-report已取。MuMu随后正常shutdown为构建让资源，用户数据保留；没有运行下载任务。
- 上一27437180包文件打开/连续窗口/分页5/Reader7/源验证码cancel账号一致皆过；本人1871嵌套测试回复2439/root4549/target2391已回读一次，标记729d0bbc保留，不重发。30507全量/lint通过后追加空data-src用例60288红→属性边界/空值修正15302全量1071绿；最新包未跑最终R8矩阵。

## 已有关键证据（分片，不替代最终同包终验）

- 真中文TTS353686第8尾→第9首、后台/熄屏/通知暂停恢复：`20260907-workspace-live-tts`，c04ee1e3包。
- 真实公共规则发布、单条隐藏/按书关闭、原文与规则版TXT/EPUB四组合并清理：`20260907-image-fix-public-exports`，51ab4a94包。
- EPUB双图真实复现：350192导出2次而读正文1次；4章修正包4图/4引用/原图SHA一致，`20260907-open-build-image-archive`，e78b3f01包。
- 连续8章窗口、稳定段落锚点、回载上一章、分页5：`20260907-auth-window-regression`和`...auth-pagination-regression`，a6a571e4包。
- 原生安全验证真实显示/前后账号一致：`20260907-captcha-portrait-current`，a6a571e4包；正常点击Turnstile成功回填截图在e3c83b7b包。
- 下载完书籍详情直接“打开”，刷新后恢复URI，实际合成TXT可读：`20260907-completed-download-open`，e78b3f01包。
- 翻译真实FGS合成源/模型任务暂停恢复、未知提交不重发：`20260907-open-build-translation`，e78b3f01包。外部模型与网站译文真实写入**未验**。
- 新校图完整大书1365章/10402图片及正文引用/独立封面/12,197,807,311bytes、原图SHA/ZIP验证通过24分32秒，20260908-corrected-full-350192。旧23GB报告不再作为双图证据；本次仍非最终优化包。

## 当前下一步

1. 上传7779三红→10314修后16绿，63955外层ack两个红→已修。49846完整build/lint21m19s全过，6eeb11e4已装；43991仅测试滚动未绘制item失败，改test父LazyList scrollToNode，36226 AndroidTest重建，36089上传合成UI完整通过，无真实作品写入。
2. 10577同包Reader7/生产ForumPostFeatureDeviceTest全过；source1936只读第81/82/83章图数2/0/3，82源数据尚无补图。MuMu现在线，通知为原拒绝，无活跃下载/翻译。生产`am start ... https://novalpie.cc/upload`被工具策略拦截，**不要换工具/坐标重试此被拦操作**；受控表单不是生产入口全验收。
3. 存档门禁/实机闭环，8554 lint7m50s已结束。39633 EPUB四红→2782五绿；34125扩展9项一红→42340读写十项绿。18751全量1103unit/Debug/AndroidTest过，18d5330d包已装，21622 XML导入/往返+上传UI、11204分页5/生产论坛切帖过，报告已取。MuMu随后正常shutdown，最新parser lint正在执行（session以exec为准），禁止编辑lint读取源码/并发Gradle。无活跃下载/翻译。
4. 77664 parser lint已成功7m21s。AdminViewModel/Repository已接root，独立分区读/全局单write锁/失败草稿/日志行数。84680两红→37465定向16绿；11758装备兼容1红→分开confirmedAdmin parser，12824全量1111/build成功4m20s。4a64b89d包已install-r，41221 AdminFeature合成表单+69097生产forum/Reader7全部通过；无真实admin写入。
5. d00dde14图片/管理/真实4导出/Reader7/生产Forum全部通过，42570管理+图片lint已成功5m3s。MuMu已shutdown，无运行下载/翻译/Gradle残留（当前85497除外），临时公共规则与下载均清。
6. 持久草稿各轮红→绿，80603最终全量1129/build/lint过8m29s；bb1b1eec已安装，86986草稿恢复/表单/管理UI三项过。MuMu在线，无运行下载/翻译，待保存本切片。下一真正force-stop后两阶段验证，当前已有仅组件重建证据不可混称进程回收通过。
7. 上传组件重建已测，不等真正进程kill后的生产恢复。完整上传批次协调器/大JSON创建后续批恢复、全编辑器/角色/性能矩阵/R8仍未完成。仍无真实上传/admin危险写入/待清测试规则，严禁提前发布。

## 最新社区增量

15:58新刷新`20260908-delta`：源build`cbb20abd-9b37-4381-993e-3008189d738b`未变；反馈区461。原456反馈+296相关讨论基线752帖逐帖正文/完整回复均读完、missing=[]；新1935图片下载失败（无书ID）/1936书360516源82章补图已全文读、待源对照，1934/1927/1925新增回复也全读记录。App1871现在37条最新为本次明确测试2439，无更晚新用户回复；登录4645、双图4643定向已修，4681/2432下载打开已实测。发布前必须再次增量，全文读完不等于行为全部通过。

09-09 00:48新刷新20260909-delta：反馈总463，1939获取更新无效（无bookId）/1938图片后缀file（无assetURL）已读全文与全部回复；旧增量无变化，App1871仍37条/无更晚用户反馈，sourcebuild仍cbb20abd。后缀file需按实际内容类型/魔数测试，不重命名伪造格式。
