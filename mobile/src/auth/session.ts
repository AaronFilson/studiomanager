/** What sign-in or a refresh returns. */
export interface Tokens {
  accessToken: string;
  /** When the access token expires, in epoch milliseconds. */
  expiresAt: number;
  /** Absent when the identity provider didn't rotate it; the previous one stays valid. */
  refreshToken?: string;
}

/** Where the refresh token is kept between launches: the device keychain or keystore. */
export interface RefreshTokenStore {
  get(): Promise<string | null>;
  set(token: string): Promise<void>;
  clear(): Promise<void>;
}

/** The identity provider refused the refresh token, so signing in again is the only way back. */
export class RefreshRejected extends Error {
  constructor(reason: string) {
    super(`The sign-in has ended: ${reason}`);
    this.name = 'RefreshRejected';
  }
}

export interface SessionDeps {
  /** Throws {@link RefreshRejected} if the token was refused; any other error (offline) keeps the session. */
  refresh: (refreshToken: string) => Promise<Tokens>;
  store: RefreshTokenStore;
  now?: () => number;
}

/** Refresh this long before expiry, so a token never runs out mid-request. */
const EXPIRY_MARGIN_MS = 60_000;

/**
 * The signed-in session: the access token in memory and the refresh token in secure storage.
 * Several requests asking at once share one refresh. A refresh the identity provider refuses
 * (revoked, expired after long disuse) ends the session.
 */
export class Session {
  private current: Tokens | null = null;
  private refreshing: Promise<string | undefined> | null = null;
  private readonly now: () => number;
  private readonly listeners = new Set<(signedIn: boolean) => void>();

  constructor(private readonly deps: SessionDeps) {
    this.now = deps.now ?? Date.now;
  }

  /** True if a refresh token from an earlier launch is stored. Doesn't contact the server. */
  async restore(): Promise<boolean> {
    return (await this.deps.store.get()) !== null;
  }

  async signedIn(tokens: Tokens): Promise<void> {
    this.current = tokens;
    if (tokens.refreshToken) {
      await this.deps.store.set(tokens.refreshToken);
    }
    this.emit(true);
  }

  async signOut(): Promise<void> {
    this.current = null;
    await this.deps.store.clear();
    this.emit(false);
  }

  /** A valid access token, refreshing it if needed; `undefined` when signed out. */
  getAccessToken(): Promise<string | undefined> {
    const current = this.current;
    if (current && current.expiresAt - EXPIRY_MARGIN_MS > this.now()) {
      return Promise.resolve(current.accessToken);
    }
    this.refreshing ??= this.refresh().finally(() => {
      this.refreshing = null;
    });
    return this.refreshing;
  }

  onChange(listener: (signedIn: boolean) => void): () => void {
    this.listeners.add(listener);
    return () => this.listeners.delete(listener);
  }

  private async refresh(): Promise<string | undefined> {
    const refreshToken = this.current?.refreshToken ?? (await this.deps.store.get());
    if (!refreshToken) {
      return undefined;
    }
    let tokens: Tokens;
    try {
      tokens = await this.deps.refresh(refreshToken);
    } catch (error) {
      if (error instanceof RefreshRejected) {
        await this.signOut();
        return undefined;
      }
      // Offline or a server error: stay signed in, and let this request fail
      throw error;
    }
    this.current = { ...tokens, refreshToken: tokens.refreshToken ?? refreshToken };
    if (tokens.refreshToken && tokens.refreshToken !== refreshToken) {
      await this.deps.store.set(tokens.refreshToken);
    }
    return tokens.accessToken;
  }

  private emit(signedIn: boolean): void {
    for (const listener of this.listeners) listener(signedIn);
  }
}
