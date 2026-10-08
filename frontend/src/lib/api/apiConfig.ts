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

export const API_BASE_URL = resolveApiBaseUrl(
  typeof import.meta !== 'undefined' && import.meta.env ? import.meta.env.VITE_API_BASE_URL : undefined
);

/**
 * Resolves a media / image URL for rendering in the browser.
 * Handles cross-origin deployments where the frontend is hosted separately
 * (e.g., Cloudflare Pages) from the API backend (e.g., Railway).
 *
 * Rules:
 * 1. null / undefined / empty -> returns ''
 * 2. Absolute URL (http://, https://, data:, blob:) -> returns unchanged
 * 3. Relative path starting with '/api/v1' -> prepends backend origin from API_BASE_URL
 * 4. Relative path starting with '/uploads' or 'uploads/' -> prepends API_BASE_URL
 * 5. Other relative paths -> returned as-is (preserves local frontend assets)
 */
export function resolveMediaUrl(rawPath?: string | null, apiBaseUrl: string = API_BASE_URL): string {
  if (!rawPath || typeof rawPath !== 'string') {
    return '';
  }

  const trimmedPath = rawPath.trim();
  if (trimmedPath === '') {
    return '';
  }

  // Already absolute or browser-internal (blob, data) URL
  if (
    trimmedPath.startsWith('http://') ||
    trimmedPath.startsWith('https://') ||
    trimmedPath.startsWith('data:') ||
    trimmedPath.startsWith('blob:')
  ) {
    return trimmedPath;
  }

  // Normalize API base URL by stripping trailing slashes
  const normalizedBase = (apiBaseUrl || '').trim().replace(/\/+$/, '');

  // Extract origin (everything before /api/v1)
  const origin = normalizedBase.endsWith('/api/v1')
    ? normalizedBase.slice(0, -'/api/v1'.length)
    : normalizedBase;

  // Case 1: Path begins with /api/v1/ (e.g. /api/v1/uploads/services/uuid.jpg)
  if (trimmedPath.startsWith('/api/v1/')) {
    if (!origin) {
      // Local dev / same-origin proxy
      return trimmedPath;
    }
    return `${origin}${trimmedPath}`;
  }

  // Case 2: Uploaded media path without /api/v1 (e.g. /uploads/... or uploads/...)
  const uploadsPrefix = trimmedPath.startsWith('/uploads/')
    ? trimmedPath
    : trimmedPath.startsWith('uploads/')
      ? `/${trimmedPath}`
      : null;

  if (uploadsPrefix) {
    if (!origin) {
      return `/api/v1${uploadsPrefix}`;
    }
    return `${origin}/api/v1${uploadsPrefix}`;
  }

  // Case 3: Other relative path (e.g. local assets /icons/...) -> preserve as-is
  return trimmedPath;
}
