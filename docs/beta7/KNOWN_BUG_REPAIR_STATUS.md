# Beta7已知Bug修复台账（持续任务）

## 2026-10-16 最新回归入口（优先于下列历史“当前”字样）

当前R2修订为`2026101602`，收藏夹快捷创建/自选去向、ZIP等级、历史同步状态与更新章数已整合。1492单测、18工具测试、MuMu70个不同设备用例最终通过；完整Lint0错误/19警告/1信息。具体失败驱动说明、线上/受控边界和剩余风险统一见[本轮全模块回归](20261016-full-functional-verification.md)。最终R8安装与GitHub资产必须另有收据才算发布；下方保留历史，不把旧包证据重新认证为当前包。


当前目标：**修好已知bug，并且已实现功能不能缺失。**

更新时间：2026-10-05；工程Owner：Codex。工作开始于2026-10-04，证据目录因此沿用该日期。

**目标仍未完成。不得将下列第一批进展当作Beta7全量验收、优化发布包通过或允许发布。**

## 固定基线与实际进展

- 基线HEAD：`b4616be2f7f2852c6a8bf86127a804c6a335c8fd`；本轮源码修改与新测试仍在工作区，未commit/push/tag/Release。不得reset或用旧APK覆盖这些修复。
- 原审计：`D:\NovalPie\agent-bridge\artifacts\beta7-problem-verify-20261004\VERIFICATION-REPORT.md`，另有58项完整覆盖表；这些是修复前证据，保留不覆盖。
- 新增36项仓库回归；**默认全量1300 tests /180 suites，0失败/错误/跳过**。其中Root 13、cache ownership 8、download integrity 8、image compression 4、atomic file 3。
- 当前源码 `testDebugUnitTest + assembleDebug + lintDebug` 完整命令exit0；Lint为0 errors /17 warnings /1 information。没有以修改旧测试期望、删测试或删功能的方式取得通过。
- 原72项外置审计全部再次运行：仓库1300仍0失败；外置从43个失败断言降至**26个**，总1372项、exit1。断言数不是独立Bug数；仍有明确未修和合同风险，不能说全绿。
- 本轮Debug验证包24,484,914B，SHA256 `f5c544557c2cfbae3079733da5fc7da57cf4280fef1c6598ec7002341d035030`；MuMu `127.0.0.1:16384` install-r成功，设备base.apk hash一致。保留登录、收藏67条与4列设置；论坛五个横轴入口可达。没有执行真实扣分下载、发帖/评论或他人资产修改。
- 此包仅工程验证，仍DEBUGGABLE；未将它上传GitHub，也未声称全部实机/角色/OEM/R8矩阵完成。

完整证据：`D:\NovalPie\agent-bridge\artifacts\beta7-repair-20261004`，含full-gate-final.log、remaining-probes.log、results/、remaining-counts.json、remaining-cases.json、安装hash及截图。

## 本批已实现并有回归证据

| 项目 | 实现与验证 | 尚需的完整验收 |
|---|---|---|
| S01账户缓存隔离 | 缓存按稳定账号/独立匿名/不含明文凭据的opaque session空间分离；请求开始捕获scope；账号切换重新加载阅读器；403等明确拒绝不直接回退缓存，拒绝标记可在新授权保存后恢复。A→B、退出→访客、同账号续token、晚A写入B、拒绝/恢复和503离线正向回归通过。 | 真正多账户/各类403（权限、签名、CF等）的兼容细分；未归属旧缓存保留但不能自动给下一登录者，需要补受控恢复说明/流程；拒绝标记磁盘写失败不得使阅读器崩溃需故障注入。 |
| D09缓存原图地址 | originalSrc完整存/取；透明/动态图的独立原图URL不再丢失。 | 旧缓存没有原图字段不能凭空恢复；原图长按与离线真实图仍需完整矩阵。 |
| S03站内链接返回栈 | App内部HTTP(S)站内链接保留起点；外部Intent与native scheme仍明确重置到对应根入口；支持www别名。Root论坛→书→Back及外部启动既有回归通过。 | 真正触摸链接和多层Book A→B返回后的详情/草稿身份还要补回归，不能只有导航route正确而内容仍是B。 |
| S04a书评busy | 已发送任务在同书子页面完成，能清busy/保留失败草稿；成功刷新在返回时补做；已换书/账号的旧结果不能污染当前书。 | 跨不同书再返回、发送中继续编辑新草稿、未知写结果需进一步审查。 |
| S06a错误赞提示 | success:false且无message不再显示“已同步”；普通成功/失败刷新仍保留。 | 具体helpful/down/funny计数优先级S06b仍未修。 |
| S04b章节旧回包 | 单独编辑请求revision与环境身份；新草稿/图片占位编辑会失效旧读取，正常最新读取仍通过。 | 管理区其余保存、排序、批量动作的乱序/权限全矩阵仍需。 |
| S10a未保存取消 | 取消/返回先确认放弃；可继续编辑保留内容或明确放弃；加载取消使旧回包失效。明确拒绝保存保留草稿，不伪称已保存。UI已接入确认动作，Root三条相关回归通过。 | 对应Compose触摸/小屏/键盘与实际受控管理页面。 |
| B05末章状态 | 只对已加载的相邻章去重，不再把“没有后继”提前return；保留防跳下下章意图与末章刷新确认。 | 连续阅读真实慢网、初始缓存后晚回包不吞新追加章、底部控件触摸不被正文手势吞掉、分页EP49/50往返。 |
| D01错误图片体 | 已识别图片签名仍优先；JSON和明确非图片/错误页拒绝写入；原图不重编码。 | 更多媒体格式及完整任务失败/明确容错结果。 |
| D02错误正文 | 有界前缀校验拒绝200 HTML/JSON错误体，不生成成功TXT；UTF8 BOM与合法源HTML字节保持。 | 206/无章头/其他异常回执、章节完整性；不能泛称已识别所有服务器错误。 |
| D07、S11a源恢复 | 损坏source.complete可重取且复用授权；正文/receipt用可回滚owned atomic事务；中断part只清本任务临时文件；旧有效数据与备份可恢复。 | 实机强杀/磁盘满/授权过期/最终保存故障；S11b发布成功但receipt未存的重复成品仍未修。 |
| D06答复窗口 | runner先注册waiter再发布可见prompt；control跨线程同步，重复resolve不创建新待答。 | 旧外置“resolve发生在首次request之前”的控制器断言仍失败。须补真正runner/UI立即回应及重复/跨prompt的闭环，不偷偷把旧红灯删掉或强行把未经请求的答案用于下一轮。 |
| D08诊断URL | streamAsset异常label去query/fragment/URL凭据，实际下载URL不改；合成签名异常测试通过。 | 下游日志/缺图占位及服务器错误正文反射URL还需审查，不能宣称所有导出诊断均已脱敏。 |
| 图片压缩 | 静态不透明图片仍能按质量缩放/压缩；透明PNG和动态WebP保持原始字节；APNG识别保留动画；关闭压缩JPG原字节不变。3项外置旧失败已转绿。 | 实机完整EPUB、GIF/WebP/透明层、不同ABI/最低系统。可选压缩不删除、不改成整页网站下载。 |
| ZIP0 | 真实level0不压缩文本；纯writer无参数默认保留历史Deflate6行为；App明确传用户选择的0..9，原始图片仍STORE。旧无损压缩测试和新0测试同时通过。 | 实机格式选择/设置与外部阅读器打开。ZIP压缩无损，不能混称为图片质量压缩。 |

## 必须继续修的原问题（未因本批绿色基线而关闭）

- T01：TTS重复段的稳定blockId/offset定位，分页/滚动/高亮一起修；完成设备连续发声、跨章首句、暂停恢复、焦点/耳机/后台。
- R01–R05、S07：统一富文本组合解析与预览；balanced fold、黑幕包书卡/样式、code/escape opaque scope、隐藏范围URL不能跑外部预览；发帖预览不能露格式代码。论坛/书评/章评/动态都复验。
- D03–D05：阅读、TTS、TXT/EPUB同一文本节点派生；不改HTML属性，不让替换输出内部token重复图，无章头不能静默跳规则。
- S11b、S12、插图校对：发布与持久结果窗口防重复、metadata未知失败披露、插图校对状态可见；保留正常重试/主动容错打包与8路/自定义并发。
- E01：编辑器EPUB资源和元数据parse→export完整；原文件上传已证保留，不得回退/删除原生上传。
- P01、P02：资料与签到设置部分回执，消息/工作区未知回执需真实合同/读回证据；不盲加服务器不支持的幂等字段。
- S02、S10b：书评/章评/管理分页及原生资源上传；保留所有已实现管理功能和权限守卫。
- S06b：计数具体桶优先，不把聚合赞/表情映射成有价值/欢乐。
- S08、S09：收藏总页数、阅读器亮度（进入/退出恢复系统值）。
- 徽章CSS常态/hover范围解析与真实素材适配；自动签到GET失败/已签回执后不重复POST。
- 原58项、Beta7补充需求中的所有其他线上/权限/适配/性能/升级/优化包证据缺口，仍按原覆盖表逐项补齐，不能把单测等同全面实机。

## 本批发现的新输入/待复现，不丢掉

1. 论坛2026/10/4用户qwertyui《epub 下载不了》摘要：`https://novalpie.cc/book/355500`，反馈“44章图片不符”。已记录当前模拟器feed截图；详情在本次短等待内仍Loading，未拿到最新评论，不冒称已修好。下一步应读取详情并对源文件/阅读API第44章逐图核对，不能简单跳过校验或伪造图片。
2. 多层Book A→Book B→Back的route和实际详情身份、草稿必须一致；现goBack仅补pending刷新，源码仍需完整根ViewModel复现。
3. 发送过程中用户编辑新草稿后，旧成功回执不能把新草稿清空；不同书的在途写回执/回读不应被简单忽略后让用户重复提交。
4. 初始缓存正文已展示、下一章已追加后，首章迟到回包可能用最初window覆盖新窗口；需延迟真实root fixture，不能用只测试ReaderScreen的正向续章代替。
5. 拒绝缓存标记的存储失败与跨Cookie会话身份要补受控故障/协议回归；安全修复不能靠删掉同账号离线能力或无提示丢旧数据。

## 保留的不变量

- 现有外观、五导航、原生EPUB/TXT、原图模式/可选压缩、8路/自定义并发及有效调度限流、公共规则按书启停/逐条屏蔽/个人优先、导出规则快照。
- 2/3/4列、名称作者与进度对齐、头像框外层、真实badge/动画、作者/tag搜索、系统TTS、音量键开关与无动画。
- 不恢复正文复制/全选、轮盘、无效网页正文键；不加外部爬虫/付费云TTS；不将已有原生业务降级整页WebView。
- 保留Beta6身份/签名及用户已有本地数据；不清账号、不导出Cookie/Token/密码/私信，不删除用户下载；未归属旧文件保留，不静默给新账号。
- 不以子agent宣称/旧截图/旧APK充当本批安装或全量证据，不发布未解决阻断问题的包。

下一轮Owner仍Codex。优先TTS/阅读器真实续章与触摸、文本替换/富文本，再完成下载一致性和其余模块。**持续目标完整保留，非“小批通过即完成”。**

## 2026-10-05 阅读器/TTS第二批进展
- TTS重复句按chapter/block/offset定位，分页跟随/高亮正确区分重复段；等文本文档仍更新位置和最新后继。实际设备可从第二处同句返回第一处。
- 独立发布目录/章评/收藏；慢首章回包不吞续入章节、不清章评草稿、不把已读进度回写首章；缓存来源改变复用解析。
- Final-pass正文手势尊重子控件，中央章末按钮真实触摸不误开菜单；单击菜单与加载不弹菜单保持。
- 15新增回归：1315/183全绿；完整Debug/AndroidTest/Lint exit0。MuMu同构建11设备用例全绿，含真实中文PCM与两段播放；登录保留，未发布GitHub。
- 当前设备验证包1314b1f8e3771d9cb166e5c3c6fa64a927e1e1185da9bb7690c8cb31dd73ba99，24,475,499B。仍Debug，不能当最终优化包验收。
- 证据/失败驱动区别见D:\NovalPie\agent-bridge\artifacts\beta7-repair-reader-20261005\READER-REPAIR-REPORT.md。
- T01基本调用链已有本轮证据，完整TTS生命周期矩阵仍缺；其它剩余26旧断言/合同风险与全部未验项继续，不宣称目标完成。Next owner: Codex。
## 2026-10-05 富文本第三批进展
- 保护代码/转义/黑幕/折叠作用域，balanced fold/details；内层close不泄漏外层尾文，代码不活化书卡/图片，收起内容不外抽URL预览。
- 复杂黑幕揭示后保留真书卡/链接等；发帖预览复用正式富文本且站内点击原生。新增raw richExcerpt，保留旧纯excerpt协议，不改旧测试期望。
- 18新增回归，默认1333/186全绿；最后版本Debug/AndroidTest构建通过，MuMu8集成全绿，登录保留。最后混合黑幕改动后未新跑Lint（此前版本Lint通过），不冒称最终版新Lint。
- 旧72复验19失败（组合1405 exit1），3类旧fixture时效需按实际调用链判断；其它未修项继续，目标不缩减、未发布GitHub。
- 报告D:\NovalPie\agent-bridge\artifacts\beta7-repair-richtext-20261005\RICHTEXT-REPAIR-REPORT.md。Next owner: Codex。

## 2026-10-05 导航/书籍写回执/拒绝缓存第四批修复
- 本轮修复：Book A→B→Back实际详情/草稿/目录查询恢复；账号/书籍保存tabs/列表/书评展开并支持Activity recreate；迟到UI回调带原书ID。
- 书评/反应/收藏/新章写操作用独立epoch/book/lane/ticket，不随读取revision变化丢回执、不清另一书草稿、不盲重发。提交snapshot只清已发送的同一draft/回复目标，新回复目标保留。
- cache revoke I/O不穿透；marker损坏也拒绝，独立持久索引与自有恢复字节重命名兜底。先发布拒绝/停止本章TTS；晚首章403保留已授权后继和章评草稿；旧403不否定新的授权缓存。
- 9新增先红8，另11边界，共20新回归；默认1353 tests/189 suites全绿；同源码Debug MuMu18通过（中文PCM/两段、后台/熄屏、分页/触摸/富文本及真实root导航/Activity重建）。旧75外置复验56绿19红，3新增审计已绿；余13确认缺陷/2回执风险/4旧驱动仍按分类继续，不能把19红算19独立bug或忽略剩余。
- 最终R8 Beta 97d175abdd42af5939fd2667ccdb12621d66c855d1f5c03efa0c635c68604b4a，3,341,773B；MuMu install-r、同签名、非debug、设备hash相同，登录/收藏67/4列保留。最终Lint0errors18warnings1info；含外置红灯的组合命令exit1，不冒称全部门禁绿。
- 优化包补实际书籍deep-link验证被工具策略阻止，未执行未绕过；不能把Debug白盒当R8全矩阵。没有清用户数据/下载、真实评论/付费下载或他人资产；没有commit/push/tag/替换GitHub，公开包仍旧资产。
- 全报告：D:\NovalPie\agent-bridge\artifacts\beta7-repair-navigation-cache-20261005\NAVIGATION-CACHE-REPAIR-REPORT.md；仓库同步docs/beta7/20261005-navigation-cache-repair.md。
- Next owner: Codex；优先继续EPUB编辑资源/下载发布事务/文本节点替换及完整性反馈。全目标未完成，不交Gemini重做外观。

## 2026-10-05 原生下载发布第五批修复（Owner Codex；目标未完成）
- 发布之前durable nonce/owner/bytes/SHA意图、绑定URI、验证真实字节、commit/receipt，Runner独立完成收据。新事务在Completed写盘失败或清理work后可恢复同URI，不重新下载/授权/publish；不能以普通文件名认领/覆盖同名用户文件。
- MediaStore真实验证修了leading-dot被系统改成_.的问题、pending row未建backing file的ENOENT、commit回执I/O失败误入local重复保存。原生App-local fallback、原图字节、并发/暂停/取消/重试保留；23/28分支真实API guard，未关闭Lint。
- 6用例先红6；共16新增，默认1369/190全绿；最后Debug/AndroidTest/Lint exit0（0errors18warnings1info）。MuMu同源码Debug11用例全绿，MediaStore五故障边界确实authority=media/同URI/1row/原始字节，不拿fallback冒充成功。
- 外置75复验57绿18红：12确认缺陷/2空回执风险/4旧驱动继续；S11b/V04当前新事务案例绿。旧无journal历史未知窗口、完整崩溃/大书/空间/OEM/R8白盒及收据生命周期/更清楚UI仍未全认证，不能说下载全部完成。
- 最终R8 Beta 5e63806fb3971a3bff685e369f6cf3ee9388fc96cb8169da719088df47d67735，3,341,771B；MuMu同签名install-r，非debug，设备hash同，登录/收藏67/4列保留。无真实评论/扣积分/他人资产操作，无commit/push/tag/替换GitHub。
- 报告D:\NovalPie\agent-bridge\artifacts\beta7-repair-publication-20261005\PUBLICATION-REPAIR-REPORT.md；仓库docs/beta7/20261005-download-publication-repair.md。测试自有pending/files已清，不删用户下载；shell content工具有查询后segfault单独记录，不假报App崩溃或无工具错误。
- Next owner: Codex；继续TXT/EPUB文本节点派生、编辑EPUB资源与其余全部缺陷/验收矩阵，不交Gemini重做视觉。

## 2026-10-05 导出文本节点第六批修复（Owner Codex；目标未完成）
- D03/V05 TXT attrs/URL不改；D04/V06源图直接生成结构，replacement内token不复制img；D05/V07无章头应用规则但不造title。共同节点扫描先保护attrs/script/comment/image/URL再替换；EPUB在格式解析前保护；TXT保留plain字面字符与真实换行，旧结构测试没有改期望。
- 前缀64KiB后spool到任务work，取消/异常只清自有part，空源不造章、正文前导空行保留；headerless/single-huge-chapter最终String资源风险仍未关闭，不以分块改regex或跳规则掩盖。未发布旧变换包pipeline5重建、旧已应用/含图legacy禁CRC假认证，授权/source/assets保留。
- 24新回归，默认1393 tests/191 suites全绿；最终Debug/AndroidTest/Lint完成（0errors18warnings1info）。MuMu同源码Debug19全绿，真实Android原文/替换EPUB+TXT透明PNG字节/SHA/CRC/XML和DOM一次图，加保存/导航/触摸/分页/TTS回归。原驱动/换行失败均保留并说明，没有冒充线上/大书验收。
- 外置75→60绿15红：D03/D04/D05三个旧原例绿；余9确认缺陷、2空回执风险、4旧驱动按分类继续，全量目标未完成。
- 最新R8 Beta eb6a649d1aa6db197a88adf40b06cbe89e00a2b6433e0a41296c5054abcd6ca8，3,358,157B；MuMu install-r同签名非debug、设备hash同，登录/67收藏/4列保留；无commit/push/tag/更新GitHub或真实扣分/评论/他人资产。
- 报告D:\NovalPie\agent-bridge\artifacts\beta7-repair-export-text-20261005\EXPORT-TEXT-REPAIR-REPORT.md；仓库docs/beta7/20261005-export-text-node-repair.md。
- Next owner: Codex，继续编辑器EPUB资源、metadata/校图完整性及全部回执/展示/资源/历史迁移/完整矩阵；不交Gemini改主题。

## 2026-10-05 编辑器EPUB资源第七批修复（Owner Codex；目标未完成）
- E01/V03 parse→writer与编辑器导入/prepareUpload保留磁盘原包、media marker/raw chapter provenance、原图/透明/动态资源、CSS/fonts/cover/meta。纯图章保留；未改raw body样式保留，改文保留media位置；marker重建+存档reload带bundle，缺失/损坏SHA/node明确拒绝。无图片ByteArray塞Compose、无外部抓源/协议替换/删原生编辑。
- 4新测试先红4；共11新仓库回归，默认1404/193全绿，Debug/AndroidTest/Lint exit0（0errors18warnings1info）。MuMuDebug11全绿：有效透明PNG字节/1资源2DOM引用、实际Repository.prepareUpload与ArchiveStore恢复/原EpubImport/存档/导出文本/事务/导航。edited.epub主机CRC/spine/XML复查通过，全合成无网站上传/扣分。
- 外置75复验61绿14红；E01旧原API例绿；8确认缺陷/2空回执风险/4旧驱动继续。新资源生命周期GC/旧存档关联/复杂格式与初始CRC/SAF生成发布事务边界仍继续，不因基础资源修复即宣称编辑器全完成。
- 最终R8 Beta 3aa6341bcc95d87dd9e5659869ca19002bc3134867a98f6952fa1597b3fde5b7，3,374,540B；MuMuinstall-r同签名非debug、设备hash相同，登录/67收藏/4列保留。没有commit/push/tag/替换GitHub、清用户下载或存档。
- 完整报告D:\NovalPie\agent-bridge\artifacts\beta7-repair-editor-epub-20261005\EDITOR-EPUB-REPAIR-REPORT.md；仓库docs/beta7/20261005-editor-epub-resource-repair.md。Next owner Codex，不交Gemini改外观；全目标未完成。

## 2026-10-05 回执/签到/统计第八批修复（Owner Codex；全目标未完成）
- P01资料与签到两步保存明确partial，settings拒绝/unknown不吞，后续草稿保留，外层false不被data掩盖；空设置不默认重置。S13账号+China站点日持久Attempted/Rejected/Confirmed，先登记后POST；成功/已签same-day防refresh/recreate重发，unknown/rejected不自动盲重试，跨日/账号独立，manual未预加载hero仍resolve账号后可用。
- S06保留legacy aggregate字段，新增typed helpful/down/funny display getter，论坛/书评/章评实际UI消费specific桶，0不覆盖；旧API likeCount=4测试原样保留不改成2。S08收藏/历史totalPages独立，真实UI映射不拿currentPage，总页数据未知显示—，sourceTotal推导，筛选/历史隔离；当前67收藏总页数4。
- 27新增，默认1431/198全绿，Debug/AndroidTest/Lint exit0（0errors18warnings1info）；MuMuDebug17全绿含实际首页5总页而page1、书评13/0/2、真实stored prefs未来synthetic账号防重和partial draft，加原EPUB/导出/事务/导航/触摸/TTS。没有真实签到/资料写入/评论/付费下载。
- 外置75→63绿12红：P01/S13原案例绿；S06/S08旧fixture断言legacy字段/外部传page index仍红且未删除，按当前实际UI强证据分类不适配。4原确认/2unknown receipt风险/6旧驱动继续；数量不是完整需求范围，所有新边界/分页/大书/角色/R8矩阵仍未完成。
- 最新R8 Beta 4677904c3645445c00343774cfc244dd5c8507ad680551ab8a629795828c7b66，3,374,533B；MuMuinstall-r同签名非debug、设备hash同、登录/67/4列保留；无commit/push/tag/替换GitHub。
- 报告D:\NovalPie\agent-bridge\artifacts\beta7-repair-receipts-stats-20261005\RECEIPTS-STATS-REPAIR-REPORT.md；仓库docs/beta7/20261005-receipts-and-statistics-repair.md。Next owner Codex；继续下载完整性/徽章/亮度及已记录所有边界，不交Gemini改主题。

## 2026-10-08 第九/十批：下载完整性、GitHub历史搜索/重翻/亮度/徽章
- Ethan当前优先反馈已落地：搜索清保存快照不清当前规则/tags/结果，新增明确保存条件按钮及状态反馈；清标签只清tags；R8实际选中/屏蔽、清缓存、保存、冷启动恢复已证，受控选择已恢复原状态。
- 原生重翻详情菜单与阅读器当前章入口、真实当前网站v2协议/本人API资格、章节范围严校、迟到目录当前章定位、独立身份、防重复/未知回执已补。Mock协议+设备受控提交+R8真实91章目录对话框通过；未真正排队调用外部模型，不能说翻译全流程线上已证。
- 第九批metadata/正文图源异常披露、显式确认/重试、警告durable/receipt恢复闭环；新增决定立即回传不丢、metadata重试不重复授权和源未知不当空quota回归。TXT/EPUB状态和图成功数修复保留。
- 阅读器亮度窗口lease/系统跟随、持久化、离开恢复已补；badge静态基础CSS不被hover/keyframes污染，保留dot/pseudo/变量；旧合法单测期望未改。
- 72历史提交/575变动路径重新整理，远端HEAD确认与本地一致。历史203 API模板复扫仅作为发现清单，不算实际调用或全功能通过。
- 最终1456tests/204suites 0失败/错误/跳过；Debug/AndroidTest/Lint/R8组合exit0，Lint0errors19warnings1info。MuMuDebug20通过；R8 Beta install-r、非debug、同签名/设备hash相同、登录/67收藏/4列/4页保留。APK `d01cdf5fa144a073c7f4319c133e77057ec3f326ef74f911039e00b0fbe7df28` 3,390,920B。
- 真实搜索GET/标签GET在后半程发生网络超时，已记录；uiautomator退出SIGSEGV为shell工具，不冒报App崩溃。实际Save反馈后冷启恢复通过，失败驱动保留。
- 报告 `D:/NovalPie/agent-bridge/artifacts/beta7-search-retranslate-20261008/GITHUB-HISTORY-REPAIR-REPORT.md`；仓库 `docs/beta7/20261008-github-history-search-retranslation-audit.md`。未commit/push/tag/release，不误称GitHub公开资产已更新。
- Owner仍Codex，持续审计未完成；消息/工作区未知回执、超大章资源/复杂EPUB/历史迁移/全部线上角色与适配性能证据等仍必须继续，不把本轮绿灯当成完整Beta7完成。

## 2026-10-15 R2用户实机反馈：重翻/确切末章/搜索小控件
- 原生预检把配置列表缺approval_status误当无可用API。真实帐号3条healthyactive、approval均省略；精确book358153最新89章v2POST真实接受，修后原生UI再点收到409任务处理中。改用服务端权威判断，保留未知回执/owner与取消防重。
- 末章后台目录确认不丢旧目录/不显示大型书卡骨架，按真实windowedge判定；先发布终态后扫描缓存；15s真实HTTP预算包含认证回退，无目录时失败重试仍可用。确切用户书89章实机自动已读完，无手动retry。
- 搜索控件实点：补none/no-query/all三态，主动保存关键词+完整条件，清快照不清当前tag；11组实际query全成功，来源/字数结果断言。用户设置未改。
- 最终1468tests/208suites全绿，Debug/AndroidTest构建exit0，MuMu22设备全通过；初次组合1个正文准备驱动race已修等待语义并保留日志。最后共享deadline小改后未重跑Lint/R8（前一版Lint0error19warning1info），最终为Debug验证包，不误称优化发布包。
- 安装包SHA256 `de178792a1eeeb1b26ac9fd2f26ab03183857aada86cfba82e7a14c28bd589eb`，设备hash相同，登录保留。未commit/push/tag/替换GitHub，现公开R2不含本批；报告D:/NovalPie/agent-bridge/artifacts/beta7-runtime-feedback-20261015/RUNTIME-FEEDBACK-REPAIR-REPORT.md。Owner仍Codex，完整审计继续。

## 2026-10-15 历史自动同步 + R2同日更新2发布准备
- 修历史遗漏3处：根入口dirty进度刷新、阅读器返回、localprogress reconciliation；LibraryContentViewModel后台刷新保留历史行/多页，不混收藏记录。
- 4条新回归旧9tests/4fail，现1472单测/208suites全绿；同源码Debug MuMu23设备全通过。历史UI1/20→返回4/20→底导航返回5/20，不点同步。此前重翻/章末/搜索未公开修复也包含。
- 用户明确要求更新GitHub推荐3MBR2，versionCode2026101502递增，仍同Release，不改旧21MB对照资产。发布收据待R8实际安装和远端digest复核。
