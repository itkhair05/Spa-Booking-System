import { useState, useEffect, useCallback } from 'react';
import type { ReactNode } from 'react';
import api from '../../lib/api/axios';
import { subscribeToAuthFailure } from '../../lib/api/authEvents';
import type { LoginResponse } from '../../types/auth';
import { AuthContext } from './authTypes';
import type { AuthContextProps } from './authTypes';

/**
 * Read and validate persisted auth state from localStorage.
 * Returns the restored state or null values if data is missing/malformed.
 */
function loadPersistedAuth(): { token: string | null; user: AuthContextProps['user'] } {
  const storedToken = localStorage.getItem('accessToken');
  const storedUser = localStorage.getItem('authUser');
  if (storedToken && storedUser) {
    try {
      const parsed = JSON.parse(storedUser) as { username?: string; roles?: string[] };
      if (
        typeof parsed === 'object' &&
        parsed !== null &&
        typeof parsed.username === 'string' &&
        Array.isArray(parsed.roles)
      ) {
        return { token: storedToken, user: { username: parsed.username, roles: parsed.roles } };
      }
    } catch {
      // Malformed JSON — fall through to clear
    }
    // Persisted data is invalid — clear it
    localStorage.removeItem('accessToken');
    localStorage.removeItem('authUser');
  }
  return { token: null, user: null };
}

function clearStorage() {
  localStorage.removeItem('accessToken');
  localStorage.removeItem('authUser');
}

export const AuthProvider = ({ children }: { children: ReactNode }) => {
  // Use lazy initializers to synchronously restore persisted auth (no effect needed)
  const [persisted] = useState(loadPersistedAuth);
  const [user, setUser] = useState<AuthContextProps['user']>(persisted.user);
  const [token, setToken] = useState<string | null>(persisted.token);

  // isLoading is false from the start because we initialize synchronously
  const isLoading = false;

  // Stable logout reference (used by the event-bus subscription)
  const logout = useCallback(() => {
    clearStorage();
    setToken(null);
    setUser(null);
  }, []);

  // Subscribe to Axios 401 events so expired/invalid sessions are cleared automatically
  useEffect(() => {
    const unsubscribe = subscribeToAuthFailure(logout);
    return unsubscribe;
  }, [logout]);

  const login = async (data: Parameters<AuthContextProps['login']>[0]) => {
    const response = await api.post<LoginResponse>('/auth/login', data);
    const { accessToken, username, roles } = response.data;
    const authUser = { username, roles };
    localStorage.setItem('accessToken', accessToken);
    localStorage.setItem('authUser', JSON.stringify(authUser));
    setToken(accessToken);
    setUser(authUser);
  };

  const updateUser = useCallback((updatedUser: { username: string; roles: string[] }, newToken?: string) => {
    setUser(updatedUser);
    localStorage.setItem('authUser', JSON.stringify(updatedUser));
    if (newToken) {
      setToken(newToken);
      localStorage.setItem('accessToken', newToken);
    }
  }, []);

  return (
    <AuthContext.Provider value={{ user, token, isLoading, login, logout, updateUser }}>
      {children}
    </AuthContext.Provider>
  );
};
