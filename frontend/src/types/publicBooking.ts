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
