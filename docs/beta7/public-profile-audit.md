# 公开主页与动态分页（进行中）

- 独立`PublicProfileViewModel/Repository`，每用户缓存分区/筛选/滚动与请求代次，最多6个用户；切账号清理，离开页不再丢掉有效回包导致返回卡Loading。
- 动态多源并集旧`take(limit)`丢内容已在88541先红后修；每源保留真实page/hasMore，合并去重但不截断。部分读取失败不会直接跳下一页，有重试本页提示。
- 本人和公开主页新增“加载更早动态”，刷新重新读取已加载页，不丢前页。帖与书评明确其他author过滤缺口43090先红已修。
- 43921全量1010tests，35053全量1012tests/Debug/AndroidTest通过，公开主页scroll按用户/tab/filter保存，旧tab销毁不会写入新tab。

## 2026-09-07 实机

APK `da559464674de004e9caeb20a8d8aed9c289cf9b1efd8d9c4691e75dad7d83f7`，MuMuAndroid15/360dp/font1.0，install-r/cold4.782s。

- 论坛作者进入榛名全色100002，头像/头部/46部作品/215天签到/动态均加载。`20260907-public-{profile,books,checkins}.png`已查看。
- 作品《在野外获得了画廊》进入648章详情，返回书卡标题位置保持`[36,497][204,561]`。
- 动态首贴进入论坛详情再返回，标题位置保持`[45,327][476,390]`。证据`20260907-public-activity-back.png`。
- 没有点击私信、屏蔽、签到或其他真实写入。
- 发现公开作品卡无指标仍留空且长名称省略，后续改紧凑作品卡并完整测量名称。当前91819全量/打包，尚未安装这一展示修订。
- 新增`PublicProfilePagingLiveDeviceTest`只读100002公开动态，服务器page_size2用于实际点更多，报告只留前后条目数量/页码；尚未运行，不能算通过。

尚需最终优化包、宽度/字体/深链/所有分区、真正多页按钮及源异常组合验证；书籍列表源接口未提供分页参数，不虚构分页或强行抓取更多。

## 后续同切片验证

- 91819全量1012tests/Debug通过后，首轮live脚本对屏外LazyColumn子节点performScrollTo失败，保持失败记录。改用明确列表testTag和performScrollToNode，不放宽追加断言。
- 60199配套APK `84db5453b51b8f117efc1166252021e0bc2ddcb062a4f0849ffa554cead9aae1`已install-r/cold4.026s；`PublicProfilePagingLiveDeviceTest`真实只读分页按钮通过，3.97s。
- page_size2的4源合并第一页6条，实际点更多→page2共17条，旧6条全保留。报告`20260907-public-activity-paging-report.json`，门禁`20260907-public-activity-paging-retry.{json,log}`。
- 公开作品紧凑卡真实长标题完整显示，同排作者基线对齐；原无指标的额外底部空区去掉。截图`20260907-public-books-compact.png`已查看。没有额外业务写入。
