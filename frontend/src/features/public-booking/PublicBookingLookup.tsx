import { useState, useEffect, useCallback, useRef } from 'react';
import { useSearchParams } from 'react-router-dom';
import { getPublicBookingByCode } from '../../lib/api/publicBooking';
import type { PublicBookingDetailResponse } from '../../types/publicBooking';
import { formatCurrency, formatTimeRange, formatDateLongFromYMD } from '../../lib/format';
import {
  Search,
  Loader2,
  AlertCircle,
  Clock,
  Calendar,
  UserRound,
  MapPin,
  Phone,
  Mail,
  Copy,
  Check,
  CreditCard,
  Banknote,
  CheckCircle2,
  Sparkles,
  Tag,
  ShieldCheck,
  ArrowRight,
} from 'lucide-react';

interface PublicBookingLookupProps {
  slug: string;
  spaName: string;
  spaPhone?: string;
  defaultCode?: string;
}

const APPOINTMENT_STATUS_DETAILS: Record<string, { label: string; tone: string; desc: string }> = {
  PENDING: {
    label: 'Chờ xác nhận',
    tone: 'bg-amber-50 text-amber-800 border-amber-200',
    desc: 'Lịch hẹn đã được tiếp nhận. Nhân viên TIKEY SPA sẽ liên hệ xác nhận trong thời gian sớm nhất.',
  },
  CONFIRMED: {
    label: 'Đã xác nhận',
    tone: 'bg-emerald-50 text-emerald-800 border-emerald-200',
    desc: 'Lịch hẹn đã được xác nhận. Rất hân hạnh được đón tiếp quý khách đúng giờ.',
  },
  COMPLETED: {
    label: 'Đã hoàn thành',
    tone: 'bg-stone-100 text-stone-700 border-stone-200',
    desc: 'Lịch hẹn đã hoàn tất. Cảm ơn quý khách đã tin tưởng dịch vụ tại TIKEY SPA.',
  },
  CANCELLED: {
    label: 'Đã hủy',
    tone: 'bg-rose-50 text-rose-700 border-rose-200',
    desc: 'Lịch hẹn đã được hủy. Quý khách có thể đặt lại lịch hẹn mới bất cứ lúc nào.',
  },
};

const PAYMENT_STATUS_DETAILS: Record<string, { label: string; tone: string; desc: string }> = {
  UNPAID: {
    label: 'Chưa thanh toán',
    tone: 'bg-stone-100 text-stone-700 border-stone-200',
    desc: 'Quý khách vui lòng thanh toán trực tiếp khi đến làm dịch vụ tại cơ sở spa.',
  },
  PENDING: {
    label: 'Đang xử lý',
    tone: 'bg-amber-50 text-amber-800 border-amber-200',
    desc: 'Giao dịch trực tuyến đang chờ xử lý từ cổng thanh toán.',
  },
  PAID: {
    label: 'Đã thanh toán',
    tone: 'bg-emerald-50 text-emerald-800 border-emerald-200',
    desc: 'Giao dịch đã được xác nhận thanh toán thành công qua cổng thanh toán.',
  },
  CANCELLED: {
    label: 'Thanh toán thất bại / Đã hủy',
    tone: 'bg-rose-50 text-rose-700 border-rose-200',
    desc: 'Giao dịch trực tuyến đã bị hủy. Quý khách có thể thanh toán tại spa khi đến.',
  },
  FAILED: {
    label: 'Thanh toán thất bại / Đã hủy',
    tone: 'bg-rose-50 text-rose-700 border-rose-200',
    desc: 'Giao dịch thanh toán không thành công. Quý khách có thể thanh toán tại spa khi đến.',
  },
};

export function PublicBookingLookup({ slug, spaName, spaPhone, defaultCode = '' }: PublicBookingLookupProps) {
  const [searchParams] = useSearchParams();
  const urlLookupCode = searchParams.get('lookupCode') || searchParams.get('code') || '';
  const initialCode = urlLookupCode || defaultCode;

  const [code, setCode] = useState(initialCode);
  const [loading, setLoading] = useState(false);
  const [result, setResult] = useState<PublicBookingDetailResponse | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [copied, setCopied] = useState(false);
  const lastLookupRef = useRef<string>('');

  const executeLookup = useCallback(async (codeToLookup: string, syncUrl: boolean = true) => {
    const cleanCode = codeToLookup.trim();
    if (!cleanCode) {
      setError('Vui lòng nhập mã lịch hẹn (ví dụ: BK-...)');
      return;
    }

    if (syncUrl) {
      const currentUrl = new URL(window.location.href);
      currentUrl.searchParams.delete('serviceId');
      currentUrl.searchParams.delete('lookupCode');
      currentUrl.searchParams.set('code', cleanCode);
      currentUrl.hash = 'tra-cuu';
      window.history.replaceState({}, '', currentUrl.pathname + currentUrl.search + currentUrl.hash);
    }

    lastLookupRef.current = cleanCode;
    setLoading(true);
    setError(null);
    setResult(null);

    try {
      const data = await getPublicBookingByCode(slug, cleanCode);
      setResult(data);
    } catch (err: unknown) {
      const axiosErr = err as { response?: { status: number } };
      if (axiosErr.response?.status === 404) {
        setError('Không tìm thấy lịch hẹn với mã này tại TIKEY SPA. Vui lòng kiểm tra lại mã đã nhập.');
      } else {
        setError('Không thể kết nối đến hệ thống tra cứu. Vui lòng thử lại sau.');
      }
    } finally {
      setLoading(false);
    }
  }, [slug]);

  // Handle URL param or defaultCode changes dynamically
  useEffect(() => {
    const targetCode = (urlLookupCode || defaultCode).trim();
    if (targetCode && targetCode !== lastLookupRef.current) {
      setCode(targetCode);
      executeLookup(targetCode, false);
    }
  }, [urlLookupCode, defaultCode, executeLookup]);

  // Listen to custom window event for in-page immediate lookup without remount
  useEffect(() => {
    const handleImmediateLookupEvent = (e: CustomEvent<{ code: string }>) => {
      const targetCode = e.detail?.code?.trim();
      if (targetCode) {
        setCode(targetCode);
        executeLookup(targetCode, true);
      }
    };

    window.addEventListener('public-booking-lookup', handleImmediateLookupEvent as EventListener);
    return () => {
      window.removeEventListener('public-booking-lookup', handleImmediateLookupEvent as EventListener);
    };
  }, [executeLookup]);

  const handleLookup = async (e?: React.FormEvent) => {
    if (e) e.preventDefault();
    await executeLookup(code, true);
  };

  const handleClear = () => {
    setCode('');
    setResult(null);
    setError(null);
    lastLookupRef.current = '';
    const currentUrl = new URL(window.location.href);
    currentUrl.searchParams.delete('code');
    currentUrl.searchParams.delete('lookupCode');
    window.history.replaceState({}, '', currentUrl.pathname + currentUrl.search + currentUrl.hash);
  };

  const handleStartFreshBooking = () => {
    const currentUrl = new URL(window.location.href);
    currentUrl.searchParams.delete('serviceId');
    currentUrl.searchParams.delete('code');
    currentUrl.searchParams.delete('lookupCode');
    currentUrl.hash = 'booking';
    window.history.replaceState({}, '', currentUrl.pathname + currentUrl.search + currentUrl.hash);
    window.dispatchEvent(new CustomEvent('reset-booking-session'));
    const el = document.getElementById('booking');
    if (el) {
      el.scrollIntoView({ behavior: 'smooth' });
    }
  };

  const handleCopy = async (text: string) => {
    try {
      await navigator.clipboard.writeText(text);
      setCopied(true);
      setTimeout(() => setCopied(false), 2000);
    } catch {
      setCopied(false);
    }
  };

  const parseProcessSteps = (stepsJson?: string | null): string[] => {
    if (!stepsJson) return [];
    try {
      const parsed = JSON.parse(stepsJson);
      if (Array.isArray(parsed)) return parsed;
    } catch {
      return stepsJson.split('\n').filter((s) => s.trim().length > 0);
    }
    return [];
  };

  const formatPaidTime = (iso?: string | null): string => {
    if (!iso) return '';
    try {
      const d = new Date(iso);
      if (isNaN(d.getTime())) return iso;
      return d.toLocaleString('vi-VN', {
        hour: '2-digit',
        minute: '2-digit',
        day: '2-digit',
        month: '2-digit',
        year: 'numeric',
      });
    } catch {
      return iso;
    }
  };

  const appointmentStatusConfig = result ? (APPOINTMENT_STATUS_DETAILS[result.status] || {
    label: result.status,
    tone: 'bg-stone-50 text-stone-700 border-stone-200',
    desc: 'Thông tin lịch hẹn.',
  }) : null;

  const paymentStatusConfig = result ? (PAYMENT_STATUS_DETAILS[result.paymentStatus] || {
    label: result.paymentStatus,
    tone: 'bg-stone-100 text-stone-700 border-stone-200',
    desc: 'Trạng thái thanh toán.',
  }) : null;

  const isVnPay = result?.paymentMethod === 'VNPAY';
  const processSteps = result ? parseProcessSteps(result.processSteps) : [];

  return (
    <div className="wellness-card p-6 sm:p-8" id="tra-cuu">
      <div className="max-w-3xl mx-auto">
        {/* Header */}
        <div className="text-center mb-6">
          <div className="w-12 h-12 rounded-full bg-[#f2f6f3] border border-[#e2ece4] flex items-center justify-center mx-auto mb-3 text-[#465d4c]">
            <Search className="w-5 h-5" aria-hidden="true" />
          </div>
          <h3 className="text-xl sm:text-2xl font-serif-title font-medium text-stone-900 mb-1">
            Tra cứu thông tin lịch hẹn tại {spaName}
          </h3>
          <p className="text-sm text-stone-500">
            Nhập mã lịch hẹn bạn nhận được sau khi đăng ký để kiểm tra trạng thái dịch vụ và thanh toán chi tiết.
            {spaPhone && (
              <span className="block mt-1 text-xs text-stone-400">
                Hỗ trợ trực tiếp qua hotline: <strong className="text-stone-600">{spaPhone}</strong>
              </span>
            )}
          </p>
        </div>

        {/* Search Form */}
        <form onSubmit={handleLookup} className="flex flex-col sm:flex-row gap-2.5 mb-6">
          <div className="relative flex-1">
            <input
              id="public-lookup-input"
              type="text"
              value={code}
              onChange={(e) => {
                setCode(e.target.value);
                if (error) setError(null);
              }}
              placeholder="Nhập mã lịch hẹn (VD: BK-9C4B7D2F...)"
              className="w-full h-11 sm:h-12 pl-4 pr-10 rounded-xl bg-stone-50 border border-stone-200 text-stone-900 text-sm font-mono focus:outline-none focus:bg-white focus:border-[#465d4c] focus:ring-2 focus:ring-[#465d4c]/15 transition-all"
              aria-label="Mã lịch hẹn"
              disabled={loading}
            />
            {code && (
              <button
                type="button"
                onClick={handleClear}
                className="absolute right-3 top-1/2 -translate-y-1/2 text-xs text-stone-400 hover:text-stone-700 cursor-pointer"
              >
                Xóa
              </button>
            )}
          </div>
          <button
            type="submit"
            disabled={loading || !code.trim()}
            className="h-11 sm:h-12 px-6 rounded-xl bg-[#465d4c] hover:bg-[#374a3c] text-white text-sm font-medium inline-flex items-center justify-center gap-2 transition-colors disabled:opacity-50 disabled:cursor-not-allowed shadow-xs shrink-0 cursor-pointer"
          >
            {loading ? (
              <>
                <Loader2 className="w-4 h-4 animate-spin" aria-hidden="true" />
                <span>Đang tra cứu...</span>
              </>
            ) : (
              <>
                <Search className="w-4 h-4" aria-hidden="true" />
                <span>Tra cứu</span>
              </>
            )}
          </button>
        </form>

        {/* Error message */}
        {error && (
          <div className="flex items-start gap-3 p-4 rounded-xl bg-rose-50 border border-rose-200 text-rose-800 text-sm mb-6" role="alert">
            <AlertCircle className="w-5 h-5 shrink-0 mt-0.5 text-rose-600" aria-hidden="true" />
            <div className="flex-1">
              <p className="font-medium">Không tìm thấy kết quả</p>
              <p className="text-xs text-rose-700 mt-0.5">{error}</p>
            </div>
          </div>
        )}

        {/* REDESIGNED BOOKING DETAIL CARD */}
        {result && (
          <div className="bg-white rounded-3xl border border-stone-200 shadow-sm overflow-hidden animate-in fade-in duration-300">
            {/* Header: Booking Code & Status Pill */}
            <div className="p-5 sm:p-6 bg-gradient-to-r from-stone-50 via-amber-50/20 to-stone-50 border-b border-stone-200/80 flex flex-col sm:flex-row sm:items-center justify-between gap-3">
              <div>
                <span className="text-[11px] uppercase tracking-wider font-semibold text-stone-400">
                  Mã lịch hẹn khách hàng
                </span>
                <div className="flex items-center gap-2.5 mt-0.5">
                  <span className="font-mono font-bold text-xl sm:text-2xl text-stone-900 select-all">
                    {result.bookingCode}
                  </span>
                  <button
                    type="button"
                    onClick={() => handleCopy(result.bookingCode)}
                    className="p-1.5 rounded-lg hover:bg-stone-200 text-stone-500 hover:text-stone-900 transition-colors cursor-pointer"
                    title="Sao chép mã"
                    aria-label="Sao chép mã"
                  >
                    {copied ? <Check className="w-4 h-4 text-emerald-600" /> : <Copy className="w-4 h-4" />}
                  </button>
                </div>
              </div>

              {appointmentStatusConfig && (
                <div className="flex items-center gap-2">
                  <span className="text-xs text-stone-400">Trạng thái:</span>
                  <span className={`inline-flex items-center px-3 py-1 rounded-full text-xs font-semibold border ${appointmentStatusConfig.tone}`}>
                    {appointmentStatusConfig.label}
                  </span>
                </div>
              )}
            </div>

            {/* Status Narrative Note */}
            {appointmentStatusConfig && (
              <div className="px-5 sm:px-6 py-3 bg-stone-50/60 border-b border-stone-100 text-xs sm:text-sm text-stone-600 flex items-center gap-2">
                <CheckCircle2 className="w-4 h-4 text-[#465d4c] shrink-0" />
                <span>{appointmentStatusConfig.desc}</span>
              </div>
            )}

            {/* 4 Summary Grid Sections */}
            <div className="p-5 sm:p-6 space-y-6">
              <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                {/* Section 1: Appointment & Service */}
                <div className="p-4 sm:p-5 rounded-2xl bg-stone-50/80 border border-stone-200/80 space-y-3">
                  <div className="flex items-center justify-between pb-2 border-b border-stone-200/70">
                    <span className="text-xs font-bold uppercase tracking-wider text-stone-500 flex items-center gap-1.5">
                      <Sparkles className="w-3.5 h-3.5 text-[#8a704c]" />
                      Thông tin lịch hẹn
                    </span>
                    {result.categoryName && (
                      <span className="inline-flex items-center gap-1 text-[11px] font-medium text-stone-600 bg-white px-2 py-0.5 rounded-md border border-stone-200">
                        <Tag className="w-3 h-3 text-stone-400" />
                        {result.categoryName}
                      </span>
                    )}
                  </div>

                  <div>
                    <h4 className="font-semibold text-stone-900 text-base">{result.serviceName}</h4>
                    <div className="flex items-center gap-3 text-xs text-stone-500 mt-1">
                      <span className="inline-flex items-center gap-1">
                        <Clock className="w-3.5 h-3.5 text-stone-400" />
                        {result.durationMinutes} phút
                      </span>
                      <span className="font-bold text-[#8a704c] text-sm">
                        {formatCurrency(result.price)}
                      </span>
                    </div>
                  </div>

                  <div className="pt-2 border-t border-stone-200/60 space-y-2 text-xs">
                    <div className="flex items-start gap-2 text-stone-800">
                      <Calendar className="w-4 h-4 text-stone-400 mt-0.5 shrink-0" />
                      <div>
                        <p className="font-medium text-sm text-stone-900">
                          {formatTimeRange(result.startTime, result.endTime)}
                        </p>
                        <p className="text-xs text-stone-500 capitalize">
                          {result.startTime ? formatDateLongFromYMD(result.startTime.split('T')[0]) : ''}
                        </p>
                      </div>
                    </div>

                    <div className="flex items-center gap-2 text-stone-700 pt-1">
                      <UserRound className="w-4 h-4 text-stone-400 shrink-0" />
                      <span>Kỹ thuật viên: <strong className="text-stone-900">{result.staffName || 'TIKEY SPA sắp xếp'}</strong></span>
                    </div>
                  </div>
                </div>

                {/* Section 2: Customer Details */}
                <div className="p-4 sm:p-5 rounded-2xl bg-stone-50/80 border border-stone-200/80 space-y-3">
                  <div className="flex items-center justify-between pb-2 border-b border-stone-200/70">
                    <span className="text-xs font-bold uppercase tracking-wider text-stone-500 flex items-center gap-1.5">
                      <UserRound className="w-3.5 h-3.5 text-[#465d4c]" />
                      Thông tin khách hàng
                    </span>
                  </div>

                  <div className="space-y-2.5 text-xs sm:text-sm text-stone-700">
                    <div className="flex justify-between items-center py-1 border-b border-stone-200/50">
                      <span className="text-stone-500">Họ và tên:</span>
                      <span className="font-semibold text-stone-900">{result.customerName}</span>
                    </div>

                    <div className="flex justify-between items-center py-1 border-b border-stone-200/50">
                      <span className="text-stone-500">Số điện thoại:</span>
                      <a href={`tel:${result.customerPhone}`} className="font-medium text-stone-900 hover:text-[#465d4c]">
                        {result.customerPhone}
                      </a>
                    </div>

                    {result.customerEmail && (
                      <div className="flex justify-between items-center py-1 border-b border-stone-200/50">
                        <span className="text-stone-500">Email:</span>
                        <span className="text-stone-800">{result.customerEmail}</span>
                      </div>
                    )}
                  </div>
                </div>

                {/* Section 3: Authoritative Payment Summary */}
                <div className="p-4 sm:p-5 rounded-2xl bg-stone-50/80 border border-stone-200/80 space-y-3">
                  <div className="flex items-center justify-between pb-2 border-b border-stone-200/70">
                    <span className="text-xs font-bold uppercase tracking-wider text-stone-500 flex items-center gap-1.5">
                      {isVnPay ? <CreditCard className="w-3.5 h-3.5 text-blue-600" /> : <Banknote className="w-3.5 h-3.5 text-stone-500" />}
                      Thông tin thanh toán
                    </span>
                    {paymentStatusConfig && (
                      <span className={`inline-flex items-center px-2.5 py-0.5 rounded-full text-[11px] font-semibold border ${paymentStatusConfig.tone}`}>
                        {paymentStatusConfig.label}
                      </span>
                    )}
                  </div>

                  <div className="space-y-2 text-xs sm:text-sm">
                    <div className="flex justify-between items-center">
                      <span className="text-stone-500">Tổng thanh toán:</span>
                      <span className="text-base sm:text-lg font-bold text-[#8a704c]">
                        {formatCurrency(result.price)}
                      </span>
                    </div>

                    <div className="flex justify-between items-center">
                      <span className="text-stone-500">Phương thức:</span>
                      <span className="font-medium text-stone-900">
                        {isVnPay ? 'VNPay Sandbox (Trực tuyến)' : 'Thanh toán tại spa'}
                      </span>
                    </div>

                    {result.paidAt && (
                      <div className="flex justify-between items-center pt-1 border-t border-stone-200/50">
                        <span className="text-stone-500">Thời gian thanh toán:</span>
                        <span className="font-mono text-xs text-stone-800">{formatPaidTime(result.paidAt)}</span>
                      </div>
                    )}

                    {paymentStatusConfig?.desc && (
                      <p className="text-[11px] text-stone-500 italic pt-1 leading-snug">
                        {paymentStatusConfig.desc}
                      </p>
                    )}
                  </div>
                </div>

                {/* Section 4: Spa Profile & Location */}
                <div className="p-4 sm:p-5 rounded-2xl bg-stone-50/80 border border-stone-200/80 space-y-3">
                  <div className="flex items-center justify-between pb-2 border-b border-stone-200/70">
                    <span className="text-xs font-bold uppercase tracking-wider text-stone-500 flex items-center gap-1.5">
                      <MapPin className="w-3.5 h-3.5 text-[#465d4c]" />
                      Địa chỉ & Liên hệ Spa
                    </span>
                  </div>

                  <div className="space-y-2 text-xs sm:text-sm">
                    <div>
                      <h5 className="font-semibold text-stone-900">{result.spaName}</h5>
                      <p className="text-xs text-stone-600 mt-0.5 flex items-start gap-1">
                        <MapPin className="w-3.5 h-3.5 text-stone-400 shrink-0 mt-0.5" />
                        <span>{result.spaAddress}</span>
                      </p>
                    </div>

                    <div className="pt-2 border-t border-stone-200/60 flex flex-wrap gap-x-4 gap-y-1 text-xs">
                      {result.spaPhone && (
                        <a href={`tel:${result.spaPhone}`} className="inline-flex items-center gap-1 text-[#465d4c] font-medium hover:underline">
                          <Phone className="w-3.5 h-3.5" />
                          <span>Hotline: {result.spaPhone}</span>
                        </a>
                      )}
                      {result.spaEmail && (
                        <a href={`mailto:${result.spaEmail}`} className="inline-flex items-center gap-1 text-stone-600 hover:text-stone-900">
                          <Mail className="w-3.5 h-3.5 text-stone-400" />
                          <span>{result.spaEmail}</span>
                        </a>
                      )}
                    </div>
                  </div>
                </div>
              </div>

              {/* Section 5: Service Description & Process Steps */}
              {(result.serviceDescription || processSteps.length > 0) && (
                <div className="p-5 rounded-2xl bg-stone-50/70 border border-stone-200/80 space-y-3 text-left">
                  <h4 className="text-xs font-bold uppercase tracking-wider text-stone-500 flex items-center gap-1.5">
                    <ShieldCheck className="w-4 h-4 text-[#465d4c]" />
                    Quy trình & Chi tiết phác đồ trị liệu
                  </h4>

                  {result.serviceDescription && (
                    <p className="text-xs sm:text-sm text-stone-700 leading-relaxed font-sans">
                      {result.serviceDescription}
                    </p>
                  )}

                  {processSteps.length > 0 && (
                    <div className="pt-2">
                      <span className="text-[11px] font-semibold uppercase tracking-wider text-stone-400 block mb-2">
                        Các bước thực hiện ({processSteps.length} bước tiêu chuẩn):
                      </span>
                      <div className="grid grid-cols-1 sm:grid-cols-2 gap-2">
                        {processSteps.map((step, idx) => (
                          <div key={idx} className="flex items-start gap-2.5 p-2.5 rounded-xl bg-white border border-stone-200/70 text-xs">
                            <span className="flex items-center justify-center w-5 h-5 rounded-full bg-[#465d4c] text-white text-[11px] font-semibold shrink-0 mt-0.5">
                              {idx + 1}
                            </span>
                            <span className="text-stone-800 leading-snug">{step.replace(/^\d+[.\s]*/, '')}</span>
                          </div>
                        ))}
                      </div>
                    </div>
                  )}
                </div>
              )}

              {/* Action: Start fresh booking */}
              <div className="pt-2 flex justify-end">
                <button
                  type="button"
                  onClick={handleStartFreshBooking}
                  className="inline-flex items-center gap-2 px-5 py-2.5 rounded-xl bg-stone-900 text-white text-xs font-semibold hover:bg-stone-800 transition-colors cursor-pointer shadow-xs"
                >
                  <span>Đặt lịch mới</span>
                  <ArrowRight className="w-3.5 h-3.5" />
                </button>
              </div>
            </div>
          </div>
        )}
      </div>
    </div>
  );
}
