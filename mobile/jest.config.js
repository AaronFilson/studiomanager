/** @type {import('jest').Config} */
module.exports = {
  preset: 'jest-expo',
  // pnpm keeps packages under node_modules/.pnpm, so match React Native packages anywhere in the path.
  // The API client is TypeScript source, so it is transformed too.
  transformIgnorePatterns: [
    'node_modules/(?!(\\.pnpm/[^/]+/node_modules/)?((jest-)?react-native|@react-native(-community)?|expo(nent)?|@expo(nent)?/.*|expo-.*|@expo/.*|@studiomanager/.*))',
  ],
  setupFilesAfterEnv: ['<rootDir>/src/test/setup.ts'],
  // A suite's first test also loads React Native, which on a busy CI runner (the web tests run
  // alongside) takes longer than Jest's default 5 s
  testTimeout: 30000,
};
