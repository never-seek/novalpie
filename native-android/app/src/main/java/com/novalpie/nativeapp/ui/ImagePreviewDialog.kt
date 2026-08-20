package com.novalpie.nativeapp.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FitScreen
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material.icons.filled.ZoomOut
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import coil.size.Precision

internal fun clampImagePreviewScale(value: Float): Float = value.coerceIn(1f, 6f)

internal fun clampImagePreviewOffset(offset: Offset, scale: Float, viewport: IntSize): Offset {
    if (scale <= 1f || viewport.width <= 0 || viewport.height <= 0) return Offset.Zero
    val maxX = viewport.width * (scale - 1f) / 2f
    val maxY = viewport.height * (scale - 1f) / 2f
    return Offset(offset.x.coerceIn(-maxX, maxX), offset.y.coerceIn(-maxY, maxY))
}

/**
 * Preview images are displayed inside a phone-sized viewport. Keeping a modest upper bound avoids
 * turning every frame of an animated GIF into a 3072px bitmap, while [Precision.INEXACT] keeps
 * smaller originals at their native size instead of upscaling them.
 */
internal data class ImagePreviewLoadPolicy(
    val maxWidthPx: Int,
    val maxHeightPx: Int,
    val precision: Precision,
)

internal fun imagePreviewLoadPolicy(): ImagePreviewLoadPolicy = ImagePreviewLoadPolicy(
    maxWidthPx = 1440,
    maxHeightPx = 2160,
    precision = Precision.INEXACT,
)

/**
 * A full-width [Dialog] may receive a zero navigation-bar inset in gesture mode even though its
 * final pixels are clipped by the display edge. Keep the icon touch targets above that edge on
 * every device, then add the reported navigation-bar inset when one is available.
 */
internal fun imagePreviewBottomSafePadding() = 16.dp

/** A resolved full-screen image can be displayed, while a lazy original lookup owns the dialog. */
@Composable
internal fun ImagePreviewHost(
    state: ImagePreviewState,
    onDismiss: () -> Unit,
) {
    if (state.title.isBlank()) return
    when {
        state.loading -> ImagePreviewLoadingDialog(title = state.title, onDismiss = onDismiss)
        !state.displayUrl.isNullOrBlank() -> ImagePreviewDialog(
            imageUrl = state.displayUrl,
            title = state.title,
            onDismiss = onDismiss,
        )
        else -> ImagePreviewUnavailableDialog(title = state.title, onDismiss = onDismiss)
    }
}

@Composable
private fun ImagePreviewLoadingDialog(title: String, onDismiss: () -> Unit) =
    ImagePreviewStatusDialog(title = title, message = "正在加载原图…", onDismiss = onDismiss) {
        CircularProgressIndicator(color = Color.White)
    }

@Composable
private fun ImagePreviewUnavailableDialog(title: String, onDismiss: () -> Unit) =
    ImagePreviewStatusDialog(title = title, message = "原图加载失败", onDismiss = onDismiss)

@Composable
private fun ImagePreviewStatusDialog(
    title: String,
    message: String,
    onDismiss: () -> Unit,
    indicator: @Composable (() -> Unit)? = null,
) {
    BackHandler(onBack = onDismiss)
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, dismissOnBackPress = true, dismissOnClickOutside = false),
    ) {
        Column(
            modifier = Modifier.fillMaxSize().background(Color(0xFF0B0D12)),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(title, color = Color.White, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Box(modifier = Modifier.padding(top = 20.dp), contentAlignment = Alignment.Center) {
                indicator?.invoke() ?: Text(message, color = Color.White)
            }
            if (indicator != null) {
                Text(message, modifier = Modifier.padding(top = 14.dp), color = Color.White.copy(alpha = 0.72f))
            }
            IconButton(onClick = onDismiss, modifier = Modifier.padding(top = 20.dp)) {
                Icon(Icons.Filled.Close, contentDescription = "关闭大图", tint = Color.White)
            }
        }
    }
}

/** Full-screen preview with fixed chrome above and below the image viewport. */
@Composable
internal fun ImagePreviewDialog(
    imageUrl: String,
    title: String,
    onDismiss: () -> Unit
) {
    var scale by remember(imageUrl) { mutableFloatStateOf(1f) }
    var offset by remember(imageUrl) { mutableStateOf(Offset.Zero) }
    var viewport by remember(imageUrl) { mutableStateOf(IntSize.Zero) }
    val context = LocalContext.current
    val loadPolicy = imagePreviewLoadPolicy()
    // Keep one request across zoom/pan recompositions. Rebuilding the model during a gesture can
    // restart an animated Drawable at frame zero, which makes GIF covers look like static images.
    val imageRequest = remember(imageUrl, context) {
        ImageRequest.Builder(context)
            .data(imageUrl)
            .size(loadPolicy.maxWidthPx, loadPolicy.maxHeightPx)
            .precision(loadPolicy.precision)
            .crossfade(false)
            .build()
    }
    val transformState = rememberTransformableState { zoomChange, panChange, _ ->
        val nextScale = clampImagePreviewScale(scale * zoomChange)
        scale = nextScale
        offset = clampImagePreviewOffset(offset + panChange, nextScale, viewport)
    }
    fun setScale(next: Float) {
        scale = clampImagePreviewScale(next)
        offset = clampImagePreviewOffset(offset, scale, viewport)
    }
    fun reset() {
        scale = 1f
        offset = Offset.Zero
    }

    // The preview must own Back while it is on screen. Relying on Dialog's platform callback alone
    // allowed the underlying reader route to consume Back first on some emulator builds.
    BackHandler(onBack = onDismiss)

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, dismissOnBackPress = true, dismissOnClickOutside = false)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF0B0D12))
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(bottom = imagePreviewBottomSafePadding()),
                color = Color.Black.copy(alpha = 0.72f)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(title, color = Color.White, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text("双击复原/放大 · 双指缩放 · 放大后拖动", color = Color.White.copy(alpha = 0.72f), style = MaterialTheme.typography.labelSmall)
                    }
                    Text("${(scale * 100).toInt()}%", color = Color.White, style = MaterialTheme.typography.labelMedium)
                    IconButton(onClick = onDismiss) { Icon(Icons.Filled.Close, "关闭大图", tint = Color.White) }
                }
            }

            // The image receives its own measured viewport between fixed chrome regions. This
            // guarantees ContentScale.Fit can show the entire original without a floating tool
            // bar covering its bottom edge.
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .onSizeChanged { viewport = it },
                contentAlignment = Alignment.Center,
            ) {
                SubcomposeAsyncImage(
                    model = imageRequest,
                    contentDescription = "$title 大图",
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            scaleX = scale
                            scaleY = scale
                            translationX = offset.x
                            translationY = offset.y
                        }
                        .pointerInput(imageUrl) {
                            detectTapGestures(
                                onDoubleTap = {
                                    if (scale > 1.05f) reset() else setScale(2.5f)
                                }
                            )
                        }
                        .transformable(transformState),
                    contentScale = ContentScale.Fit,
                    loading = { Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = Color.White) } },
                    error = {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("大图加载失败", color = Color.White)
                        }
                    }
                )
            }

            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = Color.Black.copy(alpha = 0.72f)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { setScale(scale - 0.5f) }) { Icon(Icons.Filled.ZoomOut, "缩小", tint = Color.White) }
                    IconButton(onClick = ::reset) { Icon(Icons.Filled.FitScreen, "适应屏幕", tint = Color.White) }
                    IconButton(onClick = ::reset) { Icon(Icons.Filled.Refresh, "还原", tint = Color.White) }
                    IconButton(onClick = { setScale(scale + 0.5f) }) { Icon(Icons.Filled.ZoomIn, "放大", tint = Color.White) }
                }
            }
        }
    }
}
