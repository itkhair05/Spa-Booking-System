import api from './axios';
import type {
  Staff,
  CreateStaffRequest,
  UpdateStaffRequest,
  CreateStaffAccountRequest,
  StaffAccountResponse,
  UpdateStaffSelfProfileRequest,
} from '../../types/staff';

export const getStaff = async (status?: string): Promise<Staff[]> => {
  const params = status && status !== 'ALL' ? { status } : undefined;
  const response = await api.get<Staff[]>('/staff', { params });
  return response.data;
};

export const getMyProfile = async (): Promise<Staff> => {
  const response = await api.get<Staff>('/staff/me');
  return response.data;
};

export const updateMyProfile = async (data: UpdateStaffSelfProfileRequest): Promise<Staff> => {
  const response = await api.put<Staff>('/staff/me', data);
  return response.data;
};

export const getStaffById = async (id: number): Promise<Staff> => {
  const response = await api.get<Staff>(`/staff/${id}`);
  return response.data;
};

export const createStaff = async (data: CreateStaffRequest): Promise<Staff> => {
  const response = await api.post<Staff>('/staff', data);
  return response.data;
};

export const updateStaff = async (id: number, data: UpdateStaffRequest): Promise<Staff> => {
  const response = await api.put<Staff>(`/staff/${id}`, data);
  return response.data;
};

export const deleteStaff = async (id: number): Promise<void> => {
  await api.delete(`/staff/${id}`);
};

export const createStaffAccount = async (id: number, data: CreateStaffAccountRequest): Promise<StaffAccountResponse> => {
  const response = await api.post<StaffAccountResponse>(`/staff/${id}/account`, data);
  return response.data;
};

export const uploadMyAvatar = async (file: File): Promise<Staff> => {
  const formData = new FormData();
  formData.append('file', file);
  const response = await api.post<Staff>('/staff/me/avatar', formData, {
    headers: { 'Content-Type': 'multipart/form-data' },
  });
  return response.data;
};

export const deleteMyAvatar = async (): Promise<Staff> => {
  const response = await api.delete<Staff>('/staff/me/avatar');
  return response.data;
};

export const uploadStaffAvatar = async (id: number, file: File): Promise<Staff> => {
  const formData = new FormData();
  formData.append('file', file);
  const response = await api.post<Staff>(`/staff/${id}/avatar`, formData, {
    headers: { 'Content-Type': 'multipart/form-data' },
  });
  return response.data;
};

export const deleteStaffAvatar = async (id: number): Promise<Staff> => {
  const response = await api.delete<Staff>(`/staff/${id}/avatar`);
  return response.data;
};
