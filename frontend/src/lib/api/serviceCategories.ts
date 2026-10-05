import api from './axios';
import type {
  ServiceCategory,
  CreateServiceCategoryRequest,
  UpdateServiceCategoryRequest,
} from '../../types/serviceCategory';

export const getServiceCategories = async (): Promise<ServiceCategory[]> => {
  const { data } = await api.get<ServiceCategory[]>('/service-categories');
  return data;
};

export const getActiveServiceCategories = async (): Promise<ServiceCategory[]> => {
  const { data } = await api.get<ServiceCategory[]>('/service-categories/active');
  return data;
};

export const getServiceCategory = async (id: number): Promise<ServiceCategory> => {
  const { data } = await api.get<ServiceCategory>(`/service-categories/${id}`);
  return data;
};

export const createServiceCategory = async (
  request: CreateServiceCategoryRequest
): Promise<ServiceCategory> => {
  const { data } = await api.post<ServiceCategory>('/service-categories', request);
  return data;
};

export const updateServiceCategory = async (
  id: number,
  request: UpdateServiceCategoryRequest
): Promise<ServiceCategory> => {
  const { data } = await api.put<ServiceCategory>(`/service-categories/${id}`, request);
  return data;
};

export const deleteServiceCategory = async (id: number): Promise<void> => {
  await api.delete(`/service-categories/${id}`);
};
