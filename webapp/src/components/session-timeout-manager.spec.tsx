import * as React from 'react';
import '@testing-library/jest-dom';
import { act, waitFor } from '@testing-library/react';
import { renderWithProviders } from '../test-utils';
import SessionTimeoutManager from './session-timeout-manager';
import { useGetMetaDataQuery, useLogoutMutation } from '../store/rest-api';
import { setupStore } from '../store/store';
import { setAuthenticated } from '../store/auth-slice';
import { loadLastActivity } from '../store/auth-session';

jest.mock('../store/rest-api', () => {
  return {
    useGetMetaDataQuery: jest.fn(),
    useLogoutMutation: jest.fn(),
  };
});

describe('SessionTimeoutManager', () => {
  const createRefetchResult = (data: Record<string, unknown>) => ({
    data,
    unwrap: jest.fn().mockResolvedValue(data),
  });

  beforeEach(() => {
    jest.clearAllMocks();
    const mockLogout = jest.fn().mockReturnValue({
      unwrap: jest.fn().mockResolvedValue({}),
    });
    (useLogoutMutation as jest.Mock).mockReturnValue([mockLogout]);
  });

  afterEach(() => {
    jest.useRealTimers();
    jest.restoreAllMocks();
  });

  it('revalidates with the server when the timer fires and clears expired sessions', async () => {
    jest.useFakeTimers();
    jest.spyOn(Date, 'now').mockReturnValue(1_000_000);

    const refetch = jest.fn().mockReturnValue(
      createRefetchResult({
        version: 'local',
        authenticationEnabled: true,
        authenticated: false,
        sessionTimeoutMinutes: 0.01,
      })
    );

    (useGetMetaDataQuery as jest.Mock).mockReturnValue({
      data: { version: 'local', authenticationEnabled: true, authenticated: true, sessionTimeoutMinutes: 0.01 },
      refetch,
      isLoading: false,
    });

    const store = setupStore();
    store.dispatch(setAuthenticated(true));

    renderWithProviders(<SessionTimeoutManager />, { store });

    act(() => {
      jest.advanceTimersByTime(1_000);
    });

    await waitFor(() => expect(refetch).toHaveBeenCalledTimes(1));
    await waitFor(() => {
      expect(store.getState().auth.isAuthenticated).toBe(false);
    });
    expect(window.sessionStorage.getItem('fakesmtp.auth.lastActivity')).toBeNull();
  });

  it('refreshes activity and re-arms the timer after a successful timer revalidation', async () => {
    jest.useFakeTimers();
    let now = 1_000_000;
    jest.spyOn(Date, 'now').mockImplementation(() => now);

    const refetch = jest.fn().mockReturnValue(
      createRefetchResult({
        version: 'local',
        authenticationEnabled: true,
        authenticated: true,
        sessionTimeoutMinutes: 1,
      })
    );

    (useGetMetaDataQuery as jest.Mock).mockReturnValue({
      data: { version: 'local', authenticationEnabled: true, authenticated: true, sessionTimeoutMinutes: 1 },
      refetch,
      isLoading: false,
    });

    const store = setupStore();
    store.dispatch(setAuthenticated(true));

    renderWithProviders(<SessionTimeoutManager />, { store });

    now = 1_060_000;
    act(() => {
      jest.advanceTimersByTime(60_000);
    });

    await waitFor(() => expect(refetch).toHaveBeenCalledTimes(1));
    await waitFor(() => {
      expect(store.getState().auth.isAuthenticated).toBe(true);
    });

    await waitFor(() => expect(loadLastActivity()).toBe(now));

    now = 1_120_000;
    act(() => {
      jest.advanceTimersByTime(60_000);
    });

    await waitFor(() => expect(refetch).toHaveBeenCalledTimes(2));
  });

  it('revalidates on visibility change and clears an expired server session', async () => {
    jest.useFakeTimers();
    jest.spyOn(Date, 'now').mockReturnValue(1_000_000);

    const refetch = jest.fn().mockReturnValue(
      createRefetchResult({
        version: 'local',
        authenticationEnabled: true,
        authenticated: false,
        sessionTimeoutMinutes: 0.01,
      })
    );

    (useGetMetaDataQuery as jest.Mock).mockReturnValue({
      data: { version: 'local', authenticationEnabled: true, authenticated: true, sessionTimeoutMinutes: 0.01 },
      refetch,
      isLoading: false,
    });

    const store = setupStore();
    store.dispatch(setAuthenticated(true));

    Object.defineProperty(document, 'visibilityState', {
      configurable: true,
      value: 'visible',
    });

    renderWithProviders(<SessionTimeoutManager />, { store });

    act(() => {
      document.dispatchEvent(new Event('visibilitychange'));
    });

    await waitFor(() => expect(refetch).toHaveBeenCalledTimes(1));
    await waitFor(() => expect(store.getState().auth.isAuthenticated).toBe(false));
    expect(window.sessionStorage.getItem('fakesmtp.auth.lastActivity')).toBeNull();
  });

  it('revalidates on activity checkpoints', async () => {
    jest.useFakeTimers();
    jest.spyOn(Date, 'now').mockReturnValue(1_000_000);

    const refetch = jest.fn().mockReturnValue(
      createRefetchResult({
        version: 'local',
        authenticationEnabled: true,
        authenticated: true,
        sessionTimeoutMinutes: 0.01,
      })
    );

    (useGetMetaDataQuery as jest.Mock).mockReturnValue({
      data: { version: 'local', authenticationEnabled: true, authenticated: true, sessionTimeoutMinutes: 0.01 },
      refetch,
      isLoading: false,
    });

    const store = setupStore();
    store.dispatch(setAuthenticated(true));

    renderWithProviders(<SessionTimeoutManager />, { store });

    act(() => {
      window.dispatchEvent(new Event('mousemove'));
    });

    await waitFor(() => expect(refetch).toHaveBeenCalledTimes(1));
    expect(store.getState().auth.isAuthenticated).toBe(true);
  });

  it('clears auth state and session storage when activity revalidation returns authenticated false', async () => {
    jest.useFakeTimers();
    jest.spyOn(Date, 'now').mockReturnValue(1_000_000);

    const refetch = jest.fn().mockReturnValue(
      createRefetchResult({
        version: 'local',
        authenticationEnabled: true,
        authenticated: false,
        sessionTimeoutMinutes: 0.01,
      })
    );

    (useGetMetaDataQuery as jest.Mock).mockReturnValue({
      data: { version: 'local', authenticationEnabled: true, authenticated: true, sessionTimeoutMinutes: 0.01 },
      refetch,
      isLoading: false,
    });

    const store = setupStore();
    store.dispatch(setAuthenticated(true));

    renderWithProviders(<SessionTimeoutManager />, { store });

    act(() => {
      window.dispatchEvent(new Event('mousemove'));
    });

    await waitFor(() => expect(refetch).toHaveBeenCalledTimes(1));
    await waitFor(() => expect(store.getState().auth.isAuthenticated).toBe(false));
    expect(window.sessionStorage.getItem('fakesmtp.auth.lastActivity')).toBeNull();
    expect(window.sessionStorage.getItem('fakesmtp.auth.sessionTimeoutMs')).toBeNull();
  });

  it.each([401, 403])('clears auth state and session storage when activity revalidation fails with %s', async (status) => {
    jest.useFakeTimers();
    jest.spyOn(Date, 'now').mockReturnValue(1_000_000);

    const refetch = jest.fn().mockReturnValue({
      unwrap: jest.fn().mockRejectedValue({ status }),
    });

    (useGetMetaDataQuery as jest.Mock).mockReturnValue({
      data: { version: 'local', authenticationEnabled: true, authenticated: true, sessionTimeoutMinutes: 0.01 },
      refetch,
      isLoading: false,
    });

    const store = setupStore();
    store.dispatch(setAuthenticated(true));

    renderWithProviders(<SessionTimeoutManager />, { store });

    act(() => {
      window.dispatchEvent(new Event('mousemove'));
    });

    await waitFor(() => expect(refetch).toHaveBeenCalledTimes(1));
    await waitFor(() => expect(store.getState().auth.isAuthenticated).toBe(false));
    expect(window.sessionStorage.getItem('fakesmtp.auth.lastActivity')).toBeNull();
    expect(window.sessionStorage.getItem('fakesmtp.auth.sessionTimeoutMs')).toBeNull();
  });

  it('does not throttle the first activity check after a session is cleared and quickly recreated', async () => {
    jest.useFakeTimers();
    let now = 1_000_000;
    jest.spyOn(Date, 'now').mockImplementation(() => now);

    const refetch = jest.fn()
      .mockReturnValueOnce(createRefetchResult({
        version: 'local',
        authenticationEnabled: true,
        authenticated: false,
        sessionTimeoutMinutes: 1,
      }))
      .mockReturnValueOnce(createRefetchResult({
        version: 'local',
        authenticationEnabled: true,
        authenticated: true,
        sessionTimeoutMinutes: 1,
      }));

    (useGetMetaDataQuery as jest.Mock).mockReturnValue({
      data: { version: 'local', authenticationEnabled: true, authenticated: true, sessionTimeoutMinutes: 1 },
      refetch,
      isLoading: false,
    });

    const store = setupStore();
    store.dispatch(setAuthenticated(true));

    renderWithProviders(<SessionTimeoutManager />, { store });

    act(() => {
      window.dispatchEvent(new Event('mousemove'));
    });

    await waitFor(() => expect(refetch).toHaveBeenCalledTimes(1));
    await waitFor(() => expect(store.getState().auth.isAuthenticated).toBe(false));

    now = 1_002_000;
    act(() => {
      store.dispatch(setAuthenticated(true));
    });

    act(() => {
      window.dispatchEvent(new Event('mousemove'));
    });

    await waitFor(() => expect(refetch).toHaveBeenCalledTimes(2));
  });
});
