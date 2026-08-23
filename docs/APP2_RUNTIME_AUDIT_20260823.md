# NovalPie native 2.0 runtime audit — 2026-08-23

本轮针对上一轮未完成项做了最小修复和 MuMu 端到端回归：原生下载入口、音量键翻页、翻页效果设置，以及公开用户主页的作品、动态、签到资料。

## 本轮修复

- 公开用户主页改用完整的 `userContentActivityFeed()`。此前只取 `activities` 列表，丢弃了接口中的帖子和评论聚合计数，所以动态能显示但主页统计显示 0。现在列表和计数来自同一份 feed。
- 新增 `publicProfileLoadPresentation()` 纯逻辑映射，并用回归测试保证活动条目、评论计数和作品计数不会再次分离。
- 保留原生 EPUB/TXT 下载实现：下载请求、正文流、图片并发 staging、MediaStore 保存和临时文件清理均在 App 内完成，不跳回网页。
- 阅读器保留音量下键下一页、音量上键上一页/上一章边界处理，以及 `无动画 / 淡入 / 覆盖 / 滑动 / 仿真` 五种翻页效果。

## 静态验证

在 `D:\NovalPie\native-android` 执行：

```text
:app:testDebugUnitTest :app:lintDebug :app:assembleDebug --offline --no-daemon --max-workers=1 --console=plain
```

结果：598 tests，0 failures，0 errors，0 skipped；lint 无 issue；`git diff --check` 通过。

最终 Debug APK：

- 文件：`D:\NovalPie\native-android\app\build\outputs\apk\debug\app-debug.apk`
- SHA-256：`6CA8F02430BB34023528E2685B7D16536E20E22792B515D25475B048BD795976`
- 已用 `adb install -r` 安装到 MuMu `127.0.0.1:16384`，未清除应用数据；`adb reverse tcp:7890 tcp:7890` 保持有效。

## MuMu 证据

| 流程 | 结果 | 证据 |
| --- | --- | --- |
| 阅读器初始页 | 冷启动后恢复到上次阅读位置 | `D:\NovalPie\agent-bridge\screenshots\20260823-final-installed-home.png` |
| 音量下键 | 视口前进到下一页 | `D:\NovalPie\agent-bridge\screenshots\20260823-final-reader-volume-down.png` |
| 音量上键 | 回到前一页/原章末位置 | `D:\NovalPie\agent-bridge\screenshots\20260823-final-reader-volume-up.png` |
| 翻页设置 | 五个效果可见，“无动画”可选并已选中 | `D:\NovalPie\agent-bridge\screenshots\20260823-final-reader-layout-expanded.png` |
| 我的-动态 | 真实动态卡片正常加载 | `D:\NovalPie\agent-bridge\screenshots\20260823-final-my-activities.png` |
| 我的-书籍 | 上传书籍列表正常加载 | `D:\NovalPie\agent-bridge\screenshots\20260823-final-my-uploaded-books.png` |
| 我的-签到 | 200 天统计和本年签到记录正常 | `D:\NovalPie\agent-bridge\screenshots\20260823-final-my-checkins.png` |
| 公开主页 | `榛名全色` 显示作品 46、评论 180、连续签到 200 天 | `D:\NovalPie\agent-bridge\screenshots\20260823-final-public-profile-100002.png` |
| 公开作品 | 公开主页“书籍”页签显示作品卡片 | `D:\NovalPie\agent-bridge\screenshots\20260823-final-public-profile-100002-books.png` |
| 公开签到 | 公开主页“签到”页签显示记录 | `D:\NovalPie\agent-bridge\screenshots\20260823-final-public-profile-100002-checkins.png` |
| 原生下载入口 | 书籍菜单明确显示原生 EPUB/TXT 下载项 | `D:\NovalPie\agent-bridge\screenshots\20260823-final-native-download-menu.png` |

本轮没有再次点击付费下载确认，因此没有重复消耗积分；此前的真实 EPUB 图片数量审计和清理记录见 `docs/EPUB_DOWNLOAD_AUDIT.md`。

## 进程健康

安装后 App PID 仍为 `8594`。清空 logcat 后的检查未发现 `FATAL EXCEPTION`、`ANR in` 或 `OutOfMemoryError`；启动日志见 `D:\NovalPie\agent-bridge\artifacts\20260823-final-installed-start-logcat.txt`。
