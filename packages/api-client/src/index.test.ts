import { afterEach, describe, expect, it, vi } from 'vitest';

import { ApiError, client, configureApiClient, getMe, getStudio } from './index';

const studioId = '0b7f3f0e-6f1c-4c7a-9d55-1d2f2b8b4a10';

function stubFetch(response: Response) {
  const fetch = vi.fn<(request: Request) => Promise<Response>>().mockResolvedValue(response);
  client.setConfig({ fetch: fetch as unknown as typeof globalThis.fetch });
  return fetch;
}

function sentRequest(fetch: ReturnType<typeof stubFetch>): Request {
  const request = fetch.mock.calls[0]?.[0];
  if (!request) {
    throw new Error('fetch was not called');
  }
  return request;
}

describe('configureApiClient', () => {
  afterEach(() => {
    client.setConfig({ fetch: undefined });
  });

  it('sends the bearer token to the configured base URL', async () => {
    configureApiClient({ baseUrl: 'https://api.test', getAccessToken: () => Promise.resolve('token-123') });
    const fetch = stubFetch(Response.json({ account: { id: 'a1' }, memberships: [] }));

    const { data } = await getMe({ throwOnError: true });

    const request = sentRequest(fetch);
    expect(request.url).toBe('https://api.test/api/v1/me');
    expect(request.headers.get('Authorization')).toBe('Bearer token-123');
    expect(data.account.id).toBe('a1');
  });

  it('omits the Authorization header when signed out', async () => {
    configureApiClient({ baseUrl: 'https://api.test', getAccessToken: () => Promise.resolve(undefined) });
    const fetch = stubFetch(Response.json({ account: { id: 'a1' }, memberships: [] }));

    await getMe({ throwOnError: true });

    expect(sentRequest(fetch).headers.has('Authorization')).toBe(false);
  });

  it('turns an empty-bodied 403 into an ApiError with the status', async () => {
    configureApiClient({ baseUrl: 'https://api.test', getAccessToken: () => Promise.resolve('t') });
    stubFetch(new Response(null, { status: 403 }));

    const error: unknown = await getStudio({ path: { studioId }, throwOnError: true }).catch((e: unknown) => e);

    expect(error).toBeInstanceOf(ApiError);
    expect((error as ApiError).status).toBe(403);
  });

  it('keeps problem details from the response body', async () => {
    configureApiClient({ baseUrl: 'https://api.test', getAccessToken: () => Promise.resolve('t') });
    stubFetch(
      Response.json(
        { title: 'Bad Request', status: 400, detail: 'timeZone is not a valid IANA zone' },
        { status: 400, headers: { 'Content-Type': 'application/problem+json' } },
      ),
    );

    const error: unknown = await getStudio({ path: { studioId }, throwOnError: true }).catch((e: unknown) => e);

    expect(error).toMatchObject({ status: 400, title: 'Bad Request', detail: 'timeZone is not a valid IANA zone' });
    expect((error as ApiError).message).toBe('timeZone is not a valid IANA zone');
  });

  it('registers a single error interceptor across repeated calls', async () => {
    const getAccessToken = () => Promise.resolve('t');
    configureApiClient({ baseUrl: 'https://api.test', getAccessToken });
    configureApiClient({ baseUrl: 'https://api.test', getAccessToken });
    stubFetch(new Response(null, { status: 403 }));

    const error: unknown = await getStudio({ path: { studioId }, throwOnError: true }).catch((e: unknown) => e);

    // A duplicate interceptor would re-wrap the error without changing its status, so
    // assert on the interceptor list itself.
    expect(client.interceptors.error.fns.filter(Boolean)).toHaveLength(1);
    expect((error as ApiError).status).toBe(403);
  });
});
