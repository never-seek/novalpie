# Beta7 原生下载发布事务修复

日期：2026-10-05。Owner：Codex；持续目标未完成。当前HEAD仍b4616be，本轮和之前工作区改动均保留，未commit/push/tag/更新GitHub。

## 1. 解决的当前发布窗口

原问题S11b/V04：成品已经进入Downloads，但Completed任务记录写盘失败；重试又publish一份。现在：

- 创建任何目标前，先在应用noBackupFilesDir/download-publications保存原子发布意图，冻结task/account/book/format/created/ruleHash、原始文件bytes/SHA256和随机nonce。文件名唯一，不认领/覆盖同名用户旧文件。
- MediaStore先用带nonce的pending文件名保留自己创建的row；写入前持久化URI。即使死亡发生在insert→绑定URI之间，也能按之前已保存的随机reservation查找同一owned row。
- 验证真实保存字节再commit，之后持久化发布收据。读取时校验owner/name/URI与SHA，发现损坏/外来记录/已改文件明确失败，不丢记录、覆盖文件或另存一份。
- 系统commit已发生却回执失败时，从最新持久URI查询状态；不能因为最初局部变量尚未含URI就误判“没有row”而转本地保存，造成公开+本地两份。
- 系统目录确实不能写且自己的pending row已安全清理，才继续原有App-local fallback。原本本地转存、FileProvider打开与原图字节能力保留；转存后源文件不存在也能恢复同一目标。
- Runner在task Completed保存前，独立持久化完成收据，完成后清理work目录也不会丢它。恢复已发布结果先验证收据/目标，不访问metadata、重新下载正文、重新授权扣分或再publish。
- 打包检查点已有SHA用于发布校验，避免额外整包扫描；流式复制及恢复校验仍64KiB有界内存、服从暂停/取消，不重编码图片。
- Android23/28保留应用外部目录文件路径，29+才使用MediaStore；新增真实API guard与RequiresApi，不关闭NewApi检查或添加lint baseline。

## 2. 失败驱动过程：没有把单测绿色当实机通过

- 最初6个新回归在旧实现全部红：丢Completed重复发布、work清理后再调用、同书同时间不同任务冲突、同名用户文件、跨账号/跨书ID、pre29重复发布。
- 现在16新增仓库回归，包括本地move后断收据、公共commit后断收据、已改/损坏目标拒绝、取消与再开、损坏runner记录、Unicode文件名字节上限、最小SDK23、同任务并发。全部在当前默认1369 tests/190 suites内通过，0失败/错误/跳过。
- MuMu初次发现MediaStore把leading-dot文件名改成`_.novalpie...`，导致owner/name检查失败而误入fallback。真实只读诊断保存于synthetic-pending-row-diagnostic.txt；不是只根据猜测改代码。当前reservation不再以dot开头且有正确TXT/EPUB后缀，保留严格owner/name检查，不放宽成任意同名可认领。
- 修正文件名后又发现insert完成但backing file尚未创建时，读取descriptor抛ENOENT。现在仅对已确认owned pending row作“未写完”处理，继续写同一row；published/nonpending目标不可这样重建。
- 两处实机失败日志全部保留；不是把fallback返回成功当作MediaStore通过。最终用例明确assert authority=media、保留完整private输入、同一任务URI不变且row count=1。
- 旧QA pending行按日志中精确随机task ID+包名+pending状态清理，仅本轮synthetic资产；用户已有下载未删除。最后App侧清理用例通过，shell读取报告No result found，但该模拟器content工具随后自身segfault；这个工具错误不冒报为App crash，也不隐瞒。

## 3. 当前验证证据

| 项目 | 当前证据 |
|---|---|
| 默认全量 | 1369 tests/190 suites/0 failures/errors/skipped；unit-final-results及full-gate-complete.log，exit0 |
| Debug/AndroidTest/Lint | 同命令全部完成；0 lint errors/18 warnings/1 information。最初4个NewApi errors已用API guard/注解修正，未改旧测试期望或关闭检查 |
| MuMu Debug | device-publication-verified.log：OK(11 tests)，31.292秒 |
| 真实MediaStore | 五个crash boundary：insert、绑定URI、写完字节、commit、receipt；全部恢复一份、同URI、原始bytes一致。另commit后IOException不转本地重复保存、同名旧文件保留 |
| 已有功能回归 | 本地FileProvider、真实root返回/Activity重建、阅读器触摸、中文PCM非静音和连续朗读同包通过 |
| 外置75项 | 57通过、18需求断言红、0 errors；S11b/V04已转绿。18=12确认缺陷案例+2空回执合同风险+4旧驱动/输入不适配，不当作18个独立bug |
| R8优化构建 | external-beta-gate.log assembleBeta完成；组合exit1来自外置18红灯，不声称所有门禁绿 |
| 最终已装包 | SHA256 `5e63806fb3971a3bff685e369f6cf3ee9388fc96cb8169da719088df47d67735`，3,341,771B；设备base.apk SHA相同 |
| 包/签名 | com.novalpie.app.debug /2026100101 /2.0.0-native-beta7，非debug；public signer摘要与原Debug/Beta一致 |
| 保留状态 | R8覆盖安装成功，冷启动约1.1秒，登录、收藏67、四列保留；optimized-home.png；未清数据 |
| 构建输入 | source-final-hashes与最终source一致；git diff --check clean；所有测试下载均合成，不发真实网站评论/不扣积分 |

MuMu：127.0.0.1:16384，Android15，900×1600/density240/字体1.0/60Hz。11白盒用例在同源码Debug包；R8只检查安装、hash、签名、冷启动及首页。**不能把Debug白盒当R8全矩阵、Robolectric23当Android23真机，或把小TXT当1GB+图文下载验收。**

## 4. 生产修改与新增测试

- D:\NovalPie\native-android\app\src\main\java\com\novalpie\nativeapp\feature\download\DownloadPublicationJournal.kt（新增）
- D:\NovalPie\native-android\app\src\main\java\com\novalpie\nativeapp\feature\download\AndroidDownloadPublisher.kt
- D:\NovalPie\native-android\app\src\main\java\com\novalpie\nativeapp\feature\download\NativeDownloadTaskRunner.kt
- D:\NovalPie\native-android\app\src\main\java\com\novalpie\nativeapp\feature\download\DownloadPackageCheckpoint.kt
- D:\NovalPie\native-android\app\src\test\java\com\novalpie\nativeapp\feature\download\DownloadPublicationRegressionTest.kt（新增）
- D:\NovalPie\native-android\app\src\androidTest\java\com\novalpie\nativeapp\ui\DownloadPublicationDeviceTest.kt（新增）

没有删EPUB/TXT、原图、可选压缩、并发、自定义并发、暂停/取消/重试或App-local备用保存；没有用WebView替代原生下载。唯一文件名新增随机后缀，书名/书ID/时间仍保留，避免碰撞与错误认领。

## 5. 完整目标仍需继续，尤其下列边界不提前关闭

1. **旧版本没有任何发布journal的历史保存窗口**：不能从人类文件名猜测一份文件就归本任务；本轮不提供静默自动认领。升级后这类未知窗口需要受控迁移/确认或强证据恢复，仍列缺口；新协议窗口的通过不等于所有历史任务均已认证。
2. 进程真实强杀/设备重启、空间不足、1GB+实际EPUB/TXT、外部阅读器打开及原文/替换完整下载矩阵还未按此最终包完成；本轮是系统transaction boundary受控故障，而非声称已杀真实进程。
3. 所有持久位置同时不可写、task record损坏/遗失但publisher journal完好、用户删记录的pending-row/收据生命周期、OEM owner字段未知/权限异常需要继续加覆盖。发现新输入不能被此批绿色用例掩盖。
4. 协调器遇到“文件已发出、完成记录保存失败”的反馈仍可能显示Failed；重试能按收据恢复不重复，但更明确的UI反馈与可打开入口还要审查。
5. 其余12确认缺陷（TXT/EPUB节点替换3项、编辑EPUB资源丢失、metadata/校图不完整、计数/回执/签到/总页数/亮度/徽章）与2合同风险、完整Beta7/58项矩阵继续。目标不缩减，未更新GitHub资产。

Next owner: Codex。下一重点：统一TXT/EPUB文本节点派生、编辑EPUB资源保真，继续完整性反馈和上述恢复边界。

技术基线：按任务持久nonce+URI+字节/SHA而不是仅文件名做归属。[Android官方MediaColumns文档](https://developer.android.com/reference/android/provider/MediaStore.MediaColumns#OWNER_PACKAGE_NAME)说明owner字段可能未知且受包可见性影响；当前代码对未知归属明确拒绝，不通过增加全局包/存储权限来绕过。
