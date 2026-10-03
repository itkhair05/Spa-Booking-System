import api from './axios';
import type { Customer } from '../../types/customer';

export const getCustomers = async (): Promise<Customer[]> => {
  const response = await api.get<Customer[]>('/customers');
  return response.data;
};
