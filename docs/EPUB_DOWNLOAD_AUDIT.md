# EPUB 下载审计（2026-08-13）

## 结论

当前手机端网页下载路线可以继续沿用：网站在 WebView 内生成最终 EPUB `Blob`，原生层只负责把这份已经生成的字节流保存到 Android `Downloads`，不会重新解析章节、重新抓图或重新打包 EPUB。

现有证据不支持“当前下载桥重复插图”的结论：审计到的 EPUB 都没有重复 ZIP 路径。个别文件存在相同图片内容被保存为两个不同图片条目的情况，但它们分别被不同章节引用，这是网站按章节生成资源的结果。下载端不能按图片哈希删除，否则可能使某个章节缺图。

## 旧 WebView 版本与当前方案

旧版本里有两条不同的 EPUB 路径，不能混为一谈：

1. `blob:` 传输路径：网页先生成 EPUB，App 把最终 Blob 保存到 Downloads。这条路径适合手机大文件，且不会自行插入图片。
2. 旧 App 内重打包路径：从 TXT 重新拆章节、扫描图片 URL、逐张下载后用原生 ZIP/EPUB 写出。这条路径会再次处理插图；即使它按 URL 缓存，等价图片的不同 URL、查询参数或重定向仍可能变成多个资源条目。这是历史“重复插图”反馈最可能的来源。

当前 App 仅保留第 1 条。`WebDownloadBridge.kt` 和它的页面注入脚本不包含 `JSZip`、章节解析、图片 URL 扫描、图片压缩或 EPUB 写入逻辑；它只写入网页已生成 Blob 的原始字节。

当前网站生成器会为每一个插图占位符创建稳定图片索引和 ZIP 文件名（`img_0001.jpg` 等），再由每章 XHTML 按自己的占位符引用相应的条目。因此审计时应区分：

- 重复的 ZIP 路径：异常，需要修复。
- 相同内容哈希但不同路径、且被不同 XHTML 引用：源站生成结果，不应在下载端删除或合并。

## 网页现场

- 页面：`https://novalpie.cc/book/354491`
- 入口：书籍详情 -> 操作菜单 -> 下载 -> EPUB
- 网页文案：`EPUB 1 积分 客户端打包（图片直连下载，不经服务器）`
- 当前账号显示为需要支付 1 积分确认；本次只打开确认页，没有提交支付或下载。
- 证据截图：
  - `D:\NovalPie\native-android\qa-screenshots\turn132-webview-download-audit\01-before-download.png`
  - `D:\NovalPie\native-android\qa-screenshots\turn132-webview-download-audit\03-download-menu.png`
  - `D:\NovalPie\native-android\qa-screenshots\turn132-webview-download-audit\04-epub-payment-confirm.png`
  - `D:\NovalPie\native-android\qa-screenshots\turn132-webview-download-audit\05-downloads-after-no-submit.txt`

## ZIP 审计结果

| 文件 | ZIP 条目 | XHTML | 图片条目 | 重复路径 | 唯一图片哈希 | 同哈希组 |
| --- | ---: | ---: | ---: | ---: | ---: | ---: |
| `verify-epub-current.epub` | 134 | 109 | 15 | 0 | 15 | 0 |
| `verify-epub-regress.epub` | 134 | 109 | 15 | 0 | 15 | 0 |
| `竹林管理员 (13).epub` | 895 | 833 | 52 | 0 | 50 | 2 |

`竹林管理员 (13).epub` 的两组同哈希资源分别是：

- `chapter-1.xhtml -> img_0001.jpg`，`chapter-2.xhtml -> img_0002.jpg`
- `chapter-566.xhtml -> img_0016.jpg`，`chapter-567.xhtml -> img_0017.jpg`

这说明是跨章节资源复用/复制，不是同一个 ZIP 路径被写入两次，也不是原生传输追加了第二份图片。

## 当前桥接的防重复策略

`WebDownloadBridge.kt` 目前包含以下保护：

1. document-start 注入，避免网页早期创建 Blob 时错过拦截。
2. 对同一 Blob 使用 `WeakMap` 身份 key；多个 `blob:` URL 不会各自创建独立下载 key。
3. 同一 URL 的点击拦截与 `DownloadListener` 共用一个 Promise，避免双击竞态；同一 Blob 的完成状态也按 `WeakMap` 身份 key 共享，因此下载完成后页面即使再创建新的 `blob:` URL，也不会重传完整文件。
4. 同一 Blob key 的原始 Blob HTTP 传输与分块 fallback 共用一个 Promise；原始 Blob 失败时再使用同 key 的分块路径。
5. 失败不会标记 URL 为完成，下一次点击可以重试。
6. 原生 `WebDownloadIdempotencyGate` 在传输中阻止重复文件，成功 key 保留 10 分钟；失败会释放 key。
7. Android 10+ 使用 `MediaStore.IS_PENDING`，只有完整写入后才发布到 Downloads；失败会删除临时条目。
8. 页面重载遇到原生仍在进行的同 key 传输时，`IN_PROGRESS` 不会被误报为成功；该 URL 会保持可重试。
9. 路由离开时会显式停止并销毁 WebView、解绑 document-start 脚本、关闭活动本地上传 socket，并删除未完成的 `IS_PENDING` 临时文件。
10. 已经发布到 Downloads 的完整文件不会因响应 socket 随后断开而被删除；同 key 仍保持幂等。
11. Compose 重组不再根据 WebView 当前 URL 重载页面；只有目标 URL、代理或认证状态真正变化时才重新导航，避免下载页重复执行网站生成器。
12. 代理设置失败回调也会校验目标 URL 和页面状态标记；用户已经离开页面时，旧回调不能重新加载旧的下载页。
13. 原始 Blob HTTP 上传优先，避免大 EPUB 在 WebView 中额外生成大量 Base64 临时字符串；分块桥仅作为兼容回退。
14. 下载桥避免 API 24 才有的集合操作，保持 `minSdk 23` 兼容；JavaScript 入口的 `@JavascriptInterface` 标记经过 lint 验证。

## 与旧 WebView 版本的差异

旧版 Blob 传输方向是正确的：直接保存网页最终 `Blob`，而不是在 App 内重建 EPUB。历史源码中另有 `writeEpubFromText` / `getOrDownloadImage` 重打包链路，不应恢复为下载功能。旧传输实现仍有四个竞态窗口：

- 每个 `blob:` URL 都单独发起传输，同一个 Blob 被网页创建多个 URL 时没有共享身份 key。
- 旧版 HTTP 上传没有共享传输 key，失败/重试时可能再创建第二个文件；当前实现用同一原生 key 保护回退。
- `URL.revokeObjectURL` 过早清理映射，以及重复点击没有共享 Promise，可能导致一次点击失败、再次点击重新开始。
- WebView 页面重组和路由释放没有完整销毁边界，可能让旧页面继续运行或让半完成会话残留。

当前实现保留了旧版适合手机的大文件原始 Blob 路线，并以分块桥作为兼容回退；同一 Blob/URL 的所有入口共享幂等 key 和 Promise。这样不会修改网站 EPUB 内的图片资源，也不会为了“去重”牺牲章节完整性。

## 验证

- `:app:testDebugUnitTest`：通过
- `:app:testDebugUnitTest --tests com.novalpie.nativeapp.ui.WebDownloadBridgeTest`：通过
- `BLOB_DOWNLOAD_SCRIPT` Node JavaScript 语法解析：通过
- `:app:compileDebugKotlin`：通过
- `:app:assembleDebug`：通过
- `:app:lintDebug`：`0 errors, 27 warnings`（仅现有 AGP/依赖版本提示）
- 全量 Debug 单测：70 suites、416 tests、0 failures、0 errors、0 skipped
- WebFallback 重载边界与下载桥生命周期回归：通过
- MuMu `127.0.0.1:16384`：本轮 `adb install -r` 成功，应用启动正常，`tcp:7890` reverse 保持有效；未清除应用数据、登录态或阅读进度。
- WebView 生命周期：已从“打开网站”进入登录同步的网站首页，返回后正常回到原生应用设置；未点击下载、支付或写操作。
- 最终 Debug APK SHA-256：`D326AA7308B43E0ABF3D7BF6FF84724F9B5638EAB3A9F04387273ABBA66BC3A9`
- 本轮截图：
  - `D:\NovalPie\native-android\qa-screenshots\turn133-after-install.png`
  - `D:\NovalPie\native-android\qa-screenshots\turn133-web-home.png`
  - `D:\NovalPie\native-android\qa-screenshots\turn133-web-home-back.png`

## 后续复测方式

要验证一次真实付费下载，只需在网页确认页点击一次“支付 1 积分并下载”，然后记录 Downloads 下载前后的文件列表，并对新增 EPUB 做 ZIP 审计。不要在下载端做图片哈希去重；应检查章节 XHTML 是否各自引用了对应图片条目。

推荐回归：在下载页连续触发两次入口、以及下载中离开页面一次。前者应只产生一份完整文件；后者不应留下可见半文件。新 EPUB 的 ZIP 路径必须唯一，但相同图片内容的不同路径需要结合章节引用再判断。

## 2026-08-13 旧 WebView 下载链路复核

用户反馈旧 WebView 版本的下载体验更适合手机端，因此重新比对了旧壳层与当前原生桥接。

- 旧壳层的有效部分是 `fetch(uploadUrl, { body: blob })`：网页完成 EPUB 生成后，最终 Blob 原样流入 Android Downloads。
- 旧壳层没有 Blob 身份 key、并发 Promise 或原生下载 gate；重复点击、`DownloadListener` 与锚点点击竞态可能生成第二个文件，但不会把两次结果拼接到同一个 EPUB 内。
- 旧壳层额外的低内存图片压缩注入会在网页生成 EPUB 前重新处理图片，不适合迁入当前实现：它会改变原图质量，也无法从根本上处理站点自身的图片索引语义。

当前桥接已调整为：

1. 原始 Blob HTTP 上传优先，避免大文件逐块转 Base64 所产生的额外字符串和内存峰值。
2. 分块桥仅作为 WebView 无法向 localhost 流式 POST Blob 时的兼容 fallback。
3. 两条路径复用同一个 `WeakMap` Blob key、页面 Promise 和 Android `WebDownloadIdempotencyGate`；HTTP 响应丢失后触发 fallback 时也不会发布第二个文件。
4. App 不打开、不解析、不修改 EPUB ZIP 内容，不扫描插图 URL，也不会根据图片哈希删除或合并条目。

本轮已完成：

- `WebDownloadBridgeTest`：通过。
- `BLOB_DOWNLOAD_SCRIPT` Node 语法检查：通过。
- `:app:testDebugUnitTest`：70 suites、416 tests、0 failures、0 errors、0 skipped。
- `:app:lintDebug`：0 errors、27 warnings（仅 AGP/依赖版本更新提示）。
- `:app:assembleDebug`：通过。
- MuMu `127.0.0.1:16384`：Debug APK 已 `install -r` 覆盖安装，保留数据；`tcp:7890 -> tcp:7890` reverse 已恢复；应用运行正常，未产生新的下载文件。
- 三份本地 EPUB 样本均为 134 ZIP 条目、15 图片条目、0 重复 ZIP 路径、15/15 图片被 XHTML 引用；未发现桥接追加插图的证据。

本轮运行截图：

- `D:\NovalPie\native-android\qa-screenshots\turn135-download-bridge-runtime.png`

实际“支付积分并下载”的 EPUB 流程未在本轮触发。若要做最终真机验收，只应在用户明确授权后点击一次确认按钮，随后记录 Downloads 增量并对新增 EPUB 进行同样的 ZIP 条目和 XHTML 图片引用审计。

## 2026-08-13 follow-up：重复插图风险定位

本次进一步对照了已保存的网站前端生成器 `D:\NovalPie\site-js-current\e4j1Urtp.js`：

- `de(text)` 会为正文中每一次插图占位符创建一个 image descriptor，并递增 `index`。
- `ye(descriptors, ...)` 按 descriptor index 生成 `img_0001.jpg`、`img_0002.jpg` 等文件名。
- `cacheKey` 只用于复用网络图片缓存；它不会把两个占位符合并成同一个 EPUB 条目。
- 生成器随后对每一个有效 descriptor 调用 ZIP 的 `file("OEBPS/Images/" + filename, data)`。

因此，若同一张图片被两个章节分别引用，网站本身可能生成两个不同路径但内容相同的图片条目。这不是 WebView 传输追加，也不能在下载端按 hash 删除；删除会改变网页生成结果，甚至让章节引用失效。

旧商业壳层 `commercial-app/source/android/app/src/main/java/com/novalpie/app/MainActivity.java` 还保留过另一条历史路径：`writeEpubFromText` -> `writeParagraphs` -> `getOrDownloadImage`。这条路径会在原生端重新解析文本、重新下载插图并写 ZIP；它只按原始 URL 做缓存，且可能同时把首张资源写成 cover 和普通图片条目。该路径才是“插图重复”反馈的高风险来源。当前 `native-android` 下载入口没有调用它，也没有调用 `EpubWriter` 来生成网站下载文件。

本次对现有历史 EPUB 的复核结果：

| 文件 | ZIP 条目 | XHTML | 图片条目 | 图片被 XHTML 引用 | 重复 ZIP 路径 | 同内容不同路径 |
| --- | ---: | ---: | ---: | ---: | ---: | ---: |
| `commercial-app/verify-恋爱喜剧里的路人太帅了.epub` | 133 | 108 | 15 | 15 | 0 | 0 |
| `commercial-app/verify/恋爱喜剧里的路人太帅了 (1).epub` | 133 | 108 | 15 | 15 | 0 | 0 |
| `commercial-app/verify/downloads/latest-zhu-lin-current.epub` | 895 | 833 | 52 | 52 | 0 | 2 |
| `commercial-app/verify/竹林管理员 (10).epub` | 895 | 833 | 52 | 52 | 0 | 2 |

两组同内容图片分别只被相邻章节引用：`chapter-1.xhtml` / `chapter-2.xhtml` 使用 `img_0001.jpg` / `img_0002.jpg`，`chapter-566.xhtml` / `chapter-567.xhtml` 使用 `img_0016.jpg` / `img_0017.jpg`。现有证据不支持“桥接把一张插图追加了两次”。

后续真实设备验收按以下标准执行：

1. 连续触发同一个网页下载入口两次，只允许 Downloads 增加一份完整文件。
2. 下载中离开页面，不得留下可见的半文件或 `IS_PENDING` 条目。
3. 新 EPUB 的 ZIP 路径必须唯一；图片条目必须能被章节 XHTML 解析到。
4. 同内容图片只有在不同章节确实引用时才保留，不按 hash 擅自合并。

本地桥接仿真 `tools/verify-web-download-bridge.mjs` 也覆盖了同一 Blob 创建多个 `blob:` URL、同一 URL 连续触发、以及完成后重新创建 URL 的竞态：网页点击与 `DownloadListener` 共享一次原始 Blob HTTP 传输，成功时不会进入分块 fallback；原始上传失败时也只创建一个分块会话，且回退字节与原 Blob 完全一致。该仿真不访问网站、不消耗积分，也不读取登录态。

## 2026-08-22 图片数量与 EPUB 体积差异复核（book 350192）

本轮针对社区反馈“同一本书 App 与网页端下载的图片数量不一致，体积也大几倍”重新对照了已保存的网页生成器 `D:\NovalPie\site-js-current\e4j1Urtp.js` 与原生下载入口。

源站目录接口 `GET /api/v2/novels/350192/chapters` 的只读基线已保存为
`D:\NovalPie\agent-bridge\artifacts\20260822-book-350192-chapters.json`：

| 指标 | 源站值 |
| --- | ---: |
| 章节数 | 1350 |
| `image_count` 总和 | 10160 |
| 含插图章节 | 1239 |
| 单章最高插图数 | 56 |

网页 EPUB 生成器的图片契约：

- 只把正文中的 `[图片: URL]`、`[图片：URL]`、`[图片 URL]` 占位符转换为图片 descriptor；Markdown `![...]()` 和 HTML `<img>` 不会额外进入图片资源列表。
- 每一次有效占位符都是一个独立的 EPUB 图片条目；网络缓存只复用字节，不按 URL 或 hash 合并 ZIP 条目。
- 空的 `[图片]` 会因 URL 校验失败而保留为普通文本，不会创建失败图片条目。
- URL 会先删除所有空白、去掉前置冒号；协议相对地址补 `https:`，其他无协议地址补 `https://` 并去掉开头斜杠。
- 默认 `compressImages=false`、`zipCompressionLevel=0`，即图片保留原始字节，ZIP 条目使用 STORE。
- 网页进度中的插图总数只统计正文 descriptor；封面在独立阶段处理，不计入正文插图总数。
- 网站下载页把 `photoUrl`（页面封面 URL）传给 EPUB 生成器；`photoTrueUrl` 是预览用的原图，不应被原生下载默认替换进去。

原生 writer 本轮已对齐上述可观察契约：只解析有效的 `[图片...]` 占位符；保留重复出现的条目但只复用同 URL 的一次成功网络读取；失败结果不进入缓存，并按 descriptor 重试，避免一次瞬时失败扩散到后续引用；按网站规则清洗 URL；所有 ZIP 条目使用 STORE；封面不计入正文插图进度；原生下载封面改用 `NovelCard.coverUrl`，不再默认写入 `fullCoverUrl`。原图字节和 WebP/GIF 等实际媒体类型仍保持不变，避免为“体积对齐”破坏动态图/幻影坦克。

本轮没有点击真实“支付积分并下载”按钮，因此没有消耗积分，也没有声称已经得到 350192 的最终 EPUB 对比包。以上是 2026-08-22 针对 `350192` 的历史记录；下方记录 2026-08-23 在用户授权下完成的另一部书 `359136` App 实际下载结果。无论哪一轮，都不得用 hash 擅自删除跨章节重复图片。

## 2026-08-23 最终真实 App 下载审计（book 359136）

本轮针对论坛反馈“下载后的图片数量不对”完成了真实 MuMu 下载和 ZIP/XHTML 复核。目标书为 `魔法少女梦见败北`（`359136`），使用当前 native EPUB 入口；没有清除登录态、收藏、阅读进度、缓存或其他已有下载。

### 源站只读目录基线

证据文件：`D:\NovalPie\agent-bridge\artifacts\20260823-public-359136-chapters-final.json`

| 指标 | 源站接口值 |
| --- | ---: |
| 章节数 | 125 |
| `image_count` 总和 | 566 |
| 含图片章节 | 121 |
| 单章最高 `image_count` | 10 |

### App 最终产物

审计时使用的临时文件（已在核验后按白名单删除）：`D:\NovalPie\agent-bridge\artifacts\20260823-image-count-audit\final-359136.epub`

| 指标 | 实测值 |
| --- | ---: |
| ZIP 条目 | 768 |
| 章节 XHTML | 125 |
| XHTML 总数（含 `nav.xhtml`） | 126 |
| 正文图片条目 | 637 |
| 独立封面图片条目 | 1 |
| 正文 `<img>` 引用 | 637 |
| 正文唯一图片哈希 | 566 |
| 相同内容的重复哈希组 | 71 |
| 重复 occurrence（不应删除） | 71 |
| `.file` 错误扩展名条目 | 0 |
| 图片读取/CRC 错误 | 0 |
| ZIP 文件大小 | 1,178,747,895 bytes |
| SHA-256 | `833B4799ADABFA48D7E97896915A811A290973EA774B78F2003018E12D19A40F` |

结论：原生端现在按每一个正文图片 occurrence 保留独立 EPUB 条目；成功请求可以复用已下载的原始字节，但不会按 URL 或图片 hash 合并 ZIP 路径。637 个正文条目均有对应 XHTML 引用，71 组重复内容是源文档中不同 occurrence 的结果，删除它们会造成章节内容缺失。图片保持原始字节，未做压缩或重采样；媒体类型按文件头推断，不再把 `application/octet-stream` 图片直接写成 `.file`。

本轮已取得并审计 App 的真实 EPUB，但尚未取得同一书籍由网页端实际生成的最终 EPUB 字节，因此不能宣称两端 ZIP 总大小已经完全一致。当前可以确认的是章节基线、图片 occurrence/引用完整性、重复路径策略和原始图片保存策略均已记录；若要完成字节级网页对比，应分别取得网页产物后再比较，不应以源站 `image_count` 单字段推断 ZIP 条目数。
