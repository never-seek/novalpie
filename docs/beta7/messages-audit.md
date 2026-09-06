# 消息模块：Beta 7 分片审计

2026-09-06；对应 E07、F03、F05、F07、F10、F11。当前仅分片验收，不能代替整版发布门禁。

## 原网站基线

- 页面 `/messages`：已登录入口。基线源码 `Bjv5odFQ.js`；私信弹窗 `DSvo202g.js`；API wrapper `CxFG0gqQ.js`。
- 列表按类型、已读、优先级、关键词过滤，`page/page_size`分页。网站每次20条；原生保留追加与返回已加载页。
- 私信原站每页100条，按`created_at`升序。原生增加显式更早页入口，不把前100条当全部会话。
- 设置：启用通知/邮件/浏览器推送、通知类型、免打扰、自动已读天数；源码允许0。原站省略空的免打扰/null类型字段，清除语义尚待真实服务器验证，不宣称已修复清空设置。
- 不读取、导出或保存实际私信正文。模拟数据端到端测试只在本进程使用替代仓库，不连接线上私信API。

## 控件与协议对应

以下为源码发现 + 本地协议测试，不等于全部线上写入验证。

| 控件 | 方法/路径 | 主要参数与结果 | 验证约束 |
|---|---|---|---|
| 初始/搜索/过滤/更多 | GET `/api/messages` | keyword,message_type,is_read,priority,page,page_size；list/pagination | 输入草稿与已提交查询分开；统计不阻塞列表；旧query回包不能混页 |
| 消息统计 | GET `/api/messages/stats` | total/unread/read/starred/important/recent_7days/unread_by_type | 失败显示错误，不能success:false归一成0 |
| 消息详情 | GET `/api/messages/{id}` | message或data对象 | 返回id必须等于请求id |
| 已读 | POST `/api/messages/{id}/read` | id | 单次写入、不盲重发 |
| 多选已读/全部已读 | POST `/api/messages/read` | ids或all=true | 点击时冻结ids；success:false不算成功 |
| 星标 | POST `/api/messages/{id}/star` | starred=0/1 | 反向状态精确绑定id |
| 删除 | DELETE `/api/messages/{id}` | id,permanent=false | 保留确认；重复点击仅一请求；迟到回包不能返回另一页面 |
| 批量删除 | DELETE `/api/messages` | ids | 仅所选id；测试不删除用户实际通知 |
| 私信对话/更早消息 | GET `/api/messages/conversations` | target_user_id,page,page_size | 按id合并、时间升序；刷新不丢草稿 |
| 发送私信 | POST `/api/messages` | user_id,execute_user_id,message_type=8,message_title,message_content | 收件人和内容在点击时冻结；草稿按收件人隔离；网络未知结果提示先刷新确认 |
| 读取设置 | GET `/api/messages/settings` | 开关、类型、时间、自动已读 | 拒绝响应不能变默认配置 |
| 保存设置 | PUT `/api/messages/settings` | 与网站相同字段 | 保存快照与后来编辑不同；后者仍提示未保存；账号切换取消旧结果 |

## 已复现的回归

- 旧算法合成测试38802：6项中5失败（刷新丢稿、A发送回包覆盖B、新输入被清掉、明确拒绝误报成功、设置取错快照）。修复后62908：11项通过。
- 协议测试59617：4项全部失败。复现外层`success:false`被内层`data:{}`遮蔽、拒绝读取被当空数据、详情错id和HTTP拒绝被当未知送达。已修，72294全量122 suites/967 tests/0失败、Debug/AndroidTest打包通过。
- `MessageFeatureDeviceTest`实际Compose输入/刷新/发送失败/切收件人两项已通过（3.316s），只用合成文字。开发APK `3e3c4528cfd630082be0f015e81a4ee10edca269942a22a066a81850536fa960`；360dp/Android15/font1.0。报告`beta7-device/20260906-messages-split-device.{json,log}`，截图`20260906-message-conversation-draft.png`已查看，输入框与发送仍可达。这是独立Compose测试页面，不代替生产Scaffold全尺寸验证。
- 生产入口已实际从“工具→打开完整消息中心→消息设置”进入。分类/已读/优先级搜索控件、统计、消息设置及下方免打扰/自动已读输入正常可见，截图`20260906-message-settings-native.png`。未打开实际私人会话，未执行已读/删除/改设置；临时UI结构只在内存过滤固定控件标签。
- 补充77707设置“编辑→刷新失败→重试”丢稿回归先失败；独立dirty标记修复后消息12项已通过，进入APK`1c5cc984...1bb166eb`。不要把较早截图当该额外路径的实机证据。

## 架构

`AppContainer.messagesRepository → MessageInboxViewModel / MessageDetailViewModel / ConversationViewModel / MessageSettingsViewModel`。

各自immutable state与独立子Job，不会关闭注入的父scope；环境变化隔离旧账号。App外壳只负责路由与按钮委派，移除旧约400行共享数据任务。网络协议/正常入口不改成WebView页面。

## 待完成

- 全量门禁与MuMu合成控件验收；低宽/大字体/键盘组合。
- 线上只收集消息控件/请求状态（不记录正文）；真实写入只用明确测试内容且不影响其他用户通知资产。
- 设置可空字段的真实清除协议、私信后台更新策略以及会话进程恢复草稿的隐私边界。
