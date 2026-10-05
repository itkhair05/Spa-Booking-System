export interface Customer {
  id: number;
  name: string;
  phone: string | null;
  email: string | null;
  lastVisit: string | null;
  totalBookings?: number;
  lastBookingAt?: string | null;
  isActive: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface CreateCustomerRequest {
  name: string;
  phone?: string;
  email?: string;
}

export interface UpdateCustomerRequest {
  name: string;
  phone?: string;
  email?: string;
}
