package com.novalpie.nativeapp.ui.design

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material.icons.outlined.Inbox
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Error, empty and loading states.
 *
 * The app had five different notice idioms. The most-used one, `ErrorBlock` (19 call sites), was
 * the only one that did *not* use the error colour role, so most failures looked like ordinary
 * neutral cards; the other four used Material's unbranded pink `errorContainer`. Loading was a
 * progress bar plus a Chinese sentence in 36 places, with no layout reservation, so content jumped
 * every time a request finished.
 */

/**
 * A failed request.
 *
 * [onRetry] is required rather than optional on purpose. The reader previously rendered its error
 * card with no top bar and no retry, leaving the screen with no visible way out — the only escape
 * was the system back gesture. A state that cannot be recovered from should not be representable.
 */
@Composable
fun NpErrorState(
    message: String,
    retryLabel: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
    secondaryLabel: String? = null,
    onSecondary: (() -> Unit)? = null,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .background(MaterialTheme.colorScheme.errorContainer)
            .padding(NovalPieSpacing.lg),
        verticalArrangement = Arrangement.spacedBy(NovalPieSpacing.md),
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(NovalPieSpacing.sm),
            verticalAlignment = Alignment.Top,
        ) {
            Icon(
                imageVector = Icons.Outlined.CloudOff,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onErrorContainer,
                modifier = Modifier.size(NovalPieSize.iconMd),
            )
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onErrorContainer,
                modifier = Modifier.weight(1f),
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(NovalPieSpacing.sm)) {
            Button(onClick = onRetry) { Text(retryLabel) }
            if (secondaryLabel != null && onSecondary != null) {
                TextButton(onClick = onSecondary) { Text(secondaryLabel) }
            }
        }
    }
}

/**
 * A successful request that returned nothing.
 *
 * Distinct from [NpErrorState] because the two mean different things and the old UI conflated them:
 * an empty bookshelf is not a failure and should not be red. [actionLabel] is optional — some empty
 * states genuinely have no next step.
 */
@Composable
fun NpEmptyState(
    title: String,
    modifier: Modifier = Modifier,
    description: String? = null,
    icon: ImageVector = Icons.Outlined.Inbox,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = NovalPieSpacing.lg, vertical = NovalPieSpacing.xxl),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(NovalPieSpacing.md),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .size(48.dp)
                // Decorative, so it stays quiet rather than competing with the message.
                .alpha(0.6f),
        )
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
        )
        if (description != null) {
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
        if (actionLabel != null && onAction != null) {
            Button(onClick = onAction) { Text(actionLabel) }
        }
    }
}

/**
 * A shimmering placeholder block.
 *
 * Reserves the space the real content will occupy, which is the point: the previous "progress bar
 * plus sentence" approach occupied a few dozen pixels and then the arriving content shoved
 * everything down the page.
 */
@Composable
fun NpSkeleton(
    modifier: Modifier = Modifier,
    height: Dp = 16.dp,
    widthFraction: Float = 1f,
    shape: RoundedCornerShape = RoundedCornerShape(NovalPieRadius.xs),
) {
    val transition = rememberInfiniteTransition(label = "skeleton")
    val alpha by transition.animateFloat(
        initialValue = 0.35f,
        targetValue = 0.75f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 900, easing = NovalPieMotion.standard),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "skeletonAlpha",
    )
    Box(
        modifier = modifier
            .fillMaxWidth(widthFraction)
            .height(height)
            .clip(shape)
            .background(MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = alpha)),
    )
}

/** Skeleton shaped like a book row: fixed 2:3 cover slot plus three text lines. */
@Composable
fun NpBookRowSkeleton(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = NovalPieSpacing.sm),
        horizontalArrangement = Arrangement.spacedBy(NovalPieSpacing.md),
    ) {
        NpSkeleton(
            modifier = Modifier.width(NovalPieSize.coverWidthRow),
            height = NovalPieSize.coverWidthRow / NovalPieSize.coverAspectRatio,
            shape = RoundedCornerShape(NovalPieRadius.sm),
        )
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(NovalPieSpacing.sm),
        ) {
            NpSkeleton(height = 18.dp, widthFraction = 0.7f)
            NpSkeleton(height = 14.dp, widthFraction = 0.45f)
            NpSkeleton(height = 14.dp, widthFraction = 0.9f)
        }
    }
}

@Preview(name = "States", showBackground = true)
@Composable
private fun NpStatesPreview() {
    com.novalpie.nativeapp.ui.NovalPieTheme {
        Column(
            modifier = Modifier.padding(NovalPieSpacing.lg),
            verticalArrangement = Arrangement.spacedBy(NovalPieSpacing.lg),
        ) {
            NpErrorState(
                message = "书架请求失败: 网络连接超时",
                retryLabel = "重试",
                onRetry = {},
                secondaryLabel = "网页收藏",
                onSecondary = {},
            )
            NpBookRowSkeleton()
            NpEmptyState(
                title = "书架还是空的",
                description = "收藏喜欢的作品后会出现在这里",
                actionLabel = "去搜索",
                onAction = {},
            )
        }
    }
}
