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
}

export interface PublicBookingDetailResponse {
  bookingCode: string;
  status: string;
  serviceName: string;
  durationMinutes: number;
  price: number;
  startTime: string;
  endTime: string;
  staffName: string;
  spaName: string;
  spaPhone: string;
  spaAddress: string;
}
