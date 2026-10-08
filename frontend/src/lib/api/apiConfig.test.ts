import { describe, it } from 'node:test';
import assert from 'node:assert/strict';
import { resolveMediaUrl, resolveApiBaseUrl } from './apiConfig.ts';

describe('resolveApiBaseUrl', () => {
  it('defaults to /api/v1 when undefined or blank', () => {
    assert.equal(resolveApiBaseUrl(), '/api/v1');
    assert.equal(resolveApiBaseUrl(''), '/api/v1');
    assert.equal(resolveApiBaseUrl('   '), '/api/v1');
  });

  it('ensures /api/v1 suffix without duplicating it', () => {
    assert.equal(
      resolveApiBaseUrl('https://api.example.com'),
      'https://api.example.com/api/v1'
    );
    assert.equal(
      resolveApiBaseUrl('https://api.example.com/'),
      'https://api.example.com/api/v1'
    );
    assert.equal(
      resolveApiBaseUrl('https://api.example.com/api/v1'),
      'https://api.example.com/api/v1'
    );
    assert.equal(
      resolveApiBaseUrl('https://api.example.com/api/v1/'),
      'https://api.example.com/api/v1'
    );
  });
});

describe('resolveMediaUrl', () => {
  const prodApiBase = 'https://spa-booking-system-production-6279.up.railway.app/api/v1';
  const localApiBase = '/api/v1';
  const localhostApiBase = 'http://localhost:8080/api/v1';

  it('returns empty string for null, undefined, or empty path', () => {
    assert.equal(resolveMediaUrl(null, prodApiBase), '');
    assert.equal(resolveMediaUrl(undefined, prodApiBase), '');
    assert.equal(resolveMediaUrl('', prodApiBase), '');
    assert.equal(resolveMediaUrl('   ', prodApiBase), '');
  });

  it('preserves absolute URLs (http, https, data, blob) unchanged', () => {
    assert.equal(
      resolveMediaUrl('https://images.unsplash.com/photo-123.jpg', prodApiBase),
      'https://images.unsplash.com/photo-123.jpg'
    );
    assert.equal(
      resolveMediaUrl('http://example.com/avatar.png', prodApiBase),
      'http://example.com/avatar.png'
    );
    assert.equal(
      resolveMediaUrl('blob:http://localhost:5173/uuid-blob-123', prodApiBase),
      'blob:http://localhost:5173/uuid-blob-123'
    );
    assert.equal(
      resolveMediaUrl('data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAE=', prodApiBase),
      'data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAE='
    );
  });

  it('prepends backend origin in production for /api/v1/uploads/... paths without duplicating /api/v1', () => {
    const input = '/api/v1/uploads/services/123e4567-e89b-12d3-a456-426614174000.jpg';
    const expected = 'https://spa-booking-system-production-6279.up.railway.app/api/v1/uploads/services/123e4567-e89b-12d3-a456-426614174000.jpg';
    assert.equal(resolveMediaUrl(input, prodApiBase), expected);
  });

  it('works with backend origin ending in slash or without /api/v1', () => {
    const input = '/api/v1/uploads/avatars/staff-1.png';
    const expected = 'https://spa-booking-system-production-6279.up.railway.app/api/v1/uploads/avatars/staff-1.png';
    assert.equal(
      resolveMediaUrl(input, 'https://spa-booking-system-production-6279.up.railway.app'),
      expected
    );
    assert.equal(
      resolveMediaUrl(input, 'https://spa-booking-system-production-6279.up.railway.app/'),
      expected
    );
  });

  it('preserves relative /api/v1 path in local development when same-origin/proxy is used', () => {
    const input = '/api/v1/uploads/services/test.jpg';
    assert.equal(resolveMediaUrl(input, localApiBase), input);
  });

  it('prepends localhost backend origin when configured with explicit localhost port', () => {
    const input = '/api/v1/uploads/services/test.jpg';
    assert.equal(
      resolveMediaUrl(input, localhostApiBase),
      'http://localhost:8080/api/v1/uploads/services/test.jpg'
    );
  });

  it('handles paths with /uploads/ or uploads/ without /api/v1 prefix gracefully', () => {
    assert.equal(
      resolveMediaUrl('/uploads/services/test.jpg', prodApiBase),
      'https://spa-booking-system-production-6279.up.railway.app/api/v1/uploads/services/test.jpg'
    );
    assert.equal(
      resolveMediaUrl('uploads/articles/cover.webp', prodApiBase),
      'https://spa-booking-system-production-6279.up.railway.app/api/v1/uploads/articles/cover.webp'
    );
    assert.equal(
      resolveMediaUrl('/uploads/services/test.jpg', localApiBase),
      '/api/v1/uploads/services/test.jpg'
    );
  });

  it('preserves other relative asset paths as-is', () => {
    assert.equal(resolveMediaUrl('/assets/spa-logo.svg', prodApiBase), '/assets/spa-logo.svg');
    assert.equal(resolveMediaUrl('./favicon.ico', prodApiBase), './favicon.ico');
  });

  it('does not produce double slashes or duplicated /api/v1 in output', () => {
    const result = resolveMediaUrl('/api/v1/uploads/services/item.png', 'https://api.example.com/api/v1/');
    assert.equal(result, 'https://api.example.com/api/v1/uploads/services/item.png');
    assert.ok(!result.includes('//api/v1'));
    assert.ok(!result.includes('/api/v1/api/v1'));
  });
});
