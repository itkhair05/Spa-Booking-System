/**
 * A tiny event-bus that lets the Axios response interceptor signal an
 * authentication failure without importing React hooks into the API layer.
 *
 * Usage:
 *   - Axios interceptor fires: emitAuthFailure()
 *   - AuthProvider listens:    subscribeToAuthFailure(callback)
 */

type Listener = () => void;

const listeners = new Set<Listener>();

export function emitAuthFailure(): void {
  listeners.forEach((cb) => cb());
}

export function subscribeToAuthFailure(cb: Listener): () => void {
  listeners.add(cb);
  return () => {
    listeners.delete(cb);
  };
}
