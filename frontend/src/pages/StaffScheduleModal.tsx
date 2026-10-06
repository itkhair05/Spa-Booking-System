import { useState, useEffect, useCallback } from 'react';
import type { Staff as StaffType } from '../types/staff';
import type { StaffWorkingHours, StaffDayOff, DayOfWeek } from '../types/schedule';
import {
  getStaffWorkingHours,
  updateStaffWorkingHours,
  getStaffDaysOff,
  addStaffDayOff,
  deleteStaffDayOff,
} from '../lib/api/schedule';
import { Button } from '../components/ui/Button';
import { Alert } from '../components/ui/Alert';
import { X, Calendar, Clock, Trash2, Plus, CalendarOff, CheckCircle2 } from 'lucide-react';

interface StaffScheduleModalProps {
  staff: StaffType | null;
  isOpen: boolean;
  onClose: () => void;
  isOwner: boolean;
}

const DAY_LABELS: Record<DayOfWeek, string> = {
  MONDAY: 'Thứ Hai',
  TUESDAY: 'Thứ Ba',
  WEDNESDAY: 'Thứ Tư',
  THURSDAY: 'Thứ Năm',
  FRIDAY: 'Thứ Sáu',
  SATURDAY: 'Thứ Bảy',
  SUNDAY: 'Chủ Nhật',
};

const ORDERED_DAYS: DayOfWeek[] = [
  'MONDAY',
  'TUESDAY',
  'WEDNESDAY',
  'THURSDAY',
  'FRIDAY',
  'SATURDAY',
  'SUNDAY',
];

export function StaffScheduleModal({ staff, isOpen, onClose, isOwner }: StaffScheduleModalProps) {
  const [activeTab, setActiveTab] = useState<'workingHours' | 'daysOff'>('workingHours');

  // Working Hours State
  const [workingHours, setWorkingHours] = useState<StaffWorkingHours[]>([]);
  const [whLoading, setWhLoading] = useState(false);
  const [whSaving, setWhSaving] = useState(false);
  const [whError, setWhError] = useState<string | null>(null);
  const [whSuccess, setWhSuccess] = useState<string | null>(null);

  // Days Off State
  const [daysOff, setDaysOff] = useState<StaffDayOff[]>([]);
  const [doLoading, setDoLoading] = useState(false);
  const [doError, setDoError] = useState<string | null>(null);
  const [doSuccess, setDoSuccess] = useState<string | null>(null);

  // New Day Off form
  const [newDate, setNewDate] = useState('');
  const [newReason, setNewReason] = useState('');
  const [isAddingDayOff, setIsAddingDayOff] = useState(false);

  const fetchScheduleData = useCallback(async () => {
    if (!staff) return;
    setWhLoading(true);
    setDoLoading(true);
    setWhError(null);
    setDoError(null);
    try {
      const [whData, doData] = await Promise.all([
        getStaffWorkingHours(staff.id),
        getStaffDaysOff(staff.id),
      ]);

      // Ensure all 7 days exist in ordered array
      const whMap = new Map(whData.map((w) => [w.dayOfWeek, w]));
      const completeList: StaffWorkingHours[] = ORDERED_DAYS.map((day) => {
        const existing = whMap.get(day);
        if (existing) {
          // Format start and end time to HH:mm for inputs
          return {
            ...existing,
            startTime: existing.startTime.substring(0, 5),
            endTime: existing.endTime.substring(0, 5),
          };
        }
        return {
          dayOfWeek: day,
          startTime: '09:00',
          endTime: '18:00',
          isActive: day !== 'SUNDAY',
        };
      });

      setWorkingHours(completeList);
      setDaysOff(doData);
    } catch (err: unknown) {
      const errorObj = err as { response?: { data?: { message?: string } } };
      setWhError(errorObj.response?.data?.message || 'Không thể tải thông tin lịch làm việc.');
    } finally {
      setWhLoading(false);
      setDoLoading(false);
    }
  }, [staff]);

  useEffect(() => {
    if (isOpen && staff) {
      // eslint-disable-next-line react-hooks/set-state-in-effect
      fetchScheduleData();
      setWhSuccess(null);
      setDoSuccess(null);
      setNewDate('');
      setNewReason('');
    }
  }, [isOpen, staff, fetchScheduleData]);

  if (!isOpen || !staff) return null;

  const handleWorkingHoursChange = (
    dayOfWeek: DayOfWeek,
    field: 'startTime' | 'endTime' | 'isActive',
    value: string | boolean
  ) => {
    setWorkingHours((prev) =>
      prev.map((item) => {
        if (item.dayOfWeek === dayOfWeek) {
          return { ...item, [field]: value };
        }
        return item;
      })
    );
  };

  const handleSaveWorkingHours = async () => {
    if (!isOwner) return;
    setWhSaving(true);
    setWhError(null);
    setWhSuccess(null);
    try {
      // Validate time boundaries
      for (const item of workingHours) {
        if (item.isActive) {
          if (!item.startTime || !item.endTime) {
            throw new Error(`Vui lòng nhập đầy đủ giờ làm việc cho ${DAY_LABELS[item.dayOfWeek]}`);
          }
          if (item.startTime >= item.endTime) {
            throw new Error(
              `Giờ bắt đầu phải trước giờ kết thúc cho ${DAY_LABELS[item.dayOfWeek]}`
            );
          }
        }
      }

      const formatted = workingHours.map((w) => ({
        dayOfWeek: w.dayOfWeek,
        startTime: w.startTime.length === 5 ? `${w.startTime}:00` : w.startTime,
        endTime: w.endTime.length === 5 ? `${w.endTime}:00` : w.endTime,
        isActive: w.isActive,
      }));

      await updateStaffWorkingHours(staff.id, { workingHours: formatted });
      setWhSuccess('Lưu lịch làm việc thành công!');
      setTimeout(() => setWhSuccess(null), 3000);
    } catch (err: unknown) {
      const errorObj = err as { response?: { data?: { message?: string } }; message?: string };
      setWhError(
        errorObj.response?.data?.message ||
          errorObj.message ||
          'Không thể cập nhật lịch làm việc.'
      );
    } finally {
      setWhSaving(false);
    }
  };

  const handleAddDayOff = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!isOwner || !newDate) return;
    setIsAddingDayOff(true);
    setDoError(null);
    setDoSuccess(null);
    try {
      await addStaffDayOff(staff.id, {
        date: newDate,
        reason: newReason.trim() || undefined,
      });
      setNewDate('');
      setNewReason('');
      setDoSuccess('Thêm ngày nghỉ thành công!');
      setTimeout(() => setDoSuccess(null), 3000);
      const updatedDaysOff = await getStaffDaysOff(staff.id);
      setDaysOff(updatedDaysOff);
    } catch (err: unknown) {
      const errorObj = err as { response?: { data?: { message?: string } } };
      setDoError(errorObj.response?.data?.message || 'Không thể thêm ngày nghỉ.');
    } finally {
      setIsAddingDayOff(false);
    }
  };

  const handleDeleteDayOff = async (dayOffId: number) => {
    if (!isOwner) return;
    setDoError(null);
    try {
      await deleteStaffDayOff(staff.id, dayOffId);
      setDaysOff((prev) => prev.filter((d) => d.id !== dayOffId));
      setDoSuccess('Đã xóa ngày nghỉ.');
      setTimeout(() => setDoSuccess(null), 3000);
    } catch (err: unknown) {
      const errorObj = err as { response?: { data?: { message?: string } } };
      setDoError(errorObj.response?.data?.message || 'Không thể xóa ngày nghỉ.');
    }
  };

  const todayStr = new Intl.DateTimeFormat('en-CA', {
    timeZone: 'Asia/Ho_Chi_Minh',
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
  }).format(new Date());

  return (
    <div
      className="fixed inset-0 z-50 overflow-y-auto bg-stone-900/60 backdrop-blur-xs flex items-center justify-center p-3 sm:p-4"
      role="dialog"
      aria-modal="true"
      aria-labelledby="schedule-modal-title"
    >
      <div className="bg-white rounded-2xl shadow-xl w-full max-w-2xl max-h-[90vh] flex flex-col overflow-hidden border border-stone-200 animate-in fade-in zoom-in-95 duration-150">
        {/* Header */}
        <div className="flex items-center justify-between px-6 py-4 border-b border-stone-200 bg-stone-50/50">
          <div>
            <h3 id="schedule-modal-title" className="text-lg font-semibold text-stone-900">
              Lịch làm việc & Ngày nghỉ
            </h3>
            <p className="text-sm text-stone-500">
              Nhân viên: <span className="font-medium text-stone-800">{staff.name}</span>
            </p>
          </div>
          <button
            onClick={onClose}
            className="p-2 text-stone-400 hover:text-stone-700 hover:bg-stone-100 rounded-lg transition-colors"
            aria-label="Đóng"
          >
            <X size={20} />
          </button>
        </div>

        {/* Tab Navigation */}
        <div className="flex border-b border-stone-200 px-6 pt-2 bg-stone-50/30">
          <button
            onClick={() => setActiveTab('workingHours')}
            className={`flex items-center gap-2 py-3 px-4 text-sm font-medium border-b-2 transition-colors ${
              activeTab === 'workingHours'
                ? 'border-stone-900 text-stone-900'
                : 'border-transparent text-stone-500 hover:text-stone-800'
            }`}
          >
            <Clock size={16} />
            Lịch làm việc tuần
          </button>
          <button
            onClick={() => setActiveTab('daysOff')}
            className={`flex items-center gap-2 py-3 px-4 text-sm font-medium border-b-2 transition-colors ${
              activeTab === 'daysOff'
                ? 'border-stone-900 text-stone-900'
                : 'border-transparent text-stone-500 hover:text-stone-800'
            }`}
          >
            <CalendarOff size={16} />
            Ngày nghỉ phép ({daysOff.length})
          </button>
        </div>

        {/* Tab Content */}
        <div className="flex-1 overflow-y-auto p-6 space-y-4">
          {activeTab === 'workingHours' && (
            <div>
              {whError && <Alert tone="error" className="mb-4">{whError}</Alert>}
              {whSuccess && <Alert tone="success" className="mb-4">{whSuccess}</Alert>}

              {whLoading ? (
                <div className="space-y-3 animate-pulse">
                  {[1, 2, 3, 4, 5, 6, 7].map((i) => (
                    <div key={i} className="h-12 bg-stone-100 rounded-xl" />
                  ))}
                </div>
              ) : (
                <div className="space-y-3">
                  <div className="hidden sm:grid sm:grid-cols-12 gap-3 text-xs font-semibold text-stone-500 uppercase tracking-wider px-3 pb-1">
                    <span className="col-span-4">Thứ trong tuần</span>
                    <span className="col-span-3">Trạng thái</span>
                    <span className="col-span-5">Khung giờ làm việc</span>
                  </div>

                  {workingHours.map((wh) => (
                    <div
                      key={wh.dayOfWeek}
                      className={`p-3 rounded-xl border transition-all ${
                        wh.isActive
                          ? 'border-stone-200 bg-white'
                          : 'border-stone-200/60 bg-stone-50/70 text-stone-400'
                      }`}
                    >
                      <div className="flex flex-col sm:grid sm:grid-cols-12 gap-3 sm:items-center">
                        {/* Day Name */}
                        <div className="col-span-4 font-medium text-stone-900 flex items-center justify-between sm:justify-start gap-2">
                          <span>{DAY_LABELS[wh.dayOfWeek]}</span>
                          {!wh.isActive && (
                            <span className="text-xs bg-stone-200 text-stone-600 px-2 py-0.5 rounded-full sm:hidden">
                              Nghỉ
                            </span>
                          )}
                        </div>

                        {/* Active Checkbox */}
                        <div className="col-span-3 flex items-center">
                          {isOwner ? (
                            <label className="flex items-center gap-2 cursor-pointer text-sm">
                              <input
                                type="checkbox"
                                checked={wh.isActive}
                                onChange={(e) =>
                                  handleWorkingHoursChange(wh.dayOfWeek, 'isActive', e.target.checked)
                                }
                                className="w-4 h-4 rounded border-stone-300 text-stone-900 focus:ring-stone-900"
                              />
                              <span className={wh.isActive ? 'text-stone-700' : 'text-stone-400'}>
                                {wh.isActive ? 'Làm việc' : 'Nghỉ'}
                              </span>
                            </label>
                          ) : (
                            <span
                              className={`text-xs px-2.5 py-1 rounded-full font-medium ${
                                wh.isActive
                                  ? 'bg-emerald-50 text-emerald-700 border border-emerald-200'
                                  : 'bg-stone-100 text-stone-600'
                              }`}
                            >
                              {wh.isActive ? 'Làm việc' : 'Nghỉ'}
                            </span>
                          )}
                        </div>

                        {/* Time Inputs */}
                        <div className="col-span-5 flex items-center gap-2">
                          {wh.isActive ? (
                            isOwner ? (
                              <div className="flex items-center gap-2 w-full">
                                <input
                                  type="time"
                                  value={wh.startTime}
                                  onChange={(e) =>
                                    handleWorkingHoursChange(wh.dayOfWeek, 'startTime', e.target.value)
                                  }
                                  className="w-full px-2.5 py-1.5 text-sm rounded-lg border border-stone-300 focus:outline-none focus:ring-1 focus:ring-stone-800"
                                />
                                <span className="text-stone-400">—</span>
                                <input
                                  type="time"
                                  value={wh.endTime}
                                  onChange={(e) =>
                                    handleWorkingHoursChange(wh.dayOfWeek, 'endTime', e.target.value)
                                  }
                                  className="w-full px-2.5 py-1.5 text-sm rounded-lg border border-stone-300 focus:outline-none focus:ring-1 focus:ring-stone-800"
                                />
                              </div>
                            ) : (
                              <span className="text-sm font-medium text-stone-800">
                                {wh.startTime} — {wh.endTime}
                              </span>
                            )
                          ) : (
                            <span className="text-xs text-stone-400 italic">Không có ca làm việc</span>
                          )}
                        </div>
                      </div>
                    </div>
                  ))}
                </div>
              )}
            </div>
          )}

          {activeTab === 'daysOff' && (
            <div>
              {doError && <Alert tone="error" className="mb-4">{doError}</Alert>}
              {doSuccess && <Alert tone="success" className="mb-4">{doSuccess}</Alert>}

              {/* Add Day Off Form (Owner only) */}
              {isOwner && (
                <form
                  onSubmit={handleAddDayOff}
                  className="p-4 mb-6 rounded-xl border border-stone-200 bg-stone-50/50 space-y-3"
                >
                  <h4 className="text-sm font-semibold text-stone-800 flex items-center gap-1.5">
                    <Plus size={16} /> Thêm ngày nghỉ mới
                  </h4>
                  <div className="grid grid-cols-1 sm:grid-cols-12 gap-3">
                    <div className="sm:col-span-5">
                      <label htmlFor="day-off-date" className="block text-xs font-medium text-stone-600 mb-1">
                        Ngày nghỉ *
                      </label>
                      <input
                        id="day-off-date"
                        type="date"
                        min={todayStr}
                        value={newDate}
                        onChange={(e) => setNewDate(e.target.value)}
                        required
                        className="w-full px-3 py-2 text-sm rounded-lg border border-stone-300 focus:outline-none focus:ring-1 focus:ring-stone-800 bg-white"
                      />
                    </div>
                    <div className="sm:col-span-5">
                      <label htmlFor="day-off-reason" className="block text-xs font-medium text-stone-600 mb-1">
                        Lý do (tùy chọn)
                      </label>
                      <input
                        id="day-off-reason"
                        type="text"
                        placeholder="Nghỉ phép cá nhân, du lịch, nghỉ lễ..."
                        value={newReason}
                        onChange={(e) => setNewReason(e.target.value)}
                        className="w-full px-3 py-2 text-sm rounded-lg border border-stone-300 focus:outline-none focus:ring-1 focus:ring-stone-800 bg-white"
                      />
                    </div>
                    <div className="sm:col-span-2 flex items-end">
                      <Button
                        type="submit"
                        disabled={isAddingDayOff || !newDate}
                        className="w-full"
                      >
                        {isAddingDayOff ? 'Đang thêm...' : 'Thêm'}
                      </Button>
                    </div>
                  </div>
                </form>
              )}

              {/* Days Off List */}
              {doLoading ? (
                <div className="space-y-2 animate-pulse">
                  {[1, 2, 3].map((i) => (
                    <div key={i} className="h-12 bg-stone-100 rounded-xl" />
                  ))}
                </div>
              ) : daysOff.length === 0 ? (
                <div className="py-8 text-center text-stone-500 bg-stone-50 rounded-xl border border-stone-100">
                  <Calendar className="w-8 h-8 mx-auto mb-2 text-stone-400" />
                  <p className="text-sm">Chưa có ngày nghỉ nào được đăng ký cho nhân viên này.</p>
                </div>
              ) : (
                <div className="space-y-2">
                  {daysOff.map((item) => (
                    <div
                      key={item.id}
                      className="flex items-center justify-between p-3.5 rounded-xl border border-stone-200 bg-white hover:border-stone-300 transition-colors"
                    >
                      <div className="flex items-center gap-3">
                        <div className="w-9 h-9 rounded-lg bg-rose-50 text-rose-600 flex items-center justify-center shrink-0">
                          <CalendarOff size={18} />
                        </div>
                        <div>
                          <div className="text-sm font-semibold text-stone-900">
                            {item.date}
                          </div>
                          <div className="text-xs text-stone-500">
                            {item.reason || 'Nghỉ phép'}
                          </div>
                        </div>
                      </div>

                      {isOwner && (
                        <button
                          onClick={() => handleDeleteDayOff(item.id)}
                          className="p-2 text-stone-400 hover:text-rose-600 hover:bg-rose-50 rounded-lg transition-colors"
                          title="Hủy ngày nghỉ"
                          aria-label="Hủy ngày nghỉ"
                        >
                          <Trash2 size={16} />
                        </button>
                      )}
                    </div>
                  ))}
                </div>
              )}
            </div>
          )}
        </div>

        {/* Footer */}
        <div className="px-6 py-4 border-t border-stone-200 bg-stone-50/50 flex items-center justify-between">
          <Button variant="secondary" onClick={onClose}>
            Đóng
          </Button>

          {activeTab === 'workingHours' && isOwner && (
            <Button
              onClick={handleSaveWorkingHours}
              disabled={whSaving || whLoading}
            >
              <CheckCircle2 size={16} />
              {whSaving ? 'Đang lưu...' : 'Lưu lịch làm việc'}
            </Button>
          )}
        </div>
      </div>
    </div>
  );
}
