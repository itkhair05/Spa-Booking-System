import axios from 'axios';
import type { InternalAxiosRequestConfig } from 'axios';
import { emitAuthFailure } from './authEvents';
import { API_BASE_URL } from './apiConfig';

const api = axios.create({
  baseURL: API_BASE_URL,
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
