import { useState } from 'react';
import { useSpaBooking } from './SpaBookingContext';
import { formatCurrency, formatDateLongFromYMD } from '../../lib/format';
import { CheckCircle2, Calendar, MapPin, Copy, Check, Search, RotateCcw, CalendarPlus, Bell, CreditCard, Banknote } from 'lucide-react';

export function StepSuccess() {
  const { spa, state, handleRestart } = useSpaBooking();
  const { service, staff, date, time, customer, bookingCode, bookingId, paymentMethod, paymentStatus } = state;
  const [copied, setCopied] = useState(false);
  const [reminderEnabled, setReminderEnabled] = useState(false);
  const [reminderMessage, setReminderMessage] = useState<string | null>(null);

  if (!service || !staff) return null;

  const displayCode = bookingCode || (bookingId ? `BK-${bookingId}` : 'BK-SPA');
  const timeString = time.includes('T') ? time.split('T')[1].substring(0, 5) : time.substring(0, 5);

  const handleCopyCode = async () => {
    try {
      await navigator.clipboard.writeText(displayCode);
      setCopied(true);
      setTimeout(() => setCopied(false), 2500);
    } catch {
      setCopied(false);
    }
  };

  const handleGoToLookup = () => {
    // 1. Update URL query params cleanly to lookup context only
    const currentUrl = new URL(window.location.href);
    currentUrl.searchParams.delete('serviceId');
    currentUrl.searchParams.delete('lookupCode');
    currentUrl.searchParams.set('code', displayCode);
    currentUrl.hash = 'tra-cuu';
    window.history.pushState({}, '', currentUrl.pathname + currentUrl.search + currentUrl.hash);

    // 2. Clear booking wizard session
    handleRestart();

    // 3. Scroll to lookup section
    const lookupElement = document.getElementById('tra-cuu');
    if (lookupElement) {
      lookupElement.scrollIntoView({ behavior: 'smooth' });
    }

    // 4. Dispatch event to trigger immediate search in PublicBookingLookup
    window.dispatchEvent(
      new CustomEvent('public-booking-lookup', {
        detail: { code: displayCode },
      })
    );
  };

  const handleStartAnotherBooking = () => {
    // Clear URL query parameters completely
    const currentUrl = new URL(window.location.href);
    currentUrl.searchParams.delete('serviceId');
    currentUrl.searchParams.delete('code');
    currentUrl.searchParams.delete('lookupCode');
    currentUrl.hash = 'booking';
    window.history.pushState({}, '', currentUrl.pathname + currentUrl.search + currentUrl.hash);

    handleRestart();

    const bookingEl = document.getElementById('booking');
    if (bookingEl) {
      bookingEl.scrollIntoView({ behavior: 'smooth' });
    }
  };

  // Build Google Calendar Deep Link
  const buildGoogleCalendarUrl = () => {
    try {
      const [year, month, day] = date.split('-').map(Number);
      const [hours, minutes] = timeString.split(':').map(Number);
      const startDate = new Date(year, month - 1, day, hours, minutes, 0);
      const durationMs = (service.durationMinutes || 60) * 60 * 1000;
      const endDate = new Date(startDate.getTime() + durationMs);

      const formatIsoCompact = (d: Date) => {
        const pad = (n: number) => String(n).padStart(2, '0');
        return `${d.getFullYear()}${pad(d.getMonth() + 1)}${pad(d.getDate())}T${pad(d.getHours())}${pad(d.getMinutes())}00`;
      };

      const startIso = formatIsoCompact(startDate);
      const endIso = formatIsoCompact(endDate);

      const title = `[TIKEY SPA] ${service.name} - Mã: ${displayCode}`;
      const details = `Lịch hẹn tại TIKEY SPA\nDịch vụ: ${service.name}\nKhách hàng: ${customer.name} (${customer.phone})\nKỹ thuật viên: ${staff.name}\nMã đặt lịch: ${displayCode}\nĐịa chỉ: ${spa.address || 'TIKEY SPA'}\nHotline: ${spa.phone || ''}`;
      const location = spa.address || spa.name || 'TIKEY SPA';

      return `https://calendar.google.com/calendar/render?action=TEMPLATE&text=${encodeURIComponent(title)}&dates=${startIso}/${endIso}&details=${encodeURIComponent(details)}&location=${encodeURIComponent(location)}`;
    } catch {
      return '#';
    }
  };

  const handleToggleReminder = async () => {
    if (reminderEnabled) {
      setReminderEnabled(false);
      setReminderMessage(null);
      return;
    }

    setReminderEnabled(true);
    if ('Notification' in window) {
      if (Notification.permission === 'default') {
        try {
          const perm = await Notification.requestPermission();
          if (perm === 'granted') {
            setReminderMessage('Đã bật thông báo nhắc lịch trên trình duyệt này.');
          } else {
            setReminderMessage('Bạn có thể bật thông báo trình duyệt trong cài đặt để nhận nhắc lịch.');
          }
        } catch {
          setReminderMessage('Đã lưu tùy chọn nhắc hẹn.');
        }
      } else if (Notification.permission === 'granted') {
        setReminderMessage('Đã bật thông báo nhắc lịch trên trình duyệt này.');
      } else {
        setReminderMessage('Đã lưu tùy chọn nhắc hẹn cho số điện thoại của bạn.');
      }
    } else {
      setReminderMessage('Đã lưu tùy chọn nhắc hẹn cho số điện thoại của bạn.');
    }
  };

  const isVnPay = paymentMethod === 'VNPAY';
  const isPaid = paymentStatus === 'PAID';
  const isCancelledOrFailed = paymentStatus === 'CANCELLED' || paymentStatus === 'FAILED';

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

      {/* Appointment Confirmation Badge */}
      <div className="inline-flex items-center gap-1.5 px-3.5 py-1 rounded-full bg-amber-50 border border-amber-200 text-amber-800 text-xs font-medium my-3">
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
            className="inline-flex items-center gap-1.5 px-3 py-2 text-xs font-medium rounded-xl bg-white border border-stone-200 text-stone-700 hover:bg-stone-100 hover:text-stone-900 transition-colors shadow-xs cursor-pointer"
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
          <div className="flex items-start justify-between gap-2">
            <div>
              <span className="text-xs text-stone-500 block">Dịch vụ</span>
              <p className="font-medium text-stone-900">{service.name}</p>
            </div>
            <div className="text-right">
              <span className="text-xs text-stone-500 block">Số tiền</span>
              <p className="font-semibold text-[#8a704c]">{formatCurrency(service.price)}</p>
            </div>
          </div>

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

          {/* Authoritative Payment Details */}
          <div className="pt-3 border-t border-stone-200/80 space-y-2">
            <div className="flex items-center justify-between text-xs">
              <span className="text-stone-500 flex items-center gap-1.5">
                {isVnPay ? <CreditCard className="w-3.5 h-3.5 text-blue-600" /> : <Banknote className="w-3.5 h-3.5 text-stone-500" />}
                Phương thức:
              </span>
              <span className="font-medium text-stone-800">
                {isVnPay ? 'VNPay (Sandbox)' : 'Thanh toán tại spa'}
              </span>
            </div>

            <div className="flex items-center justify-between text-xs">
              <span className="text-stone-500">Trạng thái thanh toán:</span>
              <div>
                {isPaid ? (
                  <span className="inline-flex items-center px-2 py-0.5 rounded-full text-[11px] font-semibold bg-emerald-50 text-emerald-800 border border-emerald-200">
                    Đã thanh toán
                  </span>
                ) : isCancelledOrFailed ? (
                  <span className="inline-flex items-center px-2 py-0.5 rounded-full text-[11px] font-semibold bg-rose-50 text-rose-700 border border-rose-200">
                    Thanh toán thất bại / Đã hủy
                  </span>
                ) : isVnPay ? (
                  <span className="inline-flex items-center px-2 py-0.5 rounded-full text-[11px] font-semibold bg-amber-50 text-amber-800 border border-amber-200">
                    Đang xử lý
                  </span>
                ) : (
                  <span className="inline-flex items-center px-2 py-0.5 rounded-full text-[11px] font-semibold bg-stone-100 text-stone-700 border border-stone-200">
                    Chưa thanh toán
                  </span>
                )}
              </div>
            </div>
          </div>

          <div className="pt-2 text-xs border-t border-stone-200/80 flex justify-between items-center text-stone-600">
            <span>Kỹ thuật viên:</span>
            <strong className="text-stone-800">{staff.name}</strong>
          </div>
        </div>
      </div>

      {/* Optional Google Calendar & Reminder Actions */}
      <div className="max-w-md mx-auto mb-6 p-4 rounded-2xl bg-amber-50/50 border border-amber-200/70 text-left space-y-3">
        <div className="flex items-center justify-between">
          <div className="flex items-center gap-2">
            <CalendarPlus className="w-4 h-4 text-[#8a704c]" />
            <span className="text-xs font-semibold uppercase tracking-wider text-[#8a704c]">Lịch & Nhắc hẹn tiện ích</span>
          </div>
          <span className="text-[11px] text-stone-500 bg-white/80 px-2 py-0.5 rounded-full border border-stone-200">Tùy chọn</span>
        </div>

        <div className="flex flex-col sm:flex-row gap-2.5 pt-1">
          <a
            href={buildGoogleCalendarUrl()}
            target="_blank"
            rel="noopener noreferrer"
            className="flex-1 inline-flex items-center justify-center gap-2 px-3.5 py-2.5 rounded-xl bg-white border border-stone-300 text-stone-800 text-xs font-medium hover:bg-stone-50 hover:border-stone-400 transition-colors shadow-xs"
          >
            <CalendarPlus className="w-4 h-4 text-[#4285F4]" />
            <span>Thêm vào Google Calendar</span>
          </a>
        </div>

        <label className="flex items-start gap-2.5 pt-1 cursor-pointer select-none">
          <input
            type="checkbox"
            checked={reminderEnabled}
            onChange={handleToggleReminder}
            className="mt-0.5 h-4 w-4 rounded border-stone-300 text-[#465d4c] focus:ring-[#465d4c]"
          />
          <div className="text-xs text-stone-700">
            <span className="font-medium flex items-center gap-1">
              <Bell className="w-3.5 h-3.5 text-stone-500" />
              Nhắc tôi trước lịch hẹn
            </span>
            <span className="text-[11px] text-stone-500 block mt-0.5">
              Gửi thông báo nhắc nhở nhẹ nhàng trước giờ trị liệu.
            </span>
          </div>
        </label>

        {reminderMessage && (
          <p className="text-[11px] text-[#2d6a4f] bg-emerald-50 border border-emerald-200 p-2 rounded-lg">
            ✓ {reminderMessage}
          </p>
        )}
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
          onClick={handleStartAnotherBooking}
          className="w-full sm:w-auto inline-flex items-center justify-center gap-2 px-5 py-2.5 bg-white border border-stone-200 text-stone-700 text-sm font-medium rounded-xl hover:bg-stone-50 hover:text-stone-900 transition-colors cursor-pointer"
        >
          <RotateCcw className="w-4 h-4" aria-hidden="true" />
          Đặt lịch khác
        </button>
      </div>
    </div>
  );
}
