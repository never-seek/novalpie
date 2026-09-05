# Reader Tail Pagination and Web Progress Reconciliation Design

## Scope

Implement the two user-approved fixes found in the current App feedback audit:

1. In page-turn mode, a short terminal chapter/comment page must not repeat the preceding illustration or last paragraph.
2. A website reading position that is strictly ahead of the local App chapter must become the App's resume point, while preserving an App viewport anchor only when both sources still point to the same chapter.

The working tree is intentionally shared and already contains unreleased NovalPie changes, so this work stays in the current checkout rather than creating a worktree that would omit the active release candidate.

## Reader terminal page

### Alternatives considered

- Force the next chapter as soon as the final paragraph is visible. Rejected: it hides the chapter comments page and reintroduces skipped-content behavior.
- Mask repeated content after LazyColumn clamps. Rejected: it leaves the logical page position wrong and makes Back/history unreliable.
- Add a terminal spacer item only in page mode, then treat that item as a semantic next-chapter boundary. Recommended.

### Design

After the existing reader body/comment items, page mode appends exactly one spacer whose height equals the current reader viewport. It gives the last real content item enough physical scroll range for `scrollToItem()` to align its top rather than being clamped against the list tail.

The spacer is never rendered as a user-visible page. `ReaderPageTurnPlan` receives its index: a forward target equal to the terminal spacer resolves immediately to `NextChapter`; a target before it remains an in-chapter page. Backward page history continues to store only real page starts, so returning from the adjacent chapter returns to the final real comments page.

This applies only when the reader is in page mode and the body is successful. Continuous/infinite scrolling receives no tail spacer and keeps its existing article-comment-next-chapter order.

## Website progress reconciliation

### Alternatives considered

- Always let website progress overwrite local progress. Rejected: website records only a chapter and would destroy a newer native position or an exact local paragraph anchor.
- Keep the current display-only merge. Rejected: it causes the reported split brain: card progress, update label, and resume location disagree.
- Adopt website progress only when its source chapter is strictly ahead; preserve local state for same/older remote progress. Recommended.

### Design

Create one pure `ReaderProgress` merge rule. Given a local reader progress record and a `FavoriteEntry` from the website:

- Do nothing unless the entry has both a positive `lastChapterId` and positive `lastChapter`.
- If local progress is absent, create local resume progress from the remote chapter.
- If remote chapter number is greater than local chapter number, replace the local chapter ID/number and clear the local viewport anchor by saving a different chapter.
- If remote chapter is equal or behind, retain the local record and its same-chapter anchor.
- If the remote chapter reaches the current source chapter count, set `chapterCountAtLastRead` to the current total; this clears a stale “更新 N 章” marker after the person finishes those chapters on the website. Partial remote reads retain the prior completion baseline.

Run the reconciliation only after a fresh favorites/history page response is accepted by `loadHome` or `loadMoreFavorites`. Refresh `readerProgress`, recent progress and loaded collection state from the store immediately after a real merge. No extra network write is made: the remote read was already performed by the website.

## Error handling

- Missing or zero remote chapter IDs/numbers are ignored.
- A late/cancelled home request does not reconcile because the existing request serial guard runs first.
- The change is local-only and does not mutate a website reading record, favorites, comments or account data.

## Verification

- Unit tests first prove terminal spacer page planning and remote-ahead/equal/partial-complete merge behavior.
- MuMu page-mode regression uses book 360661 chapter 49: final content page → comments page → chapter 50, then Back to chapter 49.
- MuMu sync regression uses safe local test state and a real current favorites refresh only; it does not alter website progress. Unit coverage demonstrates the remote-ahead merge rule independently.
- Final gate: Debug unit tests, assembleDebug, lintDebug, `git diff --check`, install/hash verification and app PID log review.
