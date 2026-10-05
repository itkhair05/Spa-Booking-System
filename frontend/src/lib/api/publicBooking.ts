import { publicAxios } from './publicAxios';
import type {
  PublicSpaInfoResponse,
  PublicServiceResponse,
  PublicStaffResponse,
  PublicCategoryResponse,
  PublicArticleResponse,
  PublicReviewResponse,
  CreatePublicBookingRequest,
  PublicBookingResponse,
  PublicBookingDetailResponse,
  VNPayCallbackResult,
} from '../../types/publicBooking';

export const getPublicSpaInfo = async (slug: string): Promise<PublicSpaInfoResponse> => {
  const { data } = await publicAxios.get<PublicSpaInfoResponse>(`/public/spas/${slug}`);
  return data;
};

export const getPublicServices = async (slug: string): Promise<PublicServiceResponse[]> => {
  const { data } = await publicAxios.get<PublicServiceResponse[]>(`/public/spas/${slug}/services`);
  return data;
};

export const getPublicFeaturedServices = async (slug: string): Promise<PublicServiceResponse[]> => {
  const { data } = await publicAxios.get<PublicServiceResponse[]>(`/public/spas/${slug}/services/featured`);
  return data;
};

export const getPublicCategories = async (slug: string): Promise<PublicCategoryResponse[]> => {
  const { data } = await publicAxios.get<PublicCategoryResponse[]>(`/public/spas/${slug}/categories`);
  return data;
};

export const getPublicStaff = async (slug: string): Promise<PublicStaffResponse[]> => {
  const { data } = await publicAxios.get<PublicStaffResponse[]>(`/public/spas/${slug}/staff`);
  return data;
};

export const getPublicArticles = async (slug: string): Promise<PublicArticleResponse[]> => {
  const { data } = await publicAxios.get<PublicArticleResponse[]>(`/public/spas/${slug}/articles`);
  return data;
};

export const getPublicArticle = async (slug: string, slugOrId: string): Promise<PublicArticleResponse> => {
  const { data } = await publicAxios.get<PublicArticleResponse>(`/public/spas/${slug}/articles/${encodeURIComponent(slugOrId)}`);
  return data;
};

export const getPublicReviews = async (slug: string): Promise<PublicReviewResponse[]> => {
  const { data } = await publicAxios.get<PublicReviewResponse[]>(`/public/spas/${slug}/reviews`);
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

export const getPublicBookingByCode = async (
  slug: string,
  bookingCode: string
): Promise<PublicBookingDetailResponse> => {
  const { data } = await publicAxios.get<PublicBookingDetailResponse>(
    `/public/spas/${slug}/bookings/${encodeURIComponent(bookingCode)}`
  );
  return data;
};

export const verifyVNPayCallback = async (
  slug: string,
  params: Record<string, string>
): Promise<VNPayCallbackResult> => {
  const { data } = await publicAxios.get<VNPayCallbackResult>(
    `/public/spas/${slug}/payments/vnpay-callback`,
    { params }
  );
  return data;
};
