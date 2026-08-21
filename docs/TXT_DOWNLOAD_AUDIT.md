# TXT 原生下载审计（2026-08-19）

## 结论

TXT 下载没有从网站功能中删除，之前只是没有接入 native 详情菜单。当前 native 已恢复独立的 TXT 下载入口：

1. 请求 `POST /downloads`，发送 `{"novel_id": <bookId>, "download_type": "txt"}`。
2. 使用登录态和站点返回的 `file_name` 获取授权文件流。
3. 通过 `MediaStore.Downloads` 流式写入 Android 的 `Download/` 目录，MIME 为 `text/plain`。
4. 下载过程不打开 WebView，不把全文一次性读入内存，也不重新解析或改写 TXT。

EPUB 入口仍然保留，使用同一授权接口但发送 `download_type=epub`，继续走原生 EPUB 封装流程。

## 网页接口证据

已保存的站点前端源码 `D:\NovalPie\site-js-current\BaXNkoha.js` 中，下载函数把用户选择的类型直接放入请求：

```json
POST /api/downloads
{
  "novel_id": 354491,
  "download_type": "txt"
}

## Live native verification (2026-08-19)

This section supersedes the earlier statement that no real download authorization was used.

- Root cause of the first native failure: the app used the frontend route `/downloads`, while the
  site's runtime config declares `apiBase = https://novalpie.cc/api`. A frontend HTML page was
  returned with HTTP 200, so the old parser reported an invalid authorization response.
- Fix: both the authorization request and the authorized file stream now use `/api/downloads`.
  `NovalPieApiTest` covers `POST /api/downloads` and `GET /api/downloads/{fileName}`.
- With the user's approval, exactly one native TXT request was made for book `354491`. The native
  success state appeared without opening a WebView, and no repeat request was made.
- Account points changed from `1005100` to `1005099` (one point consumed), confirming that the
  live authorization executed rather than a mock or cached response.
- Android MediaStore published one file with MIME `text/plain`:
  `Download/勇者回归_将她们吃干抹净_354491_1787130468623.txt` (`1,603,636` bytes).
  The MediaProvider log records its pending-to-final atomic move; the captured first and last
  bytes are readable TXT content.
- Evidence: `D:\NovalPie\agent-bridge\screenshots\20260819-native-txt-fixed-menu-pretrigger.png`,
  `D:\NovalPie\agent-bridge\screenshots\20260819-native-txt-fixed-after-20s.png`,
  `D:\NovalPie\agent-bridge\screenshots\20260819-native-txt-points-before.png`,
  `D:\NovalPie\agent-bridge\screenshots\20260819-native-txt-points-after.png`, and
  `D:\NovalPie\agent-bridge\artifacts\20260819-native-txt-logcat-fixed.txt`.
```

源码同时存在 `download_txt: "下载TXT"` 文案；因此 native 使用 `txt`，没有猜测或复用 EPUB 参数。

## Native 实现

- API：`app/src/main/java/com/novalpie/nativeapp/data/NovalPieApi.kt`
  - `requestTxtDownload(bookId)` 复用通用下载授权请求。
  - `streamDownloadFile(fileName, consumer)` 保持流式读取和认证 header。
- ViewModel：`app/src/main/java/com/novalpie/nativeapp/ui/NovalPieViewModel.kt`
  - `downloadBookTxt(bookId)` 直接复制输入流到 Downloads 输出流。
  - Android 10+ 使用 `IS_PENDING`，只有完整写入后才发布文件；失败会删除临时条目。
  - 旧 Android 使用应用的 Downloads 目录并触发媒体扫描。
- UI：`app/src/main/java/com/novalpie/nativeapp/ui/NovalPieApp.kt`
  - 详情菜单并列显示 `原生下载 EPUB（保存到下载）` 与 `原生下载 TXT（保存到下载）`。
  - `打开网页详情` 仍是单独动作，不会被下载入口复用。

## 验证

- API 回归测试确认 TXT 请求的路径、方法、`novel_id` 和 `download_type=txt`。
- 既有流式下载测试确认正文 bytes 不被改变，认证 Cookie/Bearer header 保持。
- 全量门禁：80 suites / 559 tests / 0 failures / 0 errors / 0 skipped。
- `:app:lintDebug`：`No issues found`。
- `:app:assembleDebug`：成功。
- `git diff --check`：通过。
- Debug APK：`D:\NovalPie\native-android\app\build\outputs\apk\debug\app-debug.apk`。
- SHA-256：`0A04F33EF92C0F9027110BC52D62F47F67650AAFE9ECF4F3045EF24A57F79436`。

## MuMu 证据

- Serial：`127.0.0.1:16384`。
- APK 通过 `adb install -r` 安装，保留应用数据、登录态、Cookie 和代理设置。
- 书籍详情菜单截图：
  `D:\NovalPie\agent-bridge\screenshots\20260819-native-txt-download-menu.png`
- 返回详情页截图：
  `D:\NovalPie\agent-bridge\screenshots\20260819-txt-download-after-back.png`
- 本轮没有点击真实下载授权，因此没有消耗账户下载积分，也没有制造测试下载文件。
