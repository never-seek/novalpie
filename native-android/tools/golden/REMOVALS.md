# Deliberate removals from the string golden master

`tools/golden_strings.py` fails when a user-visible string disappears, because during this refactor
a disappearing string usually means lost content. A few removals are intentional. Each one is
recorded here with its justification, so that rebaselining stays auditable rather than becoming a
way to wave failures through.

## 2026-07-26 — Phase 2: fabricated forum content (31 strings)

`ProductCopy.forumFeedItems()` returned six hardcoded forum threads. `ForumScreen` substituted them
for real content whenever the feed was idle, loading, **failed**, or returned zero posts
(`NovalPieApp.kt:606-607`), and `ForumStatsStrip` computed its totals from them. The result was
fabricated forum activity — invented people, invented engagement counts, 置顶/精华 badges —
presented to users as real, most reliably when the network was down.

This is not content from novalpie.cc, so removing it is a correctness fix rather than content loss.
The brief was to retain every feature and content element *from the original website*; this was
neither.

Removed strings, for the record:

- Invented authors: `北港读者`, `栗子校对`, `协作校对`, `校对`, `灰页`, `雾灯`
- Invented thread titles: `角色弧光讨论`, `最新章节伏笔整理`, `翻译名词校对`, `结局走向猜测`,
  `收藏榜单变化`, `作者更新说明`, `榜单观察`, `运营记录`
- Invented containers: `热门作品`, `连载专区`, `站内公告`, `作品榜`, `榜单`
- Invented timestamps: `1小时前`, `2小时前`, `8分钟前`, `23分钟前`, `今天`
- Invented tags: `热议`, `长评`, `长篇讨论`, `伏笔`, `剧情`, `推理`, `术语`

`forumFeedTabs()` (`全部 / 书评 / 章节 / 动态`) is retained: those are the website's real forum
categories and `GET /api/posts` accepts the matching `type` parameter. Wiring the tabs to that
parameter is Phase 6 work — they are currently `onClick = {}`, which is a separate defect.

Two tests in `ProductCopyTest` were removed with the fixture. They asserted it held exactly six
entries with pinned and featured examples, so they would have failed had anyone deleted the
fabricated data — the tests were holding the defect in place.

## 2026-07-26 — Phase 2: mojibake (2 strings)

`璁哄潧` and `鏍囩` were UTF-8 bytes decoded as GBK. They are `论坛` and `标签`, and they were passed
to `toLoadResult(label)`, so a failed forum or tag request rendered `璁哄潧请求失败: …` on screen.
Replaced with the correct characters, so they leave the baseline as removals and `标签` enters as an
addition. `README.md` claims turn 37 eliminated all mojibake; it missed these two.

## 2026-07-26 — Phase 2: loading copy replaced by skeletons (1 string)

`正在同步论坛` was a sentence shown beside a progress bar. Loading now uses `NpBookRowSkeleton`,
which reserves the space the rows will occupy so arriving content does not shove the page down.
There are ~40 more `正在…` strings that will disappear the same way as Phase 6 converts each screen;
they should be appended here as that happens.
