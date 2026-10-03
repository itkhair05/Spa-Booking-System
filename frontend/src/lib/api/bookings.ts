import api from './axios';
import type { 
  Booking, 
  BookingFilterParams, 
  CreateBookingRequest, 
  UpdateBookingRequest, 
  UpdateBookingStatusRequest 
} from '../../types/booking';

export const getBookings = async (params?: BookingFilterParams): Promise<Booking[]> => {
  const response = await api.get<Booking[]>('/bookings', { params });
  return response.data;
};

export const getBookingById = async (id: number): Promise<Booking> => {
  const response = await api.get<Booking>(`/bookings/${id}`);
  return response.data;
};

export const createBooking = async (data: CreateBookingRequest): Promise<Booking> => {
  const response = await api.post<Booking>('/bookings', data);
  return response.data;
};

export const updateBooking = async (id: number, data: UpdateBookingRequest): Promise<Booking> => {
  const response = await api.put<Booking>(`/bookings/${id}`, data);
  return response.data;
};

export const updateBookingStatus = async (id: number, data: UpdateBookingStatusRequest): Promise<Booking> => {
  const response = await api.patch<Booking>(`/bookings/${id}/status`, data);
  return response.data;
};
