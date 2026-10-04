import { useState } from 'react';
import { useSpaBooking } from './SpaBookingContext';
import { createPublicBooking } from '../../lib/api/publicBooking';
import { formatCurrency, formatDateLongFromYMD } from '../../lib/format';
import { ArrowLeft, User, Calendar, AlertCircle, FileText, CheckCircle2, Loader2 } from 'lucide-react';
import type { CreatePublicBookingRequest } from '../../types/publicBooking';

export function StepReview() {
  const { slug, state, setStep, updateState } = useSpaBooking();
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [isConflict, setIsConflict] = useState(false);

  const { service, staff, date, time, customer } = state;

  if (!service || !staff) {
    return null;
  }

  const handleSubmit = async () => {
    setSubmitting(true);
    setError(null);
    setIsConflict(false);

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
      updateState({
        bookingId: response.id,
        bookingCode: response.bookingCode,
        bookingStatus: response.status,
      });
      setStep(6);
    } catch (err: unknown) {
      const e = err as { response?: { status: number } };
      if (e.response?.status === 409) {
        setIsConflict(true);
        setError('Khung giờ này vừa được đặt. Vui lòng chọn khung giờ khác.');
      } else if (e.response?.status === 400) {
        setError('Thông tin đặt lịch không hợp lệ. Vui lòng kiểm tra lại.');
      } else {
        setError('Đã xảy ra lỗi khi đặt lịch. Vui lòng thử lại.');
      }
      setSubmitting(false);
    }
  };

  const timeString = time.includes('T') ? time.split('T')[1].substring(0, 5) : time.substring(0, 5);

  return (
    <div>
      <div className="flex items-center mb-6">
        <button
          onClick={() => !submitting && setStep(4)}
          disabled={submitting}
          className="mr-3 p-2 -ml-2 max-sm:min-h-11 max-sm:min-w-11 rounded-full text-stone-400 hover:text-stone-700 hover:bg-stone-100 transition-colors disabled:opacity-50"
          aria-label="Quay lại"
        >
          <ArrowLeft className="w-5 h-5" aria-hidden="true" />
        </button>
        <h2 className="text-xl sm:text-2xl font-medium text-stone-900">Xác nhận đặt lịch</h2>
      </div>

      {error && (
        <div
          role="alert"
          className="mb-6 p-4 rounded-xl bg-rose-50 border border-rose-100 flex flex-wrap items-center gap-3 text-rose-700"
        >
          <span className="flex items-start">
            <AlertCircle className="w-5 h-5 mr-3 shrink-0 mt-0.5" aria-hidden="true" />
            <span>{error}</span>
          </span>
          {isConflict && (
            <button
              onClick={() => setStep(3)}
              className="ml-8 text-sm font-medium underline underline-offset-2 hover:text-rose-800 max-sm:min-h-11"
            >
              Chọn khung giờ khác
            </button>
          )}
        </div>
      )}

      <div className="space-y-6">
        {/* Booking Details Card */}
        <div className="bg-stone-50 rounded-2xl p-5 sm:p-6 border border-stone-100">
          <h3 className="text-sm font-semibold text-stone-400 uppercase tracking-wider mb-4">
            Chi tiết lịch hẹn
          </h3>

          <dl className="space-y-4">
            <div className="flex justify-between items-start gap-4">
              <dt className="flex items-start gap-3 text-stone-500">
                <FileText className="w-5 h-5 text-stone-400 mt-0.5 shrink-0" aria-hidden="true" />
                <span>
                  <span className="block font-medium text-stone-900">{service.name}</span>
                  <span className="block text-sm text-stone-500">{service.durationMinutes} phút</span>
                </span>
              </dt>
              <dd className="font-medium text-stone-900 shrink-0">{formatCurrency(service.price)}</dd>
            </div>

            <div className="flex items-start gap-3">
              <User className="w-5 h-5 text-stone-400 mt-0.5 shrink-0" aria-hidden="true" />
              <dt className="sr-only">Nhân viên</dt>
              <dd className="font-medium text-stone-900">{staff.name}</dd>
            </div>

            <div className="flex items-start gap-3">
              <Calendar className="w-5 h-5 text-stone-400 mt-0.5 shrink-0" aria-hidden="true" />
              <dt className="sr-only">Thời gian</dt>
              <dd>
                <span className="block font-medium text-stone-900">{timeString}</span>
                <span className="block text-sm text-stone-500 capitalize">{formatDateLongFromYMD(date)}</span>
              </dd>
            </div>
          </dl>
        </div>

        {/* Customer Details Card */}
        <div className="bg-stone-50 rounded-2xl p-5 sm:p-6 border border-stone-100">
          <h3 className="text-sm font-semibold text-stone-400 uppercase tracking-wider mb-4">
            Thông tin của bạn
          </h3>
          <dl className="space-y-2 text-stone-900">
            <div className="flex flex-wrap gap-x-2">
              <dt className="text-stone-500">Họ và tên:</dt>
              <dd className="font-medium">{customer.name}</dd>
            </div>
            <div className="flex flex-wrap gap-x-2">
              <dt className="text-stone-500">Điện thoại:</dt>
              <dd className="font-medium">{customer.phone}</dd>
            </div>
            {customer.email && (
              <div className="flex flex-wrap gap-x-2">
                <dt className="text-stone-500">Email:</dt>
                <dd>{customer.email}</dd>
              </div>
            )}
          </dl>
        </div>

        <div className="pt-4 flex flex-col sm:flex-row gap-4 items-center">
          <button
            onClick={handleSubmit}
            disabled={submitting}
            className="w-full sm:w-auto px-8 py-3.5 bg-stone-900 text-white font-medium rounded-xl hover:bg-stone-800 transition-colors disabled:opacity-50 disabled:cursor-not-allowed flex items-center justify-center gap-2 max-sm:min-h-11"
          >
            {submitting ? (
              <>
                <Loader2 className="w-5 h-5 animate-spin" aria-hidden="true" />
                Đang xử lý...
              </>
            ) : (
              <>
                <CheckCircle2 className="w-5 h-5" aria-hidden="true" />
                Xác nhận đặt lịch
              </>
            )}
          </button>
        </div>
      </div>
    </div>
  );
}
