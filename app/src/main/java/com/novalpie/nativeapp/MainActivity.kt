package com.novalpie.nativeapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.novalpie.nativeapp.data.NetworkConfigStore
import com.novalpie.nativeapp.data.configureNovalPieImageLoader
import com.novalpie.nativeapp.ui.NovalPieApp
import com.novalpie.nativeapp.ui.NovalPieTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        configureNovalPieImageLoader(this, NetworkConfigStore(this).loadProxySettings())
        val startUri = intent?.data?.toString()
        setContent {
            NovalPieTheme {
                NovalPieApp(startUri = startUri)
            }
        }
    }
}
