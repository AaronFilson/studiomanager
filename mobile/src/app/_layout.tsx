import { configureApiClient } from '@studiomanager/api-client';
import { QueryClientProvider } from '@tanstack/react-query';
import { Stack } from 'expo-router';
import { StatusBar } from 'expo-status-bar';
import { type ReactNode, useState } from 'react';

import { AuthProvider, useAuth } from '../auth/AuthProvider';
import { buildEnv, readConfig } from '../config';
import { createQueryClient } from '../queryClient';
import { Body, Screen, Title } from '../ui';

const config = readConfig(buildEnv);

export default function RootLayout() {
  const [queryClient] = useState(createQueryClient);

  if (!config.auth) {
    return (
      <Screen>
        <Title>Sign-in isn&apos;t set up</Title>
        {config.problems.map((problem) => (
          <Body key={problem}>{problem}</Body>
        ))}
        <Body muted>See mobile/README.md.</Body>
      </Screen>
    );
  }
  return (
    <QueryClientProvider client={queryClient}>
      <AuthProvider settings={config.auth}>
        <ApiClient baseUrl={config.apiBaseUrl}>
          <StatusBar style="auto" />
          <Stack screenOptions={{ headerShown: false }} />
        </ApiClient>
      </AuthProvider>
    </QueryClientProvider>
  );
}

/** Points the shared API client at the API, with the session's tokens, before any screen queries it. */
function ApiClient({ baseUrl, children }: { baseUrl: string; children: ReactNode }) {
  const { getAccessToken } = useAuth();
  useState(() => {
    configureApiClient({ baseUrl, getAccessToken });
  });
  return children;
}
