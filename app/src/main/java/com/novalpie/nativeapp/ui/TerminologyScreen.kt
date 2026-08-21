package com.novalpie.nativeapp.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.novalpie.nativeapp.model.LoadResult
import com.novalpie.nativeapp.model.TerminologyEntry
import com.novalpie.nativeapp.ui.design.NovalPieSize
import com.novalpie.nativeapp.ui.design.NovalPieSpacing
import com.novalpie.nativeapp.ui.design.NpCard
import com.novalpie.nativeapp.ui.design.NpChip
import com.novalpie.nativeapp.ui.design.NpChipRow
import com.novalpie.nativeapp.ui.design.NpChipTone
import com.novalpie.nativeapp.ui.design.NpEmptyState
import com.novalpie.nativeapp.ui.design.NpErrorState
import com.novalpie.nativeapp.ui.design.NpSearchField
import com.novalpie.nativeapp.ui.design.NpSectionHeader

/**
 * The source terminology table can contain tens of thousands of entries. Keep this as a keyed
 * LazyColumn and request one server page at a time so a large novel never freezes the reader.
 */
@Composable
internal fun TerminologyScreen(
    state: TerminologyState,
    onKeywordChange: (String) -> Unit,
    onSearch: () -> Unit,
    onRetry: () -> Unit,
    onLoadMore: () -> Unit,
    onOpenWeb: () -> Unit,
) {
    val loadedEntries = (state.entries as? LoadResult.Success)?.value.orEmpty()
    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(
            start = NovalPieSpacing.screenHorizontal,
            end = NovalPieSpacing.screenHorizontal,
            top = NovalPieSpacing.md,
            bottom = NovalPieSpacing.listBottom,
        ),
        verticalArrangement = Arrangement.spacedBy(NovalPieSpacing.md),
    ) {
        item {
            NpSearchField(
                value = state.keyword,
                onValueChange = onKeywordChange,
                onSearch = { onSearch() },
                placeholder = "搜索原文、译名或备注",
                clearContentDescription = "清除术语关键词",
            )
        }
        item {
            TerminologyOverviewCard(
                state = state,
                loadedCount = loadedEntries.size,
                onOpenWeb = onOpenWeb,
            )
        }
        when (val entries = state.entries) {
            LoadResult.Idle -> item {
                NpEmptyState(
                    title = "输入关键词后搜索",
                    description = "可按原文、译名或备注筛选当前作品的术语。",
                )
            }

            LoadResult.Loading -> item { TerminologyLoadingCard() }

            is LoadResult.Error -> item {
                NpErrorState(
                    message = entries.message,
                    retryLabel = "重新加载术语表",
                    onRetry = onRetry,
                    secondaryLabel = "网页管理",
                    onSecondary = onOpenWeb,
                )
            }

            is LoadResult.Success -> {
                if (entries.value.isEmpty()) {
                    item {
                        NpEmptyState(
                            title = if (state.keyword.isBlank()) "当前作品还没有术语" else "没有匹配的术语",
                            description = "可以调整关键词，或在网页端管理术语。",
                            actionLabel = "网页管理",
                            onAction = onOpenWeb,
                        )
                    }
                } else {
                    items(entries.value, key = TerminologyEntry::id) { entry ->
                        TerminologyEntryCard(entry)
                    }
                    item {
                        TerminologyPagingFooter(
                            state = state,
                            loadedCount = entries.value.size,
                            onLoadMore = onLoadMore,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TerminologyOverviewCard(
    state: TerminologyState,
    loadedCount: Int,
    onOpenWeb: () -> Unit,
) {
    val summary = state.page?.let { page -> terminologySummaryLabel(page, loadedCount) }
        ?: "当前作品的源站术语表"
    NpCard(contentPadding = NovalPieSpacing.md) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(NovalPieSpacing.xxs)) {
                Text(
                    "术语表",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.semantics { heading() },
                )
                Text(
                    summary,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            TextButton(onClick = onOpenWeb) {
                Icon(
                    imageVector = Icons.Filled.OpenInBrowser,
                    contentDescription = null,
                    modifier = Modifier.size(NovalPieSize.iconSm),
                )
                Spacer(Modifier.width(NovalPieSpacing.xxs))
                Text("网页管理", maxLines = 1)
            }
        }
        Text(
            "术语内容原生只读；需要新增、编辑、导入或导出时可在网页端继续操作。",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun TerminologyLoadingCard() {
    NpCard(contentPadding = NovalPieSpacing.md) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(NovalPieSpacing.sm),
        ) {
            CircularProgressIndicator(modifier = Modifier.size(NovalPieSize.iconMd), strokeWidth = 2.dp)
            Text("正在加载术语表", style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun TerminologyEntryCard(entry: TerminologyEntry) {
    val lock = terminologyLockPresentation(entry.lockStatus)
    val active = terminologyActivePresentation(entry.isActive)
    NpCard(contentPadding = NovalPieSpacing.md) {
        Text(
            terminologySourceLabel(entry),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            terminologyTargetLabel(entry),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.primary,
        )
        NpChipRow {
            NpChip(
                label = active.label,
                tone = if (active.isWarning) NpChipTone.Warning else NpChipTone.Status,
            )
            NpChip(
                label = lock.label,
                tone = if (lock.isWarning) NpChipTone.Warning else NpChipTone.Neutral,
            )
        }
        entry.description?.takeIf(String::isNotBlank)?.let { description ->
            SelectionContainer {
                Text(
                    description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        entry.updatedAt?.takeIf(String::isNotBlank)?.let { updatedAt ->
            Text(
                "更新于 $updatedAt",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun TerminologyPagingFooter(
    state: TerminologyState,
    loadedCount: Int,
    onLoadMore: () -> Unit,
) {
    val page = state.page ?: return
    val canLoadMore = canLoadMoreTerminologyEntries(page, loadedCount)
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(NovalPieSpacing.sm),
    ) {
        state.loadMoreError?.let { message ->
            Text(
                message,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
            )
        }
        when {
            state.loadingMore -> {
                CircularProgressIndicator(modifier = Modifier.size(NovalPieSize.iconMd), strokeWidth = 2.dp)
                Text("正在加载更多术语", style = MaterialTheme.typography.bodySmall)
            }

            canLoadMore -> OutlinedButton(onClick = onLoadMore) { Text("加载更多") }

            else -> Text("已显示全部术语", style = MaterialTheme.typography.bodySmall)
        }
    }
}
