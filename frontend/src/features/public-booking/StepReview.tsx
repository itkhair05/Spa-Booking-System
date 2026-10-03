import { useState } from 'react';
import { useSpaBooking } from './SpaBookingContext';
import { createPublicBooking } from '../../lib/api/publicBooking';
import { ArrowLeft, User, Calendar, AlertCircle, FileText, CheckCircle2 } from 'lucide-react';
import type { CreatePublicBookingRequest } from '../../types/publicBooking';

export function StepReview() {
  const { slug, state, setStep, updateState } = useSpaBooking();
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const { service, staff, date, time, customer } = state;

  if (!service || !staff) {
    return null;
  }

  const handleSubmit = async () => {
    setSubmitting(true);
    setError(null);

    // time can be "2026-10-10T10:00:00" or just "10:00:00" or "10:00" depending on what backend returned
    // we need to pass a LocalDateTime to the backend: YYYY-MM-DDTHH:mm:ss
    let startTimeStr = time;
    if (!startTimeStr.includes('T')) {
      const timeParts = time.split(':');
      const formattedTime = timeParts.length === 2 ? `${time}:00` : time;
      startTimeStr = `${date}T${formattedTime}`;
    }

    const payload: CreatePublicBookingRequest = {
      serviceId: service.id,
      staffId: staff.id,
      startTime: startTimeStr,
      customerName: customer.name,
      customerPhone: customer.phone,
      customerEmail: customer.email || undefined,
    };

    try {
      const response = await createPublicBooking(slug, payload);
      updateState({ bookingId: response.id });
      setStep(6);
    } catch (err: unknown) {
      const e = err as { response?: { status: number } };
      if (e.response?.status === 409) {
        setError('Khung giờ này vừa được đặt. Vui lòng quay lại và chọn khung giờ khác.');
      } else if (e.response?.status === 400) {
        setError('Thông tin đặt lịch không hợp lệ. Vui lòng kiểm tra lại.');
      } else {
        setError('Đã xảy ra lỗi khi đặt lịch. Vui lòng thử lại.');
      }
      setSubmitting(false);
    }
  };

  const formattedDate = new Date(date).toLocaleDateString('vi-VN', {
    weekday: 'long',
    year: 'numeric',
    month: 'long',
    day: 'numeric',
  });

  // Extract HH:mm safely
  const timeString = time.includes('T') ? time.split('T')[1].substring(0, 5) : time.substring(0, 5);

  return (
    <div>
      <div className="flex items-center mb-6">
        <button
          onClick={() => !submitting && setStep(4)}
          disabled={submitting}
          className="mr-3 p-2 -ml-2 rounded-full text-stone-400 hover:text-stone-700 hover:bg-stone-100 transition-colors disabled:opacity-50"
          aria-label="Quay lại"
        >
          <ArrowLeft className="w-5 h-5" />
        </button>
        <h2 className="text-xl sm:text-2xl font-medium text-stone-900">Xác nhận đặt lịch</h2>
      </div>

      {error && (
        <div className="mb-6 p-4 rounded-xl bg-rose-50 border border-rose-100 flex items-start text-rose-700">
          <AlertCircle className="w-5 h-5 mr-3 shrink-0 mt-0.5" />
          <p>{error}</p>
        </div>
      )}

      <div className="space-y-6">
        {/* Booking Details Card */}
        <div className="bg-stone-50 rounded-2xl p-5 sm:p-6 border border-stone-100">
          <h3 className="text-sm font-semibold text-stone-400 uppercase tracking-wider mb-4">Chi tiết lịch hẹn</h3>
          
          <div className="space-y-4">
            <div className="flex justify-between items-start">
              <div className="flex items-start gap-3">
                <FileText className="w-5 h-5 text-stone-400 mt-0.5" />
                <div>
                  <p className="font-medium text-stone-900">{service.name}</p>
                  <p className="text-sm text-stone-500">{service.durationMinutes} phút</p>
                </div>
              </div>
              <span className="font-medium text-stone-900 shrink-0">
                {new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(service.price)}
              </span>
            </div>

            <div className="flex items-start gap-3">
              <User className="w-5 h-5 text-stone-400 mt-0.5" />
              <div>
                <p className="font-medium text-stone-900">Nhân viên: {staff.name}</p>
              </div>
            </div>

            <div className="flex items-start gap-3">
              <Calendar className="w-5 h-5 text-stone-400 mt-0.5" />
              <div>
                <p className="font-medium text-stone-900">{timeString}</p>
                <p className="text-sm text-stone-500 capitalize">{formattedDate}</p>
              </div>
            </div>
          </div>
        </div>

        {/* Customer Details Card */}
        <div className="bg-stone-50 rounded-2xl p-5 sm:p-6 border border-stone-100">
          <h3 className="text-sm font-semibold text-stone-400 uppercase tracking-wider mb-4">Thông tin của bạn</h3>
          <div className="space-y-2 text-stone-900">
            <p><span className="text-stone-500 w-24 inline-block">Họ và tên:</span> <span className="font-medium">{customer.name}</span></p>
            <p><span className="text-stone-500 w-24 inline-block">Điện thoại:</span> <span className="font-medium">{customer.phone}</span></p>
            {customer.email && (
              <p><span className="text-stone-500 w-24 inline-block">Email:</span> <span>{customer.email}</span></p>
            )}
          </div>
        </div>

        <div className="pt-4 flex flex-col sm:flex-row gap-4 items-center">
          <button
            onClick={handleSubmit}
            disabled={submitting}
            className="w-full sm:w-auto px-8 py-3.5 bg-stone-900 text-white font-medium rounded-xl hover:bg-stone-800 transition-colors disabled:opacity-50 disabled:cursor-not-allowed flex items-center justify-center gap-2"
          >
            {submitting ? (
              <>
                <svg className="animate-spin -ml-1 mr-2 h-5 w-5 text-white" fill="none" viewBox="0 0 24 24">
                  <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4" />
                  <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z" />
                </svg>
                Đang xử lý...
              </>
            ) : (
              <>
                <CheckCircle2 className="w-5 h-5" />
                Xác nhận đặt lịch
              </>
            )}
          </button>
        </div>
      </div>
    </div>
  );
}
