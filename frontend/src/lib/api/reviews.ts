import api from './axios';
import type { Review, UpdateReviewRequest } from '../../types/review';

export const getReviews = async (): Promise<Review[]> => {
  const { data } = await api.get<Review[]>('/reviews');
  return data;
};

export const getReview = async (id: number): Promise<Review> => {
  const { data } = await api.get<Review>(`/reviews/${id}`);
  return data;
};

export const updateReview = async (
  id: number,
  request: UpdateReviewRequest
): Promise<Review> => {
  const { data } = await api.put<Review>(`/reviews/${id}`, request);
  return data;
};

export const publishReview = async (id: number): Promise<Review> => {
  const { data } = await api.patch<Review>(`/reviews/${id}/publish`);
  return data;
};

export const unpublishReview = async (id: number): Promise<Review> => {
  const { data } = await api.patch<Review>(`/reviews/${id}/unpublish`);
  return data;
};

export const deleteReview = async (id: number): Promise<void> => {
  await api.delete(`/reviews/${id}`);
};
