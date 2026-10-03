import { useState, useEffect } from 'react';
import { useSpaBooking } from './SpaBookingContext';
import { getPublicStaff } from '../../lib/api/publicBooking';
import type { PublicStaffResponse } from '../../types/publicBooking';
import { AlertCircle, User, ArrowLeft } from 'lucide-react';

export function StepStaff() {
  const { slug, state, updateState, setStep } = useSpaBooking();
  const [staffList, setStaffList] = useState<PublicStaffResponse[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    let isMounted = true;
    getPublicStaff(slug)
      .then((data) => {
        if (isMounted) {
          setStaffList(data);
          setLoading(false);
        }
      })
      .catch(() => {
        if (isMounted) {
          setError('Không thể tải danh sách nhân viên.');
          setLoading(false);
        }
      });
    return () => {
      isMounted = false;
    };
  }, [slug]);

  const handleSelect = (staff: PublicStaffResponse) => {
    if (state.staff?.id !== staff.id) {
      updateState({ staff, time: '' });
    } else {
      updateState({ staff });
    }
    setStep(3);
  };

  return (
    <div>
      <div className="flex items-center mb-6">
        <button
          onClick={() => setStep(1)}
          className="mr-3 p-2 -ml-2 rounded-full text-stone-400 hover:text-stone-700 hover:bg-stone-100 transition-colors"
          aria-label="Quay lại"
        >
          <ArrowLeft className="w-5 h-5" />
        </button>
        <h2 className="text-xl sm:text-2xl font-medium text-stone-900">Chọn nhân viên</h2>
      </div>

      {loading ? (
        <div className="py-12 text-center text-stone-500 animate-pulse">Đang tải nhân viên...</div>
      ) : error ? (
        <div className="py-8 text-center text-rose-500">
          <AlertCircle className="w-8 h-8 mx-auto mb-2" />
          <p>{error}</p>
        </div>
      ) : staffList.length === 0 ? (
        <div className="py-12 text-center text-stone-500">Hiện chưa có nhân viên khả dụng.</div>
      ) : (
        <div className="grid gap-4 sm:grid-cols-2 md:grid-cols-3">
          {staffList.map((staff) => (
            <button
              key={staff.id}
              onClick={() => handleSelect(staff)}
              className={`flex items-center p-4 rounded-xl border transition-all text-left ${
                state.staff?.id === staff.id
                  ? 'border-stone-800 bg-stone-50 ring-1 ring-stone-800'
                  : 'border-stone-200 hover:border-stone-300 hover:bg-stone-50'
              }`}
            >
              <div className="w-10 h-10 rounded-full bg-stone-200 flex items-center justify-center text-stone-500 mr-4 shrink-0">
                <User className="w-5 h-5" />
              </div>
              <div>
                <div className="font-medium text-stone-900">{staff.name}</div>
              </div>
            </button>
          ))}
        </div>
      )}
    </div>
  );
}
