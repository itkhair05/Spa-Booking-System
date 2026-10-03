import { useState, useEffect, useMemo } from 'react';
import { useSpaBooking } from './SpaBookingContext';
import { getPublicAvailability } from '../../lib/api/publicBooking';
import { ArrowLeft, Calendar, AlertCircle } from 'lucide-react';

export function StepDateTime() {
  const { slug, state, updateState, setStep } = useSpaBooking();
  const [availability, setAvailability] = useState<string[]>([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const today = useMemo(() => {
    // Force Asia/Ho_Chi_Minh timezone as required by product
    const formatter = new Intl.DateTimeFormat('en-CA', {
      timeZone: 'Asia/Ho_Chi_Minh',
      year: 'numeric',
      month: '2-digit',
      day: '2-digit',
    });
    return formatter.format(new Date());
  }, []);

  const selectedDate = state.date || today;

  useEffect(() => {
    if (!state.service || !state.staff || !selectedDate) return;

    let isMounted = true;
    // eslint-disable-next-line
    setLoading(true);
    setError(null);

    getPublicAvailability(slug, state.service.id, selectedDate, state.staff.id)
      .then((data) => {
        if (isMounted) {
          setAvailability(data);
          setLoading(false);
        }
      })
      .catch(() => {
        if (isMounted) {
          setError('Không thể tải lịch trống. Vui lòng thử lại.');
          setLoading(false);
        }
      });

    return () => {
      isMounted = false;
    };
  }, [slug, state.service, state.staff, selectedDate]);

  const handleDateChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    updateState({ date: e.target.value, time: '' });
  };

  const handleTimeSelect = (t: string) => {
    updateState({ time: t, date: selectedDate });
    setStep(4);
  };

  return (
    <div>
      <div className="flex items-center mb-6">
        <button
          onClick={() => setStep(2)}
          className="mr-3 p-2 -ml-2 rounded-full text-stone-400 hover:text-stone-700 hover:bg-stone-100 transition-colors"
          aria-label="Quay lại"
        >
          <ArrowLeft className="w-5 h-5" />
        </button>
        <h2 className="text-xl sm:text-2xl font-medium text-stone-900">Chọn ngày & giờ</h2>
      </div>

      <div className="mb-8">
        <label className="block text-sm font-medium text-stone-700 mb-2">
          Ngày hẹn
        </label>
        <div className="relative max-w-sm">
          <input
            type="date"
            min={today}
            value={selectedDate}
            onChange={handleDateChange}
            className="w-full pl-10 pr-4 py-3 rounded-xl border border-stone-300 focus:outline-none focus:ring-2 focus:ring-stone-800 focus:border-stone-800 transition-shadow text-stone-900"
          />
          <Calendar className="w-5 h-5 text-stone-400 absolute left-3 top-3.5 pointer-events-none" />
        </div>
      </div>

      <div>
        <label className="block text-sm font-medium text-stone-700 mb-4">
          Khung giờ khả dụng
        </label>

        {loading ? (
          <div className="py-8 text-center text-stone-500 animate-pulse">Đang tìm khung giờ...</div>
        ) : error ? (
          <div className="py-4 text-rose-500 flex items-center">
            <AlertCircle className="w-5 h-5 mr-2" />
            <span>{error}</span>
          </div>
        ) : availability.length === 0 ? (
          <div className="py-8 text-center text-stone-500 bg-stone-50 rounded-xl border border-stone-100">
            Ngày này không còn khung giờ trống. Vui lòng chọn ngày khác.
          </div>
        ) : (
          <div className="grid grid-cols-3 sm:grid-cols-4 md:grid-cols-5 gap-3">
            {availability.map((t) => {
              // Parse time string from YYYY-MM-DDTHH:mm:ss to HH:mm
              const timeString = t.includes('T') ? t.split('T')[1].substring(0, 5) : t.substring(0, 5); // fallback if it's already HH:mm

              return (
                <button
                  key={t}
                  onClick={() => handleTimeSelect(t)}
                  className={`py-3 px-2 text-center rounded-xl font-medium transition-all ${
                    state.time === t
                      ? 'bg-stone-900 text-white shadow-md'
                      : 'bg-white border border-stone-200 text-stone-700 hover:border-stone-800 hover:text-stone-900'
                  }`}
                >
                  {timeString}
                </button>
              );
            })}
          </div>
        )}
      </div>
    </div>
  );
}
