package com.novalpie.nativeapp.data

import android.content.Context
import android.os.Build
import coil.Coil
import coil.ImageLoader
import coil.annotation.ExperimentalCoilApi
import coil.decode.BitmapFactoryDecoder
import coil.decode.GifDecoder
import coil.decode.ImageDecoderDecoder
import coil.request.CachePolicy
import coil.request.Disposable
import coil.request.ImageRequest
import coil.size.Precision
import kotlinx.coroutines.Dispatchers
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit
import kotlin.math.roundToInt

private const val IMAGE_USER_AGENT = "NovalPieNative/2.0 Android"
private const val IMAGE_REFERER = "https://novalpie.cc/"

/** Shared size for two-column book cards and their background preload requests. */
internal const val NOVALPIE_BOOK_COVER_WIDTH_PX = 512
internal const val NOVALPIE_BOOK_COVER_HEIGHT_PX = 768
private const val NOVALPIE_MIN_THUMBNAIL_WIDTH_PX = 96
private const val NOVALPIE_MIN_THUMBNAIL_HEIGHT_PX = 144
/** Keep speculative cover work below the visible grid's network demand. */
internal const val NOVALPIE_BOOK_COVER_PREFETCH_BATCH_SIZE = 4
private const val NOVALPIE_BITMAP_FACTORY_MAX_PARALLELISM = 2
private const val NOVALPIE_STATIC_IMAGE_CACHE_PREFIX = "novalpie-static-image:"

// Source covers are original files rather than CDN thumbnails. Limit background fetches so a
// quick search scroll never makes the visible two-card row wait behind an off-screen batch.
private val coverPrefetchDispatcher = Dispatchers.IO.limitedParallelism(2)

/** Visible cards and the just-returned first row must never wait behind speculative scrolling. */
internal enum class NovelCoverLoadPriority {
    Visible,
    Speculative,
}

internal data class NovelCoverRequestSize(
    val widthPx: Int,
    val heightPx: Int,
)

/** Keep tiny forum thumbnails cheap while retaining the shared high-resolution default elsewhere. */
internal fun novalPieBookCoverTargetSize(
    widthPx: Int?,
    heightPx: Int?,
): NovelCoverRequestSize {
    if (widthPx == null || heightPx == null || widthPx <= 0 || heightPx <= 0) {
        return NovelCoverRequestSize(
            widthPx = NOVALPIE_BOOK_COVER_WIDTH_PX,
            heightPx = NOVALPIE_BOOK_COVER_HEIGHT_PX,
        )
    }
    val boundedWidth = widthPx.coerceIn(
        NOVALPIE_MIN_THUMBNAIL_WIDTH_PX,
        NOVALPIE_BOOK_COVER_WIDTH_PX,
    )
    val boundedHeight = (boundedWidth * 1.5f).roundToInt().coerceIn(
        NOVALPIE_MIN_THUMBNAIL_HEIGHT_PX,
        NOVALPIE_BOOK_COVER_HEIGHT_PX,
    )
    return NovelCoverRequestSize(boundedWidth, boundedHeight)
}

internal fun NovelCoverLoadPriority.usesBackgroundDispatcher(): Boolean =
    this == NovelCoverLoadPriority.Speculative

fun configureNovalPieImageLoader(context: Context, proxySettings: ProxySettings) {
    Coil.setImageLoader(buildNovalPieImageLoader(context.applicationContext, proxySettings))
}

/** Clears the actual Coil image caches used by book covers and reader illustrations. */
@OptIn(ExperimentalCoilApi::class)
fun clearNovalPieImageCaches(context: Context) {
    val imageLoader = Coil.imageLoader(context.applicationContext)
    imageLoader.memoryCache?.clear()
    imageLoader.diskCache?.clear()
}

internal fun buildNovalPieImageLoader(
    context: Context,
    proxySettings: ProxySettings,
    emulatorRuntime: Boolean = isEmulatorRuntime(),
): ImageLoader =
    ImageLoader.Builder(context.applicationContext)
        .okHttpClient { novalPieImageOkHttpClient(proxySettings, emulatorRuntime) }
        // The source uses GIF and Animated WebP covers. Coil's base Compose artifact decodes
        // only the first frame. Prefer Coil's MovieDrawable for GIFs: unlike the platform
        // AnimatedImageDrawable it keeps one bounded software frame buffer on MuMu while still
        // playing the real inner animation. Animated WebP/HEIF continues through the platform
        // decoder below.
        .components {
            add(GifDecoder.Factory())
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                add(ImageDecoderDecoder.Factory())
            }
        }
        // Source covers are high-resolution originals. Decoding four at once saturates the CPU
        // during a fast grid fling; two queued decodes retain the same image quality and leave a
        // core available for Compose's next frame.
        .bitmapFactoryMaxParallelism(NOVALPIE_BITMAP_FACTORY_MAX_PARALLELISM)
        .crossfade(true)
        .build()

/**
 * Keeps visible cards and search preloads on the exact same Coil cache key.  Without one shared
 * request shape, a background fetch can populate the disk cache but still miss the in-memory
 * bitmap used by the card after a fast scroll.
 */
internal fun novalPieBookCoverRequest(
    context: Context,
    url: String,
    priority: NovelCoverLoadPriority = NovelCoverLoadPriority.Visible,
    targetSize: NovelCoverRequestSize = NovelCoverRequestSize(
        widthPx = NOVALPIE_BOOK_COVER_WIDTH_PX,
        heightPx = NOVALPIE_BOOK_COVER_HEIGHT_PX,
    ),
): ImageRequest =
    ImageRequest.Builder(context.applicationContext)
        .data(url)
        .size(targetSize.widthPx, targetSize.heightPx)
        .precision(Precision.INEXACT)
        .memoryCachePolicy(CachePolicy.ENABLED)
        .diskCachePolicy(CachePolicy.ENABLED)
        .networkCachePolicy(CachePolicy.ENABLED)
        .crossfade(false)
        .apply {
            if (priority.usesBackgroundDispatcher()) fetcherDispatcher(coverPrefetchDispatcher)
        }
        .build()

/**
 * Builds a small first-frame request for UI chrome such as forum avatars, avatar frames, badges,
 * and review-card covers. Those assets are displayed at a fixed, tiny size; keeping the source
 * GIF/WebP animation alive for every recycled row makes the render thread decode frames forever.
 * Full-size covers and reader illustrations continue to use [novalPieBookCoverRequest] so their
 * source animation remains available where it is useful.
 */
internal fun novalPieStaticImageRequest(
    context: Context,
    url: String,
    widthPx: Int,
    heightPx: Int,
): ImageRequest {
    val boundedWidth = widthPx.coerceAtLeast(1)
    val boundedHeight = heightPx.coerceAtLeast(1)
    val cacheKey = "$NOVALPIE_STATIC_IMAGE_CACHE_PREFIX$url:${boundedWidth}x$boundedHeight"
    return ImageRequest.Builder(context.applicationContext)
        .data(url)
        .size(boundedWidth, boundedHeight)
        .precision(Precision.INEXACT)
        // BitmapFactory decodes animated GIF/WebP sources as a still bitmap, avoiding an
        // AnimatedImageDrawable and preserving a separate cache entry from animated surfaces.
        .decoderFactory(BitmapFactoryDecoder.Factory())
        .memoryCacheKey(cacheKey)
        .diskCacheKey(cacheKey)
        .crossfade(false)
        .build()
}

/**
 * Starts one bounded, best-effort preload batch. The caller owns the returned disposables and
 * cancels them when its search target changes, so a fast fling cannot leave stale background
 * requests competing with the covers on screen. [maxPreloadCount] stays small at each call site:
 * scrolling uses one row look-ahead while a newly returned result page warms just its first
 * visible row before the user reaches it.
 */
internal fun preloadNovalPieBookCovers(
    context: Context,
    urls: Iterable<String>,
    maxPreloadCount: Int = NOVALPIE_BOOK_COVER_PREFETCH_BATCH_SIZE,
    priority: NovelCoverLoadPriority = NovelCoverLoadPriority.Speculative,
): List<Disposable> {
    if (maxPreloadCount <= 0) return emptyList()
    val imageLoader = Coil.imageLoader(context.applicationContext)
    return urls.asSequence()
        .map(String::trim)
        .filter(String::isNotBlank)
        .distinct()
        .take(maxPreloadCount)
        .map { url -> imageLoader.enqueue(novalPieBookCoverRequest(context, url, priority = priority)) }
        .toList()
}

/**
 * [emulatorRuntime] is a parameter rather than an internal call so that both routing paths can be
 * tested deterministically. Reading ambient `Build` state made the emulator branch untestable: in a
 * plain JUnit test the fields are unstubbed, so the value was whatever the environment happened to
 * yield.
 */
internal fun novalPieImageOkHttpClient(
    proxySettings: ProxySettings,
    emulatorRuntime: Boolean = isEmulatorRuntime(),
): OkHttpClient {
    val builder = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(45, TimeUnit.SECONDS)
        .callTimeout(60, TimeUnit.SECONDS)
        .addInterceptor { chain ->
            val request = chain.request().newBuilder()
                .header("user-agent", IMAGE_USER_AGENT)
                .header("referer", IMAGE_REFERER)
                .build()
            chain.proceed(request)
        }

    builder.proxySelector(proxySettings.toProxySelector(emulatorRuntime = emulatorRuntime))

    return builder.build()
}
