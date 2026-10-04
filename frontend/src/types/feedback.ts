export type FeedbackType = 'SUGGESTION' | 'COMPLAINT' | 'PRAISE' | 'OTHER';

export type FeedbackStatus = 'NEW' | 'IN_REVIEW' | 'RESOLVED';

export interface CreateFeedbackRequest {
  name: string;
  phone: string;
  email?: string;
  type: FeedbackType;
  bookingCode?: string;
  message: string;
}

export interface FeedbackResponse {
  id: number;
  name: string;
  phone: string;
  email?: string | null;
  type: FeedbackType;
  bookingCode?: string | null;
  message: string;
  status: FeedbackStatus;
  createdAt: string;
  updatedAt: string;
}

export interface UpdateFeedbackStatusRequest {
  status: FeedbackStatus;
}
