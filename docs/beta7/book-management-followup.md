# Book management remaining defect investigation

2026-09-13晚覆盖更新：元信息模块独立VM/Repository/UiState已接线，草稿保留/账号与环境隔离/迟到保存及封面/确认框身份/稳定picker lazy key/管理深链导航代次已修；真实失败回归与1225全量通过，独立复审通过。下面是历史调查，不再代表这些元信息问题未修。章节列表管理仍是下一片；元信息管理线上入口在工具策略拒绝的组合命令内，未绕行，不能宣称已经线上改书验收。

Latest, 2026-09-12: the missing policy initialization/unknown-value guard has been implemented and covered in BookManagementApiTest. Separate write-acknowledgement handling is fixed for metadata, policy, transfer, illustrations and six chapter mutations (1203 full tests green; see 20260912-management-receipts.md). The remaining extraction and stale-response/draft work below is NOT done.

Next concrete cases to reproduce before extraction: loadBookEditInfo currently resets the same-book draft on retry; save/transfer callbacks check route but not bookEditRequestSerial or account revision; account/proxy observer currently does not reset/invalidate book-edit/chapter-manager state. Chapter manager shares similar route-only write guards. Preserve account isolation, same-account proxy drafts, and no replay of unknown writes while moving each domain to its own state/repository. Test these against current behavior before replacing modules, and keep the new strict acknowledgement boundary.

2026-09-11 read-only source audit while a separate worker owns editor fixes. No implementation or real permissions write in this investigation.

## Existing access-policy values are not loaded into the form

- `BookEditInfo` includes title/author/source/status/tags/cover, but not allow_download or read/download threshold fields.
- `NovalPieApi.managedBookInfo` normalizes detail fields without those policy values.
- `NovalPieViewModel.loadBookEditInfo` creates `BookEditState` with text draft only; `accessPolicyDraft` remains default true/none/0.
- `BookEditScreens.BookAccessPolicySection` renders and can submit those default values under a general successful permissions response.
- Save calls `PATCH /api/users/me/novels/{id}/permissions` with all five policy fields, so unchanged-looking defaults can overwrite a work's actual restrictions.

Current website source ledger confirms PATCH fields in BY0xgLBE.js:allow_download/download_threshold_type/value/read_threshold_type/value. Need verify the read source: website populates modal from uploaded-novel entry/detail, then compare actual response samples. Do not assume missing values mean unrestricted. Disable save until authoritative policy loaded; then preserve values and draft while refreshing. Final source naming/permissions must be tested before modifying live controls.

Scope for the next implementation slice: independent book-management state/repository, identity and account generation, proper policy initialization, explicit success acknowledgement/draft preservation; protocol and synthetic UI regression, no unmarked modifications to user or other people's assets.
