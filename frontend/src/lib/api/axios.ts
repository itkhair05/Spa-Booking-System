import axios from 'axios';
import type { InternalAxiosRequestConfig } from 'axios';
import { emitAuthFailure } from './authEvents';

// Base URL from environment variable; Vite dev-server proxy handles /api/* already,
// so when VITE_API_BASE_URL is not set the default baseURL stays as /api/v1.
const apiBaseUrl = import.meta.env.VITE_API_BASE_URL ?? '';

const api = axios.create({
  baseURL: apiBaseUrl ? `${apiBaseUrl}` : '/api/v1',
  headers: {
    'Content-Type': 'application/json',
  },
});

// Request interceptor: attach JWT if present
api.interceptors.request.use((config: InternalAxiosRequestConfig) => {
  const token = localStorage.getItem('accessToken');
  if (token) {
    config.headers.set('Authorization', `Bearer ${token}`);
  }
  return config;
});

// Response interceptor: handle 401 centrally
api.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response?.status === 401) {
      // Clear all stored auth state
      localStorage.removeItem('accessToken');
      localStorage.removeItem('authUser');
      // Notify AuthContext via event-bus (avoids importing React hooks here)
      emitAuthFailure();
    }
    return Promise.reject(error);
  },
);

export default api;
