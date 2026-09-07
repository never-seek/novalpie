# 工作区协议与原生缺口（进行中）

源码基线`dSlFh-Ca.js`（Nuxt工作区），页面个人翻译/API/Cookie/状态/上传。以下是源码发现，不等同已调用。

## 已有配置模块

- API列表/新增/编辑/启停/删除，Cookie列表/新增/编辑/启停/删除，统计/翻译器健康分别读取。
- 原生旧load会等4个接口全部返回才发布；WorkspaceViewModelTest先三红后修独立加载、冻结写入、账号失效、明确ack检查。
- 保存失败保留API/Cookie草稿，可重新打开编辑；表单可滚动，不在日志展示key/cookie。账号隔离旧store先红，正在按原登录归属迁移，保留原keys。
- 原有共享取消需要删除当前账号自己共享的远端配置；恢复旧数据不自动共享、启动或执行任何任务。

## 必须实现的真实翻译流程

旧原生`updateWorkspaceJobStatus`只改local jobs，源码没有实际翻译执行器。此功能仍未完成，不能以UI存在宣称已支持。

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
