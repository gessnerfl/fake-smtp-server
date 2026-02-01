import {
  clearAuthSessionStorage,
  loadLastActivity,
  persistLastActivity,
  persistSessionTimeoutMs,
} from './auth-session';

describe('auth-session', () => {
  afterEach(() => {
    jest.restoreAllMocks();
  });

  it('persists and loads last activity timestamps', () => {
    expect(loadLastActivity()).toBeNull();
    persistLastActivity(1_000);
    expect(loadLastActivity()).toBe(1_000);
  });

  it('clears stored session tracking data', () => {
    persistLastActivity(2_000);
    persistSessionTimeoutMs(3_000);

    clearAuthSessionStorage();

    expect(loadLastActivity()).toBeNull();
    expect(window.sessionStorage.getItem('fakesmtp.auth.sessionTimeoutMs')).toBeNull();
  });
});
