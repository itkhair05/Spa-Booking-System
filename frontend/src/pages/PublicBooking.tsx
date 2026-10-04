import { useState, useEffect } from 'react';
import { useParams } from 'react-router-dom';
import { getPublicSpaInfo } from '../lib/api/publicBooking';
import type { PublicSpaInfoResponse } from '../types/publicBooking';
import { SpaBookingFlow } from '../features/public-booking/SpaBookingFlow';
import { SpaLanding } from '../features/public-booking/SpaLanding';
import { AlertCircle } from 'lucide-react';

export default function PublicBooking() {
  const { slug } = useParams<{ slug: string }>();

  const [spa, setSpa] = useState<PublicSpaInfoResponse | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (!slug) return;

    let isMounted = true;
    // eslint-disable-next-line react-hooks/set-state-in-effect
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
      <div className="min-h-screen bg-stone-50 font-sans" aria-busy="true">
        <div className="border-b border-stone-200 bg-white">
          <div className="max-w-5xl mx-auto px-4 sm:px-6 lg:px-8 h-16 flex items-center justify-between">
            <div className="h-5 w-32 bg-stone-100 rounded animate-pulse" />
            <div className="h-9 w-24 bg-stone-100 rounded-xl animate-pulse" />
          </div>
        </div>
        <div className="max-w-3xl mx-auto px-4 sm:px-6 lg:px-8 pt-16">
          <div className="h-4 w-36 bg-stone-100 rounded animate-pulse mb-5" />
          <div className="h-10 w-2/3 bg-stone-100 rounded animate-pulse mb-6" />
          <div className="h-5 w-1/2 bg-stone-100 rounded animate-pulse mb-10" />
          <div className="h-12 w-40 bg-stone-100 rounded-xl animate-pulse" />
        </div>
        <span className="sr-only">Đang tải thông tin spa...</span>
      </div>
    );
  }

  if (error || !spa) {
    return (
      <div className="min-h-screen bg-stone-50 font-sans flex items-center justify-center p-4">
        <div className="max-w-md w-full bg-white rounded-2xl border border-stone-200 p-8 text-center">
          <AlertCircle className="w-12 h-12 text-rose-500 mx-auto mb-4" aria-hidden="true" />
          <h1 className="text-xl font-medium text-stone-900 mb-2">Đã xảy ra lỗi</h1>
          <p className="text-stone-500 mb-6">{error}</p>
          <button
            onClick={() => window.location.reload()}
            className="px-6 py-2.5 bg-stone-900 text-white font-medium rounded-xl hover:bg-stone-800 transition-colors max-sm:min-h-11"
          >
            Thử lại
          </button>
        </div>
      </div>
    );
  }

  return (
    <SpaLanding slug={slug!} spa={spa}>
      <SpaBookingFlow slug={slug!} spa={spa} />
    </SpaLanding>
  );
}
