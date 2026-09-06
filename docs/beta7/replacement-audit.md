# 替换规则同步与优先级审计（进行中）

保留原外观、个人优先、本书总开关、默认开关、单条屏蔽、复制、正文/标题/范围、原文或应用规则导出。不是新建另一套公共规则服务。

## 原网站当前依据

- Nuxt基线`8L_S-edK.js`约6349：公共词表按`created_at`倒序、同时间id倒序，按完整`source_name`去重；个人规则覆盖同源公共。
- 同文件约44261：字面规则原文长度倒序优先，随后正则；`\n`转`br`。App保留默认顺序，额外用户order只在本机生效，不能宣称网站同步了排序/章范围。
- 来源以`re:/pattern/gims`编码。字面字符串和正则pattern即使显示相同也不是同一语义。
- 创建`POST /api/users/me/glossaries`、更新目标`PUT /api/users/me/glossaries/{id}`；source不可原地改，修改source需要创建另一条并删除自己旧条目。

## 回归与修改

- 65994四项先失败：同名regex/literal合并丢规则、旧公共版本消耗匹配、远端新目标不更新、用户order未执行。
- 24862替换46项通过（含新增5项identity、store6、codec1、presentation34）。增加`websiteSource/websiteReplacement`作为已确认同步基线，旧未记录基线的本机规则保持原样，不按时间猜测覆盖。远端新版本只更新未被本机改动的文本对，停用/范围/顺序仍保留。
- 23839旧同步顺序3项全部失败：create失败先删旧贡献、清理失败前无新ID检查点、先前不确定POST的同值规则再次发布。
- 新`ReplacementRemoteWriter`先读取个人词表确认已存在同值规则；不存在才创建。source变化先创建并检查point新ID，再删旧；清理失败保留`websiteCleanupRuleId`，用户重试时仅清理，不重复创建。无自动无限重试。
- 原书确认结果写入`ReaderReplacementRulesStore(bookId)`，页面只在当前书匹配时更新，不因切页丢server ID；写入按书+localRuleId串行。加载增加账号/代次校验。
- 16758替换50项与Debug/AndroidTest通过；之后legacy基线保护、明确协议拒绝与失败删除保留，4724/69179全量984项通过。69179新增live测试多写.content编译失败已修，48180应用/测试包配套成功。当前准备安装APK `830903ddf5a3eb4f371cc7b77ce92053eebf4f83694085488597dfba6482291e`，尚未真实发布临时规则。
- 仍需根协程并发操作的完整回归、账号私有规则存储迁移、真实公共规则写入/源正文/EPUB/TXT验收、文本节点结构保护边界。可选清理检查点只属于本次QA，不删除用户原有规则。

本文件只记录证据级别，不把原先修复记录或新单测等同于Beta7交付通过。

## 2026-09-06 22:52 真实公共规则与导出验证通过

APK `830903ddf5a3eb4f371cc7b77ce92053eebf4f83694085488597dfba6482291e`，MuMu Android15/360dp/font1.0。`ReplacementLiveDeviceTest`明确opt-in，仅353686《透明龙》/6072567。

- 从真实章节选未与用户个人规则重名的短语，创建一条带唯一Beta7临时标记的规则；服务端回读公共列表确实存在。
- 本书启用时派生正文含标记；关闭公共策略或仅屏蔽该条均不含；TTS句群含标记。再次读取原始章节字节内容不变。
- 同一公共列表快照分别原生导出TXT/EPUB×原文/应用规则四组合；原文不含临时标记，替换模式含标记，两个EPUB均11个XHTML（含目录/封面）覆盖9章。
- `OK (1 test)` 39.306秒，总测试核心15.55秒（先读取准备不在核心计时内）。报告`beta7-device/20260906-public-replacement-report.json`；门禁`20260906-public-replacement-live.{json,log}`含安装包hash。
- 结束删除精确新建规则ID及四个本次公开下载URI，随后GET确认无该规则，`ruleRemoved=true`；本机pending清理检查点不存在。没有修改用户原有规则/原始章节，没有把读取正文写入报告。
- 这是实际API/管线/服务导出验收；正文显示和TTS文本判定在App真实管线中完成，不冒充用户触控了所有规则UI按钮，实际规则UI及R8终验仍待。

## 文字节点边界（后续开发中）

- `ReaderDerivedStructureTest`21963四项2红，实际旧路径把替换输出`![...](...)`解析成图、`**...**`解析成新格式；旧源强调/标签实体控制样本保持通过。
- 现增加仅内存`ReaderTextDerivation`，`readerBlocksForContent`先解析原章结构，再在已有文字/格式节点间派生，保持原图片块与span范围。原缓存序列化不保存派生字段。前台阅读与后台TTS使用同一入口；67261相关定向已通过。
- `DownloadReplacementStructureTest`87890确认替换能触发额外图片请求。EPUB改为原文先切图、再对文字区段调用冻结规则，不再对替换后字符串重新找图；73820相关用例通过。
- TXT标签字符被HTML转义的边界58838先失败，当前改为输出原图标记+替换后的原样可见文本，与EPUB的XML编码分开。74945全量/打包正在运行，未安装此节点改动。
