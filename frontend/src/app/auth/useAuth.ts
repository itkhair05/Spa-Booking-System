import { useContext } from 'react';
import { AuthContext } from './authTypes';
import type { AuthContextProps } from './authTypes';

export type { AuthContextProps };

export const useAuth = (): AuthContextProps => {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error('useAuth must be used within an AuthProvider');
  }
  return context;
};
