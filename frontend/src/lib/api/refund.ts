import api from './axios';
import type { InitiateRefundRequest, RefundEligibilityResponse, RefundResponse } from '../../types/refund';

export const getRefundEligibility = async (bookingId: number): Promise<RefundEligibilityResponse> => {
  const response = await api.get<RefundEligibilityResponse>(`/bookings/${bookingId}/refund-eligibility`);
  return response.data;
};

export const initiateRefund = async (
  bookingId: number,
  data?: InitiateRefundRequest
): Promise<RefundResponse> => {
  const response = await api.post<RefundResponse>(`/bookings/${bookingId}/refund`, data || {});
  return response.data;
};

export const getRefund = async (bookingId: number): Promise<RefundResponse> => {
  const response = await api.get<RefundResponse>(`/bookings/${bookingId}/refund`);
  return response.data;
};
