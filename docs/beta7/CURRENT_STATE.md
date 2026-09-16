# Beta 7 当前工作点

## 2026-09-13 19:49 本轮结束点（优先于后面的历史）

521be94e...a423d6最新优化包已装，完整1225/173测试、R8/资源压缩/lint0错误5警告、签名/版本/设备hash均通过。源码含离线百分比null+UI—、EPUB文本Deflate、独立书籍元信息状态与root同步环境钩子、确认/封面picker书籍身份、稳定lazy key、管理深链导航revision取消。Review Avicenna追加2P2已7923红→37112绿并复审PASS。最后完整49881绿，22071构建/lint绿。

实机真实44章/6正文图+封面EPUB完成，58ZIP/CRC完整，7图SHA/bytes对比09-10旧原图包全同；新包5,853,374bytes（约省5.07%），Librera图/正文打开。离线进度—实机截图已看，网络/尺寸/字号已恢复、无任务服务，最后正常冷启动1735ms、联网2.27%。报告20260913-lossless-metadata-runtime.json。

重要：精准删除本次设备/主机EPUB与打开管理深链的组合命令被policy拒绝；未执行/未绕行。两份测试EPUB仍存在，路径见报告。不得换tool/入口重复被拒绝动作。原文/替换四组合、其他章节管理/reader数据等原任务仍待，不因这轮成功而全关闭。

节点：Kant实现中503后关闭，恢复其部分源文件并由controller完成；旧test fixture递归已修，UI初始selector假红不计、86716已真正复现确认与picker错误。两个scope reviews均通过。未commit/push/tag/release，dirty所有前序工作保留。下一轮先看REMAINING/本节，别重复已完成红绿。

更新：2026-09-13。完整经过见 execution-log.md；最新结论见STATUS.md，剩余交付见REMAINING.md。下方按新到旧保存执行记录。

## 09-13 安装与回归

- bfeddab2...911af已install-r，设备与本地hash一致；原账号/65收藏/搜索筛选/阅读恢复保留。冷启动正常，最后普通启动2802ms无crash。TTS开始/暂停/继续/停止、HOME/熄屏音频输出、停止服务已验，通知恢复拒绝。
- Reader320dp/font2在线正文页脚不再裁半行；音量下页进度2.27→2.31、上页回2.27。12尺寸字号采集完成，UI bounds已读，不能标成全App视觉矩阵通过；已恢复540x1098/density240/font1。
- App网络设置旧10.0.2.2:7890出现22秒超时，改为127.0.0.1并ADB reverse后目录/详情0.8–2.3s返回；普通手机默认不变。发现离线缺目录百分比伪0.00%新缺陷待修，已有截图，不以联网成功掩盖。
- 旧Debug instrumentation第一次runner不匹配，重建测试APK4999后仍在AndroidJUnitRunner.onCreate缺Trace；业务测试未开始且其ReaderTestActivity本就非R8成品组件。不要再盲重试旧Debug测试/降级/给成品加测试入口，需独立SDK/UI黑盒。普通App已重新启动并清晰区分工具错误。
- 本轮无业务源码改动、无真实管理/规则/下载写入；文档新增BETA7-CHANGES-ZH.md并更新剩余项，尚未发布。最终报告20260913-beta7-core-runtime.json，截图beta7-20260913-*。

## 09-12 新增缺陷回归

- **17:26本轮门禁完成：** 1203tests/169suites全绿、8548 Beta编译、61157低内存R8/resources shrink/assembleBeta、19408 lintDebug（0错误5警告）、git diff --check均通过。bfeddab2...911af新包静态身份已核验。构建退出后可用提交2.65GiB<MuMu原4GiB、ADB为空，未安装/未发布；最后安装仍5b58ddbb。下次先读STATUS.md和book-management-followup.md，不重做本轮已通过的红绿流程。

- **17:18产物更新：** 61157低内存独立assembleBeta成功5m39s，保持R8/resources shrinking开启。新APK bfeddab2d81f542baf4c8eaf92bc7a9cca681306410987b1ea64df138de911af / 3,259,834bytes，验包脚本确认原Beta6证书/`.debug`包名/非debug/无audit组件。19408独立lintDebug进行中；尚无新包安装。此前83160 OOM记录保留，不再当最新打包结果。

- **17:08验证更新：** 完整79656已通过1203tests/169suites/0失败错误跳过；8548 Beta编译成功。83160在R8时JVM本地分配约1MiB失败（hs_err_pid24476.log，物理空闲870MiB），不是业务测试失败；未完成本轮R8/lint。启动一次更低预算（heap1024/metaspace384/direct64/ActiveProcessorCount2）的独立assembleBeta，保留R8不降级。最新磁盘APK仍6ec86cb3旧包，未安装新代码。

- 编辑器删除期间打开另一章导致下标错位：34153真实Compose红，busy禁用卡片与回调guard后20121的27tests/4suites全绿；Hooke独立复核原第6项已解决。没有改变现有主题/布局。
- 书籍管理回执：新增7项6562全红后修复，95822前133项全绿，新增六种章节操作的12个空/拒绝回执组合红。现已统一严格确认，完整单测79656进行中。详情见20260912-management-receipts.md。禁止将旧1194全绿/6ec86cb3包当作本次代码证据。
- 20260912-delta已全文读新增1951：357637原文获取卡住、近500章未获取；先归为源获取问题待源数据核对，未伪造成功。反馈总472，App1871的39条回复中最新两条仅旧WebView下载位置问答，未有新增故障描述。
- MuMu仍关闭；可用提交约2–3GiB，原配置4GiB无法可靠启动。没有结束系统进程/改分页文件/改MuMu内存/清登录。最新优化包仍6ec86cb3（本次更改未打包），最后已安装5b58ddbb；Beta7未发布。

## 09-10 正在处理

- **09-12 09:50当前：** 1194全量测试/新Beta编译/R8/lint（0errors5warnings）均已完成；新APK6ec86cb3...5262c2/3,259,838bytes已验原证书/非debug。MuMu正常launch失败，明确`VERR_NEM_NOT_ENOUGH_PAGING`；已shutdown失败实例，没有安装新APK，设备仍旧5b58ddbb。
- 查私有提交占用（非仅驻留RAM）：igfxextN20100约22.38GiB，explorer14224约6.35GiB；当前剩余提交约1.2GB，MuMu原配置4GB。igfxextN路径在当前权限下不可读取，未结束/提权任何系统进程，未改OS分页文件、MuMu RAM或实例数据。正常关闭游戏后构建恢复了，但系统提交内存仍阻止模拟器。需要用户先释放系统资源/重启电脑，不能假称安装验收已完成。

- **09-12 09:42：** 当前源码1194tests/168suites/0fail。分段43448Beta编译成功2m14s；67861R8/assembleBeta/lintDebug成功10m12s。新包SHA6ec86cb3bfb2a5d59a3cc11995e180745e2dc34a2166ac3a9f8ffa0b695262c2，3,259,838bytes，原签名/非debug/audit组件无均验包通过。MuMu启动中，尚未安装本包；不要把旧5b58ddbb图当本包结果。

- **09-12 09:27：** 51869低内存模式全量testDebugUnitTest成功5m30s，当前所有新增回归包含在本轮。可用提交回升约3.8GB。按两段构建：先低内存compileBetaKotlin/Java，结束后释放本项目Kotlin daemon，再单独R8/打包/lint，避免峰值重叠。此时仍未生成最新可安装包。

- **09-12 09:21资源恢复：** 用户继续后检查nikke已退出（本任务之前的强制终止失败，不归因为我们成功关闭），可用物理约2.2GB/提交约3.7GB，无项目Java/MuMu。51869使用命令级低内存双JVM配置重新运行全量testDebugUnitTest，尚未返回。不会同时启动MuMu或第二构建；代码与已安装5b58ddbb不变，尚未发布。

- **09-12 09:10续接仍受内存阻塞：** 无项目Java/MuMu运行，主机FreePhysical约0.5GB/FreeVirtual约1.4GB。针对上一轮点名的nikke32076先CloseMainWindow返回true但进程未退出，Stop-Process被OS拒绝访问。shell后续“Closed”文字不是成功证据；再查进程仍在。未提权、未结束其他程序、未重启构建。需要用户手动退出该游戏或释放其他内存后再验证；代码/测试/安装包状态保持上段。

- **09-12 08:51 当前验证阻塞：** 74009原预算native OOM；40302降为Gradle512/Kotlin1280、direct128、禁fallback后仍在compileDebugKotlin阶段daemon丢失。峰值可用物理内存454MB、可用提交额度仅110MB。两次均未跑新全量测试。已终止本任务构建并检查无残留项目Java；未结束用户nikke/Antigravity/其他应用。停止后可用提交额度约2.3GB，仍不足以可靠完整构建+R8验证。需要用户释放其他应用内存或明确许可关闭具体高占用应用；不修改分页文件/OS设置，不清缓存冒充修好。
- 当前最新源码包括门槛原值/未知禁保存、六项review修复及footer行高适配。最后安装包仍5b58ddbb。最新完整绿门禁仍是此前1169；后续1191全量一项EPUB导入分配失败已由51443四项定向证实修复，但随后门槛改动未完成全量。不要宣称新包已生成/安装/发布。

- **09-12 08:48：** 接管后全量98572发现执行者最后补的EPUB导入测试确实仍有16MB UI分配；将parse+toWebsiteIdentifiers同置IO，51443四项真实文件测试已绿。书籍门槛13856红后已接camel/snake当前值、缺项null并禁保存，尚未完整过门禁。
- 74009在Kotlin编译阶段主机native内存不足，非业务测试失败；hs_err_pid39516记录仅510MB空闲，未跑到测试。无残留项目Java进程，未关闭用户其他软件；本轮改命令级Gradle512MiB/Kotlin1280MiB、direct128MiB且禁fallback重跑单测，session见工具。源gradle.properties没改，不再用已知失败的in-process大JVM方案。
- MuMu已关，已安装仍5b58ddbb，不含六项复核修复/大字号footer/门槛读取。没有新的云配置或下载残留。Raman复核也429失败，独立再复核未完成，不当通过。

- **09-11 22:25接管：** 子agent两次429后不可恢复，原writer已无活动进程。磁盘留下六项修复，编辑器最终25tests/0fail（14focused+3realfile+8VM）确认。主agent接管并启动完整testDebugUnitTest/assembleBeta/lintDebug，运行session见当前tool。不要重复旧red轮。新六项+footer字体修复未安装，设备仍5b58ddbb，MuMu已关、无线上测试残留。
- 热缓存3轮低5%与真实云配置全CRUD/清理证据已记录，完整矩阵/其他模块仍待；App1871现39条新增仅WebView下载位置问答。书籍管理门槛未加载原值问题已确认源detail提供camelCase5字段，下一独立修复片，不用真实资产试写。

- 09-11 15:52：第二reviewer Faraday返回六Important，已验代码并交唯一writer McClintock（01a08f65-522a-77a0-8e17-6b28f42b659b）修，见.superpowers/sdd/FINAL_PASS/fix-brief.md/fix-report.md。当前该worker在测试/修改，主agent不改源码、不跑Gradle或MuMu。Footer测量68836红→97162定向绿已完成并要求worker保留。
- App1871最新只读强制刷新为39条（17根），新增2453/2455在4541线程，仅问旧WebView在哪/指路，无新Bug；正在20260911-delta持久采集。未发回复。
- 主agent只读另发现book-management门槛表单未加载原值，会默认none/allow=true；下一修复片见book-management-followup.md，未操作真实作品权限。

- **09-11 10:50最新：** 新包5b58ddbb云端配置已真实create/read/apply/update/default/delete+refresh空列表，唯一测试名Beta7-QA-20260911-0344已清理、原列表为空、无他人配置被改。截图20260911-cloud-profile-loaded/deleted已看。已安装包不变，不重建旧片。
- 同包纯文字360214第2章固定滑动3轮：278/273/291frames，jank0.72%/1.10%/0.34%，均低5%；报告20260911-warm-text-scroll.json。单次meminfo PSS67470KB/Activity1仅快照，不当泄漏证明/真机高刷认证。
- 320dp/font2.0真实发现页脚时间和章节裁切，正文分页无半行。当前68836 ReaderScaledChromeTest测真实TextMeasurer line budget预期红；readerChromeLayout新增参数暂未实现，待改按实际sp→dp行高预留高度。设备恢复540x1098/font1.0，分页模式已恢复，MuMu正常shutdown，无任务。
- 只读reviewer Goodall第一次429失败，resume返回agent not found，未得到代码审查结论。继续保持一个agent写代码。

- 09-11 03:41续接：71222最终success22m14s/1169tests/lint0errors32warnings；5b58ddbb22f3a3d8652605a5a2f83693aa60d22e68eabbdc2b94cc3e8dbadade已实际install-r，冷启动3190ms，65收藏保留；第1章内上一页百分比2.27→2.14，footer修订已初步实测。云配置/编辑器仍待同包运行，尚未真实创建测试配置。
- Android QA/performance与Superpowers技能现可用并已读取；沿用用户指定现工作区不新开worktree。只读reviewer Goodall因429服务限流失败，无代码结论，不当通过；没有其他agent改文件。
- MuMu验证间隔后发现实例已关闭、ADB退出，已请求正常launch继续；不要卸载或清数据，不重跑已完成构建。

- 15:55 71222单测1169/164suites全部通过，optimizedBeta已生成，lint仍运行。尚未启动MuMu/未安装编辑器与云配置新片。验包工具21597已运行/结果以tool为准。当前新增原生云配置没有真实写入，保持最后安装a7fdae0e和原用户状态。

- 15:43当前71222全量unit+assembleBeta+lintDebug运行，MuMu仍shutdown，无活跃真实上传/下载/TTS/规则/云配置写入。Reader云配置已接独立VM/Repository/Codec/Panel，协议/字段定向过，刷新丢新名称27944红已修；阅读进度当前章分数已接，进度只被footer读取以控制重组。Editor拆分及修复已接且8项状态/本机分章定向过。新功能全部待此次打包和实际安装，不混记已装a7fdae0e。

- 15:41工作点：Editor独立VM/Repository/EditorUiState已接、根减少约845行；AI正则/存档总文本/旧结果覆稿三红→三绿，后台分章/代理保稿/账号隔离/旧脚本ID/文件迟到扩展通过。Reader本章百分比修订4249红→65613定向通过，后续改为仅footer读取节流状态、尚未实机。
- 云端阅读配置确定缺口正在补：Codec两红40041→54734字段/GET/POST/PUT/DELETE协议+Editor定向绿，界面主动加载/保存/覆盖/默认/删除接入Other“偏好配置”。27944云配置VM状态回归运行中，预计刷新时新名称可能被旧snapshot覆盖。整个新片尚未打包/安装，设备仍a7fdae0e、MuMu离线，无真实配置写入。

- 14:33用户要求完整对账并做完：新增REMAINING.md明确六类剩余，纠正“只剩打包”误解。最新App1871根/嵌套37条完整读取无新用户回复；反馈467，新1944无ID插图全失败、1943书36098349章无图、1942书359622109章起作者改图，源码build不变。未执行反馈文本里的补图/改书请求。
- 当前工程切片：Editor独立EditorViewModel/EditorRepository已接root，移出约808行业务，保持UI入口。59990三项预期红灯测试运行：AI正则结果未应用、清空后旧API覆新草稿、修改章节存档仍保存旧text。这一拆分尚未安装/不得算全绿，MuMu仍shutdown，已安装包a7fdae0e不变。

- **13:54最新可用状态：** 67267已成功14m41s（会话过期后从daemon日志和09:10 lint报告确认），1152unit、lint0errors/32warnings、optimizedBeta全部通过。当前已实际无损安装`a7fdae0e8c8b93936520b94f5602e7be7df9f0be150612bcd7e5bc00103fcf21`，3,243,445bytes，冷启动1608ms、收藏65/继续阅读仍在。不是早先801cd2e或Debug。
- 最新包重新原生导出360214：44章/6正文图引用+封面/6,166,238bytes，EPUB SHA8f12ea9c...1b8ba40b0，58条ZIP CRC/49个XML均过，260strong/204em；7图SHA逐一和前包相同。Librera同一第1章第3页截图20260910-styled-epub-librera-bold-confirmed已看，源**代码消失，图片与正文打开正常。报告20260910-styled-epub-report.json；不宣称与未另下载源字节比较。
- 本次新EPUB及host副本已先核对SHA再精确删除，旧用户下载未动。通知恢复原拒绝、MuMu正常shutdown，没有运行Gradle/下载/TTS。三个本地提交ae3af7d/cee96be/f87d04d已保存，未push。
- 下一阶段：按FINAL_PASS同包核心矩阵（连续章评/替换四导出/TTS焦点等）、上传批次真正设备进程恢复/界面、完整尺寸字体/冷热性能、普通角色/剩余业务入口。当前仅完成上述新缺陷闭环，不是Beta7整体验收/发布通过；源新1940无具体书ID仍待定位。

- 08:52 58067全量1152tests/160suites/0失败通过；67267正assembleBeta+lintDebug，只有该Gradle。EPUB排版新片未装，MuMu已关。下一步完成67267验签install-r，再从360214详情原生导出EPUB用Librera复查strong/em样式和6正文图片；只清新生成的测试文件，用户下载不动。旧包801cd2e仍安装中。

- 08:49 EPUB样式进展：44463的28项仅测试apostrophe XML转义断言一失败已改为&apos;（保护实现未放宽）；新增跨空行强调XML测试71949明确SAX失败。renderStyledText在段落边界闭合/重开strong/em/del，pipeline4防重用旧EPUB，58067全量单测执行中。新样式片尚未打包/实机/提交，不混记801cd2e旧包。

- 08:44 EPUB强调回归39351红；新增renderStyledText复用Reader的Markdown spans、在替换之前解析源样式，44463跑NativeEpubArchiveWriterTest中。待确认：apostrophe escaping断言、强调跨空行XHTML结构、导出pipeline版本需升以失效旧package检查点，原图/图片匹配保持不变。该片尚未commit/build/install；设备仍801cd2e。

- 08:41最新：90389优化构建成功，新包`801cd2e56fec5e001ae1063e16afd03fc4c274bce9cef29f1d681e8bee851604` 3,243,446bytes已验签/install-r，冷启动1754ms。第2章首页音量上→第1章完整尾页→音量下章评→再下第2章首页；左侧点击也回上章尾页，当前回调阻断实机闭环。截图20260910-fixed-previous-chapter-tail/next-chapter-first均已看。
- 本地提交ae3af7d阅读/TTS、cee96be上传批次已保存，未push。当前有beta工具/文档/测试脚本未提交，GitHub仍不变。
- 用当前801cd2e包原生下载360214《说我是学院的恶劣CP厨理事长？》EPUB：44章/6正文图片资源及引用+独立封面/6,160,100bytes/58条ZIP，CRC与大小/引用完整，保存真正/sdcard/Download。Librera打开封面/正文成功，Readest自身WebView版本检查拦住（未更改系统）。**新缺陷：EPUB正文露出源Markdown `**`，需修导出富文本。** 报告20260910-core-runtime-report.json；没有声称源原图SHA比对完成。新1940无具体样本，不能靠此书成功关单。
- 当前已删除本次唯一测试EPUB及拉到host副本（先比对SHA8734861c...3c4f0f），原有下载未动；通知恢复原拒绝、无运行播放/下载，MuMu正常shutdown。下一步先做EPUB原文/派生文本排版回归并修复，再继续最终全矩阵/上传批次设备恢复等。

- 08:25 90389全量1149tests/160suites/0失败（含hasPrevious/hasNext迟到Compose用例），beta lintVital通过，R8打包仍运行。最终待安装复验，上次实机反向跨章失败尚不关闭。

- 08:21 94413分页/异步目录回调定向成功1m38s。补对称晚到hasNext回归后，90389全量unit+assembleBeta正在执行；暂无新安装包，设备仍133ec9ba。原代码改动仅latestMove的局部函数引用改lambda，其他同包模块保持。MuMu离线，无活跃后台任务。

- 08:19 36025真实Compose组件回归红：目录后到hasPrevious=true仍无法发上一章。已定位`rememberUpdatedState(::move)`局部函数引用等值导致闭包保留旧不可变flags，改捕获lambda；94413定向分页/回调测试正在运行。当前设备133ec9ba尚不含该修复，MuMu已shutdown/通知原拒绝/TTS已停止。

- 新包133ec9ba已实际install-r成功、冷启动2003ms，恢复原书与阅读位置。当前设备就是该优化包。首次TTS绑定1.5秒后PLAYING、音频帧824320→881664→1363968，暂停PAUSED、恢复后HOME仍PLAYING、UI停止后media_session消失；相关20260910-core-tts-playing截图已看。已停止/恢复通知拒绝，MuMu正常shutdown。
- 新运行阻断：第2章首页音量上一页/左侧点击没有返回第1章末页，但章内下/上页正常，菜单“上章”可进入第1章。截图20260910-core-previous-chapter-tail名字有误（实际第二章首页），不得当上章末页通过。NativePagingCallbackTest正以真实Compose复现目录晚到hasPrevious旧回调，36025运行中，尚未改此实现/不得发布。
- 第一章标题/插图/正文已经一起显示，20260910-core-reader-title已看；但图片迟到/反向跨章完整矩阵尚未过。无新增真实上传/评论/规则。

- 03:30 30408完整lintDebug/assembleBeta通过15m09s；lint0errors/32warnings（主要在线依赖更新提示，未升级依赖）。新优化APK SHA `133ec9babed87e1c5eee9df113f0b826bd60b1146c50414de7ad58f9ce5c2eb4`，3,243,449bytes，versionCode2026090901/原签名/non-debuggable/无audit组件；验包报告20260910-core-beta-artifact.json。MuMu已正常launch启动中，尚未安装，旧设备仍8900b7de。无活跃Gradle。

- 03:15全量1147单测/159suites/0失败通过；73255因lint-gradle31.7.3离线依赖缺失停在lint，非代码错误。30408联网恢复lint并assembleBeta运行，禁止并发源码修改或MuMu。
- 20260910-delta公开增量完整：站点build未变、反馈464、新1940“多本书插图下载失败”无书ID/图片URL/0回复，待最终同包复现；App1871仍37条，无新用户回复。源数据问题不伪造成功。

- 03:13最新：94975定向43项只有“绑定中shutdown未释放引擎”一红，其余上传协议/恢复与TTS12秒初始化/停止/超时通过。已修关闭时无条件shutdown+closed guard；补完成后选择新文件清旧批次/中断批固定元数据/UI继续批次按钮。73255全量unit/lintDebug/assembleBeta运行中，不编辑源码、不同时开MuMu；完成后验包安装再做真实首页与听书复验。

- 54926 源码/测试编译通过；直接 JUnitCore 对当前已编译类运行 14 项，全部通过（标题分页、offset/返回一致性、TTS 20 秒有界策略）。此前同类 12 项有两红，形成复现→修复闭环；不等于设备冷 TTS 已复验。
- 54308 Gradle 正常测试已恢复，16 项两红仅 UploadBatchProtocolTest。新建大 JSON 上传单 POST、部分成功不报告书 ID 均已复现。
- 正在44564定向上传/分页/TTS测试：创建首批后按回执 ID 追加、前后检查点、未知结果不重发、已拒绝批可续传、持久保存批次/账号隔离已接线，仍待测试结果与完整验收。没有真实作品写入。
- 百分比0.00源码确认是全书按章节序号计算，第一章原本为0%；当前观察不能单独算分页进度损坏。章节锚点仍另需运行复验。
- MuMu仍未启动，本轮未安装新包，GitHub不变。优化运行/TTS生命周期补充测试与最终同包矩阵继续。

## 最新续接（优先于下方历史工作点）

- 纯 Java/SDK 黑盒驱动已解决 R8 测试启动依赖问题。`20260909-beta7-r8-runtime-verify.log` 实际通过：259 项原偏好、11 个离线文件的长度/SHA、登录身份保留，非调试 MainActivity 正常启动；冷启动约 2134ms。此前“运行未通过”已过时，但不代表全业务通过。
- 当前安装包仍为 `8900b7deed6ecd766b67d7ce1dcd81eb70849b961de754c6049ef4f3d98fa4f3`。真实运行已查看收藏、默认搜索、等高双列、交流、书评流、详情及正文；新分页/TTS 修复尚未安装。
- 实测新缺陷：章节 10846528 首页只有标题，下一页才有正文；分页整段保留策略需避免孤立标题。系统 TTS 冷启动约 10 秒超过当前 5 秒期限；再次点击后系统 PLAYING 且音频帧增长、后台仍运行，但首次超时必须修复。`...tts-running.png` 实际是超时画面，不能作为成功证据。
- 当前 MuMu 离线。18261 正在恢复单测依赖并运行两项预期红灯用例，不并发启动另一 Gradle。新增 UploadBatchProtocolTest 也尚未通过，禁止声称全量绿色。
- 后续顺序：标题分页/TTS 冷启动修复与定向回归 → 大书上传分批及恢复 → 统一优化包安装与性能/业务矩阵。不要卸载、清数据或降级 Beta6；升级只使用 VerifyOnly。
- GitHub 未发布；普通角色、全矩阵、部分生产入口及外部模型写入仍有验收缺口。真实目标被工具拒绝的操作保持未验，不换工具绕行。

## 发布状态

**尚未完成，不可发布。** 首个optimized beta已生成/静态验包/真实覆盖安装：versionCode2026090901、versionName2.0.0-native-beta7、`.debug`包名/原证书、debuggable=false/R8。SHA8900b7deed6ecd766b67d7ce1dcd81eb70849b961de754c6049ef4f3d98fa4f3，3,227,047bytes。**升级完整性与主界面启动已通过，新发现的分页/TTS缺陷正在修复，最新源码尚未完成同包运行验收。未tag/push/创建Release。** GitHub Beta6保持原样。

## 工程与设备

- 分支`codex/native-app2-current-20260821`，最新本地提交`9dd52ce`（未push），持久草稿/管理/图片等切片已保存。当前未提交仅两阶段进程测试脚本、beta构建版本配置/验包工具与文档，没有远端发布。
- **当前已安装8900b7de...98fa4f3优化beta（code2026090901，non-debuggable），不是开发包。不要用旧debug gate/run-as；不要重装Beta6强行降级。** 先前bb1b1eec开发包1129unit/build/lint和多个实机回归保持历史证据。
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
6. 持久草稿9dd52ce已保存，bb1b1eec开发包两阶段真正force-stop通过：24654，PID2082→2229，真实currentUser一致，合成草稿完整恢复且0远端上传、普通submit不重发；测试目录成功后已删。97731只构建AndroidTest45s。此过程没有真实后台服务或用户文件被清理。
7. 38112 R8+验包过；31561真实Beta6→Beta7安装成功，旧runner缺Trace后改纯Java/SDK；87335用`-PbetaBlackBox=true`才将自定义runner实际登记。20:55 MuMu恢复后新版runner能跑，但旧快照路径别名不一致导致文件lookup失败（prefs检查已执行，非证明文件缺失）。已仅正规化精确`../../../../data/<package>/files/`别名，仍校验每个原文件SHA/长度。
8. 20:56发现Gradle依赖缓存已缺，36209离线找不到AGP8.7.3；**46535在线恢复并构建AndroidTest -PbetaBlackBox=true进行中，代理127.0.0.1:7890，仅一个Gradle/MuMushutdown，不编辑读中源码**。完成launch/reverse，verify-beta-upgrade.ps1 -VerifyOnly -EvidenceName新的证据名；保持优化APK8900b7de不变，绝不卸载/降级。首次MuMu服务连接失败已正常launch恢复，无清数据操作。
8. 已找到实际公开Beta6基线APK：agent-bridge/artifacts/20260905-beta6-reader-header-tail-release-upload/NovalPie-native-2.0.0-beta6-debug.apk，SHA30F07AC0...9707D6C3。后续需要完整升级前后数据校验，不把当前开发包覆盖测试算Beta6→optimizedBeta7全部状态迁移通过。完整上传批次协调器/全编辑器/角色/性能矩阵仍未完成，禁止提前发布。

## 最新社区增量

15:58新刷新`20260908-delta`：源build`cbb20abd-9b37-4381-993e-3008189d738b`未变；反馈区461。原456反馈+296相关讨论基线752帖逐帖正文/完整回复均读完、missing=[]；新1935图片下载失败（无书ID）/1936书360516源82章补图已全文读、待源对照，1934/1927/1925新增回复也全读记录。App1871现在37条最新为本次明确测试2439，无更晚新用户回复；登录4645、双图4643定向已修，4681/2432下载打开已实测。发布前必须再次增量，全文读完不等于行为全部通过。

09-09 00:48新刷新20260909-delta：反馈总463，1939获取更新无效（无bookId）/1938图片后缀file（无assetURL）已读全文与全部回复；旧增量无变化，App1871仍37条/无更晚用户反馈，sourcebuild仍cbb20abd。后缀file需按实际内容类型/魔数测试，不重命名伪造格式。
