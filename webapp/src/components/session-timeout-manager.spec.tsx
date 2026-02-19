import * as React from 'react';
import '@testing-library/jest-dom';
import { act, waitFor } from '@testing-library/react';
import { renderWithProviders } from '../test-utils';
import SessionTimeoutManager from './session-timeout-manager';
import { useGetMetaDataQuery, useLogoutMutation } from '../store/rest-api';
import { setupStore } from '../store/store';
import { setAuthenticated } from '../store/auth-slice';
import { persistLastActivity } from '../store/auth-session';

jest.mock('../store/rest-api', () => {
  return {
    useGetMetaDataQuery: jest.fn(),
    useLogoutMutation: jest.fn(),
  };
});

describe('SessionTimeoutManager', () => {
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

  it('logs out after inactivity timeout', async () => {
    jest.useFakeTimers();
    jest.spyOn(Date, 'now').mockReturnValue(1_000_000);

    (useGetMetaDataQuery as jest.Mock).mockReturnValue({
      data: { version: 'local', authenticationEnabled: true, sessionTimeoutMinutes: 0.01 },
      isLoading: false,
    });

    const store = setupStore();
    store.dispatch(setAuthenticated(true));

    renderWithProviders(<SessionTimeoutManager />, { store });

    act(() => {
      jest.advanceTimersByTime(1_000);
    });

    await waitFor(() => {
      expect(store.getState().auth.isAuthenticated).toBe(false);
    });
    expect(window.sessionStorage.getItem('fakesmtp.auth.lastActivity')).toBeNull();
  });

  it('logs out immediately when stored activity is already expired', async () => {
    jest.spyOn(Date, 'now').mockReturnValue(1_000_000);

    (useGetMetaDataQuery as jest.Mock).mockReturnValue({
      data: { version: 'local', authenticationEnabled: true, sessionTimeoutMinutes: 0.01 },
      isLoading: false,
    });

    const store = setupStore();
    store.dispatch(setAuthenticated(true));
    persistLastActivity(990_000);

    renderWithProviders(<SessionTimeoutManager />, { store });

    await waitFor(() => {
      expect(store.getState().auth.isAuthenticated).toBe(false);
    });
  });
});
