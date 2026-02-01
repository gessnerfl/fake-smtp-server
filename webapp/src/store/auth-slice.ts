import { createSlice, PayloadAction } from '@reduxjs/toolkit';

interface AuthState {
  isAuthenticated: boolean;
  authenticationRequired: boolean;
  error: string | null;
}

const initialState: AuthState = {
  isAuthenticated: false,
  authenticationRequired: false,
  error: null,
};

export const authSlice = createSlice({
  name: 'auth',
  initialState,
  reducers: {
    setAuthenticated: (state, action: PayloadAction<boolean>) => {
      state.isAuthenticated = action.payload;
      if (action.payload) {
        state.error = null;
      }
    },
    clearAuthentication: (state) => {
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
  setAuthenticated,
  clearAuthentication,
  setAuthenticationRequired,
  setAuthError,
  clearAuthError
} = authSlice.actions;

export default authSlice.reducer;
