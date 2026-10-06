import { useState, useEffect, useCallback } from 'react';
import type { DailyScheduleItem } from '../types/schedule';
import type { Staff as StaffType } from '../types/staff';
import { getDailySchedule } from '../lib/api/schedule';
import { Badge } from '../components/ui/Badge';
import { Button } from '../components/ui/Button';
import { EmptyState } from '../components/ui/EmptyState';
import { ErrorState } from '../components/ui/ErrorState';
import {
  Calendar,
  ChevronLeft,
  ChevronRight,
  Clock,
  User,
  Sparkles,
  Phone,
  Tag,
} from 'lucide-react';

interface DailyScheduleViewProps {
  staffList: StaffType[];
  isOwner: boolean;
  currentStaffId?: number | null;
}

const STATUS_CONFIG: Record<
  string,
  { label: string; tone: 'neutral' | 'info' | 'warning' | 'success' | 'danger' }
> = {
  PENDING: { label: 'Chờ xác nhận', tone: 'warning' },
  CONFIRMED: { label: 'Đã xác nhận', tone: 'info' },
  CHECKED_IN: { label: 'Đã đến', tone: 'info' },
  IN_PROGRESS: { label: 'Đang thực hiện', tone: 'warning' },
  COMPLETED: { label: 'Hoàn tất', tone: 'success' },
  CANCELLED: { label: 'Đã hủy', tone: 'danger' },
  NO_SHOW: { label: 'Vắng mặt', tone: 'neutral' },
};

function formatVnd(amount: number): string {
  return new Intl.NumberFormat('vi-VN', {
    style: 'currency',
    currency: 'VND',
  }).format(amount);
}

function formatTime(isoString: string): string {
  if (!isoString) return '';
  const date = new Date(isoString);
  if (isNaN(date.getTime())) {
    // fallback if iso is YYYY-MM-DDTHH:mm:ss
    return isoString.includes('T') ? isoString.split('T')[1].substring(0, 5) : isoString;
  }
  return date.toLocaleTimeString('vi-VN', {
    hour: '2-digit',
    minute: '2-digit',
    hour12: false,
    timeZone: 'Asia/Ho_Chi_Minh',
  });
}

export function DailyScheduleView({
  staffList,
  isOwner,
  currentStaffId,
}: DailyScheduleViewProps) {
  const getTodayString = () => {
    return new Intl.DateTimeFormat('en-CA', {
      timeZone: 'Asia/Ho_Chi_Minh',
      year: 'numeric',
      month: '2-digit',
      day: '2-digit',
    }).format(new Date());
  };

  const [selectedDate, setSelectedDate] = useState<string>(getTodayString);
  const [selectedStaffId, setSelectedStaffId] = useState<number | ''>(
    isOwner ? '' : (currentStaffId || '')
  );
  const [items, setItems] = useState<DailyScheduleItem[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const fetchSchedule = useCallback(async () => {
    setIsLoading(true);
    setError(null);
    try {
      const staffParam = selectedStaffId === '' ? undefined : Number(selectedStaffId);
      const data = await getDailySchedule(selectedDate, staffParam);
      setItems(data);
    } catch (err: unknown) {
      const errorObj = err as { response?: { data?: { message?: string } } };
      setError(errorObj.response?.data?.message || 'Không thể tải lịch trực trong ngày.');
    } finally {
      setIsLoading(false);
    }
  }, [selectedDate, selectedStaffId]);

  useEffect(() => {
    // eslint-disable-next-line react-hooks/set-state-in-effect
    fetchSchedule();
  }, [fetchSchedule]);

  const handlePrevDay = () => {
    const d = new Date(selectedDate);
    d.setDate(d.getDate() - 1);
    setSelectedDate(d.toISOString().split('T')[0]);
  };

  const handleNextDay = () => {
    const d = new Date(selectedDate);
    d.setDate(d.getDate() + 1);
    setSelectedDate(d.toISOString().split('T')[0]);
  };

  const handleToday = () => {
    setSelectedDate(getTodayString());
  };

  const isToday = selectedDate === getTodayString();

  return (
    <div className="space-y-6">
      {/* Controls Bar */}
      <div className="flex flex-col md:flex-row items-stretch md:items-center justify-between gap-4 p-4 rounded-2xl bg-white border border-stone-200 shadow-xs">
        {/* Date Selector */}
        <div className="flex items-center gap-2 flex-wrap">
          <Button variant="secondary" onClick={handlePrevDay} aria-label="Ngày trước">
            <ChevronLeft size={16} />
          </Button>

          <Button
            variant={isToday ? 'primary' : 'secondary'}
            onClick={handleToday}
            className="text-xs font-semibold px-3 py-1.5"
          >
            Hôm nay
          </Button>

          <Button variant="secondary" onClick={handleNextDay} aria-label="Ngày sau">
            <ChevronRight size={16} />
          </Button>

          <div className="relative">
            <input
              type="date"
              value={selectedDate}
              onChange={(e) => setSelectedDate(e.target.value)}
              className="pl-9 pr-3 py-2 text-sm font-medium rounded-xl border border-stone-300 focus:outline-none focus:ring-1 focus:ring-stone-800 text-stone-900 bg-white"
            />
            <Calendar
              size={16}
              className="absolute left-3 top-2.5 text-stone-400 pointer-events-none"
            />
          </div>
        </div>

        {/* Staff Filter (Owner only) */}
        {isOwner && (
          <div className="flex items-center gap-2">
            <label htmlFor="staff-filter-select" className="text-sm font-medium text-stone-600 whitespace-nowrap">
              Kỹ thuật viên:
            </label>
            <select
              id="staff-filter-select"
              value={selectedStaffId}
              onChange={(e) =>
                setSelectedStaffId(e.target.value === '' ? '' : Number(e.target.value))
              }
              className="w-full md:w-56 px-3 py-2 text-sm rounded-xl border border-stone-300 focus:outline-none focus:ring-1 focus:ring-stone-800 bg-white"
            >
              <option value="">Tất cả nhân viên</option>
              {staffList
                .filter((s) => s.isActive && !s.accessToken)
                .map((staff) => (
                  <option key={staff.id} value={staff.id}>
                    {staff.name}
                  </option>
                ))}
            </select>
          </div>
        )}
      </div>

      {/* Content */}
      {isLoading && (
        <div className="space-y-3 animate-pulse">
          {[1, 2, 3, 4].map((i) => (
            <div key={i} className="h-24 bg-stone-100 rounded-2xl border border-stone-200" />
          ))}
        </div>
      )}

      {error && !isLoading && (
        <ErrorState message={error} onRetry={fetchSchedule} />
      )}

      {!isLoading && !error && items.length === 0 && (
        <EmptyState
          icon={<Calendar size={28} className="text-stone-400" />}
          title="Không có lịch hẹn nào"
          description={`Không có lịch hẹn nào được phân công cho ngày ${selectedDate}.`}
        />
      )}

      {!isLoading && !error && items.length > 0 && (
        <div className="space-y-3">
          <div className="text-xs font-semibold text-stone-500 uppercase tracking-wider px-1">
            Tổng cộng: {items.length} ca hẹn
          </div>

          <div className="grid gap-3">
            {items.map((item) => {
              const statusCfg = STATUS_CONFIG[item.status] || {
                label: item.status,
                tone: 'neutral',
              };

              return (
                <div
                  key={item.id}
                  className="p-4 rounded-2xl border border-stone-200 bg-white hover:border-stone-300 transition-all shadow-xs flex flex-col md:flex-row md:items-center justify-between gap-4"
                >
                  {/* Left: Time & Core Info */}
                  <div className="flex items-start sm:items-center gap-4">
                    <div className="px-3 py-2 rounded-xl bg-stone-100 text-stone-900 font-semibold text-sm flex flex-col items-center justify-center shrink-0 min-w-24 text-center">
                      <div className="flex items-center gap-1 text-xs text-stone-500 mb-0.5">
                        <Clock size={12} /> Giờ hẹn
                      </div>
                      <div>
                        {formatTime(item.startTime)} — {formatTime(item.endTime)}
                      </div>
                    </div>

                    <div className="space-y-1">
                      <div className="flex items-center gap-2 flex-wrap">
                        <span className="font-semibold text-stone-900 text-base">
                          {item.serviceName}
                        </span>
                        {item.serviceDuration && (
                          <span className="text-xs px-2 py-0.5 rounded-md bg-stone-100 text-stone-600 font-medium">
                            {item.serviceDuration} phút
                          </span>
                        )}
                        <Badge tone={statusCfg.tone}>{statusCfg.label}</Badge>
                      </div>

                      <div className="flex items-center gap-4 text-xs text-stone-500 flex-wrap">
                        <span className="flex items-center gap-1 font-medium text-stone-700">
                          <User size={13} /> Khách: {item.customerName}
                        </span>
                        {item.customerPhone && (
                          <span className="flex items-center gap-1">
                            <Phone size={13} /> {item.customerPhone}
                          </span>
                        )}
                        {item.bookingCode && (
                          <span className="flex items-center gap-1 text-stone-400">
                            <Tag size={13} /> Mã: {item.bookingCode}
                          </span>
                        )}
                      </div>
                    </div>
                  </div>

                  {/* Right: Staff & Price */}
                  <div className="flex items-center justify-between md:justify-end gap-6 pt-2 md:pt-0 border-t md:border-t-0 border-stone-100">
                    <div className="flex items-center gap-2">
                      <div className="w-8 h-8 rounded-full bg-stone-100 flex items-center justify-center text-stone-600 font-medium text-xs">
                        <Sparkles size={14} />
                      </div>
                      <div>
                        <div className="text-xs text-stone-400">Kỹ thuật viên</div>
                        <div className="text-sm font-semibold text-stone-800">
                          {item.staffName}
                        </div>
                      </div>
                    </div>

                    <div className="text-right">
                      <div className="text-xs text-stone-400">Giá dịch vụ</div>
                      <div className="text-sm font-semibold text-stone-900">
                        {formatVnd(item.price)}
                      </div>
                    </div>
                  </div>
                </div>
              );
            })}
          </div>
        </div>
      )}
    </div>
  );
}
