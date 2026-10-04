import { useState } from 'react';
import { useSpaBooking } from './SpaBookingContext';
import { formatDateLongFromYMD } from '../../lib/format';
import { CheckCircle2, Calendar, MapPin, Copy, Check, Search, RotateCcw } from 'lucide-react';

export function StepSuccess() {
  const { spa, state, handleRestart } = useSpaBooking();
  const { service, staff, date, time, customer, bookingCode, bookingId } = state;
  const [copied, setCopied] = useState(false);

  if (!service || !staff) return null;

  const displayCode = bookingCode || (bookingId ? `BK-${bookingId}` : 'BK-SPA');
  const timeString = time.includes('T') ? time.split('T')[1].substring(0, 5) : time.substring(0, 5);

  const handleCopyCode = async () => {
    try {
      await navigator.clipboard.writeText(displayCode);
      setCopied(true);
      setTimeout(() => setCopied(false), 2500);
    } catch {
      // Fallback
      setCopied(false);
    }
  };

  const handleGoToLookup = () => {
    const lookupElement = document.getElementById('tra-cuu');
    if (lookupElement) {
      lookupElement.scrollIntoView({ behavior: 'smooth' });
      const input = document.getElementById('public-lookup-input') as HTMLInputElement | null;
      if (input) {
        input.value = displayCode;
        input.focus();
        // Trigger event if needed
        input.dispatchEvent(new Event('input', { bubbles: true }));
      }
    }
  };

  return (
    <div className="text-center py-6 sm:py-8" role="status">
      <div className="w-16 h-16 bg-[#edf7f2] rounded-full flex items-center justify-center mx-auto mb-5 shadow-sm border border-[#b7e4c7]">
        <CheckCircle2 className="w-8 h-8 text-[#2d6a4f]" aria-hidden="true" />
      </div>

      <h2 className="text-2xl sm:text-3xl font-serif-title font-medium text-stone-900 mb-2">
        Đặt lịch thành công!
      </h2>
      <p className="text-stone-600 mb-1">
        Cảm ơn <span className="font-semibold text-stone-900">{customer.name}</span>. Yêu cầu đặt lịch của bạn đã được ghi nhận.
      </p>
      <div className="inline-flex items-center gap-1.5 px-3 py-1 rounded-full bg-amber-50 border border-amber-200 text-amber-800 text-xs font-medium my-3">
        <span className="w-1.5 h-1.5 rounded-full bg-amber-500 animate-pulse" />
        Trạng thái: Chờ xác nhận (PENDING)
      </div>
      <p className="text-sm text-stone-500 max-w-md mx-auto mb-6">
        TIKEY SPA sẽ kiểm tra và liên hệ số điện thoại <span className="font-medium text-stone-700">{customer.phone}</span> để xác nhận lịch hẹn của bạn.
      </p>

      {/* Prominent Booking Code Card */}
      <div className="max-w-md mx-auto bg-stone-50 rounded-2xl p-5 sm:p-6 border border-stone-200 text-left mb-6 shadow-sm">
        <div className="flex items-center justify-between gap-2 pb-4 border-b border-stone-200">
          <div>
            <span className="text-xs uppercase tracking-wider font-semibold text-stone-500">Mã tra cứu lịch hẹn</span>
            <p className="text-xl sm:text-2xl font-mono font-bold text-stone-900 tracking-tight mt-0.5 select-all">
              {displayCode}
            </p>
          </div>
          <button
            type="button"
            onClick={handleCopyCode}
            className="inline-flex items-center gap-1.5 px-3 py-2 text-xs font-medium rounded-xl bg-white border border-stone-200 text-stone-700 hover:bg-stone-100 hover:text-stone-900 transition-colors shadow-xs"
            aria-label="Sao chép mã tra cứu"
          >
            {copied ? (
              <>
                <Check className="w-3.5 h-3.5 text-emerald-600" aria-hidden="true" />
                <span className="text-emerald-700">Đã chép</span>
              </>
            ) : (
              <>
                <Copy className="w-3.5 h-3.5 text-stone-500" aria-hidden="true" />
                <span>Sao chép mã</span>
              </>
            )}
          </button>
        </div>

        <div className="pt-4 space-y-3">
          <div className="flex items-start gap-3">
            <Calendar className="w-4 h-4 text-stone-400 mt-1 shrink-0" aria-hidden="true" />
            <div>
              <p className="font-medium text-stone-900">{timeString}</p>
              <p className="text-xs text-stone-500 capitalize">{formatDateLongFromYMD(date)}</p>
            </div>
          </div>

          <div className="flex items-start gap-3">
            <MapPin className="w-4 h-4 text-stone-400 mt-1 shrink-0" aria-hidden="true" />
            <div>
              <p className="font-medium text-stone-900">{spa.name}</p>
              {spa.address && <p className="text-xs text-stone-500">{spa.address}</p>}
            </div>
          </div>

          <div className="pt-2 text-xs border-t border-stone-200/80 flex justify-between items-center text-stone-600">
            <span>Dịch vụ: <strong className="text-stone-800">{service.name}</strong></span>
            <span>KTV: <strong className="text-stone-800">{staff.name}</strong></span>
          </div>
        </div>
      </div>

      <div className="flex flex-col sm:flex-row items-center justify-center gap-3">
        <button
          type="button"
          onClick={handleGoToLookup}
          className="w-full sm:w-auto inline-flex items-center justify-center gap-2 px-5 py-2.5 bg-[#465d4c] text-white text-sm font-medium rounded-xl hover:bg-[#374a3c] transition-colors shadow-sm"
        >
          <Search className="w-4 h-4" aria-hidden="true" />
          Tra cứu lịch hẹn
        </button>

        <button
          type="button"
          onClick={handleRestart}
          className="w-full sm:w-auto inline-flex items-center justify-center gap-2 px-5 py-2.5 bg-white border border-stone-200 text-stone-700 text-sm font-medium rounded-xl hover:bg-stone-50 hover:text-stone-900 transition-colors"
        >
          <RotateCcw className="w-4 h-4" aria-hidden="true" />
          Đặt lịch khác
        </button>
      </div>
    </div>
  );
}
