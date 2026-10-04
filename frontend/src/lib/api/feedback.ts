import api from './axios';
import { publicAxios } from './publicAxios';
import type { CreateFeedbackRequest, FeedbackResponse, FeedbackStatus } from '../../types/feedback';

export const submitPublicFeedback = async (
  slug: string,
  data: CreateFeedbackRequest
): Promise<FeedbackResponse> => {
  const response = await publicAxios.post<FeedbackResponse>(`/public/spas/${slug}/feedback`, data);
  return response.data;
};

export const getFeedback = async (): Promise<FeedbackResponse[]> => {
  const response = await api.get<FeedbackResponse[]>('/feedback');
  return response.data;
};

export const updateFeedbackStatus = async (
  id: number,
  status: FeedbackStatus
): Promise<FeedbackResponse> => {
  const response = await api.patch<FeedbackResponse>(`/feedback/${id}/status`, { status });
  return response.data;
};
