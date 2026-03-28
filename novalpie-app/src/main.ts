import { Capacitor } from '@capacitor/core';
import { Haptics, ImpactStyle } from '@capacitor/haptics';

document.addEventListener('deviceready', onDeviceReady, false);

let iframe: HTMLIFrameElement;
let loadingOverlay: HTMLElement;
let floatingRefresh: HTMLElement;
let gestureHint: HTMLElement;
let errorToast: HTMLElement;
let ttsIndicator: HTMLElement;

let refreshVisible = true;
let hideTimeout: ReturnType<typeof setTimeout> | null = null;

let lastTapTime = 0;
let touchStartY = 0;
let touchStartX = 0;
let touchStartTime = 0;

const SWIPE_THRESHOLD = 50;
const TAP_MAX_TIME = 200;
const TAP_MAX_MOVEMENT = 20;

async function onDeviceReady() {
  console.log('[回退版] Capacitor ready');
  initDOMElements();
  setupEventListeners();

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
}

function setupEventListeners() {
  floatingRefresh.addEventListener('click', () => {
    refreshPage();
  });

  iframe.addEventListener('load', onIframeLoad);
  iframe.addEventListener('error', onIframeError);

  setupGestureDetection();
  setupWebviewMessageListener();
}

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

    if (duration < TAP_MAX_TIME && Math.abs(deltaX) < TAP_MAX_MOVEMENT && Math.abs(deltaY) < TAP_MAX_MOVEMENT) {
      handleTap(touchEndX, touchEndY);
    } else if (Math.abs(deltaY) > Math.abs(deltaX) && Math.abs(deltaY) > SWIPE_THRESHOLD) {
      handleSwipe(deltaY);
    }
  }, { passive: true });
}

function handleTap(_x: number, y: number) {
  const now = Date.now();
  const screenHeight = window.innerHeight;

  if (now - lastTapTime < 300) {
    toggleRefreshButton();
    lastTapTime = 0;
    return;
  }

  lastTapTime = now;

  if (y < 100 || y > screenHeight - 150) {
    showRefreshButton();
  }
}

function handleSwipe(deltaY: number) {
  if (deltaY > 0) {
    hideRefreshButton();
  } else {
    showRefreshButton();
    showGestureHint();
  }
}

function toggleRefreshButton() {
  if (refreshVisible) {
    hideRefreshButton();
  } else {
    showRefreshButton();
  }
  triggerHaptic();
}

function hideRefreshButton() {
  if (!refreshVisible) return;
  floatingRefresh.classList.add('hidden');
  refreshVisible = false;
}

function showRefreshButton() {
  if (refreshVisible) return;
  floatingRefresh.classList.remove('hidden');
  refreshVisible = true;
}

function showGestureHint() {
  gestureHint.textContent = '👆 上滑显示刷新按钮';
  gestureHint.classList.add('show');
  if (hideTimeout) clearTimeout(hideTimeout);
  hideTimeout = setTimeout(() => {
    gestureHint.classList.remove('show');
  }, 1500);
}

function refreshPage() {
  floatingRefresh.classList.add('refreshing');
  iframe.src = iframe.src;
  setTimeout(() => {
    floatingRefresh.classList.remove('refreshing');
  }, 1000);
  triggerHaptic();
}

function setupWebviewMessageListener() {
  window.addEventListener('message', (event) => {
    const data = event.data;
    if (!data || typeof data !== 'object') return;

    switch (data.type) {
      case 'TTS_STATUS':
        updateTTSIndicator(data.playing);
        break;
      case 'PAGE_SCROLLED':
        handleScrollFromWebview(data);
        break;
      case 'TTS_SPEAK':
        if (data.text) speakText(data.text);
        break;
      case 'TTS_STOP':
        stopTTS();
        break;
    }
  });
}

function handleScrollFromWebview(data: any) {
  const { direction, scrollTop } = data;

  if (direction === 'down' && scrollTop > 200) {
    hideRefreshButton();
  } else if (direction === 'up') {
    showRefreshButton();
  }
}

function updateTTSIndicator(playing: boolean) {
  if (playing) {
    ttsIndicator.classList.add('show');
  } else {
    ttsIndicator.classList.remove('show');
  }
}

function speakText(text: string) {
  try {
    const androidTTS = (window as any).AndroidTTS;
    if (androidTTS && typeof androidTTS.speak === 'function') {
      androidTTS.speak(text);
      updateTTSIndicator(true);
    } else {
      showError('语音功能不可用');
    }
  } catch {
    showError('语音朗读失败');
  }
}

function stopTTS() {
  try {
    const androidTTS = (window as any).AndroidTTS;
    if (androidTTS && typeof androidTTS.stop === 'function') {
      androidTTS.stop();
    }
  } catch {}
  updateTTSIndicator(false);
}

function onIframeLoad() {
  loadingOverlay.classList.add('hidden');
}

function onIframeError() {
  showError('页面加载失败，请检查网络');
  loadingOverlay.classList.add('hidden');
}

async function triggerHaptic() {
  try {
    if (Capacitor.isNativePlatform()) {
      await Haptics.impact({ style: ImpactStyle.Light });
    }
  } catch {}
}

function showError(message: string) {
  errorToast.textContent = message;
  errorToast.classList.add('show');
  setTimeout(() => {
    errorToast.classList.remove('show');
  }, 2000);
}

(window as any).refreshPage = refreshPage;
(window as any).showRefreshButton = showRefreshButton;
(window as any).hideRefreshButton = hideRefreshButton;
