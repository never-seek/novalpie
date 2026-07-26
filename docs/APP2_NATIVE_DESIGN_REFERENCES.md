# NovalPie Native 2.0 Design References

This document records the native App 2.0 UI/product boundary for NovalPie.
NovalPie is a novel forum/community client, not a generic browser wrapper.

## Reference Projects

| Project | Local path | Snapshot | Use |
| --- | --- | --- | --- |
| Lingyan000/fluxdo | `D:\NovalPie\reference-projects\fluxdo` | `917c921` | Forum feed, topic detail, compact action bars, native community app feel |
| Pacalini/PicaComic | `D:\NovalPie\reference-projects\PicaComic` | `a9c896d` | Browse/detail/reader structure, favorites/history, media-heavy lists |
| gedoor/legado | `D:\NovalPie\reference-projects\legado` | `9bb0569` | Reader structure boundary only |

Current public-reference check on 2026-07-07:
- FluxDO is a Linux.do third-party community client. Its relevant direction is a Material-style forum client with native topic lists, topic detail, reply/search/notification flows, image caching/lazy loading, and chunked HTML rendering for smoother scrolling.
- Legado is only a reading-structure reference for toolbar, catalog, font, theme, and chapter navigation patterns.
- PicaComic is a comic client with browse, online reading, favorites, and reading history. Its useful direction is the browse/detail/reader structure, not provider/source management.

## Product Boundary

Keep native:
- Forum feed, forum topic detail, post body preview, topic comments, comment replies.
- Post and comment actions where website endpoints exist: like, dislike, emoji/reaction, reward.
- Link preview rows for long links in posts and comments.
- Book list, bookshelf, search, book detail, chapter catalog, reader, profile, local reading preferences.
- Book cards/details should preserve website-visible data where API supports it: cover, title, author, favorite count, tags, read counts, word count, update state, and description.
- Smooth list loading, stable route state, no blank result cards during image/network loading, clear loading/error/empty states.

Keep website fallback:
- Login and Cloudflare session.
- Admin editing, uploads, payments, workspace, role-sensitive flows.
- Any mutating endpoint not proven safe enough for native use.

Do not add:
- Legado-style book source editing, rule editing, crawler/source management, purifier/editor surfaces.
- PicaComic-style multi-source comic provider management.
- Native EPUB/image recompression/download rebuilding unless a separate website-owned download design is approved.
- Debug/API labels in visible product UI.

## UI Direction

Forum, based on FluxDO and the NovalPie website:
- Feed rows should look like forum cards: avatar/author/date, title, pinned/featured badges, body preview, reply count, like/reaction/reward counts, view count, and last activity.
- Native detail page should contain title, metadata, body, link previews, comment composer, comment list, reply mode, and compact action bars.
- Mobile-first single-column list; future tablet can use master-detail.
- Touch targets must not misfire while scrolling. List rows should only open detail from intentional taps, not from drag gestures.

Reader, based on Legado/PicaComic structure:
- Immersive reading body with explicit top/bottom tools.
- Chapter catalog opens as an overlay/panel, not appended under text.
- Toolbar actions stay stable: back, catalog, previous/next, font, theme, web fallback.
- Keep website-native reading/catalog behavior only. No source/rule/crawler/editor/download tooling.

Book/detail, based on PicaComic browsing structure:
- Cover, title, author, status, favorite/read counts, tags, description, primary reading action, favorite status, and chapter list.
- Comments/reviews should migrate natively when endpoint evidence is available; otherwise use website fallback.
- Loading must reserve layout space so image/network fetches do not flash blank book cards.

Discover/search, based on website parity:
- Search filter surface should cover website concepts: rules/sort, tags, word count, scope, adult filter, source, search mode, sort field, and sort direction.
- Admin-only search tools, help/settings/cache buttons should remain available when the account/session exposes them, but unimplemented or unsafe controls should use website fallback rather than disappearing.
- Use native website hot tags as discovery chips.
- After search starts, promote book results above history/tags/filter sections.
- Book result rows should be light mobile list rows with fixed cover slots, clipped descriptions, compact facts, and compact tags.

## Current Native Implementation Notes

- `ForumPost` and `NovalPieApi.forumPosts()` use observed read-only `GET /api/posts`.
- Native forum feed rows include website-style footer metrics: replies, likes, reactions, rewards, and views.
- Native forum feed/detail rows include website-style `置顶` and `精华` badges
  through pinned/featured normalization aliases.
- Native forum detail includes post body, link previews, comment composer, comments, comment link previews, and compact `赞 / 踩 / 表情 / 打赏 / 网页` action bars.
- Native comments include `赞 / 踩 / 表情 / 打赏 / 回复`.
- Native forum detail groups replies under parent comments, shows a
  `X 条评论 · Y 条回复` summary, and preserves orphan replies as standalone
  comments so paginated/missing parents do not hide user content.
- Forum screen consumes native forum state and falls back to local seed rows only during loading/empty development states.
- Discover uses native website hot tags and promotes live book results above history/tags/filter sections once search starts.
- Discover/Search includes website-parity `字数` ranges and sends
  `min_word_count` / `max_word_count` to `/api/search`; persisted settings now
  include source and word-count range.
- Native book/search/detail surfaces preserve website-style favorite/read/source
  metrics and full de-duplicated tags where the API provides them.
- Latest mobile source/API hint snapshot:
  `D:\NovalPie\site-research\live-20260708-mobile`.
- Visible Chinese product copy has been restored from UTF-8 mojibake.
- Reader keeps only website-native reading concepts; no source/rule/crawler/editor features.
