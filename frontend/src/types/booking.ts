export type BookingStatus = 'PENDING' | 'CONFIRMED' | 'COMPLETED' | 'CANCELLED';

export interface Booking {
  id: number;
  customerId: number;
  customerName: string;
  serviceId: number;
  serviceName: string;
  staffId: number;
  staffName: string;
  startTime: string;
  endTime: string;
  status: BookingStatus;
  price: number;
  isReminded: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface BookingFilterParams {
  staffId?: number;
  startDate?: string;
  endDate?: string;
  status?: BookingStatus;
}

export interface CreateBookingRequest {
  customerId: number;
  serviceId: number;
  staffId: number;
  startTime: string;
  endTime: string;
}

export interface UpdateBookingRequest {
  customerId: number;
  serviceId: number;
  staffId: number;
  startTime: string;
  endTime: string;
}

export interface UpdateBookingStatusRequest {
  status: BookingStatus;
}
