import { createContext, useContext } from 'react';
import type { PublicSpaInfoResponse, PublicServiceResponse, PublicStaffResponse } from '../../types/publicBooking';

export interface BookingState {
  step: number;
  service: PublicServiceResponse | null;
  staff: PublicStaffResponse | null;
  date: string; // YYYY-MM-DD
  time: string; // HH:mm
  customer: {
    name: string;
    phone: string;
    email: string;
  };
  bookingId: number | null; // set after success
  bookingCode: string | null;
  bookingStatus?: string | null;
}

export interface SpaBookingContextType {
  slug: string;
  spa: PublicSpaInfoResponse;
  state: BookingState;
  setStep: (step: number) => void;
  updateState: (updates: Partial<BookingState>) => void;
  handleRestart: () => void;
}

export const SpaBookingContext = createContext<SpaBookingContextType | undefined>(undefined);

export function useSpaBooking() {
  const context = useContext(SpaBookingContext);
  if (!context) {
    throw new Error('useSpaBooking must be used within SpaBookingFlow');
  }
  return context;
}
