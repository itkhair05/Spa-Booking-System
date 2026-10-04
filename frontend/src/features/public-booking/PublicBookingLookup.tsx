import { useState } from 'react';
import { getPublicBookingByCode } from '../../lib/api/publicBooking';
import type { PublicBookingDetailResponse } from '../../types/publicBooking';
import { formatCurrency, formatTimeRange } from '../../lib/format';
import { Search, Loader2, AlertCircle, Clock, Calendar, UserRound, MapPin, Phone, Copy, Check } from 'lucide-react';

interface PublicBookingLookupProps {
  slug: string;
  spaName: string;
  spaPhone?: string;
  defaultCode?: string;
}

const STATUS_DETAILS: Record<string, { label: string; tone: string; desc: string }> = {
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

export function PublicBookingLookup({ slug, spaName, spaPhone, defaultCode = '' }: PublicBookingLookupProps) {
  const [code, setCode] = useState(defaultCode);
  const [loading, setLoading] = useState(false);
  const [result, setResult] = useState<PublicBookingDetailResponse | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [copied, setCopied] = useState(false);

  const handleLookup = async (e?: React.FormEvent) => {
    if (e) e.preventDefault();
    const cleanCode = code.trim();
    if (!cleanCode) {
      setError('Vui lòng nhập mã lịch hẹn (ví dụ: BK-...)');
      return;
    }

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

  const statusConfig = result ? (STATUS_DETAILS[result.status] || {
    label: result.status,
    tone: 'bg-stone-50 text-stone-700 border-stone-200',
    desc: 'Thông tin lịch hẹn.',
  }) : null;

  return (
    <div className="wellness-card p-6 sm:p-8" id="tra-cuu">
      <div className="max-w-xl mx-auto">
        <div className="text-center mb-6">
          <div className="w-12 h-12 rounded-full bg-[#f2f6f3] border border-[#e2ece4] flex items-center justify-center mx-auto mb-3 text-[#465d4c]">
            <Search className="w-5 h-5" aria-hidden="true" />
          </div>
          <h3 className="text-xl sm:text-2xl font-serif-title font-medium text-stone-900 mb-1">
            Tra cứu thông tin lịch hẹn tại {spaName}
          </h3>
          <p className="text-sm text-stone-500">
            Nhập mã lịch hẹn bạn nhận được sau khi đăng ký để kiểm tra trạng thái và chi tiết cuộc hẹn.
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
                onClick={() => { setCode(''); setResult(null); setError(null); }}
                className="absolute right-3 top-1/2 -translate-y-1/2 text-xs text-stone-400 hover:text-stone-700"
              >
                Xóa
              </button>
            )}
          </div>
          <button
            type="submit"
            disabled={loading || !code.trim()}
            className="h-11 sm:h-12 px-6 rounded-xl bg-[#465d4c] hover:bg-[#374a3c] text-white text-sm font-medium inline-flex items-center justify-center gap-2 transition-colors disabled:opacity-50 disabled:cursor-not-allowed shadow-xs shrink-0"
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

        {/* Booking Detail Result */}
        {result && (
          <div className="bg-stone-50 rounded-2xl border border-stone-200 p-5 sm:p-6 space-y-5 animate-in fade-in duration-300">
            {/* Top row: Code + Status */}
            <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3 pb-4 border-b border-stone-200">
              <div>
                <span className="text-xs uppercase tracking-wider font-semibold text-stone-500">Mã lịch hẹn</span>
                <div className="flex items-center gap-2 mt-0.5">
                  <span className="font-mono font-bold text-lg text-stone-900">{result.bookingCode}</span>
                  <button
                    type="button"
                    onClick={() => handleCopy(result.bookingCode)}
                    className="p-1.5 rounded-lg hover:bg-stone-200 text-stone-500 hover:text-stone-900 transition-colors"
                    title="Sao chép mã"
                    aria-label="Sao chép mã"
                  >
                    {copied ? <Check className="w-3.5 h-3.5 text-emerald-600" /> : <Copy className="w-3.5 h-3.5" />}
                  </button>
                </div>
              </div>

              {statusConfig && (
                <div className="sm:text-right">
                  <span className={`inline-flex items-center px-3 py-1 rounded-full text-xs font-semibold border ${statusConfig.tone}`}>
                    {statusConfig.label}
                  </span>
                </div>
              )}
            </div>

            {/* Status note */}
            {statusConfig && (
              <p className="text-xs sm:text-sm text-stone-600 bg-white p-3.5 rounded-xl border border-stone-200/80">
                {statusConfig.desc}
              </p>
            )}

            {/* Service & Time details */}
            <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
              <div className="bg-white p-4 rounded-xl border border-stone-200/80 space-y-2">
                <span className="text-xs font-semibold uppercase tracking-wider text-stone-400">Dịch vụ đã chọn</span>
                <p className="font-semibold text-stone-900">{result.serviceName}</p>
                <div className="flex items-center justify-between text-xs text-stone-500 pt-1">
                  <span className="flex items-center gap-1">
                    <Clock className="w-3.5 h-3.5" />
                    {result.durationMinutes} phút
                  </span>
                  <span className="font-semibold text-[#465d4c] text-sm">
                    {formatCurrency(result.price)}
                  </span>
                </div>
              </div>

              <div className="bg-white p-4 rounded-xl border border-stone-200/80 space-y-2">
                <span className="text-xs font-semibold uppercase tracking-wider text-stone-400">Thời gian & Chuyên viên</span>
                <div className="flex items-center gap-1.5 text-sm font-medium text-stone-900">
                  <Calendar className="w-4 h-4 text-stone-400" />
                  <span>{formatTimeRange(result.startTime, result.endTime)}</span>
                </div>
                <div className="flex items-center gap-1.5 text-xs text-stone-600 pt-1">
                  <UserRound className="w-3.5 h-3.5 text-stone-400" />
                  <span>Kỹ thuật viên: <strong className="text-stone-900">{result.staffName || 'Đang sắp xếp'}</strong></span>
                </div>
              </div>
            </div>

            {/* Spa Contact */}
            <div className="pt-3 border-t border-stone-200/80 flex flex-col sm:flex-row items-start sm:items-center justify-between gap-3 text-xs text-stone-500">
              <div className="flex items-center gap-1.5">
                <MapPin className="w-3.5 h-3.5 text-stone-400 shrink-0" />
                <span>{result.spaAddress || result.spaName}</span>
              </div>
              {result.spaPhone && (
                <a
                  href={`tel:${result.spaPhone}`}
                  className="inline-flex items-center gap-1 text-[#465d4c] font-medium hover:underline"
                >
                  <Phone className="w-3.5 h-3.5" />
                  <span>Hotline: {result.spaPhone}</span>
                </a>
              )}
            </div>
          </div>
        )}
      </div>
    </div>
  );
}
