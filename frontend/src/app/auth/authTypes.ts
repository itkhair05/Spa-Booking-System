import { createContext } from 'react';
import type { LoginRequest } from '../../types/auth';

export interface AuthContextProps {
  user: { username: string; roles: string[] } | null;
  token: string | null;
  isLoading: boolean;
  login: (data: LoginRequest) => Promise<void>;
  logout: () => void;
}

export const AuthContext = createContext<AuthContextProps | undefined>(undefined);
