# 上传与追加审计（进行中）

当前来源：20260905网站Nuxt `Dr5_eFLl.js` 的 `rr/nr` 解析、`dt` 提交、`cn/un` 分批创建/追加；原生旧实现NovalPieViewModel约3017–3185行。这里只列已核对源码，不把发现的POST路径标为真实上传验收。

## 已确认协议

- 文件大于50MiB走5MiB原始文件分片，`POST /api/uploads/chunks`，merge后`POST /api/uploads/epubs {file_path,parse_only:true}`。
- 新书`POST /api/uploads/books` multipart，附chapters JSON及对应MD5；追加`POST /api/users/me/chapters/append`绑定existing_novel_id。
- 网页正文JSON超过5MiB时分每50章；创建首批返回novel_id后，其余批次追加到该ID，每批带chunk_index/total_chunks/is_chunked。网页原实现最多重试3次；App不得盲重发创建/追加。
- 网页状态仅连载中/已完结，非成人直接spans=status；成人连载spans=19，成人完结spans=19 完结，同时保留is_adult。
- 网页只有success为真才当确认，新增书ID用于后续目标；缺失或不匹配的回执不能当成功。

## 当前缺陷与下一步

1. 旧默认spans=balanced（分块方式语义），UI没有状态选项；与当前网站不符。
2. 旧normalizeUploadResult缺success默认true，可能把pending/畸形响应当创建成功；追加不校验回执novelId。
3. 旧submit冻结了部分草稿，但在协程执行时仍读取当前serverFilePath，结果直接写全局uploadBookState，切换追加对象后可串页；旧editor生成完成才从当前路由找append目标。
4. 旧新书上传未按源站大JSON创建首批→追加余批，重试/部分成功没有可核对的完整状态。需独立任务状态与检查点，不伪报完成。
5. 原生上传选取与追加、编辑器转入需要独立业务ViewModel/Repository，保留当前UI及失败草稿；不降级网页。

7779新增协议回执/默认status回归正跑旧实现；尚未上传任何真实新书或追加他人的章节。后续实机只用明确标记的自有测试数据，保持真实作品不变。

## 2026-09-08 第一切片

7779三项均红：错书回执继续追加、无success默认为成功、默认balanced。新增UploadBookViewModel/Repository已接root，移出原全局解析/提交约170行；每个目标独立draft/文件/parse/结果，提交包含冻结serverFilePath。编辑器目标在生成前捕获，页面/账号变化不转入其他作品；系统返回恢复对应上传状态。

UI保留原样，仅补连载中/已完结选项、结果未知时明确核对/再提交警告。原生status编码与源成人开关一致。六个独立feature回归覆盖迟到解析、切书回执、不可取消旧响应、失败草稿、重复提交保护/明确拒绝重试和4种状态组合；10314正在验证，尚未安装。

仍未关单：上传大JSON分批新建/追加的持久检查点、跨进程恢复/真实受控上传、编辑器资源上传映射与所有源控件。不能以这次切片声称创作模块已完整。

15:47追加两个协议边界63955红：外层success与内层data不能兼容、外层false被内层true遮住。修为收集envelope明确ack，任一false优先，完全缺失仍未知；不修改原multipart/MD5/授权协议。

源码补查：网页`xn`也将章节正文提取为纯文本；插图另按rawPath/spineIndex和原EPUB/illustrations提交。因此不能只看到原生stripTags就声称“所有上传图片都丢失”，需要受控带图EPUB实际上传对照。原生当前EpubParser将缺失spine文件跳过、只按spine而非TOC片段拆章、读取head文字等仍需具体回归。

编辑器持久存档另发现旧save先删旧JSON/TXT再rename两文件，断电/保存失败有混代或丢稿风险。尚未修，接下来建立故障注入/缺正文/原子发布回归，保留旧存档兼容。

16:09当前切片完整门禁49846通过：151suites/1088tests/0失败，Debug、AndroidTest和lint（0错误5既有警告）全过。APK `6EEB11E434CF0B275CE26E2542B567E2988D8E82A61A8C51AE4E0AEFFA61713E`。MuMu正在启动，尚未安装/运行本切片，不拿之前985ee739包当新上传UI证据。

16:13已经install-r覆盖同6eeb11e4包，首个43991设备用例在寻找尚未组合的LazyColumn底部提交节点时失败，模型请求尚未发生；“已完结”已实际滚到并点击。修测试为父列表performScrollToNode定位未绘制item，而不改产品或降低断言；36226仅重建AndroidTest通过，准备重跑。新增1936源82章邻近只读测试不下载图片、不记录阅读进度。

16:17第二轮36089设备用例通过（20260908-upload-form-correct-scroll）：同6eeb11e4包，真实Android Compose上传界面点击已完结，模拟回执确认spans=19 完结；第一次中断保留草稿，取消重试没有请求，明确核对后再次提交得到ID900013并显示。只模拟2次submit，没有真实上传文件/作品。直接`am start`打开生产`https://novalpie.cc/upload`被工具策略拦截，不通过其他方式重试该被拦操作；这是不同于已通过受控表单的生产入口证据缺口。
