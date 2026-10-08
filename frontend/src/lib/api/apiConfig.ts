/**
 * Resolves the backend API base URL from the VITE_API_BASE_URL environment variable.
 * Ensures the resulting base URL ends with '/api/v1' for API path consistency across
 * same-origin (local dev / Nginx proxy) and cross-origin (production custom domain) deployments.
 */
export function resolveApiBaseUrl(rawUrl?: string): string {
  if (!rawUrl || rawUrl.trim() === '') {
    return '/api/v1';
  }
  const trimmed = rawUrl.trim().replace(/\/+$/, '');
  return trimmed.endsWith('/api/v1') ? trimmed : `${trimmed}/api/v1`;
}

export const API_BASE_URL = resolveApiBaseUrl(import.meta.env.VITE_API_BASE_URL);
