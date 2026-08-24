# 社区反馈复核（2026-08-23）

复核对象：论坛帖子 `1828`《原生app下载》。评论通过只读接口
`GET /api/posts/1828/comments?page=1&limit=100` 获取；现场证据保存在
`D:\NovalPie\agent-bridge\artifacts\20260823-post-1828-comments.json`。

## 已验证

- 回复嵌套：最新评论仍能展开，点击 `xiase` 的回复会填入 `@xiase`；此前修复的根评论
  `comment_id` 逻辑已由单测和 MuMu UI 树覆盖。本轮没有发送新评论。
- EPUB 原生下载：MuMu 实际完成了一次 `359136` 下载，显示“EPUB 已保存到下载目录”。
  临时文件和对应 MediaStore 条目已在审计后删除。
- EPUB 完整性：旧版现场产物审计为 125 个章节 XHTML、637 个正文图片条目、637 个正文
  引用、0 重复 ZIP 路径、0 读取/CRC 错误；566 个唯一图片内容和 71 组重复内容来自不同
  occurrence，不能按 hash 删除，否则会丢章节插图。
- 长按预览：当前静止长按阈值在系统值上增加 400ms；达到 touch slop 会取消预览。具体
  的 800/950/1000ms MuMu 复测记录见文档末尾的 2026-08-24 小节。

## 发现并修复

早期原生包只有 `cover-image` 元数据，没有网页生成器使用的独立 `cover.xhtml` 封面页。
这会让部分 Android EPUB 阅读器不显示封面。现已补充封面页、manifest 项和 spine 项，
同时保留原始 WebP/GIF/PNG 字节和媒体类型，不转码、不增加第二份封面图片。

## 仍需区分的反馈

- 评论中“网页 3700+、App 2500”对应的作品是 `356636《变成魔法少女了??》`。源站目录
  的 `image_count` 合计 2474 与正文实际 occurrence 不是同一统计口径；本书的当前原生
  产物已在文档末尾单独审计，不再用 `359136` 的结果替代它。
- “番茄/起点打不开”不能仅由 MuMu 验证：当前模拟器没有这些第三方阅读器包。当前包已
  具备标准 container、OPF、nav、章节 XHTML、原始图片和封面页结构；第三方应用兼容性
  仍需在安装对应阅读器的设备上实测。
- “网页正文空白”是 WebView/模拟器渲染链路问题，不能用 native reader 已通过的章节加载
  结果代替；应单独用 WebView 页面截图复测。
- “掉帧”需要单独做 gfxinfo/Perfetto 采样；本轮没有把静态构建通过误当成性能修复。

## 当前门禁

本轮长按变更的定向测试已通过；完整门禁为 604 tests、0 failures、0 errors、0 skipped，
lint 和 `assembleDebug` 均通过，详见文档末尾的 2026-08-24 小节。

## 2026-08-24 最新评论 4396/2305 复核

本次针对帖子 `1828` 中 `xiase` 的评论 `4396` 及回复 `2305` 做了逐项复核；2026-08-24
再次读取 `GET /api/posts/1828/comments?page=1&limit=100`，评论原文也保存在
`D:\NovalPie\agent-bridge\artifacts\20260823-post-1828-comments.json`。
回复描述的目标作品是 `356636《变成魔法少女了??》`，本轮取得的原生 EPUB 审计产物和
源 TXT 副本分别为：

- `D:\NovalPie\agent-bridge\artifacts\20260824-target-356636-epub-audit.json`
- `D:\NovalPie\agent-bridge\artifacts\20260824-target-356636-source.txt`（临时源 TXT 副本，
  审计完成后已删除；计数与结论保留在下方和 JSON 审计文件中）

### 四项反馈的结论

1. **“没有封面”**：已修复并实测。EPUB 有独立 `cover.xhtml`，且 OPF 同时包含封面页、
   封面图片 manifest 和 spine 项；封面图片字节与源封面一致。
2. **“番茄/起点打不开、多看看不到插画”**：不能宣称全部修复。当前 EPUB 的
   container/OPF/nav/XHTML、媒体类型和 CRC 均通过，但 MuMu 没安装番茄、起点或多看，
   因此第三方阅读器的兼容性仍需在安装对应阅读器的设备上实测。
3. **“插画重复”**：没有发现原生 writer 额外制造 occurrence。源 TXT 有 3752 个图片
   占位符，EPUB 也有 3752 个正文图片条目和 3752 个 XHTML 引用；源内容本身存在 552
   组相邻相同图片内容、共 1235 组重复内容复用。重复内容来自源文档的不同 occurrence，
   不能按 hash 删除，否则会丢章节插图。
4. **“图片数量与网站不符”**：`/chapters` 的 `image_count` 合计是 2474，但源 TXT 实际
   图片占位符是 3752；当前 EPUB 按网站生成器的可观察 occurrence 契约输出 3752 条，
   没有缺失引用、失败图片标记或 ZIP/CRC 读取错误。因此 2474 与 3752 是目录统计字段
   和正文实际 occurrence 的口径差异，不能据此判定 App 少下载了图片。

本次 356636 EPUB 还通过了 130 个章节 XHTML、1 个独立封面图片、0 缺失引用、0 读取/CRC
错误、0 重复 ZIP 路径检查；文件大小为 5,763,394,051 字节。原始 ZIP 审计详见
`D:\NovalPie\agent-bridge\artifacts\20260824-target-356636-epub-audit.json`。

### 长按预览复测

用户反馈长按过早触发时，旧实现为系统长按阈值 +250ms（MuMu 约 750ms）。本轮先让
“800ms 不触发、899ms 不触发、900ms 触发”的回归测试在旧代码下失败，再把额外等待调整为
400ms（MuMu 约 900ms）。同时保留 touch slop 取消规则：指针达到滑动阈值后，无论按住多久都
不打开预览。

MuMu `127.0.0.1:16384` 实测证据：

- 800ms 静止按压：未打开大图（普通卡片释放后进入详情）——
  `D:\NovalPie\agent-bridge\screenshots\20260824-followup-final-longpress-800.png`
- 950ms 静止按压：打开封面全屏预览——
  `D:\NovalPie\agent-bridge\screenshots\20260824-followup-final-longpress-950.png`
- 1000ms 且手指移动：未打开预览——
  `D:\NovalPie\agent-bridge\screenshots\20260824-followup-final-longpress-scroll-1000-large.png`

本轮全量门禁为 82 个 XML 结果文件、604 个测试，0 failures、0 errors、0 skipped；lint 和
`assembleDebug` 均通过。APK SHA-256：
`EC8E7FA38423BEABA3CA2B143D3F1E59311B3EB93D13F678A8AB0CCFD49FB6E5`。
