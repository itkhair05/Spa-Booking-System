import api from './axios';
import type { ChangePasswordRequest } from '../../types/auth';

export const changePassword = async (data: ChangePasswordRequest): Promise<void> => {
  await api.post('/auth/change-password', data);
};

export const forgotPassword = async (email: string): Promise<{ message: string }> => {
  const response = await api.post<{ message: string }>('/auth/forgot-password', { email });
  return response.data;
};

export const resetPassword = async (token: string, newPassword: string): Promise<{ message: string }> => {
  const response = await api.post<{ message: string }>('/auth/reset-password', { token, newPassword });
  return response.data;
};
