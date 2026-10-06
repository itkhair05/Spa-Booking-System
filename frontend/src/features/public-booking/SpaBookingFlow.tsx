import { useEffect, useRef, useState, useCallback } from 'react';
import { useSearchParams } from 'react-router-dom';
import { SpaBookingContext, type BookingState } from './SpaBookingContext';
import { StepServices } from './StepServices';
import { StepStaff } from './StepStaff';
import { StepDateTime } from './StepDateTime';
import { StepCustomer } from './StepCustomer';
import { StepReview } from './StepReview';
import { StepSuccess } from './StepSuccess';
import { verifyVNPayCallback, getPublicBookingByCode, getPublicServices } from '../../lib/api/publicBooking';
import type { PublicSpaInfoResponse, PublicServiceResponse } from '../../types/publicBooking';

interface SpaBookingFlowProps {
  slug: string;
  spa: PublicSpaInfoResponse;
}

const STEP_LABELS = ['Chọn dịch vụ', 'Chọn nhân viên', 'Chọn ngày & giờ', 'Thông tin của bạn', 'Xác nhận'];

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
  bookingCode: null,
  bookingStatus: null,
  paymentMethod: 'PAY_AT_SPA',
  paymentStatus: null,
  paymentUrl: null,
  paidAmount: null,
  paidAt: null,
  vnpayMessage: null,
};

export function SpaBookingFlow({ slug, spa }: SpaBookingFlowProps) {
  const [state, setState] = useState<BookingState>(INITIAL_STATE);
  const panelRef = useRef<HTMLDivElement>(null);
  const prevStepRef = useRef(state.step);

  const setStep = (step: number) => setState((prev) => ({ ...prev, step }));

  const updateState = (updates: Partial<BookingState>) => {
    setState((prev) => ({ ...prev, ...updates }));
  };

  const handleRestart = () => {
    setState(INITIAL_STATE);
  };

  useEffect(() => {
    // Check for VNPay redirect return parameters
    const urlParams = new URLSearchParams(window.location.search);
    if (urlParams.has('vnp_ResponseCode') && urlParams.has('vnp_TxnRef')) {
      const params: Record<string, string> = {};
      urlParams.forEach((val, key) => {
        params[key] = val;
      });

      verifyVNPayCallback(slug, params)
        .then(async (result) => {
          if (result.bookingCode) {
            try {
              const detail = await getPublicBookingByCode(slug, result.bookingCode);
              setState({
                step: 6,
                service: {
                  id: 0,
                  name: detail.serviceName,
                  description: detail.serviceDescription || null,
                  durationMinutes: detail.durationMinutes,
                  price: detail.price,
                },
                staff: {
                  id: 0,
                  name: detail.staffName,
                },
                date: detail.startTime ? detail.startTime.split('T')[0] : '',
                time: detail.startTime || '',
                customer: {
                  name: detail.customerName,
                  phone: detail.customerPhone,
                  email: detail.customerEmail || '',
                },
                bookingId: null,
                bookingCode: detail.bookingCode,
                bookingStatus: detail.status,
                paymentMethod: 'VNPAY',
                paymentStatus: result.status || detail.paymentStatus,
                paymentUrl: null,
                paidAmount: result.amount || detail.paidAmount || detail.price,
                paidAt: result.paidAt || detail.paidAt || null,
                vnpayMessage: result.message,
              });
            } catch {
              setState((prev) => ({
                ...prev,
                step: 6,
                bookingCode: result.bookingCode || null,
                paymentMethod: 'VNPAY',
                paymentStatus: result.status,
                vnpayMessage: result.message,
              }));
            }

            const nextUrl = new URL(window.location.href);
            const toDelete: string[] = [];
            nextUrl.searchParams.forEach((_, k) => {
              if (k.startsWith('vnp_') || k === 'serviceId' || k === 'lookupCode') {
                toDelete.push(k);
              }
            });
            toDelete.forEach((k) => nextUrl.searchParams.delete(k));
            nextUrl.searchParams.set('code', result.bookingCode);
            nextUrl.hash = 'tra-cuu';
            window.history.replaceState({}, document.title, nextUrl.pathname + nextUrl.search + nextUrl.hash);
            window.dispatchEvent(
              new CustomEvent('public-booking-lookup', {
                detail: { code: result.bookingCode },
              })
            );
          } else {
            window.history.replaceState({}, document.title, window.location.pathname);
          }
        })
        .catch(() => {
          window.history.replaceState({}, document.title, window.location.pathname);
        });
    }
  }, [slug]);

  const [searchParams] = useSearchParams();
  const serviceIdParam = searchParams.get('serviceId');

  const preselectServiceById = useCallback(async (serviceIdStr: string) => {
    const targetId = Number(serviceIdStr);
    if (isNaN(targetId) || targetId <= 0) return;

    try {
      const services = await getPublicServices(slug);
      const matched = services.find((s: PublicServiceResponse) => s.id === targetId);
      if (matched) {
        setState({
          ...INITIAL_STATE,
          service: matched,
          step: 2, // Skip step 1 and advance directly to step 2
        });
      } else {
        // Fallback safely to step 1
        setState(INITIAL_STATE);
      }
    } catch {
      // If error occurs, fallback safely to step 1
      setState(INITIAL_STATE);
    }
  }, [slug]);

  // Handle serviceId from query params
  useEffect(() => {
    if (serviceIdParam) {
      // Ensure code is removed if serviceId is provided
      const currentUrl = new URL(window.location.href);
      if (currentUrl.searchParams.has('code') || currentUrl.searchParams.has('lookupCode')) {
        currentUrl.searchParams.delete('code');
        currentUrl.searchParams.delete('lookupCode');
        window.history.replaceState({}, '', currentUrl.pathname + currentUrl.search + currentUrl.hash);
      }
      // eslint-disable-next-line react-hooks/set-state-in-effect
      preselectServiceById(serviceIdParam);
    }
  }, [serviceIdParam, preselectServiceById]);

  // Listen for reset-booking-session event to clear booking wizard
  useEffect(() => {
    const handleReset = () => {
      setState(INITIAL_STATE);
    };
    window.addEventListener('reset-booking-session', handleReset);
    return () => {
      window.removeEventListener('reset-booking-session', handleReset);
    };
  }, []);

  // Handle browser back/forward buttons
  useEffect(() => {
    const handlePopState = () => {
      const currentParams = new URLSearchParams(window.location.search);
      const currentHash = window.location.hash;
      if (currentHash === '#tra-cuu') {
        setState(INITIAL_STATE);
      } else if (currentHash === '#booking') {
        if (!currentParams.has('serviceId')) {
          setState((prev) => (prev.bookingCode || prev.step === 6 ? INITIAL_STATE : prev));
        }
      }
    };
    window.addEventListener('popstate', handlePopState);
    return () => {
      window.removeEventListener('popstate', handlePopState);
    };
  }, []);

  // Handle in-page direct booking event from service cards without reload
  useEffect(() => {
    const handleSelectServiceEvent = (e: CustomEvent<{ serviceId: number }>) => {
      if (e.detail?.serviceId) {
        preselectServiceById(String(e.detail.serviceId));
      }
    };

    window.addEventListener('select-service-booking', handleSelectServiceEvent as EventListener);
    return () => {
      window.removeEventListener('select-service-booking', handleSelectServiceEvent as EventListener);
    };
  }, [preselectServiceById]);

  useEffect(() => {
    // Only focus the panel when user actively advances to a new step, preventing initial page scroll
    if (prevStepRef.current !== state.step) {
      prevStepRef.current = state.step;
      panelRef.current?.focus({ preventScroll: true });
    }
  }, [state.step]);

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
            <div className="flex gap-2" aria-hidden="true">
              {[1, 2, 3, 4, 5].map((i) => (
                <div
                  key={i}
                  className={`h-1.5 w-8 rounded-full transition-colors ${
                    i <= state.step ? 'bg-stone-800' : 'bg-stone-200'
                  }`}
                />
              ))}
            </div>
            <span className="text-sm font-medium text-stone-500">
              Bước {state.step} / 5 &middot; {STEP_LABELS[state.step - 1]}
            </span>
          </div>
        )}

        <div
          ref={panelRef}
          tabIndex={-1}
          className="p-6 sm:p-8 focus:outline-none"
        >
          <div key={state.step} className="step-enter">
            {state.step === 1 && <StepServices />}
            {state.step === 2 && <StepStaff />}
            {state.step === 3 && <StepDateTime />}
            {state.step === 4 && <StepCustomer />}
            {state.step === 5 && <StepReview />}
            {state.step === 6 && <StepSuccess />}
          </div>
        </div>
      </div>
    </SpaBookingContext.Provider>
  );
}
