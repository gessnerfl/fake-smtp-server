import React, { useState, useEffect } from 'react';
import { useDispatch, useSelector } from 'react-redux';
import { setCredentials, setAuthenticationRequired, setAuthError, clearAuthError } from '../store/auth-slice';
import { useGetMetaDataQuery, useLoginMutation } from '../store/rest-api';
import { Box, Button, Card, CardContent, Container, TextField, Typography, Alert } from '@mui/material';
import { RootState } from '../store/store';

const Login: React.FC = () => {
  const dispatch = useDispatch();
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const { data, isLoading } = useGetMetaDataQuery();
  const [login, { isLoading: isLoggingIn }] = useLoginMutation();
  const error = useSelector((state: RootState) => state.auth.error);

  useEffect(() => {
    if (data) {
      dispatch(setAuthenticationRequired(data.authenticationEnabled));
    }
  }, [data, dispatch]);

  useEffect(() => {
    dispatch(clearAuthError());
  }, [dispatch, username, password]);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();

    try {
      await login({ username, password }).unwrap();
      dispatch(setCredentials({ username, password }));
    } catch (error) {
      console.error(error);
      dispatch(setAuthError('Invalid username or password. Please try again.'));
    }
  };

  if (isLoading) {
    return <Box sx={{ display: 'flex', justifyContent: 'center', mt: 4 }}>Loading...</Box>;
  }

  if (!data?.authenticationEnabled) {
    return null;
  }

  return (
    <Container maxWidth="sm">
      <Box sx={{ mt: 8, display: 'flex', flexDirection: 'column', alignItems: 'center' }}>
        <Card sx={{ width: '100%' }}>
          <CardContent>
            <Typography component="h1" variant="h5" sx={{ mb: 2, textAlign: 'center' }}>
              Login to FakeSMTP Server
            </Typography>
            {error && (
              <Alert severity="error" sx={{ mb: 2 }}>
                {error}
              </Alert>
            )}
            <form onSubmit={handleSubmit}>
              <TextField
                margin="normal"
                required
                fullWidth
                id="username"
                label="Username"
                name="username"
                autoComplete="username"
                autoFocus
                value={username}
                onChange={(e) => setUsername(e.target.value)}
                disabled={isLoggingIn}
                error={!!error}
              />
              <TextField
                margin="normal"
                required
                fullWidth
                name="password"
                label="Password"
                type="password"
                id="password"
                autoComplete="current-password"
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                disabled={isLoggingIn}
                error={!!error}
              />
              <Button
                type="submit"
                fullWidth
                variant="contained"
                sx={{ mt: 3, mb: 2 }}
                disabled={!username || !password || isLoggingIn}
              >
                {isLoggingIn ? 'Signing In...' : 'Sign In'}
              </Button>
            </form>
          </CardContent>
        </Card>
      </Box>
    </Container>
  );
};

export default Login;
