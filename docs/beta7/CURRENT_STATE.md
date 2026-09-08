# Beta 7 当前工作点

更新：2026-09-09 01:23。完整经过见 execution-log.md，本文只保留最新事实，避免把旧包证据误认成终验。

## 发布状态

**尚未完成，不可发布。** 未升级Beta7版本号、未新增optimized beta variant、未tag/push/创建Release。Beta6保持原样。必须同包完成R8/无损升级/运行矩阵后再交付。

## 工程与设备

- 分支`codex/native-app2-current-20260821`，最新本地提交`83b6c77`（未push），管理/上传/原子存档/EPUB导入切片已保存。图片类型/缓存修订在未提交变化中；没有远端发布或版本号变更。
- 最后已安装开发APK：`D00DDE143EA804F878A64098DE903A50B0AFA9402C6DFF274873CED2B9A6EA16`，1116全量unit/Debug/AndroidTest；MuMu图片真实staging/ZIP类型、Admin合成UI、真实公共规则/原文替换四导出均过。前4a64b89d管理/论坛/Reader7过。管理+图片最新lint尚待。版本仍Beta6开发字段，不是Beta7成品。
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
5. 78654图片类型/HTML两红→7790绿，73042旧pipeline2一红→改3；17940全量1116/build通过。d00dde14已装，31553图片类型/管理UI过，8778真实公共规则+4导出全过8.226s且临时规则和下载已清；79006同包Reader7/生产Forum回归完成以session/报告为准。MuMu在线，无运行下载/翻译，图片切片待commit/lint。其余上传持久队列/全编辑器/角色/性能矩阵/R8升级未完成，不提前发布。

## 最新社区增量

15:58新刷新`20260908-delta`：源build`cbb20abd-9b37-4381-993e-3008189d738b`未变；反馈区461。原456反馈+296相关讨论基线752帖逐帖正文/完整回复均读完、missing=[]；新1935图片下载失败（无书ID）/1936书360516源82章补图已全文读、待源对照，1934/1927/1925新增回复也全读记录。App1871现在37条最新为本次明确测试2439，无更晚新用户回复；登录4645、双图4643定向已修，4681/2432下载打开已实测。发布前必须再次增量，全文读完不等于行为全部通过。

09-09 00:48新刷新20260909-delta：反馈总463，1939获取更新无效（无bookId）/1938图片后缀file（无assetURL）已读全文与全部回复；旧增量无变化，App1871仍37条/无更晚用户反馈，sourcebuild仍cbb20abd。后缀file需按实际内容类型/魔数测试，不重命名伪造格式。
