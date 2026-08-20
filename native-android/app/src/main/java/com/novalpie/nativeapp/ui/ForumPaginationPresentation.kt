package com.novalpie.nativeapp.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp
import com.novalpie.nativeapp.ui.design.NovalPieRadius
import com.novalpie.nativeapp.ui.design.NovalPieSize
import com.novalpie.nativeapp.ui.design.NovalPieSpacing

internal sealed interface ForumPaginationToken {
    data class Page(val number: Int) : ForumPaginationToken
    object Ellipsis : ForumPaginationToken
}

internal data class ForumPaginationWindow(
    val currentPage: Int,
    val totalPages: Int,
    val tokens: List<ForumPaginationToken>,
    val previousPage: Int?,
    val nextPage: Int?
)

/** Mirrors the source footer: first/last pages stay reachable even for the 1,000+ review pages. */
internal fun forumPaginationWindow(
    page: Int,
    totalPages: Int?,
    maxVisiblePages: Int = 5
): ForumPaginationWindow {
    require(maxVisiblePages > 0) { "maxVisiblePages must be positive" }
    val normalizedTotal = (totalPages ?: page).coerceAtLeast(1)
    val currentPage = page.coerceIn(1, normalizedTotal)
    val visibleCount = minOf(maxVisiblePages, normalizedTotal)
    val maxStart = (normalizedTotal - visibleCount + 1).coerceAtLeast(1)
    val start = (currentPage - visibleCount / 2).coerceIn(1, maxStart)
    val end = start + visibleCount - 1
    val tokens = buildList {
        if (start > 1) add(ForumPaginationToken.Page(1))
        if (start > 2) add(ForumPaginationToken.Ellipsis)
        for (number in start..end) add(ForumPaginationToken.Page(number))
        if (end < normalizedTotal - 1) add(ForumPaginationToken.Ellipsis)
        if (end < normalizedTotal) add(ForumPaginationToken.Page(normalizedTotal))
    }
    return ForumPaginationWindow(
        currentPage = currentPage,
        totalPages = normalizedTotal,
        tokens = tokens,
        previousPage = (currentPage - 1).takeIf { it >= 1 },
        nextPage = (currentPage + 1).takeIf { it <= normalizedTotal }
    )
}

internal fun forumPageJumpTarget(value: String, totalPages: Int): Int? =
    value.trim().toIntOrNull()?.takeIf { it in 1..totalPages.coerceAtLeast(1) }

@Composable
internal fun ForumPaginationBar(
    page: Int,
    totalPages: Int?,
    loading: Boolean,
    onGoToPage: (Int) -> Unit
) {
    val window = forumPaginationWindow(page = page, totalPages = totalPages)
    var jumpValue by remember(window.currentPage, window.totalPages) { mutableStateOf("") }
    var jumpDialogVisible by remember { mutableStateOf(false) }
    val jumpTarget = forumPageJumpTarget(jumpValue, window.totalPages)

    if (jumpDialogVisible) {
        AlertDialog(
            onDismissRequest = { jumpDialogVisible = false },
            title = { Text("跳转页码") },
            text = {
                OutlinedTextField(
                    value = jumpValue,
                    onValueChange = { value -> jumpValue = value.filter(Char::isDigit).take(7) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    label = { Text("页码（共 ${window.totalPages} 页）") },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go),
                    keyboardActions = KeyboardActions(
                        onGo = {
                            if (!loading && jumpTarget != null) {
                                jumpDialogVisible = false
                                onGoToPage(jumpTarget)
                            }
                        }
                    )
                )
            },
            confirmButton = {
                TextButton(
                    enabled = !loading && jumpTarget != null,
                    onClick = {
                        jumpTarget?.let { target ->
                            jumpDialogVisible = false
                            onGoToPage(target)
                        }
                    }
                ) { Text("跳转") }
            },
            dismissButton = { TextButton(onClick = { jumpDialogVisible = false }) { Text("取消") } }
        )
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(NovalPieSpacing.xs)
    ) {
        Text(
            text = "第 ${window.currentPage} / ${window.totalPages} 页",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = NovalPieSpacing.xxs),
            horizontalArrangement = Arrangement.spacedBy(NovalPieSpacing.xs),
            verticalAlignment = Alignment.CenterVertically
        ) {
            item(key = "forum-page-previous") {
                ForumCompactPagerCell(
                    label = "‹",
                    contentDescription = "上一页",
                    enabled = !loading && window.previousPage != null,
                    onClick = { window.previousPage?.let(onGoToPage) }
                )
            }
            window.tokens.forEachIndexed { index, token ->
                when (token) {
                    is ForumPaginationToken.Page -> item(key = "forum-page-${token.number}") {
                        ForumCompactPagerCell(
                            label = token.number.toString(),
                            contentDescription = "第 ${token.number} 页",
                            selected = token.number == window.currentPage,
                            enabled = !loading,
                            onClick = { onGoToPage(token.number) }
                        )
                    }
                    ForumPaginationToken.Ellipsis -> item(key = "forum-page-ellipsis-$index") {
                        Box(
                            modifier = Modifier.size(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("…", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
            item(key = "forum-page-next") {
                ForumCompactPagerCell(
                    label = "›",
                    contentDescription = "下一页",
                    enabled = !loading && window.nextPage != null,
                    onClick = { window.nextPage?.let(onGoToPage) }
                )
            }
            item(key = "forum-page-jump") {
                Surface(
                    modifier = Modifier
                        .height(32.dp)
                        .clickable(enabled = !loading) { jumpDialogVisible = true },
                    shape = RoundedCornerShape(NovalPieRadius.sm),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = NovalPieSpacing.sm),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("共 ${window.totalPages} 页", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }
    }
}

@Composable
private fun ForumCompactPagerCell(
    label: String,
    contentDescription: String,
    enabled: Boolean,
    selected: Boolean = false,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .size(32.dp)
            .semantics { this.contentDescription = contentDescription }
            .clickable(enabled = enabled, onClick = onClick),
        shape = RoundedCornerShape(NovalPieRadius.sm),
        color = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
        contentColor = if (selected) MaterialTheme.colorScheme.onPrimaryContainer
        else MaterialTheme.colorScheme.onSurfaceVariant,
        border = if (selected) null else BorderStroke(
            NovalPieSize.hairline,
            MaterialTheme.colorScheme.outlineVariant
        )
    ) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
            )
        }
    }
}
