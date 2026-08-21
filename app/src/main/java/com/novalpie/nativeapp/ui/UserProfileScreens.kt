package com.novalpie.nativeapp.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.novalpie.nativeapp.model.LoadResult
import com.novalpie.nativeapp.model.UserActivity
import com.novalpie.nativeapp.model.UserCheckinRecord

@Composable
@OptIn(ExperimentalMaterial3Api::class)
internal fun UserProfileDetailScreen(
    state: UserProfileDetailState,
    hasAuthToken: Boolean,
    onRetry: () -> Unit,
    onTabSelected: (UserProfileTab) -> Unit,
    onActivityFilterSelected: (ProfileActivityFilter) -> Unit,
    onOpenActivity: (UserActivity) -> Unit,
    onOpenBook: (Long) -> Unit,
    onMessageUser: (Long, String?) -> Unit,
    onOpenLogin: () -> Unit
) {
    val profile = (state.profile as? LoadResult.Success)?.value
    val stats = (state.checkinStats as? LoadResult.Success)?.value
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        when (state.profile) {
            LoadResult.Idle, LoadResult.Loading -> item { PublicProfileStatusCard("正在加载用户资料") }
            is LoadResult.Error -> item {
                ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("用户资料加载失败", fontWeight = FontWeight.Bold)
                        Text(state.profile.message, style = MaterialTheme.typography.bodySmall)
                        OutlinedButton(onClick = onRetry) { Text("重试") }
                    }
                }
            }
            is LoadResult.Success -> {
                item { ProfileHeroCard(state.profile.value, stats) }
                item {
                    if (hasAuthToken) {
                        Button(
                            onClick = {
                                state.profile.value.id?.let { onMessageUser(it, state.profile.value.name) }
                            },
                            enabled = state.profile.value.id != null,
                            modifier = Modifier.fillMaxWidth()
                        ) { Text("发送私信") }
                    } else {
                        OutlinedButton(onClick = onOpenLogin, modifier = Modifier.fillMaxWidth()) {
                            Text("登录后发送私信")
                        }
                    }
                }
            }
        }

        if (profile != null) {
            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(UserProfileTab.values()) { tab ->
                        FilterChip(
                            selected = state.selectedTab == tab,
                            onClick = { onTabSelected(tab) },
                            label = { Text(userProfileTabLabel(tab)) }
                        )
                    }
                }
            }

            when (state.selectedTab) {
                UserProfileTab.Checkin -> {
                    val settings = (state.checkinSettings as? LoadResult.Success)?.value
                    if (settings?.showCheckin == false) {
                        item { PublicProfileStatusCard("该用户未公开签到记录") }
                    } else {
                        item {
                            ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text("签到", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                    when (val value = state.checkinStats) {
                                        LoadResult.Idle, LoadResult.Loading -> Text("正在加载签到统计")
                                        is LoadResult.Error -> Text(value.message, style = MaterialTheme.typography.bodySmall)
                                        is LoadResult.Success -> {
                                            Text("累计 ${value.value.totalDays} 天 · ${value.value.totalPoints} 积分")
                                            Text("当前连续 ${value.value.currentStreak} 天 · 最长 ${value.value.maxStreak} 天")
                                        }
                                    }
                                }
                            }
                        }
                        item { PublicCheckinRecords(state.checkinRecords) }
                    }
                }

                UserProfileTab.Activities -> when (val value = state.activities) {
                    LoadResult.Idle, LoadResult.Loading -> {
                        item {
                            UserProfileActivityFilterRail(
                                selected = state.activityFilter,
                                onSelected = onActivityFilterSelected,
                            )
                        }
                        item { PublicProfileStatusCard("正在加载用户动态") }
                    }
                    is LoadResult.Error -> {
                        item {
                            UserProfileActivityFilterRail(
                                selected = state.activityFilter,
                                onSelected = onActivityFilterSelected,
                            )
                        }
                        item { PublicProfileStatusCard(value.message) }
                    }
                    is LoadResult.Success -> {
                        item {
                            UserProfileActivityFilterRail(
                                selected = state.activityFilter,
                                onSelected = onActivityFilterSelected,
                            )
                        }
                        val visibleActivities = filterProfileActivities(value.value, state.activityFilter)
                        if (visibleActivities.isEmpty()) {
                            item {
                                PublicProfileStatusCard(
                                    if (value.value.isEmpty()) "暂无公开动态" else "该分类暂无动态"
                                )
                            }
                        }
                        items(visibleActivities, key = { "${it.type}-${it.id}" }) { activity ->
                            UserActivityCard(activity, onOpenActivity)
                        }
                    }
                }

                UserProfileTab.Books -> when (val value = state.books) {
                    LoadResult.Idle, LoadResult.Loading -> item { PublicProfileStatusCard("正在加载用户作品") }
                    is LoadResult.Error -> item { PublicProfileStatusCard(value.message) }
                    is LoadResult.Success -> {
                        if (value.value.isEmpty()) item { PublicProfileStatusCard("暂无上传作品") }
                        items(value.value.chunked(2)) { rowBooks ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.Top
                            ) {
                                rowBooks.forEach { book ->
                                    androidx.compose.foundation.layout.Box(Modifier.weight(1f)) {
                                        NovelCardItem(
                                            book = book,
                                            previewPolicy = CoverPreviewPolicy.Disabled,
                                            onClick = { onOpenBook(book.id) },
                                        )
                                    }
                                }
                                repeat(2 - rowBooks.size) {
                                    androidx.compose.foundation.layout.Spacer(Modifier.weight(1f))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun UserActivityCard(activity: UserActivity, onOpenActivity: (UserActivity) -> Unit) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(999.dp),
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Text(
                        userActivityTypeLabel(activity.type),
                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall
                    )
                }
                activity.createdAt?.let {
                    Text(it, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
                overflow = TextOverflow.Ellipsis
            )
            activity.content?.let {
                ForumRichExcerpt(
                    content = userActivityPreviewText(it),
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 4,
                    onOpenContent = { onOpenActivity(activity) },
                    semanticDescription = "动态摘要",
                )
            }
        }
    }
}

/** Activity cards are previews; keep long review bodies out of the scrolling text layout. */
internal fun userActivityPreviewText(content: String): String = forumFeedExcerptText(content)

@Composable
private fun UserProfileActivityFilterRail(
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

@Composable
private fun PublicCheckinRecords(records: LoadResult<List<UserCheckinRecord>>) {
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("本年签到记录", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            when (records) {
                LoadResult.Idle, LoadResult.Loading -> Text("正在加载签到记录")
                is LoadResult.Error -> Text(records.message, style = MaterialTheme.typography.bodySmall)
                is LoadResult.Success -> {
                    if (records.value.isEmpty()) Text("暂无签到记录")
                    records.value.takeLast(30).reversed().forEach { record ->
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(record.date)
                            Text("+${record.points} 积分", color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PublicProfileStatusCard(message: String) {
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Text(message, modifier = Modifier.padding(18.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

private fun userProfileTabLabel(tab: UserProfileTab): String = when (tab) {
    UserProfileTab.Checkin -> "签到"
    UserProfileTab.Activities -> "帖子与评论"
    UserProfileTab.Books -> "书籍"
}

private fun userActivityTypeLabel(type: String): String = when (type) {
    "novel_comment" -> "书评"
    "chapter_comment" -> "章评"
    "post_comment" -> "评论"
    "post" -> "帖子"
    else -> "动态"
}
