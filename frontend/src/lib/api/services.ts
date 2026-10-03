import api from './axios';
import type { Service, CreateServiceRequest, UpdateServiceRequest } from '../../types/service';

export const getServices = async (): Promise<Service[]> => {
  const response = await api.get<Service[]>('/services');
  return response.data;
};

export const getServiceById = async (id: number): Promise<Service> => {
  const response = await api.get<Service>(`/services/${id}`);
  return response.data;
};

export const createService = async (data: CreateServiceRequest): Promise<Service> => {
  const response = await api.post<Service>('/services', data);
  return response.data;
};

export const updateService = async (id: number, data: UpdateServiceRequest): Promise<Service> => {
  const response = await api.put<Service>(`/services/${id}`, data);
  return response.data;
};

export const deleteService = async (id: number): Promise<void> => {
  await api.delete(`/services/${id}`);
};
