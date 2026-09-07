# Beta 7 已发现事项（不是完成清单）

所有记录均待按 Beta 7 当前 APK 复现/验收。`source-gap` 代表已从网站与 App 源码确认功能缺口，不等同于已修复。

| ID | 来源/证据 | 发现与处理要求 | 状态 |
|---|---|---|---|
| GAP-001 | 登录后 `/user/100164` 实际页面含“屏蔽列表”；`BY0xgLBE.js` 的 `getBlockedUsers/block/unblock/blockStatus`；App 源码未找到相应路由/API/界面；浏览器 `/api/v2/users/me/blocks` 返回 200 | 原生个人页补屏蔽列表及个人主页屏蔽/解除功能，按身份与服务端权限；需要验证屏蔽后 feed 变化，不只增加空页面 | read-verified/native-gap |
| GAP-002 | `DGTbzAP1.js` 的 HTTP wrapper `baseURL=d()`，`d()=>w(2)`，`w(e)=>apiBase+'/v'+e` | 个人新接口属于 `/api/v2/users/...`；不能把源码相对 `/users/...` 误接成旧 `/api/users/...`。静态提取 wrapper 标为 base-unverified，逐条人工解析 | source-confirmed |
| GAP-003 | 源码 `GET/POST/DELETE /novels/{id}/block`，主页已有“屏蔽列表”但 App 尚无对应业务 | 连同用户屏蔽核对作品屏蔽的 UI 入口、权限和列表影响；最终使用对应 wrapper v2 的真实路径 | source-gap |
| GAP-004 | 安卓网页搜索“字数”实际为双端范围滑杆，档位 0/5万/30万/50万/100万/200万/500万/1千万/∞ | App 旧五档筛选不能代表任意最小/最大区间；重构时按实际 API 参数映射补齐，保持原生样式 | runtime-contract-observed |
| FB-1871-4595 | 2026-09-04 醉月摇影：首行与加载提示被页眉截断，章节末页无法续章 | Beta 6 仅有代码/测试补丁，新 Beta 7 真实行分页与非覆盖安全区必须重测 | runtime-pending |
| FB-1871-4546 | 三台设备复现 `360661` EP49 图片加载后漏末段、EP50 返回截断 | 作为固定 reader 示例，按多宽度、图片迟到和反向翻页验收 | runtime-pending |
| FB-1871-4549 | 网页已读后 App 卡片章节同步，但更新提示/继续阅读仍旧 | 统一远端章节与本地段落锚点合并；旧补丁保留为参考，新 state/repository 不能丢 | runtime-pending |
| FB-1871-2387 | 是昔流芳认可逐条屏蔽，但不知道如何添加规则 | 保留屏蔽和手动添加/公共发布入口，优化可发现性；不恢复系统正文复制或不完整轮盘 | design-preserve |
| FB-1904 | 2026-09-03：每章下载多出重复图片 | 校验源出现次数、正文引用、资源去重和独立封面；禁止仅用 ZIP 条目数量判断正确 | runtime-pending |
| FB-1895 | 2026-09-01：临时建议规则替换下载才有图 | 必须把原文/替换两模式分别验收；该帖并非证明原文模式仍有 bug | needs-reproduction |
| FB-1889 | 2382 先报原文无图，2384 后更正为原文也正常 | 保存矛盾回复，不自动认定修复/缺陷；需当前同书和模式对照 | needs-reproduction |
| FB-1913/1892/1886 | 多条反馈 EPUB 下载图片失败，但阅读可见 | 区分 source marker/URL 与 App HTTP/保存失败；显示可重试失败清单，不静默生成无图成功包 | needs-reproduction |
| FB-1893 | 同一本书的书评刷屏，想屏蔽某书评论 | 先核对当前网站屏蔽语义，记录客户端可做的本地阅读过滤；不得扩成后台删除内容 | candidate |
| FB-1878 | 回复消息没有红点 | 核对消息 stats、未读刷新周期、进入/退出会话、后台重开，避免遗漏未读状态 | needs-reproduction |
| FB-1884/1854 | 书内已更新而搜索更新时间/排序未更新 | 核对数据来源与缓存有效期；App 可刷新过期缓存/提示源站延迟，不伪造排序时间 | needs-source-check |
| FB-1879 | 工作区重翻加入后“待翻译章节为0” | 校验所选章节、prepare 响应、队列与开始按钮参数；源站无可用正文时显示原因 | needs-reproduction |
| FB-1917/1920 | 新书未翻出章节/自助翻译缺插画 | 可能为上游内容/服务问题；保留未获取状态与合法重试，不客户端生成虚构正文/图片 | needs-source-check |
| FB-466 | 追加章节表单显示书 A，确认后追加到 B；回复建议每次刷新 | 高风险目标身份问题：请求必须绑定提交时 bookId 和 draft，切书/返回/迟到回包不能写入另一书 | source-feedback/runtime-pending |
| FB-482 | 旧 App 连点工具误触相同位置的退出，且重登配置丢失 | 开菜单的同一次手势不能穿透到新按钮；退出独立确认，阅读配置不与会话清除绑定 | source-feedback/runtime-pending |
| FB-1439/1572/1210 | 评论页码变化但请求仍是 page=1；论坛只有首20条 | 每个 feed/pageId 独立、请求和可见页一致，返回恢复真实页码；不能仅检测 page label | source-feedback/runtime-pending |
| FB-901/833 | 收藏分组过多时选择区裁切，移动弹窗出现在原分组下方 | 嵌套弹窗正确层级、安全区和可滚动列表；受控临时分组验证后只清理测试组 | source-feedback/runtime-pending |
| FB-1217 | 多本千万字 TXT 的 `/api/v2/novels/{id}/download?type=txt` 返回 524 | 先区分源站生成超时与传输失败；保留授权检查点，不重扣积分，不宣称网络重试必能解决源站超时 | source-feedback/runtime-pending |
| FB-1713/447 | 段落空行开关与首行缩进耦合，关空行后缩进失效 | 排版参数独立，组合测试/重排/持久化和实际字符布局验收 | source-feedback/runtime-pending |
| FB-1221 | 详情收藏与阅读器收藏两入口造成重复 | 收藏仓库统一身份和 in-flight 操作，确认服务器状态，不以 UI 两份布尔值各自切换 | source-feedback/runtime-pending |
| ENV-001 | 新 MuMu 管理路径、Android 15 实例正常启动，ADB `127.0.0.1:16384` 可用 | 恢复基线/最终安装验证；保留原用户数据 | resolved-environment |
| ENV-002 | MuMu 已装包 `1d1dcc...`，本地/发布是 `30f07a...` | 基线包须按哈希区分，不把模拟器旧包截图算最新修订验证 | verification-gap |
| ENV-003 | MuMu `tts_default_synth=null` | 后续配置可信中文系统引擎并真实发声；不能用错误提示验收 TTS | pending |
| FB-1871-4620 | 2026-09-05 18:50:29 新回复：应显示尖括号内容，却显示`&lt;例子1&gt;` | ReaderText纯文本快速路径没有解码HTML实体；复现测试已添加，待修复/实机；不把尖括号中内容当标签删掉 | reproduced-in-source/test-pending |
| GAP-TTS-QUERY | MuMu装好eSpeak后App的TTS初始化-1；日志AppsFilter BLOCKED | Manifest增加TTS_SERVICE查询后，真实中文PCM/播放与原生Controller连续两段均通过 | runtime-verified-development-apk |
| GAP-SHARED-LOCAL | 原生规则canSync=false会删除已上传规则，All视图对自己的停用规则硬编码开启 | 本机启停/范围不删公共规则；共享副本按server ID去重；All行展示isEnabled | unit-verified/runtime-pending |
| GAP-HTML-REPLACE | 旧reader在HTML字符串全局替换，URL/属性被人名规则破坏；导出只保一种图片标记 | 共用文本节点派生管线，回归覆盖HTML/Markdown链接/图片/实体/br/非法正则 | unit-verified/runtime-pending |

## 当前线上 wrapper 的人工解析

`BY0xgLBE.js` 导入 `{r as ie}` 自 `DGTbzAP1.js`；后者导出 `T as r`，`T.get/post/delete` 均使用 `baseURL=apiBase+'/v2'`。因此以下为 **source-discovered**，尚未请求/写入验证：

- `GET /api/v2/users/me/blocks`：query `page, limit`，返回 `blocked_users, pagination`。
- `GET /api/v2/users/{userId}/block`：屏蔽状态。
- `POST /api/v2/users/{userId}/block`：屏蔽；无请求正文。
- `DELETE /api/v2/users/{userId}/block`：解除屏蔽；无请求正文。

个人页的本人专属 tab 在源码条件 `m.value` 下追加 `settings/personalization/blocked-users`。不能只按管理员账号一份 UI 推断普通用户入口。

## 2026-09-06 当前补充

- 社区反馈456帖已全文/嵌套回复逐条分类，见community-review.md；读完不等于App验收通过。
- App帖最新4621附图已直接在原网页查看（341×50）：还是`&lt;例子1&gt;`实体露码，不是新的下载或分页诉求。
- 屏蔽原生功能已迁独立Repository/ViewModel，真实空列表与网页一致；未真实屏蔽其他用户资产。查询/POST/DELETE仍按单独证据标记。
- 下载外站资源以前携带站点认证、卡住socket取消要等completion，两回归已复现修复；fixture测试全绿，未打印或导出用户会话。
- Native分页横滑曾漏接、图失败仅空白曾复现，当前开发包四项MuMu回归通过。启动ANR在重启MuMu、无构建并发后未复现，但最终稳定性门禁仍未关闭。
- forum feed已独立状态，分区page/query/scroll恢复、切账号缓存隔离4项测试；实际帖子/书评流需继续不同分区分页和返回验收。
- 派生文本尚需追加边界测试：替换输出新Markdown图片/格式符时不能变成新结构，尤其plain-text路径再走Markdown解析；目前HTML属性/原图片URL保护已有测试，不等同整个Markdown AST已安全隔离。
- 消息原生旧实现已确认：刷新对话会丢未发草稿；发送A回包可能清空/重新导航覆盖正在编辑的B；发送中输入的第二条会被清空；HTTP成功但`success:false`也当作发送成功；设置保存捕获的是协程运行时的可变草稿，回包会误标后来改动为已保存。新feature切片先保留这些旧算法运行合成回归，不连接真实私信服务，修复后再接root。
- 当前网页消息设置源码 `Bjv5odFQ.js:~9019`：保存会省略空的免打扰时间/null通知类型，自动已读0可发送；需要实测服务器如何清除这些可空字段，不用猜测的null协议直接冒充已支持。对话源码 `DSvo202g.js:~1310` 用`target_user_id/page/page_size=100`读取，并按created_at升序呈现。
- 消息协议回归已修`success:false`被data包装吞掉、错误消息ID与HTTP拒绝说明；相同`normalizeForumActionResult`仍有外层success被包装隐藏的风险，需要独立回归并覆盖论坛/章评/进度，而不能认为消息修复已覆盖所有action结果。
- 替换同步待迁移风险：成功创建回包仅在当前bookId相等时绑定server ID，离开本书可能丢绑定；修改source先删旧再create，后一步失败造成远端旧贡献消失；多个Update可乱序；公共列表加载只有bookId检查没有代次。下一切片按规则身份/账号/操作串行及持久检查点处理。
- TTS慢网本地进度阻塞已先失败修复；实际353686第8→9章后台/首句/熄屏/暂停恢复通过。远端进度独立GET回验、用户主动重试入口、同段重复文本的准确跟随仍待补齐。
- 2026-09-07工作区审计：`WorkspaceScreens.workspaceQueueItems`暂停/继续绑定root `updateWorkspaceJobStatus`，只写WorkspaceLocalStore状态；全源码除store解析没有新建WorkspaceTranslationJob或实际job执行器。故旧“本机翻译任务与进度”不能按按钮存在记为业务通过，需按网站自助翻译协议/源权限实现真正执行或清晰迁移，不能伪报暂停。
- 公开/本人动态旧合并四个源分页后再take(limit)会漏掉本页已取回但未显示条目；已先红后修不截并集/hasMore/partialFailure同页重试。帖子及书评流另有明确author过滤缺口，同样已先红修复；当前这一批尚未设备验收。
