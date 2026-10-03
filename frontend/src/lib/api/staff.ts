import api from './axios';
import type { Staff, CreateStaffRequest, UpdateStaffRequest } from '../../types/staff';

export const getStaff = async (): Promise<Staff[]> => {
  const response = await api.get<Staff[]>('/staff');
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
