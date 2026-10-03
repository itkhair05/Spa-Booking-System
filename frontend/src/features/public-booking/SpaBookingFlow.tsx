import { useState } from 'react';
import { SpaBookingContext, type BookingState } from './SpaBookingContext';
import { StepServices } from './StepServices';
import { StepStaff } from './StepStaff';
import { StepDateTime } from './StepDateTime';
import { StepCustomer } from './StepCustomer';
import { StepReview } from './StepReview';
import { StepSuccess } from './StepSuccess';
import type { PublicSpaInfoResponse } from '../../types/publicBooking';

interface SpaBookingFlowProps {
  slug: string;
  spa: PublicSpaInfoResponse;
}

const INITIAL_STATE: BookingState = {
  step: 1,
  service: null,
  staff: null,
  date: '',
  time: '',
  customer: {
    name: '',
    phone: '',
    email: '',
  },
  bookingId: null,
};

export function SpaBookingFlow({ slug, spa }: SpaBookingFlowProps) {
  const [state, setState] = useState<BookingState>(INITIAL_STATE);

  const setStep = (step: number) => setState((prev) => ({ ...prev, step }));
  
  const updateState = (updates: Partial<BookingState>) => {
    setState((prev) => ({ ...prev, ...updates }));
  };

  const handleRestart = () => {
    setState(INITIAL_STATE);
  };

  const contextValue = {
    slug,
    spa,
    state,
    setStep,
    updateState,
    handleRestart,
  };

  return (
    <SpaBookingContext.Provider value={contextValue}>
      <div className="bg-white rounded-2xl shadow-sm border border-stone-100 overflow-hidden">
        {state.step < 6 && (
          <div className="bg-stone-50 border-b border-stone-100 px-6 py-4 flex items-center justify-between">
            <div className="flex gap-2">
              {[1, 2, 3, 4, 5].map((i) => (
                <div
                  key={i}
                  className={`h-1.5 w-8 rounded-full transition-colors ${
                    i <= state.step ? 'bg-stone-800' : 'bg-stone-200'
                  }`}
                />
              ))}
            </div>
            <span className="text-sm font-medium text-stone-500">Bước {state.step} / 5</span>
          </div>
        )}
        
        <div className="p-6 sm:p-8">
          {state.step === 1 && <StepServices />}
          {state.step === 2 && <StepStaff />}
          {state.step === 3 && <StepDateTime />}
          {state.step === 4 && <StepCustomer />}
          {state.step === 5 && <StepReview />}
          {state.step === 6 && <StepSuccess />}
        </div>
      </div>
    </SpaBookingContext.Provider>
  );
}
