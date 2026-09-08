# 工作区协议与原生缺口（进行中）

源码基线`dSlFh-Ca.js`（Nuxt工作区），页面个人翻译/API/Cookie/状态/上传。以下是源码发现，不等同已调用。

## 已有配置模块

- API列表/新增/编辑/启停/删除，Cookie列表/新增/编辑/启停/删除，统计/翻译器健康分别读取。
- 原生旧load会等4个接口全部返回才发布；WorkspaceViewModelTest先三红后修独立加载、冻结写入、账号失效、明确ack检查。
- 保存失败保留API/Cookie草稿，可重新打开编辑；表单可滚动，不在日志展示key/cookie。账号隔离旧store先红，正在按原登录归属迁移，保留原keys。
- 原有共享取消需要删除当前账号自己共享的远端配置；恢复旧数据不自动共享、启动或执行任何任务。

## 必须实现的真实翻译流程

旧原生`updateWorkspaceJobStatus`只改local jobs，原源码没有实际翻译执行器。2026-09-07已新增独立协议、逐块检查点、实际模型调用与完整章节提交、队列和前台服务，正在回归验证；尚未通过实机完整执行，不能以UI存在宣称已验收。

网页真实流程：

1. `GET /api/translations/chapters/raw?novel_id&untranslated_only=1[&chapter_id]`：需要站点登录，返回`success,data`；只处理pending/failed/user_translate真实状态。
2. `GET /api/translations/prepare?novel_id&chapter_id`：返回`novel_id,chapter_id,chapter_title,total_chunks,chunks[index,content,glossary]`，词表值取target。
3. 按用户自己配置的模型服务发送OpenAI兼容chat/completions。请求包含model、messages、temperature/top_p/frequency_penalty与JSON mode；原站期望逐行数字key或translation格式。不在App追加付费默认云服务，不取外部书源。
4. 对每块验证完整行/空行/图片标记/有效内容；不能像旧网页fallback用正则挖残缺JSON然后默默补空行。
5. `POST /api/translations`提交`novel_id,chapter_id,translated_content,translated_title,terminology_updates{table_add,table_remove},translation_stats{translator_type,model,tokens_used,translation_time,success}`。原站会重试3次；App不得盲重放非幂等提交，网络未知必须确认状态后手动重试。
6. 原站还有`POST /api/translations/save`但executeTranslationJob走的是上面的submit；不混用协议。只有全部指定章节提交确认才完成，部分失败保留清单/进度，不能“部分成功”也当全部完成。

原生计划：单独前台dataSync服务/可恢复任务和每块检查点，配置ID引用而不将key写进任务日志；开始前明确目标书/账号/模型和消耗提示。停止/暂停/恢复必须控制实际请求，取消不提交半章。App现有功能入口不改成整页WebView。

## 证据与待验收

- 配置VM49677三测试先红、48186修后三绿；50051接root全量1015tests/Debug/AndroidTest通过，尚未实机。
- 95223账号隔离与工作区明确拒绝两测试先红，当前修订82679定向验证中。
- 未读取用户实际配置secret；实际外部模型测试仅使用授权配置或mock，不能泄露第三方密钥，不能发布给其他用户。
- 真队列、配置UI失败草稿恢复、账号切换迁移、权限与实际source写入门禁仍未关闭。

## 2026-09-07 当前协议路径纠错

基线HTML runtime明确`apiBase=https://novalpie.cc/api`；dSlFh-Ca的`/workspace/...`均拼在apiBase后。原生旧代码错误地直接用根站点`/workspace/...`，旧WorkspaceApiTest也固化了这个错误。

新增`WorkspaceCurrentRouteTest`81739先红，修全部13个工作区请求为`/api/workspace/...`（GET/POST/PUT/DELETE/状态），不改页面路由`/workspace`。相应旧协议测试改为当前网站已核对路径，参数/次数等断言保留，不能以旧测试通过掩盖错接口。电脑匿名GET `/api/workspace/stats`实测200/application-json/552字节，只记录状态/类型不记录配置值。

当前data.apiStatus是翻译状态数组，旧原生当汇总object导致全0；36304先红，修为translationCounts与从translators归纳API数。22708全量1019tests/Debug/AndroidTest通过。

MuMu当前开发APK `eac6038ced0d84192f86627991b49e80a8123de318f8abf9d0c35b17b04cd598` 实际显示API383/健康18和7类翻译状态计数；截图`20260907-workspace-status.png`已查看。`WorkspaceDraftDeviceTest`在9862376c与本包两次通过，只用合成数据；实际配置未改，secret未导出。账号无损迁移/未知身份确认恢复已代码测试，完整执行队列仍待实现。

## 2026-09-07 真队列回归（未验收）

- 新`TranslationRunner/Coordinator/Service/TaskStore/CompatibleTranslationModel`；每章全部分块成功才提交，源序合并，保留原行及插图标记，配置只引用ID，任务记录不写入Key/Cookie。
- 模型请求使用单独client，不继承站点会话；禁自动重试与重定向，8MB响应上限，每次读取当前代理；并发最大8个在途分块。
- 46263新9项出现3个实际红灯：章节退出待翻列表被误认提交成功、停止掩盖待确认状态、确认后配置删除导致异常。86037修后9项通过。
- 60263追加账号切换/暂停/多任务并发后16项中2失败；59858保留断言记录实际task.json确认为旧Queued。根因是Android15 AtomicFile的覆盖rename在Windows JVM不具备相同行为，finishWrite仅日志不抛错。生产store增加提交字节读回核验，禁止写入未确认仍继续POST；JVM原子恢复用API28备份算法，安卓15相同行为另设MuMu实文件用例，不以改期望值放行。13558定向19项全部通过。
- 67679全量1039项只新增畸形术语用例失败；已校验table_add的source_name/target_name/info及table_remove字符串结构，未知删除项不得提交。91994完整unit/Debug/AndroidTest进行中。
- 工作区任务列表改为单一LazyColumn，长队列不会一次性组合全部卡片；书籍详情按原有来源/登录条件增加本人API自助翻译入口，预填书籍ID并仍要求二次确认。不删除旧功能、不换主题。
- 尚未执行任何真实模型调用或网站译文写入；不读取/使用别人的共享Key。MuMu仍关闭以供构建使用内存，最新真队列开发APK尚未安装。

### MuMu 当前包验证（2026-09-07 15:49）

`C04EE1E3E8D2200A3F8F5360A3523B1C00FA6D33FC936929DAB2FCB78436CEE4`已无损install-r，77506完整unit1040/Debug/AndroidTest通过。`BackgroundTaskPermissionDeviceTest`实际系统通知弹窗授权后动作只执行一次通过，`TranslationQueueDeviceTest`实际安卓15前台服务：HOME后台→通知暂停→在途块完成落盘但不派下一块→继续→完整提交确认落盘；第二任务提交中停止服务→SubmissionUncertain持久化→普通Resume不再POST，全部通过。源与模型为合成，不声称真实外部模型质量或站点译文写入通过。

证据：`agent-bridge/artifacts/beta7-device/20260907-background-permission-dialog.{json,log}`、`20260907-translation-queue-final.{json,log}`。测试只清理自己cache目录，用户配置、登录和下载保留。临时通知授权在完成后台回归后恢复原设置。

| 接口 | 当前证据级别 | 原生行为 |
|---|---|---|
| GET /api/translations/chapters/raw | 当前Nuxt源码发现 + MockWebServer协议验证，未真实读回 | 只选pending/failed/user_translate，不以列表缺项推断先前提交成功 |
| GET /api/translations/prepare | 源码发现 + 协议验证，未真实读回 | 校验书章身份、完整块数、块序与词表 |
| POST /api/translations | 源码发现 + 协议验证，未真实写入 | 提交前落盘、完整章提交、明确ack才完成；断线保持SubmissionUncertain |
| 用户配置的 /chat/completions | 合成HTTP协议测试 | 只发送本人模型Key，不发送站点Cookie；不重定向、不自动重试，可取消、动态代理、响应大小上限 |

2026-09-07 16:26补充：当前c04ee1e3包以原App会话实际GET测试书353686返回0候选，`20260907-workspace-translation-read-report.json`记录candidates=0、prepared=false、writeRequests=0、modelCalls=0。只升级raw接口为空响应实际读取证据，prepare和submit仍未真实写入验收。

## 2026-09-08 重试范围与检查点完整性

13152新增三回归全红：失败重试纳入新到章节、原任务未完成候选消失却显示Completed、修改可读JSON的译文未被发现而提交。修后64923全部翻译定向通过。

- 首次读取冻结目标章ID/顺序，保存后才允许本人模型调用；schema2兼容读取schema1，旧未冻结且已开始的任务不猜范围、不假完成，保留记录并提示核对后重建。
- 后续仅处理原定未完成章节。原定候选暂消失则明确未确认，已提交回执和总目标数不减少；新增章节需要新任务。
- 分块/标题保存原输入身份及结果SHA256，复用时重验行/空行/图片标记/词表结构。损坏或不完整旧检查点不偷偷重新调用收费模型，也不继续提交。
- 输入hash使用有边界的JSON序列和排序词表，避免直接字符串拼接碰撞及Map顺序带来的无谓重翻。
- 追加旧record迁移、旧部分任务不猜范围、标题损坏回归；89054完整unit/build/AndroidTest/lint进行中。未安装本修订，不声称真实外部模型/网站译文写入已验。
