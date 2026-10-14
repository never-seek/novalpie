# Beta7 导航/写回执/拒绝缓存修复

日期：2026-10-05；Owner：Codex。持续目标未完成。

## 本轮落实

1. 修复Book A→B→Back只还原route、不还原详情身份的问题。BookDetailViewModel保存账号所有的书籍快照/草稿/回复目标/目录查询；重返回时立刻恢复正确书并独立刷新，迟到B或旧A读取不能覆盖。重列表缓存最多12本，未发送草稿独立保留。账号环境变化清理私有状态。
2. Book写操作不再绑定容易变化的详情读取revision。按账号epoch、书籍、操作lane和ticket管理：进入B不吞掉A的已提交回执，不让A的回执清B草稿；相同任务只能完成一次，不因返回就重新POST。收藏、新章请求也按书归属。
3. 评论提交冻结原始draft和回复目标，成功只能清同一份已提交composer；后续编辑、切回复目标或取消回复的状态保留。Compose回调显式携带原书ID，过时A输入/发送/点赞/收藏回调不能操作B。
4. 书籍tabs、滚动、目录顺序、书评编辑框展开和已展开回复按账号/书籍保存；Back及Activity recreate恢复。没有重做主题、卡片或导航。
5. revoke改成受控I/O：先内存拒绝，marker为目录等损坏形状仍当拒绝；marker不可写时用独立持久索引；必要时只重命名本条目的恢复字节，不删除内容。所有持久路线失败则保持本进程拒绝并给明确提示，不抛未捕获异常。
6. 权限拒绝先发布正文状态、停止本章TTS再落盘；迟到首章拒绝只移除它自己的缓存预览，保留已授权后继与章评草稿；旧请求403不得否定较新的授权读取。没有删除同账户503离线能力，也没有清登录或用户下载。

## 回归证据

- 先新增9个用例跑旧实现，8失败、1账号隔离对照通过；红灯见red-tests.log/red-results。之后补11条边界，共20新增仓库用例，没有修改旧测试期望或删除功能。
- 当前默认全量1353 tests /189 suites，0失败/错误/跳过；composer-final-gate.log（Debug和AndroidTest打包exit0），unit-final-results/与unit-final-counts.json。
- 外置75项原样重新验证：56通过、19失败、0 errors；其中新增审计的返回串书、拒绝持久化未捕获、新草稿被旧回执清空3项全部转绿。余19不能当19个独立Bug：仍有旧驱动/输入不适配和合同风险；本轮不关闭其余13确认缺陷/2空回执风险。
- 最新Debug MuMu：device-tests-final.log明确OK(18 tests)，含生产root书籍返回/真实评论输入/Activity重建，测量分页、富文本、阅读器触摸、中文PCM+连续朗读、后台/熄屏媒体控制。根测试用合成Repository，不发真实评论/投票/付费下载。先前两个找不到写评论的驱动失败保留；实际入口先需点“写书评”，最终按真实路径验证，断言没有放松。
- back-to-A.png与recreated-A.png已人工查看：A书名正确，评论tab仍选中，未发送A草稿、展开编辑框保留，重建前后显示一致。保持600dp/字体1.0/当前主题；不推广成全尺寸矩阵。
- 收藏仍67、四列与登录保留；截图preserved-home.png。测试结束没有继续播放测试TTS。

## 本轮文件

生产：
- D:\NovalPie\native-android\app\src\main\java\com\novalpie\nativeapp\feature\books\BookDetailViewModel.kt
- D:\NovalPie\native-android\app\src\main\java\com\novalpie\nativeapp\ui\BookDetailPresentation.kt
- D:\NovalPie\native-android\app\src\main\java\com\novalpie\nativeapp\ui\NovalPieViewModel.kt
- D:\NovalPie\native-android\app\src\main\java\com\novalpie\nativeapp\ui\NovalPieApp.kt
- D:\NovalPie\native-android\app\src\main\java\com\novalpie\nativeapp\ui\ReaderPresentation.kt
- D:\NovalPie\native-android\app\src\main\java\com\novalpie\nativeapp\data\ReaderChapterCacheStore.kt
- D:\NovalPie\native-android\app\src\main\java\com\novalpie\nativeapp\core\AppContainer.kt

新增测试：RootBookIsolationRegressionTest.kt、BookDetailNavigationRegressionTest.kt、ReaderCacheRevocationIoRegressionTest.kt、BookNavigationIsolationDeviceTest.kt。

## 当前验证边界

最终R8 Beta已覆盖安装：SHA256 `97d175abdd42af5939fd2667ccdb12621d66c855d1f5c03efa0c635c68604b4a`，3,341,773B；base.apk实机SHA一致。沿用同包/同签名，versionCode2026100101、versionName2.0.0-native-beta7，DEBUGGABLE=false；Dex确实含新的拒绝索引/归属保护。冷启动约2.6秒，实际首页登录/收藏67/四列保留。

最终生产源码对应Lint完成：0errors/18warnings/1information。`remaining-probes-beta-lint.log`整体exit1是75项外置审计还有19红灯；使用--continue后assembleBeta与lintDebug分别完成，不宣称“总门禁全绿”。18个白盒设备用例是在同源码Debug包上运行；最终优化包只作覆盖安装/签名/hash/启动/首页数据保留检查，不能冒充R8全白盒矩阵。

尝试补优化包实际书籍deep-link时，工具策略阻止了命令创建，没有执行，也没有改用其他方式绕过。因此这项线上优化包书籍流保持未验；Debug真实root合成导航和再创建证据独立成立。截图optimized-home.png；详细版本、hash和任务状态见final-verification.json。

无git提交/推送/tag/发布，保留全部以前未提交修复。Next owner仍Codex，不交Gemini重做外观。

下一步：EPUB编辑资源保真、下载发布幂等窗口、TXT/EPUB文本节点派生及完整性状态，然后其他已确认缺陷和完整矩阵。全过程目标“已知bug修好、已有功能不缺失”不缩减。历史58项覆盖和全Beta7需求仍继续逐项对账。
