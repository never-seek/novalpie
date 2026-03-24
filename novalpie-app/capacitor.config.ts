import type { CapacitorConfig } from '@capacitor/cli';

const config: CapacitorConfig = {
  appId: 'com.novalpie.app',
  appName: 'NovalPie',
  webDir: 'dist',
  server: {
    androidScheme: 'https',
    cleartext: false,
    allowedHosts: ['novalpie.cc', 'images.novelpia.com', '*.novelpia.com', '*.novalpie.cc']
  },
  plugins: {
    SplashScreen: {
      launchShowDuration: 2000,
      backgroundColor: "#ea4c89",
      showSpinner: false,
      androidScaleType: "CENTER_CROP",
      splashFullScreen: false,
      splashImmersive: false
    }
  },
  android: {
    backgroundColor: '#ffffff',
    allowMixedContent: true,
    captureInput: true,
    webContentsDebuggingEnabled: true  // 允许调试以便排查问题
  }
};

export default config;
