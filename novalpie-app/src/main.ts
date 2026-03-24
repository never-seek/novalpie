/**
 * 最终版本：合并所有修复
 * - 刷新按钮自动隐藏：手势检测（下滑隐藏，上滑/双击显示）
 * - TTS：调用原生 AndroidTTS 接口
 * - 滚动监听：正确接收网页滚动事件
 */

import { Capacitor } from '@capacitor/core';
import { Haptics, ImpactStyle } from '@capacitor/haptics';

document.addEventListener('deviceready', onDeviceReady, false);

// DOM 元素
let iframe: HTMLIFrameElement;
let loadingOverlay: HTMLElement;
let floatingRefresh: HTMLElement;
let gestureHint: HTMLElement;
let errorToast: HTMLElement;
let ttsIndicator: HTMLElement;

// 状态
let refreshVisible = true;
let hideTimeout: ReturnType<typeof setTimeout> | null = null;

// 手势检测状态
let lastTapTime = 0;
let touchStartY = 0;
let touchStartX = 0;
let touchStartTime = 0;

const SWIPE_THRESHOLD = 50;
const TAP_MAX_TIME = 200;
const TAP_MAX_MOVEMENT = 20;

async function onDeviceReady() {
  console.log('[最终版] Capacitor ready');
  
  initDOMElements();
  setupEventListeners();
  
  // 3秒后隐藏加载状态
  setTimeout(() => {
    loadingOverlay.classList.add('hidden');
  }, 3000);
}

function initDOMElements() {
  iframe = document.getElementById('webview-frame') as HTMLIFrameElement;
  loadingOverlay = document.getElementById('loading-overlay')!;
  floatingRefresh = document.getElementById('floating-refresh')!;
  gestureHint = document.getElementById('double-tap-hint')!;
  errorToast = document.getElementById('error-toast')!;
  ttsIndicator = document.getElementById('tts-indicator')!;
  
  console.log('[最终版] DOM 元素初始化完成');
}

function setupEventListeners() {
  // 刷新按钮点击
  floatingRefresh.addEventListener('click', () => {
    console.log('[最终版] 刷新按钮点击');
    refreshPage();
  });
  
  // WebView 加载状态
  iframe.addEventListener('load', onIframeLoad);
  iframe.addEventListener('error', onIframeError);
  
  // 触摸事件 - 手势检测
  setupGestureDetection();
  
  // 监听网页消息
  setupWebviewMessageListener();
  
  console.log('[最终版] 事件监听器设置完成');
}

/**
 * 手势检测：
 * - 下滑 = 隐藏刷新按钮
 * - 上滑 = 显示刷新按钮
 * - 双击 = 切换显示/隐藏
 */
function setupGestureDetection() {
  document.addEventListener('touchstart', (e) => {
    touchStartY = e.touches[0].clientY;
    touchStartX = e.touches[0].clientX;
    touchStartTime = Date.now();
  }, { passive: true });
  
  document.addEventListener('touchend', (e) => {
    const touchEndY = e.changedTouches[0].clientY;
    const touchEndX = e.changedTouches[0].clientX;
    const touchEndTime = Date.now();
    
    const deltaY = touchEndY - touchStartY;
    const deltaX = touchEndX - touchStartX;
    const duration = touchEndTime - touchStartTime;
    
    // 点击手势（快速且移动小）
    if (duration < TAP_MAX_TIME && Math.abs(deltaX) < TAP_MAX_MOVEMENT && Math.abs(deltaY) < TAP_MAX_MOVEMENT) {
      handleTap(touchEndX, touchEndY);
    }
    // 滑动手势（纵向滑动超过阈值）
    else if (Math.abs(deltaY) > Math.abs(deltaX) && Math.abs(deltaY) > SWIPE_THRESHOLD) {
      handleSwipe(deltaY);
    }
  }, { passive: true });
}

/**
 * 处理点击手势
 */
function handleTap(_x: number, y: number) {
  const now = Date.now();
  const screenHeight = window.innerHeight;
  
  // 检测双击（300ms 内两次点击）
  if (now - lastTapTime < 300) {
    console.log('[最终版] 双击 - 切换刷新按钮');
    toggleRefreshButton();
    lastTapTime = 0;
    return;
  }
  
  lastTapTime = now;
  
  // 顶部或底部区域点击 - 显示刷新按钮
  if (y < 100 || y > screenHeight - 150) {
    showRefreshButton();
  }
}

/**
 * 处理滑动手势
 */
function handleSwipe(deltaY: number) {
  if (deltaY > 0) {
    // 下滑 - 隐藏
    console.log('[最终版] 下滑 - 隐藏刷新按钮');
    hideRefreshButton();
  } else {
    // 上滑 - 显示
    console.log('[最终版] 上滑 - 显示刷新按钮');
    showRefreshButton();
    showGestureHint();
  }
}

/**
 * 切换刷新按钮显示/隐藏
 */
function toggleRefreshButton() {
  if (refreshVisible) {
    hideRefreshButton();
  } else {
    showRefreshButton();
  }
  triggerHaptic();
}

/**
 * 隐藏刷新按钮
 */
function hideRefreshButton() {
  if (!refreshVisible) return;
  floatingRefresh.classList.add('hidden');
  refreshVisible = false;
  console.log('[最终版] 刷新按钮已隐藏');
}

/**
 * 显示刷新按钮
 */
function showRefreshButton() {
  if (refreshVisible) return;
  floatingRefresh.classList.remove('hidden');
  refreshVisible = true;
  console.log('[最终版] 刷新按钮已显示');
}

/**
 * 显示手势提示
 */
function showGestureHint() {
  gestureHint.textContent = '👆 上滑显示刷新按钮';
  gestureHint.classList.add('show');
  if (hideTimeout) clearTimeout(hideTimeout);
  hideTimeout = setTimeout(() => {
    gestureHint.classList.remove('show');
  }, 1500);
}

/**
 * 刷新页面
 */
function refreshPage() {
  floatingRefresh.classList.add('refreshing');
  iframe.src = iframe.src;
  setTimeout(() => {
    floatingRefresh.classList.remove('refreshing');
  }, 1000);
  triggerHaptic();
}

/**
 * 监听网页消息
 */
function setupWebviewMessageListener() {
  window.addEventListener('message', (event) => {
    const data = event.data;
    if (!data || typeof data !== 'object') return;
    
    switch (data.type) {
      case 'TTS_STATUS':
        console.log('[最终版] TTS 状态:', data.playing);
        updateTTSIndicator(data.playing);
        break;
        
      case 'PAGE_SCROLLED':
        // 根据滚动方向智能显示/隐藏
        handleScrollFromWebview(data);
        break;
        
      case 'TTS_SPEAK':
        if (data.text) {
          console.log('[最终版] 请求 TTS 朗读:', data.text.substring(0, 30) + '...');
          speakText(data.text);
        }
        break;
        
      case 'TTS_STOP':
        console.log('[最终版] 停止 TTS');
        stopTTS();
        break;
    }
  });
}

/**
 * 处理网页滚动事件
 */
function handleScrollFromWebview(data: any) {
  const { direction, scrollTop } = data;
  
  // 快速向下滑动且超过200px - 隐藏
  if (direction === 'down' && scrollTop > 200) {
    hideRefreshButton();
  }
  // 向上滑动 - 显示
  else if (direction === 'up') {
    showRefreshButton();
  }
}

/**
 * 更新 TTS 指示器
 */
function updateTTSIndicator(playing: boolean) {
  if (playing) {
    ttsIndicator.classList.add('show');
  } else {
    ttsIndicator.classList.remove('show');
  }
}

/**
 * 调用原生 TTS 朗读
 */
function speakText(text: string) {
  try {
    const androidTTS = (window as any).AndroidTTS;
    if (androidTTS && typeof androidTTS.speak === 'function') {
      androidTTS.speak(text);
      updateTTSIndicator(true);
      console.log('[最终版] TTS 开始朗读');
    } else {
      console.warn('[最终版] AndroidTTS 接口不可用');
      showError('语音功能不可用');
    }
  } catch (e) {
    console.error('[最终版] TTS 错误:', e);
    showError('语音朗读失败');
  }
}

/**
 * 停止 TTS
 */
function stopTTS() {
  try {
    const androidTTS = (window as any).AndroidTTS;
    if (androidTTS && typeof androidTTS.stop === 'function') {
      androidTTS.stop();
    }
  } catch (e) {
    console.error('[最终版] TTS 停止错误:', e);
  }
  updateTTSIndicator(false);
}

/**
 * Iframe 加载完成
 */
function onIframeLoad() {
  console.log('[最终版] Iframe 加载完成');
  loadingOverlay.classList.add('hidden');
}

/**
 * Iframe 加载错误
 */
function onIframeError() {
  console.error('[最终版] Iframe 加载错误');
  showError('页面加载失败，请检查网络');
  loadingOverlay.classList.add('hidden');
}

/**
 * 触发触觉反馈
 */
async function triggerHaptic() {
  try {
    if (Capacitor.isNativePlatform()) {
      await Haptics.impact({ style: ImpactStyle.Light });
    }
  } catch (error) {
    console.log('[最终版] Haptics 不可用');
  }
}

/**
 * 显示错误提示
 */
function showError(message: string) {
  errorToast.textContent = message;
  errorToast.classList.add('show');
  setTimeout(() => {
    errorToast.classList.remove('show');
  }, 2000);
}

// 暴露到全局，供 Android 调用
(window as any).refreshPage = refreshPage;
(window as any).showRefreshButton = showRefreshButton;
(window as any).hideRefreshButton = hideRefreshButton;
