package com.novalpie.nativeapp.feature.download

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.DownloadDone
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.novalpie.nativeapp.data.DownloadFailureDecision
import com.novalpie.nativeapp.ui.NativeBookDownloadFormat
import com.novalpie.nativeapp.ui.NativeDownloadReplacementMode
import com.novalpie.nativeapp.ui.NativeEpubDownloadState
import com.novalpie.nativeapp.ui.Text

@Composable
internal fun NativeDownloadProgressDialog(
    bookTitle: String,
    downloadState: NativeEpubDownloadState,
    onDismiss: () -> Unit,
    onTogglePause: () -> Unit,
    onCancel: () -> Unit,
    onRetry: () -> Unit,
    onResolveDecision: (DownloadFailureDecision) -> Unit,
    onOpenFile: () -> Unit,
    onShareFile: () -> Unit,
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .widthIn(max = 480.dp)
                .wrapContentHeight(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                // Header row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    val headerIcon = when {
                        downloadState.completedUri != null -> Icons.Filled.DownloadDone
                        downloadState.awaitingFailureDecision -> Icons.Filled.Warning
                        else -> Icons.Filled.Download
                    }
                    val headerTint = when {
                        downloadState.completedUri != null -> Color(0xFF2E7D32)
                        downloadState.awaitingFailureDecision -> MaterialTheme.colorScheme.error
                        else -> MaterialTheme.colorScheme.primary
                    }
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(headerTint.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = headerIcon,
                            contentDescription = null,
                            tint = headerTint,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                    Spacer(Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "下载进度",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            text = bookTitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    val formatLabel = if (downloadState.format == NativeBookDownloadFormat.Txt) "TXT" else "EPUB"
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        modifier = Modifier.padding(end = 4.dp),
                    ) {
                        Text(
                            text = formatLabel,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(28.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = "关闭",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                // Progress calculation
                val progressData = downloadState.progress
                val totalChapters = progressData?.totalChapters ?: 0
                val completedChapters = progressData?.completedChapters ?: 0
                val totalImages = progressData?.totalImages ?: 0
                val completedImages = progressData?.completedImages ?: 0
                val failedImages = progressData?.failedImages ?: 0

                val progressFraction: Float = when {
                    downloadState.completedUri != null -> 1f
                    downloadState.format == NativeBookDownloadFormat.Txt -> {
                        if (totalChapters > 0) (completedChapters.toFloat() / totalChapters).coerceIn(0f, 1f) else 0.05f
                    }
                    totalImages > 0 && totalChapters > 0 -> {
                        val chapPart = (completedChapters.toFloat() / totalChapters) * 0.2f
                        val imgPart = (completedImages.toFloat() / totalImages) * 0.8f
                        (chapPart + imgPart).coerceIn(0.02f, 0.99f)
                    }
                    totalChapters > 0 -> (completedChapters.toFloat() / totalChapters).coerceIn(0.02f, 0.99f)
                    downloadState.busy -> 0.05f
                    else -> 0f
                }

                // Progress Status Header & Percentage
                val statusTitle = when {
                    downloadState.completedUri != null -> "EPUB 生成完成！"
                    downloadState.awaitingFailureDecision -> "⚠️ 插图下载完成，部分失败"
                    downloadState.paused -> "下载已暂停"
                    downloadState.busy -> if (totalImages > 0) "正在处理插图并打包..." else "正在生成章节文本..."
                    downloadState.message != null -> downloadState.message
                    else -> "准备下载..."
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = statusTitle,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = if (downloadState.awaitingFailureDecision) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = "${(progressFraction * 100).toInt()}%",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }

                // Progress Bar
                LinearProgressIndicator(
                    progress = { progressFraction },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                )

                // Stats Subtitle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    if (totalChapters > 0) {
                        Text(
                            text = "章节: $completedChapters / $totalChapters",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    if (downloadState.format != NativeBookDownloadFormat.Txt && totalImages > 0) {
                        val successCount = (completedImages - failedImages).coerceAtLeast(0)
                        Text(
                            text = "插图: $completedImages / $totalImages (成功 $successCount / 失败 $failedImages)",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (failedImages > 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                // Console / Logs Area (1:1 with NovalPie console card)
                val logs = downloadState.logs
                val listState = rememberLazyListState()
                LaunchedEffect(logs.size) {
                    if (logs.isNotEmpty()) {
                        listState.animateScrollToItem(logs.size - 1)
                    }
                }

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp),
                    shape = RoundedCornerShape(8.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)),
                ) {
                    if (logs.isEmpty()) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = "等待日志输出...",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                            )
                        }
                    } else {
                        LazyColumn(
                            state = listState,
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            items(logs) { line ->
                                val lineColor = when {
                                    line.contains("失败") || line.contains("错误") -> MaterialTheme.colorScheme.error
                                    line.contains("重试") -> Color(0xFFE65100)
                                    line.contains("完成") || line.contains("成功") -> Color(0xFF2E7D32)
                                    line.contains("正在") || line.contains("开始") -> MaterialTheme.colorScheme.onSurface
                                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                                }
                                Text(
                                    text = line,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 11.sp,
                                    lineHeight = 16.sp,
                                    color = lineColor,
                                )
                            }
                        }
                    }
                }

                // Failure Decision Prompt Card (Two-Choice Card)
                if (downloadState.awaitingFailureDecision) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.85f)
                        ),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f)),
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Filled.Warning,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(18.dp),
                                )
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    text = "有 ${downloadState.failedImageCount} 张插图重试后仍下载失败",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                )
                            }
                            Text(
                                text = "您可以切换 Wi-Fi/移动网络或节点后重试失败插图；或直接完成打包（缺图处保留占位）。",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                OutlinedButton(
                                    onClick = { onResolveDecision(DownloadFailureDecision.RetryWithNewNetwork) },
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        contentColor = MaterialTheme.colorScheme.onErrorContainer
                                    ),
                                ) {
                                    Text("换网络后重试", maxLines = 1)
                                }
                                Button(
                                    onClick = { onResolveDecision(DownloadFailureDecision.PackageAnyway) },
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.error,
                                        contentColor = MaterialTheme.colorScheme.onError,
                                    ),
                                ) {
                                    Text("直接打包", maxLines = 1)
                                }
                            }
                        }
                    }
                }

                // Action Buttons at Bottom
                if (downloadState.completedUri != null) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Button(
                            onClick = onOpenFile,
                            modifier = Modifier.weight(1f),
                        ) {
                            Text("打开")
                        }
                        OutlinedButton(
                            onClick = onShareFile,
                            modifier = Modifier.weight(1f),
                        ) {
                            Text("分享")
                        }
                        TextButton(onClick = onDismiss) {
                            Text("关闭")
                        }
                    }
                } else if (downloadState.busy && !downloadState.awaitingFailureDecision) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Button(
                            onClick = onDismiss,
                            modifier = Modifier.weight(1f),
                        ) {
                            Text("后台运行")
                        }
                        OutlinedButton(
                            onClick = onTogglePause,
                        ) {
                            Text(if (downloadState.paused) "继续" else "暂停")
                        }
                        TextButton(
                            onClick = onCancel,
                            colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                        ) {
                            Text("取消")
                        }
                    }
                } else if (!downloadState.busy && downloadState.canRetry) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Button(
                            onClick = onRetry,
                            modifier = Modifier.weight(1f),
                        ) {
                            Text("重试下载")
                        }
                        OutlinedButton(
                            onClick = onDismiss,
                            modifier = Modifier.weight(1f),
                        ) {
                            Text("关闭")
                        }
                    }
                }
            }
        }
    }
}
