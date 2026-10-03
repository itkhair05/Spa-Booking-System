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
}

export interface PublicStaffResponse {
  id: number;
  name: string;
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
  id: number;
  serviceId: number;
  serviceName: string;
  staffId: number;
  staffName: string;
  startTime: string;
  endTime: string;
  status: string;
  price: number;
}
