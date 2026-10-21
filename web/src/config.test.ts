import { describe, expect, it } from 'vitest';

import { readConfig } from './config';

const origin = 'http://localhost:5173';

describe('readConfig', () => {
  it('reads Entra External ID settings', () => {
    const config = readConfig(
      {
        VITE_ENTRA_CLIENT_ID: ' spa-client-id ',
        VITE_ENTRA_AUTHORITY: 'https://studiomanagerdev.ciamlogin.com/',
        VITE_API_SCOPES: 'api://api-client-id/access_as_user  openid',
        VITE_API_BASE_URL: 'https://api.example.com/',
      },
      origin,
    );

    expect(config.problems).toEqual([]);
    expect(config.apiBaseUrl).toBe('https://api.example.com');
    expect(config.auth).toEqual({
      clientId: 'spa-client-id',
      authority: 'https://studiomanagerdev.ciamlogin.com/',
      knownAuthority: 'studiomanagerdev.ciamlogin.com',
      scopes: ['api://api-client-id/access_as_user', 'openid'],
    });
  });

  it('calls the API on the page origin when no API origin is set', () => {
    expect(readConfig({ VITE_API_BASE_URL: '' }, origin).apiBaseUrl).toBe(origin);
  });

  it('lists every missing setting instead of failing on the first', () => {
    const config = readConfig({}, origin);

    expect(config.auth).toBeNull();
    expect(config.problems).toEqual([
      'VITE_ENTRA_CLIENT_ID is not set.',
      'VITE_ENTRA_AUTHORITY is not set.',
      'VITE_API_SCOPES is not set.',
    ]);
  });

  it('rejects an authority that is not a URL', () => {
    const config = readConfig(
      { VITE_ENTRA_CLIENT_ID: 'id', VITE_ENTRA_AUTHORITY: 'studiomanagerdev', VITE_API_SCOPES: 'scope' },
      origin,
    );

    expect(config.auth).toBeNull();
    expect(config.problems).toEqual(['VITE_ENTRA_AUTHORITY is not a URL: "studiomanagerdev".']);
  });
});
