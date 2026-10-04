import api from './axios';
import type { BusinessProfileResponse, UpdateBusinessProfileRequest } from '../../types/businessProfile';

export const getBusinessProfile = async (): Promise<BusinessProfileResponse> => {
  const response = await api.get<BusinessProfileResponse>('/business-profile');
  return response.data;
};

export const updateBusinessProfile = async (
  data: UpdateBusinessProfileRequest
): Promise<BusinessProfileResponse> => {
  const response = await api.put<BusinessProfileResponse>('/business-profile', data);
  return response.data;
};
