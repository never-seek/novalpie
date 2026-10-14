# Beta7 写回执、签到与实际计数修复

日期：2026-10-05；Owner Codex；持续目标未完成。当前工作区全部既有修复保留；未commit/push/tag/替换GitHub公开包。

## 1. 四个已确认问题落实

### P01/V11：资料两步保存不能吞掉第二步拒绝

- WebsiteProfileRepository第一步确认资料PATCH；只有已确认后才提交签到设置。第二步显式false与HTTP/网络异常不再runCatching后无条件return success。
- 使用ProfileSavePartialFailure区分“基础资料已保存”和“签到设置拒绝/未确认”，携带已确认基础资料；ViewModel显示部分保存，保留签到草稿、后续名称/简介编辑，不能提示全部已保存或清掉新输入。
- 可空设置不再用true/false默认值重置用户没提交的另一字段；当前正常UI仍提交明确两项，局部API调用只发送有值项。
- 外层success:false不能被内层data/result缺success掩盖；first PATCH拒绝不会提交second PATCH。没有自动重发写入或臆造client_request_id。

### S13/V12：刷新/重建不能重复自动签到

- 按稳定accountId+站点Asia/Shanghai日期保存Attempted/Rejected/Confirmed回执，无Cookie/Token/profile内容。
- 自动写之前持久Attempted；签到成功/明确“今日已签到”保存Confirmed。同日重复刷新、GET记录失败、feature重建或process重新加载回执不能自动再POST；跨账号和跨日独立。
- unknown网络回执与明确拒绝不再盲目自动重发；保留用户明确手动重试和下一日自动签到。未知/拒绝不会假冒已签到、加积分或伪造记录。
- 缺预加载hero时手动签到仍可先resolve当前账号再写，保留原功能；不靠改掉原ProfileViewModelTest让初始checkin失效。
- 读取损坏receipt按未知请求处理、持久化失败不POST；“already rate limited”“本月已签到3天”等不当作今天成功。站点日期不依赖手机时区/午夜前后一致性。

### S06/V10：有价值/踩/欢乐用自己的桶

- ChapterComment和ForumComment分别保留helpfulCount/notHelpfulCount/funnyCount；原API likeCount/dislikeCount/reactionCount聚合含义保留，旧兼容别名/旧测试不改。
- 原生论坛评论、书评/章评公共action row与metric helper改用display*属性；显式0是有效值，不被aggregate覆盖，确实缺桶时仍fallback原计数。
- 冲突样本legacy16/1/7与specific13/0/2共存；当前实际原生书评UI展示13/0/2，不把原legacy字段重新定义成另一统计来“让测试绿”。嵌套回复以及论坛评论同样覆盖。

### S08/V13：总页数不是加载到第几页

- HomeState分别保存favoriteTotalPages/historyTotalPages；直接source total_pages优先，缺失但有source total/pageSize时正确ceil推导；无source总数时显示—，不把page1当总页数1。
- 当前page字段继续真实用于后续分页，loadMore/返回刷新不被总页数篡改；筛选变化清掉旧页数，历史页数不覆盖收藏总页数。
- 实际UI统一libraryOverviewForState；受控设备证明currentPage1、totalPages5且标签显示5；当前真实账号收藏67/20一页，首页显示总页数4，之前显示1的问题已改。

## 2. 失败驱动与旧测试不回避

- 原审计已分别确认上述四项。新增first-red9用例中6红，但两项未加Robolectric导致SystemClock测试环境错误，不把所有6个红灯冒报为6个App缺陷。添加适当运行器后实调生产协议/状态；原错误日志保留。
- 第一次保存/签到修复全量发现手动签到未预加载profile的新回归，实际实现改为先resolve账号；原两条ProfileViewModelTest期望保留通过。
- 第一次计数全量发现原NovalPieApiTest要求legacy likeCount=4而specific helpfulCount=2。这是原有聚合协议语义，不能把旧断言改成2掩盖改变。生产改为两组字段/独立display getter，原API测试完全原样通过，原生UI却显示正确specific桶。
- 原外置S06 fixture仍断言legacy likeCount应该13，因此继续红；S08 fixture在测试外自行调用libraryOverview(pageCount=state.favoritesPage)，因此仍红。**未删除或改这两条，分类单独解释，并用生产mapping和真实UI补更强证据。** 不是不修问题、只改测试或在源码假写一个未消费新字段。

## 3. 当前测试与安装

| 项目 | 当前结果 |
|---|---|
| 本轮新增默认回归 | 27项：12签到/草稿/回执、5资料协议、3评论桶、3真实存储/站点日、4收藏分页mapping |
| 默认全量 | **1431 tests /198 suites，0 failures/errors/skipped**；unit-final-results / full-gate-final.log，exit0 |
| 构建/Lint | Debug/AndroidTest/Lint全门禁完成；0errors/18warnings/1information，没有baseline/禁lint/改旧期望 |
| MuMu Debug | **OK(17 tests)**，50.091秒；真实首页5页mapping、root UI评论13/0/2、Android真实SharedPreferences重建抑制自动POST、部分保存保留草稿；加编辑EPUB资源、导出文本、发布事务、导航/触摸和中文PCM/连续朗读回归 |
| 外置75项 | **63通过、12红、0 errors**；原资料部分保存和签到重复写案例绿；12=4确认案例+2空回执风险+6旧驱动/模型断言不适配，不能说全部问题只剩12项或12个独立Bug |
| R8 Beta | external-beta-gate.log的assembleBeta完成；组合exit1来自外置红灯，不谎称总门禁全绿 |
| 最终安装包 | SHA256 `4677904c3645445c00343774cfc244dd5c8507ad680551ab8a629795828c7b66`，3,374,533B；MuMu base.apk SHA一致 |
| 身份/签名 | com.novalpie.app.debug /2026100101 /2.0.0-native-beta7，非debug，原签名/包身份覆盖兼容 |
| 保留状态 | install-r成功，冷启动约2秒，登录/收藏67/4列保留；当前真实首页总页数4 |
| 一致性 | 最终app/src与source-final-hashes一致、git diff --check clean；无运行测试下载/TTS遗留 |

MuMu127.0.0.1:16384，Android15，900×1600/density240/字体1.0/60Hz。17白盒用例在同源码Debug；R8仅安装/hash/签名/启动/首页抽查，不等于R8全部白盒或真实/OEM完整验收。

测试网站写入全部MockWebServer或FakeRepository，Android持久测试只用synthetic account999999990的未来日期，finally精准删自己的key；没有为了测试发真实签到、资料PATCH、评论、付费下载或他人资产。保留原登录、用户下载、规则和本地数据。

截图：back-to-A.png/recreated-A.png已查看，实际显示赞13、踩0、表情2和未发草稿；debug-home.png/optimized-home.png当前真实67收藏显示总页数4；不造假“服务器实时全部评论统计无冲突”的线上结论。

## 4. 文件

生产新增：D:\NovalPie\native-android\app\src\main\java\com\novalpie\nativeapp\feature\profile\CheckinReceiptStore.kt。

生产修改（同main/java/com/novalpie/nativeapp目录）：feature/profile/ProfileRepository.kt、ProfileViewModel.kt；feature/library/LibraryContentViewModel.kt；data/NovalPieApi.kt；model/Models.kt；ui/NovalPieViewModel.kt、NovalPieApp.kt、LibraryPresentation.kt、ReaderPresentation.kt。

新增测试：ProfileSaveReceiptRegressionTest.kt、ProfileCheckinReceiptRegressionTest.kt、StoredCheckinReceiptRegressionTest.kt、CommentReactionBucketRegressionTest.kt、LibraryTotalPagesRegressionTest.kt；LibraryTotalPagesDeviceTest.kt、ProfileReceiptDeviceTest.kt。原BookNavigationIsolationDeviceTest新增specific桶UI断言，不删原返回/recreate断言。

## 5. 尚需完成，完整目标没有缩小

1. 当前4个原审计确认案例：下载metadata未知仍不披露、校图不一致任务反馈、阅读器亮度能力、badge常态与hover CSS范围。2个消息/工作区未知回执需真实合同/读回，不能一刀切拒绝合法204或默认当成功。
2. 这些数量仅是旧75回归的分类，**全部Beta7/58项/后来反馈还要逐项验收**。书评/章评/管理员分页及管理资源上传（原观察用例证明的能力缺口）、正式角色/权限/首次登录、社区反馈全分页/新355500第44章图对照不因单测绿而消失。
3. 本轮已签到防重的真实站点各语言/code响应合同、签到日server-time跨午夜、未知结果后的手动确认UI、账号身份不一致/切换与迟到回执、后台/进程重建全链需要继续扩展。持久receipt小写盘当前同步；应进一步验证启动/低存储性能与避免不必要同日重复commit，而不是在纯文本滚动路径引入I/O。
4. 第二步未知后UI已说明部分保存，仍需要更细粒度“只重试签到设置”的受控动作/真实readback，不盲重发整个资料PATCH。未提交另一setting用null省略的实际站点版本兼容仍须真实合同验证；正常UI两个明确设置没有改协议。
5. 之前明确记录的EPUB资源安全GC/旧存档关联/复杂格式/初始CRC、SAF完整生成与发布事务、历史下载无journal受控恢复、超大单章流式规则语义、大书与空间满、最终R8升级/外部阅读器/全适配/性能/TTS/音量矩阵全部继续。不能把现在4旧确认说成只剩4个App问题。
6. 已登录Edge当前连接前提尚未恢复，原登录态未退出或导出；网页前后/全部控件/最新反馈审计未完成。没有基于旧截图充当当前线上证据。

Next owner: Codex。继续下载完整性反馈和徽章/亮度，然后全部新增边界及完整验收；保留现有外观/五导航/所有原生增强，不交Gemini重做视觉。当前未替换GitHub包，不能宣称最终成品全量通过。
