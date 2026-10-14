# Beta7 原生编辑器EPUB资源往返修复

日期：2026-10-05；Owner Codex。持续目标未完成；所有此前工作区修改保留，未commit/push/tag/更新GitHub公开资产。

## 1. 本轮解决的E01/V03

原案例：EpubParser→EpubWriter（以及编辑器导入→生成上传文件）丢嵌入图片和多数元数据；原文件直接multipart上传一直正确。本轮没有降级/删除原生编辑器，也未误改原文件直传。

- 导入时流式保留本次原始EPUB到私有resource目录，记录大小/SHA；媒体不以ByteArray塞进Compose，章节来源、raw markup、资源manifest与opaque media节点都是磁盘描述符。
- 未改的章节沿用原body/head/root namespaces/classes/样式结构；链接根据新包位置改写。原始图片、GIF/WebP、字体、CSS、音频等资源在ZIP中流式复制，原始字节不解码/重压缩；资源名按bundle隔离，防止多EPUB同名覆盖。
- 正文转成可编辑文本时保留`[[epub-node:bundle:node]]`位置标记；图片专属原markup保留在私有文件，不将img吞成空白。修改文字/移动或删除标记会反映在新正文，重复引用保持两次位置但资源仅存一份。纯图片章节不会再当“无内容”丢掉。
- Editor metadata跟随原资源bundle；单文件/批量EPUB导入保留，手工改metadata期间的晚加载也不丢来源。EditorArchiveStore新字段向后兼容：旧存档没有bundle仍可读，新存档保存原资源描述，load→重建章节→export仍有图片。
- writer可从chapter provenance或metadata bundle匹配原章，保持“标记文本重新分章后”的资源；不需要全局强绑定某个最后导入书。直接EpubParser.parse→EpubWriter.write旧API也保留图片。
- OPF保留原identifier、publisher/date/rights/subjects等原元数据，应用当前title/author/description/language/用户tags与source编辑；cover-image/v2 cover标识恢复；modified写实际UTC而非历史硬编码2026-07-10。
- 源包、raw章/节点结构文件有完整性校验；找不到节点或实际本地资源明确失败，不假称成功丢图。解析失败只清该随机import目录中已知自有文件，不递归删用户数据；缺少实际spine/TOC条目与重复路径仍拒绝。
- 复制原始source XHTML作为非spine辅助资源，为未重排的相对资源/链接提供保留；**spine只列本轮编辑输出章节**，不会在正常阅读次序额外加入原始副本。

## 2. 回归方法与证据

- 最初4个往返/图片/样式字体/元数据/文字编辑回归在旧生产实现4项全部红，red-tests.log和red-results保留。
- 共11新增仓库回归：原图字节和两次出现位置；原CSS/font/cover；用户新标题与原identifier/publisher/date/rights/tag；编辑文字仍保留图片；marker重建、存档reload；image-only；原包/raw markup损坏拒绝；未知node拒绝；spine仅编辑章节。
- 初次严格检查所有manifest item使原5个EpubParserTest失败，因为样本有未被spine/TOC引用的缺失文本条目。修正生产兼容规则：仍拒绝缺实际正文或媒体资源，但不会因从未参与阅读的旧manifest text项丢掉原可导入功能。**旧测试与期望均未删改**。
- 损坏源包的新fixture第一次把本来为0的ZIP尾字节写0，没有真正损坏；诊断后改为xor字节，source SHA失败断言现在确实执行。保留失败日志，不将测试驱动错误算App bug。
- 默认全量 **1404 tests /193 suites，0失败/错误/跳过**，full-gate.log：Debug、AndroidTest、Lint整体exit0；最终Lint0errors/18warnings/1information，没有屏蔽检查/baseline。
- MuMu同源码Debug **OK(11 tests)**，13.915秒：真实Android parser、EditorArchiveStore reload、文本编辑后EPUB和AndroidEditorRepository.prepareUpload；有效透明PNG完整字节、两个DOM img引用、1PNG资源、CSS及publisher/rights/cover原meta；并回归原EpubImport、原存档故障/校验、TXT/EPUB节点替换、MediaStore事务和根导航重建。
- 实机生成的edited.epub已复制到本目录只读验证ZIP CRC、manifest/spine和XML；结果result.json。全部synthetic书，**没有向网站发真实上传/评论或消耗积分**，临时准备上传文件与本轮resource bundle精确清理，用户文件和存档未清。
- 原外置75项再次执行：**61通过、14红、0 errors**，E01旧原调用回归已转绿；14=8确认需求缺陷案例+2空回执合同风险+4旧驱动/输入不适配，不等于14个独立bug，也不是只剩这14项需求需验证。

## 3. 当前构建、安装与证据范围

| 对象 | 当前结果 |
|---|---|
| HEAD/分支 | b4616be2f7f2852c6a8bf86127a804c6a335c8fd / codex/native-app2-current-20260821；新修改均工作区 |
| 当前优化Beta | SHA256 `3aa6341bcc95d87dd9e5659869ca19002bc3134867a98f6952fa1597b3fde5b7`，3,374,540 bytes |
| 安装 | MuMu127.0.0.1:16384 install-r成功，设备base.apk SHA相同、原同签名，非DEBUGGABLE |
| 包/版本 | com.novalpie.app.debug /2026100101 /2.0.0-native-beta7 |
| 状态保留 | 冷启动约1.7秒；登录、收藏67、4列保留，optimized-home.png；没有清规则/用户下载 |
| R8门禁 | external-beta-gate.log中assembleBeta完成；组合exit1来自外置14红灯，不说全部门禁绿 |
| 构建一致性 | source-final-hashes与完成后app/src相同，git diff --check clean |
| 结束时 | 正常收藏页、无运行测试下载/TTS服务，当前崩溃缓冲未找到App process crash行 |

Android15，900×1600/density240/字体1.0/60Hz。11个白盒测试在同源码Debug；R8只验同签名/版本/hash、覆盖安装、启动和首页保留。不能把它说成R8白盒全矩阵、所有大书/格式/外部阅读器或真机/OEM均认证。

证据入口：unit-final-results、device-tests.log、edited.epub、result.json、final-verification.json、lint-final.xml、source-final-hashes.json、external-final-results和remaining-classified-cases.json。截图after-editor-tests.png只是测试后设备状态，不拿它冒充完整编辑器视觉/外部阅读器效果。

## 4. 修改文件

生产新增：
- D:\NovalPie\native-android\app\src\main\java\com\novalpie\nativeapp\model\EpubDocumentSource.kt
- D:\NovalPie\native-android\app\src\main\java\com\novalpie\nativeapp\data\EpubResourceSupport.kt
- D:\NovalPie\native-android\app\src\main\java\com\novalpie\nativeapp\data\EpubBundleJson.kt

生产修改：Models.kt、EpubParser.kt、EpubWriter.kt、EditorArchiveStore.kt、feature/editor/EditorRepository.kt、EditorViewModel.kt（全部位于D:\NovalPie\native-android\app\src\main\java\com\novalpie\nativeapp）。

新增测试：data/EpubResourceRoundTripRegressionTest.kt、data/EditorEpubArchiveRegressionTest.kt、ui/EditorEpubResourceDeviceTest.kt。

## 5. 必须继续补的边界和已发现新工作，目标不缩减

1. **资源生命周期**：新私有原包保留用于存档/重导出，不能随clear草稿删掉所有bundle。目前多次导入取消、无引用bundle、删除最后存档后的安全GC/空间管理还未完整实现；要建立引用跟踪与明确清理入口，不能广泛删整个resource目录。
2. **旧存档迁移**：历史parse已丢图的纯文本存档不能凭空恢复；本轮新增字段只保护新导入/新保存。旧source若仍有原EPUB，要提供受控关联恢复，不能伪造图片或无提示当完整。
3. **完整格式忠实度**：文字编辑后的分章为纯文段+opaque媒体，普通CSS/图片/font样本已证；复杂SVG/MathML/嵌套非文本、srcset/inline CSS绝对url、跨文件内部锚点多fragment、完整嵌套nav等尚需更广fixture与外部阅读器，不宣称所有EPUB任意格式完全无损。未编辑原raw chapter结构保留与“任意编辑仍全样式无损”是不同要求。
4. **原始包边界**：非标准container OPF路径别名、加密资源、原源zip CRC异常、超大asset、12GB+源复制空间不足/取消需要继续；当前source完整SHA防后续修改，不等于验证所有源asset初始CRC。
5. **最终导出文件事务**：Repository.export仍写用户选定SAF流，生成中/最终写入失败时可能留下不完整目标；先生成完整自有临时EPUB再发布、明确最终保存失败与旧目标保护需进一步修，不能用界面报错当成目标字节完整保证。
6. 新资源字段没有强制网站协议变更/外部抓图；原文件上传正向仍通过。但最终优化包的实际文件选择/编辑/存档/再打开/上传返回、不发送或受控实际线上上传、外部阅读器打开、全尺寸UI/权限/无损升级仍未整套认证。
7. 其余8确认案例：metadata未知缺失、校图任务反馈、反应桶优先级、资料部分保存、自动签到重复写、总页数、亮度、badge CSS状态；2未知写回执合同与完整Beta7/58项/新反馈全部继续。**本轮E01最小旧原例绿不能让这些工作或上述新边界消失。**

Next owner: Codex。继续导出提交事务/资源生命周期和完整性反馈、回执等剩余功能。保留外观、五导航、原生下载、公共规则/个人优先、原图/压缩、自定义并发及所有先前增强；不回退或交Gemini重做视觉。没有发布或替换GitHub安装包。
