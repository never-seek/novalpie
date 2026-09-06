package com.novalpie.nativeapp.audit

/** Debug-only reader harness with the same phone orientation contract as MainActivity. */
class ReaderTestActivity:androidx.activity.ComponentActivity() {
    override fun onCreate(savedInstanceState:android.os.Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }
}
