# 2026-10-08 GitHub 变更链复审与当前修复

## 审查身份和范围

- 当前分支 `codex/native-app2-current-20260821`，HEAD `b4616be2f7f2852c6a8bf86127a804c6a335c8fd`。本次 `git ls-remote` 确认远端 HEAD 和同名分支均为同一提交；没有把旧本地历史冒充远端最新。
- 已重新收集全部 72 个祖先提交、575 个历史变动路径；清单保存在 `agent-bridge/artifacts/beta7-search-retranslate-20261008/github-history-72-commits.json`。这些数量代表历史清单，不代表每个页面都已完成实机验收。
- 保留此前八批及第九批的全部未提交修复；没有 reset、commit、push、tag 或更换 GitHub 资产。
- 对照原 58 项用户验收点、后续 Beta7 计划、当前测试及外置红灯分类，不能因为历史提交标题写“已修复”便关闭需求。

## 本次确认的缺陷及处理

| 项目 | 原因与处理 | 验证入口 |
|---|---|---|
| 搜索清除连带删除所有条件 | `9297dd4` 抽取后的 `SearchViewModel.clearOptions()` 同时清持久快照、重建默认条件、取消请求。现在只清下次启动用的快照；当前 tag 包含/屏蔽、规则、字数、结果、页码和位置不动。 | `SearchConditionRegressionTest`，旧实现 2 条红灯已留存 |
| 保存条件没有直观、有效的独立动作 | 原禁用图标只是切换自动缓存策略，不是“保存”。新增明确的保存图标及反馈；即使原来禁用自动缓存，也可一次保存完整条件并开启保留。原自动保留开关仍保留。 | 同测试 + `SearchRetranslationDeviceTest` |
| 缺少重翻章节 | 原菜单只有“自助翻译”，并非网页的重翻请求。新增原生章节范围对话框、详情菜单和阅读器导航入口；使用 `POST /api/v2/novels/{id}/chapters/retranslate` 的 `chapter_range` 数组。 | `ChapterRetranslationTest`、`ChapterRetranslationProtocolTest`、设备对话框 |
| 当前章重翻可能误当全书 | 使用真实章节序号，不使用 chapter ID 或过滤列表下标；目录迟到时先等待、再定位当前章，定位失败禁止自动全书提交。 | 异步目录回归 |
| 下载 metadata / 正文图源异常没有披露 | 第九批完整性规则继续闭环：要求重试或明确接受保留原文；警告持久保留并可从完成收据恢复，不把来源读取失败当空图配额。TXT 完成不能显示 EPUB 完成。 | `DownloadIntegrityDisclosureRegressionTest`、`DownloadIntegrityDecisionTest` |
| 徽章 hover 样式覆盖常态 | 常态预览只取静态基础 paint，过滤 hover/active/focus 和 keyframes；避免正常 cover 被 hover contain 等污染。未执行网站 CSS/HTML 脚本。 | `BrightnessBadgeRegressionTest` |
| 阅读器缺少亮度 | 新增“显示→亮度跟随系统 / 阅读亮度”；只更改当前 App 窗口，离开阅读器或 Activity 销毁恢复进入前值，不改全局系统亮度、不申请 WRITE_SETTINGS。 | 持久化/窗口恢复单测及设备测试 |

## 重翻的当前网站合同

2026-10-08 从网站公开前端实际读取了当前 `C3JKrkCD.js`、`CRIFFJ6s.js` (RetranslateModalV2) 与 `BTJr4_PB.js`；材料保存在本次 artifacts。网站要求服务端本人 API 为 active、healthy、approved；仅本机模型走工作区自助翻译。新入口区分这两者，不会偷偷调用其他用户共享密钥。

- 支持单章、逗号列表、区间及留空选择全部现有章节。严格校验，拒绝 `1abc`、不存在章节、逆序范围、超大数字。
- 操作在提交时冻结书籍和范围。账号切换、A→B 和旧回执不会串到新对话框。重复点击、关闭后重新打开都不会重复在途提交。
- 明确区分“提交成功”与“后台翻译完成”；正文/目录刷新由用户在任务完成后操作。
- 网络断开或无明确回执显示结果待确认，不自动重发；409 显示已有任务，不重复调用。
- 当前真实网站 API 排队及本人外部模型执行不能用 MockWebServer 或缺模型提示冒充。具体线上执行证据见最终报告；未执行的明确标为未验证。

## 全模块审查结论边界

- 阅读器 / TTS / 替换 / EPUB-TXT / 论坛富文本 / 收藏 / 个人资料 / 上传编辑 / 工作区 / 消息 / 管理权限均保留既有回归测试；本次完整门禁结果另存，不用修改旧期望掩盖丢失能力。
- 历史网站 API 清单 203 行重新比对源码，145 行字符串/模板命中、58 行候选未字面命中。未命中包含查询参数分离、v2 wrapper 与路径拼接，**不是 58 个已确认 Bug**，也不是接口实际调用成功证明。
- 消息设置和工作区的空回执属于仍需合同/实际读回核验的风险；单超大章整章 String、复杂 EPUB 编辑资源生命周期、旧发布无 journal 的历史窗口、OEM/真实大书/完整适配性能矩阵仍未关闭。
- 完整 Beta7 原计划尚不能称全部验收通过；不提前发布“功能完整、无 Bug”的包。当前修复逐项关联新的源码/测试/设备证据，未验事项仍跟踪在 `KNOWN_BUG_REPAIR_STATUS.md`。

## 本轮验证

最终门禁：1456单测/204 suites全部通过；Debug/AndroidTest/Lint/R8构建exit0。MuMu20设备测试通过，优化Beta实际覆盖安装、同签名非debug且hash一致（`d01cdf5fa144a073c7f4319c133e77057ec3f326ef74f911039e00b0fbe7df28`）。真实搜索包含/屏蔽tags经清缓存、保存、冷启恢复；测试后恢复原条件。真实91章书籍的原生重翻对话框通过，线上翻译排队未执行。详见 `D:/NovalPie/agent-bridge/artifacts/beta7-search-retranslate-20261008/GITHUB-HISTORY-REPAIR-REPORT.md`。
