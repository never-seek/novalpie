# Beta7 原生TXT/EPUB文本节点派生修复

日期：2026-10-05；Owner Codex；持续目标未完成。当前工作区和之前所有未提交修复保留，未commit/push/tag/替换GitHub公开包。

## 1. 本轮目标与完成项

三个原有确认案例D03/V05、D04/V06、D05/V07分别是：TXT规则误改HTML属性、EPUB规则输出内部token使真实插图重复、无识别章头TXT不应用规则。本轮均落实，并在原外置用例转绿。

- DerivedTextPipeline使用同一source节点扫描边界：原HTML标签/属性、script/style、注释、原图片标记、Markdown图片地址、链接目的地和普通URL不送进替换器，链接可见label可变。
- Reader/TTS保持原Markup序列化规则；TXT按原内容语法处理但保留真实换行与纯文本字面替换字符；EPUB按可见文本XML编码，不把替换输出解释成新标签/图片。
- 原生EPUB不再全字符串回扫`__NOVALPIE_IMAGE_*__` token，结构只从真实source image match处输出。因此一幅真图仍一次引用，重复原图引用仍照原source位置保留、字节未重编码。
- EPUB先识别source保护范围，再处理可见Markdown样式；避免`title="**Alice**"`等属性被Markdown切开后误替换。旧加粗/斜体/下划线/删除线、跨空行的完整XHTML和不额外采集HTML/Markdown图片的原网站下载合同保留。
- TXT在真正发现首个章头时，前段作为metadata原样输出；若一直无章头，则作为chapterOrder1正文变换、callback title为空且不向输出插入假章名。跨行正则在同一正文处理，未按缓冲块切碎规则作用域。
- 首个章头前只用64KiB内存前缀，超出写本任务spool；暂停/取消/失败关闭并只删自己的spool。生产runner传入已验证work目录、重试清自己的不可恢复preamble part。空源不生成假章节，前导正文空行不再丢。
- 未发布缓存包的exportPipeline升级5；旧TXT/EPUB变换包不能绕过新处理。旧已应用替换/含任何图片（包括cover）的无收据包不凭CRC自动认领为已验证；保留source/assets/授权来重建。已经发布成品的保存事务仍按上一批收据保护，不静默覆盖或重复发布用户文件。

## 2. 现有功能保留与失败驱动

- 不删除EPUB/TXT、原文导出选项、应用替换选项、个人优先/公共按书开关及逐条隐藏、regex/$1/范围/标题正文、frozen snapshot、原图/可选压缩、并发/自定义、暂停/取消/重试。
- 最初9个新探针8断言红，原始日志/XML保留。两个新增fixture曾忽略规则默认Content而期望标题变、或把原Markdown下划线当作普通可见字符；已明确用Both和原有格式语义，不把驱动问题算App bug，也未修改任何旧仓库期望。
- 第一轮全量暴露旧`DownloadReplacementStructureTest.txtKeepsVisibleReplacementCharactersWithoutHtmlEscapes`回归：纯TXT必须保留用户输入`<em>... **...**`字面字符。实现修正，旧断言原样保留通过，不靠把它改成接受实体逃过回归。
- 实机发现TXT因为源包含script而把正常换行改成`<br/>`；新增仓库回归并修正TXT的换行序列化，Reader仍按原HTML换行语义，不退掉安全文本节点扫描。
- 另一实机失败读取的是合法“未被识别成网站metadata的prologue”chapter1而非实际图文章节。通过输出XML确认，修正合成输入到真实网站metadata合同（书名+总章节数）；没有改生产去丢可能合法的prologue，也没有放宽图片数量断言。实机最终以DOM/ZIP/CRC/源图SHA检查一幅图一次，而不是只看ZIP能打开或ADB退出0。

## 3. 当前验证

| 项目 | 结果 |
|---|---|
| 新仓库回归 | 24项：21文本导出/前缀/作用域/图结构；2共享扫描器；1旧变换包禁绕过检查点 |
| 默认全量 | **1393 tests /191 suites，0失败/错误/跳过**；unit-final-results / full-gate-complete.log，exit0 |
| Debug与AndroidTest | device-recheck-gate.log已对最新生产代码打包，之后只调整测试输入；最终测试APK重建完成 |
| 最终Lint | 0errors/18warnings/1information；生产源码对应lint-final.xml。未添加baseline/禁错误/放松旧测试期望 |
| MuMu Debug | **OK(19 tests)**，35.815秒：实际Android原文/应用替换EPUB+TXT、有效透明PNG原始bytes、ZIP CRC/完整XML、同图一次；并复验保存事务、导航重建、触摸、分页、中文PCM/连续朗读 |
| EPUB/TXT实际测试内容 | 全部合成、无网站调用或扣积分；原文/替换两个版本的PNG bytes一致，source原图URL和HTML属性保持；个人规则优先，公共规则/隐藏按既有策略 |
| 外置75旧审计 | **60通过、15红、0 errors**；三个D03/D04/D05原案例转绿，没删除旧用例。15=9确认需求缺陷案例+2空回执风险+4旧驱动/输入不适配，不等于15独立bug |
| R8 | external-beta-gate.log中assembleBeta完成；组合exit1来自外置红灯，不冒称所有门禁全绿 |
| 最终MuMu优化包 | **eb6a649d1aa6db197a88adf40b06cbe89e00a2b6433e0a41296c5054abcd6ca8**，3,358,157B；设备base.apk SHA一致，同签名，非debug |
| 状态保留 | com.novalpie.app.debug /2026100101 /2.0.0-native-beta7；install-r、冷启动约1.1秒，登录/收藏67/四列保留，optimized-home.png；未删用户下载/规则/会话 |
| 构建一致性 | source-final-hashes与最终app/src一致；git diff --check clean；无运行测试下载/TTS遗留 |

设备：MuMu127.0.0.1:16384，Android15，900×1600/density240/字体1.0/60Hz。Debug白盒用例不冒充R8白盒全矩阵；优化包本轮只验安装/hash/签名/启动与首页，不拿此前网站/大书截图来补本轮缺证据。

实际Android证据：chapter-original.xhtml、chapter-replaced.xhtml、same-pipeline.json（有效透明PNG SHA）、device-tests-final.log。截图：after-native-export-tests.png、optimized-home.png；不造假展示下载速度、积分或完成状态。

## 4. 文件

生产：
- D:\NovalPie\native-android\app\src\main\java\com\novalpie\nativeapp\feature\reader\text\DerivedTextPipeline.kt
- D:\NovalPie\native-android\app\src\main\java\com\novalpie\nativeapp\data\NativeEpubArchiveWriter.kt
- D:\NovalPie\native-android\app\src\main\java\com\novalpie\nativeapp\feature\download\NativeDownloadTaskRunner.kt
- D:\NovalPie\native-android\app\src\main\java\com\novalpie\nativeapp\feature\download\DownloadPackageCheckpoint.kt

新/补回归：NativeExportTextNodeRegressionTest.kt、DerivedTextPipelineTest.kt、DownloadPackageCheckpointTest.kt、NativeExportTextDeviceTest.kt。

## 5. 全目标未完成；下列不提前算通过

- **超大单章/整本无章头**：前缀探测可spool，但既有transformChapter仍接收String，最终无章头/单章正文仍需整体String。不能把64KiB前缀缓冲说成所有书恒定内存；这是S11c/C03资源风险，需继续真正的流式/分页输入规则处理及明确故障资源验证，不能静默跳规则、按块改变regex语义或退回原文。
- 已出版旧pipeline有历史错误的成品不应被后台静默覆盖；源码保持不重复保存原则，旧无journal/受损未知窗口仍须受控迁移/说明。当前新输入/新未发布包正确不等于历史每份下载已被自动修复。
- 当前最终R8真实授权原文/替换EPUB/TXT、1GB+图文/动态图、外部阅读器、空间满/强杀/重启/OEM/低系统完整矩阵尚未本轮重跑。两个合成Android包不等于所有线上下载完成。
- 当前剩余9确认案例：编辑器EPUB资源往返、metadata未知缺失、校图状态、反应桶、部分资料/签到回执、总页数、亮度、badge状态样式；2空写回执合同风险、全部Beta7/58项/新增要求的未认证继续。
- Website网页登录/全反馈分页与新355500第44章源图对照仍未完成；不宣称所有源数据都正确，也不伪造缺图。

Next owner: Codex。优先继续EPUB编辑资源/元数据保真、完整性状态与回执，资源/历史恢复边界同步补回归。不给Gemini新的主题重做任务。完整目标保持“修好已知bug，已实现功能不缺失”，本轮不是最终发布交付。
