export interface PublicSpaInfoResponse {
  name: string;
  slug: string;
  phone: string;
  email: string;
  address: string;
  timezone: string;
}

export interface PublicServiceResponse {
  id: number;
  name: string;
  description: string | null;
  durationMinutes: number;
  price: number;
  imageUrl?: string | null;
  categoryId?: number | null;
  categoryName?: string | null;
  isFeatured?: boolean;
  processSteps?: string | null;
}

export interface PublicCategoryResponse {
  id: number;
  name: string;
  description?: string | null;
  displayOrder: number;
}

export interface PublicArticleResponse {
  id: number;
  title: string;
  slug: string;
  category?: string | null;
  readTime?: string | null;
  excerpt?: string | null;
  content: string;
  coverImage?: string | null;
  publishedAt?: string | null;
}

export interface PublicReviewResponse {
  id: number;
  customerName: string;
  rating: number;
  comment: string;
  serviceName?: string | null;
  isDemo?: boolean;
  createdAt: string;
}

export interface PublicStaffResponse {
  id: number;
  name: string;
  avatarUrl?: string | null;
}

export interface CreatePublicBookingRequest {
  serviceId: number;
  staffId: number;
  startTime: string; // ISO String (LocalDateTime)
  customerName: string;
  customerPhone: string;
  customerEmail?: string;
  paymentMethod?: 'PAY_AT_SPA' | 'VNPAY';
}

export interface PublicBookingResponse {
  id?: number;
  bookingCode: string;
  serviceId: number;
  serviceName: string;
  staffId: number;
  staffName: string;
  startTime: string;
  endTime: string;
  status: string;
  price: number;
  paymentMethod?: string;
  paymentStatus?: string;
  paymentUrl?: string | null;
}

export interface PublicBookingDetailResponse {
  bookingCode: string;
  status: string;
  serviceName: string;
  categoryName?: string | null;
  durationMinutes: number;
  price: number;
  startTime: string;
  endTime: string;
  staffName: string;
  customerName: string;
  customerPhone: string;
  customerEmail?: string | null;
  paymentMethod: string;
  paymentStatus: string;
  paidAmount?: number | null;
  paidAt?: string | null;
  refundAmount?: number | null;
  refundStatus?: string | null;
  cancellationFee?: number | null;
  spaName: string;
  spaPhone: string;
  spaAddress: string;
  spaEmail?: string | null;
  serviceDescription?: string | null;
  processSteps?: string | null;
}

export interface VNPayCallbackResult {
  success: boolean;
  message: string;
  bookingCode?: string | null;
  txnRef?: string | null;
  amount?: number | null;
  status?: string | null;
  responseCode?: string | null;
  transactionNo?: string | null;
  paidAt?: string | null;
}
