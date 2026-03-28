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
      backgroundColor: '#000000',
      showSpinner: false,
      androidScaleType: 'CENTER_CROP',
      splashFullScreen: false,
      splashImmersive: false
    },
    StatusBar: {
      overlaysWebView: false
    }
  },
  android: {
    backgroundColor: '#000000',
    allowMixedContent: true,
    captureInput: false,
    webContentsDebuggingEnabled: true
  }
};

export default config;
