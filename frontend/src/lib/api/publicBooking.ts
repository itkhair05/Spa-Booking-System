import { publicAxios } from './publicAxios';
import type {
  PublicSpaInfoResponse,
  PublicServiceResponse,
  PublicStaffResponse,
  CreatePublicBookingRequest,
  PublicBookingResponse,
} from '../../types/publicBooking';

export const getPublicSpaInfo = async (slug: string): Promise<PublicSpaInfoResponse> => {
  const { data } = await publicAxios.get<PublicSpaInfoResponse>(`/public/spas/${slug}`);
  return data;
};

export const getPublicServices = async (slug: string): Promise<PublicServiceResponse[]> => {
  const { data } = await publicAxios.get<PublicServiceResponse[]>(`/public/spas/${slug}/services`);
  return data;
};

export const getPublicStaff = async (slug: string): Promise<PublicStaffResponse[]> => {
  const { data } = await publicAxios.get<PublicStaffResponse[]>(`/public/spas/${slug}/staff`);
  return data;
};

export const getPublicAvailability = async (
  slug: string,
  serviceId: number,
  date: string,
  staffId?: number
): Promise<string[]> => {
  const { data } = await publicAxios.get<string[]>(`/public/spas/${slug}/availability`, {
    params: {
      serviceId,
      date,
      ...(staffId ? { staffId } : {}),
    },
  });
  return data;
};

export const createPublicBooking = async (
  slug: string,
  request: CreatePublicBookingRequest
): Promise<PublicBookingResponse> => {
  const { data } = await publicAxios.post<PublicBookingResponse>(
    `/public/spas/${slug}/bookings`,
    request
  );
  return data;
};
