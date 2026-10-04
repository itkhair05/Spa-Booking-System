import api from './axios';
import type { ChangePasswordRequest } from '../../types/auth';

export const changePassword = async (data: ChangePasswordRequest): Promise<void> => {
  await api.post('/auth/change-password', data);
};
