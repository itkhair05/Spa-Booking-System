import { useState, useEffect } from 'react';
import { useSpaBooking } from './SpaBookingContext';
import { getPublicServices } from '../../lib/api/publicBooking';
import { formatCurrency } from '../../lib/format';
import type { PublicServiceResponse } from '../../types/publicBooking';
import { Clock, AlertCircle, CheckCircle2 } from 'lucide-react';

export function StepServices() {
  const { slug, state, updateState, setStep } = useSpaBooking();
  const [services, setServices] = useState<PublicServiceResponse[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    let isMounted = true;
    getPublicServices(slug)
      .then((data) => {
        if (isMounted) {
          setServices(data);
          setLoading(false);
        }
      })
      .catch(() => {
        if (isMounted) {
          setError('Không thể tải danh sách dịch vụ.');
          setLoading(false);
        }
      });
    return () => {
      isMounted = false;
    };
  }, [slug]);

  const handleSelect = (service: PublicServiceResponse) => {
    // If selecting a new service, clear subsequent dependent state
    if (state.service?.id !== service.id) {
      updateState({
        service,
        time: '',
      });
    } else {
      updateState({ service });
    }
    setStep(2);
  };

  if (loading) {
    return (
      <div>
        <h2 className="text-xl sm:text-2xl font-medium text-stone-900 mb-6">Chọn dịch vụ</h2>
        <div className="grid gap-4 sm:grid-cols-2">
          {[1, 2, 3, 4].map((i) => (
            <div key={i} className="p-5 rounded-xl border border-stone-100 animate-pulse">
              <div className="flex justify-between items-start mb-3">
                <div className="h-5 w-32 bg-stone-100 rounded" />
                <div className="h-5 w-20 bg-stone-100 rounded" />
              </div>
              <div className="h-4 w-full bg-stone-100 rounded mb-2" />
              <div className="h-4 w-24 bg-stone-100 rounded" />
            </div>
          ))}
        </div>
      </div>
    );
  }

  if (error) {
    return (
      <div className="py-8 text-center text-rose-500">
        <AlertCircle className="w-8 h-8 mx-auto mb-2" aria-hidden="true" />
        <p>{error}</p>
      </div>
    );
  }

  if (services.length === 0) {
    return (
      <div className="py-12 text-center text-stone-500">
        Hiện chưa có dịch vụ để đặt lịch.
      </div>
    );
  }

  return (
    <div>
      <h2 className="text-xl sm:text-2xl font-medium text-stone-900 mb-6">Chọn dịch vụ</h2>
      <div className="grid gap-4 sm:grid-cols-2">
        {services.map((service) => {
          const selected = state.service?.id === service.id;
          return (
            <button
              key={service.id}
              onClick={() => handleSelect(service)}
              aria-pressed={selected}
              className={`relative text-left p-5 rounded-xl border transition-all ${
                selected
                  ? 'border-stone-800 bg-stone-50 ring-1 ring-stone-800'
                  : 'border-stone-200 hover:border-stone-300 hover:bg-stone-50'
              }`}
            >
              {selected && (
                <span className="absolute -top-2 -right-2 w-6 h-6 rounded-full bg-stone-800 text-white flex items-center justify-center">
                  <CheckCircle2 className="w-4 h-4" aria-hidden="true" />
                </span>
              )}
              <div className="flex justify-between items-start mb-2">
                <h3 className="font-medium text-stone-900 pr-4">{service.name}</h3>
                <span className="font-medium text-stone-900 shrink-0">
                  {formatCurrency(service.price)}
                </span>
              </div>
              {service.description && (
                <p className="text-sm text-stone-500 mb-4 line-clamp-2">{service.description}</p>
              )}
              <div className="flex items-center text-sm text-stone-500 bg-white px-2 py-1 rounded-md border border-stone-100 w-fit">
                <Clock className="w-4 h-4 mr-1.5" aria-hidden="true" />
                {service.durationMinutes} phút
              </div>
            </button>
          );
        })}
      </div>
    </div>
  );
}
