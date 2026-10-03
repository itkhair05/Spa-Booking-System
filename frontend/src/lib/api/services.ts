import api from './axios';
import type { Service } from '../../types/service';

export const getServices = async (): Promise<Service[]> => {
  const response = await api.get<Service[]>('/services');
  return response.data;
};
