# 小书原生下载实机验收

2026-09-06，MuMu Android15，App已有登录态。测试书353686《透明龙》，公开目录9章、允许下载、无阅读/下载积分门槛。下载请求仍按网站正常授权，可消耗用户已授权测试积分。

开发APK SHA256 `187E04D9FE44D31999EC1070083046A3D6D47C02D73AFE4325CD725124A2F6B1`，Beta6-debug版本字段（尚非Beta7交付）。

`NativeDownloadLiveDeviceTest`共4种组合，实际调用AppContainer/NativeDownloadService，结果`OK (1 test)`、43.605秒（exec79393）：

| 输出 | 字节数 | SHA256 |
|---|---:|---|
| 原文TXT | 87390 | `21e7d6dc26df29d9462ab188a3b7c3f0450a5dd53efa33b139247dd08936c816` |
| 替换TXT | 95788 | `fd5258a37d6946b93364301a4ec81a8034fe999c02c94ef74ff59aaf1137f5a1` |
| 原文EPUB | 206867 | `86f4c344d98a3a3c0253488df84e92bc8268e066cdc069d977a6c7e78bcc9e53` |
| 替换EPUB | 215265 | `998242a1841e0acb740399ed15aa0664bf4fc1d6cc1afde6c9fb56a3545921f9` |

替换快照是本次任务专有的测试规则“透明龙→透明龙【Beta7测试】”，没有写入用户个人规则或公共词表。实际检查TXT和EPUB正文包含替换结果，原始导出另存。EPUB检查mimetype、container.xml、opf与正文。文件位于外部证据`agent-bridge/artifacts/beta7-device/download-small/`，不提交正文到Git。

首次真实TXT测试失败：完整私有正文已下载，但MediaStore的SIZE缓存列在发布前返回0，误判“下载保存不完整”。改查文件描述符statSize，必要时读字节计数，不放宽完整性。复验复用旧任务的下载票据/源文件，旧步骤没有再次请求授权；所有4份本次公开Download URI验证后精确删除。已有用户下载未动，失败私有检查点随最终成功回收。

未通过/待做：1GB+图书、GIF/WebP原始字节/图片出现次数、暂停/取消/网络恢复/进程中断、外部阅读器打开、任务列表与最终R8包复验。不得以本小书通过代替完整下载门禁。

外部阅读器初试：Readest官方v0.12.6 arm64包86,182,242 bytes，签名SHA256`652d1167761229141842cb3d1850b6e47e46e12f4be47f5a6c14b6d712741e82`验证通过，MuMu可安装。启动即提示Android System WebView过旧（系统110），所以此时不能将Readest打开测试算通过。截图`20260906-readest-epub.png`。没有修改主机WebView/安全设置，后续可选兼容阅读器或明确模拟器验证缺口。

## 独立阅读器正文与目录验证（后续）

Readest在当前WebView110未能完成正文验收，因此换用F-Droid官方Librera `com.foobnix.pro.pdf.reader` 9.5.7-fdroid（7222），只安装到MuMu、不作为App依赖。下载来源 `https://f-droid.org/repo/com.foobnix.pro.pdf.reader_7222.apk`，99,754,068 bytes，APK SHA256 `b43e0991b7e356231077013667a90420010754681cec55873342229ead610eb8`；apksigner通过，F-Droid证书SHA256 `ea0d90df4dde7b9a9e8eaebe2e8aa47ed13f4df9d77aa64bc99c7ed309d2a84d`。

使用明确文件 `file:///sdcard/Download/Beta7-QA-replaced.epub` 打开，而非从同名历史文件猜测。正文实际显示“透明龙【Beta7测试】”，可以翻页；目录包含独立封面、第1–9章，能滚动到末章，实际跳转第6章正文。证据：

- `agent-bridge/screenshots/20260906-librera-qa-clean.png`：实际替换后正文，不是只有封面。
- `agent-bridge/screenshots/20260906-librera-toc-proof.png`：目录前部。
- `agent-bridge/screenshots/20260906-librera-toc-end.png`：末部第6–9章。
- `agent-bridge/screenshots/20260906-librera-final-chapter.png`：选中第6章后页眉及正文随目录跳转。

设备回读该测试EPUB SHA256=`998242a1841e0acb740399ed15aa0664bf4fc1d6cc1afde6c9fb56a3545921f9`与原验收副本一致。完成后精确删除 `/sdcard/Download/Beta7-QA-replaced.epub`；其他用户下载未动，私有验收副本可恢复。

MuMu横屏ADB默认输入目标与逻辑屏幕不一致；显式 `input touchscreen -d 0` 后按实际控件坐标可操作。这不是NovalPie分页缺陷。以上只证明该小书EPUB在独立引擎可解析/阅读，仍不代替1GB+、GIF/WebP、重复引用与最终R8包验收。
