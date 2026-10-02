import { useAuth } from '../app/auth/useAuth';
import type { LoginRequest } from '../types/auth';

const LoginPage = () => {
  const { login } = useAuth();

  const handleLogin = async () => {
    const demoCredentials: LoginRequest = { username: 'owner', password: 'password' };
    await login(demoCredentials);
  };

  return (
    <div className="flex flex-col items-center justify-center min-h-screen">
      <h2 className="text-2xl mb-4">Login (demo)</h2>
      <button onClick={handleLogin} className="px-4 py-2 bg-blue-600 text-white rounded">
        Demo Login
      </button>
    </div>
  );
};

export default LoginPage;
