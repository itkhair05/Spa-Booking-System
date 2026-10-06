import { useState, useEffect, useCallback, useMemo } from 'react';
import AppShell from '../components/AppShell';
import { useAuth } from '../app/auth/useAuth';
import { PageHeader } from '../components/ui/PageHeader';
import { Card, CardContent } from '../components/ui/Card';
import { Button } from '../components/ui/Button';
import { Badge } from '../components/ui/Badge';
import { Alert } from '../components/ui/Alert';
import { EmptyState } from '../components/ui/EmptyState';
import { ErrorState } from '../components/ui/ErrorState';
import {
  getBookings,
  getBookingById,
  confirmBooking,
  checkInBooking,
  startBooking,
  completeBooking,
  cancelBooking,
  noShowBooking,
  rescheduleBooking,
  assignBookingStaff
} from '../lib/api/bookings';
import { getRefundEligibility, initiateRefund } from '../lib/api/refund';
import { getStaff } from '../lib/api/staff';
import type { Booking, BookingDetail, BookingStatus } from '../types/booking';
import type { RefundEligibilityResponse } from '../types/refund';
import type { Staff } from '../types/staff';
import { BookingForm } from './BookingForm';
import { formatCurrency, formatTimeRange } from '../lib/format';
import type { BadgeTone } from '../components/ui/Badge';
import {
  Calendar,
  Clock,
  Plus,
  UserRound,
  Search,
  UserCheck,
  Eye,
  X,
  Phone,
  Mail,
  AlertCircle,
  Copy,
  Check,
  CreditCard,
  Sparkles,
  CalendarClock,
  UserX,
  Play,
  CheckCircle2,
  Ban,
  RotateCcw
} from 'lucide-react';

const STATUS_CONFIG: Record<BookingStatus, { label: string; tone: BadgeTone }> = {
  PENDING: { label: 'Chờ xác nhận', tone: 'warning' },
  CONFIRMED: { label: 'Đã xác nhận', tone: 'info' },
  CHECKED_IN: { label: 'Đã check-in', tone: 'info' },
  IN_PROGRESS: { label: 'Đang thực hiện', tone: 'info' },
  COMPLETED: { label: 'Đã hoàn thành', tone: 'success' },
  CANCELLED: { label: 'Đã hủy', tone: 'neutral' },
  NO_SHOW: { label: 'Không đến', tone: 'neutral' },
};

type FilterStatus = 'ALL' | BookingStatus;

const FILTER_TABS: FilterStatus[] = [
  'ALL',
  'PENDING',
  'CONFIRMED',
  'CHECKED_IN',
  'IN_PROGRESS',
  'COMPLETED',
  'CANCELLED',
  'NO_SHOW',
];

const extractDateAndTimeString = (isoString?: string) => {
  if (!isoString) return { date: '', time: '' };
  try {
    const d = new Date(isoString);
    const pad = (n: number) => String(n).padStart(2, '0');
    const date = `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}`;
    const time = `${pad(d.getHours())}:${pad(d.getMinutes())}`;
    return { date, time };
  } catch {
    return { date: '', time: '' };
  }
};

const Bookings = () => {
  const { user } = useAuth();
  const isOwner = user?.roles?.includes('ROLE_OWNER') ?? false;

  const [bookings, setBookings] = useState<Booking[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  // Filters & search
  const [statusFilter, setStatusFilter] = useState<FilterStatus>('ALL');
  const [searchQuery, setSearchQuery] = useState('');

  // Form state (Create / Edit)
  const [isFormOpen, setIsFormOpen] = useState(false);
  const [editingBooking, setEditingBooking] = useState<Booking | undefined>(undefined);

  // Booking Detail Modal
  const [detailBooking, setDetailBooking] = useState<BookingDetail | null>(null);
  const [detailLoading, setDetailLoading] = useState(false);
  const [copiedCode, setCopiedCode] = useState(false);

  // Assignment Modal
  const [assigningBooking, setAssigningBooking] = useState<Booking | null>(null);
  const [activeStaffList, setActiveStaffList] = useState<Staff[]>([]);
  const [selectedStaffId, setSelectedStaffId] = useState<number | null>(null);
  const [assignSubmitting, setAssignSubmitting] = useState(false);
  const [assignError, setAssignError] = useState<string | null>(null);

  // Operation in flight
  const [busyOp, setBusyOp] = useState<{ id: number; action: string } | null>(null);
  const [actionError, setActionError] = useState<string | null>(null);

  // Reschedule Modal
  const [rescheduleTarget, setRescheduleTarget] = useState<Booking | BookingDetail | null>(null);
  const [rescheduleDate, setRescheduleDate] = useState('');
  const [rescheduleTime, setRescheduleTime] = useState('');
  const [rescheduleStaffId, setRescheduleStaffId] = useState<number | null>(null);
  const [rescheduleSubmitting, setRescheduleSubmitting] = useState(false);
  const [rescheduleError, setRescheduleError] = useState<string | null>(null);

  // Cancel Modal
  const [cancelTarget, setCancelTarget] = useState<Booking | BookingDetail | null>(null);
  const [cancelReason, setCancelReason] = useState('');
  const [cancelSubmitting, setCancelSubmitting] = useState(false);
  const [cancelError, setCancelError] = useState<string | null>(null);
  const [cancelEligibility, setCancelEligibility] = useState<RefundEligibilityResponse | null>(null);
  const [cancelEligibilityLoading, setCancelEligibilityLoading] = useState(false);

  // Refund Modal (Owner only)
  const [refundTarget, setRefundTarget] = useState<BookingDetail | null>(null);
  const [refundEligibility, setRefundEligibility] = useState<RefundEligibilityResponse | null>(null);
  const [refundLoading, setRefundLoading] = useState(false);
  const [refundReason, setRefundReason] = useState('');
  const [refundSubmitting, setRefundSubmitting] = useState(false);
  const [refundError, setRefundError] = useState<string | null>(null);

  // No-Show Modal
  const [noShowTarget, setNoShowTarget] = useState<Booking | BookingDetail | null>(null);
  const [noShowSubmitting, setNoShowSubmitting] = useState(false);
  const [noShowError, setNoShowError] = useState<string | null>(null);

  const fetchBookings = useCallback(async () => {
    setIsLoading(true);
    setError(null);
    try {
      const data = await getBookings();
      setBookings(data);
    } catch (err: unknown) {
      const errorObj = err as { response?: { data?: { message?: string } } };
      setError(errorObj.response?.data?.message || 'Không thể tải dữ liệu lịch hẹn.');
    } finally {
      setIsLoading(false);
    }
  }, []);

  useEffect(() => {
    // eslint-disable-next-line react-hooks/set-state-in-effect
    fetchBookings();
  }, [fetchBookings]);

  const loadStaffForAssignment = async () => {
    try {
      const allStaff = await getStaff();
      setActiveStaffList(allStaff.filter((s) => s.isActive));
    } catch {
      setActiveStaffList([]);
    }
  };

  const handleOpenForm = (booking?: Booking) => {
    setEditingBooking(booking);
    setIsFormOpen(true);
  };

  const handleCloseForm = () => {
    setIsFormOpen(false);
    setEditingBooking(undefined);
  };

  const handleFormSuccess = () => {
    handleCloseForm();
    fetchBookings();
  };

  const handleOpenDetail = async (id: number) => {
    setDetailLoading(true);
    setDetailBooking(null);
    try {
      const detail = await getBookingById(id);
      setDetailBooking(detail);
    } catch {
      // Fallback
    } finally {
      setDetailLoading(false);
    }
  };

  const handleOpenAssign = async (booking: Booking | BookingDetail) => {
    setAssigningBooking(booking as Booking);
    setSelectedStaffId(booking.staffId || null);
    setAssignError(null);
    await loadStaffForAssignment();
  };

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

  const handleConfirmAssign = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!assigningBooking || !selectedStaffId) return;

    setAssignSubmitting(true);
    setAssignError(null);
    try {
      await assignBookingStaff(assigningBooking.id, selectedStaffId);
      setAssigningBooking(null);
      await fetchBookings();
      if (detailBooking && detailBooking.id === assigningBooking.id) {
        handleOpenDetail(detailBooking.id);
      }
    } catch (err: unknown) {
      const axiosErr = err as { response?: { status: number; data?: { message?: string } } };
      if (axiosErr.response?.status === 409) {
        setAssignError('Kỹ thuật viên này đã có lịch hẹn trùng giờ trong khoảng thời gian đã chọn.');
      } else if (axiosErr.response?.status === 400) {
        setAssignError(axiosErr.response.data?.message || 'Không thể phân công: kỹ thuật viên không hợp lệ hoặc lịch đã kết thúc.');
      } else {
        setAssignError('Không thể phân công nhân viên. Vui lòng thử lại sau.');
      }
    } finally {
      setAssignSubmitting(false);
    }
  };

  const handleCopyCode = async (text: string) => {
    try {
      await navigator.clipboard.writeText(text);
      setCopiedCode(true);
      setTimeout(() => setCopiedCode(false), 2000);
    } catch {
      setCopiedCode(false);
    }
  };

  // Operation Actions
  const handleConfirmBooking = async (id: number) => {
    setBusyOp({ id, action: 'confirm' });
    setActionError(null);
    try {
      await confirmBooking(id);
      await fetchBookings();
      if (detailBooking && detailBooking.id === id) {
        handleOpenDetail(id);
      }
    } catch (err: unknown) {
      const errorObj = err as { response?: { data?: { message?: string } } };
      setActionError(errorObj.response?.data?.message || 'Không thể xác nhận lịch hẹn.');
    } finally {
      setBusyOp(null);
    }
  };

  const handleCheckIn = async (id: number) => {
    setBusyOp({ id, action: 'checkin' });
    setActionError(null);
    try {
      await checkInBooking(id);
      await fetchBookings();
      if (detailBooking && detailBooking.id === id) {
        handleOpenDetail(id);
      }
    } catch (err: unknown) {
      const errorObj = err as { response?: { data?: { message?: string } } };
      setActionError(errorObj.response?.data?.message || 'Không thể check-in lịch hẹn.');
    } finally {
      setBusyOp(null);
    }
  };

  const handleStart = async (id: number) => {
    setBusyOp({ id, action: 'start' });
    setActionError(null);
    try {
      await startBooking(id);
      await fetchBookings();
      if (detailBooking && detailBooking.id === id) {
        handleOpenDetail(id);
      }
    } catch (err: unknown) {
      const errorObj = err as { response?: { data?: { message?: string } } };
      setActionError(errorObj.response?.data?.message || 'Không thể bắt đầu thực hiện liệu trình.');
    } finally {
      setBusyOp(null);
    }
  };

  const handleComplete = async (id: number) => {
    setBusyOp({ id, action: 'complete' });
    setActionError(null);
    try {
      await completeBooking(id);
      await fetchBookings();
      if (detailBooking && detailBooking.id === id) {
        handleOpenDetail(id);
      }
    } catch (err: unknown) {
      const errorObj = err as { response?: { data?: { message?: string } } };
      setActionError(errorObj.response?.data?.message || 'Không thể hoàn tất lịch hẹn.');
    } finally {
      setBusyOp(null);
    }
  };

  // Open Reschedule Modal
  const handleOpenReschedule = async (booking: Booking | BookingDetail) => {
    const { date, time } = extractDateAndTimeString(booking.startTime);
    setRescheduleTarget(booking);
    setRescheduleDate(date);
    setRescheduleTime(time);
    setRescheduleStaffId(booking.staffId || null);
    setRescheduleError(null);
    if (isOwner && activeStaffList.length === 0) {
      await loadStaffForAssignment();
    }
  };

  const handleConfirmReschedule = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!rescheduleTarget || !rescheduleDate || !rescheduleTime) {
      setRescheduleError('Vui lòng chọn ngày và giờ hẹn mới.');
      return;
    }

    setRescheduleSubmitting(true);
    setRescheduleError(null);
    try {
      const newStartTime = `${rescheduleDate}T${rescheduleTime}:00`;
      await rescheduleBooking(rescheduleTarget.id, {
        startTime: newStartTime,
        staffId: isOwner ? (rescheduleStaffId || undefined) : undefined,
      });
      setRescheduleTarget(null);
      await fetchBookings();
      if (detailBooking && detailBooking.id === rescheduleTarget.id) {
        handleOpenDetail(detailBooking.id);
      }
    } catch (err: unknown) {
      const errorObj = err as { response?: { status?: number; data?: { message?: string } } };
      if (errorObj.response?.status === 409) {
        setRescheduleError(errorObj.response.data?.message || 'Thời gian đã chọn bị trùng lịch hẹn khác.');
      } else {
        setRescheduleError(errorObj.response?.data?.message || 'Không thể dời lịch hẹn. Vui lòng kiểm tra lại thời gian.');
      }
    } finally {
      setRescheduleSubmitting(false);
    }
  };

  // Open Cancel Modal
  const handleOpenCancel = async (booking: Booking | BookingDetail) => {
    setCancelTarget(booking);
    setCancelReason('');
    setCancelError(null);
    setCancelEligibility(null);

    // If paid, preview policy calculation
    if (booking.price > 0) {
      setCancelEligibilityLoading(true);
      try {
        const elig = await getRefundEligibility(booking.id);
        setCancelEligibility(elig);
      } catch {
        // Silently skip if not eligible or error
      } finally {
        setCancelEligibilityLoading(false);
      }
    }
  };

  const handleConfirmCancel = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!cancelTarget) return;

    setCancelSubmitting(true);
    setCancelError(null);
    try {
      await cancelBooking(cancelTarget.id, {
        reason: cancelReason.trim() ? cancelReason.trim() : undefined,
      });
      setCancelTarget(null);
      await fetchBookings();
      if (detailBooking && detailBooking.id === cancelTarget.id) {
        handleOpenDetail(detailBooking.id);
      }
    } catch (err: unknown) {
      const errorObj = err as { response?: { data?: { message?: string } } };
      setCancelError(errorObj.response?.data?.message || 'Không thể hủy lịch hẹn.');
    } finally {
      setCancelSubmitting(false);
    }
  };

  // Open Refund Modal (Owner only)
  const handleOpenRefund = async (booking: BookingDetail) => {
    setRefundTarget(booking);
    setRefundReason(booking.cancellationReason || 'Khách yêu cầu hủy');
    setRefundError(null);
    setRefundEligibility(null);
    setRefundLoading(true);
    try {
      const elig = await getRefundEligibility(booking.id);
      setRefundEligibility(elig);
    } catch (err: unknown) {
      const errorObj = err as { response?: { data?: { message?: string } } };
      setRefundError(errorObj.response?.data?.message || 'Không thể tải điều kiện hoàn tiền.');
    } finally {
      setRefundLoading(false);
    }
  };

  const handleConfirmRefund = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!refundTarget) return;

    setRefundSubmitting(true);
    setRefundError(null);
    try {
      await initiateRefund(refundTarget.id, {
        reason: refundReason.trim() || undefined,
      });
      const targetId = refundTarget.id;
      setRefundTarget(null);
      await fetchBookings();
      handleOpenDetail(targetId);
    } catch (err: unknown) {
      const errorObj = err as { response?: { data?: { message?: string } } };
      setRefundError(errorObj.response?.data?.message || 'Không thể thực hiện yêu cầu hoàn tiền.');
    } finally {
      setRefundSubmitting(false);
    }
  };

  // Open No-Show Modal
  const handleOpenNoShow = (booking: Booking | BookingDetail) => {
    setNoShowTarget(booking);
    setNoShowError(null);
  };

  const handleConfirmNoShow = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!noShowTarget) return;

    setNoShowSubmitting(true);
    setNoShowError(null);
    try {
      await noShowBooking(noShowTarget.id);
      setNoShowTarget(null);
      await fetchBookings();
      if (detailBooking && detailBooking.id === noShowTarget.id) {
        handleOpenDetail(noShowTarget.id);
      }
    } catch (err: unknown) {
      const errorObj = err as { response?: { data?: { message?: string } } };
      setNoShowError(errorObj.response?.data?.message || 'Không thể đánh dấu khách không đến.');
    } finally {
      setNoShowSubmitting(false);
    }
  };

  // Filter & Search logic
  const filteredBookings = useMemo(() => {
    return bookings.filter((b) => {
      const matchesStatus = statusFilter === 'ALL' || b.status === statusFilter;
      const query = searchQuery.trim().toLowerCase();
      if (!query) return matchesStatus;

      const matchesQuery =
        b.customerName?.toLowerCase().includes(query) ||
        b.serviceName?.toLowerCase().includes(query) ||
        b.staffName?.toLowerCase().includes(query) ||
        (b.bookingCode && b.bookingCode.toLowerCase().includes(query));

      return matchesStatus && matchesQuery;
    });
  }, [bookings, statusFilter, searchQuery]);

  if (isFormOpen) {
    return (
      <AppShell title={isOwner ? 'Quản lý lịch hẹn' : 'Lịch của tôi'}>
        <PageHeader title={editingBooking ? 'Chỉnh sửa lịch hẹn' : 'Tạo lịch hẹn mới'} />
        <div className="max-w-2xl mx-auto">
          <BookingForm
            booking={editingBooking}
            onSuccess={handleFormSuccess}
            onCancel={handleCloseForm}
          />
        </div>
      </AppShell>
    );
  }

  return (
    <AppShell title={isOwner ? 'Quản lý lịch hẹn' : 'Lịch của tôi'}>
      {/* Header */}
      <div className="flex flex-col sm:flex-row justify-between items-start sm:items-center mb-6 gap-4">
        <PageHeader
          title={isOwner ? 'Quản lý Lịch hẹn' : 'Lịch hẹn của tôi'}
          description={
            isOwner
              ? 'Theo dõi, phê duyệt và vận hành quy trình lịch hẹn tại TIKEY SPA.'
              : 'Danh sách các cuộc hẹn chăm sóc khách hàng được phân công cho bạn.'
          }
        />
        {isOwner && (
          <Button onClick={() => handleOpenForm()}>
            <Plus aria-hidden="true" size={16} />
            Tạo lịch hẹn
          </Button>
        )}
      </div>

      {actionError && (
        <Alert
          className="mb-6"
          actions={
            <Button variant="secondary" size="sm" onClick={() => setActionError(null)}>
              Đóng
            </Button>
          }
        >
          {actionError}
        </Alert>
      )}

      {/* Filter Tabs & Search Bar */}
      <div className="flex flex-col sm:flex-row items-stretch sm:items-center justify-between gap-3 mb-6">
        {/* Status Tabs */}
        <div className="flex items-center gap-1 p-1 bg-stone-100 rounded-xl overflow-x-auto scrollbar-none">
          {FILTER_TABS.map((st) => {
            const label = st === 'ALL' ? 'Tất cả' : STATUS_CONFIG[st].label;
            const count = st === 'ALL' ? bookings.length : bookings.filter((b) => b.status === st).length;
            const active = statusFilter === st;

            return (
              <button
                key={st}
                type="button"
                onClick={() => setStatusFilter(st)}
                className={`px-3 py-1.5 rounded-lg text-xs font-medium whitespace-nowrap transition-colors flex items-center gap-1.5 ${
                  active ? 'bg-white text-stone-900 shadow-2xs font-semibold' : 'text-stone-600 hover:text-stone-900'
                }`}
              >
                <span>{label}</span>
                <span className={`px-1.5 py-0.2 rounded-full text-[10px] ${active ? 'bg-stone-100 text-stone-700' : 'text-stone-400'}`}>
                  {count}
                </span>
              </button>
            );
          })}
        </div>

        {/* Search Input */}
        <div className="relative w-full sm:w-64">
          <Search size={14} className="absolute left-3 top-1/2 -translate-y-1/2 text-stone-400" />
          <input
            type="text"
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
            placeholder="Tìm theo tên, mã, dịch vụ..."
            className="w-full h-9 pl-9 pr-3 text-xs rounded-xl bg-white border border-stone-200 text-stone-900 focus:outline-none focus:border-[#465d4c] focus:ring-1 focus:ring-[#465d4c]"
          />
        </div>
      </div>

      {isLoading && (
        <div className="grid grid-cols-1 gap-3 animate-pulse" aria-busy="true">
          {[1, 2, 3].map((i) => (
            <Card key={i}>
              <CardContent className="h-24 bg-stone-100 rounded-xl" />
            </Card>
          ))}
        </div>
      )}

      {error && !isLoading && (
        <ErrorState message={error} onRetry={fetchBookings} />
      )}

      {!isLoading && !error && filteredBookings.length === 0 && (
        <EmptyState
          icon={<Calendar aria-hidden="true" size={24} />}
          title={searchQuery ? 'Không tìm thấy kết quả' : 'Chưa có lịch hẹn'}
          description={
            searchQuery
              ? 'Thử thay đổi từ khóa hoặc bộ lọc trạng thái để tìm kiếm.'
              : isOwner
              ? 'Lịch hẹn mới từ khách hoặc do bạn tạo sẽ được hiển thị tại đây.'
              : 'Bạn chưa có lịch hẹn nào trong danh mục này.'
          }
          action={
            isOwner ? (
              <Button onClick={() => handleOpenForm()}>
                <Plus aria-hidden="true" size={16} />
                Tạo lịch hẹn
              </Button>
            ) : undefined
          }
        />
      )}

      {!isLoading && !error && filteredBookings.length > 0 && (
        <div className="grid grid-cols-1 gap-3.5">
          {filteredBookings.map((booking) => {
            const status = STATUS_CONFIG[booking.status];
            const isBusy = busyOp?.id === booking.id;
            return (
              <Card key={booking.id} className="border-[#e7e2d8] hover:border-[#c6d8c9] transition-all">
                <CardContent className="flex flex-col gap-4 p-4 sm:p-5 lg:flex-row lg:items-center lg:justify-between">
                  <div className="min-w-0 flex-1">
                    <div className="flex flex-wrap items-center gap-x-2.5 gap-y-1">
                      <span className="font-semibold text-stone-900 text-sm sm:text-base">
                        {booking.customerName}
                      </span>
                      <Badge tone={status.tone}>{status.label}</Badge>
                      {booking.bookingCode && (
                        <span className="text-[11px] font-mono text-stone-500 bg-stone-100 px-2 py-0.5 rounded border border-stone-200">
                          {booking.bookingCode}
                        </span>
                      )}
                    </div>

                    <p className="mt-1 text-xs sm:text-sm font-medium text-[#465d4c]">
                      {booking.serviceName}
                      <span className="font-normal text-stone-500">
                        {' '}· {formatCurrency(booking.price)}
                      </span>
                    </p>

                    <div className="mt-2 flex flex-wrap gap-x-4 gap-y-1 text-xs text-stone-600">
                      <span className="inline-flex items-center gap-1.5">
                        <Clock aria-hidden="true" size={14} className="text-stone-400" />
                        {formatTimeRange(booking.startTime, booking.endTime)}
                      </span>
                      <span className="inline-flex items-center gap-1.5">
                        <UserRound aria-hidden="true" size={14} className="text-stone-400" />
                        <span>KTV: <strong className="text-stone-800">{booking.staffName || 'Chưa phân công'}</strong></span>
                      </span>
                    </div>
                  </div>

                  {/* Actions Bar */}
                  <div className="flex flex-wrap items-center gap-2 lg:shrink-0 pt-2 lg:pt-0 border-t lg:border-t-0 border-stone-100">
                    <Button
                      variant="secondary"
                      size="sm"
                      onClick={() => handleOpenDetail(booking.id)}
                    >
                      <Eye size={14} className="mr-1" />
                      Chi tiết
                    </Button>

                    {/* OWNER-ONLY Management: Assign & Edit (only for active, non-terminal bookings) */}
                    {isOwner && booking.status !== 'COMPLETED' && booking.status !== 'CANCELLED' && booking.status !== 'NO_SHOW' && (
                      <>
                        <Button
                          variant="secondary"
                          size="sm"
                          onClick={() => handleOpenAssign(booking)}
                          disabled={isBusy}
                          title="Phân công nhân viên"
                        >
                          <UserCheck size={14} className="mr-1" />
                          Phân công
                        </Button>

                        <Button
                          variant="secondary"
                          size="sm"
                          onClick={() => handleOpenForm(booking)}
                          disabled={isBusy}
                        >
                          Sửa
                        </Button>
                      </>
                    )}

                    {/* PENDING Operations */}
                    {booking.status === 'PENDING' && (
                      <>
                        <Button
                          size="sm"
                          onClick={() => handleConfirmBooking(booking.id)}
                          disabled={isBusy}
                        >
                          {isBusy && busyOp?.action === 'confirm' ? 'Đang duyệt...' : 'Xác nhận'}
                        </Button>
                        <Button
                          variant="secondary"
                          size="sm"
                          onClick={() => handleOpenReschedule(booking)}
                          disabled={isBusy}
                        >
                          <CalendarClock size={13} className="mr-1" />
                          Đổi giờ
                        </Button>
                        <Button
                          variant="danger-outline"
                          size="sm"
                          onClick={() => handleOpenCancel(booking)}
                          disabled={isBusy}
                        >
                          Hủy
                        </Button>
                      </>
                    )}

                    {/* CONFIRMED Operations */}
                    {booking.status === 'CONFIRMED' && (
                      <>
                        <Button
                          size="sm"
                          onClick={() => handleCheckIn(booking.id)}
                          disabled={isBusy}
                        >
                          {isBusy && busyOp?.action === 'checkin' ? 'Đang xử lý...' : 'Check-in'}
                        </Button>
                        <Button
                          variant="secondary"
                          size="sm"
                          onClick={() => handleOpenReschedule(booking)}
                          disabled={isBusy}
                        >
                          <CalendarClock size={13} className="mr-1" />
                          Đổi giờ
                        </Button>
                        <Button
                          variant="secondary"
                          size="sm"
                          onClick={() => handleOpenNoShow(booking)}
                          disabled={isBusy}
                          className="text-stone-600 hover:text-stone-900"
                        >
                          <UserX size={13} className="mr-1" />
                          No-show
                        </Button>
                        <Button
                          variant="danger-outline"
                          size="sm"
                          onClick={() => handleOpenCancel(booking)}
                          disabled={isBusy}
                        >
                          Hủy
                        </Button>
                      </>
                    )}

                    {/* CHECKED_IN Operations */}
                    {booking.status === 'CHECKED_IN' && (
                      <>
                        <Button
                          size="sm"
                          onClick={() => handleStart(booking.id)}
                          disabled={isBusy}
                          className="bg-indigo-700 hover:bg-indigo-800 text-white"
                        >
                          <Play size={13} className="mr-1" />
                          {isBusy && busyOp?.action === 'start' ? 'Đang bắt đầu...' : 'Bắt đầu'}
                        </Button>
                        <Button
                          variant="danger-outline"
                          size="sm"
                          onClick={() => handleOpenCancel(booking)}
                          disabled={isBusy}
                        >
                          Hủy
                        </Button>
                      </>
                    )}

                    {/* IN_PROGRESS Operations */}
                    {booking.status === 'IN_PROGRESS' && (
                      <Button
                        size="sm"
                        onClick={() => handleComplete(booking.id)}
                        disabled={isBusy}
                        className="bg-emerald-700 hover:bg-emerald-800 text-white"
                      >
                        <CheckCircle2 size={13} className="mr-1" />
                        {isBusy && busyOp?.action === 'complete' ? 'Đang hoàn tất...' : 'Hoàn thành'}
                      </Button>
                    )}
                  </div>
                </CardContent>
              </Card>
            );
          })}
        </div>
      )}

      {/* Booking Detail Modal */}
      {(detailBooking || detailLoading) && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-stone-900/50 backdrop-blur-xs">
          <div className="bg-white rounded-2xl border border-stone-200 max-w-xl w-full p-6 sm:p-7 shadow-2xl relative animate-in fade-in zoom-in-95 duration-200 max-h-[90vh] overflow-y-auto">
            <button
              type="button"
              onClick={() => setDetailBooking(null)}
              className="absolute right-4 top-4 text-stone-400 hover:text-stone-700 p-2 rounded-xl hover:bg-stone-100 transition-colors"
              aria-label="Đóng"
            >
              <X size={18} />
            </button>

            {detailLoading ? (
              <div className="py-16 text-center text-sm text-stone-500">Đang tải chi tiết cuộc hẹn...</div>
            ) : detailBooking && (
              <div className="space-y-5">
                {/* Header */}
                <div className="pr-8">
                  <div className="flex flex-wrap items-center gap-2 mb-1.5">
                    <h3 className="font-serif-title font-semibold text-xl text-stone-900">
                      Chi tiết lịch hẹn
                    </h3>
                    <Badge tone={STATUS_CONFIG[detailBooking.status].tone}>
                      {STATUS_CONFIG[detailBooking.status].label}
                    </Badge>
                  </div>
                  <div className="flex flex-wrap items-center gap-x-4 gap-y-1 text-xs text-stone-500">
                    {detailBooking.bookingCode && (
                      <span className="flex items-center gap-1 font-mono">
                        Mã: <strong className="text-stone-800">{detailBooking.bookingCode}</strong>
                        <button
                          type="button"
                          onClick={() => handleCopyCode(detailBooking.bookingCode!)}
                          className="text-stone-400 hover:text-stone-700 p-0.5 rounded transition-colors"
                          title="Sao chép mã"
                        >
                          {copiedCode ? <Check size={12} className="text-emerald-600" /> : <Copy size={12} />}
                        </button>
                      </span>
                    )}
                    {detailBooking.createdAt && (
                      <span>Tạo lúc: {new Date(detailBooking.createdAt).toLocaleString('vi-VN')}</span>
                    )}
                  </div>
                </div>

                {/* Operations Bar in Detail Modal */}
                <div className="p-3 bg-stone-100/80 rounded-xl border border-stone-200 flex flex-wrap items-center justify-between gap-2.5">
                  <div className="flex items-center gap-2">
                    <span className="text-xs font-semibold text-stone-700">Trạng thái:</span>
                    <Badge tone={STATUS_CONFIG[detailBooking.status].tone}>
                      {STATUS_CONFIG[detailBooking.status].label}
                    </Badge>
                  </div>
                  <div className="flex flex-wrap items-center gap-1.5">
                    {detailBooking.status === 'PENDING' && (
                      <>
                        <Button
                          size="sm"
                          onClick={() => handleConfirmBooking(detailBooking.id)}
                          disabled={busyOp?.id === detailBooking.id}
                        >
                          Xác nhận
                        </Button>
                        <Button
                          variant="secondary"
                          size="sm"
                          onClick={() => handleOpenReschedule(detailBooking)}
                          disabled={busyOp?.id === detailBooking.id}
                        >
                          <CalendarClock size={13} className="mr-1" />
                          Đổi giờ
                        </Button>
                        <Button
                          variant="danger-outline"
                          size="sm"
                          onClick={() => handleOpenCancel(detailBooking)}
                          disabled={busyOp?.id === detailBooking.id}
                        >
                          Hủy
                        </Button>
                      </>
                    )}

                    {detailBooking.status === 'CONFIRMED' && (
                      <>
                        <Button
                          size="sm"
                          onClick={() => handleCheckIn(detailBooking.id)}
                          disabled={busyOp?.id === detailBooking.id}
                        >
                          Check-in
                        </Button>
                        <Button
                          variant="secondary"
                          size="sm"
                          onClick={() => handleOpenReschedule(detailBooking)}
                          disabled={busyOp?.id === detailBooking.id}
                        >
                          <CalendarClock size={13} className="mr-1" />
                          Đổi giờ
                        </Button>
                        <Button
                          variant="secondary"
                          size="sm"
                          onClick={() => handleOpenNoShow(detailBooking)}
                          disabled={busyOp?.id === detailBooking.id}
                        >
                          <UserX size={13} className="mr-1" />
                          No-show
                        </Button>
                        <Button
                          variant="danger-outline"
                          size="sm"
                          onClick={() => handleOpenCancel(detailBooking)}
                          disabled={busyOp?.id === detailBooking.id}
                        >
                          Hủy
                        </Button>
                      </>
                    )}

                    {detailBooking.status === 'CHECKED_IN' && (
                      <>
                        <Button
                          size="sm"
                          onClick={() => handleStart(detailBooking.id)}
                          disabled={busyOp?.id === detailBooking.id}
                          className="bg-indigo-700 hover:bg-indigo-800 text-white"
                        >
                          <Play size={13} className="mr-1" />
                          Bắt đầu
                        </Button>
                        <Button
                          variant="danger-outline"
                          size="sm"
                          onClick={() => handleOpenCancel(detailBooking)}
                          disabled={busyOp?.id === detailBooking.id}
                        >
                          Hủy
                        </Button>
                      </>
                    )}

                    {detailBooking.status === 'IN_PROGRESS' && (
                      <Button
                        size="sm"
                        onClick={() => handleComplete(detailBooking.id)}
                        disabled={busyOp?.id === detailBooking.id}
                        className="bg-emerald-700 hover:bg-emerald-800 text-white"
                      >
                        <CheckCircle2 size={13} className="mr-1" />
                        Hoàn thành
                      </Button>
                    )}

                    {detailBooking.status === 'COMPLETED' && (
                      <span className="text-xs text-stone-500 italic">Đã hoàn thành dịch vụ</span>
                    )}

                    {detailBooking.status === 'CANCELLED' && (
                      <span className="text-xs text-rose-600 font-medium">Lịch hẹn đã hủy</span>
                    )}

                    {detailBooking.status === 'NO_SHOW' && (
                      <span className="text-xs text-stone-500 font-medium">Khách không đến</span>
                    )}
                  </div>
                </div>

                {/* Operations History Timestamps */}
                {(detailBooking.confirmedAt ||
                  detailBooking.checkedInAt ||
                  detailBooking.startedAt ||
                  detailBooking.completedAt ||
                  detailBooking.cancelledAt ||
                  detailBooking.noShowAt ||
                  detailBooking.cancellationReason) && (
                  <div className="bg-stone-50 rounded-xl p-3.5 border border-stone-200/70 space-y-2">
                    <span className="text-[11px] uppercase font-bold text-stone-400 tracking-wider flex items-center gap-1.5">
                      <Clock size={13} /> Lịch sử vận hành
                    </span>
                    <div className="grid grid-cols-1 sm:grid-cols-2 gap-2 text-xs">
                      {detailBooking.confirmedAt && (
                        <div>
                          <span className="text-stone-400 block text-[11px]">Xác nhận lúc</span>
                          <span className="text-stone-800 font-medium">{new Date(detailBooking.confirmedAt).toLocaleString('vi-VN')}</span>
                        </div>
                      )}
                      {detailBooking.checkedInAt && (
                        <div>
                          <span className="text-stone-400 block text-[11px]">Check-in lúc</span>
                          <span className="text-stone-800 font-medium">{new Date(detailBooking.checkedInAt).toLocaleString('vi-VN')}</span>
                        </div>
                      )}
                      {detailBooking.startedAt && (
                        <div>
                          <span className="text-stone-400 block text-[11px]">Bắt đầu làm lúc</span>
                          <span className="text-stone-800 font-medium">{new Date(detailBooking.startedAt).toLocaleString('vi-VN')}</span>
                        </div>
                      )}
                      {detailBooking.completedAt && (
                        <div>
                          <span className="text-stone-400 block text-[11px]">Hoàn thành lúc</span>
                          <span className="text-stone-800 font-medium">{new Date(detailBooking.completedAt).toLocaleString('vi-VN')}</span>
                        </div>
                      )}
                      {detailBooking.cancelledAt && (
                        <div>
                          <span className="text-stone-400 block text-[11px]">Hủy lúc</span>
                          <span className="text-stone-800 font-medium">{new Date(detailBooking.cancelledAt).toLocaleString('vi-VN')}</span>
                        </div>
                      )}
                      {detailBooking.noShowAt && (
                        <div>
                          <span className="text-stone-400 block text-[11px]">Đánh dấu No-show lúc</span>
                          <span className="text-stone-800 font-medium">{new Date(detailBooking.noShowAt).toLocaleString('vi-VN')}</span>
                        </div>
                      )}
                      {detailBooking.cancellationReason && (
                        <div className="sm:col-span-2 pt-1 border-t border-stone-200/50">
                          <span className="text-stone-400 block text-[11px]">Lý do hủy</span>
                          <p className="text-rose-700 italic">{detailBooking.cancellationReason}</p>
                        </div>
                      )}
                    </div>
                  </div>
                )}

                {/* Customer Information */}
                <div className="bg-stone-50 rounded-xl p-4 border border-stone-200/70 space-y-2.5">
                  <span className="text-[11px] uppercase font-bold text-stone-400 tracking-wider flex items-center gap-1.5">
                    <UserRound size={13} /> Thông tin khách hàng
                  </span>
                  <div className="grid grid-cols-1 sm:grid-cols-2 gap-2 text-xs">
                    <div>
                      <span className="text-stone-400 block text-[11px]">Họ và tên</span>
                      <strong className="text-stone-900 text-sm font-semibold">{detailBooking.customerName}</strong>
                    </div>
                    <div>
                      <span className="text-stone-400 block text-[11px]">Số điện thoại</span>
                      {detailBooking.customerPhone ? (
                        <a href={`tel:${detailBooking.customerPhone}`} className="text-stone-800 font-medium hover:underline flex items-center gap-1">
                          <Phone size={12} className="text-stone-400" /> {detailBooking.customerPhone}
                        </a>
                      ) : (
                        <span className="text-stone-400">Chưa có</span>
                      )}
                    </div>
                    {detailBooking.customerEmail && (
                      <div className="sm:col-span-2">
                        <span className="text-stone-400 block text-[11px]">Email</span>
                        <a href={`mailto:${detailBooking.customerEmail}`} className="text-stone-800 hover:underline flex items-center gap-1">
                          <Mail size={12} className="text-stone-400" /> {detailBooking.customerEmail}
                        </a>
                      </div>
                    )}
                  </div>
                </div>

                {/* Service & Schedule */}
                <div className="bg-stone-50 rounded-xl p-4 border border-stone-200/70 space-y-3">
                  <span className="text-[11px] uppercase font-bold text-stone-400 tracking-wider flex items-center gap-1.5">
                    <Sparkles size={13} /> Dịch vụ & Lịch hẹn
                  </span>
                  <div className="grid grid-cols-1 sm:grid-cols-2 gap-3 text-xs">
                    <div>
                      <span className="text-stone-400 block text-[11px]">Dịch vụ</span>
                      <p className="font-semibold text-stone-900 text-sm mt-0.5">{detailBooking.serviceName}</p>
                      {detailBooking.categoryName && (
                        <span className="inline-block mt-1 text-[10px] font-medium text-stone-600 bg-stone-200/70 px-1.5 py-0.5 rounded">
                          {detailBooking.categoryName}
                        </span>
                      )}
                    </div>
                    <div>
                      <span className="text-stone-400 block text-[11px]">Thời lượng & Đơn giá</span>
                      <p className="text-stone-800 mt-0.5 font-medium">
                        {detailBooking.durationMinutes || detailBooking.serviceDuration || 60} phút &middot; <span className="text-[#465d4c] font-semibold">{formatCurrency(detailBooking.price)}</span>
                      </p>
                    </div>
                    <div className="sm:col-span-2 pt-1 border-t border-stone-200/50 flex items-center gap-2 text-stone-700">
                      <Clock size={14} className="text-[#465d4c]" />
                      <span>Thời gian hẹn: <strong>{formatTimeRange(detailBooking.startTime, detailBooking.endTime)}</strong></span>
                    </div>
                  </div>

                  {/* Read-only Process Steps */}
                  {parseSteps(detailBooking.processSteps).length > 0 && (
                    <div className="pt-2.5 border-t border-stone-200/60">
                      <span className="text-[11px] font-bold text-stone-500 uppercase tracking-wider block mb-1.5">
                        Quy trình thực hiện ({parseSteps(detailBooking.processSteps).length} bước)
                      </span>
                      <ol className="space-y-1 text-xs text-stone-700 list-decimal list-inside pl-1 bg-white p-2.5 rounded-lg border border-stone-200/60">
                        {parseSteps(detailBooking.processSteps).map((step, idx) => (
                          <li key={idx} className="leading-relaxed">{step}</li>
                        ))}
                      </ol>
                    </div>
                  )}
                </div>

                {/* Assigned Staff */}
                <div className="bg-stone-50 rounded-xl p-4 border border-stone-200/70 space-y-2">
                  <span className="text-[11px] uppercase font-bold text-stone-400 tracking-wider flex items-center gap-1.5">
                    <UserCheck size={13} /> Kỹ thuật viên phụ trách
                  </span>
                  <div className="flex flex-wrap items-center justify-between gap-2 text-xs">
                    <div>
                      <p className="font-semibold text-stone-900 text-sm">{detailBooking.staffName || 'Chưa phân công'}</p>
                      {detailBooking.staffPhone && (
                        <p className="text-stone-600 mt-0.5 flex items-center gap-1">
                          <Phone size={12} className="text-stone-400" /> {detailBooking.staffPhone}
                        </p>
                      )}
                    </div>
                    {isOwner && detailBooking.status !== 'COMPLETED' && detailBooking.status !== 'CANCELLED' && detailBooking.status !== 'NO_SHOW' && (
                      <Button
                        size="sm"
                        variant="secondary"
                        onClick={() => {
                          const target = bookings.find((b) => b.id === detailBooking.id) || detailBooking;
                          setDetailBooking(null);
                          handleOpenAssign(target);
                        }}
                      >
                        Đổi nhân viên
                      </Button>
                    )}
                  </div>
                </div>

                {/* Payment Details */}
                <div className="bg-stone-50 rounded-xl p-4 border border-stone-200/70 space-y-2.5">
                  <span className="text-[11px] uppercase font-bold text-stone-400 tracking-wider flex items-center gap-1.5">
                    <CreditCard size={13} /> Thông tin thanh toán
                  </span>
                  <div className="grid grid-cols-1 sm:grid-cols-2 gap-2.5 text-xs">
                    <div>
                      <span className="text-stone-400 block text-[11px]">Phương thức</span>
                      <strong className="text-stone-900">
                        {detailBooking.paymentMethod === 'VNPAY' ? 'VNPay Sandbox' : 'Thanh toán tại Spa'}
                      </strong>
                    </div>
                    <div>
                      <span className="text-stone-400 block text-[11px]">Trạng thái thanh toán</span>
                      <span className={`inline-block font-semibold px-2 py-0.5 rounded text-[11px] ${
                        detailBooking.paymentStatus === 'PAID'
                          ? 'bg-emerald-100 text-emerald-800'
                          : detailBooking.paymentStatus === 'CANCELLED'
                          ? 'bg-rose-100 text-rose-800'
                          : 'bg-amber-100 text-amber-800'
                      }`}>
                        {detailBooking.paymentStatus === 'PAID'
                          ? 'Đã thanh toán (PAID)'
                          : detailBooking.paymentStatus === 'CANCELLED'
                          ? 'Đã hủy (CANCELLED)'
                          : 'Chờ thanh toán (UNPAID)'}
                      </span>
                    </div>
                    <div>
                      <span className="text-stone-400 block text-[11px]">Số tiền</span>
                      <strong className="text-stone-900 text-sm font-serif-title">
                        {formatCurrency(detailBooking.paidAmount || detailBooking.price)}
                      </strong>
                    </div>
                    {detailBooking.paidAt && (
                      <div>
                        <span className="text-stone-400 block text-[11px]">Thời gian thanh toán</span>
                        <span className="text-stone-700">
                          {new Date(detailBooking.paidAt).toLocaleString('vi-VN')}
                        </span>
                      </div>
                    )}
                    {detailBooking.transactionNo && (
                      <div>
                        <span className="text-stone-400 block text-[11px]">Mã GD Cổng (Transaction No)</span>
                        <span className="font-mono text-stone-700">{detailBooking.transactionNo}</span>
                      </div>
                    )}
                    {detailBooking.txnRef && (
                      <div>
                        <span className="text-stone-400 block text-[11px]">Mã tham chiếu (TxnRef)</span>
                        <span className="font-mono text-stone-700 text-[11px] truncate block" title={detailBooking.txnRef}>
                          {detailBooking.txnRef}
                        </span>
                      </div>
                    )}
                    {detailBooking.bankCode && (
                      <div>
                        <span className="text-stone-400 block text-[11px]">Ngân hàng / Cổng</span>
                        <span className="text-stone-700">{detailBooking.bankCode}</span>
                      </div>
                    )}
                  </div>

                  {/* Refund Information Block if applicable */}
                  {detailBooking.refundStatus && (
                    <div className="mt-3 pt-3 border-t border-stone-200/60 bg-white/70 p-3 rounded-lg border space-y-2">
                      <div className="flex items-center justify-between">
                        <span className="text-xs font-semibold uppercase tracking-wider text-stone-700 flex items-center gap-1.5">
                          <RotateCcw size={13} className="text-[#8a704c]" />
                          Thông tin hoàn tiền (VNPay Refund)
                        </span>
                        <span className={`inline-flex items-center px-2 py-0.5 rounded text-[11px] font-semibold ${
                          detailBooking.refundStatus === 'REFUNDED'
                            ? 'bg-emerald-100 text-emerald-800'
                            : detailBooking.refundStatus === 'REFUND_PENDING'
                            ? 'bg-amber-100 text-amber-800'
                            : 'bg-rose-100 text-rose-800'
                        }`}>
                          {detailBooking.refundStatus === 'REFUNDED'
                            ? 'Đã hoàn tiền (REFUNDED)'
                            : detailBooking.refundStatus === 'REFUND_PENDING'
                            ? 'Đang xử lý (REFUND_PENDING)'
                            : 'Hoàn tiền thất bại (REFUND_FAILED)'}
                        </span>
                      </div>

                      <div className="grid grid-cols-1 sm:grid-cols-2 gap-2 text-xs">
                        <div>
                          <span className="text-stone-400 block text-[11px]">Số tiền hoàn thực tế</span>
                          <strong className="text-emerald-700 text-sm font-serif-title">
                            {formatCurrency(detailBooking.refundAmount || 0)}
                          </strong>
                        </div>
                        <div>
                          <span className="text-stone-400 block text-[11px]">Phí hủy lịch (TIKEY SPA)</span>
                          <strong className="text-stone-800 text-sm font-serif-title">
                            {formatCurrency(detailBooking.cancellationFee || 0)}
                          </strong>
                        </div>
                        {detailBooking.refundPolicyPercentage != null && (
                          <div>
                            <span className="text-stone-400 block text-[11px]">Tỷ lệ hoàn</span>
                            <span className="text-stone-800 font-medium">{detailBooking.refundPolicyPercentage}%</span>
                          </div>
                        )}
                        {detailBooking.refundReason && (
                          <div className="sm:col-span-2">
                            <span className="text-stone-400 block text-[11px]">Lý do hoàn</span>
                            <span className="text-stone-700 italic">{detailBooking.refundReason}</span>
                          </div>
                        )}
                      </div>
                    </div>
                  )}

                  {/* Owner Refund Button if Cancelled and paid online but not yet refunded */}
                  {isOwner &&
                    detailBooking.paymentMethod === 'VNPAY' &&
                    detailBooking.status === 'CANCELLED' &&
                    (!detailBooking.refundStatus || detailBooking.refundStatus === 'REFUND_FAILED') && (
                      <div className="mt-3 pt-2 border-t border-stone-200/60 flex justify-end">
                        <Button
                          size="sm"
                          variant="danger"
                          onClick={() => handleOpenRefund(detailBooking)}
                        >
                          <RotateCcw size={13} className="mr-1" />
                          {detailBooking.refundStatus === 'REFUND_FAILED' ? 'Thử lại hoàn tiền' : 'Hoàn tiền qua VNPay'}
                        </Button>
                      </div>
                    )}
                </div>

                {/* Footer Buttons */}
                <div className="flex justify-end gap-2 pt-2 border-t border-stone-100">
                  <Button variant="secondary" onClick={() => setDetailBooking(null)}>
                    Đóng
                  </Button>
                </div>
              </div>
            )}
          </div>
        </div>
      )}

      {/* Reschedule Modal */}
      {rescheduleTarget && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-stone-900/50 backdrop-blur-xs">
          <div className="bg-white rounded-2xl border border-stone-200 max-w-md w-full p-6 shadow-2xl animate-in fade-in zoom-in-95 duration-200">
            <div className="flex items-center justify-between mb-3">
              <h3 className="font-serif-title font-semibold text-lg text-stone-900 flex items-center gap-2">
                <CalendarClock size={20} className="text-[#465d4c]" />
                Dời lịch hẹn (Reschedule)
              </h3>
              <button
                type="button"
                onClick={() => setRescheduleTarget(null)}
                className="text-stone-400 hover:text-stone-700 p-1.5 rounded-lg hover:bg-stone-100 transition-colors"
                aria-label="Đóng"
              >
                <X size={16} />
              </button>
            </div>

            <div className="p-3 bg-stone-50 rounded-xl border border-stone-200/70 mb-4 text-xs space-y-1">
              <div>
                <span className="text-stone-500">Khách hàng: </span>
                <strong className="text-stone-900">{rescheduleTarget.customerName}</strong>
                {rescheduleTarget.bookingCode && (
                  <span className="ml-2 font-mono text-[11px] text-stone-500">({rescheduleTarget.bookingCode})</span>
                )}
              </div>
              <div>
                <span className="text-stone-500">Dịch vụ: </span>
                <strong className="text-stone-800">{rescheduleTarget.serviceName}</strong>
              </div>
              <div>
                <span className="text-stone-500">Thời gian hiện tại: </span>
                <span className="text-stone-700 font-medium">
                  {formatTimeRange(rescheduleTarget.startTime, rescheduleTarget.endTime)}
                </span>
              </div>
            </div>

            {rescheduleError && (
              <div className="p-3 rounded-xl bg-rose-50 border border-rose-200 text-rose-800 text-xs mb-4 flex items-start gap-2">
                <AlertCircle size={15} className="shrink-0 mt-0.5 text-rose-600" />
                <span>{rescheduleError}</span>
              </div>
            )}

            <form onSubmit={handleConfirmReschedule} className="space-y-4">
              <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
                <div>
                  <label className="block text-xs font-semibold uppercase tracking-wider text-stone-600 mb-1.5">
                    Ngày hẹn mới
                  </label>
                  <input
                    type="date"
                    value={rescheduleDate}
                    onChange={(e) => setRescheduleDate(e.target.value)}
                    required
                    className="w-full h-11 px-3 rounded-xl bg-stone-50 border border-stone-200 text-stone-900 text-sm focus:outline-none focus:border-[#465d4c]"
                  />
                </div>

                <div>
                  <label className="block text-xs font-semibold uppercase tracking-wider text-stone-600 mb-1.5">
                    Giờ bắt đầu mới
                  </label>
                  <input
                    type="time"
                    value={rescheduleTime}
                    onChange={(e) => setRescheduleTime(e.target.value)}
                    required
                    className="w-full h-11 px-3 rounded-xl bg-stone-50 border border-stone-200 text-stone-900 text-sm focus:outline-none focus:border-[#465d4c]"
                  />
                </div>
              </div>

              {isOwner && activeStaffList.length > 0 && (
                <div>
                  <label className="block text-xs font-semibold uppercase tracking-wider text-stone-600 mb-1.5">
                    Kỹ thuật viên phụ trách
                  </label>
                  <select
                    value={rescheduleStaffId || ''}
                    onChange={(e) => setRescheduleStaffId(Number(e.target.value) || null)}
                    className="w-full h-11 px-3 rounded-xl bg-stone-50 border border-stone-200 text-stone-900 text-sm focus:outline-none focus:border-[#465d4c]"
                  >
                    <option value="">Giữ kỹ thuật viên hiện tại</option>
                    {activeStaffList.map((st) => (
                      <option key={st.id} value={st.id}>
                        {st.name} {st.phone ? `(${st.phone})` : ''}
                      </option>
                    ))}
                  </select>
                </div>
              )}

              <p className="text-[11px] text-stone-400">
                Hệ thống sẽ giữ nguyên mã booking, thông tin thanh toán và tự động kiểm tra trùng giờ.
              </p>

              <div className="flex justify-end gap-2 pt-2 border-t border-stone-100">
                <Button
                  type="button"
                  variant="secondary"
                  onClick={() => setRescheduleTarget(null)}
                  disabled={rescheduleSubmitting}
                >
                  Quay lại
                </Button>
                <Button
                  type="submit"
                  disabled={rescheduleSubmitting}
                >
                  {rescheduleSubmitting ? 'Đang cập nhật...' : 'Xác nhận dời lịch'}
                </Button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* Cancel Confirmation Modal */}
      {cancelTarget && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-stone-900/50 backdrop-blur-xs">
          <div className="bg-white rounded-2xl border border-stone-200 max-w-md w-full p-6 shadow-2xl animate-in fade-in zoom-in-95 duration-200">
            <div className="flex items-center justify-between mb-3">
              <h3 className="font-serif-title font-semibold text-lg text-stone-900 flex items-center gap-2">
                <Ban size={20} className="text-rose-600" />
                Hủy lịch hẹn?
              </h3>
              <button
                type="button"
                onClick={() => setCancelTarget(null)}
                className="text-stone-400 hover:text-stone-700 p-1.5 rounded-lg hover:bg-stone-100 transition-colors"
                aria-label="Đóng"
              >
                <X size={16} />
              </button>
            </div>

            <div className="p-3 bg-stone-50 rounded-xl border border-stone-200/70 mb-4 text-xs space-y-1.5">
              <div>
                <span className="text-stone-500">Mã booking: </span>
                <strong className="font-mono text-stone-900">{cancelTarget.bookingCode || `#${cancelTarget.id}`}</strong>
              </div>
              <div>
                <span className="text-stone-500">Khách hàng: </span>
                <strong className="text-stone-900">{cancelTarget.customerName}</strong>
              </div>
              <div>
                <span className="text-stone-500">Dịch vụ: </span>
                <strong className="text-stone-800">{cancelTarget.serviceName}</strong>
              </div>
              <div>
                <span className="text-stone-500">Thời gian: </span>
                <span className="text-stone-700 font-medium">
                  {formatTimeRange(cancelTarget.startTime, cancelTarget.endTime)}
                </span>
              </div>
            </div>

            {/* Refund Policy Preview if Paid Online */}
            {cancelEligibilityLoading ? (
              <div className="p-3 bg-stone-50 rounded-xl border border-stone-200/70 mb-4 text-xs text-stone-500 italic">
                Đang đối soát chính sách hoàn tiền...
              </div>
            ) : cancelEligibility && cancelEligibility.refundEligible && (
              <div className="p-3.5 bg-amber-50/70 rounded-xl border border-amber-200/80 mb-4 text-xs space-y-1.5 text-stone-800">
                <div className="font-semibold text-amber-900 flex items-center gap-1.5">
                  <RotateCcw size={14} className="text-amber-700" />
                  Chính sách hủy & hoàn tiền TIKEY SPA
                </div>
                <div className="grid grid-cols-2 gap-2 pt-1">
                  <div>
                    <span className="text-stone-500 block text-[11px]">Đã thanh toán</span>
                    <strong className="text-stone-900">{formatCurrency(cancelEligibility.originalPaidAmount)}</strong>
                  </div>
                  <div>
                    <span className="text-stone-500 block text-[11px]">Dự kiến hoàn ({cancelEligibility.refundPercentage}%)</span>
                    <strong className="text-emerald-700">{formatCurrency(cancelEligibility.refundAmount)}</strong>
                  </div>
                  {cancelEligibility.cancellationFee > 0 && (
                    <div className="col-span-2 text-stone-600 pt-0.5">
                      <span className="text-stone-500">Phí hủy lịch ({100 - cancelEligibility.refundPercentage}%): </span>
                      <span className="font-semibold text-rose-700">{formatCurrency(cancelEligibility.cancellationFee)}</span>
                    </div>
                  )}
                </div>
                <p className="text-[11px] text-stone-600 italic pt-1 border-t border-amber-200/50">
                  {cancelEligibility.policyDescription}
                </p>
              </div>
            )}

            {cancelError && (
              <div className="p-3 rounded-xl bg-rose-50 border border-rose-200 text-rose-800 text-xs mb-4 flex items-start gap-2">
                <AlertCircle size={15} className="shrink-0 mt-0.5 text-rose-600" />
                <span>{cancelError}</span>
              </div>
            )}

            <form onSubmit={handleConfirmCancel} className="space-y-4">
              <div>
                <label className="block text-xs font-semibold uppercase tracking-wider text-stone-600 mb-1.5">
                  Lý do hủy (không bắt buộc)
                </label>
                <textarea
                  value={cancelReason}
                  onChange={(e) => setCancelReason(e.target.value)}
                  placeholder="Ví dụ: Khách bận đột xuất, Khách yêu cầu dời lịch khác..."
                  rows={2}
                  className="w-full p-2.5 rounded-xl bg-stone-50 border border-stone-200 text-stone-900 text-xs focus:outline-none focus:border-rose-500 resize-none"
                />
              </div>

              <p className="text-[11px] text-stone-500 italic">
                Lịch hẹn sẽ được chuyển sang trạng thái <strong>Đã hủy (CANCELLED)</strong> và lưu lại lịch sử. Dữ liệu thanh toán được bảo lưu.
              </p>

              <div className="flex justify-end gap-2 pt-2 border-t border-stone-100">
                <Button
                  type="button"
                  variant="secondary"
                  onClick={() => setCancelTarget(null)}
                  disabled={cancelSubmitting}
                >
                  Quay lại
                </Button>
                <Button
                  type="submit"
                  variant="danger"
                  disabled={cancelSubmitting}
                >
                  {cancelSubmitting ? 'Đang hủy...' : 'Xác nhận hủy'}
                </Button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* Owner VNPay Refund Modal */}
      {refundTarget && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-stone-900/50 backdrop-blur-xs">
          <div className="bg-white rounded-2xl border border-stone-200 max-w-md w-full p-6 shadow-2xl animate-in fade-in zoom-in-95 duration-200">
            <div className="flex items-center justify-between mb-3">
              <h3 className="font-serif-title font-semibold text-lg text-stone-900 flex items-center gap-2">
                <RotateCcw size={20} className="text-amber-600" />
                Xác nhận hoàn tiền qua VNPay
              </h3>
              <button
                type="button"
                onClick={() => setRefundTarget(null)}
                className="text-stone-400 hover:text-stone-700 p-1.5 rounded-lg hover:bg-stone-100 transition-colors"
                aria-label="Đóng"
              >
                <X size={16} />
              </button>
            </div>

            <div className="p-3 bg-stone-50 rounded-xl border border-stone-200/70 mb-4 text-xs space-y-1.5">
              <div>
                <span className="text-stone-500">Mã booking: </span>
                <strong className="font-mono text-stone-900">{refundTarget.bookingCode || `#${refundTarget.id}`}</strong>
              </div>
              <div>
                <span className="text-stone-500">Khách hàng: </span>
                <strong className="text-stone-900">{refundTarget.customerName}</strong>
              </div>
              <div>
                <span className="text-stone-500">Dịch vụ: </span>
                <strong className="text-stone-800">{refundTarget.serviceName}</strong>
              </div>
            </div>

            {/* Authoritative Refund Policy Breakdown */}
            {refundLoading ? (
              <div className="p-4 bg-stone-50 rounded-xl border border-stone-200 text-xs text-stone-500 text-center">
                Đang xác minh điều kiện hoàn tiền từ máy chủ...
              </div>
            ) : refundEligibility ? (
              <div className="p-4 bg-gradient-to-br from-amber-50/80 to-stone-50 rounded-xl border border-amber-200/80 mb-4 text-xs space-y-2 text-stone-800">
                <div className="flex items-center justify-between border-b border-amber-200/60 pb-2">
                  <span className="text-stone-600">Đã thanh toán (VNPay):</span>
                  <span className="font-bold text-stone-900 text-sm">
                    {formatCurrency(refundEligibility.originalPaidAmount)}
                  </span>
                </div>

                <div className="flex items-center justify-between">
                  <span className="text-stone-600">
                    Số tiền dự kiến hoàn ({refundEligibility.refundPercentage}%):
                  </span>
                  <strong className="text-emerald-700 text-base font-serif-title">
                    {formatCurrency(refundEligibility.refundAmount)}
                  </strong>
                </div>

                <div className="flex items-center justify-between">
                  <span className="text-stone-600">
                    Phí hủy lịch ({100 - refundEligibility.refundPercentage}%):
                  </span>
                  <span className="text-rose-700 font-semibold">
                    {formatCurrency(refundEligibility.cancellationFee)}
                  </span>
                </div>

                <p className="text-[11px] text-stone-600 italic pt-1.5 border-t border-amber-200/60">
                  {refundEligibility.policyDescription}
                </p>
              </div>
            ) : null}

            {refundError && (
              <div className="p-3 rounded-xl bg-rose-50 border border-rose-200 text-rose-800 text-xs mb-4 flex items-start gap-2">
                <AlertCircle size={15} className="shrink-0 mt-0.5 text-rose-600" />
                <span>{refundError}</span>
              </div>
            )}

            <form onSubmit={handleConfirmRefund} className="space-y-4">
              <div>
                <label className="block text-xs font-semibold uppercase tracking-wider text-stone-600 mb-1.5">
                  Lý do hoàn tiền
                </label>
                <textarea
                  value={refundReason}
                  onChange={(e) => setRefundReason(e.target.value)}
                  placeholder="Khách yêu cầu hủy lịch..."
                  rows={2}
                  className="w-full p-2.5 rounded-xl bg-stone-50 border border-stone-200 text-stone-900 text-xs focus:outline-none focus:border-amber-500 resize-none"
                />
              </div>

              <p className="text-[11px] text-stone-500 italic">
                Hệ thống sẽ gửi yêu cầu hoàn tiền đến cổng VNPay Sandbox với mã giao dịch duy nhất (idempotent). Số tiền hoàn được tính toán tự động từ máy chủ.
              </p>

              <div className="flex justify-end gap-2 pt-2 border-t border-stone-100">
                <Button
                  type="button"
                  variant="secondary"
                  onClick={() => setRefundTarget(null)}
                  disabled={refundSubmitting}
                >
                  Hủy bỏ
                </Button>
                <Button
                  type="submit"
                  disabled={refundSubmitting || refundLoading || (refundEligibility !== null && !refundEligibility.refundEligible)}
                  className="bg-amber-700 hover:bg-amber-800 text-white"
                >
                  {refundSubmitting ? 'Đang gửi yêu cầu hoàn tiền...' : 'Xác nhận hoàn tiền'}
                </Button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* No-Show Confirmation Modal */}
      {noShowTarget && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-stone-900/50 backdrop-blur-xs">
          <div className="bg-white rounded-2xl border border-stone-200 max-w-md w-full p-6 shadow-2xl animate-in fade-in zoom-in-95 duration-200">
            <div className="flex items-center justify-between mb-3">
              <h3 className="font-serif-title font-semibold text-lg text-stone-900 flex items-center gap-2">
                <UserX size={20} className="text-amber-600" />
                Xác nhận khách không đến?
              </h3>
              <button
                type="button"
                onClick={() => setNoShowTarget(null)}
                className="text-stone-400 hover:text-stone-700 p-1.5 rounded-lg hover:bg-stone-100 transition-colors"
                aria-label="Đóng"
              >
                <X size={16} />
              </button>
            </div>

            <div className="p-3 bg-stone-50 rounded-xl border border-stone-200/70 mb-4 text-xs space-y-1.5">
              <div>
                <span className="text-stone-500">Mã booking: </span>
                <strong className="font-mono text-stone-900">{noShowTarget.bookingCode || `#${noShowTarget.id}`}</strong>
              </div>
              <div>
                <span className="text-stone-500">Khách hàng: </span>
                <strong className="text-stone-900">{noShowTarget.customerName}</strong>
              </div>
              <div>
                <span className="text-stone-500">Dịch vụ: </span>
                <strong className="text-stone-800">{noShowTarget.serviceName}</strong>
              </div>
              <div>
                <span className="text-stone-500">Thời gian hẹn: </span>
                <span className="text-stone-700 font-medium">
                  {formatTimeRange(noShowTarget.startTime, noShowTarget.endTime)}
                </span>
              </div>
            </div>

            {noShowError && (
              <div className="p-3 rounded-xl bg-rose-50 border border-rose-200 text-rose-800 text-xs mb-4 flex items-start gap-2">
                <AlertCircle size={15} className="shrink-0 mt-0.5 text-rose-600" />
                <span>{noShowError}</span>
              </div>
            )}

            <p className="text-xs text-stone-600 leading-relaxed mb-4">
              Lịch hẹn sẽ được chuyển sang trạng thái <strong>Không đến (NO_SHOW)</strong>. Lịch này sẽ <strong className="text-stone-900">không được tính vào doanh thu</strong> hoàn thành và không cập nhật ngày đến gần nhất của khách hàng.
            </p>

            <form onSubmit={handleConfirmNoShow}>
              <div className="flex justify-end gap-2 pt-2 border-t border-stone-100">
                <Button
                  type="button"
                  variant="secondary"
                  onClick={() => setNoShowTarget(null)}
                  disabled={noShowSubmitting}
                >
                  Quay lại
                </Button>
                <Button
                  type="submit"
                  disabled={noShowSubmitting}
                  className="bg-stone-800 hover:bg-stone-900 text-white"
                >
                  {noShowSubmitting ? 'Đang lưu...' : 'Đánh dấu No-show'}
                </Button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* Staff Assignment Modal (OWNER ONLY) */}
      {assigningBooking && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-stone-900/50 backdrop-blur-xs">
          <div className="bg-white rounded-2xl border border-stone-200 max-w-md w-full p-6 shadow-xl animate-in fade-in zoom-in-95 duration-200">
            <h3 className="font-serif-title font-semibold text-lg text-stone-900 mb-1">
              Phân công nhân viên phụ trách
            </h3>
            <p className="text-xs text-stone-500 mb-4">
              Lịch hẹn của <strong>{assigningBooking.customerName}</strong> ({assigningBooking.serviceName}, {formatTimeRange(assigningBooking.startTime, assigningBooking.endTime)})
            </p>

            {assignError && (
              <div className="p-3 rounded-xl bg-rose-50 border border-rose-200 text-rose-800 text-xs mb-4 flex items-start gap-2">
                <AlertCircle size={15} className="shrink-0 mt-0.5 text-rose-600" />
                <span>{assignError}</span>
              </div>
            )}

            <form onSubmit={handleConfirmAssign} className="space-y-4">
              <div>
                <label className="block text-xs font-semibold uppercase tracking-wider text-stone-600 mb-1.5">
                  Chọn kỹ thuật viên còn hoạt động
                </label>
                <select
                  value={selectedStaffId || ''}
                  onChange={(e) => setSelectedStaffId(Number(e.target.value))}
                  className="w-full h-11 px-3 rounded-xl bg-stone-50 border border-stone-200 text-stone-900 text-sm focus:outline-none focus:border-[#465d4c]"
                  required
                >
                  <option value="" disabled>-- Chọn kỹ thuật viên --</option>
                  {activeStaffList.map((st) => (
                    <option key={st.id} value={st.id}>
                      {st.name} {st.phone ? `(${st.phone})` : ''}
                    </option>
                  ))}
                </select>
                <p className="text-[11px] text-stone-400 mt-1">
                  Hệ thống sẽ tự động kiểm tra trùng giờ trước khi hoàn tất phân công.
                </p>
              </div>

              <div className="flex justify-end gap-2 pt-2 border-t border-stone-100">
                <Button
                  type="button"
                  variant="secondary"
                  onClick={() => setAssigningBooking(null)}
                  disabled={assignSubmitting}
                >
                  Hủy
                </Button>
                <Button
                  type="submit"
                  disabled={assignSubmitting || !selectedStaffId}
                >
                  {assignSubmitting ? 'Đang phân công...' : 'Lưu phân công'}
                </Button>
              </div>
            </form>
          </div>
        </div>
      )}
    </AppShell>
  );
};

export default Bookings;
