package com.novalpie.nativeapp.data

import android.content.Context
import coil.Coil
import coil.ImageLoader
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

private const val IMAGE_USER_AGENT = "NovalPieNative/2.0 Android"
private const val IMAGE_REFERER = "https://novalpie.cc/"

fun configureNovalPieImageLoader(context: Context, proxySettings: ProxySettings) {
    Coil.setImageLoader(buildNovalPieImageLoader(context.applicationContext, proxySettings))
}

internal fun buildNovalPieImageLoader(context: Context, proxySettings: ProxySettings): ImageLoader =
    ImageLoader.Builder(context.applicationContext)
        .okHttpClient { novalPieImageOkHttpClient(proxySettings) }
        .crossfade(true)
        .build()

internal fun novalPieImageOkHttpClient(proxySettings: ProxySettings): OkHttpClient {
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

    builder.proxySelector(
        proxySettings.toProxySelector(
            preferEmulatorProxy = shouldPreferEmulatorProxy()
        )
    )

    return builder.build()
}
