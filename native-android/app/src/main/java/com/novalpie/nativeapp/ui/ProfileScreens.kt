package com.novalpie.nativeapp.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.SubcomposeAsyncImage
import coil.compose.SubcomposeAsyncImageContent
import com.novalpie.nativeapp.data.novalPieStaticImageRequest
import com.novalpie.nativeapp.model.LoadResult
import com.novalpie.nativeapp.model.NovelCard
import com.novalpie.nativeapp.model.UserActivity
import com.novalpie.nativeapp.model.UserCheckinRecord
import com.novalpie.nativeapp.model.UserCheckinStats
import com.novalpie.nativeapp.model.UserInventory
import com.novalpie.nativeapp.model.UserInventoryItem
import com.novalpie.nativeapp.model.UserBadge
import com.novalpie.nativeapp.model.UserProfile
import com.novalpie.nativeapp.model.UserQuizRewardStatus
import com.novalpie.nativeapp.model.ShopItem
import com.novalpie.nativeapp.ui.design.NpSearchField

@Composable
internal fun ProfileScreen(
    state: ProfileState,
    hasAuthToken: Boolean,
    onRefresh: () -> Unit,
    onOpenLogin: () -> Unit,
    onTabSelected: (ProfileTab) -> Unit,
    bookQuery: String,
    onBookQueryChange: (String) -> Unit,
    onBookGridColumnsChange: (Int) -> Unit,
    onNameChange: (String) -> Unit,
    onBioChange: (String) -> Unit,
    onShowCheckinChange: (Boolean) -> Unit,
    onAutoCheckinChange: (Boolean) -> Unit,
    onAdultBirthYearChange: (String) -> Unit,
    onSave: () -> Unit,
    onCheckin: () -> Unit,
    onVerifyAdult: () -> Unit,
    onAvatarSelected: (String) -> Unit,
    onOpenSettings: () -> Unit,
    onOpenActivity: (UserActivity) -> Unit,
    onActivityFilterSelected: (ProfileActivityFilter) -> Unit,
    onOpenBook: (Long) -> Unit,
    onPersonalizationTabSelected: (PersonalizationTab) -> Unit,
    onPurchaseShopItem: (ShopItem) -> Unit,
    onEquipInventoryItem: (UserInventoryItem) -> Unit,
) {
    val checkinStats = (state.checkinStats as? LoadResult.Success)?.value
    val profileWidthDp = LocalConfiguration.current.screenWidthDp
    var confirmCheckin by remember { mutableStateOf(false) }
    var confirmAdult by remember { mutableStateOf(false) }
    // Account is an information hub by default. Keeping the form closed avoids making a
    // first visit look like an unfinished settings page while retaining every edit field.
    var profileEditorExpanded by rememberSaveable { mutableStateOf(false) }
    var adultVerificationExpanded by rememberSaveable { mutableStateOf(false) }
    val profileListState = rememberLazyListState()
    var previousProfileTab by remember { mutableStateOf<ProfileTab?>(null) }
    val avatarPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.toString()?.let(onAvatarSelected)
    }

    // Switching away from Account used to retain its lower scroll offset, placing the first
    // control of Books (the local uploaded-book search field) above the viewport. Keep the
    // source-style tab rail and the selected page's first control in view after a tab change.
    LaunchedEffect(state.selectedTab, state.profile is LoadResult.Success) {
        val didChangeTab = previousProfileTab != null && previousProfileTab != state.selectedTab
        if (didChangeTab && state.profile is LoadResult.Success) {
            val tabRailIndex = if (state.selectedTab == ProfileTab.Account) 2 else 1
            profileListState.scrollToItem(tabRailIndex)
        }
        previousProfileTab = state.selectedTab
    }

    if (confirmCheckin) {
        AlertDialog(
            onDismissRequest = { confirmCheckin = false },
            title = { Text("确认签到") },
            text = { Text("签到会立即提交到 NovalPie 账号。") },
            confirmButton = {
                Button(
                    onClick = {
                        confirmCheckin = false
                        onCheckin()
                    }
                ) { Text("确认签到") }
            },
            dismissButton = {
                TextButton(onClick = { confirmCheckin = false }) { Text("取消") }
            }
        )
    }

    if (confirmAdult) {
        AlertDialog(
            onDismissRequest = { confirmAdult = false },
            title = { Text("确认成年验证") },
            text = { Text("出生年份将提交到 NovalPie 完成年验证。提交前请确认填写正确。") },
            confirmButton = {
                Button(
                    onClick = {
                        confirmAdult = false
                        onVerifyAdult()
                    }
                ) { Text("确认提交") }
            },
            dismissButton = {
                TextButton(onClick = { confirmAdult = false }) { Text("取消") }
            }
        )
    }

    LazyColumn(
        state = profileListState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        when (state.profile) {
            LoadResult.Idle, LoadResult.Loading -> item { ProfileLoadingCard() }
            is LoadResult.Error -> item {
                ProfileErrorCard(
                    message = state.profile.message,
                    hasAuthToken = hasAuthToken,
                    onRefresh = onRefresh,
                    onOpenLogin = onOpenLogin
                )
            }
            is LoadResult.Success -> {
                item {
                    OwnProfileHero(
                        profile = state.profile.value,
                        checkinStats = checkinStats,
                        uploadingAvatar = state.uploadingAvatar,
                        onRefresh = onRefresh,
                        onChangeAvatar = { avatarPicker.launch(arrayOf("image/*")) }
                    )
                }
                if (state.selectedTab == ProfileTab.Account) {
                    item {
                        ProfileAccountHub(
                            profile = state.profile.value,
                            checkinStats = checkinStats,
                            onOpenBooks = { onTabSelected(ProfileTab.Books) },
                            onOpenCheckin = { onTabSelected(ProfileTab.Checkin) }
                        )
                    }
                }
                item {
                    ProfileTabRail(selected = state.selectedTab, onSelected = onTabSelected)
                }

                when (state.selectedTab) {
                    ProfileTab.Account -> {
                        item {
                            ProfileSettingsCard(
                                hasAuthToken = hasAuthToken,
                                onOpenSettings = onOpenSettings,
                                onOpenLogin = onOpenLogin
                            )
                        }
                        item {
                            ProfileEditCard(
                                expanded = profileEditorExpanded,
                                onExpandedChange = { profileEditorExpanded = it },
                                name = state.nameDraft,
                                bio = state.bioDraft,
                                showCheckin = state.showCheckin,
                                autoCheckin = state.autoCheckin,
                                saving = state.saving,
                                onNameChange = onNameChange,
                                onBioChange = onBioChange,
                                onShowCheckinChange = onShowCheckinChange,
                                onAutoCheckinChange = onAutoCheckinChange,
                                onSave = onSave
                            )
                        }
                        if (state.profile.value.isAdult != true) {
                            item {
                                ProfileAdultVerificationCard(
                                    expanded = adultVerificationExpanded,
                                    onExpandedChange = { adultVerificationExpanded = it },
                                    birthYear = state.adultBirthYearDraft,
                                    verifying = state.verifyingAdult,
                                    onBirthYearChange = onAdultBirthYearChange,
                                    onVerify = { confirmAdult = true }
                                )
                            }
                        }
                    }

                    ProfileTab.Checkin -> {
                        item {
                            ProfileCheckinCard(
                                stats = state.checkinStats,
                                checkingIn = state.checkingIn,
                                onCheckin = { confirmCheckin = true }
                            )
                        }
                        item { ProfileCheckinHistoryCard(state.checkinRecords) }
                        item { ProfileQuizRewardCard(state.quizReward) }
                    }

                    ProfileTab.Activities -> {
                        item {
                            ProfileActivityFilterRail(
                                selected = state.activityFilter,
                                onSelected = onActivityFilterSelected,
                            )
                        }
                        when (val activities = state.activities) {
                            LoadResult.Idle, LoadResult.Loading -> item { ProfileInlineStatusCard("正在同步个人动态") }
                            is LoadResult.Error -> item { ProfileInlineStatusCard("个人动态暂时无法加载") }
                            is LoadResult.Success -> {
                                val visibleActivities = filterProfileActivities(activities.value, state.activityFilter)
                                if (visibleActivities.isEmpty()) {
                                    item {
                                        ProfileInlineStatusCard(
                                            if (activities.value.isEmpty()) "暂无公开动态" else "该分类暂无动态"
                                        )
                                    }
                                } else {
                                    items(visibleActivities, key = { "profile-${it.type}-${it.id}" }) { activity ->
                                        ProfileActivityCard(activity = activity, onOpenActivity = onOpenActivity)
                                    }
                                }
                            }
                        }
                    }

                ProfileTab.Books -> {
                    // Keep local search available while the authenticated source endpoint is
                    // loading or retrying. It doubles as a clear visible entry to refresh later.
                    item {
                        NpSearchField(
                            value = bookQuery,
                            onValueChange = onBookQueryChange,
                            onSearch = { },
                            placeholder = "搜索上传书籍（书名、作者、标签）",
                            clearContentDescription = "清除上传书籍搜索"
                        )
                    }
                    item {
                        ProfileBooksGridColumnsPicker(
                            selectedColumns = state.booksGridColumns,
                            onSelected = onBookGridColumnsChange,
                        )
                    }
                    when (val books = state.books) {
                        LoadResult.Idle, LoadResult.Loading -> item { ProfileInlineStatusCard("正在同步上传书籍") }
                        is LoadResult.Error -> item { ProfileInlineStatusCard("上传书籍暂时无法加载") }
                        is LoadResult.Success -> {
                        val visibleBooks = filterBooks(books.value, bookQuery)
                        if (books.value.isEmpty()) {
                                item { ProfileInlineStatusCard("暂无上传书籍") }
                            } else if (visibleBooks.isEmpty()) {
                                item { ProfileInlineStatusCard("没有匹配的上传书籍") }
                            } else {
                                val columns = state.booksGridColumns.coerceIn(2, 4)
                                val gridCoverHeight = searchGridCoverHeightDp(
                                    availableWidthDp = profileWidthDp,
                                    columnCount = columns,
                                ).dp
                                items(visibleBooks.chunked(columns), key = { row -> "profile-books-${row.joinToString { it.id.toString() }}" }) { rowBooks ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                                        verticalAlignment = Alignment.Top
                                    ) {
                                        rowBooks.forEach { book ->
                                            CompactLibraryBookCardItem(
                                                book = book,
                                                presentation = compactUploadedBookCardPresentation(book),
                                                modifier = Modifier.weight(1f),
                                                gridCoverHeight = gridCoverHeight,
                                                previewPolicy = CoverPreviewPolicy.Disabled,
                                            ) { onOpenBook(book.id) }
                                        }
                                        repeat(columns - rowBooks.size) {
                                            Spacer(modifier = Modifier.weight(1f))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                    ProfileTab.Inventory -> item {
                        ProfilePersonalizationPanel(
                            personalizationTab = state.personalizationTab,
                            profile = state.profile.value,
                            shopItems = state.shopItems,
                            inventory = state.inventory,
                            actionInventoryId = state.inventoryActionInventoryId,
                            purchaseItemId = state.shopPurchaseItemId,
                            onTabSelected = onPersonalizationTabSelected,
                            onPurchaseItem = onPurchaseShopItem,
                            onEquipItem = onEquipInventoryItem,
                        )
                    }
                }
            }
        }

        state.actionMessage?.let { message ->
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.secondaryContainer
                ) {
                    Text(message, modifier = Modifier.padding(12.dp), style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}

@Composable
@OptIn(ExperimentalLayoutApi::class)
private fun ProfileBooksGridColumnsPicker(
    selectedColumns: Int,
    onSelected: (Int) -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "上传书籍网格列数",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = "标题、作者始终显示 · 管理页不预览封面",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            listOf(2, 3, 4).forEach { columns ->
                FilterChip(
                    selected = selectedColumns == columns,
                    onClick = { onSelected(columns) },
                    label = { Text("每行 $columns 列") },
                )
            }
        }
    }
}

@Composable
private fun OwnProfileHero(
    profile: UserProfile,
    checkinStats: UserCheckinStats?,
    uploadingAvatar: Boolean,
    onRefresh: () -> Unit,
    onChangeAvatar: () -> Unit
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                ProfileAvatar(profile)
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text(
                        profile.name,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (profile.badges.isNotEmpty()) {
                        // The live profile places its `UserBadges size=md max=6` directly under
                        // the username. Keep the equipped cosmetics in the identity block rather
                        // than hiding them below the bio and account metrics.
                        ProfileBadgeRow(
                            badges = profile.badges,
                            display = ProfileBadgeDisplay.Hero,
                            maxVisible = PROFILE_HEADER_BADGE_MAX,
                        )
                    }
                    Text(
                        if (isAdminProfile(profile)) "管理员" else "NovalPie 用户",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                    profile.id?.let {
                        Text("UID $it", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onPrimaryContainer)
                    }
                }
                Column(horizontalAlignment = Alignment.End) {
                    TextButton(onClick = onRefresh) { Text("刷新") }
                    TextButton(onClick = onChangeAvatar, enabled = !uploadingAvatar) {
                        Text(if (uploadingAvatar) "上传中…" else "换头像")
                    }
                }
            }

            profile.bio?.takeIf(String::isNotBlank)?.let {
                Text(it, style = MaterialTheme.typography.bodyMedium, maxLines = 3, overflow = TextOverflow.Ellipsis)
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ProfileHeroMetric("帖子", profile.stats["posts"], Modifier.weight(1f))
                ProfileHeroMetric("评论", profile.stats["comments"], Modifier.weight(1f))
                ProfileHeroMetric("积分", profile.points, Modifier.weight(1f))
            }

        }
    }
}

@Composable
private fun ProfileHeroMetric(label: String, value: Long?, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.74f),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(profileMetricValueLabel(value), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun ProfileTabRail(selected: ProfileTab, onSelected: (ProfileTab) -> Unit) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items(ProfileTab.values()) { tab ->
            FilterChip(
                selected = selected == tab,
                onClick = { onSelected(tab) },
                label = { Text(profileTabLabel(tab)) }
            )
        }
    }
}

@Composable
private fun ProfileCheckinHistoryCard(records: LoadResult<List<UserCheckinRecord>>) {
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("本年签到记录", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            when (records) {
                LoadResult.Idle, LoadResult.Loading -> Text("正在同步签到记录", color = MaterialTheme.colorScheme.onSurfaceVariant)
                is LoadResult.Error -> Text(records.message, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                is LoadResult.Success -> {
                    if (records.value.isEmpty()) {
                        Text("暂无签到记录", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    } else {
                        records.value.takeLast(12).reversed().forEach { record ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(record.date)
                                ProfileFactPill("+${record.points} 积分")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ProfileQuizRewardCard(status: LoadResult<UserQuizRewardStatus>) {
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("奖励问答", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            when (status) {
                LoadResult.Idle, LoadResult.Loading -> Text("正在读取奖励状态", color = MaterialTheme.colorScheme.onSurfaceVariant)
                is LoadResult.Error -> Text("奖励状态暂不可用", color = MaterialTheme.colorScheme.onSurfaceVariant)
                is LoadResult.Success -> {
                    Text(profileQuizRewardLabel(status.value), fontWeight = FontWeight.Medium)
                    status.value.rewardName?.takeIf(String::isNotBlank)?.let {
                        Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    status.value.message?.takeIf(String::isNotBlank)?.let {
                        Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

@Composable
private fun ProfileActivityCard(activity: UserActivity, onOpenActivity: (UserActivity) -> Unit) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                ProfileFactPill(profileActivityTypeLabel(activity.type))
                activity.createdAt?.takeIf(String::isNotBlank)?.let {
                    Text(it.take(10), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Text(
                activity.title,
                modifier = Modifier.forumCardTap(
                    enabled = activity.postId != null || activity.bookId != null,
                    onTap = { onOpenActivity(activity) },
                ),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            activity.content?.takeIf(String::isNotBlank)?.let {
                ForumRichExcerpt(
                    content = userActivityPreviewText(it),
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 3,
                    onOpenContent = { onOpenActivity(activity) },
                    semanticDescription = "动态摘要",
                )
            }
        }
    }
}

@Composable
private fun ProfileActivityFilterRail(
    selected: ProfileActivityFilter,
    onSelected: (ProfileActivityFilter) -> Unit,
) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(horizontal = 2.dp),
    ) {
        items(ProfileActivityFilter.values().toList()) { filter ->
            FilterChip(
                selected = selected == filter,
                onClick = { onSelected(filter) },
                label = { Text(profileActivityFilterLabel(filter)) },
            )
        }
    }
}

/** Mirrors the website owner-only “装扮” page: shop first, then the equipped inventory. */
@Composable
private fun ProfilePersonalizationPanel(
    personalizationTab: PersonalizationTab,
    profile: UserProfile,
    shopItems: LoadResult<List<ShopItem>>,
    inventory: LoadResult<UserInventory>,
    actionInventoryId: Long?,
    purchaseItemId: Long?,
    onTabSelected: (PersonalizationTab) -> Unit,
    onPurchaseItem: (ShopItem) -> Unit,
    onEquipItem: (UserInventoryItem) -> Unit,
) {
    var pendingPurchase by remember { mutableStateOf<ShopItem?>(null) }
    pendingPurchase?.let { item ->
        AlertDialog(
            onDismissRequest = { pendingPurchase = null },
            title = { Text("购买确认") },
            text = { Text("确定要花费 ${item.price} 积分购买“${item.name}”吗？") },
            confirmButton = {
                TextButton(
                    onClick = {
                        pendingPurchase = null
                        onPurchaseItem(item)
                    }
                ) { Text("购买") }
            },
            dismissButton = {
                TextButton(onClick = { pendingPurchase = null }) { Text("取消") }
            },
        )
    }
    // The source PersonalizationTab uses a compact segmented rail followed by a 16px grid.
    // Keep the panel itself at the same `space-y-6` rhythm as the website profile page.
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        ProfilePersonalizationTabRail(
            selected = personalizationTab,
            onSelected = onTabSelected,
        )
        when (personalizationTab) {
            PersonalizationTab.Shop -> ProfileShopGrid(
                shopItems = shopItems,
                profile = profile,
                onPurchaseRequested = { item -> pendingPurchase = item },
                purchaseItemId = purchaseItemId,
            )
            PersonalizationTab.Inventory -> ProfileInventoryGrid(
                inventory = inventory,
                profile = profile,
                actionInventoryId = actionInventoryId,
                onEquipItem = onEquipItem,
            )
        }
    }
}

@Composable
private fun ProfilePersonalizationTabRail(
    selected: PersonalizationTab,
    onSelected: (PersonalizationTab) -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.62f),
    ) {
        Row(
            modifier = Modifier.padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            PersonalizationTab.values().forEach { tab ->
                val selectedTab = selected == tab
                Surface(
                    modifier = Modifier
                        .clip(RoundedCornerShape(9.dp))
                        .clickable { onSelected(tab) },
                    shape = RoundedCornerShape(9.dp),
                    color = if (selectedTab) MaterialTheme.colorScheme.surface else Color.Transparent,
                    tonalElevation = if (selectedTab) 2.dp else 0.dp,
                ) {
                    Text(
                        text = personalizationTabLabel(tab),
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = if (selectedTab) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    )
                }
            }
        }
    }
}

@Composable
private fun ProfileShopGrid(
    shopItems: LoadResult<List<ShopItem>>,
    profile: UserProfile,
    onPurchaseRequested: (ShopItem) -> Unit,
    purchaseItemId: Long?,
) {
    when (shopItems) {
        LoadResult.Idle, LoadResult.Loading -> ProfileInventoryLoadingState("加载商店中…")
        is LoadResult.Error -> ProfileInventoryEmptyState("商店暂时不可用")
        is LoadResult.Success -> {
            if (shopItems.value.isEmpty()) {
                ProfileInventoryEmptyState("商店空空如也")
            } else {
                ProfilePersonalizationGrid(shopItems.value) { item ->
                    ProfileShopGridItem(
                        item = item,
                        profile = profile,
                        onPurchaseRequested = onPurchaseRequested,
                        isBusy = purchaseItemId == item.id,
                    )
                }
            }
        }
    }
}

@Composable
private fun ProfileInventoryGrid(
    inventory: LoadResult<UserInventory>,
    profile: UserProfile,
    actionInventoryId: Long?,
    onEquipItem: (UserInventoryItem) -> Unit,
) {
    when (inventory) {
        LoadResult.Idle, LoadResult.Loading -> ProfileInventoryLoadingState("加载背包中…")
        is LoadResult.Error -> ProfileInventoryEmptyState("我的仓库暂时不可用")
        is LoadResult.Success -> {
            if (inventory.value.items.isEmpty()) {
                ProfileInventoryEmptyState("背包里还没有物品，去商店看看吧")
            } else {
                ProfilePersonalizationGrid(inventory.value.items) { item ->
                    ProfileInventoryGridItem(
                        item = item,
                        profile = profile,
                        isBusy = actionInventoryId == item.inventoryId,
                        onEquipItem = onEquipItem,
                    )
                }
            }
        }
    }
}

@Composable
private fun <T> ProfilePersonalizationGrid(
    items: List<T>,
    content: @Composable (T) -> Unit,
) {
    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        // Matches the source Tailwind grid: 2 columns by default, 3 at `sm`, and 4 at `lg`.
        // A phone in portrait still gets the source's two-column density while tablets and
        // landscape screens avoid turning each cosmetic card into a wide empty panel.
        val columns = when {
            maxWidth >= 1024.dp -> 4
            maxWidth >= 640.dp -> 3
            else -> 2
        }
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            items.chunked(columns).forEach { row ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.Top,
                ) {
                    row.forEach { item ->
                        Box(Modifier.weight(1f)) { content(item) }
                    }
                    repeat(columns - row.size) { Spacer(Modifier.weight(1f)) }
                }
            }
        }
    }
}

@Composable
private fun ProfileInventoryLoadingState(message: String) {
    Box(
        modifier = Modifier.fillMaxWidth().heightIn(min = 180.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
            CircularProgressIndicator(Modifier.size(26.dp), strokeWidth = 2.dp)
            Text(message, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun ProfileInventoryEmptyState(message: String) {
    Box(
        modifier = Modifier.fillMaxWidth().heightIn(min = 180.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(message, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun ProfileInventoryGridItem(
    item: UserInventoryItem,
    profile: UserProfile,
    isBusy: Boolean,
    onEquipItem: (UserInventoryItem) -> Unit,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(PROFILE_PERSONALIZATION_CARD_HEIGHT),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(
            if (item.equipped) 2.dp else 1.dp,
            if (item.equipped) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
        ),
        tonalElevation = if (item.equipped) 1.dp else 0.dp,
    ) {
        Box {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(14.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                ProfileInventoryPreview(item = item, profile = profile)
                Text(
                    item.name,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = item.description?.takeIf(String::isNotBlank).orEmpty(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.weight(1f))
                Button(
                    onClick = { onEquipItem(item) },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isBusy,
                    shape = RoundedCornerShape(8.dp),
                    colors = if (item.equipped) {
                        ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    } else {
                        ButtonDefaults.buttonColors()
                    },
                ) {
                    if (isBusy) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color = if (item.equipped) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onPrimary,
                        )
                    } else {
                        Text(if (item.equipped) "卸下" else "装备")
                    }
                }
            }
            if (item.equipped) {
                Surface(
                    modifier = Modifier.align(Alignment.TopEnd).padding(8.dp),
                    shape = RoundedCornerShape(999.dp),
                    color = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                ) {
                    Text(
                        "已装备",
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }
    }
}

@Composable
private fun ProfileShopGridItem(
    item: ShopItem,
    profile: UserProfile,
    onPurchaseRequested: (ShopItem) -> Unit,
    isBusy: Boolean,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(PROFILE_PERSONALIZATION_CARD_HEIGHT),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            ProfileShopPreview(item = item, profile = profile)
            Text(
                item.name,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                item.description?.takeIf(String::isNotBlank) ?: "装扮商品",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.weight(1f))
            Button(
                onClick = { onPurchaseRequested(item) },
                enabled = !isBusy,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.primary,
                ),
            ) {
                if (isBusy) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                    )
                } else {
                    Text("${item.price} 积分购买")
                }
            }
        }
    }
}

/**
 * The website grid stretches every cell in a row. A fixed card height gives the mobile Compose
 * grid the same alignment without asking Coil's Subcompose layout for unsupported intrinsics.
 */
private val PROFILE_PERSONALIZATION_CARD_HEIGHT = 272.dp

@Composable
private fun ProfileInventoryPreview(item: UserInventoryItem, profile: UserProfile) {
    val normalizedType = item.type?.trim()?.lowercase()
    if (normalizedType == "badge") {
        ProfileBadgePreview(
            badge = UserBadge(
                id = item.itemId.takeIf { it > 0 } ?: item.id.takeIf { it > 0 },
                name = item.name,
                description = item.description,
                imageUrl = item.imageUrl,
                badgeHtml = item.badgeHtml,
                badgeCss = item.badgeCss,
            ),
        )
        return
    }

    ProfileAvatarFramePreview(
        avatarUrl = profile.avatarUrl,
        profileName = profile.name,
        frameUrl = item.imageUrl,
    )
}

@Composable
private fun ProfileShopPreview(item: ShopItem, profile: UserProfile) {
    val normalizedType = item.type.trim().lowercase()
    if (normalizedType == "badge") {
        ProfileBadgePreview(
            badge = UserBadge(
                id = item.id.takeIf { it > 0 },
                name = item.name,
                description = item.description,
                imageUrl = item.imageUrl,
                badgeHtml = item.badgeHtml,
                badgeCss = item.badgeCss,
            ),
        )
    } else {
        ProfileAvatarFramePreview(
            avatarUrl = profile.avatarUrl,
            profileName = profile.name,
            frameUrl = item.imageUrl,
        )
    }
}

@Composable
private fun ProfileBadgePreview(badge: UserBadge) {
    // Match the source backpack's `w-24 h-24` stage. The badge itself remains inline-sized;
    // stretching custom CSS across the stage creates empty banner-like pills that do not exist
    // on the website.
    Box(
        modifier = Modifier
            .size(96.dp)
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.58f), RoundedCornerShape(12.dp)),
        contentAlignment = Alignment.Center,
    ) {
        ProfileSourceBadge(
            badge = badge,
            display = ProfileBadgeDisplay.Showcase,
        )
    }
}

internal enum class ProfileBadgeDisplay {
    Inline,
    Hero,
    Showcase,
}

/** Pixel values mirror the source UserBadge sm/md variants without executing source CSS. */
internal data class ProfileBadgeVisualSpec(
    val heightDp: Int,
    val maxRadiusDp: Int,
    val horizontalPaddingDp: Int,
    val dotSizeDp: Int,
    val fontSizeSp: Int,
    val contentGapDp: Int,
    val shadowDp: Int,
)

internal fun profileBadgeVisualSpec(display: ProfileBadgeDisplay): ProfileBadgeVisualSpec = when (display) {
    ProfileBadgeDisplay.Inline -> ProfileBadgeVisualSpec(
        heightDp = 18,
        maxRadiusDp = 9,
        horizontalPaddingDp = 7,
        dotSizeDp = 5,
        fontSizeSp = 10,
        contentGapDp = 5,
        shadowDp = 3,
    )
    // The source backpack and profile header both call <UserBadge size="md">.
    ProfileBadgeDisplay.Hero,
    ProfileBadgeDisplay.Showcase,
    -> ProfileBadgeVisualSpec(
        heightDp = 22,
        maxRadiusDp = 11,
        horizontalPaddingDp = 9,
        dotSizeDp = 6,
        fontSizeSp = 12,
        contentGapDp = 5,
        shadowDp = 6,
    )
}

/**
 * A source badge can opt out of the stock pill geometry with fixed CSS pixels (for example, a
 * 125 x 34 artwork badge).  Native reads just those passive display values; it never runs the
 * stylesheet or lets server markup control layout.
 */
internal data class ProfileBadgeRenderMetrics(
    val widthDp: Int?,
    val heightDp: Int,
    val startPaddingDp: Int,
    val endPaddingDp: Int,
    val fontSizeSp: Int,
)

internal fun profileBadgeRenderMetrics(
    css: String?,
    display: ProfileBadgeDisplay,
): ProfileBadgeRenderMetrics {
    val default = profileBadgeVisualSpec(display)
    val resolvedCss = adminShopBadgePreviewResolvedCss(css)
    val maxWidth = when (display) {
        ProfileBadgeDisplay.Inline -> 144
        ProfileBadgeDisplay.Hero -> 172
        ProfileBadgeDisplay.Showcase -> 180
    }
    val maxHeight = when (display) {
        ProfileBadgeDisplay.Inline -> 34
        ProfileBadgeDisplay.Hero -> 38
        ProfileBadgeDisplay.Showcase -> 42
    }
    return ProfileBadgeRenderMetrics(
        widthDp = profileBadgeCssPixels(resolvedCss, "width")?.coerceIn(38, maxWidth),
        heightDp = profileBadgeCssPixels(resolvedCss, "height")?.coerceIn(16, maxHeight)
            ?: default.heightDp,
        startPaddingDp = profileBadgeCssPixels(resolvedCss, "padding-left")?.coerceIn(0, 24)
            ?: default.horizontalPaddingDp,
        endPaddingDp = profileBadgeCssPixels(resolvedCss, "padding-right")?.coerceIn(0, 24)
            ?: default.horizontalPaddingDp,
        fontSizeSp = profileBadgeCssPixels(resolvedCss, "font-size")?.coerceIn(10, 16)
            ?: default.fontSizeSp,
    )
}

private fun profileBadgeCssPixels(css: String, property: String): Int? =
    Regex(
        """(?:^|[;{}])\s*${Regex.escape(property)}\s*:\s*(\d+(?:\.\d+)?)px\b""",
        RegexOption.IGNORE_CASE,
    )
        .find(css)
        ?.groupValues
        ?.getOrNull(1)
        ?.toFloatOrNull()
        ?.toInt()

internal const val PROFILE_HEADER_BADGE_MAX = 6

/** Source-compatible UserBadge renderer shared by the profile, forum, and comment surfaces. */
@Composable
internal fun ProfileSourceBadge(
    badge: UserBadge,
    display: ProfileBadgeDisplay,
    modifier: Modifier = Modifier,
) {
    val isShowcase = display == ProfileBadgeDisplay.Showcase
    val visualSpec = profileBadgeVisualSpec(display)
    val renderMetrics = remember(badge.badgeCss, display) {
        profileBadgeRenderMetrics(badge.badgeCss, display)
    }
    val colors = remember(badge.badgeCss, badge.name, badge.imageUrl) {
        adminShopBadgePreviewColors(badge.badgeCss, badge.name, badge.imageUrl)
    }
    val backgroundImageUrl = remember(badge.badgeCss) {
        // UserBadge only uses image_url to select its fallback palette. Painting that asset as a
        // backdrop changes the source badge into an unrelated thumbnail. Only custom CSS may
        // provide a visual background image.
        adminShopBadgePreviewBackgroundImageUrl(badge.badgeCss)
    }
    val context = LocalContext.current
    val backgroundImageModel: Any? = remember(backgroundImageUrl, display, context) {
        backgroundImageUrl?.let { url ->
            if (display == ProfileBadgeDisplay.Inline) {
                // Inline badges are repeated in every forum row; freeze their first frame so a
                // recycled list does not keep an AnimatedImageDrawable alive per author.
                novalPieStaticImageRequest(
                    context = context,
                    url = url,
                    widthPx = 320,
                    heightPx = 96,
                )
            } else {
                url
            }
        }
    }
    val textColor = remember(badge.badgeCss) { adminShopBadgePreviewTextColor(badge.badgeCss) }
    val borderColor = remember(badge.badgeCss) { adminShopBadgePreviewBorderColor(badge.badgeCss) }
    val cornerRadius = remember(badge.badgeCss, visualSpec.maxRadiusDp) {
        adminShopBadgePreviewCornerRadius(badge.badgeCss, visualSpec.maxRadiusDp)
    }
    val label = remember(badge.badgeHtml, badge.name, badge.description, badge.id) {
        adminShopBadgePreviewForeground(
            html = badge.badgeHtml,
            fallback = badge.name,
            description = badge.description,
            id = badge.id,
        ) ?: adminShopBadgePreviewText(badge.badgeHtml, badge.name)
    }
    val hasDot = remember(badge.badgeHtml) { adminShopBadgePreviewHasDot(badge.badgeHtml) }
    val dotColor = remember(badge.badgeCss) { adminShopBadgePreviewDotColor(badge.badgeCss) }
    val badgeShape = RoundedCornerShape(cornerRadius.dp)
    val widthModifier = when {
        renderMetrics.widthDp != null -> Modifier.width(renderMetrics.widthDp.dp)
        isShowcase -> Modifier
        else -> Modifier.widthIn(min = 38.dp, max = 180.dp)
    }

    Box(
        modifier = modifier
            .then(widthModifier)
            .height(renderMetrics.heightDp.dp)
            .shadow(visualSpec.shadowDp.dp, badgeShape, clip = false)
            .clip(badgeShape)
            .background(Brush.linearGradient(colors))
            .border(1.dp, borderColor.copy(alpha = 0.74f), badgeShape)
            .padding(
                start = renderMetrics.startPaddingDp.dp,
                end = renderMetrics.endPaddingDp.dp,
            ),
        contentAlignment = Alignment.Center,
    ) {
        if (backgroundImageModel != null) {
            SubcomposeAsyncImage(
                model = backgroundImageModel,
                contentDescription = null,
                // Background artwork must follow the text-sized badge, never take the parent's
                // maximum width. matchParentSize keeps it visual-only during Box measurement.
                modifier = Modifier.matchParentSize(),
                contentScale = adminShopBadgePreviewContentScale(badge.badgeCss),
                loading = {},
                error = {},
            )
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(visualSpec.contentGapDp.dp),
        ) {
            if (hasDot) {
                Box(
                    modifier = Modifier
                        .size(visualSpec.dotSizeDp.dp)
                        .clip(CircleShape)
                        .background(dotColor)
                        .border(1.dp, Color.White.copy(alpha = 0.34f), CircleShape),
                )
            }
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                fontSize = renderMetrics.fontSizeSp.sp,
                fontWeight = FontWeight.Bold,
                color = textColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/** Mirrors the source UserBadges component: three records followed by a compact count marker. */
@Composable
private fun ProfileBadgeRow(
    badges: List<UserBadge>,
    display: ProfileBadgeDisplay = ProfileBadgeDisplay.Inline,
    maxVisible: Int = 3,
) {
    val sourceMax = maxVisible.coerceAtLeast(1)
    val visibleBadges = badges.take(sourceMax)
    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        items(visibleBadges, key = { badge -> badge.id?.toString() ?: badge.name }) { badge ->
            ProfileSourceBadge(badge = badge, display = display)
        }
        if (badges.size > visibleBadges.size) {
            item {
                Surface(
                    shape = RoundedCornerShape(999.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                ) {
                    Text(
                        "+${badges.size - visibleBadges.size}",
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }
    }
}

@Composable
private fun ProfileAvatarFramePreview(
    avatarUrl: String?,
    profileName: String,
    frameUrl: String?,
) {
    Box(
        modifier = Modifier
            .size(96.dp)
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.62f), RoundedCornerShape(8.dp)),
        contentAlignment = Alignment.Center,
    ) {
        if (avatarUrl.isNullOrBlank()) {
            Surface(modifier = Modifier.size(64.dp), shape = CircleShape, color = MaterialTheme.colorScheme.primaryContainer) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(profileName.firstOrNull()?.uppercase() ?: "N", fontWeight = FontWeight.Bold)
                }
            }
        } else {
            SubcomposeAsyncImage(
                model = avatarUrl,
                contentDescription = null,
                modifier = Modifier.size(64.dp).clip(CircleShape),
                contentScale = ContentScale.Crop,
                loading = {},
                error = {},
            )
        }
        if (!frameUrl.isNullOrBlank()) {
            // The source Avatar keeps the frame as a sibling overlay and scales it to 1.7x;
            // deliberately avoid clipping this layer back into the portrait circle.
            SubcomposeAsyncImage(
                model = frameUrl,
                contentDescription = null,
                modifier = Modifier
                    .size(64.dp)
                    .graphicsLayer(scaleX = PROFILE_AVATAR_FRAME_SCALE, scaleY = PROFILE_AVATAR_FRAME_SCALE),
                contentScale = ContentScale.Fit,
                loading = {},
                error = {},
            )
        }
    }
}

@Composable
private fun ProfileSettingsCard(
    hasAuthToken: Boolean,
    onOpenSettings: () -> Unit,
    onOpenLogin: () -> Unit
) {
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("应用设置", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text("阅读偏好、网络连接与网页登录入口", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            OutlinedButton(onClick = onOpenSettings, modifier = Modifier.fillMaxWidth()) { Text("进入应用设置") }
            if (!hasAuthToken) Button(onClick = onOpenLogin, modifier = Modifier.fillMaxWidth()) { Text("登录 NovalPie") }
        }
    }
}

@Composable
private fun ProfileAccountHub(
    profile: UserProfile,
    checkinStats: UserCheckinStats?,
    onOpenBooks: () -> Unit,
    onOpenCheckin: () -> Unit
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("账号中心", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(
                "把常用入口放在首屏，资料编辑和验证按需展开。",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                ProfileHubAction(
                    label = "上传书籍",
                    detail = "按书名、作者、标签搜索",
                    onClick = onOpenBooks,
                    modifier = Modifier.weight(1f)
                )
                ProfileHubAction(
                    label = "签到中心",
                    detail = "连续 ${profileMetricValueLabel(checkinStats?.currentStreak?.toLong())} 天",
                    onClick = onOpenCheckin,
                    modifier = Modifier.weight(1f)
                )
            }
            val statuses = profileAccountStatusLabels(profile)
            if (statuses.isNotEmpty()) {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(statuses.take(4)) { status -> ProfileFactPill(status) }
                }
            }
        }
    }
}

@Composable
private fun ProfileHubAction(
    label: String,
    detail: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.secondaryContainer
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(label, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            Text(
                detail,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun ProfileInlineStatusCard(message: String) {
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Text(message, modifier = Modifier.padding(18.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

private fun profileTabLabel(tab: ProfileTab): String = when (tab) {
    ProfileTab.Account -> "账号"
    ProfileTab.Checkin -> "签到"
    ProfileTab.Activities -> "动态"
    ProfileTab.Books -> "书籍"
    ProfileTab.Inventory -> "装扮"
}

private fun personalizationTabLabel(tab: PersonalizationTab): String = when (tab) {
    PersonalizationTab.Shop -> "商店"
    PersonalizationTab.Inventory -> "我的仓库"
}

private fun profileActivityTypeLabel(type: String): String = when (type) {
    "novel_comment" -> "书评"
    "chapter_comment" -> "章评"
    "post_comment" -> "评论"
    "post" -> "帖子"
    else -> "动态"
}

@Composable
internal fun ProfileHeroCard(profile: UserProfile, checkinStats: UserCheckinStats?) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                ProfileAvatar(profile)
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text(
                        profile.name,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (profile.badges.isNotEmpty()) {
                        ProfileBadgeRow(
                            badges = profile.badges,
                            display = ProfileBadgeDisplay.Hero,
                            maxVisible = PROFILE_HEADER_BADGE_MAX,
                        )
                    }
                    Text(
                        if (isAdminProfile(profile)) "管理员" else "普通用户",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                    profile.id?.let {
                        Text("用户 ID $it", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            if (!profile.bio.isNullOrBlank()) {
                Text(profile.bio, style = MaterialTheme.typography.bodyMedium)
            }

            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(profileWebsiteFacts(profile, checkinStats)) { fact -> ProfileFactPill(fact) }
            }

            val accountStatus = profileAccountStatusLabels(profile)
            if (accountStatus.isNotEmpty()) {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(accountStatus) { label -> ProfileFactPill(label) }
                }
            }

        }
    }
}

@Composable
private fun ProfileAvatar(profile: UserProfile) {
    // The website scales the frame beyond the avatar circle (`object-contain`, scale 1.7).
    // Reserve that full visual footprint so Compose does not crop it back into the portrait.
    Box(modifier = Modifier.size(PROFILE_AVATAR_FRAME_FOOTPRINT), contentAlignment = Alignment.Center) {
        Box(
            modifier = Modifier.size(PROFILE_AVATAR_CONTENT_SIZE),
            contentAlignment = Alignment.Center
        ) {
            val fallback: @Composable () -> Unit = {
            Surface(
                modifier = Modifier.fillMaxSize(),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer
            ) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        profile.name.firstOrNull()?.uppercase() ?: "N",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
            if (profile.avatarUrl.isNullOrBlank()) {
                fallback()
            } else {
                SubcomposeAsyncImage(
                model = profile.avatarUrl,
                contentDescription = "${profile.name}的头像",
                modifier = Modifier.fillMaxSize().clip(CircleShape),
                contentScale = ContentScale.Crop,
                loading = {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 2.dp)
                    }
                },
                error = { fallback() },
                success = { SubcomposeAsyncImageContent() }
                )
            }
        }
        if (!profile.avatarFrameUrl.isNullOrBlank()) {
            SubcomposeAsyncImage(
                model = profile.avatarFrameUrl,
                contentDescription = null,
                modifier = Modifier
                    .size(PROFILE_AVATAR_CONTENT_SIZE)
                    .graphicsLayer(scaleX = PROFILE_AVATAR_FRAME_SCALE, scaleY = PROFILE_AVATAR_FRAME_SCALE),
                contentScale = ContentScale.Fit
            )
        }
    }
}

/** Matches the source Avatar component's independent frame scale. */
internal const val PROFILE_AVATAR_FRAME_SCALE = 1.7f
private val PROFILE_AVATAR_CONTENT_SIZE = 76.dp
private val PROFILE_AVATAR_FRAME_FOOTPRINT = 130.dp

@Composable
private fun ProfileFactPill(label: String) {
    Surface(shape = RoundedCornerShape(999.dp), color = MaterialTheme.colorScheme.primaryContainer) {
        Text(
            label,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onPrimaryContainer
        )
    }
}

@Composable
private fun ProfileCheckinCard(
    stats: LoadResult<UserCheckinStats>,
    checkingIn: Boolean,
    onCheckin: () -> Unit
) {
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("每日签到", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            when (stats) {
                LoadResult.Idle, LoadResult.Loading -> Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                    Spacer(Modifier.width(8.dp))
                    Text("正在同步签到记录")
                }
                is LoadResult.Error -> Text("签到统计暂不可用", color = MaterialTheme.colorScheme.onSurfaceVariant)
                is LoadResult.Success -> {
                    Text("累计 ${stats.value.totalDays} 天 · 签到积分 ${stats.value.totalPoints}")
                    Text(
                        "当前连续 ${stats.value.currentStreak} 天 · 最长连续 ${stats.value.maxStreak} 天",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Button(onClick = onCheckin, enabled = !checkingIn, modifier = Modifier.fillMaxWidth()) {
                Text(if (checkingIn) "签到中…" else "立即签到")
            }
        }
    }
}

@Composable
private fun ProfileEditCard(
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    name: String,
    bio: String,
    showCheckin: Boolean,
    autoCheckin: Boolean,
    saving: Boolean,
    onNameChange: (String) -> Unit,
    onBioChange: (String) -> Unit,
    onShowCheckinChange: (Boolean) -> Unit,
    onAutoCheckinChange: (Boolean) -> Unit,
    onSave: () -> Unit
) {
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .animateContentSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text("编辑资料", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(
                        if (expanded) "修改后需手动保存。" else "用户名、简介和签到偏好",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                TextButton(onClick = { onExpandedChange(!expanded) }) {
                    Text(if (expanded) "收起" else "编辑")
                }
            }
            if (expanded) {
                OutlinedTextField(
                    value = name,
                    onValueChange = onNameChange,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("用户名") },
                    singleLine = true
                )
                OutlinedTextField(
                    value = bio,
                    onValueChange = onBioChange,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("个人简介") },
                    minLines = 3,
                    maxLines = 6
                )
                ProfileToggleRow("公开签到记录", showCheckin, onShowCheckinChange)
                ProfileToggleRow("自动签到", autoCheckin, onAutoCheckinChange)
                Button(onClick = onSave, enabled = !saving, modifier = Modifier.fillMaxWidth()) {
                    Text(if (saving) "保存中…" else "保存资料")
                }
            }
        }
    }
}

@Composable
private fun ProfileAdultVerificationCard(
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    birthYear: String,
    verifying: Boolean,
    onBirthYearChange: (String) -> Unit,
    onVerify: () -> Unit
) {
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .animateContentSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text("成年验证", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(
                        if (expanded) "提交前请确认出生年份。" else "按需提交出生年份完成验证",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                TextButton(onClick = { onExpandedChange(!expanded) }) {
                    Text(if (expanded) "收起" else "验证")
                }
            }
            if (expanded) {
                Text(
                    "网站可能要求账号注册满 30 天后才能验证，最终资格由服务器判断。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = birthYear,
                    onValueChange = onBirthYearChange,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("出生年份") },
                    placeholder = { Text("例如 1995") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
                Button(
                    onClick = onVerify,
                    enabled = !verifying && birthYear.length == 4,
                    modifier = Modifier.fillMaxWidth()
                ) { Text(if (verifying) "验证中…" else "提交成年验证") }
            }
        }
    }
}

@Composable
private fun ProfileToggleRow(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label)
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun ProfileLoadingCard() {
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
            CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 2.dp)
            Spacer(Modifier.width(12.dp))
            Text("正在同步个人资料")
        }
    }
}

@Composable
private fun ProfileErrorCard(
    message: String,
    hasAuthToken: Boolean,
    onRefresh: () -> Unit,
    onOpenLogin: () -> Unit
) {
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(if (hasAuthToken) "个人资料暂时无法加载" else "登录后查看个人资料", fontWeight = FontWeight.Bold)
            if (hasAuthToken) Text(message, style = MaterialTheme.typography.bodySmall)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (hasAuthToken) OutlinedButton(onClick = onRefresh) { Text("重试") }
                Button(onClick = onOpenLogin) { Text(if (hasAuthToken) "重新登录" else "去登录") }
            }
        }
    }
}
