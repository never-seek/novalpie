package com.novalpie.app;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.webkit.JavascriptInterface;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;
import android.widget.LinearLayout;

import androidx.core.content.ContextCompat;
import com.getcapacitor.BridgeActivity;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Locale;

/**
 * NovalPie 主Activity
 * - 音量键拦截翻页（dispatchKeyEvent）
 * - 动态注入悬浮刷新按钮
 * - 手势检测隐藏/显示按钮
 * - TTS 接口
 */
public class MainActivity extends BridgeActivity {

    private static final String TAG = "NovalPie-Main";
    private static final int FAB_MARGIN_DP = 24;
    private static final int SCROLL_BUFFER_DP = 50;

    private WebView webView;
    private FloatingActionButton fabRefresh;
    private TextToSpeech tts;
    private boolean ttsInitialized = false;
    private ViewGroup webViewParent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate: 开始初始化");
    }

    /**
     * 动态初始化：在 Capacitor 初始化完成后获取 WebView 并注入原生 UI
     */
    private void initializeNativeUI() {
        Log.d(TAG, "initializeNativeUI: 开始获取 WebView");

        // 获取 WebView
        webView = getCapacitorWebView();
        if (webView == null) {
            webView = findWebViewInViewTree(getWindow().getDecorView());
        }

        if (webView == null) {
            Log.e(TAG, "无法获取 WebView，3秒后重试");
            getWindow().getDecorView().postDelayed(this::initializeNativeUI, 3000);
            return;
        }

        Log.d(TAG, "成功获取 WebView，开始注入原生 UI");

        // 获取 WebView 的父容器
        webViewParent = (ViewGroup) webView.getParent();
        if (webViewParent == null) {
            Log.e(TAG, "WebView 没有父容器");
            return;
        }

        // 初始化 TTS
        initTTS();

        // 配置 WebView
        configureWebView();

        // 动态创建并注入 FAB
        createFloatingActionButton();

        // 设置手势检测
        setupGestureDetector();

        // 加载网页
        webView.loadUrl("https://novalpie.cc");
    }

    /**
     * 获取 Capacitor Bridge 中的 WebView
     */
    private WebView getCapacitorWebView() {
        try {
            Method getBridge = BridgeActivity.class.getDeclaredMethod("getBridge");
            getBridge.setAccessible(true);
            Object bridge = getBridge.invoke(this);

            if (bridge != null) {
                Field webViewField = bridge.getClass().getDeclaredField("webView");
                webViewField.setAccessible(true);
                return (WebView) webViewField.get(bridge);
            }
        } catch (Exception e) {
            Log.e(TAG, "反射获取 WebView 失败: " + e.getClass().getSimpleName());
        }
        return null;
    }

    /**
     * 遍历视图树查找 WebView
     */
    private WebView findWebViewInViewTree(View view) {
        if (view == null) return null;

        if (view instanceof WebView) {
            Log.d(TAG, "找到 WebView");
            return (WebView) view;
        }

        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) {
                WebView found = findWebViewInViewTree(group.getChildAt(i));
                if (found != null) return found;
            }
        }

        return null;
    }

    /**
     * 配置 WebView
     */
    @SuppressLint("SetJavaScriptEnabled")
    private void configureWebView() {
        WebSettings settings = webView.getSettings();

        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setDatabaseEnabled(true);
        settings.setUseWideViewPort(true);
        settings.setLoadWithOverviewMode(true);
        settings.setSupportZoom(false);
        settings.setBuiltInZoomControls(false);
        settings.setDisplayZoomControls(false);
        settings.setCacheMode(WebSettings.LOAD_DEFAULT);
        settings.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
        settings.setDefaultTextEncodingName("UTF-8");
        settings.setTextSize(WebSettings.TextSize.NORMAL);

        // 修改 User-Agent 增加标识
        String ua = settings.getUserAgentString();
        settings.setUserAgentString(ua + " NovalPieApp/1.0");

        // 注入 JS 接口
        webView.addJavascriptInterface(new WebAppInterface(), "AndroidTTS");

        // 设置 WebViewClient
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                Log.d(TAG, "页面加载完成: " + url);
                
                // 核心修复：注入 TTS Polyfill，欺骗前端它拥有 Web Speech API，并桥接到原生
                String ttsPolyfill = "(function() {" +
                    "window.speechSynthesis = {" +
                    "    speak: function(utterance) {" +
                    "        if(window.AndroidTTS) window.AndroidTTS.speak(utterance.text);" +
                    "    }," +
                    "    cancel: function() {" +
                    "        if(window.AndroidTTS) window.AndroidTTS.stop();" +
                    "    }," +
                    "    pause: function() { if(window.AndroidTTS) window.AndroidTTS.stop(); }," +
                    "    resume: function() {}," +
                    "    getVoices: function() { return []; }" +
                    "};" +
                    "window.SpeechSynthesisUtterance = function(text) { this.text = text; };" +
                    "window.Android = window.AndroidTTS;" + 
                "})();";
                
                executeScript(ttsPolyfill);
            }
        });

        Log.d(TAG, "WebView 配置完成");
    }

    /**
     * 动态创建悬浮刷新按钮 (FAB)
     */
    private void createFloatingActionButton() {
        // 创建 FAB
        fabRefresh = new FloatingActionButton(this);

        // 设置颜色 (粉色主题 #ea4c89)
        fabRefresh.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.parseColor("#ea4c89")));

        // 设置图标
        Drawable refreshIcon = ContextCompat.getDrawable(this, android.R.drawable.ic_popup_sync);
        if (refreshIcon != null) {
            fabRefresh.setImageDrawable(refreshIcon);
        }

        // 设置内容描述
        fabRefresh.setContentDescription("刷新页面");

        // 设置大小和边距
        final int margin = dpToPx(FAB_MARGIN_DP);
        
        // 创建 FrameLayout.LayoutParams 以支持 gravity
        FrameLayout.LayoutParams fabParams = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT
        );
        fabParams.setMargins(margin, margin, margin, margin);
        fabParams.gravity = Gravity.BOTTOM | Gravity.END;
        fabRefresh.setLayoutParams(fabParams);

        // 点击刷新
        fabRefresh.setOnClickListener(v -> {
            Log.d(TAG, "FAB 点击: 刷新页面");
            if (webView != null) {
                webView.reload();
            }
        });

        // 添加到 WebView 的父容器
        webViewParent.addView(fabRefresh);

        Log.d(TAG, "FAB 创建并添加到父容器");
    }

    /**
     * 设置手势检测器
     */
    private void setupGestureDetector() {
        if (webView == null || fabRefresh == null) return;

        android.view.GestureDetector gestureDetector = new android.view.GestureDetector(this,
                new android.view.GestureDetector.SimpleOnGestureListener() {

            private static final int SWIPE_THRESHOLD = 20;
            private static final int DOUBLE_TAP_TIMEOUT = 300;

            private long lastDoubleTapTime = 0;

            @Override
            public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
                // distanceY > 0: 手指向上移动，页面向下滚动 -> 隐藏按钮
                // distanceY < 0: 手指向下移动，页面向上滚动 -> 显示按钮

                if (distanceY > SWIPE_THRESHOLD && fabRefresh.isShown()) {
                    Log.d(TAG, "手势: 上滑 -> 隐藏 FAB");
                    fabRefresh.hide();
                } else if (distanceY < -SWIPE_THRESHOLD && !fabRefresh.isShown()) {
                    Log.d(TAG, "手势: 下滑 -> 显示 FAB");
                    fabRefresh.show();
                }

                return false; // 返回 false 不拦截事件，让 WebView 继续处理
            }

            @Override
            public boolean onDoubleTap(MotionEvent e) {
                long currentTime = System.currentTimeMillis();
                if (currentTime - lastDoubleTapTime < DOUBLE_TAP_TIMEOUT * 2) {
                    Log.d(TAG, "手势: 双击 -> 显示 FAB");
                    if (!fabRefresh.isShown()) {
                        fabRefresh.show();
                    }
                }
                lastDoubleTapTime = currentTime;
                return false;
            }

            @Override
            public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
                return false; // 不拦截快速滑动
            }
        });

        // 设置触摸监听器
        webView.setOnTouchListener((v, event) -> {
            gestureDetector.onTouchEvent(event);
            return false; // 必须返回 false，不拦截事件
        });

        Log.d(TAG, "手势检测器设置完成");
    }

    /**
     * 初始化 TTS
     */
    private void initTTS() {
        tts = new TextToSpeech(this, status -> {
            if (status == TextToSpeech.SUCCESS) {
                int result = tts.setLanguage(Locale.CHINESE);
                if (result == TextToSpeech.LANG_MISSING_DATA ||
                    result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    Log.w(TAG, "中文不支持，使用英文");
                    tts.setLanguage(Locale.US);
                }
                tts.setSpeechRate(1.0f);
                ttsInitialized = true;
                Log.d(TAG, "TTS 初始化成功");
            } else {
                Log.e(TAG, "TTS 初始化失败");
            }
        });

        tts.setOnUtteranceProgressListener(new android.speech.tts.UtteranceProgressListener() {
            @Override
            public void onStart(String utteranceId) {
                Log.d(TAG, "TTS 开始朗读");
            }

            @Override
            public void onDone(String utteranceId) {
                Log.d(TAG, "TTS 朗读完成");
            }

            @Override
            public void onError(String utteranceId) {
                Log.e(TAG, "TTS 朗读错误");
            }
        });
    }

    /**
     * TTS JavaScript 接口
     */
    public class WebAppInterface {
        @JavascriptInterface
        public void speak(String text) {
            Log.d(TAG, "TTS speak: " + (text != null ? text.substring(0, Math.min(30, text.length())) : "null"));
            if (ttsInitialized && text != null && !text.isEmpty() && tts != null) {
                // QUEUE_FLUSH 会打断当前语音立即播放新的
                tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "novalpie_tts");
            }
        }
        
        @JavascriptInterface
        public void stop() {
            Log.d(TAG, "TTS stop");
            if (tts != null) {
                tts.stop();
            }
        }

        @JavascriptInterface
        public void setRate(float rate) {
            if (tts != null) {
                tts.setSpeechRate(rate);
            }
        }
    }

    /**
     * 音量键按下拦截 - 触发翻页
     */
    @Override
    public boolean onKeyDown(int keyCode, android.view.KeyEvent event) {
        if (keyCode == android.view.KeyEvent.KEYCODE_VOLUME_DOWN) {
            scrollPage("down");
            return true;
        } else if (keyCode == android.view.KeyEvent.KEYCODE_VOLUME_UP) {
            scrollPage("up");
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    /**
     * 音量键抬起拦截 - 彻底防止系统音量框弹出
     */
    @Override
    public boolean onKeyUp(int keyCode, android.view.KeyEvent event) {
        // 拦截抬起事件，彻底防止系统音量框弹出
        if (keyCode == android.view.KeyEvent.KEYCODE_VOLUME_DOWN || keyCode == android.view.KeyEvent.KEYCODE_VOLUME_UP) {
            return true; 
        }
        return super.onKeyUp(keyCode, event);
    }

    /**
     * 页面滚动 - 精准定位 NovalPie 阅读器滚动容器
     */
    private void scrollPage(String direction) {
        if (webView == null) return;
        String script = "(function() {" +
            "var offset = window.innerHeight - 50;" +
            "if ('up' === '" + direction + "') offset = -offset;" +
            // 核心修复：定向寻找 Novalpie 阅读器的真实滚动容器
            "var reader = document.querySelector('.reader-main-scroll');" +
            "if (reader) {" +
            "    reader.scrollBy({top: offset, behavior: 'smooth'});" +
            "} else {" +
            "    window.scrollBy({top: offset, behavior: 'smooth'});" +
            "}" +
            "})();";
            
        executeScript(script); // 调用现有的 executeScript 方法
    }

    /**
     * 执行 JavaScript - 强制在主线程执行，防止静默失败
     */
    private void executeScript(String script) {
        // 强制将 JS 注入推入主线程，防止静默失败
        runOnUiThread(() -> {
            if (webView != null) {
                try {
                    webView.evaluateJavascript(script, null);
                } catch (Exception e) {
                    Log.e(TAG, "JS 执行失败: " + e.getMessage());
                }
            }
        });
    }

    /**
     * dp 转 px
     */
    private int dpToPx(int dp) {
        float density = getResources().getDisplayMetrics().density;
        return Math.round(dp * density);
    }

    @Override
    public void onResume() {
        super.onResume();
        // 延迟初始化，确保 Capacitor 完全准备好
        if (webView == null) {
            getWindow().getDecorView().postDelayed(this::initializeNativeUI, 500);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (tts != null) {
            tts.stop();
            tts.shutdown();
        }
        // 移除 FAB
        if (fabRefresh != null && webViewParent != null) {
            webViewParent.removeView(fabRefresh);
        }
    }
}
