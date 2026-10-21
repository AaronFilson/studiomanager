import { RefreshRejected, Session, type Tokens } from './session';

function memoryStore(initial: string | null = null) {
  let value = initial;
  return {
    get: jest.fn(() => Promise.resolve(value)),
    set: jest.fn((token: string) => {
      value = token;
      return Promise.resolve();
    }),
    clear: jest.fn(() => {
      value = null;
      return Promise.resolve();
    }),
    peek: () => value,
  };
}

const NOW = 1_000_000;
const tokens = (accessToken: string, lifetimeMs: number, refreshToken?: string): Tokens => ({
  accessToken,
  expiresAt: NOW + lifetimeMs,
  refreshToken,
});

describe('Session', () => {
  it('hands out the access token while it is fresh', async () => {
    const refresh = jest.fn();
    const session = new Session({ store: memoryStore(), refresh, now: () => NOW });
    await session.signedIn(tokens('a1', 10 * 60_000, 'r1'));

    await expect(session.getAccessToken()).resolves.toBe('a1');
    expect(refresh).not.toHaveBeenCalled();
  });

  it('refreshes near expiry, once for concurrent callers, and keeps the rotated refresh token', async () => {
    const store = memoryStore();
    const refresh = jest.fn(() => Promise.resolve(tokens('a2', 60 * 60_000, 'r2')));
    const session = new Session({ store, refresh, now: () => NOW });
    await session.signedIn(tokens('a1', 30_000, 'r1'));

    const [first, second] = await Promise.all([session.getAccessToken(), session.getAccessToken()]);

    expect([first, second]).toEqual(['a2', 'a2']);
    expect(refresh).toHaveBeenCalledTimes(1);
    expect(refresh).toHaveBeenCalledWith('r1');
    expect(store.peek()).toBe('r2');
  });

  it('restores a session from a stored refresh token after a restart', async () => {
    const refresh = jest.fn(() => Promise.resolve(tokens('a9', 60 * 60_000)));
    const session = new Session({ store: memoryStore('stored'), refresh, now: () => NOW });

    await expect(session.restore()).resolves.toBe(true);
    await expect(session.getAccessToken()).resolves.toBe('a9');
    expect(refresh).toHaveBeenCalledWith('stored');
  });

  it('signs out when the identity provider refuses the refresh token', async () => {
    const store = memoryStore('stale');
    const listener = jest.fn();
    const session = new Session({
      store,
      refresh: () => Promise.reject(new RefreshRejected('invalid_grant')),
      now: () => NOW,
    });
    session.onChange(listener);

    await expect(session.getAccessToken()).resolves.toBeUndefined();
    expect(store.peek()).toBeNull();
    expect(listener).toHaveBeenCalledWith(false);
  });

  it('stays signed in when the refresh fails for another reason, such as being offline', async () => {
    const store = memoryStore('kept');
    const session = new Session({
      store,
      refresh: () => Promise.reject(new TypeError('Network request failed')),
      now: () => NOW,
    });

    await expect(session.getAccessToken()).rejects.toThrow('Network request failed');
    expect(store.peek()).toBe('kept');
  });

  it('is signed out with nothing stored', async () => {
    const session = new Session({ store: memoryStore(), refresh: jest.fn(), now: () => NOW });

    await expect(session.restore()).resolves.toBe(false);
    await expect(session.getAccessToken()).resolves.toBeUndefined();
  });
});
