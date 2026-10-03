import { useState, useEffect } from 'react';
import { useParams } from 'react-router-dom';
import { getPublicSpaInfo } from '../lib/api/publicBooking';
import type { PublicSpaInfoResponse } from '../types/publicBooking';
import { SpaBookingFlow } from '../features/public-booking/SpaBookingFlow';
import { AlertCircle, MapPin, Phone } from 'lucide-react';

export default function PublicBooking() {
  const { slug } = useParams<{ slug: string }>();
  
  const [spa, setSpa] = useState<PublicSpaInfoResponse | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (!slug) return;
    
    let isMounted = true;
    // eslint-disable-next-line
    setLoading(true);
    setError(null);

    getPublicSpaInfo(slug)
      .then((data) => {
        if (isMounted) {
          setSpa(data);
          setLoading(false);
        }
      })
      .catch((err) => {
        if (isMounted) {
          if (err.response?.status === 404) {
            setError('Không tìm thấy spa này.');
          } else {
            setError('Không thể tải thông tin. Vui lòng thử lại sau.');
          }
          setLoading(false);
        }
      });

    return () => {
      isMounted = false;
    };
  }, [slug]);

  if (loading) {
    return (
      <div className="min-h-screen bg-slate-50 flex items-center justify-center p-4">
        <div className="text-slate-500 animate-pulse text-lg">Đang tải thông tin spa...</div>
      </div>
    );
  }

  if (error || !spa) {
    return (
      <div className="min-h-screen bg-slate-50 flex items-center justify-center p-4">
        <div className="max-w-md w-full bg-white rounded-2xl shadow-sm border border-slate-100 p-8 text-center">
          <AlertCircle className="w-12 h-12 text-rose-500 mx-auto mb-4" />
          <h1 className="text-xl font-semibold text-slate-800 mb-2">Đã xảy ra lỗi</h1>
          <p className="text-slate-600 mb-6">{error}</p>
          <button
            onClick={() => window.location.reload()}
            className="px-6 py-2 bg-slate-900 text-white rounded-xl hover:bg-slate-800 transition-colors"
          >
            Thử lại
          </button>
        </div>
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-stone-50 font-sans text-stone-800">
      {/* Header */}
      <header className="bg-white border-b border-stone-200 sticky top-0 z-10 shadow-sm">
        <div className="max-w-3xl mx-auto px-4 sm:px-6 lg:px-8 py-4 sm:py-6">
          <h1 className="text-2xl sm:text-3xl font-medium tracking-tight text-stone-900">
            {spa.name}
          </h1>
          <div className="mt-3 flex flex-col sm:flex-row sm:items-center gap-2 sm:gap-6 text-sm text-stone-500">
            {spa.address && (
              <div className="flex items-center gap-2">
                <MapPin className="w-4 h-4 shrink-0 text-stone-400" />
                <span className="truncate">{spa.address}</span>
              </div>
            )}
            {spa.phone && (
              <div className="flex items-center gap-2">
                <Phone className="w-4 h-4 shrink-0 text-stone-400" />
                <span>{spa.phone}</span>
              </div>
            )}
          </div>
        </div>
      </header>

      {/* Main Content */}
      <main className="max-w-3xl mx-auto px-4 sm:px-6 lg:px-8 py-8 sm:py-12">
        <SpaBookingFlow slug={slug!} spa={spa} />
      </main>

      {/* Footer */}
      <footer className="mt-auto py-8 text-center text-sm text-stone-400 border-t border-stone-200 bg-white">
        Powered by Spa Booking System
      </footer>
    </div>
  );
}
