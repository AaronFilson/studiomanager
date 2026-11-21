import type { ExpoConfig } from 'expo/config';

// The scheme is also the sign-in redirect (studiomanager://auth), registered in infra/identity.
const config: ExpoConfig = {
  name: 'Studio Manager',
  slug: 'studiomanager',
  scheme: 'studiomanager',
  version: '0.1.0',
  orientation: 'portrait',
  icon: './assets/icon.png',
  userInterfaceStyle: 'automatic',
  ios: { bundleIdentifier: 'io.github.aaronfilson.studiomanager' },
  android: {
    package: 'io.github.aaronfilson.studiomanager',
    adaptiveIcon: {
      foregroundImage: './assets/adaptive-icon.png',
      backgroundColor: '#f7f4ef',
    },
  },
  plugins: [
    'expo-router',
    'expo-secure-store',
    [
      'expo-image-picker',
      {
        cameraPermission: 'Studio Manager uses the camera to photograph pieces, so they can be found on the shelf.',
        photosPermission: 'Studio Manager adds the photos you choose to your pieces.',
        microphonePermission: false,
      },
    ],
  ],
};

export default config;
