import api from './axios';
import type { Customer, CreateCustomerRequest, UpdateCustomerRequest } from '../../types/customer';

export const getCustomers = async (search?: string, category?: string): Promise<Customer[]> => {
  const params: Record<string, string> = {};
  if (search && search.trim()) params.search = search.trim();
  if (category && category.trim()) params.category = category.trim();
  const response = await api.get<Customer[]>('/customers', { params });
  return response.data;
};

export const getCustomerById = async (id: number): Promise<Customer> => {
  const response = await api.get<Customer>(`/customers/${id}`);
  return response.data;
};

export const createCustomer = async (data: CreateCustomerRequest): Promise<Customer> => {
  const response = await api.post<Customer>('/customers', data);
  return response.data;
};

export const updateCustomer = async (id: number, data: UpdateCustomerRequest): Promise<Customer> => {
  const response = await api.put<Customer>(`/customers/${id}`, data);
  return response.data;
};

export const deleteCustomer = async (id: number): Promise<void> => {
  await api.delete(`/customers/${id}`);
};
