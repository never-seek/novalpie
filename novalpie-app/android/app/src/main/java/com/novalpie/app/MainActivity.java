package com.novalpie.app;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.webkit.JavascriptInterface;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;

import com.getcapacitor.BridgeActivity;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Locale;

/**
 * NovalPie 主Activity
 * - 音量键拦截翻页
 * - 动态生成悬浮刷新按钮（Capacitor 不读 XML 布局）
 * - TTS 接口
 */
public class MainActivity extends BridgeActivity {

    private static final String TAG = "NovalPie-Main";

    private WebView webView;
    private FloatingActionButton fabRefresh;
    private TextToSpeech tts;
    private boolean ttsInitialized = false;
    private Handler mainHandler = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // 彻底移除标题栏 - 必须在 super.onCreate 前调用
        supportRequestWindowFeature(Window.FEATURE_NO_TITLE);
        setTheme(R.style.AppTheme_NoActionBar);
        
        super.onCreate(savedInstanceState);

        // --- 开启沉浸式全面屏与透明系统栏 ---
        Window window = getWindow();
        window.getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
              | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
              | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
        );
        window.setStatusBarColor(Color.TRANSPARENT);
        window.setNavigationBarColor(Color.TRANSPARENT);
        // ------------------------------------------------

        // 双重保险：再次隐藏 ActionBar
        try {
            if (getSupportActionBar() != null) {
                getSupportActionBar().hide();
            }
        } catch (Exception e) {
            Log.w(TAG, "hide action bar failed", e);
        }

        Log.d(TAG, "onCreate: 开始初始化");
        getWindow().getDecorView().postDelayed(this::initializeNativeUI, 500);
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

        // 初始化 TTS
        initTTS();

        // 配置 WebView
        configureWebView();

        // 核心修复：用 Java 代码动态生成 FAB，彻底无视无效的 XML 布局
        createFloatingActionButton();

        // 设置手势检测
        setupGestureDetector();

        // 加载网页
        webView.loadUrl("https://novalpie.cc");
    }

    /**
     * 核心：用 Java 代码动态生成悬浮刷新按钮
     * 原因：Capacitor 根本不读取 activity_main.xml，必须用代码创建
     */
    private void createFloatingActionButton() {
        try {
            Log.d(TAG, "动态创建 FAB 刷新按钮...");
            
            fabRefresh = new FloatingActionButton(this);
            
            // 设置刷新图标（使用 ic_refresh.xml）
            fabRefresh.setImageResource(R.drawable.ic_refresh);
            
            // 设为迷你尺寸
            fabRefresh.setSize(FloatingActionButton.SIZE_MINI);
            
            // 设置背景色：15%透明度的蓝灰色 (#2678909C)
            // ARGB: 38=15%透明度, 120=R, 144=G, 156=B
            fabRefresh.setBackgroundTintList(ColorStateList.valueOf(Color.argb(38, 120, 144, 156)));
            
            // 设置图标颜色：60%透明度的蓝灰色 (#9978909C)
            // ARGB: 153=60%透明度
            fabRefresh.setImageTintList(ColorStateList.valueOf(Color.argb(153, 120, 144, 156)));
            
            // 拍扁去阴影
            fabRefresh.setCompatElevation(0f);

            // 设置按钮位置：右下角
            FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.WRAP_CONTENT,
                    FrameLayout.LayoutParams.WRAP_CONTENT
            );
            params.gravity = Gravity.BOTTOM | Gravity.END;
            // 距离右边 60px，底部 120px（可根据需要微调）
            params.setMargins(0, 0, 60, 120);
            fabRefresh.setLayoutParams(params);

            // 绑定点击事件：刷新 WebView
            fabRefresh.setOnClickListener(v -> {
                Log.d(TAG, "FAB 点击: 刷新页面");
                if (webView != null) {
                    webView.reload();
                }
            });

            // 强行添加到 WebView 的父容器中，保证绝对可见！
            ViewGroup parent = (ViewGroup) webView.getParent();
            if (parent instanceof FrameLayout) {
                ((FrameLayout) parent).addView(fabRefresh);
                Log.d(TAG, "FAB 已添加到视图层级");
            } else {
                // 如果父容器不是 FrameLayout，尝试强制添加
                parent.addView(fabRefresh);
                Log.d(TAG, "FAB 已添加到父容器: " + parent.getClass().getSimpleName());
            }

        } catch (Exception e) {
            Log.e(TAG, "创建 FAB 失败: " + e.getMessage());
            e.printStackTrace();
        }
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
                
                // 注入 TTS Polyfill
                String ttsPolyfill = "(function() {" +
                    "window.currentUtterance = null;" +
                    "window.onTtsStart = function() { " +
                    "    if(window.currentUtterance && window.currentUtterance.onstart) " +
                    "        window.currentUtterance.onstart({type: 'start', utterance: window.currentUtterance}); " +
                    "};" +
                    "window.onTtsEnd = function() { " +
                    "    if(window.currentUtterance && window.currentUtterance.onend) " +
                    "        window.currentUtterance.onend({type: 'end', utterance: window.currentUtterance}); " +
                    "};" +
                    "window.speechSynthesis = {" +
                    "    speak: function(u) {" +
                    "        window.currentUtterance = u;" +
                    "        if(window.AndroidTTS) window.AndroidTTS.speak(u.text);" +
                    "    }," +
                    "    cancel: function() { if(window.AndroidTTS) window.AndroidTTS.stop(); }," +
                    "    pause: function() { if(window.AndroidTTS) window.AndroidTTS.stop(); }," +
                    "    resume: function() {}," +
                    "    getVoices: function() { return [{name: 'Native Android', lang: 'zh-CN', default: true}]; }" +
                    "};" +
                    "window.SpeechSynthesisUtterance = function(text) { this.text = text; };" +
                "})();";
                
                executeScript(ttsPolyfill);
                
                // 极致安全的被动式自动提交脚本
                // 核心原则：只监听状态，绝不派发 input 事件，绝不干涉输入法缓冲区
                String safeAutoSubmit = "(function() {" +
                    "console.log('[NovalPie] 注入被动式自动提交...');" +
                    "" +
                    "let typingTimer = null;" +
                    "let isComposing = false;" +
                    "" +
                    // 仅仅记录状态，绝不派发任何事件打断输入法
                    "document.addEventListener('compositionstart', function() {" +
                    "    isComposing = true;" +
                    "    console.log('[NovalPie] 拼写开始');" +
                    "}, true);" +
                    "" +
                    "document.addEventListener('compositionend', function() {" +
                    "    isComposing = false;" +
                    "    console.log('[NovalPie] 拼写结束');" +
                    "}, true);" +
                    "" +
                    "document.addEventListener('input', function(e) {" +
                    "    if (e.target.tagName !== 'INPUT' && e.target.tagName !== 'TEXTAREA') return;" +
                    "    clearTimeout(typingTimer);" +
                    "" +
                    "    // 只有在不在打拼音的情况下，开启 1 秒倒计时" +
                    "    if (!isComposing) {" +
                    "        typingTimer = setTimeout(function() {" +
                    "            // 1秒钟没敲键盘，帮用户偷偷按一下回车" +
                    "            // 注意：只发 KeyboardEvent，绝对不发 input 事件！" +
                    "            console.log('[NovalPie] 1秒停顿，静默触发回车');" +
                    "            e.target.dispatchEvent(new KeyboardEvent('keydown', {" +
                    "                key: 'Enter'," +
                    "                code: 'Enter'," +
                    "                keyCode: 13," +
                    "                which: 13," +
                    "                bubbles: true," +
                    "                cancelable: true" +
                    "            }));" +
                    "        }, 1000);" + // 1000毫秒（1秒）缓冲，保证不吞字
                    "    }" +
                    "}, true);" +
                    "" +
                    "console.log('[NovalPie] 被动式自动提交已加载');" +
                "})();";
                
                executeScript(safeAutoSubmit);
            }
        });

        Log.d(TAG, "WebView 配置完成");
    }

    /**
     * 设置手势检测器（上下滑动隐藏/显示 FAB）
     */
    private void setupGestureDetector() {
        if (webView == null || fabRefresh == null) return;

        android.view.GestureDetector gestureDetector = new android.view.GestureDetector(this,
                new android.view.GestureDetector.SimpleOnGestureListener() {

            private static final int SWIPE_THRESHOLD = 20;

            @Override
            public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
                if (distanceY > SWIPE_THRESHOLD && fabRefresh.isShown()) {
                    fabRefresh.hide();
                } else if (distanceY < -SWIPE_THRESHOLD && !fabRefresh.isShown()) {
                    fabRefresh.show();
                }
                return false;
            }

            @Override
            public boolean onDoubleTap(MotionEvent e) {
                if (!fabRefresh.isShown()) {
                    fabRefresh.show();
                }
                return false;
            }
        });

        webView.setOnTouchListener((v, event) -> {
            gestureDetector.onTouchEvent(event);
            return false;
        });

        Log.d(TAG, "手势检测器设置完成");
    }

    /**
     * 初始化 TTS
     */
    private void initTTS() {
        Log.d(TAG, "开始连接系统 TTS 引擎...");
        
        tts = new TextToSpeech(getApplicationContext(), status -> {
            if (status == TextToSpeech.SUCCESS) {
                String engineName = tts.getDefaultEngine();
                Log.d(TAG, "引擎实例连接成功: " + engineName);
                
                int langResult = tts.setLanguage(Locale.CHINA);
                
                if (langResult >= TextToSpeech.LANG_AVAILABLE || langResult == TextToSpeech.LANG_NOT_SUPPORTED) {
                    ttsInitialized = true;
                    tts.setSpeechRate(1.0f);
                    tts.setPitch(1.0f);
                }

                tts.setOnUtteranceProgressListener(new android.speech.tts.UtteranceProgressListener() {
                    @Override 
                    public void onStart(String utteranceId) {
                        mainHandler.post(() -> executeScript("if(window.onTtsStart) window.onTtsStart();"));
                    }
                    
                    @Override 
                    public void onDone(String utteranceId) {
                        mainHandler.post(() -> executeScript("if(window.onTtsEnd) window.onTtsEnd();"));
                    }
                    
                    @Override 
                    public void onError(String utteranceId) {
                        mainHandler.post(() -> executeScript("if(window.onTtsEnd) window.onTtsEnd();"));
                    }
                });
            }
        }, null);
    }

    /**
     * TTS JavaScript 接口
     */
    public class WebAppInterface {
        @JavascriptInterface
        public void speak(String text) {
            if (text == null || text.isEmpty() || text.trim().length() <= 1) {
                mainHandler.post(() -> executeScript("if(window.onTtsEnd) window.onTtsEnd();"));
                return;
            }
            
            if (tts != null) {
                try {
                    AudioManager am = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
                    am.requestAudioFocus(null, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK);
                } catch (Exception e) {
                    Log.e(TAG, "音频焦点请求异常: " + e.getMessage());
                }
                
                Bundle params = new Bundle();
                params.putInt(TextToSpeech.Engine.KEY_PARAM_STREAM, AudioManager.STREAM_MUSIC);
                
                tts.speak(text, TextToSpeech.QUEUE_FLUSH, params, "novalpie_tts");
            }
        }

        @JavascriptInterface
        public void stop() {
            if (tts != null) {
                tts.stop();
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
     * 音量键抬起拦截
     */
    @Override
    public boolean onKeyUp(int keyCode, android.view.KeyEvent event) {
        if (keyCode == android.view.KeyEvent.KEYCODE_VOLUME_DOWN || keyCode == android.view.KeyEvent.KEYCODE_VOLUME_UP) {
            return true; 
        }
        return super.onKeyUp(keyCode, event);
    }

    /**
     * 页面滚动
     */
    private void scrollPage(String direction) {
        if (webView == null) return;
        String script = "(function() {" +
            "var offset = window.innerHeight - 50;" +
            "if ('up' === '" + direction + "') offset = -offset;" +
            "var reader = document.querySelector('.reader-main-scroll');" +
            "if (reader) {" +
            "    reader.scrollBy({top: offset, behavior: 'smooth'});" +
            "} else {" +
            "    window.scrollBy({top: offset, behavior: 'smooth'});" +
            "}" +
            "})();";
        executeScript(script);
    }

    /**
     * 执行 JavaScript
     */
    private void executeScript(String script) {
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

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (tts != null) {
            tts.stop();
            tts.shutdown();
        }
    }
}
