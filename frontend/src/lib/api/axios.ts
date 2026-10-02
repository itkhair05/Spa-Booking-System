import axios from 'axios';
import type { InternalAxiosRequestConfig } from 'axios';

// Base URL from environment variable; fallback to empty string for safety
const apiBaseUrl = import.meta.env.VITE_API_BASE_URL ?? '';

const api = axios.create({
  baseURL: `${apiBaseUrl}/api/v1`,
  headers: {
    'Content-Type': 'application/json',
  },
});

// Request interceptor to attach JWT token if present in localStorage
api.interceptors.request.use((config: InternalAxiosRequestConfig) => {
  const token = localStorage.getItem('accessToken');
  if (token) {
    config.headers.set('Authorization', `Bearer ${token}`);
  }
  return config;
});

// Response interceptor to handle auth errors globally
api.interceptors.response.use(
  (response) => response,
  (error) => {
    // If unauthorized, optionally clear token (client can react to 401 elsewhere)
    if (error.response?.status === 401) {
      localStorage.removeItem('accessToken');
    }
    return Promise.reject(error);
  },
);

export default api;
