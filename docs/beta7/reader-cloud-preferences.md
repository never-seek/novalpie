# 阅读器云端配置补齐（进行中）

## 当前网站原理

Nuxt build cbb20abd，`CxFG0gqQ.js` 223239–223946：GET `/api/reader/settings` 列表（裸数组或data数组）；GET query `config_name` 返回`preferences`；POST `{config_name,is_default,...preferences}`；PUT `{id,...preferences}`；DELETE query`id`。`8L_S-edK.js`会由用户选择配置合并到当前readerSettings；本机持续localStorage保存。不是GET一个配置就把本地原生选项全部清空。

此前App“偏好配置”仅跳缓存/重置，无上述任何API，是明确功能缺失。新增独立ReaderPreferencesViewModel/Repository/Codec/Panel，共用原会话与网络请求管线，当前未实际线上写入验证。

## 原生行为和兼容边界

- 主动刷新/选择加载，经确认才合并；新增名称保存、更新现有配置、设默认、删除明确确认。普通调整仍本机自动保存，不自动改网页。
- 兼容字号、行距、系统字体、字重/字间距、内置主题、空行缩进、正文显示、页眉页脚、安全区、内容宽度、翻页/无限滚动/动画与点击区域。
- App音量键设置、公共规则本书/逐条屏蔽、本机字体/背景文件不上传或被网页反向覆盖。轮盘强制保持关闭。网页自定义字体/主题资源映射尚未补齐，界面必须明示。
- 更新前读取现有preferences并覆盖兼容字段，保留网页未映射字段，不擅自删除customFonts/customThemes或其他配置。
- GET/写入身份复用原请求环境；账号切换取消并清理本模块。不会自动重发POST/PUT/DELETE。失败保留名称和提示，不虚报保存成功。

## 证据

- 缺失Codec回归40041两红；54734字段及MockWebServer全协议、外层拒绝不可被内层true遮盖、单请求不重发通过。
- 27944新增“刷新时输入名称不得丢”红，修为await网络之后读取最新state再copy；账号切换迟到返回/失败留草稿通过。
- 71222全量unit/optimizedBeta/lint正在运行，尚未安装此功能。
- 71222的完整单测部分已通过：164suites/1169tests/0失败，optimizedBeta已生成，lint正在运行。当前读取状态来自同一候选源码，不使用旧包证据。

待：当前源真实列表/受控唯一配置创建加载更新删除并清理、不同字号/窄屏/主题可达；网站自定义资源兼容单列，不凭源码字符串判实际成功。

## 2026-09-11 实机闭环

5b58ddbb优化APK，真实原账号首次列表为空；UI创建Beta7-QA-20260911-0344显示保存成功，点击加载得到确认弹窗并应用，更新当前配置显示成功，设为默认后列表出现默认标记；最后删除同一测试条目并刷新回空。没有其他配置被修改；未导出Cookie/Token。证据20260911-cloud-and-performance-runtime.json及cloud-profile-loaded/deleted截图。窄屏字号2.0其他设置按钮虽可达仍有拥挤，完整自定义主题资源映射和数据加载冲突边界仍需后续审计。
