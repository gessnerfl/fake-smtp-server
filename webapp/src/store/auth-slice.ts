import { createSlice, PayloadAction } from '@reduxjs/toolkit';
import { Credentials } from '../models/auth';

interface AuthState {
  isAuthenticated: boolean;
  credentials: Credentials | null;
  authenticationRequired: boolean;
  error: string | null;
}

const initialState: AuthState = {
  isAuthenticated: false,
  credentials: null,
  authenticationRequired: false,
  error: null,
};

export const authSlice = createSlice({
  name: 'auth',
  initialState,
  reducers: {
    setCredentials: (state, action: PayloadAction<Credentials>) => {
      state.credentials = action.payload;
      state.isAuthenticated = true;
      state.error = null;
    },
    clearCredentials: (state) => {
      state.credentials = null;
      state.isAuthenticated = false;
      state.error = null;
    },
    setAuthenticationRequired: (state, action: PayloadAction<boolean>) => {
      state.authenticationRequired = action.payload;
    },
    setAuthError: (state, action: PayloadAction<string>) => {
      state.error = action.payload;
      state.isAuthenticated = false;
    },
    clearAuthError: (state) => {
      state.error = null;
    },
  },
});

export const {
  setCredentials,
  clearCredentials,
  setAuthenticationRequired,
  setAuthError,
  clearAuthError
} = authSlice.actions;

export default authSlice.reducer;
