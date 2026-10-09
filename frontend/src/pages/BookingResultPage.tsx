import { useState, useEffect } from 'react';
import { useSearchParams, useNavigate, Link } from 'react-router-dom';
import { verifyVNPayCallback, getPublicBookingByCode, getPublicSpaInfo } from '../lib/api/publicBooking';
import type { PublicBookingDetailResponse, PublicSpaInfoResponse } from '../types/publicBooking';
import { formatCurrency, formatTimeRange, formatDateDMY } from '../lib/format';
import {
  CheckCircle2,
  XCircle,
  Clock,
  Calendar,
  User,
  Phone,
  Mail,
  CreditCard,
  Sparkles,
  Copy,
  Check,
  ArrowRight,
  Home,
  RefreshCw,
  Search
} from 'lucide-react';

export default function BookingResultPage() {
  const [searchParams] = useSearchParams();
  const navigate = useNavigate();

  const vnpResponseCode = searchParams.get('vnp_ResponseCode');
  const vnpTxnRef = searchParams.get('vnp_TxnRef');
  const codeParam = searchParams.get('code');
  const hasParams = !!((vnpResponseCode && vnpTxnRef) || codeParam);

  const [loading, setLoading] = useState(hasParams);
  const [error, setError] = useState<string | null>(hasParams ? null : 'Không có thông tin lịch hẹn để hiển thị.');
  const [booking, setBooking] = useState<PublicBookingDetailResponse | null>(null);
  const [spa, setSpa] = useState<PublicSpaInfoResponse | null>(null);
  const [copied, setCopied] = useState(false);

  useEffect(() => {
    let isMounted = true;
    const slug = 'tikey-spa';

    // 1. Fetch spa info
    getPublicSpaInfo(slug).then((spaData) => {
      if (isMounted) setSpa(spaData);
    }).catch(() => {
      // Non-fatal fallback
    });

    // Scenario A: Returned from VNPay with callback query params
    if (vnpResponseCode && vnpTxnRef) {
      const params: Record<string, string> = {};
      searchParams.forEach((val, key) => {
        params[key] = val;
      });

      verifyVNPayCallback(slug, params)
        .then(async (result) => {
          if (!isMounted) return;
          if (result.bookingCode) {
            try {
              const detail = await getPublicBookingByCode(slug, result.bookingCode);
              if (isMounted) {
                setBooking(detail);
                setLoading(false);
                // Deterministic & reload-safe: switch URL to clean /dat-lich/ket-qua?code=...
                window.history.replaceState(null, '', `/dat-lich/ket-qua?code=${encodeURIComponent(result.bookingCode)}`);
              }
            } catch {
              if (isMounted) {
                setError('Không thể tải chi tiết lịch hẹn.');
                setLoading(false);
              }
            }
          } else {
            if (isMounted) {
              setError(result.message || 'Xác thực thanh toán thất bại.');
              setLoading(false);
            }
          }
        })
        .catch((err) => {
          if (isMounted) {
            setError(err.response?.data?.message || 'Có lỗi xảy ra khi xác thực thanh toán với VNPay.');
            setLoading(false);
          }
        });
      return;
    }

    // Scenario B: Direct view with code parameter (/dat-lich/ket-qua?code=BK-XXXX)
    if (codeParam) {
      getPublicBookingByCode(slug, codeParam)
        .then((detail) => {
          if (isMounted) {
            setBooking(detail);
            setLoading(false);
          }
        })
        .catch((err) => {
          if (isMounted) {
            setError(err.response?.data?.message || 'Không tìm thấy thông tin lịch hẹn.');
            setLoading(false);
          }
        });
      return;
    }

    return () => {
      isMounted = false;
    };
  }, [codeParam, searchParams, vnpResponseCode, vnpTxnRef]);

  const handleCopyCode = () => {
    if (booking?.bookingCode) {
      navigator.clipboard.writeText(booking.bookingCode);
      setCopied(true);
      setTimeout(() => setCopied(false), 2000);
    }
  };

  // Parse process steps if available
  const parseSteps = (stepsStr?: string | null): string[] => {
    if (!stepsStr) return [];
    try {
      const parsed = JSON.parse(stepsStr);
      if (Array.isArray(parsed)) return parsed.map(String);
    } catch {
      return stepsStr
        .split('\n')
        .map((s) => s.trim())
        .filter(Boolean);
    }
    return [];
  };

  const stepsList = parseSteps(booking?.processSteps);

  if (loading) {
    return (
      <div className="min-h-screen bg-stone-50 flex items-center justify-center p-4">
        <div className="bg-white border border-stone-200 rounded-3xl p-8 max-w-md w-full text-center shadow-xs">
          <RefreshCw className="w-10 h-10 text-stone-800 animate-spin mx-auto mb-4" />
          <h2 className="text-lg font-serif-title font-semibold text-stone-900 mb-2">Đang xác thực giao dịch</h2>
          <p className="text-sm text-stone-500">Hệ thống đang kết nối với cổng thanh toán để cập nhật trạng thái lịch hẹn...</p>
        </div>
      </div>
    );
  }

  if (error || !booking) {
    return (
      <div className="min-h-screen bg-stone-50 flex flex-col justify-between">
        <header className="border-b border-stone-200 bg-white">
          <div className="max-w-4xl mx-auto px-4 h-16 flex items-center justify-between">
            <Link to="/" className="text-lg font-serif-title font-semibold text-stone-900">
              {spa?.name || 'TIKEY SPA'}
            </Link>
            <Link
              to="/"
              className="text-xs font-medium text-stone-600 hover:text-stone-900 flex items-center gap-1.5"
            >
              <Home size={14} /> Về trang chủ
            </Link>
          </div>
        </header>

        <main className="max-w-md mx-auto px-4 py-12 w-full text-center">
          <div className="bg-white border border-stone-200 rounded-3xl p-8 shadow-xs">
            <XCircle className="w-12 h-12 text-rose-500 mx-auto mb-4" />
            <h1 className="text-xl font-serif-title font-semibold text-stone-900 mb-2">Không thể xác nhận</h1>
            <p className="text-sm text-stone-600 mb-6">{error || 'Không tìm thấy thông tin cuộc hẹn.'}</p>
            <div className="flex flex-col sm:flex-row gap-3">
              <Link
                to="/tra-cuu"
                className="flex-1 py-3 px-4 bg-stone-900 text-white rounded-xl text-sm font-medium hover:bg-stone-800 transition-colors flex items-center justify-center gap-2"
              >
                <Search size={15} /> Tra cứu lịch hẹn
              </Link>
              <Link
                to="/"
                className="flex-1 py-3 px-4 bg-stone-100 text-stone-700 rounded-xl text-sm font-medium hover:bg-stone-200 transition-colors flex items-center justify-center gap-2"
              >
                <Home size={15} /> Trang chủ
              </Link>
            </div>
          </div>
        </main>

        <footer className="py-6 text-center text-xs text-stone-400">
          &copy; {new Date().getFullYear()} {spa?.name || 'TIKEY SPA'}. Quiet Luxury Wellness.
        </footer>
      </div>
    );
  }

  const isPaid = booking.paymentStatus === 'PAID';
  const isCancelled = booking.status === 'CANCELLED' || booking.paymentStatus === 'CANCELLED';

  return (
    <div className="min-h-screen bg-stone-50 font-sans flex flex-col justify-between">
      {/* Top Bar */}
      <header className="border-b border-stone-200 bg-white sticky top-0 z-20">
        <div className="max-w-3xl mx-auto px-4 h-16 flex items-center justify-between">
          <Link to="/" className="flex items-center gap-2">
            <span className="text-lg font-serif-title font-bold text-stone-900 tracking-tight">
              {spa?.name || 'TIKEY SPA'}
            </span>
          </Link>
          <div className="flex items-center gap-3">
            <Link
              to="/tra-cuu"
              className="text-xs font-medium text-stone-600 hover:text-stone-900 flex items-center gap-1 px-3 py-1.5 rounded-lg hover:bg-stone-100 transition-colors"
            >
              <Search size={14} /> Tra cứu
            </Link>
            <Link
              to="/"
              className="text-xs font-medium text-stone-600 hover:text-stone-900 flex items-center gap-1 px-3 py-1.5 rounded-lg hover:bg-stone-100 transition-colors"
            >
              <Home size={14} /> Trang chủ
            </Link>
          </div>
        </div>
      </header>

      {/* Main Content */}
      <main className="max-w-3xl mx-auto px-4 py-8 sm:py-12 w-full">
        <div className="bg-white border border-stone-200 rounded-3xl p-6 sm:p-10 shadow-xs space-y-8">
          
          {/* Header Status Banner */}
          <div className="text-center space-y-3 pb-6 border-b border-stone-100">
            {isPaid ? (
              <>
                <div className="inline-flex p-3.5 bg-emerald-50 text-emerald-600 rounded-2xl mb-1 ring-8 ring-emerald-50/50">
                  <CheckCircle2 className="w-10 h-10" />
                </div>
                <div className="space-y-1">
                  <span className="inline-flex items-center gap-1 px-3 py-1 rounded-full text-xs font-semibold bg-emerald-100/80 text-emerald-800 tracking-wide uppercase">
                    Giao dịch hoàn tất
                  </span>
                  <h1 className="text-2xl sm:text-3xl font-serif-title font-bold text-stone-900 pt-1">
                    Thanh toán thành công qua VNPay!
                  </h1>
                </div>
                <p className="text-stone-600 text-sm max-w-lg mx-auto leading-relaxed">
                  Cảm ơn quý khách đã tin chọn TIKEY SPA. Giao dịch trực tuyến đã được xác nhận thanh toán an toàn và lịch hẹn đã được ghi nhận vào hệ thống.
                </p>
              </>
            ) : isCancelled ? (
              <>
                <div className="inline-flex p-3.5 bg-rose-50 text-rose-600 rounded-2xl mb-1 ring-8 ring-rose-50/50">
                  <XCircle className="w-10 h-10" />
                </div>
                <div className="space-y-1">
                  <span className="inline-flex items-center gap-1 px-3 py-1 rounded-full text-xs font-semibold bg-rose-100/80 text-rose-800 tracking-wide uppercase">
                    Giao dịch đã hủy
                  </span>
                  <h1 className="text-2xl sm:text-3xl font-serif-title font-bold text-stone-900 pt-1">
                    Giao dịch đã được hủy
                  </h1>
                </div>
                <p className="text-stone-600 text-sm max-w-lg mx-auto leading-relaxed">
                  Giao dịch thanh toán trực tuyến qua VNPay đã được hủy. Quý khách có thể đặt lại lịch hẹn hoặc chọn phương thức thanh toán tại Spa khi đến trải nghiệm.
                </p>
              </>
            ) : (
              <>
                <div className="inline-flex p-3.5 bg-stone-100 text-stone-800 rounded-2xl mb-1 ring-8 ring-stone-100/50">
                  <Clock className="w-10 h-10" />
                </div>
                <h1 className="text-2xl sm:text-3xl font-serif-title font-bold text-stone-900">
                  Đã ghi nhận lịch hẹn
                </h1>
                <p className="text-stone-600 text-sm max-w-lg mx-auto leading-relaxed">
                  Lịch hẹn của bạn đã được ghi nhận. Quý khách vui lòng thanh toán trực tiếp khi đến làm dịch vụ tại spa.
                </p>
              </>
            )}

            {/* Booking Code Highlight Box */}
            <div className="inline-flex flex-wrap items-center justify-center gap-3 bg-stone-50 border border-stone-200/90 px-5 py-2.5 rounded-2xl mt-4 shadow-2xs max-w-full">
              <span className="text-xs uppercase tracking-wider font-semibold text-stone-500">Mã lịch hẹn:</span>
              <span className="font-mono text-lg font-bold text-stone-900 tracking-wide break-all">{booking.bookingCode}</span>
              <button
                type="button"
                onClick={handleCopyCode}
                className="inline-flex items-center gap-1 px-3 py-2 min-h-11 text-xs font-medium text-stone-600 hover:text-stone-900 hover:bg-white rounded-lg border border-stone-200 transition-colors cursor-pointer"
                title="Sao chép mã"
              >
                {copied ? (
                  <>
                    <Check size={14} className="text-emerald-600" />
                    <span className="text-emerald-700 font-semibold">Đã chép</span>
                  </>
                ) : (
                  <>
                    <Copy size={14} />
                    <span>Sao chép</span>
                  </>
                )}
              </button>
            </div>
          </div>

          {/* Next Steps Guidance Banner */}
          {isPaid && (
            <div className="bg-[#465d4c]/5 border border-[#465d4c]/20 rounded-2xl p-4 sm:p-5 text-left">
              <h4 className="text-xs font-bold text-[#465d4c] uppercase tracking-wider mb-2 flex items-center gap-1.5">
                <Sparkles size={14} /> Hướng dẫn tiếp theo cho quý khách
              </h4>
              <ul className="text-xs sm:text-sm text-stone-700 space-y-1.5 list-disc list-inside">
                <li>Lịch hẹn đã được xác nhận tự động và chuyển đến đội ngũ chuyên viên kỹ thuật.</li>
                <li>Quý khách vui lòng có mặt trước giờ hẹn <strong>10 - 15 phút</strong> để thưởng thức trà thảo mộc đón tiếp và chuẩn bị tốt nhất.</li>
                <li>Quý khách có thể sử dụng mã lịch hẹn <strong>{booking.bookingCode}</strong> để tra cứu hoặc liên hệ hỗ trợ bất kỳ lúc nào.</li>
              </ul>
            </div>
          )}

          {/* Details Grid */}
          <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
            {/* Service & Process */}
            <div className="bg-stone-50/70 border border-stone-100 rounded-2xl p-5 space-y-3">
              <div className="flex items-center gap-2 text-xs uppercase font-bold text-stone-400 tracking-wider">
                <Sparkles size={14} className="text-stone-500" /> Dịch vụ trị liệu
              </div>
              <div>
                <h3 className="font-serif-title font-semibold text-base text-stone-900">{booking.serviceName}</h3>
                {booking.categoryName && (
                  <span className="inline-block mt-1 text-[11px] font-medium text-stone-600 bg-stone-200/70 px-2 py-0.5 rounded-md">
                    {booking.categoryName}
                  </span>
                )}
                <div className="flex items-center gap-3 mt-2 text-xs text-stone-600">
                  <span className="flex items-center gap-1">
                    <Clock size={13} className="text-stone-400" /> {booking.durationMinutes} phút
                  </span>
                  <span className="font-semibold text-stone-900">
                    {formatCurrency(booking.price)}
                  </span>
                </div>
              </div>

              {/* Read-only Process steps if available */}
              {stepsList.length > 0 && (
                <div className="pt-2 border-t border-stone-200/60 mt-3">
                  <span className="text-[11px] font-bold text-stone-400 uppercase tracking-wider block mb-1.5">
                    Quy trình thực hiện ({stepsList.length} bước)
                  </span>
                  <ol className="space-y-1 text-xs text-stone-600 list-decimal list-inside pl-1">
                    {stepsList.map((step, idx) => (
                      <li key={idx} className="leading-relaxed">{step}</li>
                    ))}
                  </ol>
                </div>
              )}
            </div>

            {/* Time & Staff */}
            <div className="bg-stone-50/70 border border-stone-100 rounded-2xl p-5 space-y-4">
              <div className="flex items-center gap-2 text-xs uppercase font-bold text-stone-400 tracking-wider">
                <Calendar size={14} className="text-stone-500" /> Thời gian & Kỹ thuật viên
              </div>
              <div className="space-y-2 text-sm">
                <div>
                  <span className="text-xs text-stone-500 block">Thời gian hẹn:</span>
                  <strong className="text-stone-900 font-medium">
                    {formatDateDMY(booking.startTime)}, {formatTimeRange(booking.startTime, booking.endTime)}
                  </strong>
                </div>
                <div>
                  <span className="text-xs text-stone-500 block">Kỹ thuật viên phục vụ:</span>
                  <strong className="text-stone-900 font-medium">{booking.staffName || 'TIKEY Specialist'}</strong>
                </div>
                {booking.spaAddress && (
                  <div>
                    <span className="text-xs text-stone-500 block">Địa chỉ trải nghiệm:</span>
                    <span className="text-xs text-stone-700 leading-snug block">{booking.spaAddress}</span>
                  </div>
                )}
              </div>
            </div>

            {/* Customer Info */}
            <div className="bg-stone-50/70 border border-stone-100 rounded-2xl p-5 space-y-3">
              <div className="flex items-center gap-2 text-xs uppercase font-bold text-stone-400 tracking-wider">
                <User size={14} className="text-stone-500" /> Thông tin khách hàng
              </div>
              <div className="space-y-1.5 text-xs text-stone-700">
                <p className="font-semibold text-sm text-stone-900">{booking.customerName}</p>
                <p className="flex items-center gap-1.5 text-stone-600">
                  <Phone size={13} className="text-stone-400" /> {booking.customerPhone}
                </p>
                {booking.customerEmail && (
                  <p className="flex items-center gap-1.5 text-stone-600">
                    <Mail size={13} className="text-stone-400" /> {booking.customerEmail}
                  </p>
                )}
              </div>
            </div>

            {/* Payment Summary */}
            <div className="bg-stone-50/70 border border-stone-100 rounded-2xl p-5 space-y-3">
              <div className="flex items-center gap-2 text-xs uppercase font-bold text-stone-400 tracking-wider">
                <CreditCard size={14} className="text-stone-500" /> Thanh toán
              </div>
              <div className="space-y-1.5 text-xs">
                <div className="flex justify-between py-0.5">
                  <span className="text-stone-500">Hình thức:</span>
                  <span className="font-medium text-stone-900">
                    {booking.paymentMethod === 'VNPAY' ? 'VNPay Sandbox' : 'Thanh toán tại Spa'}
                  </span>
                </div>
                <div className="flex justify-between py-0.5">
                  <span className="text-stone-500">Trạng thái:</span>
                  <span className={`font-semibold ${
                    isPaid ? 'text-emerald-700' : isCancelled ? 'text-rose-600' : 'text-amber-700'
                  }`}>
                    {isPaid ? 'Đã thanh toán (PAID)' : isCancelled ? 'Đã hủy (CANCELLED)' : 'Chờ thanh toán (UNPAID)'}
                  </span>
                </div>
                <div className="flex justify-between py-0.5 pt-1.5 border-t border-stone-200">
                  <span className="text-stone-600 font-medium">Tổng tiền:</span>
                  <span className="font-serif-title font-bold text-sm text-stone-900">
                    {formatCurrency(booking.paidAmount || booking.price)}
                  </span>
                </div>
                {booking.paidAt && (
                  <div className="text-[11px] text-stone-400 pt-1">
                    Thanh toán lúc: {new Date(booking.paidAt).toLocaleString('vi-VN')}
                  </div>
                )}
              </div>
            </div>
          </div>

          {/* Action Buttons */}
          <div className="pt-4 flex flex-col sm:flex-row items-center justify-between gap-3 border-t border-stone-100">
            <Link
              to={`/tra-cuu?code=${encodeURIComponent(booking.bookingCode)}#tra-cuu`}
              className="w-full sm:w-auto px-5 py-2.5 rounded-xl border border-stone-200 text-stone-700 text-xs font-semibold hover:bg-stone-50 transition-colors text-center inline-flex items-center justify-center gap-1.5"
            >
              <Search size={14} /> Tra cứu lịch hẹn
            </Link>
            <div className="flex gap-3 w-full sm:w-auto">
              <Link
                to="/"
                className="flex-1 sm:flex-initial px-5 py-2.5 rounded-xl bg-stone-100 text-stone-700 text-xs font-semibold hover:bg-stone-200 transition-colors text-center inline-flex items-center justify-center gap-1.5"
              >
                <Home size={14} /> Về trang chủ
              </Link>
              <button
                type="button"
                onClick={() => navigate('/#booking')}
                className="flex-1 sm:flex-initial px-6 py-2.5 rounded-xl bg-stone-900 text-white text-xs font-semibold hover:bg-stone-800 transition-colors flex items-center justify-center gap-1.5 cursor-pointer shadow-xs"
              >
                Đặt lịch mới <ArrowRight size={14} />
              </button>
            </div>
          </div>
        </div>
      </main>

      {/* Footer */}
      <footer className="py-6 text-center text-xs text-stone-400">
        &copy; {new Date().getFullYear()} {spa?.name || 'TIKEY SPA'}. Quiet Luxury Wellness.
      </footer>
    </div>
  );
}
