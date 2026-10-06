export type BookingStatus =
  | 'PENDING'
  | 'CONFIRMED'
  | 'CHECKED_IN'
  | 'IN_PROGRESS'
  | 'COMPLETED'
  | 'CANCELLED'
  | 'NO_SHOW';

export interface Booking {
  id: number;
  bookingCode?: string;
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
  confirmedAt?: string;
  checkedInAt?: string;
  startedAt?: string;
  completedAt?: string;
  cancelledAt?: string;
  noShowAt?: string;
  cancellationReason?: string;
  createdAt: string;
  updatedAt: string;
}

export interface BookingDetail {
  id: number;
  bookingCode?: string;
  status: BookingStatus;
  startTime: string;
  endTime: string;
  durationMinutes?: number;
  price: number;
  isReminded: boolean;
  confirmedAt?: string;
  checkedInAt?: string;
  startedAt?: string;
  completedAt?: string;
  cancelledAt?: string;
  noShowAt?: string;
  cancellationReason?: string;
  createdAt: string;
  updatedAt: string;

  // Customer
  customerId: number;
  customerName: string;
  customerPhone?: string;
  customerEmail?: string;

  // Service
  serviceId: number;
  serviceName: string;
  categoryName?: string;
  serviceDuration?: number;
  servicePrice?: number;
  serviceDescription?: string;
  processSteps?: string;

  // Staff
  staffId?: number;
  staffName?: string;
  staffPhone?: string;
  staffEmail?: string;

  // Payment
  paymentMethod?: string;
  paymentProvider?: string;
  paymentStatus?: string;
  paidAmount?: number;
  paidAt?: string;
  txnRef?: string;
  transactionNo?: string;
  bankCode?: string;
  cardType?: string;
}

export interface AssignBookingRequest {
  staffId: number;
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

export interface CancelBookingRequest {
  reason?: string;
}

export interface RescheduleBookingRequest {
  startTime: string;
  staffId?: number;
}
