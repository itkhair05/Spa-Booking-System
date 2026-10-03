import { useState, useEffect } from 'react';
import { useSpaBooking } from './SpaBookingContext';
import { getPublicServices } from '../../lib/api/publicBooking';
import type { PublicServiceResponse } from '../../types/publicBooking';
import { Clock, AlertCircle } from 'lucide-react';

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
    return <div className="py-12 text-center text-stone-500 animate-pulse">Đang tải dịch vụ...</div>;
  }

  if (error) {
    return (
      <div className="py-8 text-center text-rose-500">
        <AlertCircle className="w-8 h-8 mx-auto mb-2" />
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
        {services.map((service) => (
          <button
            key={service.id}
            onClick={() => handleSelect(service)}
            className={`text-left p-5 rounded-xl border transition-all ${
              state.service?.id === service.id
                ? 'border-stone-800 bg-stone-50 ring-1 ring-stone-800'
                : 'border-stone-200 hover:border-stone-300 hover:bg-stone-50'
            }`}
          >
            <div className="flex justify-between items-start mb-2">
              <h3 className="font-medium text-stone-900 pr-4">{service.name}</h3>
              <span className="font-medium text-stone-900 shrink-0">
                {new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(service.price)}
              </span>
            </div>
            {service.description && (
              <p className="text-sm text-stone-500 mb-4 line-clamp-2">{service.description}</p>
            )}
            <div className="flex items-center text-sm text-stone-500 bg-white inline-flex px-2 py-1 rounded-md border border-stone-100">
              <Clock className="w-4 h-4 mr-1.5" />
              {service.durationMinutes} phút
            </div>
          </button>
        ))}
      </div>
    </div>
  );
}
