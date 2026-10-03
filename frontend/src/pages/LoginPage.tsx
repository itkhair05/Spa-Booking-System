import { useState, useId } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import { useAuth } from '../app/auth/useAuth';
import { Button } from '../components/ui/Button';
import { Sparkles } from 'lucide-react';
import { Input } from '../components/ui/Input';
import type { LoginRequest } from '../types/auth';
import type { AxiosError } from 'axios';
import type { ApiError } from '../types/api';

/**
 * Returns a safe internal redirect path.
 * Only accepts paths that begin with exactly one "/".
 * Rejects protocol-relative (//, //evil.com), backslash tricks,
 * control characters, and any path whose resolved origin differs
 * from the current page (catches https://..., http://..., etc.).
 * Falls back to /dashboard on any rejection.
 */
function sanitizeRedirect(raw: string | null): string {
  if (!raw || !raw.startsWith('/') || raw.startsWith('//') || raw.includes('\\')) {
    return '/dashboard';
  }
  // Reject control characters (U+0000–U+001F, U+007F) without a regex literal
  for (let i = 0; i < raw.length; i++) {
    const cp = raw.codePointAt(i) ?? 0;
    if (cp <= 0x1f || cp === 0x7f) {
      return '/dashboard';
    }
  }
  try {
    const url = new URL(raw, window.location.origin);
    if (url.origin !== window.location.origin) {
      return '/dashboard';
    }
    // Reconstruct from parsed parts to strip any injected scheme
    return `${url.pathname}${url.search}${url.hash}`;
  } catch {
    return '/dashboard';
  }
}


type LoginError =
  | { kind: 'credentials' }
  | { kind: 'network' }
  | { kind: 'validation' }
  | { kind: 'server' };

function resolveError(err: unknown): LoginError {
  const axiosErr = err as AxiosError<ApiError>;
  const status = axiosErr.response?.status;

  if (!axiosErr.response) {
    // Network-level failure (no response received)
    return { kind: 'network' };
  }
  if (status === 401 || status === 403) {
    return { kind: 'credentials' };
  }
  if (status === 400) {
    return { kind: 'validation' };
  }
  return { kind: 'server' };
}

function errorMessage(err: LoginError): string {
  switch (err.kind) {
    case 'credentials':
      return 'Username or password is incorrect.';
    case 'network':
      return 'Unable to connect to the server. Please try again.';
    case 'validation':
      return 'Please enter a valid username and password.';
    case 'server':
      return 'An unexpected error occurred. Please try again later.';
  }
}

const LoginPage = () => {
  const { login } = useAuth();
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();

  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [error, setError] = useState<LoginError | null>(null);

  // Stable IDs for accessibility (input <-> label association)
  const usernameId = useId();
  const passwordId = useId();
  const errorId = useId();

  const redirectTo = sanitizeRedirect(searchParams.get('redirect'));

  const handleSubmit = async (e: React.FormEvent<HTMLFormElement>) => {
    e.preventDefault();
    setError(null);

    // Basic client-side presence check
    if (!username.trim() || !password.trim()) {
      setError({ kind: 'validation' });
      return;
    }

    const credentials: LoginRequest = { username: username.trim(), password };
    setIsSubmitting(true);

    try {
      await login(credentials);
      navigate(redirectTo, { replace: true });
    } catch (err: unknown) {
      setError(resolveError(err));
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <div className="login-page">
      <div className="login-card">
        {/* Brand */}
        <div className="login-brand">
          <span className="login-brand-icon" aria-hidden="true"><Sparkles size={24} /></span>
          <span className="login-brand-name">Spa Booking</span>
        </div>

        <h1 className="login-heading">Sign in to your account</h1>

        {/* Error banner */}
        {error && (
          <div
            id={errorId}
            role="alert"
            className="login-error"
            aria-live="assertive"
          >
            {errorMessage(error)}
          </div>
        )}

        <form
          onSubmit={handleSubmit}
          noValidate
          aria-describedby={error ? errorId : undefined}
          className="flex flex-col gap-5"
        >
          <Input
            id={usernameId}
            label="Username"
            type="text"
            name="username"
            autoComplete="username"
            required
            disabled={isSubmitting}
            value={username}
            onChange={(e) => setUsername(e.target.value)}
            aria-required="true"
          />

          <Input
            id={passwordId}
            label="Password"
            type="password"
            name="password"
            autoComplete="current-password"
            required
            disabled={isSubmitting}
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            aria-required="true"
          />

          <Button
            type="submit"
            disabled={isSubmitting}
            aria-busy={isSubmitting}
            className="w-full mt-2"
            size="lg"
          >
            {isSubmitting ? 'Signing in…' : 'Sign in'}
          </Button>
        </form>
      </div>
    </div>
  );
};

export default LoginPage;
