import { afterEach, describe, expect, it, vi } from 'vitest';
import { api, ApiError } from '../api';

const okJson = (body: unknown, status = 200) =>
  Promise.resolve({ ok: status < 300, status, json: () => Promise.resolve(body) } as Response);

describe('api client', () => {
  afterEach(() => {
    vi.unstubAllGlobals();
  });

  it('returns parsed JSON on success', async () => {
    vi.stubGlobal('fetch', vi.fn().mockReturnValue(okJson({ status: 'UP' })));
    await expect(api.health()).resolves.toEqual({ status: 'UP' });
  });

  it('throws a network ApiError when fetch rejects', async () => {
    vi.stubGlobal('fetch', vi.fn().mockRejectedValue(new TypeError('failed to fetch')));
    await expect(api.health()).rejects.toMatchObject({ kind: 'network' });
  });

  it('surfaces the backend error message on 400', async () => {
    vi.stubGlobal('fetch', vi.fn().mockReturnValue(okJson({ message: 'Alpha must be in (0, 1]' }, 400)));
    await expect(api.health()).rejects.toMatchObject({ kind: 'validation', message: 'Alpha must be in (0, 1]' });
  });

  it('maps 404 to a not-found ApiError', async () => {
    vi.stubGlobal('fetch', vi.fn().mockReturnValue(okJson({ message: 'not found' }, 404)));
    await expect(api.health()).rejects.toMatchObject({ kind: 'not-found' });
  });

  it('falls back to a generic message when the error body is not JSON', async () => {
    vi.stubGlobal(
      'fetch',
      vi.fn().mockReturnValue(Promise.resolve({ ok: false, status: 500, json: () => Promise.reject(new Error('no body')) } as Response)),
    );
    await expect(api.health()).rejects.toBeInstanceOf(ApiError);
  });
});
