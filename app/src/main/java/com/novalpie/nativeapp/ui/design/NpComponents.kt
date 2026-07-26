package com.novalpie.nativeapp.ui.design

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview

/**
 * Shared structural components.
 *
 * Each of these replaces a pattern that had been re-implemented per screen with slightly different
 * padding, colour and type every time.
 */

/**
 * A section heading.
 *
 * Carries `heading()` semantics, which none of the app's 89 hand-rolled section headers did, so
 * TalkBack users had no way to jump between sections of a long screen. The optional trailing action
 * exists because the common shape was a heading with a "刷新" or "更多" button beside it, previously
 * built ad hoc with `Row(SpaceBetween)` and an un-weighted `Text` that overflowed on narrow screens.
 */
@Composable
fun NpSectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = NovalPieSpacing.lg, bottom = NovalPieSpacing.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.semantics { heading() },
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        if (actionLabel != null && onAction != null) {
            TextButton(onClick = onAction) { Text(actionLabel) }
        }
    }
}

/**
 * The app's card.
 *
 * Cards previously measured 1.04:1 against the page in light mode and 1.27:1 in dark — effectively
 * invisible, which is why screens read as undifferentiated flat blocks. Separation now comes from
 * two cheap, theme-correct sources at once: an explicit container colour from the surface ramp, and
 * a hairline outline. Elevation stays at 1dp because a reading app should not look like floating
 * paper.
 */
@Composable
fun NpCard(
    modifier: Modifier = Modifier,
    contentPadding: androidx.compose.ui.unit.Dp = NovalPieSpacing.lg,
    content: @Composable ColumnScope.() -> Unit,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = NovalPieElevation.card),
        border = BorderStroke(NovalPieSize.hairline, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(
            modifier = Modifier.padding(contentPadding),
            verticalArrangement = Arrangement.spacedBy(NovalPieSpacing.sm),
            content = content,
        )
    }
}

/**
 * The app's search input.
 *
 * There were two: the bookshelf used a pill-shaped `Surface` with a separate 搜索 button, while
 * Discover used an `OutlinedTextField` with a floating label. Same job, two looks, on adjacent tabs.
 *
 * Submitting dismisses the keyboard, which neither original did — the soft keyboard stayed up
 * covering the results the user had just asked for.
 */
@Composable
fun NpSearchField(
    value: String,
    onValueChange: (String) -> Unit,
    onSearch: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    clearContentDescription: String,
) {
    val focusManager = LocalFocusManager.current
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier.fillMaxWidth(),
        placeholder = { Text(placeholder, style = MaterialTheme.typography.bodyMedium) },
        leadingIcon = {
            Icon(
                Icons.Filled.Search,
                contentDescription = null,
                modifier = Modifier.size(NovalPieSize.iconMd),
            )
        },
        trailingIcon = {
            if (value.isNotEmpty()) {
                IconButton(onClick = { onValueChange("") }) {
                    Icon(
                        Icons.Filled.Close,
                        contentDescription = clearContentDescription,
                        modifier = Modifier.size(NovalPieSize.iconMd),
                    )
                }
            }
        },
        singleLine = true,
        shape = MaterialTheme.shapes.large,
        textStyle = MaterialTheme.typography.bodyMedium,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(
            onSearch = {
                focusManager.clearFocus()
                onSearch(value)
            },
        ),
    )
}

@Preview(name = "Components", showBackground = true)
@Composable
private fun NpComponentsPreview() {
    com.novalpie.nativeapp.ui.NovalPieTheme {
        Column(modifier = Modifier.padding(NovalPieSpacing.lg)) {
            NpSearchField(
                value = "奇幻",
                onValueChange = {},
                onSearch = {},
                placeholder = "搜索作品、作者或标签",
                clearContentDescription = "清除关键词",
            )
            NpSectionHeader(
                title = "收藏书籍",
                subtitle = "20 部作品",
                actionLabel = "刷新",
                onAction = {},
            )
            NpCard {
                Text("卡片内容", style = MaterialTheme.typography.bodyMedium)
                NpChipRow {
                    NpChip("12.4万字")
                    NpChip("已完结", NpChipTone.Status)
                }
            }
        }
    }
}
