import type { ExpoConfig } from 'expo/config';

// The scheme is also the sign-in redirect (studiomanager://auth), registered in infra/identity.
const config: ExpoConfig = {
  name: 'Studio Manager',
  slug: 'studiomanager',
  scheme: 'studiomanager',
  version: '0.1.0',
  orientation: 'portrait',
  userInterfaceStyle: 'automatic',
  ios: { bundleIdentifier: 'io.github.aaronfilson.studiomanager' },
  android: { package: 'io.github.aaronfilson.studiomanager' },
  plugins: ['expo-router', 'expo-secure-store'],
};

export default config;
