package com.novalpie.nativeapp.ui

import android.view.Window
import androidx.compose.runtime.*

/** App-window-only brightness. Never changes Settings.System or requests WRITE_SETTINGS. */
internal fun normalizeReaderBrightness(value:Float):Float =
    if(!value.isFinite()||value<0f)-1f else value.coerceIn(.01f,1f)

internal class ReaderBrightnessLease(private val window:Window) {
    private val previous=window.attributes.screenBrightness
    fun apply(value:Float) {
        val next=normalizeReaderBrightness(value)
        if(window.attributes.screenBrightness==next)return
        window.attributes=window.attributes.apply {screenBrightness=next}
    }
    fun restore() {window.attributes=window.attributes.apply {screenBrightness=previous}}
}

@Composable
internal fun ReaderWindowBrightness(window:Window?,active:Boolean,value:Float) {
    val lease=remember(window,active) { if(active&&window!=null)ReaderBrightnessLease(window) else null }
    DisposableEffect(lease) { onDispose {lease?.restore()} }
    SideEffect {lease?.apply(value)}
}
