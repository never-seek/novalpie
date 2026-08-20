package com.novalpie.nativeapp.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.automirrored.filled.ViewList
import androidx.compose.material.icons.filled.Cached
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.novalpie.nativeapp.model.FavoriteEntry
import com.novalpie.nativeapp.model.FavoritesCacheMode
import com.novalpie.nativeapp.model.FavoriteGroup
import com.novalpie.nativeapp.model.LoadResult
import com.novalpie.nativeapp.model.ReaderProgress
import com.novalpie.nativeapp.ui.design.NovalPieRadius
import com.novalpie.nativeapp.ui.design.NovalPieSize
import com.novalpie.nativeapp.ui.design.NovalPieSpacing
import com.novalpie.nativeapp.ui.design.NpChip
import com.novalpie.nativeapp.ui.design.NpChipRow
import com.novalpie.nativeapp.ui.design.NpChipTone
import com.novalpie.nativeapp.ui.design.NpSearchField

/**
 * Native equivalent of the source favourites toolbar. Presentation state is local, while group and
 * collection actions are supplied by the view model and call the verified source routes.
 */
@Composable
@OptIn(ExperimentalLayoutApi::class)
internal fun FavoritesControlPanel(
    state: HomeState,
    value: String,
    onValueChange: (String) -> Unit,
    onSelectTab: (FavoritesContentTab) -> Unit,
    onSelectGroup: (Long?) -> Unit,
    onToggleLayout: () -> Unit,
    onSelectGridColumns: (Int) -> Unit,
    onSelectDisplayMode: (FavoritesDisplayMode) -> Unit,
    onUpdateSort: (String?, String?) -> Unit,
    onToggleSelectionMode: () -> Unit,
    onCreateGroup: (String) -> Unit,
    onRenameGroup: (Long, String) -> Unit,
    onDeleteGroup: (Long) -> Unit,
    onMoveSelectedFavorites: (Long?) -> Unit,
    onRemoveSelectedFavorites: () -> Unit,
    onDeleteSelectedHistory: () -> Unit,
    onClearAllHistory: () -> Unit,
    onCycleCacheMode: () -> Unit,
    onClearFavoritesCache: () -> Unit,
    onClearImageCache: () -> Unit,
    onRetry: () -> Unit
) {
    var showGroupManager by remember { mutableStateOf(false) }
    var showSortPicker by remember { mutableStateOf(false) }
    var showMovePicker by remember { mutableStateOf(false) }
    var confirmRemove by remember { mutableStateOf(false) }
    var confirmClearHistory by remember { mutableStateOf(false) }

    val actionLabels = favoritesToolbarActionLabels(state.options.layout, state.selectionMode)
    Column(verticalArrangement = Arrangement.spacedBy(NovalPieSpacing.sm)) {
        // The source puts the query field and its six compact actions in one low-elevation panel.
        // Keeping that hierarchy gives the first book row screen space instead of a wall of buttons.
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = androidx.compose.foundation.shape.RoundedCornerShape(NovalPieRadius.md),
            color = MaterialTheme.colorScheme.surfaceContainerLow,
            border = BorderStroke(NovalPieSize.hairline, MaterialTheme.colorScheme.outlineVariant),
            shadowElevation = 2.dp
        ) {
            Column(
                modifier = Modifier.padding(NovalPieSpacing.xs),
                verticalArrangement = Arrangement.spacedBy(NovalPieSpacing.xs)
            ) {
                NpSearchField(
                    value = value,
                    onValueChange = onValueChange,
                    onSearch = { onRetry() },
                    placeholder = if (state.options.tab == FavoritesContentTab.Favorites) {
                        "搜索收藏（书名、作者、类型）..."
                    } else {
                        "搜索阅读历史..."
                    },
                    clearContentDescription = "清除收藏搜索"
                )
                Row(modifier = Modifier.fillMaxWidth()) {
                    FavoritesToolbarAction(
                        label = actionLabels[0],
                        icon = if (state.options.layout == FavoritesLayout.Grid) Icons.AutoMirrored.Filled.ViewList else Icons.Filled.GridView,
                        enabled = !state.actionLoading,
                        onClick = onToggleLayout
                    )
                    FavoritesToolbarAction(
                        label = actionLabels[1],
                        icon = Icons.Filled.Folder,
                        enabled = !state.actionLoading,
                        onClick = { showGroupManager = true }
                    )
                    FavoritesToolbarAction(
                        label = actionLabels[2],
                        icon = Icons.Filled.CheckCircle,
                        enabled = !state.actionLoading,
                        onClick = onToggleSelectionMode
                    )
                    FavoritesToolbarAction(
                        label = actionLabels[3],
                        icon = Icons.Filled.Cached,
                        enabled = !state.actionLoading,
                        selected = favoritesCacheActionIsSelected(state.options.cacheMode),
                        onClick = onCycleCacheMode
                    )
                    FavoritesToolbarAction(
                        label = actionLabels[4],
                        icon = Icons.Filled.Delete,
                        enabled = !state.actionLoading,
                        destructive = true,
                        onClick = onClearFavoritesCache
                    )
                    FavoritesToolbarAction(
                        label = actionLabels[5],
                        icon = Icons.AutoMirrored.Filled.Sort,
                        enabled = !state.actionLoading && state.options.tab == FavoritesContentTab.Favorites,
                        onClick = { showSortPicker = true }
                    )
                }
                FavoritesGridColumnsPicker(
                    selectedColumns = state.options.gridColumns,
                    enabled = favoritesGridColumnsPickerEnabled(
                        layout = state.options.layout,
                        actionLoading = state.actionLoading,
                    ),
                    onSelected = onSelectGridColumns,
                )
            }
        }

        FavoritesSourceTabs(state = state, onSelectTab = onSelectTab)

        if (state.selectionMode) {
            NpChip(label = "已选 ${state.selectedBookIds.size}", tone = NpChipTone.Status)
        }

        if (state.options.tab == FavoritesContentTab.Favorites) {
            when (val groups = state.groups) {
                is LoadResult.Success -> NpChipRow(modifier = Modifier.fillMaxWidth()) {
                    FilterChip(
                        selected = state.selectedFavoriteGroupId == null,
                        onClick = { onSelectGroup(null) },
                        label = { Text("全部") }
                    )
                    groups.value.take(8).forEach { group ->
                        val groupId = group.id
                        FilterChip(
                            selected = groupId != null && state.selectedFavoriteGroupId == groupId,
                            enabled = groupId != null && !state.actionLoading,
                            onClick = { groupId?.let(onSelectGroup) },
                            label = {
                                Text(
                                    "${group.name}${group.count?.let { " $it" } ?: ""}",
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        )
                    }
                }
                else -> Unit
            }
        }

        if (state.selectionMode) {
            FavoritesSelectionActions(
                state = state,
                onMove = { showMovePicker = true },
                onRemove = { confirmRemove = true },
                onDeleteHistory = { confirmRemove = true },
                onClearHistory = { confirmClearHistory = true }
            )
        }

        state.actionMessage?.let { message ->
            Text(
                message,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary
            )
        }
        if (state.actionLoading) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(NovalPieSpacing.sm),
                verticalAlignment = Alignment.CenterVertically
            ) {
                CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                Text("正在同步收藏", style = MaterialTheme.typography.bodySmall)
            }
        }
    }

    if (showGroupManager) {
        FavoritesGroupManagerDialog(
            groups = state.groups,
            displayMode = state.options.displayMode,
            onDismiss = { showGroupManager = false },
            onSelectDisplayMode = onSelectDisplayMode,
            onCreateGroup = onCreateGroup,
            onRenameGroup = onRenameGroup,
            onDeleteGroup = onDeleteGroup,
            onRetry = onRetry,
            onClearImageCache = onClearImageCache,
            busy = state.actionLoading
        )
    }
    if (showSortPicker) {
        FavoritesSortDialog(
            sortField = state.options.sortField,
            sortOrder = state.options.sortOrder,
            onDismiss = { showSortPicker = false },
            onUpdate = { field, order ->
                onUpdateSort(field, order)
                showSortPicker = false
            }
        )
    }
    if (showMovePicker) {
        FavoritesMoveDialog(
            groups = state.groups,
            onDismiss = { showMovePicker = false },
            onMove = { groupId ->
                onMoveSelectedFavorites(groupId)
                showMovePicker = false
            }
        )
    }
    if (confirmRemove) {
        val isHistory = state.options.tab == FavoritesContentTab.History
        AlertDialog(
            onDismissRequest = { confirmRemove = false },
            title = { Text(if (isHistory) "删除阅读记录" else "移除收藏") },
            text = { Text(if (isHistory) "将删除选中的阅读历史。" else "将从源站收藏中移除选中的作品。") },
            confirmButton = {
                Button(onClick = {
                    if (isHistory) onDeleteSelectedHistory() else onRemoveSelectedFavorites()
                    confirmRemove = false
                }) { Text("确认") }
            },
            dismissButton = { TextButton(onClick = { confirmRemove = false }) { Text("取消") } }
        )
    }
    if (confirmClearHistory) {
        AlertDialog(
            onDismissRequest = { confirmClearHistory = false },
            title = { Text("清空阅读历史") },
            text = { Text("此操作会删除源站全部阅读记录。") },
            confirmButton = {
                Button(onClick = {
                    onClearAllHistory()
                    confirmClearHistory = false
                }) { Text("确认清空") }
            },
            dismissButton = { TextButton(onClick = { confirmClearHistory = false }) { Text("取消") } }
        )
    }
}

@Composable
@OptIn(ExperimentalLayoutApi::class)
private fun FavoritesGridColumnsPicker(
    selectedColumns: Int,
    enabled: Boolean,
    onSelected: (Int) -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(NovalPieSpacing.xxs),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Filled.GridView,
                contentDescription = null,
                modifier = Modifier.size(NovalPieSize.iconSm),
                tint = MaterialTheme.colorScheme.primary,
            )
            Text(
                text = "收藏网格列数",
                modifier = Modifier.padding(start = NovalPieSpacing.xs),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = if (enabled) "标题、作者和阅读进度保留 · 长按卡片进入批量管理" else "切换网格后生效",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(NovalPieSpacing.xs),
            verticalArrangement = Arrangement.spacedBy(NovalPieSpacing.xxs),
        ) {
            listOf(2, 3, 4).forEach { columns ->
                FilterChip(
                    selected = selectedColumns == columns,
                    enabled = enabled,
                    onClick = { onSelected(columns) },
                    label = { Text("每行 $columns 列") },
                )
            }
        }
    }
}

/** Text order follows the mobile source toolbar: layout, groups, select, cache, clear, sort. */
internal fun favoritesToolbarActionLabels(
    layout: FavoritesLayout,
    selectionMode: Boolean
): List<String> = listOf(
    if (layout == FavoritesLayout.Grid) "列表" else "网格",
    "分组",
    if (selectionMode) "完成" else "选择",
    "缓存",
    "清除",
    "排序"
)

/** The source toolbar treats cache-off as a distinct state instead of a permanently active icon. */
internal fun favoritesCacheActionIsSelected(mode: FavoritesCacheMode): Boolean =
    mode != FavoritesCacheMode.None

/**
 * A column choice only has a visible result in grid mode. Disabling it in list mode prevents a
 * tap from looking like a failed setting change while preserving the saved choice for when the
 * user switches back to the grid.
 */
internal fun favoritesGridColumnsPickerEnabled(
    layout: FavoritesLayout,
    actionLoading: Boolean,
): Boolean = layout == FavoritesLayout.Grid && !actionLoading

/** Pinning is a favourite-record action; a history row must not expose a misleading pin control. */
internal fun favoritePinActionEnabled(
    tab: FavoritesContentTab,
    selecting: Boolean,
    favoriteId: Long?,
): Boolean = tab == FavoritesContentTab.Favorites && !selecting && favoriteId != null

@Composable
private fun RowScope.FavoritesToolbarAction(
    label: String,
    icon: ImageVector,
    enabled: Boolean,
    selected: Boolean = false,
    destructive: Boolean = false,
    onClick: () -> Unit
) {
    val contentColor = when {
        destructive -> MaterialTheme.colorScheme.error
        selected -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    Surface(
        modifier = Modifier
            .weight(1f)
            .heightIn(min = NovalPieSize.minTouchTarget)
            .clip(androidx.compose.foundation.shape.RoundedCornerShape(NovalPieRadius.sm))
            .clickable(enabled = enabled, onClick = onClick),
        color = if (selected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
        else MaterialTheme.colorScheme.surfaceContainerLow,
        contentColor = if (enabled) contentColor else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
    ) {
        Column(
            modifier = Modifier.padding(vertical = NovalPieSpacing.xxs),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(NovalPieSpacing.xxs)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(NovalPieSize.iconMd)
            )
            Text(label, style = MaterialTheme.typography.labelSmall, maxLines = 1)
        }
    }
}

@Composable
private fun FavoritesSourceTabs(
    state: HomeState,
    onSelectTab: (FavoritesContentTab) -> Unit
) {
    val favoriteCount = (state.favoriteEntries as? LoadResult.Success)?.value?.size ?: 0
    val historyCount = (state.history as? LoadResult.Success)?.value?.size ?: 0
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(NovalPieRadius.md),
        color = MaterialTheme.colorScheme.surfaceContainerHigh
    ) {
        Row(modifier = Modifier.padding(NovalPieSpacing.xxs)) {
            FavoritesSourceTab(
                modifier = Modifier.weight(1f),
                selected = state.options.tab == FavoritesContentTab.Favorites,
                label = "我的收藏",
                count = favoriteCount,
                icon = Icons.AutoMirrored.Filled.MenuBook,
                onClick = { onSelectTab(FavoritesContentTab.Favorites) }
            )
            FavoritesSourceTab(
                modifier = Modifier.weight(1f),
                selected = state.options.tab == FavoritesContentTab.History,
                label = "阅读历史",
                count = historyCount,
                icon = Icons.Filled.History,
                onClick = { onSelectTab(FavoritesContentTab.History) }
            )
        }
    }
}

@Composable
private fun FavoritesSourceTab(
    modifier: Modifier,
    selected: Boolean,
    label: String,
    count: Int,
    icon: ImageVector,
    onClick: () -> Unit
) {
    val contentColor = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
    Surface(
        modifier = modifier
            .clip(androidx.compose.foundation.shape.RoundedCornerShape(NovalPieRadius.sm))
            .clickable(onClick = onClick),
        color = if (selected) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.surfaceContainerHigh,
        contentColor = contentColor,
        shadowElevation = if (selected) 1.dp else 0.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = NovalPieSpacing.sm, vertical = NovalPieSpacing.sm),
            horizontalArrangement = Arrangement.spacedBy(NovalPieSpacing.xs),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(NovalPieSize.iconSm))
            Column {
                Text(label, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold, maxLines = 1)
                Text("$count 条目", style = MaterialTheme.typography.labelSmall, maxLines = 1)
            }
        }
    }
}

/** Keeps resume-reading discoverable without letting a non-source hero card bury the collection. */
@Composable
internal fun CompactFavoritesResumeRow(
    presentation: CompactFavoritesResumePresentation,
    onContinue: () -> Unit,
    onClear: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(androidx.compose.foundation.shape.RoundedCornerShape(NovalPieRadius.md))
            .clickable(onClick = onContinue),
        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f),
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
    ) {
        Row(
            modifier = Modifier.padding(horizontal = NovalPieSpacing.md, vertical = NovalPieSpacing.sm),
            horizontalArrangement = Arrangement.spacedBy(NovalPieSpacing.sm),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.AutoMirrored.Filled.MenuBook, contentDescription = null, modifier = Modifier.size(NovalPieSize.iconMd))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    presentation.bookTitle,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    presentation.chapterTitle,
                    style = MaterialTheme.typography.labelSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            TextButton(onClick = onClear) { Text("清除") }
        }
    }
}

@Composable
@OptIn(ExperimentalLayoutApi::class)
private fun FavoritesSelectionActions(
    state: HomeState,
    onMove: () -> Unit,
    onRemove: () -> Unit,
    onDeleteHistory: () -> Unit,
    onClearHistory: () -> Unit
) {
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(NovalPieSpacing.xs),
        verticalArrangement = Arrangement.spacedBy(NovalPieSpacing.xs)
    ) {
        if (state.options.tab == FavoritesContentTab.Favorites) {
            OutlinedButton(onClick = onMove, enabled = state.selectedBookIds.isNotEmpty()) { Text("移动分组") }
            TextButton(onClick = onRemove, enabled = state.selectedBookIds.isNotEmpty()) { Text("移除收藏") }
        } else {
            OutlinedButton(onClick = onDeleteHistory, enabled = state.selectedBookIds.isNotEmpty()) { Text("删除选中") }
            TextButton(onClick = onClearHistory) { Text("清空历史") }
        }
    }
}

@Composable
@OptIn(ExperimentalLayoutApi::class)
private fun FavoritesGroupManagerDialog(
    groups: LoadResult<List<FavoriteGroup>>,
    displayMode: FavoritesDisplayMode,
    onDismiss: () -> Unit,
    onSelectDisplayMode: (FavoritesDisplayMode) -> Unit,
    onCreateGroup: (String) -> Unit,
    onRenameGroup: (Long, String) -> Unit,
    onDeleteGroup: (Long) -> Unit,
    onRetry: () -> Unit,
    onClearImageCache: () -> Unit,
    busy: Boolean
) {
    var newGroupName by remember { mutableStateOf("") }
    var renameTarget by remember { mutableStateOf<FavoriteGroup?>(null) }
    var renameText by remember { mutableStateOf("") }
    var deleteTarget by remember { mutableStateOf<FavoriteGroup?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("分组与显示管理") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(NovalPieSpacing.sm)) {
                Text("选择显示视图", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(NovalPieSpacing.xs),
                    verticalArrangement = Arrangement.spacedBy(NovalPieSpacing.xs)
                ) {
                    DisplayModeChip("默认布局", "文件夹卡片 + 未分组小说", displayMode == FavoritesDisplayMode.Default) {
                        onSelectDisplayMode(FavoritesDisplayMode.Default)
                    }
                    DisplayModeChip("全部小说", "展开所有分组的收藏", displayMode == FavoritesDisplayMode.All) {
                        onSelectDisplayMode(FavoritesDisplayMode.All)
                    }
                    DisplayModeChip("未分类", "仅显示未分组收藏", displayMode == FavoritesDisplayMode.Unclassified) {
                        onSelectDisplayMode(FavoritesDisplayMode.Unclassified)
                    }
                }
                Text("管理分组", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Row(horizontalArrangement = Arrangement.spacedBy(NovalPieSpacing.xs), verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = newGroupName,
                        onValueChange = { newGroupName = it },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        label = { Text("输入新分组名称") }
                    )
                    Button(
                        enabled = newGroupName.isNotBlank() && !busy,
                        onClick = {
                            onCreateGroup(newGroupName)
                            newGroupName = ""
                        }
                    ) { Text("新建") }
                }
                when (groups) {
                    LoadResult.Idle, LoadResult.Loading -> Text("正在加载分组", style = MaterialTheme.typography.bodySmall)
                    is LoadResult.Error -> TextButton(onClick = onRetry) { Text("重新加载分组") }
                    is LoadResult.Success -> {
                        if (groups.value.isEmpty()) {
                            Text("暂无自定义分组", style = MaterialTheme.typography.bodySmall)
                        } else {
                            groups.value.forEach { group ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        "${group.name}${group.count?.let { " · $it 本" } ?: ""}",
                                        modifier = Modifier.weight(1f),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    if (group.id != null) {
                                        TextButton(onClick = {
                                            renameTarget = group
                                            renameText = group.name
                                        }, enabled = !busy) { Text("重命名") }
                                        TextButton(onClick = { deleteTarget = group }, enabled = !busy) { Text("删除") }
                                    }
                                }
                            }
                        }
                    }
                }
                Text("本地维护", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(NovalPieSpacing.xs),
                    verticalArrangement = Arrangement.spacedBy(NovalPieSpacing.xs)
                ) {
                    OutlinedButton(onClick = onRetry, enabled = !busy) { Text("刷新收藏") }
                    TextButton(onClick = onClearImageCache, enabled = !busy) { Text("清除图片缓存") }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("完成") } }
    )

    renameTarget?.let { group ->
        AlertDialog(
            onDismissRequest = { renameTarget = null },
            title = { Text("重命名分组") },
            text = {
                OutlinedTextField(
                    value = renameText,
                    onValueChange = { renameText = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            },
            confirmButton = {
                Button(
                    enabled = group.id != null && renameText.isNotBlank() && !busy,
                    onClick = {
                        group.id?.let { onRenameGroup(it, renameText) }
                        renameTarget = null
                    }
                ) { Text("保存") }
            },
            dismissButton = { TextButton(onClick = { renameTarget = null }) { Text("取消") } }
        )
    }
    deleteTarget?.let { group ->
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            title = { Text("删除分组") },
            text = { Text("删除“${group.name}”后，作品将回到未分类。") },
            confirmButton = {
                Button(onClick = {
                    group.id?.let(onDeleteGroup)
                    deleteTarget = null
                }, enabled = group.id != null && !busy) { Text("删除") }
            },
            dismissButton = { TextButton(onClick = { deleteTarget = null }) { Text("取消") } }
        )
    }
}

@Composable
private fun DisplayModeChip(title: String, subtitle: String, selected: Boolean, onClick: () -> Unit) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = {
            Column {
                Text(title)
                Text(subtitle, style = MaterialTheme.typography.labelSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    )
}

@Composable
@OptIn(ExperimentalLayoutApi::class)
private fun FavoritesSortDialog(
    sortField: String,
    sortOrder: String,
    onDismiss: () -> Unit,
    onUpdate: (String, String) -> Unit
) {
    var field by remember(sortField) { mutableStateOf(sortField) }
    var order by remember(sortOrder) { mutableStateOf(sortOrder) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("排序") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(NovalPieSpacing.sm)) {
                Text("排序字段", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(NovalPieSpacing.xs)) {
                    listOf("created_at" to "收藏时间", "last_read_time" to "阅读时间", "updated_at" to "更新顺序").forEach { (value, label) ->
                        FilterChip(selected = field == value, onClick = { field = value }, label = { Text(label) })
                    }
                }
                Text("排序方向", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(NovalPieSpacing.xs)) {
                    FilterChip(selected = order == "asc", onClick = { order = "asc" }, label = { Text("升序") })
                    FilterChip(selected = order == "desc", onClick = { order = "desc" }, label = { Text("降序") })
                }
            }
        },
        confirmButton = { Button(onClick = { onUpdate(field, order) }) { Text("应用") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } }
    )
}

@Composable
private fun FavoritesMoveDialog(
    groups: LoadResult<List<FavoriteGroup>>,
    onDismiss: () -> Unit,
    onMove: (Long?) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("移动到分组") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(NovalPieSpacing.xs)) {
                TextButton(onClick = { onMove(null) }, modifier = Modifier.fillMaxWidth()) { Text("未分类") }
                when (groups) {
                    is LoadResult.Success -> groups.value.forEach { group ->
                        group.id?.let { groupId ->
                            TextButton(onClick = { onMove(groupId) }, modifier = Modifier.fillMaxWidth()) {
                                Text(group.name)
                            }
                        }
                    }
                    LoadResult.Idle, LoadResult.Loading -> Text("正在加载分组")
                    is LoadResult.Error -> Text("分组暂不可用")
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("取消") } }
    )
}

@Composable
internal fun FavoriteGroupFolderCard(group: FavoriteGroup, onClick: () -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(88.dp)
            .clip(androidx.compose.foundation.shape.RoundedCornerShape(NovalPieRadius.md))
            .clickable(onClick = onClick),
        color = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        tonalElevation = 1.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = NovalPieSpacing.md, vertical = NovalPieSpacing.sm),
            horizontalArrangement = Arrangement.spacedBy(NovalPieSpacing.sm),
            verticalAlignment = Alignment.CenterVertically
        ) {
            FavoriteGroupPreviewStack(group)
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(NovalPieSpacing.xxs)
            ) {
                Text(group.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    text = favoriteGroupFolderSubtitle(group),
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            TextButton(onClick = onClick) { Text("打开") }
        }
    }
}

/**
 * `/api/favorites/groups?with_preview=true` includes a compact book preview for each group.
 * Keep it a visual-only stack: group cards never attach a cover-preview gesture, so long press
 * remains reserved for book batch management.
 */
@Composable
private fun FavoriteGroupPreviewStack(group: FavoriteGroup) {
    val previews = favoriteGroupPreviewEntries(group)
    Box(
        modifier = Modifier.size(width = 68.dp, height = 64.dp),
        contentAlignment = Alignment.CenterStart,
    ) {
        if (previews.isEmpty()) {
            Icon(
                imageVector = Icons.Filled.Folder,
                contentDescription = null,
                modifier = Modifier.size(32.dp),
            )
        } else {
            previews.forEachIndexed { index, entry ->
                BookCover(
                    title = entry.book.title,
                    coverUrl = novelThumbnailCoverUrl(entry.book),
                    width = 44.dp,
                    height = 64.dp,
                    modifier = Modifier.offset(x = (index * 10).dp, y = ((previews.size - index - 1) * 2).dp),
                    previewUrl = null,
                    previewPolicy = CoverPreviewPolicy.Disabled,
                )
            }
        }
    }
}

/** Deduplicate source aliases before laying out the small fixed-width folder cover stack. */
internal fun favoriteGroupPreviewEntries(group: FavoriteGroup): List<FavoriteEntry> =
    group.previews
        .asSequence()
        .filter { it.book.id > 0L }
        .distinctBy { it.book.id }
        .take(MAX_GROUP_COVER_PREVIEWS)
        .toList()

private const val MAX_GROUP_COVER_PREVIEWS = 3

@Composable
@OptIn(ExperimentalFoundationApi::class, ExperimentalLayoutApi::class)
internal fun FavoriteListRow(
    entry: FavoriteEntry,
    selected: Boolean,
    selecting: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onTogglePin: (() -> Unit)?
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(onClick = onClick, onLongClick = onLongClick),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(NovalPieRadius.md),
        color = if (selected) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp
    ) {
        Row(
            modifier = Modifier.padding(NovalPieSpacing.sm),
            horizontalArrangement = Arrangement.spacedBy(NovalPieSpacing.sm),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(modifier = Modifier.size(72.dp, 108.dp)) {
                BookCover(
                    entry.book.title,
                    novelThumbnailCoverUrl(entry.book),
                    previewUrl = novelDisplayCoverUrl(entry.book),
                    previewPolicy = CoverPreviewPolicy.Disabled,
                )
                if (selecting && selected) FavoriteSelectionMarker(Modifier.align(Alignment.TopEnd))
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(NovalPieSpacing.xs)) {
                Text(entry.book.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text(
                    entry.book.author ?: "作者未知",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                NpChipRow {
                    entry.groupName?.let { NpChip(it, NpChipTone.Neutral) }
                    // List mode must retain the same complete source tag set as the aligned grid;
                    // truncating to four made the list look like it had different book metadata.
                    novelCardTags(entry.book).forEach { NpChip(it, NpChipTone.Tag) }
                }
                entry.lastReadAt?.let { Text("阅读 ${it.take(16)}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                    ?: entry.createdAt?.let { Text("收藏 ${it.take(16)}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
            }
            if (onTogglePin != null) {
                TextButton(onClick = onTogglePin) { Text(if (entry.isPinned) "取消置顶" else "置顶") }
            }
        }
    }
}

/** Minimal list counterpart to the compact grid: cover, title, author, and reader progress only. */
@Composable
@OptIn(ExperimentalFoundationApi::class)
internal fun FavoriteListRow(
    entry: FavoriteEntry,
    selected: Boolean,
    selecting: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
) {
    val presentation = compactFavoriteBookCardPresentation(entry)
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(onClick = onClick, onLongClick = onLongClick),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(NovalPieRadius.md),
        color = if (selected) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surface,
        tonalElevation = 0.dp,
    ) {
        Row(
            modifier = Modifier.padding(NovalPieSpacing.sm),
            horizontalArrangement = Arrangement.spacedBy(NovalPieSpacing.sm),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(modifier = Modifier.size(72.dp, 108.dp)) {
                BookCover(
                    title = entry.book.title,
                    coverUrl = novelThumbnailCoverUrl(entry.book),
                    previewUrl = novelDisplayCoverUrl(entry.book),
                    previewPolicy = CoverPreviewPolicy.Disabled,
                )
                if (selecting && selected) FavoriteSelectionMarker(Modifier.align(Alignment.TopEnd))
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(NovalPieSpacing.xs),
            ) {
                Text(
                    text = presentation.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = presentation.author,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                presentation.progressLabel?.let { progress ->
                    Text(
                        text = listOfNotNull(progress, presentation.updateLabel).joinToString(" · "),
                        style = MaterialTheme.typography.labelSmall,
                        color = if (presentation.updateLabel == null) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.error
                        },
                    )
                }
            }
        }
    }
}

@Composable
internal fun FavoriteSelectionMarker(modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier
            .padding(NovalPieSpacing.xs)
            .size(24.dp),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(NovalPieRadius.pill),
        color = MaterialTheme.colorScheme.primary,
        contentColor = MaterialTheme.colorScheme.onPrimary
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text("✓", fontWeight = FontWeight.Bold)
        }
    }
}
