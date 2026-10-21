import { readConfig } from './config';

describe('readConfig', () => {
  it('reads complete settings', () => {
    expect(
      readConfig({
        EXPO_PUBLIC_ENTRA_CLIENT_ID: ' client ',
        EXPO_PUBLIC_ENTRA_ISSUER: 'https://studio.ciamlogin.com/tenant/v2.0/',
        EXPO_PUBLIC_API_SCOPES: 'api://x/access_as_user',
        EXPO_PUBLIC_API_BASE_URL: 'http://10.0.2.2:8080/',
      }),
    ).toEqual({
      apiBaseUrl: 'http://10.0.2.2:8080',
      auth: { clientId: 'client', issuer: 'https://studio.ciamlogin.com/tenant/v2.0', scopes: ['api://x/access_as_user'] },
      problems: [],
    });
  });

  it('says what is missing instead of guessing', () => {
    const config = readConfig({ EXPO_PUBLIC_ENTRA_ISSUER: 'studio.ciamlogin.com' });

    expect(config.auth).toBeNull();
    expect(config.problems).toEqual([
      'EXPO_PUBLIC_ENTRA_CLIENT_ID is not set.',
      'EXPO_PUBLIC_ENTRA_ISSUER is not an https issuer URL: "studio.ciamlogin.com".',
      'EXPO_PUBLIC_API_SCOPES is not set.',
      'EXPO_PUBLIC_API_BASE_URL is not set.',
    ]);
  });
});
