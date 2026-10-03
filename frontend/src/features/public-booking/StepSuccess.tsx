import { useSpaBooking } from './SpaBookingContext';
import { CheckCircle2, Calendar, MapPin } from 'lucide-react';

export function StepSuccess() {
  const { spa, state, handleRestart } = useSpaBooking();
  const { service, staff, date, time, customer } = state;

  if (!service || !staff) return null;

  const formattedDate = new Date(date).toLocaleDateString('vi-VN', {
    weekday: 'long',
    year: 'numeric',
    month: 'long',
    day: 'numeric',
  });

  const timeString = time.includes('T') ? time.split('T')[1].substring(0, 5) : time.substring(0, 5);

  return (
    <div className="text-center py-8">
      <div className="w-16 h-16 bg-emerald-100 rounded-full flex items-center justify-center mx-auto mb-6">
        <CheckCircle2 className="w-8 h-8 text-emerald-600" />
      </div>
      
      <h2 className="text-2xl sm:text-3xl font-medium text-stone-900 mb-2">
        Đặt lịch thành công!
      </h2>
      <p className="text-stone-500 mb-8">
        Cảm ơn {customer.name}. Lịch hẹn của bạn đã được xác nhận.
      </p>

      <div className="max-w-sm mx-auto bg-stone-50 rounded-2xl p-6 border border-stone-100 text-left mb-8 space-y-4">
        <div className="flex items-start gap-3">
          <Calendar className="w-5 h-5 text-stone-400 mt-0.5" />
          <div>
            <p className="font-medium text-stone-900">{timeString}</p>
            <p className="text-sm text-stone-500 capitalize">{formattedDate}</p>
          </div>
        </div>

        <div className="flex items-start gap-3">
          <MapPin className="w-5 h-5 text-stone-400 mt-0.5" />
          <div>
            <p className="font-medium text-stone-900">{spa.name}</p>
            {spa.address && <p className="text-sm text-stone-500">{spa.address}</p>}
          </div>
        </div>

        <div className="pt-4 border-t border-stone-200">
          <p className="text-sm font-medium text-stone-900 mb-1">{service.name}</p>
          <p className="text-sm text-stone-500">với {staff.name}</p>
        </div>
      </div>

      <button
        onClick={handleRestart}
        className="px-6 py-2.5 bg-white border border-stone-200 text-stone-700 font-medium rounded-xl hover:bg-stone-50 hover:text-stone-900 transition-colors"
      >
        Đặt lịch khác
      </button>
    </div>
  );
}
