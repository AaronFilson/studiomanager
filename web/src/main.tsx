import './styles.css';

import { configureApiClient } from '@studiomanager/api-client';
import { StrictMode } from 'react';
import { createRoot } from 'react-dom/client';

import { App } from './App';
import { AuthNotConfigured } from './auth/AuthNotConfigured';
import { acquireApiToken, createMsalInstance } from './auth/msal';
import { MsalAuthProvider } from './auth/MsalAuthProvider';
import { readConfig } from './config';
import { createQueryClient } from './queryClient';
import { createAppRouter } from './router';

const container = document.getElementById('root');
if (!container) {
  throw new Error('index.html is missing <div id="root">');
}
const root = createRoot(container);
const config = readConfig(import.meta.env, window.location.origin);

if (config.auth) {
  const { scopes } = config.auth;
  const msal = createMsalInstance(config.auth);
  // Configure the shared client before anything renders, so the first query is authenticated.
  configureApiClient({ baseUrl: config.apiBaseUrl, getAccessToken: () => acquireApiToken(msal, scopes) });
  const queryClient = createQueryClient();
  const router = createAppRouter(queryClient);

  root.render(
    <StrictMode>
      <MsalAuthProvider instance={msal} scopes={scopes}>
        <App router={router} queryClient={queryClient} />
      </MsalAuthProvider>
    </StrictMode>,
  );
} else {
  root.render(
    <StrictMode>
      <AuthNotConfigured problems={config.problems} />
    </StrictMode>,
  );
}
