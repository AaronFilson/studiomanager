/** @type {import('jest').Config} */
module.exports = {
  preset: 'jest-expo',
  // pnpm keeps packages under node_modules/.pnpm, so match React Native packages anywhere in the path.
  // The API client is TypeScript source, so it is transformed too.
  transformIgnorePatterns: [
    'node_modules/(?!(\\.pnpm/[^/]+/node_modules/)?((jest-)?react-native|@react-native(-community)?|expo(nent)?|@expo(nent)?/.*|expo-.*|@expo/.*|@studiomanager/.*))',
  ],
};
