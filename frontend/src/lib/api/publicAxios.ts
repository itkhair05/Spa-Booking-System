import axios from 'axios';

const apiBaseUrl = import.meta.env.VITE_API_BASE_URL ?? '';

export const publicAxios = axios.create({
  baseURL: apiBaseUrl ? `${apiBaseUrl}` : '/api/v1',
  headers: {
    'Content-Type': 'application/json',
  },
});
