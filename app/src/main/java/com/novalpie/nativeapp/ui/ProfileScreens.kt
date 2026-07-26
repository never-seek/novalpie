package com.novalpie.nativeapp.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import coil.compose.SubcomposeAsyncImage
import coil.compose.SubcomposeAsyncImageContent
import com.novalpie.nativeapp.model.LoadResult
import com.novalpie.nativeapp.model.UserCheckinStats
import com.novalpie.nativeapp.model.UserProfile

@Composable
internal fun ProfileScreen(
    state: ProfileState,
    hasAuthToken: Boolean,
    onRefresh: () -> Unit,
    onOpenLogin: () -> Unit,
    onNameChange: (String) -> Unit,
    onBioChange: (String) -> Unit,
    onShowCheckinChange: (Boolean) -> Unit,
    onAutoCheckinChange: (Boolean) -> Unit,
    onAdultBirthYearChange: (String) -> Unit,
    onSave: () -> Unit,
    onCheckin: () -> Unit,
    onVerifyAdult: () -> Unit,
    onAvatarSelected: (String) -> Unit,
    onOpenSettings: () -> Unit
) {
    val checkinStats = (state.checkinStats as? LoadResult.Success)?.value
    var confirmCheckin by remember { mutableStateOf(false) }
    var confirmAdult by remember { mutableStateOf(false) }
    val avatarPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.toString()?.let(onAvatarSelected)
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
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text("个人中心", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text(
                "资料、积分、签到与账号设置",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

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
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        ProfileHeroCard(profile = state.profile.value, checkinStats = checkinStats)
                        OutlinedButton(
                            onClick = { avatarPicker.launch(arrayOf("image/*")) },
                            enabled = !state.uploadingAvatar,
                            modifier = Modifier.fillMaxWidth()
                        ) { Text(if (state.uploadingAvatar) "头像上传中…" else "更换头像") }
                    }
                }
                item {
                    ProfileCheckinCard(
                        stats = checkinStats,
                        statsLoading = state.checkinStats is LoadResult.Loading,
                        checkingIn = state.checkingIn,
                        onCheckin = { confirmCheckin = true }
                    )
                }
                item {
                    ProfileEditCard(
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
                            birthYear = state.adultBirthYearDraft,
                            verifying = state.verifyingAdult,
                            onBirthYearChange = onAdultBirthYearChange,
                            onVerify = { confirmAdult = true }
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

        item {
            ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("应用设置", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(
                        "阅读偏好、网络连接与网页登录入口",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedButton(onClick = onOpenSettings, modifier = Modifier.fillMaxWidth()) {
                        Text("进入应用设置")
                    }
                    if (!hasAuthToken) {
                        Button(onClick = onOpenLogin, modifier = Modifier.fillMaxWidth()) { Text("登录 NovalPie") }
                    }
                }
            }
        }
    }
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

            if (profile.badges.isNotEmpty()) {
                Text("徽章", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(profile.badges) { badge -> ProfileFactPill(badge) }
                }
            }
        }
    }
}

@Composable
private fun ProfileAvatar(profile: UserProfile) {
    Surface(
        modifier = Modifier.size(76.dp),
        shape = CircleShape,
        color = MaterialTheme.colorScheme.primaryContainer
    ) {
        val fallback: @Composable () -> Unit = {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    profile.name.firstOrNull()?.uppercase() ?: "N",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
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
}

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
    stats: UserCheckinStats?,
    statsLoading: Boolean,
    checkingIn: Boolean,
    onCheckin: () -> Unit
) {
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("每日签到", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            if (statsLoading) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                    Spacer(Modifier.width(8.dp))
                    Text("正在同步签到记录")
                }
            } else {
                Text("累计 ${stats?.totalDays ?: 0} 天 · 签到积分 ${stats?.totalPoints ?: 0}")
                Text(
                    "当前连续 ${stats?.currentStreak ?: 0} 天 · 最长连续 ${stats?.maxStreak ?: 0} 天",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Button(onClick = onCheckin, enabled = !checkingIn, modifier = Modifier.fillMaxWidth()) {
                Text(if (checkingIn) "签到中…" else "立即签到")
            }
        }
    }
}

@Composable
private fun ProfileEditCard(
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
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("编辑资料", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
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

@Composable
private fun ProfileAdultVerificationCard(
    birthYear: String,
    verifying: Boolean,
    onBirthYearChange: (String) -> Unit,
    onVerify: () -> Unit
) {
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("成年验证", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
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
